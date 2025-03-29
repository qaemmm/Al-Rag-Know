<template>
  <div class="knowledge-chat-container">
    <div class="knowledge-header">
      <div class="back-btn">
        <el-button icon="el-icon-arrow-left" @click="goBack">返回</el-button>
      </div>
      <div class="knowledge-info">
        <h2>{{ knowledgeBase?.name || '知识库问答' }}</h2>
        <p v-if="knowledgeBase?.description">{{ knowledgeBase.description }}</p>
      </div>
    </div>

    <div class="chat-area" ref="chatArea">
      <div v-if="messages.length === 0" class="empty-chat">
        <div class="empty-icon">
          <img :src="getKnowledgeIcon()" alt="知识库图标">
        </div>
        <h3>{{ knowledgeBase?.name || '知识库' }}</h3>
        <p>向知识库提问，获取基于知识库内容的精准回答</p>
      </div>

      <div v-else class="message-list">
        <div 
          v-for="(msg, index) in messages" 
          :key="index" 
          :class="['message-item', msg.role === 'user' ? 'user-message' : 'ai-message']"
        >
          <div class="message-avatar">
            <img 
              :src="msg.role === 'user' ? userAvatar : botAvatar" 
              :alt="msg.role === 'user' ? '用户' : 'AI'"
            >
          </div>
          <div class="message-content">
            <div class="message-text" v-if="!msg.loading" v-html="formatMessage(msg.content)"></div>
            <div class="message-loading" v-else>
              <span class="dot"></span>
              <span class="dot"></span>
              <span class="dot"></span>
            </div>
            
            <!-- 引用源 -->
            <div v-if="msg.sources && msg.sources.length > 0" class="message-sources">
              <div class="sources-header" @click="msg.showSources = !msg.showSources">
                <span>引用源 ({{ msg.sources.length }})</span>
                <i :class="msg.showSources ? 'el-icon-arrow-up' : 'el-icon-arrow-down'"></i>
              </div>
              <div v-if="msg.showSources" class="sources-list">
                <div 
                  v-for="(source, sIndex) in msg.sources" 
                  :key="sIndex"
                  class="source-item"
                >
                  <div class="source-title">{{ source.title }}</div>
                  <div class="source-content">{{ source.content }}</div>
                </div>
              </div>
            </div>
            
            <div class="message-time">
              {{ formatTime(msg.time) }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="input-area">
      <div class="input-wrapper">
        <el-input
          v-model="userInput"
          type="textarea"
          :autosize="{ minRows: 1, maxRows: 5 }"
          placeholder="请输入问题..."
          @keydown.enter.exact.prevent="sendMessage"
        ></el-input>
        <div class="input-actions">
          <el-button 
            type="primary" 
            :disabled="userInput.trim() === '' || loading" 
            @click="sendMessage"
          >
            <i class="el-icon-s-promotion"></i>
            发送
          </el-button>
        </div>
      </div>
      <div class="input-options">
        <div class="temperature-setting">
          <span class="option-label">温度:</span>
          <el-slider
            v-model="temperature"
            :min="0"
            :max="1"
            :step="0.1"
            :format-tooltip="formatTemperature"
            class="temperature-slider"
          ></el-slider>
          <span class="temperature-value">{{ temperature.toFixed(1) }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, nextTick, computed, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { sendRagMessage } from '@/api/chat'
import { getKnowledgeDetail } from '@/api/knowledge'
import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'

export default {
  name: 'KnowledgeChat',
  setup() {
    const route = useRoute()
    const router = useRouter()
    const knowledgeBaseId = computed(() => route.params.id)
    const knowledgeBase = ref(null)
    const loading = ref(false)
    const userInput = ref('')
    const messages = ref([])
    const chatArea = ref(null)
    const temperature = ref(0.7)
    
    // 头像
    const userAvatar = '/icons/user.svg'
    const botAvatar = '/icons/rag.svg'
    
    // 初始化marked配置
    marked.setOptions({
      renderer: new marked.Renderer(),
      highlight: function(code, lang) {
        try {
          const language = hljs.getLanguage(lang) ? lang : 'plaintext';
          return hljs.highlight(code, { language }).value;
        } catch (e) {
          console.error(e);
          return hljs.highlight(code, { language: 'plaintext' }).value;
        }
      },
      pedantic: false,
      gfm: true,
      breaks: true,
    });
    
    // 获取知识库详情
    const fetchKnowledgeBase = async () => {
      if (!knowledgeBaseId.value) return;
      
      try {
        const res = await getKnowledgeDetail(knowledgeBaseId.value);
        if (res.code === '0000') {
          knowledgeBase.value = res.data;
        } else {
          ElMessage.error('获取知识库详情失败');
        }
      } catch (error) {
        console.error('获取知识库详情失败:', error);
        ElMessage.error('获取知识库详情失败');
      }
    };
    
    // 格式化消息内容，将markdown转换为HTML
    const formatMessage = (content) => {
      if (!content) return '';
      try {
        return marked(content);
      } catch (e) {
        console.error('Markdown解析错误:', e);
        return content;
      }
    };
    
    // 滚动到底部
    const scrollToBottom = () => {
      nextTick(() => {
        if (chatArea.value) {
          chatArea.value.scrollTop = chatArea.value.scrollHeight;
        }
      });
    };
    
    // 发送消息
    const sendMessage = async () => {
      if (userInput.value.trim() === '' || loading.value) return;
      
      const userMessage = userInput.value;
      userInput.value = '';
      
      // 添加用户消息
      messages.value.push({
        role: 'user',
        content: userMessage,
        time: new Date()
      });
      
      // 添加AI消息，设置loading状态
      messages.value.push({
        role: 'assistant',
        content: '',
        loading: true,
        time: new Date()
      });
      
      scrollToBottom();
      loading.value = true;
      
      try {
        const res = await sendRagMessage(userMessage, knowledgeBaseId.value, temperature.value);
        
        if (res.code === '0000') {
          const aiIndex = messages.value.length - 1;
          
          if (typeof res.data === 'string') {
            // 处理旧版API返回格式
            messages.value[aiIndex] = {
              role: 'assistant',
              content: res.data,
              loading: false,
              time: new Date()
            };
          } else {
            // 处理新版API返回格式
            messages.value[aiIndex] = {
              role: 'assistant',
              content: res.data.answer || '抱歉，我无法回答您的问题。',
              sources: res.data.sources || [],
              showSources: false,
              loading: false,
              time: new Date()
            };
          }
        } else {
          // 显示错误消息
          const aiIndex = messages.value.length - 1;
          messages.value[aiIndex] = {
            role: 'assistant',
            content: `抱歉，我遇到了问题：${res.message || '未知错误'}`,
            loading: false,
            time: new Date()
          };
          
          ElMessage.error(res.message || '回答失败');
        }
      } catch (error) {
        console.error('发送消息失败:', error);
        
        // 更新最后一条消息为错误状态
        const aiIndex = messages.value.length - 1;
        messages.value[aiIndex] = {
          role: 'assistant',
          content: '抱歉，服务器连接失败，请稍后再试。',
          loading: false,
          time: new Date()
        };
        
        ElMessage.error('服务器连接失败');
      } finally {
        loading.value = false;
        scrollToBottom();
      }
    };
    
    // 格式化时间
    const formatTime = (time) => {
      if (!time) return '';
      const date = new Date(time);
      return date.toLocaleString('zh-CN', { 
        hour: '2-digit', 
        minute: '2-digit',
        second: '2-digit' 
      });
    };
    
    // 格式化温度显示
    const formatTemperature = (value) => {
      return `${value.toFixed(1)}`;
    };
    
    // 获取知识库图标
    const getKnowledgeIcon = () => {
      return '/icons/rag.svg';
    };
    
    // 返回上一页
    const goBack = () => {
      router.push('/knowledge');
    };
    
    onMounted(() => {
      fetchKnowledgeBase();
    });
    
    return {
      knowledgeBase,
      userInput,
      messages,
      loading,
      chatArea,
      temperature,
      userAvatar,
      botAvatar,
      formatMessage,
      formatTime,
      formatTemperature,
      getKnowledgeIcon,
      sendMessage,
      goBack
    };
  }
}
</script>

<style scoped>
.knowledge-chat-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: #f7f8fa;
}

.knowledge-header {
  padding: 16px 20px;
  background-color: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
}

.back-btn {
  margin-right: 16px;
}

.knowledge-info {
  flex: 1;
}

.knowledge-info h2 {
  margin: 0;
  font-size: 20px;
  color: #303133;
}

.knowledge-info p {
  margin: 5px 0 0;
  font-size: 14px;
  color: #606266;
}

.chat-area {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.empty-chat {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #909399;
  text-align: center;
}

.empty-icon {
  width: 80px;
  height: 80px;
  margin-bottom: 16px;
}

.empty-icon img {
  width: 100%;
  height: 100%;
}

.empty-chat h3 {
  margin: 0 0 8px;
  color: #606266;
}

.empty-chat p {
  margin: 0;
  color: #909399;
}

.message-list {
  display: flex;
  flex-direction: column;
}

.message-item {
  display: flex;
  margin-bottom: 20px;
}

.user-message {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  margin: 0 12px;
}

.message-avatar img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
}

.message-content {
  max-width: 70%;
  background-color: #fff;
  padding: 12px 16px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.user-message .message-content {
  background-color: #ecf5ff;
}

.message-text {
  color: #303133;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-loading {
  display: flex;
  align-items: center;
  padding: 8px 0;
}

.dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #c0c4cc;
  margin: 0 4px;
  animation: dot-flashing 1s infinite alternate;
}

.dot:nth-child(2) {
  animation-delay: 0.2s;
}

.dot:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes dot-flashing {
  0% {
    opacity: 0.2;
  }
  100% {
    opacity: 1;
  }
}

.message-time {
  text-align: right;
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
}

.message-sources {
  margin-top: 12px;
  border-top: 1px solid #ebeef5;
  padding-top: 8px;
}

.sources-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: #606266;
  cursor: pointer;
  padding: 4px 0;
}

