//package org.gwh.controller;
//
//import dev.langchain4j.data.document.Document;
//import lombok.extern.slf4j.Slf4j;
//import org.gwh.ai.DocumentService;
//import org.gwh.ai.RagQueryService;
//import org.gwh.entity.KnowledgeBase;
//import org.gwh.entity.Response;
//import org.gwh.service.KnowledgeBaseService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
///**
// * RAG聊天控制器
// * 提供基于知识库的RAG检索增强生成和普通聊天功能
// * 合并原AiChatController，减少代码重复
// */
//@Slf4j
//@RestController
//@RequestMapping("/api/v1/rag")
//@CrossOrigin("*")
//public class RagChatController {
//
//    @Autowired
//    private RagQueryService ragQueryService;
//
//    @Autowired
//    private DocumentService documentService;
//
//    @Autowired
//    private KnowledgeBaseService knowledgeBaseService;
//
//    @Value("${rag.max-relevant-documents:5}")
//    private int maxRelevantDocuments;
//
//    /**
//     * 基于知识库的RAG问答接口
//     * 与原RagController的chat方法提供相同的API
//     */
//    @PostMapping("/v2/chat")
//    public Response<Map<String, Object>> ragChat(@RequestBody Map<String, Object> params) {
//        return processRagChat(params);
//    }
//
//    /**
//     * 通用聊天接口（转发至/api/v1/ai/chat）
//     * 兼容原AiChatController.chat方法
//     */
//    @RequestMapping("/chat")
//    public Response<Map<String, Object>> chat(@RequestBody Map<String, Object> params) {
//        try {
//            String message = (String) params.get("message");
//            if (message == null || message.isEmpty()) {
//                return Response.error("消息内容不能为空");
//            }
//
//            log.info("通用聊天请求: {}", message);
//            String answer = ragQueryService.chat(message);
//
//            Map<String, Object> result = new HashMap<>();
//            result.put("answer", answer);
//            result.put("sources", new ArrayList<>());
//
//            return Response.success(result);
//        } catch (Exception e) {
//            log.error("聊天失败: ", e);
//            return Response.error("聊天失败: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 处理RAG聊天请求的核心方法
//     * 抽取共用逻辑，避免代码重复
//     */
//    private Response<Map<String, Object>> processRagChat(Map<String, Object> params) {
//        try {
//            String message = (String) params.get("message");
//            String knowledgeBaseId = (String) params.get("knowledgeBaseId");
//
//            if (message == null || message.isEmpty()) {
//                return Response.error("消息内容不能为空");
//            }
//
//            if (knowledgeBaseId == null || knowledgeBaseId.isEmpty()) {
//                return Response.error("知识库ID不能为空");
//            }
//
//            // 获取知识库
//            KnowledgeBase knowledgeBase = knowledgeBaseService.getKnowledgeBaseById(knowledgeBaseId);
//            if (knowledgeBase == null) {
//                return Response.error("知识库不存在: " + knowledgeBaseId);
//            }
//
//            // 检索相关文档
//            List<Document> relevantDocuments = documentService.retrieveRelevantDocuments(
//                    message, knowledgeBase.getName(), maxRelevantDocuments);
//
//            log.info("RAG聊天请求: {}，知识库: {}，检索到 {} 个相关文档",
//                    message, knowledgeBase.getName(), relevantDocuments.size());
//
//            // 如果没有找到相关文档，使用通用聊天
//            if (relevantDocuments.isEmpty()) {
//                String answer = "在知识库中未找到与您问题相关的信息，以下是基于我已有知识的回答：\n\n"
//                        + ragQueryService.chat(message);
//
//                Map<String, Object> result = new HashMap<>();
//                result.put("answer", answer);
//                result.put("sources", new ArrayList<>());
//
//                return Response.success(result);
//            }
//
//            // 准备文档内容
//            String documentsContent = prepareDocumentsContent(relevantDocuments);
//
//            // 使用RAG服务进行问答
//            String answer = ragQueryService.chatWithRAG(message, documentsContent);
//
//            // 准备引用源
//            List<Map<String, String>> sources = prepareSourceReferences(relevantDocuments);
//
//            Map<String, Object> result = new HashMap<>();
//            result.put("answer", answer);
//            result.put("sources", sources);
//
//            return Response.success(result);
//        } catch (Exception e) {
//            log.error("RAG聊天失败: ", e);
//            return Response.error("RAG聊天失败: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 准备文档内容
//     */
//    private String prepareDocumentsContent(List<Document> documents) {
//        return documents.stream()
//                .map(doc -> "---\n" + doc.text() + "\n---")
//                .collect(Collectors.joining("\n\n"));
//    }
//
//    /**
//     * 准备引用源
//     */
//    private List<Map<String, String>> prepareSourceReferences(List<Document> documents) {
//        return documents.stream()
//                .map(doc -> {
//                    Map<String, String> source = new HashMap<>();
//                    String sourceFile = doc.metadata().get("source") != null ?
//                            doc.metadata().get("source").toString() : "未知来源";
//                    source.put("title", sourceFile);
//                    source.put("content", doc.text().length() > 200 ?
//                            doc.text().substring(0, 200) + "..." : doc.text());
//                    return source;
//                })
//                .collect(Collectors.toList());
//    }
//}