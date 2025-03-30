import request from '@/utils/request'

// 获取知识库标签列表
export function getRagTags() {
  return request({
    url: '/api/v1/rag/tags',
    method: 'get'
  })
}

// 获取知识库列表
export function getKnowledgeList() {
  return request({
    url: '/api/v1/rag/knowledge/list',
    method: 'get'
  })
}

// 创建知识库
export function createKnowledge(data) {
  return request({
    url: '/api/v1/rag/knowledge/create',
    method: 'post',
    data
  })
}

// 更新知识库
export function updateKnowledge(data) {
  return request({
    url: '/api/v1/rag/knowledge/update',
    method: 'post',
    data
  })
}

// 删除知识库
export function deleteKnowledge(id) {
  return request({
    url: '/api/v1/rag/knowledge/delete',
    method: 'post',
    data: { id }
  })
}

// 获取知识库详情
export function getKnowledgeDetail(id) {
  return request({
    url: `/api/v1/rag/knowledge/${id}`,
    method: 'get'
  })
}

// 上传文件到知识库
export function uploadFile(knowledgeBaseId, file, onProgress) {
  const formData = new FormData()
  formData.append('knowledgeBaseId', knowledgeBaseId)
  formData.append('file', file)
  
  return request({
    url: '/api/v1/document/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    onUploadProgress: onProgress ? e => {
      if (e.total > 0) {
        e.percent = (e.loaded / e.total) * 100
      }
      onProgress(e)
    } : undefined
  })
}

// 获取知识库文件列表
export function getKnowledgeFiles(knowledgeBaseId, params) {
  return request({
    url: `/api/v1/rag/knowledge/${knowledgeBaseId}/files`,
    method: 'get',
    params
  })
}

// 删除知识库文档
export function deleteKnowledgeFile(knowledgeBaseId) {
  return request({
    url: '/api/v1/document/delete',
    method: 'post',
    data: { knowledgeBaseId }
  })
}

// 基于知识库的RAG问答
export function ragChat(data) {
  return request({
    url: '/api/v1/rag/v2/chat',
    method: 'post',
    data
  })
}

// 普通聊天
export function chat(data) {
  return request({
    url: '/api/v1/rag/chat',
    method: 'post',
    data
  })
}

// 分析Git仓库
export function analyzeGitRepository(data) {
  return request({
    url: '/api/v1/ai/ollama/analyze_git_repository',
    method: 'post',
    data,
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    },
    transformRequest: [
      function(data) {
        let ret = ''
        for (let it in data) {
          ret += encodeURIComponent(it) + '=' + encodeURIComponent(data[it]) + '&'
        }
        return ret.slice(0, -1)
      }
    ]
  })
}

// 分析GitHub仓库
export function analyzeGithubRepository(data) {
  return request({
    url: '/api/v1/ai/ollama/analyze_github_repository',
    method: 'post',
    data,
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    },
    transformRequest: [
      function(data) {
        let ret = ''
        for (let it in data) {
          ret += encodeURIComponent(it) + '=' + encodeURIComponent(data[it]) + '&'
        }
        return ret.slice(0, -1)
      }
    ]
  })
} 