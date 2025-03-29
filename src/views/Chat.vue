<template>
  <div class="chat-container">
    <!-- 侧边栏 -->
    <ChatSidebar 
      ref="sidebar"
      :current-chat-id="currentChatId"
      @select-chat="handleSelectChat"
      @create-new-chat="createNewChat"
      v-if="showSidebar"
    />

    <!-- 主内容区域 -->
    <div class="main-content">
      <!-- 顶部操作栏 -->
      <div class="action-bar">
        <div class="left-actions">
          <el-button 
            icon="el-icon-s-fold" 
            circle 
            @click="toggleSidebar"
            :title="showSidebar ? '隐藏侧边栏' : '显示侧边栏'"
          />
          <div class="model-selector">
            <span class="model-label">模型:</span>
            <el-select v-model="activeModel" size="small" class="model-select">
              <el-option 
                v-for="option in modelOptions" 
                :key="option.value" 
                :label="option.label" 
                :value="option.value"
              />
            </el-select>
          </div>
          
          <!-- 知识库选择器（始终可见） -->
          <div class="knowledge-base-selector">
            <span>知识库:</span>
            <el-select 
              v-model="selectedKnowledgeBase" 
              clearable
              size="small"
              placeholder="选择知识库">
              <el-option
                v-for="kb in knowledgeBases"
                :key="kb.id"
                :label="kb.name"
                :value="kb.id">
              </el-option>
            </el-select>
          </div>
          
          <div class="temperature-control">
            <span class="temp-label">温度:</span>
            <el-slider 
              v-model="temperature" 
              :min="0" 
              :max="1" 
              :step="0.1" 
              :show-tooltip="true"
              class="temp-slider"
              size="small"
            />
            <span class="temp-value">{{ temperature }}</span>
          </div>
        </div>
        <div class="right-actions">
          <el-button size="small" @click="exportMessages" type="primary" plain>
            <i class="el-icon-download"></i> 导出对话
          </el-button>
          <el-button size="small" @click="clearMessages" type="danger" plain>
            <i class="el-icon-delete"></i> 清空对话
          </el-button>
        </div>
      </div>

      <!-- 聊天消息区域 -->
      <div class="message-container" ref="messageContainer">
        <div v-if="messages.length === 0" class="empty-chat">
          <div class="empty-chat-icon">
            <i class="el-icon-chat-dot-round"></i>
          </div>
          <div class="empty-chat-title">开始一段新对话</div>
          <div class="empty-chat-subtitle">选择模型并发送消息，开始与AI进行对话</div>
        </div>

        <template v-else>
          <div v-for="(message, index) in messages" :key="index" class="message" :class="message.role">
            <div class="message-header">
              <div class="avatar" :class="message.role">
                <img v-if="message.role === 'assistant'" :src="getModelAvatar()" alt="AI">
                <i v-else class="el-icon-user"></i>
              </div>
              <div class="message-info">
                <div class="author">{{ message.role === 'user' ? '你' : getModelName() }}</div>
                <div class="time" v-if="message.timestamp">{{ formatTime(message.timestamp) }}</div>
              </div>
            </div>
            <div class="message-content" ref="messageContent">
              <div class="markdown-body" v-html="renderMessage(message.content)" @click="handleCodeClick"></div>
            </div>
          </div>
        </template>

        <div v-if="isLoading" class="loading-indicator">
          <div class="typing-animation">
            <span></span>
            <span></span>
            <span></span>
          </div>
        </div>
      </div>

      <!-- 输入区域 -->
      <div class="input-container">
        <div class="input-wrapper">
          <el-input
            v-model="userInput"
            type="textarea"
            :rows="1"
            placeholder="输入消息，按Enter发送，Shift+Enter换行"
            resize="none"
            @keydown.enter.prevent="handleEnterKey"
            ref="inputRef"
            :disabled="isLoading"
            class="chat-input"
            autosize
          />
          <div class="input-actions">
            <el-button 
              type="primary" 
              circle 
              @click="sendMessage" 
              :disabled="isLoading || !userInput.trim()" 
              class="send-btn"
            >
              <i class="el-icon-s-promotion"></i>
            </el-button>
          </div>
        </div>
        <div class="input-footer">
          <span class="hint-text">按 Enter 发送, Shift + Enter 换行</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
