<template>
  <div v-if="initializing" v-loading="true" class="job-list-initializing" aria-label="职位列表加载中"></div>
  <div v-else class="job-list">
    <div class="page-hero" :class="heroTone">
      <div class="zk-container">
        <p v-if="activeCategoryName" class="hero-kicker">职位类别</p>
        <h1>{{ heroTitle }}</h1>
        <p>{{ heroDesc }}</p>
      </div>
    </div>

    <div class="zk-container body">
      <el-card shadow="never" class="filter-card">
        <div class="filters">
          <el-input
            v-model="filters.keyword"
            placeholder="搜索职位名称"
            clearable
            style="width: 240px"
            @keyup.enter="reload"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <!-- 类别与地点来自字典，只显示启用项（§7.3） -->
          <el-select v-model="filters.categoryId" placeholder="职位类别" clearable style="width: 170px">
            <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
          <el-select v-model="filters.locationId" placeholder="工作地点" clearable style="width: 150px">
            <el-option v-for="item in locations" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
          <el-select
            v-if="!fixedType"
            v-model="filters.type"
            placeholder="招聘类型"
            clearable
            style="width: 150px"
          >
            <el-option label="社会招聘" value="SOCIAL" />
            <el-option label="校园招聘" value="CAMPUS" />
          </el-select>
          <el-button type="primary" @click="reload">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </div>
      </el-card>

      <div v-loading="loading" class="result">
        <div class="result-head">
          <span>共 {{ total }} 个在招职位</span>
        </div>

        <el-empty v-if="!loading && jobs.length === 0" description="暂无符合条件的职位，试试调整筛选条件" />

        <router-link
          v-for="job in jobs"
          :key="job.id"
          :to="{ name: 'job-detail', params: { id: job.id } }"
          class="job-card"
        >
          <div class="job-main">
            <div class="job-title-line">
              <h3>{{ job.title }}</h3>
              <el-tag size="small" :class="isCampusJob(job) ? 'zk-tag-campus' : 'zk-tag-social'" effect="plain">
                {{ isCampusJob(job) ? '校园招聘' : '社会招聘' }}
              </el-tag>
              <el-tag v-if="job.categoryName" size="small" effect="plain">{{ job.categoryName }}</el-tag>
            </div>
            <div class="job-meta">
              <span><el-icon><LocationInformation /></el-icon>{{ job.locationName }}</span>
              <span><el-icon><Files /></el-icon>{{ job.categoryName }}</span>
              <span v-if="isCampusJob(job)"><el-icon><User /></el-icon>{{ audienceLabel(job.targetAudience) }}</span>
              <span v-else><el-icon><Clock /></el-icon>{{ job.experience || '经验不限' }}</span>
              <span><el-icon><Reading /></el-icon>{{ job.education }}</span>
              <span v-if="isCampusJob(job) && job.graduationYear">
                <el-icon><Calendar /></el-icon>{{ job.graduationYear }}
              </span>
            </div>
          </div>
          <div class="job-side">
            <div class="publish-time">{{ (job.publishedAt || '').slice(0, 10) }}</div>
            <el-button text type="primary">查看详情 →</el-button>
          </div>
        </router-link>

        <el-pagination
          v-if="total > filters.size"
          class="pager"
          layout="prev, pager, next, total"
          :total="total"
          :page-size="filters.size"
          :current-page="filters.page"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { publicApi } from '@/api'
import { toast } from '@/api/http'

const props = defineProps({
  type: { type: String, default: null }
})

const route = useRoute()
const router = useRouter()

const fixedType = computed(() => (props.type === 'SOCIAL' || props.type === 'CAMPUS' ? props.type : null))

const jobs = ref([])
const total = ref(0)
const initializing = ref(true)
const loading = ref(false)
const categories = ref([])
const locations = ref([])

const filters = reactive({
  keyword: '',
  categoryId: null,
  locationId: null,
  type: null,
  page: 1,
  size: 10
})

const activeCategoryName = computed(() => {
  if (!filters.categoryId) return ''
  return categories.value.find((item) => item.id === filters.categoryId)?.name || ''
})

const heroTone = computed(() => {
  if (fixedType.value === 'CAMPUS' || filters.type === 'CAMPUS') return 'campus'
  if (fixedType.value === 'SOCIAL' || filters.type === 'SOCIAL') return 'social'
  return 'mixed'
})

const heroTitle = computed(() => {
  if (activeCategoryName.value) return activeCategoryName.value
  if (fixedType.value === 'CAMPUS') return '校园招聘'
  if (fixedType.value === 'SOCIAL') return '社会招聘'
  return '在招职位'
})

const heroDesc = computed(() => {
  if (activeCategoryName.value) {
    return `正在展示「${activeCategoryName.value}」类别下的在招职位，可继续按社招/校招或地点缩小范围。`
  }
  if (fixedType.value === 'CAMPUS') {
    return '从校园到职场的第一步，我们提供导师制、轮岗机会与系统培养。'
  }
  if (fixedType.value === 'SOCIAL') {
    return '带着你的经验与判断力加入，用专业能力创造真实价值。'
  }
  return '社会招聘与校园招聘的在招职位都在这里，可按类别、地点继续筛选。'
})

function parseCategoryId(value) {
  if (value == null || value === '') return null
  const id = Number(value)
  return Number.isFinite(id) ? id : null
}

