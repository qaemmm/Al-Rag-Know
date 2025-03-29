package org.gwh.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.gwh.entity.KnowledgeBase;
import org.gwh.service.KnowledgeBaseService;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识库服务实现类
 * 使用Redis作为存储
 */
@Slf4j
@Service
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private static final String KNOWLEDGE_BASE_MAP_KEY = "knowledgeBase";
    
    @Autowired
    private RedissonClient redissonClient;

    @Override
    public KnowledgeBase getKnowledgeBaseById(String id) {
        log.debug("获取知识库: {}", id);
        RMap<String, KnowledgeBase> map = redissonClient.getMap(KNOWLEDGE_BASE_MAP_KEY);
        return map.get(id);
    }

    @Override
    public List<KnowledgeBase> getAllKnowledgeBases() {
        log.debug("获取所有知识库");
        RMap<String, KnowledgeBase> map = redissonClient.getMap(KNOWLEDGE_BASE_MAP_KEY);
        return new ArrayList<>(map.values());
    }

    @Override
    public void saveKnowledgeBase(KnowledgeBase knowledgeBase) {
        log.debug("保存知识库: {}", knowledgeBase.getId());
        RMap<String, KnowledgeBase> map = redissonClient.getMap(KNOWLEDGE_BASE_MAP_KEY);
        map.put(knowledgeBase.getId(), knowledgeBase);
    }

    @Override
    public void deleteKnowledgeBase(String id) {
        log.debug("删除知识库: {}", id);
        RMap<String, KnowledgeBase> map = redissonClient.getMap(KNOWLEDGE_BASE_MAP_KEY);
        map.remove(id);
    }
} 