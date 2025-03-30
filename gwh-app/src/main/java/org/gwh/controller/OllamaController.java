package org.gwh.controller;

import lombok.extern.slf4j.Slf4j;
import org.gwh.ai.OllamaService;
import org.gwh.ai.DocumentService;
import org.gwh.config.AiConfig;
import org.gwh.entity.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.Okio;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;

/**
 * Ollama API控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai")
public class OllamaController {

    @Autowired
    private OllamaService ollamaService;

    @Autowired
    private AiConfig aiConfig;
    
    @Autowired
    private DocumentService documentService;
    
    @Autowired
    private RedissonClient redissonClient;

    /**
     * 直接生成文本
     */
    @PostMapping("/ollama/direct_generate")
    public Response<String> directGenerate(@RequestBody Map<String, Object> params) {
        try {
            if (!aiConfig.getOllama().isEnabled()) {
                return Response.error("Ollama服务未启用");
            }

            String text = ollamaService.generateText(params);
            return Response.success(text);
        } catch (Exception e) {
            log.error("Ollama生成文本失败", e);
            return Response.error("生成失败: " + e.getMessage());
        }
    }

    /**
     * 获取支持的模型列表
     */
    @GetMapping("/ollama/models")
    public Response<?> getModels() {
        try {
            if (!aiConfig.getOllama().isEnabled()) {
                return Response.error("Ollama服务未启用");
            }

            return Response.success(ollamaService.listModels());
        } catch (Exception e) {
            log.error("获取Ollama模型列表失败", e);
            return Response.error("获取失败: " + e.getMessage());
        }
    }
    
    /**
     * 分析GitHub仓库并上传到知识库（使用GitHub API）
     */
    @PostMapping("/ollama/analyze_github_repository")
    public Response<String> analyzeGithubRepository(
            @RequestParam("repoOwner") String repoOwner,
            @RequestParam("repoName") String repoName,
            @RequestParam("token") String token,
            @RequestParam("knowledgeBaseId") String knowledgeBaseId) {
        
        // 使用系统临时目录创建工作目录
        File tempDirFile = new File(System.getProperty("java.io.tmpdir"), "github-temp-" + System.currentTimeMillis());
        File extractDirFile = new File(System.getProperty("java.io.tmpdir"), "github-extract-" + System.currentTimeMillis());
        
        String repoProjectName = repoName;
        log.info("开始从GitHub获取仓库: {}/{}", repoOwner, repoName);
        
        try {
            // 清理和创建临时目录
            FileUtils.deleteDirectory(tempDirFile);
            FileUtils.deleteDirectory(extractDirFile);
            tempDirFile.mkdirs();
            extractDirFile.mkdirs();
            
            // 构建GitHub API URL获取主分支压缩包
            String apiUrl = String.format(
                "https://api.github.com/repos/%s/%s/zipball/main", 
                repoOwner, repoName
            );
            
            // 配置HTTP请求
            OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(120, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build();
                
            Request request = new Request.Builder()
                .url(apiUrl)
                .header("Authorization", "token " + token)
                .header("Accept", "application/vnd.github.v3+json")
                .build();
                
            log.info("调用GitHub API下载仓库: {}", apiUrl);
            
            // 执行请求并下载ZIP
            okhttp3.Response httpResponse = client.newCall(request).execute();
            if (!httpResponse.isSuccessful()) {
                return Response.error("GitHub API调用失败: " + httpResponse.code() + " - " + httpResponse.message());
            }
            
            // 保存为ZIP文件
            File zipFile = new File(tempDirFile, repoName + ".zip");
            try (okhttp3.ResponseBody body = httpResponse.body()) {
                if (body == null) {
                    return Response.error("GitHub API调用失败: 响应体为空");
                }
                
                try (BufferedSink sink = Okio.buffer(Okio.sink(zipFile))) {
                    sink.writeAll(body.source());
                }
            }
            
            log.info("GitHub仓库ZIP包下载成功: {}, 大小: {} 字节", zipFile.getAbsolutePath(), zipFile.length());
            
            // 解压ZIP文件
            unzipFile(zipFile, extractDirFile.getAbsolutePath());
            log.info("ZIP文件解压完成: {}", extractDirFile.getAbsolutePath());
            
            // 处理解压后的文件
            processExtractedRepository(extractDirFile, repoProjectName, knowledgeBaseId);
            
            // 清理临时文件
            FileUtils.deleteDirectory(tempDirFile);
            FileUtils.deleteDirectory(extractDirFile);
            
            return Response.success("GitHub仓库分析上传成功");
        } catch (Exception e) {
            log.error("通过GitHub API分析仓库失败", e);
            return Response.error("分析仓库失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理解压后的代码仓库
     */
    private void processExtractedRepository(File extractDir, String repoName, String knowledgeBaseId) throws IOException {
        log.info("开始处理解压后的代码仓库: {}", extractDir.getAbsolutePath());
        
        // 查找第一级目录(GitHub ZIP通常包含一个根目录)
        File[] rootDirs = extractDir.listFiles(File::isDirectory);
        File rootDir = rootDirs != null && rootDirs.length > 0 ? rootDirs[0] : extractDir;
        
        // 遍历仓库文件
        Files.walkFileTree(rootDir.toPath(), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String fileName = file.getFileName().toString();
                
                // 跳过隐藏文件和二进制文件
                if (fileName.startsWith(".") || isBinaryFile(fileName)) {
                    return FileVisitResult.CONTINUE;
                }
                
                try {
                    // 获取相对路径
                    Path relativePath = rootDir.toPath().relativize(file);
                    log.info("处理代码文件: {} ({})", fileName, relativePath);
                    
                    // 根据文件类型处理
                    if (fileName.endsWith(".java")) {
                        // 结构化处理Java文件
                        List<Document> javaDocuments = processJavaFile(file.toFile(), repoName, relativePath.toString());
                        for (Document doc : javaDocuments) {
                            documentService.processStructuredDocument(doc, knowledgeBaseId);
                        }
                    } else {
                        // 处理其他文本文件
                        MultipartFile multipartFile = new FileBasedMultipartFile(file.toFile());
                        documentService.processDocument(multipartFile, knowledgeBaseId);
                    }
                } catch (Exception e) {
                    log.error("处理文件失败: {}", file.getFileName(), e);
                }

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                log.info("Failed to access file: {} - {}", file.toString(), exc.getMessage());
                return FileVisitResult.CONTINUE;
            }
        });
        
        // 添加到知识库列表
        RList<String> elements = redissonClient.getList("ragTag");
        if (!elements.contains(knowledgeBaseId)) {
            elements.add(knowledgeBaseId);
        }
        
        log.info("代码仓库处理完成: {}", repoName);
    }
    
    /**
     * 结构化处理Java文件
     */
    private List<Document> processJavaFile(File file, String repoName, String relativePath) {
        List<Document> documents = new ArrayList<>();
        
        try {
            String content = Files.readString(file.toPath());
            
            // 使用JavaParser解析Java文件
            CompilationUnit cu = StaticJavaParser.parse(content);
            
            // 文件级别的文档
            Map<String, String> fileMetadata = new HashMap<>();
            fileMetadata.put("repo", repoName);
            fileMetadata.put("file", relativePath);
            fileMetadata.put("language", "java");
            fileMetadata.put("type", "file");
            
            Document fileDoc = Document.from(content, Metadata.from(fileMetadata));
            documents.add(fileDoc);
            
            // 提取类、方法等
            for (TypeDeclaration<?> type : cu.getTypes()) {
                // 每个类一个文档
                Map<String, String> classMetadata = new HashMap<>();
                classMetadata.put("repo", repoName);
                classMetadata.put("file", relativePath);
                classMetadata.put("language", "java");
                classMetadata.put("type", "class");
                classMetadata.put("name", type.getNameAsString());
                
                Document classDoc = Document.from(
                    type.toString(),
                    Metadata.from(classMetadata)
                );
                documents.add(classDoc);
                
                // 每个方法一个文档
                for (MethodDeclaration method : type.getMethods()) {
                    Map<String, String> methodMetadata = new HashMap<>();
                    methodMetadata.put("repo", repoName);
                    methodMetadata.put("file", relativePath);
                    methodMetadata.put("language", "java");
                    methodMetadata.put("type", "method");
                    methodMetadata.put("class", type.getNameAsString());
                    methodMetadata.put("name", method.getNameAsString());
                    
                    Document methodDoc = Document.from(
                        method.toString(),
                        Metadata.from(methodMetadata)
                    );
                    documents.add(methodDoc);
                }
            }
        } catch (Exception e) {
            log.error("解析Java文件失败: {}", file.getName(), e);
            
            // 如果解析失败，至少创建一个文件级别的文档
            try {
                String content = Files.readString(file.toPath());
                Map<String, String> metadata = new HashMap<>();
                metadata.put("repo", repoName);
                metadata.put("file", relativePath);
                metadata.put("language", "java");
                metadata.put("type", "file");
                
                Document doc = Document.from(content, Metadata.from(metadata));
                documents.add(doc);
            } catch (IOException ioe) {
                log.error("读取Java文件失败: {}", file.getName(), ioe);
            }
        }
        
        return documents;
    }
    
    /**
     * 分析Git仓库并上传到知识库
     */
    @RequestMapping(value = "/ollama/analyze_git_repository", method = RequestMethod.POST)
    public Response<String> analyzeGitRepository(@RequestParam("repoUrl") String repoUrl, 
                                                @RequestParam("userName") String userName, 
                                                @RequestParam("token") String token) {
        // 使用系统临时目录
        File localPathFile = new File(System.getProperty("java.io.tmpdir"), "git-cloned-repo-" + System.currentTimeMillis());
        File tempDirFile = new File(System.getProperty("java.io.tmpdir"), "git-temp-files-" + System.currentTimeMillis());
        
        String localPath = localPathFile.getAbsolutePath();
        String tempDir = tempDirFile.getAbsolutePath();
        String repoProjectName = extractProjectName(repoUrl);
        log.info("克隆路径：{}", localPath);

        try {
            // 删除可能存在的旧目录
            FileUtils.deleteDirectory(localPathFile);
            FileUtils.deleteDirectory(tempDirFile);
            
            // 创建临时目录
            localPathFile.mkdirs();
            tempDirFile.mkdirs();

            // 增加超时设置和重试机制
            int maxRetries = 3;
            Git git = null;
            boolean cloneSuccess = false;
            
            for (int i = 0; i < maxRetries; i++) {
                try {
                    log.info("尝试克隆Git仓库 (尝试 {}/{}): {}", (i+1), maxRetries, repoUrl);
                    git = Git.cloneRepository()
                            .setURI(repoUrl)
                            .setDirectory(localPathFile)
                            .setCredentialsProvider(new UsernamePasswordCredentialsProvider(userName, token))
                            .setTimeout(600) // 设置超时时间为10分钟
                            .setCloneAllBranches(false) // 只克隆主分支
                            .setNoCheckout(false) // 必须检出文件才能访问
                            .setTransportConfigCallback(getTransportConfigCallback())
                            .call();
                    
                    cloneSuccess = true;
                    log.info("Git仓库克隆成功: {}", repoUrl);
                    break;
                } catch (Exception e) {
                    log.warn("尝试 {} 克隆失败: {}", (i+1), e.getMessage());
                    if (i == maxRetries - 1) {
                        throw new Exception("Git仓库克隆失败，已重试" + maxRetries + "次: " + e.getMessage());
                    }
                    // 等待一段时间后重试
                    Thread.sleep(5000);
                    // 确保目录干净
                    FileUtils.deleteDirectory(localPathFile);
                    localPathFile.mkdirs();
                }
            }
            
            if (!cloneSuccess || git == null) {
                return Response.error("Git仓库克隆失败，请稍后重试");
            }

            try {
                // 遍历仓库中的文件
                Files.walkFileTree(localPathFile.toPath(), new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        String fileName = file.getFileName().toString();
                        
                        // 跳过隐藏文件和二进制文件
                        if (fileName.startsWith(".") || isBinaryFile(fileName)) {
                            return FileVisitResult.CONTINUE;
                        }
                        
                        try {
                            // 获取相对路径
                            Path relativePath = localPathFile.toPath().relativize(file);
                            log.info("{} 遍历解析路径，准备上传到知识库: {} ({})", repoProjectName, fileName, relativePath);
                            
                            // 根据文件类型处理
                            if (fileName.endsWith(".java")) {
                                // 结构化处理Java文件
                                List<Document> javaDocuments = processJavaFile(file.toFile(), repoProjectName, relativePath.toString());
                                for (Document doc : javaDocuments) {
                                    documentService.processStructuredDocument(doc, repoProjectName);
                                }
                            } else {
                                // 处理其他文本文件
                                File tempFile = new File(tempDirFile, fileName);
                                Files.copy(file, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                                MultipartFile multipartFile = new FileBasedMultipartFile(tempFile);
                                documentService.processDocument(multipartFile, repoProjectName);
                            }
                        } catch (Exception e) {
                            log.error("处理文件失败: {}", file.getFileName(), e);
                        }

                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                        log.info("Failed to access file: {} - {}", file.toString(), exc.getMessage());
                        return FileVisitResult.CONTINUE;
                    }
                });

                // 添加到知识库列表
                RList<String> elements = redissonClient.getList("ragTag");
                if (!elements.contains(repoProjectName)) {
                    elements.add(repoProjectName);
                }

                log.info("Git仓库解析上传完成: {}", repoUrl);
                
                return Response.success("Git仓库解析上传成功");
            } finally {
                if (git != null) {
                    try {
                        git.close();
                    } catch (Exception e) {
                        log.warn("关闭Git资源失败", e);
                    }
                }
                
                // 清理临时目录
                try {
                    FileUtils.deleteDirectory(localPathFile);
                    FileUtils.deleteDirectory(tempDirFile);
                } catch (Exception e) {
                    log.warn("清理临时目录失败", e);
                }
            }
        } catch (Exception e) {
            log.error("分析Git仓库失败", e);
            return Response.error("分析Git仓库失败: " + e.getMessage());
        }
    }
    
    /**
     * 配置传输回调，可用于设置代理等
     */
    private TransportConfigCallback getTransportConfigCallback() {
        return transport -> {
            // 可以在这里设置代理等其他传输配置
            // 例如：
            // if (transport instanceof TransportHttp) {
            //     ((TransportHttp) transport).setProxy(
            //         new Proxy(Proxy.Type.HTTP, new InetSocketAddress("your-proxy-server.com", 8080))
            //     );
            // }
        };
    }
    
    /**
     * 处理本地ZIP代码库
     */
    @PostMapping("/ollama/upload_git_zip")
    public Response<String> uploadGitZip(@RequestParam("file") MultipartFile file,
                                                @RequestParam("knowledgeBaseId") String knowledgeBaseId) {
        if (file.isEmpty()) {
            return Response.error("请选择要上传的ZIP文件");
        }
        
        // 使用系统临时目录
        File tempDirFile = new File(System.getProperty("java.io.tmpdir"), "git-zip-temp-" + System.currentTimeMillis());
        File extractDirFile = new File(System.getProperty("java.io.tmpdir"), "git-zip-extract-" + System.currentTimeMillis());
        
        try {
            // 确保目录存在
            tempDirFile.mkdirs();
            extractDirFile.mkdirs();
            
            // 保存上传的ZIP文件
            File zipFile = new File(tempDirFile, file.getOriginalFilename());
            file.transferTo(zipFile);
            
            log.info("ZIP文件已保存: {}, 大小: {} 字节", zipFile.getAbsolutePath(), zipFile.length());
            
            // 解压ZIP文件
            unzipFile(zipFile, extractDirFile.getAbsolutePath());
            log.info("ZIP文件解压完成: {}", extractDirFile.getAbsolutePath());
            
            // 处理解压后的文件
            processExtractedRepository(extractDirFile, knowledgeBaseId, knowledgeBaseId);
            
            // 清理临时文件
            FileUtils.deleteDirectory(tempDirFile);
            FileUtils.deleteDirectory(extractDirFile);
            
            return Response.success("本地代码库上传成功");
        } catch (Exception e) {
            log.error("处理ZIP代码库失败", e);
            return Response.error("处理ZIP代码库失败: " + e.getMessage());
        }
    }
    
    /**
     * 解压ZIP文件
     */
    private void unzipFile(File zipFile, String destDirectory) throws IOException {
        byte[] buffer = new byte[4096]; // 增大缓冲区
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)))) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                File newFile = new File(destDirectory, zipEntry.getName());
                
                // 安全检查，防止ZIP滑动攻击
                String destDirPath = new File(destDirectory).getCanonicalPath();
                String destFilePath = newFile.getCanonicalPath();
                
                if (!destFilePath.startsWith(destDirPath + File.separator)) {
                    throw new IOException("ZIP中包含非法路径: " + zipEntry.getName());
                }
                
                // 如果是目录，创建目录
                if (zipEntry.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        throw new IOException("无法创建目录: " + newFile);
                    }
                } else {
                    // 确保父目录存在
                    File parent = newFile.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("无法创建目录: " + parent);
                    }
                    
                    // 提取文件
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
        }
    }
    
    /**
     * 自定义MultipartFile实现，基于文件
     */
    private static class FileBasedMultipartFile implements MultipartFile {
        private final File file;

        public FileBasedMultipartFile(File file) {
            this.file = file;
        }

        @Override
        public String getName() {
            return file.getName();
        }

        @Override
        public String getOriginalFilename() {
            return file.getName();
        }

        @Override
        public String getContentType() {
            try {
                return Files.probeContentType(file.toPath());
            } catch (IOException e) {
                return "application/octet-stream";
            }
        }

        @Override
        public boolean isEmpty() {
            return file.length() == 0;
        }

        @Override
        public long getSize() {
            return file.length();
        }

        @Override
        public byte[] getBytes() throws IOException {
            return Files.readAllBytes(file.toPath());
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new FileInputStream(file);
        }

        @Override
        public Resource getResource() {
            return new FileSystemResource(file);
        }

        @Override
        public void transferTo(File dest) throws IOException, IllegalStateException {
            Files.copy(file.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * 判断是否为二进制文件
     */
    private boolean isBinaryFile(String fileName) {
        // 如果文件没有扩展名，默认不认为是二进制文件
        if (!fileName.contains(".")) {
            return false;
        }
        
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        String[] binaryExtensions = {
            "class", "jar", "war", "ear", "zip", "tar", "gz", "rar", 
            "exe", "dll", "so", "obj", "bin", "dat", "db", "sqlite",
            "jpg", "jpeg", "png", "gif", "bmp", "ico", "tif", "tiff",
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx"
        };
        
        for (String ext : binaryExtensions) {
            if (extension.equals(ext)) {
                return true;
            }
        }
        
        return false;
    }

    private String extractProjectName(String repoUrl) {
        String[] parts = repoUrl.split("/");
        String projectNameWithGit = parts[parts.length - 1];
        return projectNameWithGit.replace(".git", "");
    }

    /**
     * DeepSeek聊天接口
     */
    @PostMapping("/deepseek/chat")
    public Response<String> deepseekChat(@RequestBody Map<String, Object> params) {
        try {
            if (!aiConfig.getDeepseek().isEnabled()) {
                return Response.error("DeepSeek服务未启用");
            }

            String message = (String) params.get("message");
            if (message == null || message.isEmpty()) {
                return Response.error("消息内容不能为空");
            }

            Double temperature = params.get("temperature") != null ? 
                    Double.parseDouble(params.get("temperature").toString()) : 0.7;

            String response = ollamaService.chatWithDeepseek(message, temperature);
            return Response.success(response);
        } catch (Exception e) {
            log.error("DeepSeek聊天失败", e);
            return Response.error("聊天失败: " + e.getMessage());
        }
    }

    /**
     * ChatGLM聊天接口
     */
    @PostMapping("/chatglm/chat")
    public Response<String> chatglmChat(@RequestBody Map<String, Object> params) {
        try {
            if (!aiConfig.getChatglm().isEnabled()) {
                return Response.error("ChatGLM服务未启用");
            }

            String message = (String) params.get("message");
            if (message == null || message.isEmpty()) {
                return Response.error("消息内容不能为空");
            }

            Double temperature = params.get("temperature") != null ? 
                    Double.parseDouble(params.get("temperature").toString()) : 0.7;

            String response = ollamaService.chatWithChatGLM(message, temperature);
            return Response.success(response);
        } catch (Exception e) {
            log.error("ChatGLM聊天失败", e);
            return Response.error("聊天失败: " + e.getMessage());
        }
    }
} 