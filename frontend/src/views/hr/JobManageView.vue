<template>
  <div class="job-manage">
    <div class="head">
      <h2>职位管理</h2>
      <el-button type="primary" @click="$router.push({ name: 'hr-job-new' })">
        <el-icon><Plus /></el-icon>新增职位
      </el-button>
    </div>

    <el-card shadow="never" class="filter-card">
      <div class="filters">
        <el-radio-group v-model="query.scope" @change="reload">
          <el-radio-button value="MINE">我负责的</el-radio-button>
          <el-radio-button value="ALL">全部</el-radio-button>
        </el-radio-group>
        <el-input v-model="query.keyword" placeholder="职位名称" clearable style="width: 200px" @keyup.enter="reload" />
        <el-select v-model="query.status" placeholder="职位状态" clearable style="width: 140px">
          <el-option label="草稿" value="DRAFT" />
          <el-option label="招聘中" value="PUBLISHED" />
          <el-option label="已关闭" value="CLOSED" />
        </el-select>
        <el-select v-model="query.type" placeholder="招聘类型" clearable style="width: 140px">
          <el-option label="社会招聘" value="SOCIAL" />
          <el-option label="校园招聘" value="CAMPUS" />
        </el-select>
        <el-button type="primary" @click="reload">查询</el-button>
      </div>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="title" label="职位名称" min-width="200">
          <template #default="{ row }">
            <div class="title-cell">
              <span>{{ row.title }}</span>
              <el-tag size="small" :class="row.recruitmentType === 'CAMPUS' ? 'zk-tag-campus' : 'zk-tag-social'" effect="plain">
                {{ row.recruitmentType === 'CAMPUS' ? '校招' : '社招' }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="categoryName" label="类别" width="110" />
        <el-table-column prop="locationName" label="地点" width="90" />
        <el-table-column prop="ownerName" label="负责人" width="110" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" effect="light" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="applicationCount" label="有效投递" width="90" align="center">
          <template #default="{ row }">
            <el-link type="primary" @click="goApplications(row.id)">{{ row.applicationCount }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="publishedAt" label="发布时间" width="160" />
        <el-table-column label="操作" width="290" fixed="right">
          <template #default="{ row }">
            <template v-if="canWrite(row)">
              <el-button text type="primary" @click="edit(row)">编辑</el-button>
              <el-button v-if="row.status === 'DRAFT'" text type="success" @click="changeStatus(row, 'PUBLISHED')">
                发布
              </el-button>
              <el-button v-if="row.status === 'PUBLISHED'" text type="danger" @click="changeStatus(row, 'CLOSED')">
                关闭
              </el-button>
              <el-button
                v-if="row.status === 'PUBLISHED' && row.applicationCount === 0"
                text
                @click="changeStatus(row, 'DRAFT')"
              >
                撤回发布
              </el-button>
              <el-button v-if="row.status === 'CLOSED'" text type="success" @click="changeStatus(row, 'PUBLISHED')">
                重新发布
              </el-button>
              <el-button v-if="auth.isAdmin" text @click="openTransfer(row)">转移负责人</el-button>
            </template>
            <!-- 无权限时按钮置灰并给出原因，而不是直接隐藏（§16.3） -->
            <el-tooltip v-else content="仅职位负责人可操作" placement="top">
              <span><el-button text disabled>编辑</el-button></span>
            </el-tooltip>
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

    <el-dialog v-model="transferDialog" title="转移职位负责人" width="420px">
      <p class="transfer-tip">
        转移后原负责人将立即失去该职位及其投递的写权限。
      </p>
      <el-select v-model="transferForm.ownerHrId" placeholder="选择新的负责人" style="width: 100%">
        <el-option
          v-for="item in hrOptions"
          :key="item.id"
          :label="item.name + (item.hrAdmin ? '（管理员）' : '')"
          :value="item.id"
        />
      </el-select>
      <template #footer>
        <el-button @click="transferDialog = false">取消</el-button>
        <el-button type="primary" :loading="transferring" @click="doTransfer">确认转移</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { hrApi } from '@/api'
import { toast } from '@/api/http'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const hrOptions = ref([])

const query = reactive({
  scope: route.query.scope || (auth.isAdmin ? 'ALL' : 'MINE'),
  keyword: '',
  status: route.query.status || '',
  type: '',
  page: 1,
  size: 10
})

const transferDialog = ref(false)
const transferring = ref(false)
const transferForm = reactive({ jobId: null, ownerHrId: null, version: 0 })

function statusLabel(status) {
  return { DRAFT: '草稿', PUBLISHED: '招聘中', CLOSED: '已关闭' }[status] || status
}

function statusType(status) {
  return { DRAFT: 'info', PUBLISHED: 'success', CLOSED: 'danger' }[status] || 'info'
}

function canWrite(row) {
  return auth.isAdmin || row.ownerHrId === auth.user?.userId
}

async function load() {
  loading.value = true
  try {
    const data = await hrApi.jobs({
      scope: query.scope,
      keyword: query.keyword || undefined,
      status: query.status || undefined,
      type: query.type || undefined,
      page: query.page,
      size: query.size
    })
    rows.value = data?.list || []
    total.value = data?.total || 0
  } catch (error) {
    toast(error, '职位加载失败')
  } finally {
    loading.value = false
  }
}

function reload() {
  query.page = 1
  load()
}

function handlePage(page) {
  query.page = page
  load()
}

function edit(row) {
  router.push({ name: 'hr-job-edit', params: { id: row.id } })
}

function goApplications(jobId) {
  router.push({ name: 'hr-applications', query: { jobId, scope: 'ALL' } })
}

async function changeStatus(row, target) {
  const labels = { PUBLISHED: '发布', CLOSED: '关闭', DRAFT: '撤回发布' }
  if (target === 'CLOSED' || target === 'DRAFT') {
    try {
      await ElMessageBox.confirm(
        target === 'CLOSED'
          ? '关闭后该职位不再接受新投递，但历史投递仍可继续处理。'
          : '撤回发布后职位回到草稿状态，官网不再展示。',
        `确认${labels[target]}？`,
        { type: 'warning' }
      )
    } catch {
      return
    }
  }
  try {
    await hrApi.updateJobStatus(row.id, { targetStatus: target, version: row.version })
    ElMessage.success(`已${labels[target]}`)
    load()
  } catch (error) {
    toast(error, '操作失败')
    load()
  }
}

async function openTransfer(row) {
  transferForm.jobId = row.id
  transferForm.version = row.version
  transferForm.ownerHrId = null
  if (!hrOptions.value.length) {
    try {
      hrOptions.value = await hrApi.hrOptions()
    } catch (error) {
      toast(error, '负责人列表加载失败')
    }
  }
  transferDialog.value = true
}

async function doTransfer() {
  if (!transferForm.ownerHrId) {
    ElMessage.warning('请选择新的负责人')
    return
  }
  transferring.value = true
  try {
    await hrApi.transferOwner(transferForm.jobId, {
      ownerHrId: transferForm.ownerHrId,
      version: transferForm.version
    })
    transferDialog.value = false
    ElMessage.success('负责人已转移')
    load()
  } catch (error) {
    toast(error, '转移失败')
    load()
  } finally {
    transferring.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.head h2 {
  margin: 0;
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
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

.transfer-tip {
  margin: 0 0 14px;
  font-size: 13px;
  color: var(--zk-text-muted);
  line-height: 1.7;
}
</style>
