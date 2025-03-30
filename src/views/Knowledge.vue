<template>
  <div class="knowledge-container">
    <div class="header">
      <h2>知识库管理</h2>
      <el-button type="primary" @click="showCreateDialog">创建知识库</el-button>
    </div>

    <!-- 知识库列表 -->
    <div class="knowledge-list" v-loading="loading">
      <el-card v-for="kb in knowledgeBases" :key="kb.id" class="knowledge-card">
        <div class="knowledge-header">
          <h3>{{ kb.name }}</h3>
          <div class="knowledge-actions">
            <el-button type="primary" size="small" @click="showUploadDialog(kb)">上传文档</el-button>
            <el-button type="info" size="small" @click="showEditDialog(kb)">编辑</el-button>
            <el-button type="danger" size="small" @click="confirmDelete(kb)">删除</el-button>
          </div>
        </div>
        <div class="knowledge-description">
          <p>{{ kb.description || '暂无描述' }}</p>
        </div>
        <div class="knowledge-meta">
          <div class="file-types">
            <span>支持文件类型: </span>
            <el-tag v-for="type in kb.fileTypes" :key="type" size="small" effect="plain">{{ type }}</el-tag>
          </div>
          <div class="create-time">创建时间: {{ formatDate(kb.createTime) }}</div>
        </div>
      </el-card>

      <div v-if="knowledgeBases.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无知识库，点击上方按钮创建"></el-empty>
      </div>
    </div>

    <!-- 创建/编辑知识库对话框 -->
    <el-dialog :title="dialogMode === 'create' ? '创建知识库' : '编辑知识库'" v-model="dialogVisible" width="500px">
      <el-form :model="knowledgeForm" label-width="80px" ref="knowledgeFormRef">
        <el-form-item label="名称" prop="name" :rules="[{ required: true, message: '请输入知识库名称', trigger: 'blur' }]">
          <el-input v-model="knowledgeForm.name" placeholder="请输入知识库名称"></el-input>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input type="textarea" v-model="knowledgeForm.description" placeholder="请输入知识库描述"></el-input>
        </el-form-item>
        <el-form-item label="文件类型" prop="fileTypes">
          <el-select v-model="knowledgeForm.fileTypes" multiple placeholder="请选择支持的文件类型">
            <el-option v-for="item in fileTypeOptions" :key="item.value" :label="item.label" :value="item.value"></el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitKnowledgeForm">确认</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 上传文档对话框 -->
    <el-dialog title="上传文档" v-model="uploadDialogVisible" width="500px">
      <el-tabs v-model="activeUploadTab">
        <el-tab-pane label="文件上传" name="file">
          <div class="upload-container">
            <el-upload
              class="upload-area"
              drag
              :action="uploadUrl"
              :headers="uploadHeaders"
              :data="uploadData"
              :before-upload="beforeUpload"
              :on-success="handleUploadSuccess"
              :on-error="handleUploadError"
              :on-progress="handleUploadProgress"
              :file-list="fileList"
              multiple
            >
              <el-icon class="el-icon--upload"><i class="el-icon-upload"></i></el-icon>
              <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
              <template #tip>
                <div class="el-upload__tip">
                  支持格式: {{ currentKnowledgeBase?.fileTypes?.join(', ') || '所有格式' }}
                </div>
              </template>
            </el-upload>
          </div>
        </el-tab-pane>
        <el-tab-pane label="代码库上传" name="git">
          <div class="git-upload-container">
            <el-form :model="gitForm" label-width="100px">
              <el-form-item label="仓库地址" required>
                <el-input v-model="gitForm.repoUrl" placeholder="例如: https://github.com/username/repository.git"></el-input>
              </el-form-item>
              <el-form-item label="用户名" required>
                <el-input v-model="gitForm.userName" placeholder="Git用户名"></el-input>
              </el-form-item>
              <el-form-item label="访问令牌" required>
                <el-input v-model="gitForm.token" type="password" placeholder="Git访问令牌" show-password></el-input>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="handleGitUpload" :loading="gitUploading">上传代码库</el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>
        <el-tab-pane label="GitHub直接上传" name="github">
          <div class="git-upload-container">
            <el-form :model="githubForm" label-width="100px">
              <el-form-item label="仓库拥有者" required>
                <el-input v-model="githubForm.repoOwner" placeholder="例如: microsoft"></el-input>
              </el-form-item>
              <el-form-item label="仓库名称" required>
                <el-input v-model="githubForm.repoName" placeholder="例如: vscode"></el-input>
              </el-form-item>
              <el-form-item label="访问令牌" required>
                <el-input v-model="githubForm.token" type="password" placeholder="GitHub访问令牌" show-password></el-input>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="handleGithubUpload" :loading="githubUploading">上传GitHub仓库</el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>
        <el-tab-pane label="本地代码上传" name="localGit">
          <div class="upload-container">
            <el-upload
              class="upload-area"
              drag
              action="/api/v1/ai/ollama/upload_git_zip"
              :headers="uploadHeaders"
              :data="{knowledgeBaseId: currentKnowledgeBase?.id || ''}"
              :before-upload="beforeUploadZip"
              :on-success="handleZipUploadSuccess"
              :on-error="handleZipUploadError"
              :file-list="zipFileList"
              :multiple="false"
              accept=".zip"
            >
              <el-icon class="el-icon--upload"><i class="el-icon-upload"></i></el-icon>
              <div class="el-upload__text">将ZIP格式的代码库拖到此处，或<em>点击上传</em></div>
              <template #tip>
                <div class="el-upload__tip">
                  请将代码库打包成ZIP格式后上传，仅支持ZIP文件
                </div>
              </template>
            </el-upload>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>

