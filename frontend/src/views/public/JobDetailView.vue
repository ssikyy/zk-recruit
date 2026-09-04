<template>
  <div v-loading="loading" class="job-detail">
    <div class="detail-hero" :class="isCampus ? 'campus' : 'social'">
      <div class="zk-container">
        <el-breadcrumb separator="/" class="crumb">
          <el-breadcrumb-item :to="{ name: 'home' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item
            v-if="job.categoryId"
            :to="{ name: 'all-jobs', query: { categoryId: String(job.categoryId) } }"
          >
            {{ job.categoryName || '职位类别' }}
          </el-breadcrumb-item>
          <el-breadcrumb-item :to="{ name: isCampus ? 'campus-jobs' : 'social-jobs' }">
            {{ isCampus ? '校园招聘' : '社会招聘' }}
          </el-breadcrumb-item>
          <el-breadcrumb-item>{{ job.title }}</el-breadcrumb-item>
        </el-breadcrumb>

        <div class="title-line">
          <h1>{{ job.title }}</h1>
          <el-tag effect="light" size="large">{{ isCampus ? '校园招聘' : '社会招聘' }}</el-tag>
          <el-tag v-if="job.status === 'CLOSED'" type="danger" effect="dark" size="large">已停止招聘</el-tag>
        </div>

        <div class="meta">
          <span>{{ job.locationName }}</span>
          <span>{{ job.categoryName }}</span>
          <span>招聘 {{ job.headcount }} 人</span>
          <span>{{ job.education }}</span>
          <span v-if="isCampus">{{ job.graduationYear }} · {{ audienceLabel }}</span>
          <span v-else>{{ job.experience || '经验不限' }}</span>
        </div>
        <p class="publish">发布时间：{{ job.publishedAt || '-' }}</p>
      </div>
    </div>

    <div class="zk-container content">
      <div class="main">
        <section>
          <h2>岗位职责</h2>
          <div class="rich" v-html="job.duty || ''"></div>
        </section>
        <section>
          <h2>任职要求</h2>
          <div class="rich" v-html="job.requirement || ''"></div>
        </section>
      </div>

      <aside class="side">
        <el-card shadow="never" class="apply-card">
          <h3>投递该职位</h3>

          <!-- HR 角色不显示投递入口（§7.5） -->
          <el-alert
            v-if="auth.isHr"
            title="HR 账号不能投递职位"
            type="info"
            :closable="false"
            show-icon
          />
          <template v-else>
            <el-button
              type="primary"
              size="large"
              class="apply-btn"
              :disabled="applyDisabled"
              :loading="applying"
              @click="handleApply"
            >
              {{ applyButtonText }}
            </el-button>

            <p v-if="applyHint" class="apply-hint">{{ applyHint }}</p>

            <el-divider />
            <ul class="notes">
              <li>投递后 HR 会看到你此刻的简历快照。</li>
              <li>投递提交后不可撤销，但可以在"我的投递"中撤回。</li>
              <li>同一职位最多投递 {{ eligibility.maxAttempts || 3 }} 次。</li>
              <li>系统不会发送通知，请留意"我的投递"页面或 HR 电话。</li>
            </ul>
          </template>
        </el-card>
      </aside>
    </div>

    <!-- 简历不完整时的引导（§7.5 第 6 步） -->
    <el-dialog v-model="missingDialog" title="请先完善资料与简历" width="420px">
      <p class="missing-text">投递前需要补全以下内容：</p>
      <ul class="missing-list">
        <li v-for="item in eligibility.missing || []" :key="item">{{ missingLabel(item) }}</li>
      </ul>
      <template #footer>
        <el-button @click="missingDialog = false">稍后再说</el-button>
        <el-button type="primary" @click="goResume">去完善简历</el-button>
      </template>
    </el-dialog>

    <!-- 投递确认（§7.5 第 7 步） -->
    <el-dialog v-model="confirmDialog" title="确认投递" width="440px">
      <p>你将以当前简历投递 <strong>{{ job.title }}</strong>。</p>
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        :title="`提交后 HR 即可看到你的简历快照；撤回后该职位还可再投递 ${remainingAfterApply} 次`"
      />
      <template #footer>
        <el-button @click="confirmDialog = false">取消</el-button>
        <el-button type="primary" :loading="applying" @click="doApply">确认投递</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { candidateApi, publicApi } from '@/api'
import { toast } from '@/api/http'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const job = ref({})
// 首次渲染即进入加载态，避免职位数据返回前展示空详情
const loading = ref(true)
const applying = ref(false)
const eligibility = ref({})
const missingDialog = ref(false)
const confirmDialog = ref(false)

const isCampus = computed(() => job.value.recruitmentType === 'CAMPUS')
const audienceLabel = computed(() =>
  job.value.targetAudience === 'INTERN' ? '实习生' : job.value.targetAudience === 'GRADUATE' ? '应届生' : '不限'
)
const remainingAfterApply = computed(() =>
  Math.max(0, (eligibility.value.remainingAttempts || 1) - 1)
)

const applyDisabled = computed(() => {
  if (job.value.status !== 'PUBLISHED') return true
  if (!auth.isLoggedIn) return false
  const e = eligibility.value
  if (e.hasActiveApplication) return true
  if (e.remainingAttempts === 0) return true
  return false
})

const applyButtonText = computed(() => {
  if (job.value.status !== 'PUBLISHED') return '该职位已停止招聘'
  if (!auth.isLoggedIn) return '立即投递'
  const e = eligibility.value
  if (e.hasActiveApplication) return '已投递'
  if (e.remainingAttempts === 0) return '投递次数已达上限'
  return '立即投递'
})

