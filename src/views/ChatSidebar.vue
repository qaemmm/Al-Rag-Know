<template>
  <div class="chat-sidebar">
    <div class="sidebar-header">
      <div class="title">对话历史</div>
      <el-button size="small" type="primary" @click="createNewChat">
        新建对话
      </el-button>
    </div>
    
    <div class="search-box">
      <el-input
        v-model="searchQuery"
        placeholder="搜索对话"
        clearable
        prefix-icon="el-icon-search"
        size="small"
      />
    </div>
    
    <div class="chat-history" v-if="sortedChats.length > 0">
      <div class="time-group" v-for="group in groupedChats" :key="group.label">
        <div class="time-label">{{ group.label }}</div>
        <div
          v-for="chat in group.chats"
          :key="chat.id"
          class="chat-item"
          :class="{ active: currentChatId === chat.id }"
          @click="selectChat(chat)"
        >
          <div class="chat-icon">
            <img :src="getModelIcon(chat.model)" alt="Model" />
          </div>
          <div class="chat-info">
            <div class="chat-title">{{ chat.title }}</div>
            <div class="chat-meta">
              <span class="chat-date">{{ formatDate(chat.timestamp) }}</span>
              <span v-if="chat.knowledgeBase" class="chat-kb">KB: {{ chat.knowledgeBase }}</span>
            </div>
          </div>
          <div class="chat-actions">
            <el-dropdown trigger="click" @command="handleCommand($event, chat)">
              <span class="el-dropdown-link">
                <i class="el-icon-more"></i>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="rename">重命名</el-dropdown-item>
                  <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </div>
    </div>
    
    <div v-else class="empty-history">
      <div class="empty-icon">
        <i class="el-icon-chat-line-round"></i>
      </div>
      <div class="empty-text">暂无对话记录</div>
      <div class="empty-hint">点击"新建对话"开始聊天</div>
    </div>
    
    <!-- 重命名对话框 -->
    <el-dialog
      title="重命名对话"
      v-model="renameDialogVisible"
      width="30%"
      :before-close="closeRenameDialog"
    >
      <el-input v-model="newChatTitle" placeholder="输入新标题"></el-input>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="closeRenameDialog">取消</el-button>
          <el-button type="primary" @click="confirmRename">确认</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getChatHistory, deleteChat, updateChat } from '@/api/chat'

