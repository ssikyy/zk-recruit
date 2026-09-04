<template>
  <div class="zk-container my-applications">
    <div class="page-head">
      <h1>我的投递</h1>
      <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 160px" @change="reload">
        <el-option label="已投递" value="SUBMITTED" />
        <el-option label="处理中" value="VIEWED" />
        <el-option label="待面试" value="INTERVIEW" />
        <el-option label="已通过" value="PASSED" />
        <el-option label="暂不匹配" value="REJECTED" />
        <el-option label="已撤回" value="WITHDRAWN" />
      </el-select>
    </div>

    <!-- 第一版无任何通知能力，页面固定提示（§9.4） -->
    <el-alert
      class="notice"
      type="info"
      show-icon
      :closable="false"
      title="面试安排更新后不会发送通知，请留意本页面或 HR 的电话联系。"
    />

    <div v-loading="loading">
      <el-empty v-if="!loading && rows.length === 0" description="还没有投递记录，去看看在招职位吧" />

      <el-card v-for="row in rows" :key="row.id" shadow="never" class="app-card">
        <div class="card-main">
          <div class="left">
            <div class="title-line">
              <h3>{{ row.jobTitle }}</h3>
              <el-tag size="small" :class="row.recruitmentType === 'CAMPUS' ? 'zk-tag-campus' : 'zk-tag-social'" effect="plain">
                {{ row.recruitmentType === 'CAMPUS' ? '校园招聘' : '社会招聘' }}
              </el-tag>
              <el-tag v-if="row.attemptNo > 1" size="small" type="info" effect="plain">
                第 {{ row.attemptNo }} 次投递
              </el-tag>
            </div>
            <div class="meta">
              <span>{{ row.locationName }}</span>
              <span>投递于 {{ row.appliedAt }}</span>
              <span v-if="row.withdrawnAt">撤回于 {{ row.withdrawnAt }}</span>
            </div>
            <div v-if="row.interviewTime" class="interview-line">
              <el-icon><Calendar /></el-icon>
              面试时间：{{ row.interviewTime }}
            </div>
          </div>
          <div class="right">
            <el-tag :type="statusType(row.status)" effect="dark" size="large">{{ row.statusLabel }}</el-tag>
            <div class="actions">
              <el-button text type="primary" @click="openDetail(row.id)">查看详情</el-button>
              <!-- 撤回入口仅在允许状态下出现（§9.6） -->
              <el-button v-if="row.canWithdraw" text type="danger" @click="openWithdraw(row)">
                撤回投递
              </el-button>
            </div>
          </div>
        </div>
      </el-card>

      <el-pagination
        v-if="total > pageSize"
        class="pager"
        layout="prev, pager, next, total"
        :total="total"
        :page-size="pageSize"
        :current-page="page"
        @current-change="handlePage"
      />
    </div>

    <!-- 撤回确认（§9.6：必须说明记录保留且 HR 可见） -->
    <el-dialog v-model="withdrawDialog" title="撤回投递" width="440px">
      <el-alert
        type="warning"
        show-icon
        :closable="false"
        title="撤回后本次投递记录仍会保留，HR 可以看到你已撤回，且不可恢复。"
      />
      <el-form label-position="top" class="withdraw-form">
        <el-form-item label="撤回原因（选填）">
          <el-select v-model="withdrawForm.reason" placeholder="请选择" clearable style="width: 100%">
            <el-option label="已找到其他工作" value="FOUND_OTHER_JOB" />
            <el-option label="投递错误" value="WRONG_APPLY" />
            <el-option label="暂不考虑该岗位" value="NOT_INTERESTED" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注（选填）">
          <el-input v-model="withdrawForm.remark" type="textarea" :rows="2" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <p class="withdraw-tip">
        撤回后该职位还可再投递 {{ remainingHint }} 次。
      </p>
      <template #footer>
        <el-button @click="withdrawDialog = false">取消</el-button>
        <el-button type="danger" :loading="withdrawing" @click="doWithdraw">确认撤回</el-button>
      </template>
    </el-dialog>

    <!-- 投递详情：展示投递时的快照 -->
    <el-drawer v-model="detailDrawer" :size="drawerSize" title="投递详情">
      <div v-if="detail" class="detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="职位">{{ detail.jobSnapshot?.title }}</el-descriptions-item>
          <el-descriptions-item label="招聘类型">
            {{ detail.jobSnapshot?.recruitmentType === 'CAMPUS' ? '校园招聘' : '社会招聘' }}
          </el-descriptions-item>
          <el-descriptions-item label="工作地点">{{ detail.jobSnapshot?.locationName }}</el-descriptions-item>
          <el-descriptions-item label="当前状态">{{ detail.statusLabel }}</el-descriptions-item>
          <el-descriptions-item label="投递时间">{{ detail.appliedAt }}</el-descriptions-item>
          <el-descriptions-item label="投递次序">第 {{ detail.attemptNo }} 次</el-descriptions-item>
        </el-descriptions>

        <div v-if="detail.interview" class="detail-block">
          <h4>面试安排</h4>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="面试时间">{{ detail.interview.interviewTime }}</el-descriptions-item>
            <el-descriptions-item label="面试方式">{{ detail.interview.methodLabel }}</el-descriptions-item>
            <el-descriptions-item label="地点/链接">{{ detail.interview.address }}</el-descriptions-item>
            <el-descriptions-item v-if="detail.interview.contactNote" label="联系说明">
              {{ detail.interview.contactNote }}
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="detail-block">
          <h4>
            投递时提交的简历
            <el-tag size="small" type="info" effect="plain">此为投递时提交的版本</el-tag>
          </h4>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="期望方向">
              {{ detail.resumeSnapshot?.intention?.expectCategory || '-' }} /
              {{ detail.resumeSnapshot?.intention?.expectCity || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="教育经历">
              <div v-for="(edu, index) in detail.resumeSnapshot?.educations || []" :key="index">
                {{ edu.school }} · {{ edu.major }} · {{ edu.degree }}（{{ edu.startDate }} ~ {{ edu.endDate }}）
              </div>
              <span v-if="!(detail.resumeSnapshot?.educations || []).length">-</span>
            </el-descriptions-item>
            <el-descriptions-item label="附件简历">
              <template v-if="detail.resumeFile">
                {{ detail.resumeFile.fileName }}
                <el-button text type="primary" @click="downloadSnapshot(detail.id)">下载</el-button>
              </template>
              <span v-else>未附带附件</span>
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { candidateApi } from '@/api'
import { toast } from '@/api/http'

const rows = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
// 首次渲染即进入加载态，避免接口返回前短暂显示“还没有投递记录”
const loading = ref(true)
const statusFilter = ref('')

const withdrawDialog = ref(false)
const withdrawing = ref(false)
const withdrawForm = reactive({ reason: '', remark: '' })
const withdrawTarget = ref(null)
const remainingHint = ref(0)

const detailDrawer = ref(false)
const detail = ref(null)
const drawerSize = computed(() => (window.innerWidth < 768 ? '90%' : '520px'))

async function load() {
  loading.value = true
  try {
    const data = await candidateApi.applications({
      status: statusFilter.value || undefined,
      page: page.value,
      size: pageSize
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
  page.value = 1
  load()
}

function handlePage(value) {
  page.value = value
  load()
}

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

async function openWithdraw(row) {
  withdrawTarget.value = row
  withdrawForm.reason = ''
  withdrawForm.remark = ''
  try {
    const eligibility = await candidateApi.eligibility(row.jobId)
    remainingHint.value = Math.max(0, eligibility.remainingAttempts || 0)
  } catch {
    remainingHint.value = 0
  }
  withdrawDialog.value = true
}

async function doWithdraw() {
  withdrawing.value = true
  try {
    await candidateApi.withdraw(withdrawTarget.value.id, { ...withdrawForm })
    withdrawDialog.value = false
    ElMessage.success('已撤回该投递')
    load()
  } catch (error) {
    toast(error, '撤回失败')
    load()
  } finally {
    withdrawing.value = false
  }
}

async function openDetail(id) {
  try {
    detail.value = await candidateApi.application(id)
    detailDrawer.value = true
  } catch (error) {
    toast(error, '详情加载失败')
  }
}

function downloadSnapshot(id) {
  window.open(candidateApi.downloadSnapshotUrl(id), '_blank')
}

onMounted(load)
</script>

<style scoped>
.my-applications {
  padding: 32px 24px 64px;
}

.page-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.page-head h1 {
  margin: 0;
  font-size: 26px;
}

.notice {
  margin-bottom: 18px;
}

.app-card {
  border-radius: var(--zk-radius);
  border: 1px solid var(--zk-border);
  margin-bottom: 14px;
}

.card-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
}

.title-line {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.title-line h3 {
  margin: 0;
  font-size: 17px;
}

.meta {
  display: flex;
  gap: 18px;
  flex-wrap: wrap;
  font-size: 13px;
  color: var(--zk-text-muted);
}

.interview-line {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 10px;
  font-size: 13px;
  color: #d97706;
  background: rgba(217, 119, 6, 0.08);
  padding: 4px 10px;
  border-radius: 6px;
}

.right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
  flex-shrink: 0;
}

.withdraw-form {
  margin-top: 16px;
}

.withdraw-tip {
  margin: 0;
  font-size: 12px;
  color: var(--zk-text-muted);
}

.detail-block {
  margin-top: 22px;
}

.detail-block h4 {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  margin: 0 0 10px;
}

.pager {
  margin-top: 20px;
  justify-content: center;
}

@media (max-width: 700px) {
  .card-main {
    flex-direction: column;
    align-items: flex-start;
  }
  .right {
    align-items: flex-start;
  }
}
</style>
