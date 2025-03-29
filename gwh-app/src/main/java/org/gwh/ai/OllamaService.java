package org.gwh.ai;

import java.util.List;
import java.util.Map;

/**
 * Ollama服务接口
 * 提供与Ollama和其他AI模型交互的方法
 */
public interface OllamaService {
    
    /**
     * 生成文本
     * @param params 请求参数
     * @return 生成的文本
     */
    String generateText(Map<String, Object> params);
    
    /**
     * 列出可用模型
     * @return 模型列表
     */
    List<Map<String, Object>> listModels();
    
    /**
     * 使用DeepSeek进行聊天
     * @param message 用户消息
     * @param temperature 温度参数
     * @return 生成的回复
     */
    String chatWithDeepseek(String message, double temperature);
    
    /**
     * 使用ChatGLM进行聊天
     * @param message 用户消息
     * @param temperature 温度参数
     * @return 生成的回复
     */
    String chatWithChatGLM(String message, double temperature);
} 