// 导入依赖
import { ref, reactive, onMounted, nextTick, computed, watch, defineEmits } from 'vue'
import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'
import ChatSidebar from './ChatSidebar.vue'
import { 
  sendChatGlmMessage, 
  sendDeepseekMessage, 
  sendDirectOllamaMessage, 
  chatRequest, 
  ragChatRequest, 
  createChatMessage,
  enhanceAnswerWithSources
} from '@/api/chat'
import { getKnowledgeList } from '@/api/knowledge'
import { ElMessage, ElMessageBox } from 'element-plus'

export default {
  name: 'ChatView',
  components: {
    ChatSidebar
  },
  emits: ['create-new-chat', 'update-chat'],
  setup(props, { emit }) {
    // 数据定义
    const messages = ref([])
    const userInput = ref('')
    const isLoading = ref(false)
    const messageContainer = ref(null)
    const messageContent = ref(null)
    const inputRef = ref(null)
    const activeModel = ref('chatglm')
    const temperature = ref(0.7)
    const showSidebar = ref(true)
    const currentChatId = ref('')
    const selectedKnowledgeBase = ref('')
    const knowledgeBases = ref([])
    const sidebar = ref(null)
    
    // 可用模型选项
    const modelOptions = [
      { label: 'ChatGLM', value: 'chatglm' },
      { label: 'DeepSeek', value: 'deepseek' },
      // { label: 'Ollama', value: 'ollama' }
    ]
    
    // 初始化
    onMounted(async () => {
      await loadKnowledgeBases()
      nextTick(() => {
        if (inputRef.value) {
          inputRef.value.focus()
        }
      })
    })
    
    // 加载知识库列表
    const loadKnowledgeBases = async () => {
      try {
        const res = await getKnowledgeList()
        knowledgeBases.value = res.data || []
      } catch (error) {
        console.error('加载知识库失败:', error)
      }
    }
    
    // 处理回车键
    const handleEnterKey = (e) => {
      if (e.shiftKey) {
        // Shift + Enter 换行，不做处理
        return
      }
      e.preventDefault()
      sendMessage()
    }
    
    // 发送消息
    const sendMessage = async () => {
      if (isLoading.value || !userInput.value.trim()) return
      
      const userMessage = userInput.value.trim()
      userInput.value = ''
      
      // 添加用户消息
      messages.value.push(createChatMessage('user', userMessage))
      
      // 滚动到底部
      await scrollToBottom()
      
      // 显示加载状态
      isLoading.value = true
      
      try {
        // 根据不同模型发送消息
        let response = null
        let assistantContent = "";
        
        if (selectedKnowledgeBase.value) {
          console.log("正在使用知识库, 当前模型:", activeModel.value)
          // 使用知识库进行问答，传递当前选择的模型
          response = await ragChatRequest({
            message: userMessage,
            knowledgeBaseId: selectedKnowledgeBase.value,
            model: activeModel.value, // 将当前选择的模型传递到后端
            temperature: temperature.value
          })
          
          // 处理知识库回答
          if (response && (response.code === '0000' || response.code === 0 || response.code === 200)) {
            const answer = response.data?.answer || response.data || ''
            const sources = response.data?.sources || []
            
            // 添加AI回复（可能包含来源引用）
            assistantContent = enhanceAnswerWithSources(answer, sources)
            messages.value.push(createChatMessage(
              'assistant',
              assistantContent,
              sources
            ))
          } else {
            throw new Error(response?.message || "知识库问答失败")
          }
        } else {
          // 根据选择的模型进行普通对话
          if (activeModel.value === 'chatglm') {
            response = await sendChatGlmMessage(userMessage, temperature.value)
          } else if (activeModel.value === 'deepseek') {
            response = await sendDeepseekMessage(userMessage, temperature.value)
          } else if (activeModel.value === 'ollama') {
            const data = {
              model: "deepseek-r1:1.5b", // 可配置的模型名称
              prompt: userMessage,
              stream: false,
              temperature: temperature.value
            }
            response = await sendDirectOllamaMessage(data)
          }
          
          // 处理响应
          if (response && (response.code === '0000' || response.code === 0 || response.code === 200)) {
            assistantContent = response.data || response.response || "AI未返回有效内容"
            messages.value.push(createChatMessage('assistant', assistantContent))
          } else {
            throw new Error(response?.message || "请求失败")
          }
        }
        
        // 创建新的聊天历史或更新现有历史 - 使用本地存储
        if (!currentChatId.value) {
          await createLocalChatHistory(userMessage, assistantContent)
        } else {
          await updateLocalChatHistory()
        }
      } catch (error) {
        console.error('发送消息失败:', error)
        messages.value.push(createChatMessage(
          'assistant',
          `发送消息失败: ${error.message || '未知错误'}`
        ))
      } finally {
        isLoading.value = false
        await scrollToBottom()
      }
    }
    
    // 创建本地聊天历史记录
    const createLocalChatHistory = async (userMessage, assistantContent) => {
      if (messages.value.length <= 1) return // 至少有一问一答才创建历史
      
      try {
        // 创建聊天历史标题，使用第一个问题作为标题
        const chatTitle = userMessage.length > 20 ? userMessage.substring(0, 20) + '...' : userMessage
        
        // 创建新的聊天历史
        const chatData = {
          id: Date.now().toString(), // 使用时间戳作为临时ID
          title: chatTitle,
          model: activeModel.value,
          messages: messages.value,
          knowledgeBase: selectedKnowledgeBase.value || '',
          timestamp: new Date().getTime()
        }
        
        // 存储到本地存储
        currentChatId.value = chatData.id
        
        // 获取现有的聊天历史
        const storedChats = JSON.parse(localStorage.getItem('chatHistories') || '[]')
        storedChats.push(chatData)
        localStorage.setItem('chatHistories', JSON.stringify(storedChats))
        
        // 通知侧边栏添加新聊天
        if (sidebar.value) {
          sidebar.value.addChat(chatData)
        }
      } catch (error) {
        console.error('创建聊天历史失败:', error)
      }
    }
    
    // 更新本地聊天历史
    const updateLocalChatHistory = async () => {
      if (!currentChatId.value) return
      
      try {
        // 从本地存储获取聊天历史
        const storedChats = JSON.parse(localStorage.getItem('chatHistories') || '[]')
        const index = storedChats.findIndex(chat => chat.id === currentChatId.value)
        
        if (index !== -1) {
          // 更新消息
          storedChats[index].messages = messages.value
          localStorage.setItem('chatHistories', JSON.stringify(storedChats))
          
          // 通知侧边栏更新
          if (sidebar.value) {
            sidebar.value.loadChatHistory()
          }
        }
      } catch (error) {
        console.error('更新聊天历史失败:', error)
      }
    }
    
    // 获取模型头像
    const getModelAvatar = () => {
      switch (activeModel.value) {
        case 'chatglm':
          return '/icons/chatglm.svg'
        case 'deepseek':
          return '/icons/deepseek.svg'
        case 'ollama':
          return '/icons/ollama.svg'
        default:
          return '/icons/chatglm.svg'
      }
    }
    
    // 获取模型名称
    const getModelName = () => {
      if (selectedKnowledgeBase.value) {
        const kb = knowledgeBases.value.find(k => k.id === selectedKnowledgeBase.value)
        return kb ? `知识库(${kb.name})` : '知识库问答'
      }
      
      switch (activeModel.value) {
        case 'chatglm':
          return 'ChatGLM'
        case 'deepseek':
          return 'DeepSeek'
        case 'ollama':
          return 'Ollama'
        default:
          return 'AI'
      }
    }
    
    // 渲染消息内容，支持 Markdown
    const renderMessage = (content) => {
      if (!content) return ''
      
      // 配置 marked
      marked.setOptions({
        highlight: function(code, lang) {
          if (lang && hljs.getLanguage(lang)) {
            try {
              return hljs.highlight(code, { language: lang }).value
            } catch (e) {
              console.error(e)
            }
          }
          return hljs.highlightAuto(code).value
        },
        breaks: true,
        gfm: true,
        tables: true,
        sanitize: false, // 允许HTML，但要小心XSS攻击
        smartLists: true,
        smartypants: true
      })
      
      try {
        // 先将内容中的换行符统一转换
        const processedContent = content.replace(/\n/g, '\n\n');
        return marked(processedContent);
      } catch (e) {
        console.error('Markdown 渲染错误:', e)
        return content
      }
    }
    
    // 处理代码块点击事件
    const handleCodeClick = (e) => {
      if (e.target.tagName === 'CODE' && e.target.parentElement.tagName === 'PRE') {
        const codeContent = e.target.textContent
        navigator.clipboard.writeText(codeContent)
          .then(() => {
            ElMessage.success('代码已复制到剪贴板')
          })
          .catch(err => {
            console.error('复制失败:', err)
            ElMessage.error('复制失败')
          })
      }
    }
    
    // 滚动到底部
    const scrollToBottom = async () => {
      await nextTick()
      if (messageContainer.value) {
        messageContainer.value.scrollTop = messageContainer.value.scrollHeight
      }
    }
    
    // 格式化时间
    const formatTime = (timestamp) => {
      if (!timestamp) return ''
      const date = new Date(timestamp)
      return date.toLocaleString()
    }
    
    // 清空聊天消息
    const clearMessages = () => {
      ElMessageBox.confirm('确定要清空所有聊天记录吗？此操作不可恢复。', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        messages.value = []
        ElMessage.success('聊天记录已清空')
      }).catch(() => {})
    }
    
    // 导出聊天记录
    const exportMessages = () => {
      if (messages.value.length === 0) {
        ElMessage.warning('没有可导出的聊天记录')
        return
      }
      
      try {
        const exportData = messages.value.map(msg => ({
          role: msg.role,
          content: msg.content,
          time: msg.timestamp ? new Date(msg.timestamp).toLocaleString() : '未知时间'
        }))
        
        const jsonStr = JSON.stringify(exportData, null, 2)
        const blob = new Blob([jsonStr], { type: 'application/json' })
        const url = URL.createObjectURL(blob)
        
        const a = document.createElement('a')
        a.href = url
        a.download = `聊天记录_${new Date().toISOString().slice(0, 10)}.json`
        document.body.appendChild(a)
        a.click()
        document.body.removeChild(a)
        URL.revokeObjectURL(url)
        
        ElMessage.success('导出成功')
      } catch (error) {
        console.error('导出失败:', error)
        ElMessage.error('导出失败')
      }
    }
    
    // 切换侧边栏
    const toggleSidebar = () => {
      showSidebar.value = !showSidebar.value
    }
    
    // 处理选择聊天
    const handleSelectChat = (chat) => {
      if (!chat) {
        createNewChat()
        return
      }
      
      currentChatId.value = chat.id
      messages.value = chat.messages || []
      activeModel.value = chat.model || 'chatglm'
      
      // 滚动到底部
      nextTick(() => {
        scrollToBottom()
      })
    }
    
    // 创建新聊天
    const createNewChat = () => {
      currentChatId.value = ''
      messages.value = []
      
      // 聚焦输入框
      nextTick(() => {
        if (inputRef.value) {
          inputRef.value.focus()
        }
      })
    }
    
    // 处理子组件创建新对话事件
    const handleCreateNewChat = (chat) => {
      if (!chat) {
        createNewChat()
        return
      }
      
      // 在侧边栏中添加新对话
      if (this.$refs.sidebar) {
        this.$refs.sidebar.addChat(chat)
      }
    }
    
    // 处理子组件更新对话事件
    const handleUpdateChat = (chat) => {
      if (this.$refs.sidebar) {
        this.$refs.sidebar.loadChatHistory()
      }
    }
    
    return {
      messages,
      userInput,
      isLoading,
      messageContainer,
      messageContent,
      inputRef,
      activeModel,
      temperature,
      modelOptions,
      showSidebar,
      currentChatId,
      selectedKnowledgeBase,
      knowledgeBases,
      sidebar,
      handleEnterKey,
      sendMessage,
      getModelAvatar,
      getModelName,
      renderMessage,
      handleCodeClick,
      scrollToBottom,
      formatTime,
      clearMessages,
      exportMessages,
      toggleSidebar,
      handleSelectChat,
      createNewChat,
      loadKnowledgeBases,
      createLocalChatHistory,
      updateLocalChatHistory,
      handleCreateNewChat,
      handleUpdateChat
    }
  }
}
</script>

