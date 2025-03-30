package org.gwh.ai;

import dev.langchain4j.data.document.Document;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 文档服务接口
 * 处理文档加载、向量化和检索
 */
public interface DocumentService {
    
    /**
     * 处理文档
     * 解析文档、切分文本并存储向量
     *
     * @param file 上传的文件
     * @param knowledgeBase 知识库名称
     * @return 处理结果信息
     * @throws IOException 文件处理异常
     */
    Map<String, Object> processDocument(MultipartFile file, String knowledgeBase) throws IOException;
    
    /**
     * 处理结构化文档
     * 直接存储已解析的文档到向量库
     *
     * @param document 已解析的文档
     * @param knowledgeBase 知识库名称
     * @return 处理结果信息
     */
    Map<String, Object> processStructuredDocument(Document document, String knowledgeBase);
    
    /**
     * 检索相关文档
     * 根据查询文本检索最相关的文档片段
     *
     * @param query 查询文本
     * @param knowledgeBase 知识库名称
     * @param maxResults 最大结果数量
     * @return 相关文档列表
     */
    List<Document> retrieveRelevantDocuments(String query, String knowledgeBase, int maxResults);
    
    /**
     * 删除知识库文档
     * 从向量存储中删除指定知识库的所有文档
     *
     * @param knowledgeBase 知识库名称
     * @return 删除的文档数量
     */
    int deleteDocuments(String knowledgeBase);
} 