function applyRouteQuery() {
  filters.categoryId = parseCategoryId(route.query.categoryId)
  if (!fixedType.value) {
    const qType = route.query.type
    filters.type = qType === 'SOCIAL' || qType === 'CAMPUS' ? qType : null
  } else {
    filters.type = fixedType.value
  }
}

function syncQuery() {
  const query = {}
  if (filters.categoryId) query.categoryId = String(filters.categoryId)
  if (!fixedType.value && filters.type) query.type = filters.type
  const same =
    String(route.query.categoryId || '') === String(query.categoryId || '') &&
    String(route.query.type || '') === String(query.type || '')
  if (!same) {
    router.replace({ name: route.name, query })
  }
}

async function loadDicts() {
  try {
    const [cats, locs] = await Promise.all([publicApi.categories(), publicApi.locations()])
    categories.value = cats || []
    locations.value = locs || []
  } catch (error) {
    toast(error, '筛选条件加载失败')
  }
}

async function loadJobs() {
  loading.value = true
  try {
    const type = fixedType.value || filters.type || undefined
    const data = await publicApi.jobs({
      type,
      keyword: filters.keyword || undefined,
      categoryId: filters.categoryId || undefined,
      locationId: filters.locationId || undefined,
      page: filters.page,
      size: filters.size
    })
    jobs.value = data?.list || []
    total.value = data?.total || 0
  } catch (error) {
    toast(error, '职位加载失败')
  } finally {
    loading.value = false
  }
}

function reload() {
  filters.page = 1
  syncQuery()
  loadJobs()
}

function resetFilters() {
  filters.keyword = ''
  filters.locationId = null
  filters.categoryId = null
  filters.type = fixedType.value
  reload()
}

function handlePageChange(page) {
  filters.page = page
  loadJobs()
}

function isCampusJob(job) {
  return job.recruitmentType === 'CAMPUS'
}

function audienceLabel(value) {
  if (value === 'GRADUATE') return '应届生'
  if (value === 'INTERN') return '实习生'
  return '不限'
}

watch(
  () => props.type,
  () => {
    filters.keyword = ''
    filters.locationId = null
    applyRouteQuery()
    reload()
  }
)

watch(
  () => [route.query.categoryId, route.query.type],
  () => {
    const nextCategory = parseCategoryId(route.query.categoryId)
    const nextType = route.query.type === 'SOCIAL' || route.query.type === 'CAMPUS' ? route.query.type : null
    const categoryChanged = nextCategory !== filters.categoryId
    const typeChanged = !fixedType.value && nextType !== filters.type
    if (categoryChanged || typeChanged) {
      applyRouteQuery()
      filters.page = 1
      loadJobs()
    }
  }
)

onMounted(async () => {
  applyRouteQuery()
  try {
    await Promise.all([loadDicts(), loadJobs()])
  } finally {
    initializing.value = false
  }
})
</script>

<style scoped>
.job-list-initializing {
  min-height: calc(100vh - var(--zk-header-height));
  background: var(--zk-bg-soft);
}

.page-hero {
  padding: 56px 0 44px;
  color: #fff;
}

.page-hero.social {
  background: linear-gradient(135deg, #12266f, #1c48d8);
}

.page-hero.campus {
  background: linear-gradient(135deg, #4c1d95, #7b5cff);
}

.page-hero.mixed {
  background:
    radial-gradient(700px 280px at 12% 20%, rgba(34, 211, 238, 0.18), transparent 60%),
    linear-gradient(135deg, #071029, #142c86 70%);
}

.hero-kicker {
  margin: 0 0 8px;
  font-size: 12px;
  letter-spacing: 3px;
  color: rgba(255, 255, 255, 0.7);
}

.page-hero h1 {
  margin: 0 0 10px;
  font-size: 34px;
}

.page-hero p {
  margin: 0;
  color: rgba(255, 255, 255, 0.85);
  font-size: 14px;
}

.body {
  padding: 28px 24px 60px;
}

.filter-card {
  border-radius: var(--zk-radius);
  border: 1px solid var(--zk-border);
}

.filters {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  align-items: center;
}

.result-head {
  margin: 24px 0 14px;
  font-size: 13px;
  color: var(--zk-text-muted);
}

.job-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  padding: 22px 24px;
  border: 1px solid var(--zk-border);
  border-radius: var(--zk-radius);
  background: #fff;
  margin-bottom: 14px;
  transition: box-shadow 0.2s, transform 0.2s, border-color 0.2s;
}

.job-card:hover {
  box-shadow: var(--zk-shadow);
  border-color: rgba(28, 72, 216, 0.35);
  transform: translateY(-2px);
}

.job-title-line {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.job-title-line h3 {
  margin: 0;
  font-size: 18px;
}

.job-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  font-size: 13px;
  color: var(--zk-text-muted);
}

.job-meta span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.job-side {
  text-align: right;
  flex-shrink: 0;
}

.publish-time {
  font-size: 12px;
  color: var(--zk-text-muted);
  margin-bottom: 6px;
}

.pager {
  margin-top: 24px;
  justify-content: center;
}

@media (max-width: 640px) {
  .job-card {
    flex-direction: column;
    align-items: flex-start;
  }
  .job-side {
    text-align: left;
  }
}
</style>
