<template>
  <div class="detail-page" v-loading="loading">
    <div class="head">
      <div class="head-left">
        <el-button text @click="$router.back()">
          <el-icon><ArrowLeft /></el-icon>返回列表
        </el-button>
        <h2>{{ detail.candidateCurrent?.name }} · {{ detail.jobSnapshot?.title }}</h2>
        <el-tag :type="statusType" effect="dark">{{ detail.statusLabel }}</el-tag>
        <el-tag v-if="detail.attemptNo > 1" type="info" effect="plain">第 {{ detail.attemptNo }} 次投递</el-tag>
      </div>
    </div>

    <el-alert
      v-if="detail.readOnly"
      class="banner"
      type="info"
      show-icon
      :closable="false"
      title="候选人已撤回该投递，记录为只读，不能再变更状态或填写信息。"
    />
    <el-alert
      v-else-if="!detail.canWrite"
      class="banner"
      type="warning"
      show-icon
      :closable="false"
      title="你不是该职位的负责人，只能查看，不能操作。"
    />
    <el-alert
      v-if="detail.jobModified"
      class="banner"
      type="warning"
      show-icon
      :closable="false"
      title="职位已被修改：以下展示的是候选人投递时的职位快照，与当前职位内容不一致。"
    />

    <div class="body">
      <div class="left">
        <!-- 候选人信息：详情页手机号明文（§10.5） -->
        <el-card shadow="never" class="card">
          <template #header><span class="card-title">候选人信息</span></template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="姓名">{{ detail.candidateCurrent?.name }}</el-descriptions-item>
            <el-descriptions-item label="手机号">
              {{ detail.candidateCurrent?.phone || '-' }}
              <el-button text type="primary" size="small" @click="copyPhone">复制</el-button>
            </el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ detail.candidateCurrent?.email }}</el-descriptions-item>
            <el-descriptions-item label="所在城市">{{ detail.candidateCurrent?.city || '-' }}</el-descriptions-item>
            <el-descriptions-item label="投递时联系信息" :span="2">
              {{ detail.candidateSnapshot?.name }} / {{ detail.candidateSnapshot?.phone || '-' }} /
              {{ detail.candidateSnapshot?.email }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 简历快照（D6） -->
        <el-card shadow="never" class="card">
          <template #header>
            <span class="card-title">
              简历快照
              <el-tag size="small" type="info" effect="plain">投递时的版本，不随候选人后续修改而变化</el-tag>
            </span>
          </template>

          <div class="resume-file">
            <template v-if="detail.resumeFile">
              <el-icon><Document /></el-icon>
              <span>{{ detail.resumeFile.fileName }}</span>
              <el-button text type="primary" @click="downloadResume">下载附件</el-button>
            </template>
            <span v-else class="muted">该次投递未附带附件简历</span>
          </div>

          <el-descriptions :column="1" border class="resume-desc">
            <el-descriptions-item label="求职意向">
              {{ resume.intention?.expectCategory || '-' }} / {{ resume.intention?.expectCity || '-' }}
              <span v-if="resume.intention?.expectSalary">/ {{ resume.intention.expectSalary }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="教育经历">
              <div v-for="(row, index) in resume.educations || []" :key="index" class="resume-row">
                {{ row.school }} · {{ row.major }} · {{ row.degree }}（{{ row.startDate }} ~ {{ row.endDate }}）
              </div>
              <span v-if="!(resume.educations || []).length" class="muted">-</span>
            </el-descriptions-item>
            <el-descriptions-item label="工作/实习经历">
              <div v-for="(row, index) in resume.experiences || []" :key="index" class="resume-row">
                <strong>{{ row.company }}</strong> · {{ row.position }}（{{ row.startDate }} ~ {{ row.endDate }}）
                <div class="resume-desc-text">{{ row.description }}</div>
              </div>
              <span v-if="!(resume.experiences || []).length" class="muted">-</span>
            </el-descriptions-item>
            <el-descriptions-item label="项目经历">
              <div v-for="(row, index) in resume.projects || []" :key="index" class="resume-row">
                <strong>{{ row.name }}</strong> · {{ row.role }}
                <div class="resume-desc-text">{{ row.description }}</div>
              </div>
              <span v-if="!(resume.projects || []).length" class="muted">-</span>
            </el-descriptions-item>
            <el-descriptions-item label="专业技能">{{ resume.skills || '-' }}</el-descriptions-item>
            <el-descriptions-item label="证书及获奖">{{ resume.certificates || '-' }}</el-descriptions-item>
            <el-descriptions-item label="自我评价">{{ resume.selfEvaluation || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 职位快照 -->
        <el-card shadow="never" class="card">
          <template #header><span class="card-title">投递时的职位快照</span></template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="职位名称">{{ detail.jobSnapshot?.title }}</el-descriptions-item>
            <el-descriptions-item label="招聘类型">
              {{ detail.jobSnapshot?.recruitmentType === 'CAMPUS' ? '校园招聘' : '社会招聘' }}
            </el-descriptions-item>
            <el-descriptions-item label="职位类别">{{ detail.jobSnapshot?.categoryName }}</el-descriptions-item>
            <el-descriptions-item label="工作地点">{{ detail.jobSnapshot?.locationName }}</el-descriptions-item>
            <el-descriptions-item label="学历要求">{{ detail.jobSnapshot?.education }}</el-descriptions-item>
            <el-descriptions-item label="经验/年份">
              {{ detail.jobSnapshot?.experience || detail.jobSnapshot?.graduationYear || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="快照版本" :span="2">
              v{{ detail.jobSnapshot?.jobVersion }}（当前职位 v{{ detail.jobCurrent?.version }}）
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 状态变更记录（§10.5） -->
        <el-card shadow="never" class="card">
          <template #header><span class="card-title">状态变更记录</span></template>
          <el-timeline>
            <el-timeline-item
              v-for="log in detail.logs || []"
              :key="log.id"
              :timestamp="log.createdAt"
              placement="top"
            >
              <div class="log-line">
                <strong>{{ actionLabel(log.action) }}</strong>
                <span v-if="log.fromStatus || log.toStatus" class="log-status">
                  {{ statusLabelOf(log.fromStatus) }} → {{ statusLabelOf(log.toStatus) }}
                </span>
                <el-tag size="small" :type="log.operatorType === 'CANDIDATE' ? 'warning' : 'info'" effect="plain">
                  {{ log.operatorType === 'CANDIDATE' ? '候选人' : 'HR' }}
                  {{ log.operatorName ? '·' + log.operatorName : '' }}
                </el-tag>
              </div>
              <div v-if="log.remark" class="log-remark">{{ log.remark }}</div>
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </div>

      <div class="right">
        <!-- 处理操作 -->
        <el-card shadow="never" class="card">
          <template #header><span class="card-title">处理操作</span></template>
          <div v-if="!detail.canWrite" class="muted">当前不可操作。</div>
          <div v-else class="actions">
            <el-button
              v-if="detail.status === 'SUBMITTED'"
              type="primary"
              @click="changeStatus('VIEWED')"
            >
              标记已查看
            </el-button>
            <el-button
              v-if="['VIEWED'].includes(detail.status)"
              type="warning"
              @click="openInterview"
            >
              安排面试
            </el-button>
            <el-button
              v-if="detail.status === 'INTERVIEW'"
              type="warning"
              @click="openInterview"
            >
              {{ detail.interview ? '修改面试安排' : '安排面试' }}
            </el-button>
            <el-button
              v-if="detail.status === 'INTERVIEW'"
              type="success"
              @click="changeStatus('PASSED')"
            >
              标记已通过
            </el-button>
            <el-button
              v-if="['VIEWED', 'INTERVIEW'].includes(detail.status)"
              type="danger"
              @click="changeStatus('REJECTED')"
            >
              标记不合适
            </el-button>
            <el-button
              v-if="['PASSED', 'REJECTED'].includes(detail.status)"
              @click="changeStatus('VIEWED')"
            >
              撤销结论
            </el-button>
          </div>
          <p class="hint">状态流转：已投递 → 已查看 → 待面试 → 已通过/不合适；结论填错可撤销回已查看。</p>
        </el-card>

        <!-- 面试信息（§10.6） -->
        <el-card shadow="never" class="card">
          <template #header><span class="card-title">面试信息</span></template>
          <template v-if="detail.interview">
            <el-descriptions :column="1" border>
              <el-descriptions-item label="面试时间">{{ detail.interview.interviewTime }}</el-descriptions-item>
              <el-descriptions-item label="面试方式">{{ detail.interview.methodLabel }}</el-descriptions-item>
              <el-descriptions-item label="地点/链接">{{ detail.interview.address }}</el-descriptions-item>
              <el-descriptions-item label="联系说明">{{ detail.interview.contactNote || '-' }}</el-descriptions-item>
              <el-descriptions-item label="面试评价">
                <span class="internal">{{ detail.interview.evaluation || '-' }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="面试结果">
                {{ resultLabel(detail.interview.result) }}
              </el-descriptions-item>
            </el-descriptions>
            <el-alert
              class="notice-hint"
              type="warning"
              show-icon
              :closable="false"
              title="系统不会通知候选人，请电话联系。"
            />
          </template>
          <div v-else class="muted">尚未安排面试。</div>
        </el-card>

        <!-- 内部备注（仅 HR 可见） -->
        <el-card shadow="never" class="card">
          <template #header><span class="card-title">内部备注（仅 HR 可见）</span></template>
          <el-input
            v-model="noteDraft"
            type="textarea"
            :rows="4"
            maxlength="2000"
            show-word-limit
            :disabled="!detail.canWrite"
            placeholder="记录筛选判断、沟通结论等，不会返回给候选人"
          />
          <el-button
            class="note-btn"
            type="primary"
            :disabled="!detail.canWrite"
            :loading="savingNote"
            @click="saveNote"
          >
            保存备注
          </el-button>
        </el-card>

        <!-- 该候选人对本职位的历史投递（§9.6） -->
        <el-card shadow="never" class="card">
          <template #header><span class="card-title">该候选人的历史投递</span></template>
          <div v-for="row in detail.history || []" :key="row.id" class="history-row">
            <span>第 {{ row.attemptNo }} 次</span>
            <el-tag size="small" :type="row.status === 'WITHDRAWN' ? 'info' : 'primary'" effect="plain">
              {{ row.statusLabel }}
            </el-tag>
            <span class="muted">{{ row.appliedAt }}</span>
            <el-tag v-if="row.current" size="small" type="success" effect="dark">当前</el-tag>
            <el-button v-else text type="primary" size="small" @click="goApplication(row.id)">查看</el-button>
          </div>
        </el-card>
      </div>
    </div>

    <!-- 面试安排弹窗 -->
    <el-dialog v-model="interviewDialog" title="面试安排" width="480px">
      <el-form ref="interviewFormRef" :model="interviewForm" :rules="interviewRules" label-width="100px">
        <el-form-item label="面试时间" prop="interviewTime">
          <el-date-picker
            v-model="interviewForm.interviewTime"
            type="datetime"
            placeholder="选择面试时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="面试方式" prop="method">
          <el-radio-group v-model="interviewForm.method">
            <el-radio value="OFFLINE">线下</el-radio>
            <el-radio value="ONLINE">线上</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="interviewForm.method === 'ONLINE' ? '会议链接' : '面试地点'" prop="address">
          <el-input v-model="interviewForm.address" maxlength="300" />
        </el-form-item>
        <el-form-item label="联系说明">
          <el-input v-model="interviewForm.contactNote" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
        <el-form-item label="面试评价">
          <el-input
            v-model="interviewForm.evaluation"
            type="textarea"
            :rows="3"
            maxlength="2000"
            placeholder="仅 HR 可见，候选人看不到"
          />
        </el-form-item>
        <el-form-item label="面试结果">
          <el-select v-model="interviewForm.result" placeholder="未出结果" clearable style="width: 100%">
            <el-option label="通过" value="PASS" />
            <el-option label="不合适" value="FAIL" />
          </el-select>
        </el-form-item>
      </el-form>
      <el-alert
        type="info"
        show-icon
        :closable="false"
        title="保存后投递状态会变为待面试。填写面试结果不会自动改变投递状态，仍需手动点击结论。"
      />
      <template #footer>
        <el-button @click="interviewDialog = false">取消</el-button>
        <el-button type="primary" :loading="savingInterview" @click="saveInterview">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { hrApi } from '@/api'
import { toast } from '@/api/http'

const route = useRoute()
const router = useRouter()

const detail = ref({})
const loading = ref(false)
const noteDraft = ref('')
const savingNote = ref(false)

const interviewDialog = ref(false)
const interviewFormRef = ref()
const savingInterview = ref(false)
const interviewForm = reactive({
  interviewTime: '',
  method: 'OFFLINE',
  address: '',
  contactNote: '',
  evaluation: '',
  result: null
})

const interviewRules = {
  interviewTime: [{ required: true, message: '请选择面试时间', trigger: 'change' }],
  method: [{ required: true, message: '请选择面试方式', trigger: 'change' }],
  address: [{ required: true, message: '请填写面试地点或会议链接', trigger: 'blur' }]
}

const resume = computed(() => detail.value.resumeSnapshot || {})
const statusType = computed(
  () =>
    ({
      SUBMITTED: 'info',
      VIEWED: 'primary',
      INTERVIEW: 'warning',
      PASSED: 'success',
      REJECTED: 'danger',
      WITHDRAWN: 'info'
    }[detail.value.status] || 'info')
)

function statusLabelOf(status) {
  return (
    {
      SUBMITTED: '已投递',
      VIEWED: '已查看',
      INTERVIEW: '待面试',
      PASSED: '已通过',
      REJECTED: '不合适',
      WITHDRAWN: '已撤回'
    }[status] || status || '-'
  )
}

function actionLabel(action) {
  return (
    {
      APPLY: '提交投递',
      WITHDRAW: '撤回投递',
      CHANGE_STATUS: '变更状态',
      UNDO_CONCLUSION: '撤销结论',
      ARRANGE_INTERVIEW: '安排面试',
      UPDATE_INTERVIEW: '更新面试信息',
      UPDATE_NOTE: '更新内部备注'
    }[action] || action
  )
}

function resultLabel(result) {
  return { PASS: '通过', FAIL: '不合适' }[result] || '未出结果'
}

async function load() {
  loading.value = true
  try {
    detail.value = await hrApi.application(route.params.id)
    noteDraft.value = detail.value.hrNote || ''
  } catch (error) {
    toast(error, '详情加载失败')
    router.push({ name: 'hr-applications' })
  } finally {
    loading.value = false
  }
}

async function changeStatus(target) {
  const labels = {
    VIEWED: detail.value.status === 'SUBMITTED' ? '标记已查看' : '撤销结论',
    PASSED: '标记已通过',
    REJECTED: '标记不合适'
  }
  // 危险操作需二次确认（§16.3）
  if (target === 'REJECTED' || (target === 'VIEWED' && detail.value.status !== 'SUBMITTED')) {
    try {
      await ElMessageBox.confirm(
        target === 'REJECTED'
          ? '标记不合适后候选人会看到"暂不匹配"，如误操作可再撤销。'
          : '撤销结论会把该投递回到"已查看"，操作会记录在变更日志中。',
        `确认${labels[target]}？`,
        { type: 'warning' }
      )
    } catch {
      return
    }
  }
  try {
    await hrApi.changeStatus(route.params.id, {
      targetStatus: target,
      version: detail.value.version,
      remark: labels[target]
    })
    ElMessage.success(`已${labels[target]}`)
    load()
  } catch (error) {
    toast(error, '操作失败')
    load()
  }
}

async function saveNote() {
  savingNote.value = true
  try {
    await hrApi.saveNote(route.params.id, { note: noteDraft.value, version: detail.value.version })
    ElMessage.success('备注已保存')
    load()
  } catch (error) {
    toast(error, '保存失败')
    load()
  } finally {
    savingNote.value = false
  }
}

function openInterview() {
  const current = detail.value.interview
  Object.assign(interviewForm, {
    interviewTime: current?.interviewTime || '',
    method: current?.method || 'OFFLINE',
    address: current?.address || '',
    contactNote: current?.contactNote || '',
    evaluation: current?.evaluation || '',
    result: current?.result || null
  })
  interviewDialog.value = true
}

async function saveInterview() {
  const valid = await interviewFormRef.value.validate().catch(() => false)
  if (!valid) return
  savingInterview.value = true
  try {
    const result = await hrApi.saveInterview(route.params.id, {
      ...interviewForm,
      version: detail.value.version
    })
    interviewDialog.value = false
    ElMessage.success('面试信息已保存')
    ElMessageBox.alert(
      `${result.noticeHint}。候选人手机号：${result.candidatePhone || '未填写'}`,
      '请手动通知候选人',
      { type: 'warning' }
    )
    load()
  } catch (error) {
    toast(error, '保存失败')
    load()
  } finally {
    savingInterview.value = false
  }
}

function copyPhone() {
  const phone = detail.value.candidateCurrent?.phone
  if (!phone) {
    ElMessage.warning('候选人未填写手机号')
    return
  }
  navigator.clipboard?.writeText(phone)
  ElMessage.success('手机号已复制')
}

function downloadResume() {
  window.open(hrApi.resumeDownloadUrl(route.params.id), '_blank')
}

function goApplication(id) {
  router.push({ name: 'hr-application-detail', params: { id } })
  setTimeout(load, 50)
}

onMounted(load)
</script>

<style scoped>
.head {
  margin-bottom: 14px;
}

.head-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.head-left h2 {
  margin: 0;
  font-size: 20px;
}

.banner {
  margin-bottom: 12px;
}

.body {
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: 16px;
  align-items: start;
}

.card {
  border-radius: var(--zk-radius);
  border: 1px solid var(--zk-border);
  margin-bottom: 16px;
}

.card-title {
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.resume-file {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: var(--zk-bg-soft);
  border-radius: 8px;
  margin-bottom: 14px;
  font-size: 13px;
}

.resume-row {
  margin-bottom: 8px;
  line-height: 1.7;
}

.resume-desc-text {
  color: var(--zk-text-muted);
  font-size: 13px;
}

.muted {
  color: var(--zk-text-muted);
  font-size: 13px;
}

.internal {
  color: #b45309;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.hint {
  margin: 14px 0 0;
  font-size: 12px;
  color: var(--zk-text-muted);
  line-height: 1.7;
}

.notice-hint {
  margin-top: 12px;
}

.note-btn {
  margin-top: 12px;
}

.history-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 0;
  font-size: 13px;
  border-bottom: 1px dashed var(--zk-border);
}

.history-row:last-child {
  border-bottom: none;
}

.log-line {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  font-size: 13px;
}

.log-status {
  color: var(--zk-text-muted);
}

.log-remark {
  margin-top: 4px;
  font-size: 12px;
  color: var(--zk-text-muted);
}

@media (max-width: 1100px) {
  .body {
    grid-template-columns: 1fr;
  }
}
</style>
