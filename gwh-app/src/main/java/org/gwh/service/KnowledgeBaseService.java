package org.gwh.service;

import org.gwh.entity.KnowledgeBase;

import java.util.List;

/**
 * 知识库服务接口
 */
public interface KnowledgeBaseService {

    /**
     * 根据ID获取知识库
     *
     * @param id 知识库ID
     * @return 知识库对象，如果不存在则返回null
     */
    KnowledgeBase getKnowledgeBaseById(String id);

    /**
     * 获取所有知识库
     *
     * @return 知识库列表
     */
    List<KnowledgeBase> getAllKnowledgeBases();

    /**
     * 保存知识库（新增或更新）
     *
     * @param knowledgeBase 知识库对象
     */
    void saveKnowledgeBase(KnowledgeBase knowledgeBase);

    /**
     * 删除知识库
     *
     * @param id 知识库ID
     */
    void deleteKnowledgeBase(String id);
} 