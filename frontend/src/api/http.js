import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * 统一请求层。
 * - Session 认证走 Cookie，因此 withCredentials 必须开启（D1）
 * - 所有写操作自动携带 X-XSRF-TOKEN，失败(1006)时自动重取 token 并重试一次（§15.7）
 * - 统一解包 { code, message, data }，业务失败抛出带 code 的错误
 */
const CSRF_COOKIE = 'XSRF-TOKEN'
const CSRF_HEADER = 'X-XSRF-TOKEN'
const WRITE_METHODS = ['post', 'put', 'delete', 'patch']

const instance = axios.create({
  baseURL: '/api',
  withCredentials: true,
  timeout: 20000
})

function readCookie(name) {
  const matched = document.cookie.match(new RegExp('(^|;\\s*)' + name + '=([^;]*)'))
  return matched ? decodeURIComponent(matched[2]) : ''
}

let csrfPromise = null

export async function ensureCsrfToken(force = false) {
  if (!force && readCookie(CSRF_COOKIE)) {
    return readCookie(CSRF_COOKIE)
  }
  if (!csrfPromise) {
    csrfPromise = axios
      .get('/api/auth/csrf', { withCredentials: true })
      .finally(() => {
        csrfPromise = null
      })
  }
  await csrfPromise
  return readCookie(CSRF_COOKIE)
}

instance.interceptors.request.use(async (config) => {
  const method = (config.method || 'get').toLowerCase()
  if (WRITE_METHODS.includes(method)) {
    const token = await ensureCsrfToken()
    config.headers[CSRF_HEADER] = token
  }
  return config
})

/** 业务错误：携带 code，便于调用方按错误码分支处理 */
export class ApiError extends Error {
  constructor(code, message, data) {
    super(message)
    this.code = code
    this.data = data
  }
}

/** 未登录时的回调，由 auth store 注册，用于自动弹出登录框 */
let unauthorizedHandler = null
export function onUnauthorized(handler) {
  unauthorizedHandler = handler
}

instance.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body.code === 'number' && body.code !== 0) {
      return Promise.reject(new ApiError(body.code, body.message, body.data))
    }
    return body ? body.data : null
  },
  async (error) => {
    const response = error.response
    const body = response && response.data
    const code = body && typeof body.code === 'number' ? body.code : -1
    const message = (body && body.message) || error.message || '请求失败'

    // CSRF token 失效：重新获取后自动重试一次（§18）
    if (code === 1006 && !error.config.__csrfRetried) {
      error.config.__csrfRetried = true
      await ensureCsrfToken(true)
      error.config.headers[CSRF_HEADER] = readCookie(CSRF_COOKIE)
      return instance.request(error.config)
    }

    if (code === 1002 && !error.config.__skipAuthHandler) {
      if (unauthorizedHandler) {
        unauthorizedHandler()
      }
    }
    return Promise.reject(new ApiError(code, message, body && body.data))
  }
)

/** 统一的错误提示，页面里只在需要自定义处理时才自己捕获 */
export function toast(error, fallback = '操作失败') {
  const message = error instanceof ApiError ? error.message : fallback
  ElMessage.error(message || fallback)
}

export default instance
