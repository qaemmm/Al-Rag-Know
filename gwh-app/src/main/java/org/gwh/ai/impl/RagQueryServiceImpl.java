package org.gwh.ai.impl;

import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.gwh.ai.RagQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import dev.langchain4j.model.chat.ChatLanguageModel;

/**
 * RAG查询服务实现类
 */
@Slf4j
@Service
public class RagQueryServiceImpl {

    @Autowired
    private ChatLanguageModel chatLanguageModel;
    
    /**
     * 创建RAG查询服务Bean
     * 使用LangChain4j的AiServices动态代理机制
     */
    @Bean
    public RagQueryService ragQueryService() {
        log.info("初始化RAG查询服务，使用模型: {}", chatLanguageModel.getClass().getSimpleName());
        return AiServices.builder(RagQueryService.class)
                .chatLanguageModel(chatLanguageModel)
                .build();
    }
} 