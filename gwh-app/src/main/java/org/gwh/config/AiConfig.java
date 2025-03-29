package org.gwh.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AI服务配置类
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai")
public class AiConfig {
    /**
     * Ollama配置
     */
    private OllamaProperties ollama = new OllamaProperties();
    
    /**
     * DeepSeek配置
     */
    private DeepseekProperties deepseek = new DeepseekProperties();
    
    /**
     * ChatGLM配置
     */
    private ChatglmProperties chatglm = new ChatglmProperties();
    
    /**
     * Ollama属性
     */
    @Data
    public static class OllamaProperties {
        /**
         * API基础URL
         */
        private String apiUrl = "http://localhost:11434/api/generate";
        
        /**
         * 默认模型
         */
        private String defaultModel = "deepseek-r1:1.5b";
        
        /**
         * 是否启用
         */
        private boolean enabled = true;
    }
    
    /**
     * DeepSeek属性
     */
    @Data
    public static class DeepseekProperties {
        /**
         * API基础URL
         */
        private String apiUrl = "https://api.deepseek.com/v1";
        
        /**
         * API密钥
         */
        private String apiKey = "sk-329ec42fccd1436e8cbd1814c9cf031a";

        private String embeddingModel = "deepseek-embed-v1";
        
        /**
         * 是否启用
         */
        private boolean enabled = true;
    }

    /**
     * chatglm
     */
    @Data
    public static class ChatglmProperties {
        /**
         * API基础URL
         */
        private String apiUrl = "https://open.bigmodel.cn/api/paas/v4/chat/completions";

        /**
         * API密钥
         */
        private String apiKey = "f93b8a992cd743c0a6d52949156a4372.wVJRhfe6KmOSbLzL";

        private String embeddingModel = "chatglm-embedding-v2";

        private boolean enabled = true;
    }
} 