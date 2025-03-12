package org.gwh.trigger.http.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.gwh.api.IGitService;
import org.gwh.api.response.Response;
import org.redisson.api.RList;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.core.io.PathResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;


@Slf4j
@Service
public class GitService implements IGitService {

    @Resource
    private PgVectorStore pgVectorStore;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private TokenTextSplitter tokenTextSplitter;

    // 使用线程池处理文件解析
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    // 使用Redis存储任务进度
    private static final String TASK_PROGRESS_KEY = "git:analysis:progress:";

    @Async
    public CompletableFuture<Response<String>> analyzeGitRepositoryAsync(String repoUrl, String userName, String token) {
        String taskId = UUID.randomUUID().toString();
        String progressKey = TASK_PROGRESS_KEY + taskId;
        RMap<String, Object> progressMap = redissonClient.getMap(progressKey);

        return CompletableFuture.supplyAsync(() -> {
            try {
                // 更新任务状态为进行中
                updateProgress(progressMap, "PROCESSING", 0, "Started repository analysis");
                log.info("Started repository analysis  0%");
                String localPath = "./git-cloned-repo-" + taskId;
                String repoProjectName = extractProjectName(repoUrl);
                File localDir = new File(localPath);

                // 克隆仓库
                updateProgress(progressMap, "PROCESSING", 10, "Cloning repository...");
                Git git = cloneRepository(repoUrl, userName, token, localDir);
                log.info("Started repository analysis  10%");
                // 获取所有文件路径
                List<Path> allFiles = new ArrayList<>();
                Files.walkFileTree(Paths.get(localPath), new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        allFiles.add(file);
                        return FileVisitResult.CONTINUE;
                    }
                });

                // 创建批处理任务
                int totalFiles = allFiles.size();
                AtomicInteger processedFiles = new AtomicInteger();
                List<CompletableFuture<Void>> futures = new ArrayList<>();

                // 批量处理文件
                for (Path file : allFiles) {
                    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        processFile(file, repoProjectName);
                    }, executorService).whenComplete((result, ex) -> {
                        // 更新进度
                        int currentProgress = (int) ((processedFiles.incrementAndGet() / (double) totalFiles) * 80) + 10;
                        updateProgress(progressMap, "PROCESSING", currentProgress,
                                String.format("Processed %d/%d files", processedFiles, totalFiles));
                    });
                    futures.add(future);
                }

                // 等待所有文件处理完成
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

                // 更新知识库标签
                updateProgress(progressMap, "PROCESSING", 90, "Updating knowledge base tags...");
                RList<String> elements = redissonClient.getList("ragTag");
                if (!elements.contains(repoProjectName)) {
                    elements.add(repoProjectName);
                }

                // 清理资源
                updateProgress(progressMap, "PROCESSING", 95, "Cleaning up...");
                git.close();
                FileUtils.deleteDirectory(localDir);

                // 完成
                updateProgress(progressMap, "COMPLETED", 100, "Analysis completed successfully");

                return Response.<String>builder()
                        .code("0000")
                        .info("Analysis completed successfully")
                        .data(taskId)  // 返回taskId供前端查询进度
                        .build();

            } catch (Exception e) {
                log.error("Repository analysis failed", e);
                updateProgress(progressMap, "FAILED", -1, "Analysis failed: " + e.getMessage());
                throw new RuntimeException("Repository analysis failed", e);
            }
        });
    }

    private void processFile(Path file, String repoProjectName) {
        try {
            TikaDocumentReader reader = new TikaDocumentReader(new PathResource(file));
            List<Document> documents = reader.get();
            List<Document> documentSplitterList = tokenTextSplitter.apply(documents);

            // 批量设置元数据
            Stream.concat(documents.stream(), documentSplitterList.stream())
                    .forEach(doc -> doc.getMetadata().put("knowledge", repoProjectName));

            // 批量保存到向量存储
            pgVectorStore.accept(documentSplitterList);

        } catch (Exception e) {
            log.error("Failed to process file: {}", file, e);
        }
    }

    private Git cloneRepository(String repoUrl, String userName, String token, File directory) throws Exception {
        return Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(directory)
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(userName, token))
                .setTimeout(60)
                .call();
    }

    private void updateProgress(RMap<String, Object> progressMap, String status, int progress, String message) {
        progressMap.put("status", status);
        progressMap.put("progress", progress);
        progressMap.put("message", message);
        progressMap.put("updateTime", System.currentTimeMillis());
    }

    // 提供进度查询接口
    public Map<String, Object> getProgress(String taskId) {
        RMap<String, Object> progressMap = redissonClient.getMap(TASK_PROGRESS_KEY + taskId);
        return new HashMap<>(progressMap.readAllMap());
    }

    private String extractProjectName(String repoUrl) {
        String[] parts = repoUrl.split("/");
        String projectNameWithGit = parts[parts.length - 1];
        return projectNameWithGit.replace(".git", "");
    }
}
