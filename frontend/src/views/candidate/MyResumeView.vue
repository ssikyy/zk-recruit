<template>
  <div
    v-if="initializing"
    v-loading="true"
    class="resume-page-state"
    aria-label="简历数据加载中"
  ></div>
  <div v-else-if="loadError" class="zk-container resume-page-state">
    <el-result icon="error" title="简历数据加载失败" sub-title="请检查网络后重新加载">
      <template #extra>
        <el-button type="primary" @click="loadAll">重新加载</el-button>
      </template>
    </el-result>
  </div>
  <div v-else class="zk-container my-resume">
    <div class="page-head">
      <div>
        <h1>我的简历</h1>
        <p>投递前需要填写姓名与手机号，并且完成在线简历或上传一份附件简历。</p>
      </div>
      <el-tag :type="eligible ? 'success' : 'warning'" effect="light" size="large">
        {{ eligible ? '已满足投递条件' : '尚不满足投递条件' }}
      </el-tag>
    </div>

    <el-alert
      v-if="!eligible && missing.length"
      class="missing-alert"
      type="warning"
      show-icon
      :closable="false"
      :title="'还需补全：' + missing.map(missingLabel).join('、')"
    />

    <el-tabs v-model="activeTab" class="tabs">
      <!-- 基本资料（§9.1） -->
      <el-tab-pane label="基本资料" name="profile">
        <el-card shadow="never" class="card">
          <el-form ref="profileFormRef" :model="profile" :rules="profileRules" label-width="100px">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="profile.name" style="max-width: 320px" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="profile.email" style="max-width: 320px" />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="profile.phone" placeholder="投递前必填，用于 HR 联系你" style="max-width: 320px" />
            </el-form-item>
            <el-form-item label="性别">
              <el-radio-group v-model="profile.gender">
                <el-radio value="MALE">男</el-radio>
                <el-radio value="FEMALE">女</el-radio>
                <el-radio value="UNKNOWN">不愿透露</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="所在城市">
              <el-input v-model="profile.city" style="max-width: 320px" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="savingProfile" @click="saveProfile">保存基本资料</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>

      <!-- 在线简历（§9.2） -->
      <el-tab-pane name="resume">
        <template #label>
          在线简历
          <el-tag v-if="resumeComplete" size="small" type="success" effect="plain">已完成</el-tag>
        </template>

        <el-card shadow="never" class="card">
          <h3 class="block-title">求职意向 <span class="required">必填</span></h3>
          <el-form label-width="110px">
            <div class="grid-2">
              <el-form-item label="期望职位类别">
                <el-select v-model="resume.intention.expectCategory" placeholder="请选择" style="width: 100%">
                  <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.name" />
                </el-select>
              </el-form-item>
              <el-form-item label="期望工作城市">
                <el-select
                  v-model="resume.intention.expectCity"
                  placeholder="请选择"
                  filterable
                  allow-create
                  style="width: 100%"
                >
                  <el-option v-for="item in locations" :key="item.id" :label="item.name" :value="item.name" />
                </el-select>
              </el-form-item>
              <el-form-item label="期望薪资">
                <el-input v-model="resume.intention.expectSalary" placeholder="选填，如 面议" />
              </el-form-item>
              <el-form-item label="备注">
                <el-input v-model="resume.intention.remark" placeholder="选填" />
              </el-form-item>
            </div>
          </el-form>

          <h3 class="block-title">
            教育经历 <span class="required">必填至少 1 条</span>
            <el-button text type="primary" @click="addRow('educations')">+ 添加</el-button>
          </h3>
          <div v-for="(row, index) in resume.educations" :key="'edu' + index" class="row-box">
            <div class="grid-3">
              <el-input v-model="row.school" placeholder="学校" />
              <el-input v-model="row.major" placeholder="专业" />
              <el-select v-model="row.degree" placeholder="学历">
                <el-option v-for="item in degrees" :key="item" :label="item" :value="item" />
              </el-select>
              <el-input v-model="row.startDate" placeholder="开始时间，如 2019-09" />
              <el-input v-model="row.endDate" placeholder="结束时间，如 2023-06" />
              <el-button type="danger" text @click="removeRow('educations', index)">删除</el-button>
            </div>
          </div>
          <el-empty v-if="!resume.educations.length" description="请添加至少一条教育经历" :image-size="60" />

          <h3 class="block-title">
            工作或实习经历
            <el-button text type="primary" @click="addRow('experiences')">+ 添加</el-button>
          </h3>
          <div v-for="(row, index) in resume.experiences" :key="'exp' + index" class="row-box">
            <div class="grid-3">
              <el-input v-model="row.company" placeholder="公司名称" />
              <el-input v-model="row.position" placeholder="职位" />
              <el-button type="danger" text @click="removeRow('experiences', index)">删除</el-button>
              <el-input v-model="row.startDate" placeholder="开始时间" />
              <el-input v-model="row.endDate" placeholder="结束时间" />
            </div>
            <el-input
              v-model="row.description"
              type="textarea"
              :rows="2"
              placeholder="主要工作内容与成果"
              class="row-desc"
            />
          </div>

          <h3 class="block-title">
            项目经历
            <el-button text type="primary" @click="addRow('projects')">+ 添加</el-button>
          </h3>
          <div v-for="(row, index) in resume.projects" :key="'prj' + index" class="row-box">
            <div class="grid-3">
              <el-input v-model="row.name" placeholder="项目名称" />
              <el-input v-model="row.role" placeholder="担任角色" />
              <el-button type="danger" text @click="removeRow('projects', index)">删除</el-button>
              <el-input v-model="row.startDate" placeholder="开始时间" />
              <el-input v-model="row.endDate" placeholder="结束时间" />
            </div>
            <el-input
              v-model="row.description"
              type="textarea"
              :rows="2"
              placeholder="项目背景与你的贡献"
              class="row-desc"
            />
          </div>

          <h3 class="block-title">专业技能 / 证书 / 自我评价</h3>
          <el-form label-width="110px">
            <el-form-item label="专业技能">
              <el-input v-model="resume.skills" type="textarea" :rows="2" placeholder="选填" />
            </el-form-item>
            <el-form-item label="证书及获奖">
              <el-input v-model="resume.certificates" type="textarea" :rows="2" placeholder="选填" />
            </el-form-item>
            <el-form-item label="自我评价">
              <el-input v-model="resume.selfEvaluation" type="textarea" :rows="3" placeholder="选填" />
            </el-form-item>
          </el-form>

          <div class="save-line">
            <el-button type="primary" :loading="savingResume" @click="saveResume">保存在线简历</el-button>
            <span class="save-tip">保存后不会影响此前已提交的投递，历史投递看到的仍是投递时的快照。</span>
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 附件简历（§9.3） -->
      <el-tab-pane label="附件简历" name="file">
        <el-card shadow="never" class="card">
          <el-alert
            type="info"
            :closable="false"
            show-icon
            title="支持 PDF / DOC / DOCX，单个文件不超过 10MB。上传新附件会替换当前附件，但历史投递仍指向投递时的版本。"
          />
          <div class="file-area">
            <div v-if="currentFile" class="file-box">
              <div class="file-info">
                <el-icon size="24"><Document /></el-icon>
                <div>
                  <div class="file-name">{{ currentFile.fileName }}</div>
                  <div class="file-meta">
                    {{ formatSize(currentFile.fileSize) }} · 上传于 {{ currentFile.uploadedAt }}
                  </div>
                </div>
              </div>
              <div class="file-actions">
                <el-button text type="primary" @click="downloadCurrent">下载</el-button>
                <el-button text type="danger" @click="removeFile">取消当前附件</el-button>
              </div>
            </div>
            <el-empty v-else description="尚未上传附件简历" :image-size="70" />

            <el-upload
              class="uploader"
              :show-file-list="false"
              :before-upload="handleUpload"
              accept=".pdf,.doc,.docx"
            >
              <el-button type="primary" :loading="uploading">
                {{ currentFile ? '替换附件简历' : '上传附件简历' }}
              </el-button>
            </el-upload>
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { candidateApi, publicApi } from '@/api'
import { toast } from '@/api/http'

