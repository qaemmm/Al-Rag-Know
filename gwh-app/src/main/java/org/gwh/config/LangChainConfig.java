package org.gwh.config;

import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.parser.apache.poi.ApachePoiDocumentParser;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.service.AiServiceContext;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * LangChain4j配置类
 * 配置文档处理器、嵌入模型和聊天模型
 */
@Slf4j
@Configuration
public class LangChainConfig {

    @Value("${ai.ollama.api-url:http://localhost:11434}")
    private String ollamaApiUrl;

    @Value("${ai.ollama.default-model:deepseek-r1:1.5b}")
    private String ollamaDefaultModel;

    @Value("${ai.ollama.timeout:60}")
    private int ollamaTimeoutSeconds;

    /**
     * 配置文本解析器
     */
    @Bean
    public TextDocumentParser textDocumentParser() {
        return new TextDocumentParser();
    }

    /**
     * 配置PDF解析器
     */
    @Bean
    public ApachePdfBoxDocumentParser pdfDocumentParser() {
        return new ApachePdfBoxDocumentParser();
    }

    /**
     * 配置Office文档解析器
     */
    @Bean
    public ApachePoiDocumentParser officeDocumentParser() {
        return new ApachePoiDocumentParser();
    }

    /**
     * 配置Ollama聊天模型
     */
    @Bean
    public ChatLanguageModel chatLanguageModel() {
        log.info("初始化Ollama聊天模型 - URL: {}, 模型: {}", ollamaApiUrl, ollamaDefaultModel);
        return OllamaChatModel.builder()
                .baseUrl(ollamaApiUrl)
                .modelName(ollamaDefaultModel)
                .timeout(Duration.ofSeconds(ollamaTimeoutSeconds))
                .temperature(0.7)
                .build();
    }

    /**
     * 配置Ollama嵌入模型
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        log.info("初始化Ollama嵌入模型 - URL: {}, 模型: {}", ollamaApiUrl, ollamaDefaultModel);
        return OllamaEmbeddingModel.builder()
                .baseUrl(ollamaApiUrl)
                .modelName(ollamaDefaultModel)
                .timeout(Duration.ofSeconds(ollamaTimeoutSeconds))
                .build();
    }

    /**
     * 配置内存向量存储
     * 注意：这只适用于开发环境，生产环境应使用持久化的向量存储
     */
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        log.info("初始化内存嵌入存储（仅用于开发环境）");
        return new InMemoryEmbeddingStore<>();
    }
}