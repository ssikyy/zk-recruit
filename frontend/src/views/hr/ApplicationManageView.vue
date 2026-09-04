<template>
  <div class="application-manage">
    <div class="head">
      <h2>投递管理</h2>
    </div>

    <el-card shadow="never" class="filter-card">
      <div class="filters">
        <el-radio-group v-model="query.scope" @change="reload">
          <el-radio-button value="MINE">我负责的</el-radio-button>
          <el-radio-button value="ALL">全部</el-radio-button>
        </el-radio-group>
        <el-input
          v-model="query.keyword"
          placeholder="候选人姓名"
          clearable
          style="width: 180px"
          @keyup.enter="reload"
        />
        <el-select v-model="query.status" placeholder="投递状态" clearable style="width: 140px" @change="reload">
          <el-option label="已投递" value="SUBMITTED" />
          <el-option label="已查看" value="VIEWED" />
          <el-option label="待面试" value="INTERVIEW" />
          <el-option label="已通过" value="PASSED" />
          <el-option label="不合适" value="REJECTED" />
          <el-option label="已撤回" value="WITHDRAWN" />
        </el-select>
        <el-select v-model="query.type" placeholder="招聘类型" clearable style="width: 130px" @change="reload">
          <el-option label="社会招聘" value="SOCIAL" />
          <el-option label="校园招聘" value="CAMPUS" />
        </el-select>
        <!-- 默认不显示已撤回记录（§9.6） -->
        <el-checkbox v-model="query.includeWithdrawn" @change="reload">包含已撤回</el-checkbox>
        <el-button type="primary" @click="reload">查询</el-button>
        <el-button @click="resetFilters">重置</el-button>
      </div>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table :data="rows" v-loading="loading" stripe @row-dblclick="openDetail">
        <el-table-column prop="candidateName" label="候选人" width="110" />
        <el-table-column prop="candidatePhone" label="手机号" width="130" />
        <el-table-column prop="jobTitle" label="投递职位" min-width="180">
          <template #default="{ row }">
            <div class="title-cell">
              <span>{{ row.jobTitle }}</span>
              <el-tag size="small" :class="row.recruitmentType === 'CAMPUS' ? 'zk-tag-campus' : 'zk-tag-social'" effect="plain">
                {{ row.recruitmentType === 'CAMPUS' ? '校招' : '社招' }}
              </el-tag>
              <el-tag v-if="row.attemptNo > 1" size="small" type="info" effect="plain">
                第 {{ row.attemptNo }} 次
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="ownerName" label="职位负责人" width="110" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" effect="light" size="small">{{ row.statusLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="appliedAt" label="投递时间" width="160" />
        <el-table-column prop="lastHandledAt" label="最近处理" width="160">
          <template #default="{ row }">{{ row.lastHandledAt || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openDetail(row)">查看</el-button>
            <el-tooltip v-if="!row.canWrite" :content="withdrawnOrOwnerTip(row)" placement="top">
              <span><el-button text disabled>处理</el-button></span>
            </el-tooltip>
            <el-button v-else text type="success" @click="openDetail(row)">处理</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pager"
        layout="prev, pager, next, total"
        :total="total"
        :page-size="query.size"
        :current-page="query.page"
        @current-change="handlePage"
      />
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { hrApi } from '@/api'
import { toast } from '@/api/http'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const rows = ref([])
const total = ref(0)
const loading = ref(false)

const query = reactive({
  scope: route.query.scope || (auth.isAdmin ? 'ALL' : 'MINE'),
  keyword: '',
  status: route.query.status || '',
  type: '',
  jobId: route.query.jobId ? Number(route.query.jobId) : null,
  includeWithdrawn: false,
  page: 1,
  size: 10
})

function statusType(status) {
  return (
    {
      SUBMITTED: 'info',
      VIEWED: 'primary',
      INTERVIEW: 'warning',
      PASSED: 'success',
      REJECTED: 'danger',
      WITHDRAWN: 'info'
    }[status] || 'info'
  )
}

function withdrawnOrOwnerTip(row) {
  return row.status === 'WITHDRAWN' ? '候选人已撤回，记录为只读' : '仅职位负责人可操作'
}

async function load() {
  loading.value = true
  try {
    const data = await hrApi.applications({
      scope: query.scope,
      keyword: query.keyword || undefined,
      status: query.status || undefined,
      type: query.type || undefined,
      jobId: query.jobId || undefined,
      includeWithdrawn: query.includeWithdrawn,
      page: query.page,
      size: query.size
    })
    rows.value = data?.list || []
    total.value = data?.total || 0
  } catch (error) {
    toast(error, '投递记录加载失败')
  } finally {
    loading.value = false
  }
}

function reload() {
  query.page = 1
  load()
}

function resetFilters() {
  query.keyword = ''
  query.status = ''
  query.type = ''
  query.jobId = null
  query.includeWithdrawn = false
  reload()
}

function handlePage(page) {
  query.page = page
  load()
}

function openDetail(row) {
  router.push({ name: 'hr-application-detail', params: { id: row.id } })
}

onMounted(load)
</script>

<style scoped>
.head h2 {
  margin: 0 0 16px;
  font-size: 22px;
}

.filter-card,
.table-card {
  border-radius: var(--zk-radius);
  border: 1px solid var(--zk-border);
}

.table-card {
  margin-top: 14px;
}

.filters {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  align-items: center;
}

.title-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