<style scoped>
.chat-container {
  display: flex;
  height: 100vh;
  background-color: #f5f7fa;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 20px;
  background-color: white;
  border-bottom: 1px solid #e4e7ed;
}

.left-actions, .right-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.model-selector, .temperature-control, .knowledge-base-selector {
  display: flex;
  align-items: center;
  gap: 8px;
}

.model-select {
  width: 120px;
}

.temp-slider {
  width: 120px;
}

.model-label, .temp-label, .temp-value {
  color: #606266;
  font-size: 14px;
}

.knowledge-base-selector span {
  color: #606266;
  font-size: 14px;
  white-space: nowrap;
}

.knowledge-base-selector .el-select {
  width: 150px;
}

.message-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background-color: #f5f7fa;
}

.empty-chat {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #909399;
}

.empty-chat-icon {
  font-size: 48px;
  color: #c0c4cc;
  margin-bottom: 16px;
}

.empty-chat-title {
  font-size: 20px;
  margin-bottom: 8px;
}

.empty-chat-subtitle {
  font-size: 14px;
}

.message {
  margin-bottom: 24px;
  max-width: 85%;
}

.message.user {
  margin-left: auto;
}

.message.assistant {
  margin-right: auto;
}

.message-header {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  background-color: #e0f2fe;
}

