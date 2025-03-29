import { createRouter, createWebHistory } from 'vue-router'
import ChatView from '../views/Chat.vue'
import KnowledgeView from '../views/Knowledge.vue'

const routes = [
  {
    path: '/',
    redirect: '/chat'
  },
  {
    path: '/chat',
    name: 'Chat',
    component: ChatView
  },
  {
    path: '/knowledge',
    name: 'Knowledge',
    component: KnowledgeView
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = to.meta.title || 'AI-RAG-Know 系统'
  next()
})

export default router 