import axios from 'axios'
import { ElMessage } from 'element-plus'

// 创建axios实例
const service = axios.create({
  baseURL: import.meta.env.VITE_BASE_API || 'http://localhost:8090', // 确保使用正确的后端端口8090
  timeout: 30000 // 请求超时时间
})

// 请求拦截器
service.interceptors.request.use(
  config => {
    // 可以在这里添加请求头等配置
    return config
  },
  error => {
    console.error('Request error:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  response => {
    const res = response.data
    // 检查响应状态码，适配后端的响应码格式
    if (res.code && res.code !== '0000' && res.code !== 0 && res.code !== 200) {
      ElMessage({
        message: res.message || res.info || 'Error',
        type: 'error',
        duration: 5 * 1000
      })
      return Promise.reject(new Error(res.message || res.info || 'Error'))
    } else {
      return res
    }
  },
  error => {
    console.error('Response error:', error)
    ElMessage({
      message: error.message || '网络错误',
      type: 'error',
      duration: 5 * 1000
    })
    return Promise.reject(error)
  }
)

// 导出文档上传地址
export const FILE_UPLOAD_URL = 'http://localhost:8090/api/v1/document/upload'

export default service 