.sources-list {
  margin-top: 8px;
}

.source-item {
  background-color: #f5f7fa;
  border-radius: 4px;
  padding: 8px 12px;
  margin-bottom: 8px;
}

.source-title {
  font-weight: 500;
  font-size: 13px;
  color: #606266;
  margin-bottom: 4px;
}

.source-content {
  font-size: 12px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}

.input-area {
  padding: 16px;
  background-color: #fff;
  border-top: 1px solid #e4e7ed;
}

.input-wrapper {
  display: flex;
  align-items: flex-end;
}

.input-wrapper .el-input {
  flex: 1;
  margin-right: 12px;
}

.input-actions {
  display: flex;
  align-items: center;
}

.input-options {
  display: flex;
  align-items: center;
  margin-top: 12px;
}

.temperature-setting {
  display: flex;
  align-items: center;
  font-size: 13px;
  color: #606266;
}

.option-label {
  margin-right: 8px;
  font-size: 13px;
}

.temperature-slider {
  width: 200px;
  margin: 0 8px 0 0;
}

.temperature-value {
  min-width: 24px;
  text-align: center;
}

/* 扩展样式 */
:deep(.message-text) {
  line-height: 1.6;
}

:deep(.message-text p) {
  margin: 0;
}

:deep(.message-text p + p) {
  margin-top: 8px;
}

:deep(.message-text code) {
  background-color: #f5f7fa;
  border-radius: 3px;
  padding: 2px 4px;
  color: #c92c2c;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
}

:deep(.message-text pre) {
  background-color: #282c34;
  border-radius: 6px;
  padding: 16px;
  overflow-x: auto;
  margin: 16px 0;
}

:deep(.message-text pre code) {
  background-color: transparent;
  color: #abb2bf;
  padding: 0;
  border-radius: 0;
}

:deep(.message-text ul, .message-text ol) {
  padding-left: 24px;
  margin: 8px 0;
}

:deep(.message-text li) {
  margin-bottom: 4px;
}

:deep(.message-text table) {
  border-collapse: collapse;
  width: 100%;
  margin: 16px 0;
}

:deep(.message-text th, .message-text td) {
  border: 1px solid #dcdfe6;
  padding: 8px 12px;
  text-align: left;
}

:deep(.message-text th) {
  background-color: #f5f7fa;
}
</style> 