.avatar.user {
  background-color: #409EFF;
  color: white;
}

.avatar img {
  width: 24px;
  height: 24px;
  object-fit: contain;
}

.message-info {
  display: flex;
  flex-direction: column;
}

.author {
  font-weight: 500;
  font-size: 14px;
  color: #303133;
}

.time {
  font-size: 12px;
  color: #909399;
}

.message-content {
  background-color: white;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
  overflow-wrap: break-word;
}

.message.user .message-content {
  background-color: #ecf5ff;
}

.loading-indicator {
  display: flex;
  justify-content: flex-start;
  margin-bottom: 24px;
}

.typing-animation {
  display: flex;
  align-items: center;
}

.typing-animation span {
  width: 8px;
  height: 8px;
  margin: 0 2px;
  background-color: #909399;
  border-radius: 50%;
  display: inline-block;
  animation: bounce 1.3s infinite ease-in-out;
}

.typing-animation span:nth-child(1) {
  animation-delay: 0s;
}

.typing-animation span:nth-child(2) {
  animation-delay: 0.16s;
}

.typing-animation span:nth-child(3) {
  animation-delay: 0.32s;
}

@keyframes bounce {
  0%, 80%, 100% { 
    transform: scale(0);
  } 40% { 
    transform: scale(1.0);
  }
}