const applyHint = computed(() => {
  if (job.value.status !== 'PUBLISHED') return '该职位已关闭，仅保留内容供查看。'
  if (!auth.isLoggedIn) return '点击后将弹出登录/注册窗口，登录成功会自动继续投递。'
  const e = eligibility.value
  if (e.hasActiveApplication) return '你已投递该职位，可在"我的投递"中查看进度或撤回。'
  if (e.remainingAttempts === 0) return '该职位投递次数已用完，无法再次投递。'
  if (!e.resumeReady) return '资料或简历尚未完善，点击后会提示需要补全的内容。'
  return `剩余投递次数：${e.remainingAttempts}`
})

async function loadJob() {
  loading.value = true
  try {
    job.value = await publicApi.job(route.params.id)
  } catch (error) {
    toast(error, '职位加载失败')
    router.push({ name: 'home' })
  } finally {
    loading.value = false
  }
}

async function loadEligibility() {
  if (!auth.isCandidate) {
    eligibility.value = {}
    return
  }
  try {
    eligibility.value = await candidateApi.eligibility(route.params.id)
  } catch (error) {
    eligibility.value = {}
  }
}

/** 未登录时保存待执行动作，登录成功后由 AuthModal 带回本页继续（§8.4） */
function handleApply() {
  if (!auth.isLoggedIn) {
    auth.openModal('login', { type: 'APPLY_JOB', jobId: route.params.id })
    return
  }
  if (!eligibility.value.resumeReady) {
    missingDialog.value = true
    return
  }
  confirmDialog.value = true
}

async function doApply() {
  applying.value = true
  try {
    const result = await candidateApi.apply(route.params.id)
    confirmDialog.value = false
    ElMessage.success(`投递成功（第 ${result.attemptNo} 次）`)
    await loadEligibility()
  } catch (error) {
    confirmDialog.value = false
    // 3003 表示简历不完整，改为引导补全（§18）
    if (error.code === 3003) {
      eligibility.value = { ...eligibility.value, missing: error.data || [], resumeReady: false }
      missingDialog.value = true
    } else {
      toast(error, '投递失败')
      await loadEligibility()
    }
  } finally {
    applying.value = false
  }
}

function goResume() {
  missingDialog.value = false
  router.push({ name: 'my-resume' })
}

function missingLabel(code) {
  const map = {
    NAME: '基本资料中的姓名',
    PHONE: '基本资料中的手机号（用于 HR 联系你）',
    RESUME: '在线简历必填部分，或上传一份附件简历'
  }
  return map[code] || code
}

/**
 * 登录后 AuthModal 会带回 apply=1。
 * 仅 query 变化时组件会被复用而不重新挂载，因此必须用 watch 而不是只依赖 onMounted。
 */
async function continuePendingApply() {
  if (route.query.apply !== '1' || !auth.isCandidate) {
    return
  }
  await loadEligibility()
  router.replace({ name: 'job-detail', params: { id: route.params.id } })
  handleApply()
}

onMounted(async () => {
  await loadJob()
  await loadEligibility()
  await continuePendingApply()
})

watch(() => route.query.apply, continuePendingApply)
watch(() => auth.isLoggedIn, loadEligibility)
</script>

<style scoped>
.detail-hero {
  padding: 40px 0 36px;
  color: #fff;
}

.detail-hero.social {
  background: linear-gradient(135deg, #12266f, #1c48d8);
}

.detail-hero.campus {
  background: linear-gradient(135deg, #4c1d95, #7b5cff);
}

.crumb {
  margin-bottom: 18px;
}

.crumb :deep(.el-breadcrumb__inner),
.crumb :deep(.el-breadcrumb__separator) {
  color: rgba(255, 255, 255, 0.72) !important;
}

.crumb :deep(.el-breadcrumb__inner:hover) {
  color: #fff !important;
}

.title-line {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.title-line h1 {
  margin: 0;
  font-size: 30px;
}

.meta {
  display: flex;
  gap: 22px;
  flex-wrap: wrap;
  margin: 16px 0 6px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.88);
}

.publish {
  margin: 0;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.content {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 32px;
  padding: 36px 24px 64px;
  align-items: start;
}

.main section {
  margin-bottom: 34px;
}

.main h2 {
  font-size: 20px;
  margin: 0 0 14px;
  padding-left: 12px;
  border-left: 4px solid var(--zk-primary);
}

.rich {
  font-size: 14px;
  line-height: 1.95;
  color: #374151;
  white-space: pre-wrap;
}

.apply-card {
  border-radius: var(--zk-radius);
  border: 1px solid var(--zk-border);
  position: sticky;
  top: calc(var(--zk-header-height) + 20px);
}

.apply-card h3 {
  margin: 0 0 16px;
  font-size: 17px;
}

.apply-btn {
  width: 100%;
}

.apply-hint {
  margin: 12px 0 0;
  font-size: 12px;
  color: var(--zk-text-muted);
  line-height: 1.7;
}

.notes {
  margin: 0;
  padding-left: 18px;
  font-size: 12px;
  color: var(--zk-text-muted);
  line-height: 1.9;
}

.missing-text {
  margin: 0 0 8px;
}

.missing-list {
  margin: 0;
  padding-left: 20px;
  line-height: 1.9;
  color: var(--zk-primary);
}

@media (max-width: 900px) {
  .content {
    grid-template-columns: 1fr;
  }
  .apply-card {
    position: static;
  }
}
</style>
