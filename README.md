# AI-RAG-KNOW

基于大语言模型和向量数据库的RAG知识库系统，集成代码评审功能

## 主要特性

- 支持多种文档格式（PDF、Word、Excel、Text等）的导入
- 基于语义向量搜索的精准信息检索
- RAG (Retrieval-Augmented Generation) 增强生成回答
- 易用的Web界面，支持知识库管理和问答交互
- 支持多种LLM和向量存储的灵活配置
- **智能代码评审**: 基于大语言模型的自动代码评审功能

## 技术架构

- **前端**: Vue.js + Element UI
- **后端**: Spring Boot 3
- **RAG实现**: LangChain4j框架
- **向量数据库**: 支持内存存储、PgVector
- **大语言模型**: 支持Ollama本地部署、OpenAI API等
- **Git操作**: JGit

## RAG实现

项目基于LangChain4j框架实现了完整的RAG流程:

1. **文档加载与解析**: 支持PDF、Word等多种格式的解析和文本提取
2. **文档切分**: 将文档切分为适合向量化的文本块
3. **文本向量化**: 使用嵌入模型将文本转换为向量表示
4. **向量存储**: 将文本向量保存到向量数据库中
5. **相似度搜索**: 基于用户查询进行语义相似度检索
6. **上下文增强生成**: 将检索到的相关文档作为上下文，生成准确回答

### LangChain4j优势

- **高级抽象**: 使用注解方式简化AI服务的定义
- **组件化设计**: 提供模块化组件，便于扩展和替换
- **链式调用**: 支持组件链式组合，简化RAG流程实现
- **开箱即用**: 集成多种LLM模型和向量存储

## 代码评审功能

代码评审模块（`gwh-codereview`）是一个基于AI和向量数据库的智能代码评审工具，可以自动分析代码变更并提供高质量的评审意见。

### 主要功能

- **自动代码评审**：通过GitHub的Push和PR事件触发，自动分析代码变更
- **RAG增强评审**：利用向量数据库存储的编程最佳实践增强评审质量
- **多语言支持**：支持Java、JavaScript、Python等多种编程语言
- **问题分级**：将发现的问题按严重程度分为高、中、低、信息四级
- **飞书通知**：评审完成后通过飞书自动通知开发团队
- **GitHub集成**：支持GitHub Actions自动触发评审流程

### 技术架构

代码评审模块采用了以下技术：

- **Spring Boot**：提供Web服务和依赖注入框架
- **LangChain4j**：AI与向量数据库交互的框架
- **PgVector**：基于PostgreSQL的向量存储，用于检索相关编程最佳实践
- **JGit**：用于克隆代码仓库和解析代码差异
- **DeepSeek API**：提供大语言模型能力，用于代码评审

### 使用方式

#### 作为独立服务

1. 配置环境变量：
   ```bash
   export DEEPSEEK_API_KEY=your_api_key
   export FEISHU_WEBHOOK_URL=your_webhook_url
   ```

2. 启动服务：
   ```bash
   java -jar gwh-codereview.jar
   ```

3. 配置GitHub Webhook：
   - Webhook URL: `http://your-server:8081/webhook/github`
   - 事件类型：Push、Pull Request

#### 通过API调用

代码评审模块提供以下API：

- **提交评审请求**：
  ```
  POST /api/code-review/review
  
  {
    "repositoryUrl": "https://github.com/user/repo.git",
    "branch": "main",
    "commitId": "abc123def456"
  }
  ```

- **获取评审结果**：
  ```
  GET /api/code-review/result/{reviewId}
  ```

### 配置说明

在`application.yml`中可以配置以下参数：

```yaml
ai:
  deepseek:
    api-key: ${DEEPSEEK_API_KEY}
    api-url: https://api.deepseek.com/v1
    model: deepseek-chat
    embedding-model: deepseek-embed-v1
  
vector-store:
  collection-name: code_review_knowledge
  dimensions: 384
  
notification:
  enabled: true
  feishu:
    enabled: true
    webhook: ${FEISHU_WEBHOOK_URL}
```

### GitHub Actions集成

在你的项目中添加以下GitHub Actions配置：

```yaml
name: Code Review

on:
  pull_request:
    types: [opened, synchronize]

jobs:
  code-review:
    runs-on: ubuntu-latest
    steps:
      - name: Trigger Code Review
        uses: peter-evans/repository-dispatch@v2
        with:
          token: ${{ secrets.REPO_ACCESS_TOKEN }}
          repository: your-org/your-repo
          event-type: code-review
          client-payload: '{"repository": "${{ github.repository }}", "pr_number": "${{ github.event.pull_request.number }}", "commit_sha": "${{ github.event.pull_request.head.sha }}"}'
```

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- Redis
- Ollama (可选，用于本地部署LLM)

### 构建与运行

```bash
# 克隆仓库
git clone https://github.com/yourusername/ai-rag-know.git
cd ai-rag-know

# 构建项目
mvn clean package -DskipTests

# 运行
java -jar gwh-app/target/gwh-app.jar
```

### 配置

在`application.yml`中配置AI服务和文档处理参数:

```yaml
# AI服务配置
ai:
  ollama:
    api-url: http://localhost:11434
    default-model: llama2
    enabled: true

# 文档处理配置
document:
  chunk:
    size: 1000
    overlap: 200
    
# 代码评审配置
code-review:
  git:
    temp-dir: ${java.io.tmpdir}/code-review
  ai:
    model: codellama
    temperature: 0.3
    top-p: 0.95
```

## API接口

### 知识库管理

- `POST /api/v1/rag/knowledge/create` - 创建知识库
- `POST /api/v1/rag/knowledge/update` - 更新知识库
- `POST /api/v1/rag/knowledge/delete` - 删除知识库
- `GET /api/v1/rag/knowledge/list` - 获取知识库列表

### 文档管理

- `POST /api/v1/document/upload` - 上传文档到知识库
- `POST /api/v1/document/delete` - 从知识库删除文档

### 问答接口

- `POST /api/v1/rag/v2/chat` - RAG知识库问答
- `POST /api/v1/rag/chat` - 普通对话

### 代码评审接口

- `POST /api/code-review/review` - 提交代码评审请求
- `GET /api/code-review/result/{reviewId}` - 获取评审结果

## 贡献指南

欢迎贡献代码、提交issue或建议。请遵循以下流程:

1. Fork仓库
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交变更 (`git commit -m 'Add some amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 创建Pull Request

## 许可证

本项目采用MIT许可证 - 详见[LICENSE](LICENSE)文件 