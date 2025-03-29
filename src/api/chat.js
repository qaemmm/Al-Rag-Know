import request from '@/utils/request'

// 获取支持的模型列表
export function getModels() {
  return request({
    url: '/api/v1/ai/ollama/models',
    method: 'get'
  })
}

// 发送普通聊天请求
export function sendMessage(message, temperature = 0.7) {
  return request({
    url: '/api/v1/rag/chat',
    method: 'post',
    data: {
      message,
      temperature
    }
  })
}

// 直接调用Ollama API
export function sendDirectOllamaMessage(data) {
  return request({
    url: '/api/v1/ai/ollama/direct_generate',
    method: 'post',
    data
  })
}

// 使用deepseek发送消息
export function sendDeepseekMessage(message, temperature = 0.7) {
  return request({
    url: '/api/v1/ai/deepseek/chat',
    method: 'post',
    data: {
      message,
      temperature
    }
  })
}

// 使用chatglm发送消息
export function sendChatGlmMessage(message, temperature = 0.7) {
  return request({
    url: '/api/v1/ai/chatglm/chat',
    method: 'post',
    data: {
      message,
      temperature
    }
  })
}

// 基于知识库的RAG聊天
export function sendRagMessage(message, knowledgeBaseId, temperature = 0.7) {
  return request({
    url: '/api/v1/rag/v2/chat',
    method: 'post',
    data: {
      message,
      knowledgeBaseId,
      temperature
    }
  })
}

// 获取聊天历史记录
export function getChatHistory(params) {
  return request({
    url: '/api/v1/chat/history',
    method: 'get',
    params
  })
}

// 获取聊天详情
export function getChatDetail(id) {
  return request({
    url: `/api/v1/chat/${id}`,
    method: 'get'
  })
}

// 创建新聊天
export function createChat(data) {
  return request({
    url: '/api/v1/chat',
    method: 'post',
    data
  })
}

// 更新聊天信息
export function updateChat(data) {
  return request({
    url: '/api/v1/chat',
    method: 'put',
    data
  })
}

// 删除聊天
export function deleteChat(data) {
  return request({
    url: '/api/v1/chat',
    method: 'delete',
    data
  })
}

/**
 * 普通聊天请求
 * @param {Object} data 聊天数据
 * @param {string} data.message 用户消息
 * @returns {Promise} 请求Promise
 */
export function chatRequest(data) {
  return request({
    url: '/api/v1/rag/chat',
    method: 'post',
    data
  })
}

/**
 * 基于知识库的RAG聊天请求
 * @param {Object} data 聊天数据
 * @param {string} data.message 用户消息
 * @param {string} data.knowledgeBaseId 知识库ID
 * @returns {Promise} 请求Promise
 */
export function ragChatRequest(data) {
  return request({
    url: '/api/v1/rag/v2/chat',
    method: 'post',
    data
  })
}

/**
 * 生成聊天消息对象
 * @param {string} role 角色，'user'或'assistant'
 * @param {string} content 消息内容
 * @param {Array} [sources=[]] 引用来源
 * @returns {Object} 聊天消息对象
 */
export function createChatMessage(role, content, sources = []) {
  return {
    role,
    content,
    sources,
    timestamp: new Date().getTime()
  }
}

/**
 * 增强生成的回答，添加源文档引用
 * @param {string} answer AI生成的回答
 * @param {Array} sources 源文档信息
 * @returns {string} 增强后的回答
 */
export function enhanceAnswerWithSources(answer, sources) {
  if (!sources || sources.length === 0) {
    return answer
  }

  let enhancedAnswer = answer + '\n\n参考来源：\n'
  
  sources.forEach((source, index) => {
    enhancedAnswer += `${index + 1}. ${source.title}\n`
    if (source.content) {
      const shortContent = source.content.length > 50 
        ? source.content.substring(0, 50) + '...' 
        : source.content
      enhancedAnswer += `   ${shortContent}\n`
    }
  })
  
  return enhancedAnswer
} 