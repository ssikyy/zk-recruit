<template>
  <div class="dashboard" v-loading="loading">
    <div class="head">
      <h2>工作台</h2>
      <!-- 视图范围切换：普通 HR 默认"我负责的"，管理员默认"全部"（§10.2） -->
      <el-radio-group v-model="scope" @change="load">
        <el-radio-button value="MINE">我负责的</el-radio-button>
        <el-radio-button value="ALL">全部</el-radio-button>
      </el-radio-group>
    </div>

    <div class="cards">
      <el-card
        v-for="card in cards"
        :key="card.key"
        shadow="never"
        class="stat-card"
        :class="card.key"
        @click="card.action && card.action()"
      >
        <div class="stat-label">{{ card.label }}</div>
        <div class="stat-value">{{ data[card.key] ?? '-' }}</div>
        <div class="stat-desc">{{ card.desc }}</div>
      </el-card>
    </div>

    <el-alert
      v-if="auth.isAdmin && data.jobsWithDisabledOwner > 0"
      class="warn"
      type="warning"
      show-icon
      :closable="false"
      :title="`有 ${data.jobsWithDisabledOwner} 个职位的负责人已被停用，请尽快转移归属`"
    />

    <el-card shadow="never" class="quick">
      <h3>快捷入口</h3>
      <div class="quick-actions">
        <el-button type="primary" @click="$router.push({ name: 'hr-job-new' })">
          <el-icon><Plus /></el-icon>发布职位
        </el-button>
        <el-button @click="goApplications('SUBMITTED')">查看待处理投递</el-button>
        <el-button @click="goApplications('INTERVIEW')">查看待面试候选人</el-button>
      </div>
      <p class="metric-note">
        指标口径：待处理 = 已投递；待面试 = 待面试且未填写面试结果；已通过为累计值。全部指标均不含已撤回记录。
      </p>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { hrApi } from '@/api'
import { toast } from '@/api/http'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const loading = ref(false)
const data = ref({})
const scope = ref(auth.isAdmin ? 'ALL' : 'MINE')

const cards = computed(() => [
  {
    key: 'publishedJobs',
    label: '招聘中职位',
    desc: '状态为招聘中的职位数',
    action: () => router.push({ name: 'hr-jobs', query: { status: 'PUBLISHED', scope: scope.value } })
  },
  {
    key: 'pendingApplications',
    label: '待处理投递',
    desc: '状态为已投递，等待查看',
    action: () => goApplications('SUBMITTED')
  },
  {
    key: 'pendingInterviews',
    label: '待面试',
    desc: '已安排面试且未填写结果',
    action: () => goApplications('INTERVIEW')
  },
  {
    key: 'passedApplications',
    label: '已通过',
    desc: '累计通过人数',
    action: () => goApplications('PASSED')
  }
])

function goApplications(status) {
  router.push({ name: 'hr-applications', query: { status, scope: scope.value } })
}

async function load() {
  loading.value = true
  try {
    data.value = await hrApi.dashboard(scope.value)
  } catch (error) {
    toast(error, '工作台加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
}

.head h2 {
  margin: 0;
  font-size: 22px;
}

.cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  border-radius: var(--zk-radius);
  border: 1px solid var(--zk-border);
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--zk-shadow);
}

.stat-label {
  font-size: 13px;
  color: var(--zk-text-muted);
}

.stat-value {
  font-size: 34px;
  font-weight: 700;
  margin: 6px 0 4px;
  color: var(--zk-primary);
}

.stat-card.pendingApplications .stat-value {
  color: #d97706;
}

.stat-card.pendingInterviews .stat-value {
  color: #7c3aed;
}

.stat-card.passedApplications .stat-value {
  color: #16a34a;
}

.stat-desc {
  font-size: 12px;
  color: var(--zk-text-muted);
}

.warn {
  margin-top: 16px;
}

.quick {
  margin-top: 18px;
  border-radius: var(--zk-radius);
  border: 1px solid var(--zk-border);
}

.quick h3 {
  margin: 0 0 14px;
  font-size: 16px;
}

.quick-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.metric-note {
  margin: 16px 0 0;
  font-size: 12px;
  color: var(--zk-text-muted);
  line-height: 1.7;
}

@media (max-width: 1100px) {
  .cards {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .cards {
    grid-template-columns: 1fr;
  }
}
</style>
