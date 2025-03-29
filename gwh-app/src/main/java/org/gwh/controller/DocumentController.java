package org.gwh.controller;

import lombok.extern.slf4j.Slf4j;
import org.gwh.ai.DocumentService;
import org.gwh.entity.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 文档管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/document")
public class DocumentController {

    @Autowired
    private DocumentService documentService;
    
    /**
     * 上传文件到知识库
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Response<?> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("knowledgeBaseId") String knowledgeBaseId) {
        try {
            log.info("接收文件上传请求: fileName={}, size={}, knowledgeBaseId={}", 
                    file.getOriginalFilename(), file.getSize(), knowledgeBaseId);

            if (file.isEmpty()) {
                return Response.error("上传文件为空");
            }

            // 保存文件并处理
            Map<String, Object> result = documentService.processDocument(file, knowledgeBaseId);
            
            log.info("文件上传成功: {}", result);
            return Response.success(result);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            return Response.error("文件上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 从知识库中删除所有文档
     */
    @PostMapping("/delete")
    public Response<Integer> deleteDocuments(@RequestBody Map<String, String> params) {
        try {
            String knowledgeBaseId = params.get("knowledgeBaseId");
            if (knowledgeBaseId == null || knowledgeBaseId.isEmpty()) {
                return Response.error("知识库ID不能为空");
            }
            
            // 执行删除操作
            return Response.success(documentService.deleteDocuments(knowledgeBaseId));
        } catch (Exception e) {
            log.error("删除文档失败", e);
            return Response.error("删除文档失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        return (lastDotIndex > 0) ? fileName.substring(lastDotIndex + 1).toLowerCase() : "";
    }
} 