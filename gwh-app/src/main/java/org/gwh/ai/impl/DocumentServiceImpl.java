package org.gwh.ai.impl;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.parser.apache.poi.ApachePoiDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.gwh.ai.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import java.nio.charset.StandardCharsets;

/**
 * 文档服务实现类
 * 处理文档加载、分段、向量化和检索
 */
@Slf4j
@Service
public class DocumentServiceImpl implements DocumentService {

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;

    @Autowired
    private ApachePdfBoxDocumentParser pdfDocumentParser;

    @Autowired
    private ApachePoiDocumentParser officeDocumentParser;

    @Value("${document.chunk.size:1000}")
    private int chunkSize;

    @Value("${document.chunk.overlap:200}")
    private int chunkOverlap;

    @Value("${document.temp.dir:./temp}")
    private String tempDirPath;

    @Override
    public Map<String, Object> processDocument(MultipartFile file, String knowledgeBase) throws IOException {
        log.info("处理文档: {}, 知识库: {}", file.getOriginalFilename(), knowledgeBase);

        // 创建临时目录
        Path tempDir = Paths.get(tempDirPath);
        if (!Files.exists(tempDir)) {
            Files.createDirectories(tempDir);
        }

        // 保存文件到临时目录
        File tempFile = tempDir.resolve(file.getOriginalFilename()).toFile();
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(file.getBytes());
        }

        try {
            // 加载文档
            Document document = loadDocument(tempFile, Map.of(
                    "knowledge_base", knowledgeBase,
                    "source", file.getOriginalFilename(),
                    "timestamp", String.valueOf(System.currentTimeMillis())
            ));

            // 分割文档
            List<TextSegment> documentChunks = splitDocument(document);

            // 添加知识库元数据并转换为 TextSegment
            List<TextSegment> segments = new ArrayList<>();
            for (TextSegment chunk : documentChunks) {
                Metadata metadata = Metadata.from(Map.of(
                        "knowledge_base", knowledgeBase,
                        "source", file.getOriginalFilename(),
                        "timestamp", String.valueOf(System.currentTimeMillis())
                ));
                segments.add(TextSegment.from(chunk.text(), metadata));
            }

            // 向量化文档并存储
            for (TextSegment segment : segments) {
                embeddingStore.add(embeddingModel.embed(segment.text()).content(), segment);
            }

            // 返回处理结果
            Map<String, Object> result = new HashMap<>();
            result.put("filename", file.getOriginalFilename());
            result.put("chunks", segments.size());
            result.put("embeddings", segments.size());
            result.put("knowledgeBase", knowledgeBase);

            return result;
        } finally {
            // 清理临时文件
            tempFile.delete();
        }
    }

    @Override
    public List<Document> retrieveRelevantDocuments(String query, String knowledgeBase, int maxResults) {
        log.info("检索相关文档: 查询: {}, 知识库: {}, 最大结果数: {}", query, knowledgeBase, maxResults);

        // 向量化查询
        var queryEmbedding = embeddingModel.embed(query).content();

        // 获取所有相关文档
        List<EmbeddingMatch<TextSegment>> relevantMatches = embeddingStore.findRelevant(queryEmbedding, maxResults * 2);

        // 过滤指定知识库的结果
        List<Document> documents = relevantMatches.stream()
                .map(EmbeddingMatch::embedded)
                .filter(segment -> knowledgeBase.equals(segment.metadata().get("knowledge_base")))
                .limit(maxResults)
                .map(segment -> Document.from(segment.text(), segment.metadata()))
                .collect(Collectors.toList());

        log.info("找到 {} 个相关文档", documents.size());
        return documents;
    }

    @Override
    public int deleteDocuments(String knowledgeBase) {
        log.info("删除知识库文档: {}", knowledgeBase);

        // 获取所有相关文档
        var queryEmbedding = embeddingModel.embed("").content();
        List<EmbeddingMatch<TextSegment>> allDocuments = embeddingStore.findRelevant(queryEmbedding, Integer.MAX_VALUE);

        // 计算待删除的文档数量
        long count = allDocuments.stream()
                .map(EmbeddingMatch::embedded)
                .filter(segment -> knowledgeBase.equals(segment.metadata().get("knowledge_base")))
                .count();

        log.info("知识库 {} 中有 {} 个文档将在应用重启后删除", knowledgeBase, count);
        return (int) count;
    }

    /**
     * 加载文档
     */
    private Document loadDocument(File file, Map<String, String> metadata) {
        try {
            String content = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
            // 创建Metadata对象而不是尝试强制转换
            Metadata docMetadata = Metadata.from(metadata);
            return Document.from(content, docMetadata);
        } catch (IOException e) {
            log.error("读取文件失败: " + file.getName(), e);
            throw new RuntimeException("读取文件失败: " + e.getMessage());
        }
    }

    /**
     * 分割文档
     */
    private List<TextSegment> splitDocument(Document document) {
        DocumentSplitter splitter = DocumentSplitters.recursive(chunkSize, chunkOverlap);
        return splitter.split(document).stream()
                .map(doc -> TextSegment.from(doc.text(), doc.metadata()))
                .collect(Collectors.toList());
    }
}