export default {
  name: 'ChatSidebar',
  props: {
    currentChatId: {
      type: String,
      default: ''
    }
  },
  emits: ['select-chat', 'create-new-chat'],
  setup(props, { emit }) {
    const chats = ref([])
    const searchQuery = ref('')
    const renameDialogVisible = ref(false)
    const newChatTitle = ref('')
    const chatToRename = ref(null)
    
    // 过滤和排序聊天历史
    const sortedChats = computed(() => {
      let filtered = chats.value
      
      // 搜索过滤
      if (searchQuery.value) {
        const query = searchQuery.value.toLowerCase()
        filtered = filtered.filter(chat => 
          chat.title.toLowerCase().includes(query)
        )
      }
      
      // 按时间倒序排序
      return [...filtered].sort((a, b) => b.timestamp - a.timestamp)
    })
    
    // 按时间分组聊天历史
    const groupedChats = computed(() => {
      const groups = {}
      const now = new Date()
      const today = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
      const yesterday = today - 86400000
      const thisWeek = today - 86400000 * 6
      
      sortedChats.value.forEach(chat => {
        let groupLabel
        const chatDate = new Date(chat.timestamp)
        
        if (chatDate.getTime() >= today) {
          groupLabel = '今天'
        } else if (chatDate.getTime() >= yesterday) {
          groupLabel = '昨天'
        } else if (chatDate.getTime() >= thisWeek) {
          groupLabel = '本周'
        } else {
          groupLabel = '更早'
        }
        
        if (!groups[groupLabel]) {
          groups[groupLabel] = {
            label: groupLabel,
            chats: []
          }
        }
        
        groups[groupLabel].chats.push(chat)
      })
      
      // 转换为数组并保持顺序
      const orderedLabels = ['今天', '昨天', '本周', '更早']
      return orderedLabels
        .filter(label => groups[label])
        .map(label => groups[label])
    })
    
    // 初始化
    onMounted(async () => {
      await loadChatHistory()
    })
    
    // 加载聊天历史
    const loadChatHistory = async () => {
      try {
        // 从本地存储获取聊天历史
        const storedChats = JSON.parse(localStorage.getItem('chatHistories') || '[]')
        chats.value = storedChats
        console.log("已从本地存储加载聊天历史:", chats.value.length)
      } catch (error) {
        console.error('加载聊天历史失败:', error)
        ElMessage.error('加载聊天历史失败')
      }
    }
    
    // 选择聊天
    const selectChat = (chat) => {
      emit('select-chat', chat)
    }
    
    // 创建新聊天
    const createNewChat = () => {
      emit('create-new-chat')
    }
    
    // 格式化日期
    const formatDate = (timestamp) => {
      if (!timestamp) return ''
      const date = new Date(timestamp)
      return date.toLocaleString()
    }
    
    // 获取模型图标
    const getModelIcon = (model) => {
      switch (model) {
        case 'chatglm':
          return '/icons/chatglm.svg'
        case 'deepseek':
          return '/icons/deepseek.svg'
        case 'ollama':
          return '/icons/ollama.svg'
        default:
          return '/icons/default.svg'
      }
    }
    
    // 下拉菜单命令处理
    const handleCommand = (command, chat) => {
      if (command === 'rename') {
        showRenameDialog(chat)
      } else if (command === 'delete') {
        confirmDelete(chat)
      }
    }
    
    // 显示重命名对话框
    const showRenameDialog = (chat) => {
      chatToRename.value = chat
      newChatTitle.value = chat.title
      renameDialogVisible.value = true
    }
    
    // 关闭重命名对话框
    const closeRenameDialog = () => {
      renameDialogVisible.value = false
      chatToRename.value = null
      newChatTitle.value = ''
    }
    
    // 确认重命名
    const confirmRename = async () => {
      if (!newChatTitle.value.trim()) {
        ElMessage.warning('标题不能为空')
        return
      }
      
      try {
        // 更新本地存储的聊天历史
        const storedChats = JSON.parse(localStorage.getItem('chatHistories') || '[]')
        const index = storedChats.findIndex(c => c.id === chatToRename.value.id)
        
        if (index !== -1) {
          storedChats[index].title = newChatTitle.value.trim()
          localStorage.setItem('chatHistories', JSON.stringify(storedChats))
          
          // 更新本地数据
          const localIndex = chats.value.findIndex(c => c.id === chatToRename.value.id)
          if (localIndex !== -1) {
            chats.value[localIndex].title = newChatTitle.value.trim()
          }
          
          ElMessage.success('重命名成功')
          closeRenameDialog()
        }
      } catch (error) {
        console.error('重命名失败:', error)
        ElMessage.error('重命名失败: ' + error.message)
      }
    }
    
    // 确认删除
    const confirmDelete = (chat) => {
      ElMessageBox.confirm(
        '确定要删除此对话吗？此操作不可恢复。',
        '提示',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
      ).then(async () => {
        try {
          // 从本地存储中删除
          const storedChats = JSON.parse(localStorage.getItem('chatHistories') || '[]')
          const filteredChats = storedChats.filter(c => c.id !== chat.id)
          localStorage.setItem('chatHistories', JSON.stringify(filteredChats))
          
          // 从本地数据移除
          chats.value = chats.value.filter(c => c.id !== chat.id)
          
          // 如果删除的是当前选中的对话，则创建新对话
          if (props.currentChatId === chat.id) {
            createNewChat()
          }
          
          ElMessage.success('删除成功')
        } catch (error) {
          console.error('删除失败:', error)
          ElMessage.error('删除失败: ' + error.message)
        }
      }).catch(() => {})
    }
    
    // 监听当前组件外部创建的新聊天
    const addChat = (chat) => {
      if (!chat) return
      
      // 更新本地数据
      const existingIndex = chats.value.findIndex(c => c.id === chat.id)
      if (existingIndex !== -1) {
        // 更新现有聊天
        chats.value[existingIndex] = { ...chats.value[existingIndex], ...chat }
      } else {
        // 添加新聊天
        chats.value.push(chat)
      }
      
      // 不需要单独更新localStorage，因为这是从Chat.vue传过来的，已经更新了localStorage
    }
    
    return {
      chats,
      searchQuery,
      sortedChats,
      groupedChats,
      renameDialogVisible,
      newChatTitle,
      selectChat,
      createNewChat,
      formatDate,
      getModelIcon,
      handleCommand,
      closeRenameDialog,
      confirmRename,
      loadChatHistory,
      addChat
    }
  }
}
</script>

<style scoped>
.chat-sidebar {
  width: 300px;
  height: 100%;
  border-right: 1px solid #e4e7ed;
  background-color: #f9fafc;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #e4e7ed;
}

.title {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}

.search-box {
  padding: 12px 16px;
  border-bottom: 1px solid #e4e7ed;
}

.chat-history {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.time-group {
  margin-bottom: 16px;
}

.time-label {
  padding: 0 16px;
  margin-bottom: 8px;
  font-size: 13px;
  color: #909399;
}

.chat-item {
  padding: 10px 16px;
  display: flex;
  align-items: center;
  cursor: pointer;
}

.chat-item:hover {
  background-color: #ecf5ff;
}

.chat-item.active {
  background-color: #ecf5ff;
}

.chat-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background-color: #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
}

.chat-icon img {
  width: 20px;
  height: 20px;
}

.chat-info {
  flex: 1;
  min-width: 0;
}

.chat-title {
  font-size: 14px;
  color: #303133;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chat-meta {
  font-size: 12px;
  color: #909399;
  display: flex;
  align-items: center;
}

.chat-date {
  margin-right: 8px;
}

.chat-kb {
  background-color: #e4e7ed;
  padding: 2px 6px;
  border-radius: 4px;
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-actions {
  opacity: 0;
  transition: opacity 0.2s;
}

.chat-item:hover .chat-actions {
  opacity: 1;
}

.empty-history {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #909399;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  color: #c0c4cc;
}

.empty-text {
  font-size: 16px;
  margin-bottom: 8px;
}

.empty-hint {
  font-size: 14px;
}

.el-dropdown-link {
  cursor: pointer;
  color: #909399;
}
</style> 