<script>
import { 
  getKnowledgeList, 
  createKnowledge, 
  updateKnowledge, 
  deleteKnowledge, 
  uploadFile,
  analyzeGitRepository,
  analyzeGithubRepository
} from '@/api/knowledge';
import { ElMessage, ElMessageBox } from 'element-plus';

export default {
  name: 'KnowledgeView',
  data() {
    return {
      loading: false,
      knowledgeBases: [],
      dialogVisible: false,
      uploadDialogVisible: false,
      dialogMode: 'create', // 'create' or 'edit'
      currentKnowledgeBase: null,
      knowledgeForm: {
        id: '',
        name: '',
        description: '',
        fileTypes: ['pdf', 'doc', 'docx', 'txt', 'md']
      },
      fileTypeOptions: [
        { label: 'PDF', value: 'pdf' },
        { label: 'Word文档', value: 'doc' },
        { label: 'Word文档(DOCX)', value: 'docx' },
        { label: '文本文件', value: 'txt' },
        { label: 'Markdown', value: 'md' },
        { label: 'Excel表格', value: 'xls' },
        { label: 'Excel表格(XLSX)', value: 'xlsx' },
        { label: 'CSV文件', value: 'csv' },
        { label: 'JSON文件', value: 'json' }
      ],
      uploadUrl: '/api/v1/document/upload',
      uploadHeaders: {},
      uploadData: {
        knowledgeBaseId: ''
      },
      fileList: [],
      activeUploadTab: 'file',
      gitForm: {
        repoUrl: '',
        userName: '',
        token: ''
      },
      githubForm: {
        repoOwner: '',
        repoName: '',
        token: ''
      },
      gitUploading: false,
      githubUploading: false,
      zipFileList: []
    };
  },
  created() {
    this.fetchKnowledgeBases();
  },
  methods: {
    // 获取知识库列表
    async fetchKnowledgeBases() {
      this.loading = true;
      try {
        const response = await getKnowledgeList();
        this.knowledgeBases = response.data || [];
      } catch (error) {
        console.error('获取知识库列表失败', error);
        ElMessage.error('获取知识库列表失败');
      } finally {
        this.loading = false;
      }
    },
    
    // 显示创建对话框
    showCreateDialog() {
      this.dialogMode = 'create';
      this.knowledgeForm = {
        id: '',
        name: '',
        description: '',
        fileTypes: ['pdf', 'doc', 'docx', 'txt', 'md']
      };
      this.dialogVisible = true;
    },
    
    // 显示编辑对话框
    showEditDialog(knowledgeBase) {
      this.dialogMode = 'edit';
      this.knowledgeForm = {
        id: knowledgeBase.id,
        name: knowledgeBase.name,
        description: knowledgeBase.description || '',
        fileTypes: knowledgeBase.fileTypes || ['pdf', 'doc', 'docx', 'txt', 'md']
      };
      this.dialogVisible = true;
    },
    
    // 提交知识库表单
    async submitKnowledgeForm() {
      try {
        if (this.dialogMode === 'create') {
          await createKnowledge(this.knowledgeForm);
          ElMessage.success('创建知识库成功');
        } else {
          await updateKnowledge(this.knowledgeForm);
          ElMessage.success('更新知识库成功');
        }
        this.dialogVisible = false;
        this.fetchKnowledgeBases();
      } catch (error) {
        console.error('提交知识库表单失败', error);
        ElMessage.error('提交失败: ' + (error.message || '未知错误'));
      }
    },
    
    // 确认删除
    confirmDelete(knowledgeBase) {
      ElMessageBox.confirm(
        `确定要删除知识库 "${knowledgeBase.name}" 吗？`,
        '删除确认',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
      ).then(async () => {
        try {
          await deleteKnowledge(knowledgeBase.id);
          ElMessage.success('删除知识库成功');
          this.fetchKnowledgeBases();
        } catch (error) {
          console.error('删除知识库失败', error);
          ElMessage.error('删除失败: ' + (error.message || '未知错误'));
        }
      }).catch(() => {});
    },
    
    // 显示上传对话框
    showUploadDialog(knowledgeBase) {
      this.currentKnowledgeBase = knowledgeBase;
      this.uploadData.knowledgeBaseId = knowledgeBase.id;
      this.fileList = [];
      this.uploadDialogVisible = true;
    },
    
    // 上传前检查
    beforeUpload(file) {
      if (!this.currentKnowledgeBase) {
        ElMessage.error('请先选择知识库');
        return false;
      }
      
      if (this.currentKnowledgeBase.fileTypes && this.currentKnowledgeBase.fileTypes.length > 0) {
        const extension = file.name.split('.').pop().toLowerCase();
        if (!this.currentKnowledgeBase.fileTypes.includes(extension)) {
          ElMessage.error(`不支持的文件类型: ${extension}`);
          return false;
        }
      }
      
      return true;
    },
    
    // 上传成功处理
    handleUploadSuccess(response, file) {
      ElMessage.success(`文件 ${file.name} 上传成功`);
    },
    
    // 上传错误处理
    handleUploadError(err, file) {
      console.error('上传文件失败', err, file);
      ElMessage.error(`文件 ${file.name} 上传失败`);
    },
    
    // 上传进度处理
    handleUploadProgress(event, file) {
      console.log('上传进度', event.percent, file);
    },
    
    // 格式化日期
    formatDate(dateStr) {
      if (!dateStr) return '未知时间';
      const date = new Date(dateStr);
      return date.toLocaleString();
    },
    
    // 处理代码库上传
    async handleGitUpload() {
      // 验证表单
      if (!this.gitForm.repoUrl.trim()) {
        ElMessage.warning('请输入代码仓库地址');
        return;
      }
      if (!this.gitForm.userName.trim()) {
        ElMessage.warning('请输入Git用户名');
        return;
      }
      if (!this.gitForm.token.trim()) {
        ElMessage.warning('请输入访问令牌');
        return;
      }

      this.gitUploading = true;
      try {
        // 调用API上传代码库
        const response = await analyzeGitRepository({
          repoUrl: this.gitForm.repoUrl,
          userName: this.gitForm.userName,
          token: this.gitForm.token,
          knowledgeBaseId: this.currentKnowledgeBase.id
        });
        
        if (response && response.code === 200) {
          ElMessage.success('代码库上传成功');
          this.uploadDialogVisible = false;
          // 清空表单
          this.gitForm = {
            repoUrl: '',
            userName: '',
            token: ''
          };
        } else {
          ElMessage.error('代码库上传失败: ' + (response.message || '未知错误'));
        }
      } catch (error) {
        console.error('代码库上传失败', error);
        ElMessage.error('代码库上传失败: ' + (error.message || '未知错误'));
      } finally {
        this.gitUploading = false;
      }
    },
    
    // 处理ZIP文件上传
    beforeUploadZip(file) {
      if (!this.currentKnowledgeBase) {
        ElMessage.error('请先选择知识库');
        return false;
      }
      
      const extension = file.name.split('.').pop().toLowerCase();
      if (extension !== 'zip') {
        ElMessage.error('仅支持ZIP格式文件');
        return false;
      }
      
      return true;
    },
    
    // ZIP文件上传成功处理
    handleZipUploadSuccess(response, file) {
      ElMessage.success(`ZIP文件 ${file.name} 上传成功`);
    },
    
    // ZIP文件上传错误处理
    handleZipUploadError(err, file) {
      console.error('上传ZIP文件失败', err, file);
      ElMessage.error(`ZIP文件 ${file.name} 上传失败`);
    },
    
    // 处理GitHub仓库上传
    async handleGithubUpload() {
      // 验证表单
      if (!this.githubForm.repoOwner.trim()) {
        ElMessage.warning('请输入仓库拥有者');
        return;
      }
      if (!this.githubForm.repoName.trim()) {
        ElMessage.warning('请输入仓库名称');
        return;
      }
      if (!this.githubForm.token.trim()) {
        ElMessage.warning('请输入GitHub访问令牌');
        return;
      }

      this.githubUploading = true;
      try {
        // 调用API上传GitHub仓库
        const response = await analyzeGithubRepository({
          repoOwner: this.githubForm.repoOwner,
          repoName: this.githubForm.repoName,
          token: this.githubForm.token,
          knowledgeBaseId: this.currentKnowledgeBase.id
        });
        
        if (response && response.code === 200) {
          ElMessage.success('GitHub仓库上传成功');
          this.uploadDialogVisible = false;
          // 清空表单
          this.githubForm = {
            repoOwner: '',
            repoName: '',
            token: ''
          };
        } else {
          ElMessage.error('GitHub仓库上传失败: ' + (response.message || '未知错误'));
        }
      } catch (error) {
        console.error('GitHub仓库上传失败', error);
        ElMessage.error('GitHub仓库上传失败: ' + (error.message || '未知错误'));
      } finally {
        this.githubUploading = false;
      }
    }
  }
};
</script>

<style scoped>
.knowledge-container {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.knowledge-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.knowledge-card {
  height: 100%;
}

.knowledge-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 10px;
}

.knowledge-header h3 {
  margin: 0;
}

.knowledge-actions {
  display: flex;
  gap: 5px;
}

.knowledge-description {
  margin-bottom: 15px;
  color: #606266;
  min-height: 40px;
}

.knowledge-meta {
  font-size: 0.9em;
  color: #909399;
}

.file-types {
  margin-bottom: 8px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 5px;
}

.empty-state {
  grid-column: 1 / -1;
  padding: 50px 0;
}

.upload-container {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.upload-area {
  width: 100%;
}

.git-upload-container {
  padding: 20px;
}
</style> 