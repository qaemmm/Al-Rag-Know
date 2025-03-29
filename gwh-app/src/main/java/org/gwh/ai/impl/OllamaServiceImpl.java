package org.gwh.ai.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.gwh.ai.OllamaService;
import org.gwh.config.AiConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ollama服务实现类
 */
@Slf4j
@Service
public class OllamaServiceImpl implements OllamaService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private AiConfig aiConfig;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public String generateText(Map<String, Object> params) {
        try {
            // 获取Ollama配置
            String apiUrl = aiConfig.getOllama().getApiUrl();
            String model = params.containsKey("model") ? params.get("model").toString() : aiConfig.getOllama().getDefaultModel();
            
            log.info("请求Ollama模型: {}, URL: {}", model, apiUrl);
            
            // 设置HTTP头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // 发送请求
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(params, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    apiUrl, requestEntity, Map.class);
            
            // 解析响应
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("Ollama响应成功: {}", response.getBody().keySet());
                if (response.getBody().containsKey("response")) {
                    return response.getBody().get("response").toString();
                } else {
                    log.warn("Ollama响应缺少response字段: {}", response.getBody());
                    return "服务器返回了空响应";
                }
            } else {
                log.error("Ollama API响应错误: {}", response.getStatusCode());
                throw new RuntimeException("生成文本失败: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("调用Ollama API失败", e);
            throw new RuntimeException("调用Ollama API失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> listModels() {
        try {
            // 获取模型列表接口
            String apiUrl = aiConfig.getOllama().getApiUrl().replace("/generate", "/tags");
            
            // 设置HTTP头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // 发送请求
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl, HttpMethod.GET, requestEntity, Map.class);
            
            // 解析响应
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> models = (List<Map<String, Object>>) response.getBody().get("models");
                return models != null ? models : new ArrayList<>();
            } else {
                throw new RuntimeException("获取模型列表失败: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("获取Ollama模型列表失败", e);
            throw new RuntimeException("获取Ollama模型列表失败: " + e.getMessage());
        }
    }

    @Override
    public String chatWithDeepseek(String message, double temperature) {
        try {
            // 获取DeepSeek配置
            String apiUrl = aiConfig.getDeepseek().getApiUrl() + "/chat/completions";
            String apiKey = aiConfig.getDeepseek().getApiKey();
            
            // 设置HTTP头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "deepseek-chat");
            requestBody.put("temperature", temperature);
            
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "user", "content", message));
            requestBody.put("messages", messages);
            
            // 发送请求
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    apiUrl, requestEntity, Map.class);
            
            // 解析响应
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> choicesMap = (Map<String, Object>) ((List) response.getBody().get("choices")).get(0);
                Map<String, Object> messageMap = (Map<String, Object>) choicesMap.get("message");
                return messageMap.get("content").toString();
            } else {
                throw new RuntimeException("DeepSeek聊天失败: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("调用DeepSeek API失败", e);
            throw new RuntimeException("调用DeepSeek API失败: " + e.getMessage());
        }
    }

    @Override
    public String chatWithChatGLM(String message, double temperature) {
        try {
            // 获取ChatGLM配置
            String apiUrl = aiConfig.getChatglm().getApiUrl() ;
            String apiKey = aiConfig.getChatglm().getApiKey();
            
            // 设置HTTP头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "chatglm_turbo");
            requestBody.put("temperature", temperature);
            
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "user", "content", message));
            requestBody.put("messages", messages);
            
            // 发送请求
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    apiUrl, requestEntity, Map.class);
            
            // 解析响应
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> choicesMap = (Map<String, Object>) ((List) response.getBody().get("choices")).get(0);
                Map<String, Object> messageMap = (Map<String, Object>) choicesMap.get("message");
                return messageMap.get("content").toString();
            } else {
                throw new RuntimeException("ChatGLM聊天失败: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("调用ChatGLM API失败", e);
            throw new RuntimeException("调用ChatGLM API失败: " + e.getMessage());
        }
    }
} 