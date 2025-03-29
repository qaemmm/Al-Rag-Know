package org.gwh.controller;

import lombok.extern.slf4j.Slf4j;
import org.gwh.entity.KnowledgeBase;
import org.gwh.entity.Response;
import org.gwh.service.KnowledgeBaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 知识库控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/rag/knowledge")
public class KnowledgeBaseController {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;
    
    /**
     * 获取知识库列表
     */
    @GetMapping("/list")
    public Response<List<KnowledgeBase>> listKnowledgeBases() {
        try {
            return Response.success(knowledgeBaseService.getAllKnowledgeBases());
        } catch (Exception e) {
            log.error("获取知识库列表失败", e);
            return Response.error("获取知识库列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 创建知识库
     */
    @PostMapping("/create")
    public Response<KnowledgeBase> createKnowledgeBase(@RequestBody KnowledgeBase knowledgeBase) {
        try {
            // 验证请求数据
            if (knowledgeBase.getName() == null || knowledgeBase.getName().trim().isEmpty()) {
                return Response.error("知识库名称不能为空");
            }
            
            // 设置ID和时间
            knowledgeBase.setId(UUID.randomUUID().toString());
            knowledgeBase.setCreateTime(new Date());
            knowledgeBase.setUpdateTime(new Date());
            
            // 设置默认支持的文件类型
            if (knowledgeBase.getFileTypes() == null || knowledgeBase.getFileTypes().isEmpty()) {
                knowledgeBase.setFileTypes(Arrays.asList("pdf", "doc", "docx", "txt", "md"));
            }
            
            // 保存知识库
            knowledgeBaseService.saveKnowledgeBase(knowledgeBase);
            
            return Response.success(knowledgeBase);
        } catch (Exception e) {
            log.error("创建知识库失败", e);
            return Response.error("创建知识库失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新知识库
     */
    @PostMapping("/update")
    public Response<KnowledgeBase> updateKnowledgeBase(@RequestBody KnowledgeBase knowledgeBase) {
        try {
            // 验证请求数据
            if (knowledgeBase.getId() == null || knowledgeBase.getId().trim().isEmpty()) {
                return Response.error("知识库ID不能为空");
            }
            
            if (knowledgeBase.getName() == null || knowledgeBase.getName().trim().isEmpty()) {
                return Response.error("知识库名称不能为空");
            }
            
            // 检查知识库是否存在
            KnowledgeBase existingKnowledgeBase = knowledgeBaseService.getKnowledgeBaseById(knowledgeBase.getId());
            if (existingKnowledgeBase == null) {
                return Response.error("知识库不存在");
            }
            
            // 更新时间
            knowledgeBase.setUpdateTime(new Date());
            
            // 保留创建时间
            knowledgeBase.setCreateTime(existingKnowledgeBase.getCreateTime());
            
            // 保存知识库
            knowledgeBaseService.saveKnowledgeBase(knowledgeBase);
            
            return Response.success(knowledgeBase);
        } catch (Exception e) {
            log.error("更新知识库失败", e);
            return Response.error("更新知识库失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除知识库
     */
    @PostMapping("/delete")
    public Response<String> deleteKnowledgeBase(@RequestBody Map<String, String> params) {
        try {
            String id = params.get("id");
            
            // 验证请求数据
            if (id == null || id.trim().isEmpty()) {
                return Response.error("知识库ID不能为空");
            }
            
            // 检查知识库是否存在
            KnowledgeBase existingKnowledgeBase = knowledgeBaseService.getKnowledgeBaseById(id);
            if (existingKnowledgeBase == null) {
                return Response.error("知识库不存在");
            }
            
            // 删除知识库
            knowledgeBaseService.deleteKnowledgeBase(id);
            
            return Response.success("删除成功");
        } catch (Exception e) {
            log.error("删除知识库失败", e);
            return Response.error("删除知识库失败: " + e.getMessage());
        }
    }
} 