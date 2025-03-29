package org.gwh.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * 知识库实体类
 */
@Data
public class KnowledgeBase {
    /**
     * 知识库ID
     */
    private String id;
    
    /**
     * 知识库名称
     */
    private String name;
    
    /**
     * 知识库描述
     */
    private String description;
    
    /**
     * 支持的文件类型
     */
    private List<String> fileTypes;
    
    /**
     * 创建时间
     */
    private Date createTime;
    
    /**
     * 更新时间
     */
    private Date updateTime;
} 