.input-container {
  padding: 16px;
  background-color: white;
  border-top: 1px solid #e4e7ed;
}

.input-wrapper {
  display: flex;
  align-items: flex-end;
}

.chat-input {
  flex: 1;
}

.input-actions {
  margin-left: 12px;
}

.send-btn {
  background-color: #409EFF;
  color: white;
}

.input-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
  padding: 0 12px;
}

.hint-text {
  color: #909399;
  font-size: 12px;
}

/* 标记语言样式 */
:deep(.markdown-body) {
  font-size: 14px;
  line-height: 1.6;
}

:deep(.markdown-body pre) {
  position: relative;
  background-color: #f6f8fa;
  border-radius: 6px;
  padding: 16px;
  margin: 16px 0;
}

:deep(.markdown-body code) {
  font-family: Consolas, Monaco, 'Andale Mono', monospace;
  font-size: 13px;
}

:deep(.markdown-body p) {
  margin: 8px 0;
}

:deep(.markdown-body ul), :deep(.markdown-body ol) {
  padding-left: 20px;
}

:deep(.markdown-body table) {
  border-collapse: collapse;
  width: 100%;
  margin: 16px 0;
}

:deep(.markdown-body th), :deep(.markdown-body td) {
  border: 1px solid #e4e7ed;
  padding: 8px 12px;
}

:deep(.markdown-body th) {
  background-color: #f5f7fa;
}
</style> 