const activeTab = ref('profile')
const profileFormRef = ref()
const savingProfile = ref(false)
const savingResume = ref(false)
const uploading = ref(false)
const initializing = ref(true)
const loadError = ref(false)

const categories = ref([])
const locations = ref([])
const degrees = ['大专', '本科', '硕士', '博士', '其他']

const profile = reactive({ name: '', email: '', phone: '', gender: 'UNKNOWN', city: '' })
const resume = reactive({
  intention: { expectCategory: '', expectCity: '', expectSalary: '', remark: '' },
  educations: [],
  experiences: [],
  projects: [],
  skills: '',
  certificates: '',
  selfEvaluation: ''
})
const resumeComplete = ref(false)
const currentFile = ref(null)
const eligibilityState = ref({})

const profileRules = {
  name: [{ required: true, min: 2, max: 20, message: '姓名长度需为 2-20 位', trigger: 'blur' }],
  email: [
    { required: true, message: '请填写邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  phone: [
    {
      pattern: /^$|^1[3-9]\d{9}$/,
      message: '手机号格式不正确',
      trigger: 'blur'
    }
  ]
}

const missing = computed(() => {
  const list = []
  if (!profile.name) list.push('NAME')
  if (!/^1[3-9]\d{9}$/.test(profile.phone || '')) list.push('PHONE')
  if (!resumeComplete.value && !currentFile.value) list.push('RESUME')
  return list
})
const eligible = computed(() => missing.value.length === 0)

function missingLabel(code) {
  const map = { NAME: '姓名', PHONE: '手机号', RESUME: '在线简历或附件简历' }
  return map[code] || code
}

function addRow(key) {
  const templates = {
    educations: { school: '', major: '', degree: '', startDate: '', endDate: '' },
    experiences: { company: '', position: '', startDate: '', endDate: '', description: '' },
    projects: { name: '', role: '', startDate: '', endDate: '', description: '' }
  }
  resume[key].push({ ...templates[key] })
}

function removeRow(key, index) {
  resume[key].splice(index, 1)
}

async function loadAll() {
  initializing.value = true
  loadError.value = false
  try {
    const [cats, locs, profileData, resumeData, fileData] = await Promise.all([
      publicApi.categories(),
      publicApi.locations(),
      candidateApi.profile(),
      candidateApi.resume(),
      candidateApi.currentFile()
    ])
    categories.value = cats || []
    locations.value = locs || []
    Object.assign(profile, profileData || {})
    if (!profile.gender) profile.gender = 'UNKNOWN'

    const content = resumeData?.content || {}
    resume.intention = {
      expectCategory: '',
      expectCity: '',
      expectSalary: '',
      remark: '',
      ...(content.intention || {})
    }
    resume.educations = content.educations || []
    resume.experiences = content.experiences || []
    resume.projects = content.projects || []
    resume.skills = content.skills || ''
    resume.certificates = content.certificates || ''
    resume.selfEvaluation = content.selfEvaluation || ''
    resumeComplete.value = !!resumeData?.complete
    currentFile.value = fileData || null
  } catch (error) {
    loadError.value = true
    toast(error, '简历数据加载失败')
  } finally {
    initializing.value = false
  }
}

async function saveProfile() {
  const valid = await profileFormRef.value.validate().catch(() => false)
  if (!valid) return
  savingProfile.value = true
  try {
    await candidateApi.saveProfile({ ...profile })
    ElMessage.success('基本资料已保存')
  } catch (error) {
    toast(error, '保存失败')
  } finally {
    savingProfile.value = false
  }
}

async function saveResume() {
  savingResume.value = true
  try {
    await candidateApi.saveResume({
      intention: resume.intention,
      educations: resume.educations,
      experiences: resume.experiences,
      projects: resume.projects,
      skills: resume.skills,
      certificates: resume.certificates,
      selfEvaluation: resume.selfEvaluation
    })
    const latest = await candidateApi.resume()
    resumeComplete.value = !!latest?.complete
    ElMessage.success(resumeComplete.value ? '在线简历已保存并满足投递要求' : '已保存草稿，必填项尚未完成')
  } catch (error) {
    toast(error, '保存失败')
  } finally {
    savingResume.value = false
  }
}

async function handleUpload(file) {
  uploading.value = true
  try {
    currentFile.value = await candidateApi.uploadFile(file)
    ElMessage.success('附件简历已上传')
  } catch (error) {
    toast(error, '上传失败')
  } finally {
    uploading.value = false
  }
  // 阻止 el-upload 自身的上传行为
  return false
}

async function removeFile() {
  try {
    await ElMessageBox.confirm(
      '取消后该附件不再作为当前简历，但历史投递看到的仍是投递时的版本。',
      '取消当前附件',
      { type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await candidateApi.deleteFile()
    currentFile.value = null
    ElMessage.success('已取消当前附件')
  } catch (error) {
    toast(error, '操作失败')
  }
}

function downloadCurrent() {
  window.open(candidateApi.downloadCurrentUrl(), '_blank')
}

function formatSize(size) {
  if (!size) return '-'
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
  return (size / 1024 / 1024).toFixed(2) + ' MB'
}

onMounted(loadAll)
</script>

<style scoped>
.resume-page-state {
  min-height: calc(100vh - var(--zk-header-height));
}

.my-resume {
  padding: 32px 24px 64px;
}

.page-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 18px;
}

.page-head h1 {
  margin: 0 0 6px;
  font-size: 26px;
}

.page-head p {
  margin: 0;
  font-size: 13px;
  color: var(--zk-text-muted);
}

.missing-alert {
  margin-bottom: 16px;
}

.card {
  border-radius: var(--zk-radius);
  border: 1px solid var(--zk-border);
}

.block-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  margin: 26px 0 14px;
  padding-left: 10px;
  border-left: 3px solid var(--zk-primary);
}

.block-title:first-child {
  margin-top: 4px;
}

.required {
  font-size: 12px;
  color: #e6a23c;
  font-weight: 400;
}

.grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 20px;
}

.grid-3 {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  align-items: center;
}

.row-box {
  padding: 14px;
  border: 1px dashed var(--zk-border);
  border-radius: 10px;
  margin-bottom: 12px;
}

.row-desc {
  margin-top: 12px;
}

.save-line {
  margin-top: 28px;
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.save-tip {
  font-size: 12px;
  color: var(--zk-text-muted);
}

.file-area {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  align-items: flex-start;
}

.file-box {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 18px;
  border: 1px solid var(--zk-border);
  border-radius: 10px;
  background: var(--zk-bg-soft);
}

.file-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.file-name {
  font-weight: 600;
}

.file-meta {
  font-size: 12px;
  color: var(--zk-text-muted);
}

@media (max-width: 768px) {
  .grid-2,
  .grid-3 {
    grid-template-columns: 1fr;
  }
}
</style>
