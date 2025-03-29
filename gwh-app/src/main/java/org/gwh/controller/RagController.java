package org.gwh.controller;

import lombok.extern.slf4j.Slf4j;
import org.gwh.ai.RagService;
import org.gwh.config.AiConfig;
import org.gwh.entity.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * RAG接口控制器
 * 提供检索增强生成相关API
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/rag")
public class RagController {

    @Autowired
    private RagService ragService;
    
    @Autowired
    private AiConfig aiConfig;

    /**
     * 普通聊天接口 - 没有RAG
     */
    @PostMapping("/chat")
    public Response<?> chat(@RequestBody Map<String, Object> params) {
        try {
            log.info("普通聊天请求: {}", params);
            
            String message = (String) params.get("message");
            if (message == null || message.isEmpty()) {
                return Response.error("消息内容不能为空");
            }
            
            // 默认使用DeepSeek模型
            String model = aiConfig.getDeepseek().isEnabled() ? "deepseek" : 
                    (aiConfig.getChatglm().isEnabled() ? "chatglm" : "ollama");
            
            Double temperature = params.get("temperature") != null ? 
                    Double.parseDouble(params.get("temperature").toString()) : 0.7;
            
            String response = ragService.chat(message, model, temperature);
            return Response.success(response);
        } catch (Exception e) {
            log.error("聊天请求处理失败", e);
            return Response.error("聊天失败: " + e.getMessage());
        }
    }

    /**
     * RAG聊天接口 - 使用知识库进行问答
     */
    @PostMapping("/v2/chat")
    public Response<?> ragChat(@RequestBody Map<String, Object> params) {
        try {
            log.info("RAG聊天请求: {}", params);
            
            String message = (String) params.get("message");
            if (message == null || message.isEmpty()) {
                return Response.error("消息内容不能为空");
            }
            
            String knowledgeBaseId = (String) params.get("knowledgeBaseId");
            if (knowledgeBaseId == null || knowledgeBaseId.isEmpty()) {
                return Response.error("知识库ID不能为空");
            }

            // 从请求参数中获取模型参数，如果没有，则使用默认模型
            String model = params.containsKey("model") ? params.get("model").toString() : 
                    (aiConfig.getDeepseek().isEnabled() ? "deepseek" : 
                    (aiConfig.getChatglm().isEnabled() ? "chatglm" : "ollama"));
            
            log.info("使用模型: {}", model);
            
            Double temperature = params.get("temperature") != null ? 
                    Double.parseDouble(params.get("temperature").toString()) : 0.7;
            
            Map<String, Object> result = ragService.ragChat(message, knowledgeBaseId, model, temperature);
            return Response.success(result);
        } catch (Exception e) {
            log.error("RAG聊天请求处理失败", e);
            return Response.error("RAG聊天失败: " + e.getMessage());
        }
    }
} 