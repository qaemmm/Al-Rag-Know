<template>
  <div id="app">
    <el-container>
      <el-aside width="200px" class="sidebar">
        <div class="logo">
          <h1>AI RAG 系统</h1>
        </div>
        <el-menu 
          router 
          :default-active="activeMenu" 
          class="sidebar-menu"
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409EFF">
          <el-menu-item index="/chat">
            <el-icon><i class="el-icon-chat-line-round"></i></el-icon>
            <span>智能问答</span>
          </el-menu-item>
          <el-menu-item index="/knowledge">
            <el-icon><i class="el-icon-folder"></i></el-icon>
            <span>知识库管理</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-container>
        <el-main>
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script>
export default {
  name: 'App',
  computed: {
    activeMenu() {
      return this.$route.path
    }
  }
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body {
  height: 100%;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
}

#app {
  height: 100vh;
}

.el-container {
  height: 100%;
}

.sidebar {
  background-color: #304156;
  color: #fff;
  height: 100vh;
  overflow-y: auto;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid #1f2d3d;
}

.logo h1 {
  font-size: 1.2rem;
  color: #fff;
  margin: 0;
}

.sidebar-menu {
  border-right: none;
}

.el-main {
  padding: 0;
  background-color: #f5f7fa;
  overflow-y: auto;
}
</style> 