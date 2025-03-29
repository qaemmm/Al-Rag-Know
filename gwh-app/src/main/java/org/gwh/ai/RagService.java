package org.gwh.ai;

import java.util.Map;

/**
 * RAG服务接口
 * 提供检索增强生成相关服务
 */
public interface RagService {
    
    /**
     * 普通聊天，不使用知识库
     *
     * @param message 用户消息
     * @param model 使用的模型，如deepseek、chatglm等
     * @param temperature 温度参数
     * @return 生成的回复
     */
    String chat(String message, String model, double temperature);
    
    /**
     * 基于知识库的RAG聊天
     *
     * @param message 用户消息
     * @param knowledgeBaseId 知识库ID
     * @param model 使用的模型，如deepseek、chatglm等
     * @param temperature 温度参数
     * @return 包含回答和引用来源的结果
     */
    Map<String, Object> ragChat(String message, String knowledgeBaseId, String model, double temperature);
} 