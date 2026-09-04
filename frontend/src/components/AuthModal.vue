<template>
  <el-dialog
    v-model="visible"
    :width="modalWidth"
    :close-on-click-modal="true"
    align-center
    class="auth-dialog"
    @closed="handleClosed"
  >
    <template #header>
      <div class="auth-header">
        <h3>{{ previewOnly ? '只读演示' : isLogin ? '登录' : '注册' }}</h3>
        <p v-if="previewOnly">当前入口可浏览首页和职位。登录、注册和投递将在 HTTPS 域名可用后开放。</p>
        <p v-else>{{ isLogin ? '登录后可维护简历并投递职位' : '注册即可创建求职者账号，无需邮箱或短信验证' }}</p>
      </div>
    </template>

    <el-form
      v-if="!previewOnly && isLogin"
      ref="loginFormRef"
      :model="loginForm"
      :rules="loginRules"
      label-position="top"
      @submit.prevent
    >
      <el-form-item label="邮箱" prop="email">
        <el-input v-model="loginForm.email" placeholder="请输入注册邮箱" autocomplete="username" />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input
          v-model="loginForm.password"
          type="password"
          show-password
          placeholder="请输入密码"
          autocomplete="current-password"
          @keyup.enter="submitLogin"
        />
      </el-form-item>
      <el-alert v-if="errorMessage" :title="errorMessage" type="error" show-icon :closable="false" />
      <el-button type="primary" class="submit-btn" :loading="loading" @click="submitLogin">
        登录
      </el-button>
      <div class="switch-line">
        还没有账号？<el-link type="primary" @click="switchMode('register')">立即注册</el-link>
      </div>
      <p class="hint">忘记密码请联系页脚的招聘联系方式，由管理员协助重置。</p>
    </el-form>

    <el-form
      v-else-if="!previewOnly"
      ref="registerFormRef"
      :model="registerForm"
      :rules="registerRules"
      label-position="top"
      @submit.prevent
    >
      <el-form-item label="姓名" prop="name">
        <el-input v-model="registerForm.name" placeholder="请输入真实姓名" />
      </el-form-item>
      <el-form-item label="邮箱" prop="email">
        <el-input v-model="registerForm.email" placeholder="将作为登录账号" />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input v-model="registerForm.password" type="password" show-password placeholder="8-20 位" />
      </el-form-item>
      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input
          v-model="registerForm.confirmPassword"
          type="password"
          show-password
          placeholder="请再次输入密码"
          @keyup.enter="submitRegister"
        />
      </el-form-item>
      <el-form-item prop="agreePrivacy">
        <el-checkbox v-model="registerForm.agreePrivacy">
          我已阅读并同意
          <el-link type="primary" :underline="false">隐私政策</el-link>
        </el-checkbox>
      </el-form-item>
      <el-alert v-if="errorMessage" :title="errorMessage" type="error" show-icon :closable="false" />
      <el-button type="primary" class="submit-btn" :loading="loading" @click="submitRegister">
        注册并登录
      </el-button>
      <div class="switch-line">
        已有账号？<el-link type="primary" @click="switchMode('login')">返回登录</el-link>
      </div>
      <p class="hint">本系统为演示环境，请勿提交真实个人信息。</p>
    </el-form>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { ApiError } from '@/api/http'

const auth = useAuthStore()
const router = useRouter()
// 公网 HTTP 预览不显示凭据输入框；本地开发仍可正常登录。
const previewOnly = import.meta.env.PROD && window.location.protocol !== 'https:' &&
  !['localhost', '127.0.0.1', '[::1]'].includes(window.location.hostname)

const loginFormRef = ref()
const registerFormRef = ref()
const loading = ref(false)
const errorMessage = ref('')

const visible = computed({
  get: () => auth.modalVisible,
  set: (value) => {
    if (!value) {
      auth.closeModal()
    }
  }
})
const isLogin = computed(() => auth.modalMode === 'login')
const modalWidth = computed(() => (window.innerWidth < 640 ? '92%' : '420px'))

const loginForm = reactive({ email: '', password: '' })
const registerForm = reactive({
  name: '',
  email: '',
  password: '',
  confirmPassword: '',
  agreePrivacy: false
})

const loginRules = {
  email: [
    { required: true, message: '请填写邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  password: [{ required: true, message: '请填写密码', trigger: 'blur' }]
}

const registerRules = {
  name: [{ required: true, min: 2, max: 20, message: '姓名长度需为 2-20 位', trigger: 'blur' }],
  email: [
    { required: true, message: '请填写邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  password: [{ required: true, min: 8, max: 20, message: '密码长度需为 8-20 位', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== registerForm.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  agreePrivacy: [
    {
      validator: (_rule, value, callback) =>
        value ? callback() : callback(new Error('请先同意隐私政策')),
      trigger: 'change'
    }
  ]
}

watch(
  () => auth.modalVisible,
  (opened) => {
    if (opened) {
      errorMessage.value = ''
    }
  }
)

function switchMode(mode) {
  auth.modalMode = mode
  errorMessage.value = ''
}

function handleClosed() {
  // 关闭弹窗不清空已填写的非敏感内容（§8.1），仅清除密码
  loginForm.password = ''
  registerForm.password = ''
  registerForm.confirmPassword = ''
  errorMessage.value = ''
}

async function submitLogin() {
  const valid = await loginFormRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  errorMessage.value = ''
  try {
    const user = await auth.login({ email: loginForm.email, password: loginForm.password })
    afterAuth(user)
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : '登录失败，请稍后再试'
  } finally {
    loading.value = false
  }
}

async function submitRegister() {
  const valid = await registerFormRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  errorMessage.value = ''
  try {
    const user = await auth.register({ ...registerForm })
    afterAuth(user)
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : '注册失败，请稍后再试'
  } finally {
    loading.value = false
  }
}

/**
 * 登录成功后的分流（§8.2、§8.4）：
 * HR 进入后台；求职者停留当前页面；若此前触发过"立即投递"则继续该职位的投递流程。
 */
function afterAuth(user) {
  const action = auth.consumePendingAction()
  auth.modalVisible = false
  ElMessage.success(`欢迎，${user.name}`)

  if (user.role === 'HR') {
    router.push({ name: 'hr-dashboard' })
    return
  }
  if (action && action.type === 'APPLY_JOB') {
    router.push({ name: 'job-detail', params: { id: action.jobId }, query: { apply: '1' } })
  }
}
</script>

<style scoped>
.auth-header h3 {
  margin: 0 0 6px;
  font-size: 20px;
}

.auth-header p {
  margin: 0;
  font-size: 13px;
  color: var(--zk-text-muted);
}

.submit-btn {
  width: 100%;
  margin-top: 16px;
  height: 42px;
  font-size: 15px;
}

.switch-line {
  margin-top: 14px;
  font-size: 13px;
  color: var(--zk-text-muted);
  text-align: center;
}

.hint {
  margin: 10px 0 0;
  font-size: 12px;
  color: var(--zk-text-muted);
  text-align: center;
  line-height: 1.6;
}

:deep(.el-alert) {
  margin-top: 4px;
}
</style>
