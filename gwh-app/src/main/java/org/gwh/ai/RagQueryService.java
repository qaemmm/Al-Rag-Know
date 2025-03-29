package org.gwh.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * RAG查询服务接口
 * 使用LangChain4j的AiService注解实现自动代理
 */
public interface RagQueryService {

    /**
     * 普通聊天
     * 不使用知识库，直接与LLM对话
     */
    @SystemMessage("你是一个智能助手，请用中文回答用户的问题。")
    String chat(@UserMessage String message);
    
    /**
     * 基于RAG的聊天
     * 使用提供的文档内容作为上下文，回答用户的问题
     */
    @SystemMessage("""
        你是一个专业的RAG问答助手，请依据以下提供的文档内容用中文回答用户的问题。
        如果问题无法从文档中得到答案，请明确告知用户并提供可能的建议。
        保持回答严谨、清晰和有条理，避免编造不存在的信息。
        仅根据提供的文档内容回答，不添加其他来源的信息。
        """)
    String chatWithRAG(@UserMessage("问题：") String userQuestion, @UserMessage("参考文档：") String documentsContent);
} 