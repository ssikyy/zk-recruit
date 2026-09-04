import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api'
import { ApiError, ensureCsrfToken } from '@/api/http'

/**
 * 登录态与"待执行动作"（§8.4）。
 * 未登录用户点击立即投递时先记录动作，登录成功后继续原流程。
 */
export const useAuthStore = defineStore('auth', () => {
  const user = ref(null)
  const loaded = ref(false)
  const modalVisible = ref(false)
  const modalMode = ref('login')
  const pendingAction = ref(null)
  let bootstrapPromise = null

  const isLoggedIn = computed(() => !!user.value)
  const isCandidate = computed(() => user.value?.role === 'CANDIDATE')
  const isHr = computed(() => user.value?.role === 'HR')
  const isAdmin = computed(() => !!user.value?.hrAdmin)

  /** 并发调用只发一次请求：路由守卫与 App 挂载会同时触发 */
  function bootstrap() {
    if (loaded.value) {
      return Promise.resolve()
    }
    if (!bootstrapPromise) {
      bootstrapPromise = (async () => {
        await ensureCsrfToken()
        try {
          user.value = await authApi.me()
        } catch (error) {
          if (!(error instanceof ApiError) || error.code !== 1002) {
            // 非未登录的错误不掩盖，交给页面提示
            console.warn('获取登录态失败', error)
          }
          user.value = null
        } finally {
          loaded.value = true
          bootstrapPromise = null
        }
      })()
    }
    return bootstrapPromise
  }

  function openModal(mode = 'login', action = null) {
    modalMode.value = mode
    pendingAction.value = action
    modalVisible.value = true
  }

  function closeModal() {
    modalVisible.value = false
    // 关闭弹窗即清除待执行动作（§8.4）
    pendingAction.value = null
  }

  async function login(payload) {
    user.value = await authApi.login(payload)
    return user.value
  }

  async function register(payload) {
    user.value = await authApi.register(payload)
    return user.value
  }

  async function logout() {
    await authApi.logout()
    user.value = null
    await ensureCsrfToken(true)
  }

  function consumePendingAction() {
    const action = pendingAction.value
    pendingAction.value = null
    return action
  }

  return {
    user,
    loaded,
    modalVisible,
    modalMode,
    pendingAction,
    isLoggedIn,
    isCandidate,
    isHr,
    isAdmin,
    bootstrap,
    openModal,
    closeModal,
    login,
    register,
    logout,
    consumePendingAction
  }
})
