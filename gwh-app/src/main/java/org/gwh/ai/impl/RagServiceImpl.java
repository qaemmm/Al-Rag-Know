package org.gwh.ai.impl;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import lombok.extern.slf4j.Slf4j;
import org.gwh.ai.DocumentService;
import org.gwh.ai.OllamaService;
import org.gwh.ai.RagService;
import org.gwh.config.AiConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RAG服务实现类
 */
@Slf4j
@Service
public class RagServiceImpl implements RagService {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private OllamaService ollamaService;

    @Autowired
    private AiConfig aiConfig;

    // 用于多轮对话的上下文管理
    private final Map<String, MessageWindowChatMemory> chatMemories = new HashMap<>();

    @Override
    public String chat(String message, String model, double temperature) {
        log.info("普通聊天: 消息={}, 模型={}, 温度={}", message, model, temperature);

        try {
            if ("deepseek".equalsIgnoreCase(model)) {
                return ollamaService.chatWithDeepseek(message, temperature);
            } else if ("chatglm".equalsIgnoreCase(model)) {
                return ollamaService.chatWithChatGLM(message, temperature);
            } else {
                // 使用Ollama API直接生成
                Map<String, Object> params = new HashMap<>();
                params.put("model", aiConfig.getOllama().getDefaultModel());
                params.put("prompt", message);
                params.put("temperature", temperature);
                params.put("stream", false);
                return ollamaService.generateText(params);
            }
        } catch (Exception e) {
            log.error("聊天失败", e);
            throw new RuntimeException("聊天失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> ragChat(String message, String knowledgeBaseId, String model, double temperature) {
        log.info("RAG聊天: 消息={}, 知识库={}, 模型={}, 温度={}", message, knowledgeBaseId, model, temperature);

        try {
            // 1. 检索相关文档
            List<Document> relevantDocs = documentService.retrieveRelevantDocuments(message, knowledgeBaseId, 5);
            log.info("找到 {} 个相关文档", relevantDocs.size());

            if (relevantDocs.isEmpty()) {
                // 如果没有找到相关文档，退化为普通聊天
                String answer = chat(message, model, temperature);
                Map<String, Object> result = new HashMap<>();
                result.put("answer", answer);
                result.put("sources", new ArrayList<>());
                return result;
            }

            // 2. 构建带有上下文的提示
            String context = relevantDocs.stream()
                    .map(Document::text)
                    .collect(Collectors.joining("\n\n"));

            String prompt = String.format(
                    "基于以下信息回答用户的问题。如果信息中没有相关内容，请说明无法从提供的信息中找到答案。\n\n" +
                            "信息:\n%s\n\n" +
                            "用户问题: %s",
                    context,
                    message
            );

            // 3. 使用模型生成回答
            String answer;
            if ("deepseek".equalsIgnoreCase(model)) {
                answer = ollamaService.chatWithDeepseek(prompt, temperature);
            } else if ("chatglm".equalsIgnoreCase(model)) {
                answer = ollamaService.chatWithChatGLM(prompt, temperature);
            } else {
                // 使用配置中默认的Ollama模型
                Map<String, Object> params = new HashMap<>();
                params.put("model", aiConfig.getOllama().getDefaultModel());
                params.put("prompt", prompt);
                params.put("temperature", temperature);
                params.put("stream", false);
                answer = ollamaService.generateText(params);
            }

            // 4. 构建带有引用来源的结果
            List<Map<String, String>> sources = new ArrayList<>();
            for (Document doc : relevantDocs) {
                Map<String, String> sourceInfo = new HashMap<>();
                sourceInfo.put("title", doc.metadata().get("source").toString());
                sourceInfo.put("content", doc.text().length() > 100 ? doc.text().substring(0, 100) + "..." : doc.text());
                sources.add(sourceInfo);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("answer", answer);
            result.put("sources", sources);

            return result;
        } catch (Exception e) {
            log.error("RAG聊天失败", e);
            throw new RuntimeException("RAG聊天失败: " + e.getMessage());
        }
    }
}