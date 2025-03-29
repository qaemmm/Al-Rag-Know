package org.gwh.controller;

import lombok.extern.slf4j.Slf4j;
import org.gwh.ai.OllamaService;
import org.gwh.config.AiConfig;
import org.gwh.entity.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Ollama API控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai")
public class OllamaController {

    @Autowired
    private OllamaService ollamaService;

    @Autowired
    private AiConfig aiConfig;

    /**
     * 直接生成文本
     */
    @PostMapping("/ollama/direct_generate")
    public Response<String> directGenerate(@RequestBody Map<String, Object> params) {
        try {
            if (!aiConfig.getOllama().isEnabled()) {
                return Response.error("Ollama服务未启用");
            }

            String text = ollamaService.generateText(params);
            return Response.success(text);
        } catch (Exception e) {
            log.error("Ollama生成文本失败", e);
            return Response.error("生成失败: " + e.getMessage());
        }
    }

    /**
     * 获取支持的模型列表
     */
    @GetMapping("/ollama/models")
    public Response<?> getModels() {
        try {
            if (!aiConfig.getOllama().isEnabled()) {
                return Response.error("Ollama服务未启用");
            }

            return Response.success(ollamaService.listModels());
        } catch (Exception e) {
            log.error("获取Ollama模型列表失败", e);
            return Response.error("获取失败: " + e.getMessage());
        }
    }

    /**
     * DeepSeek聊天接口
     */
    @PostMapping("/deepseek/chat")
    public Response<String> deepseekChat(@RequestBody Map<String, Object> params) {
        try {
            if (!aiConfig.getDeepseek().isEnabled()) {
                return Response.error("DeepSeek服务未启用");
            }

            String message = (String) params.get("message");
            if (message == null || message.isEmpty()) {
                return Response.error("消息内容不能为空");
            }

            Double temperature = params.get("temperature") != null ? 
                    Double.parseDouble(params.get("temperature").toString()) : 0.7;

            String response = ollamaService.chatWithDeepseek(message, temperature);
            return Response.success(response);
        } catch (Exception e) {
            log.error("DeepSeek聊天失败", e);
            return Response.error("聊天失败: " + e.getMessage());
        }
    }

    /**
     * ChatGLM聊天接口
     */
    @PostMapping("/chatglm/chat")
    public Response<String> chatglmChat(@RequestBody Map<String, Object> params) {
        try {
            if (!aiConfig.getChatglm().isEnabled()) {
                return Response.error("ChatGLM服务未启用");
            }

            String message = (String) params.get("message");
            if (message == null || message.isEmpty()) {
                return Response.error("消息内容不能为空");
            }

            Double temperature = params.get("temperature") != null ? 
                    Double.parseDouble(params.get("temperature").toString()) : 0.7;

            String response = ollamaService.chatWithChatGLM(message, temperature);
            return Response.success(response);
        } catch (Exception e) {
            log.error("ChatGLM聊天失败", e);
            return Response.error("聊天失败: " + e.getMessage());
        }
    }
} 