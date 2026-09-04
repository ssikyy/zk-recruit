<template>
  <div class="job-edit">
    <div class="head">
      <h2>{{ isEdit ? '编辑职位' : '新增职位' }}</h2>
      <el-button @click="$router.back()">返回</el-button>
    </div>

    <el-card shadow="never" class="card" v-loading="loading">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="招聘类型" prop="recruitmentType">
          <el-radio-group v-model="form.recruitmentType">
            <el-radio-button value="SOCIAL">社会招聘</el-radio-button>
            <el-radio-button value="CAMPUS">校园招聘</el-radio-button>
          </el-radio-group>
          <span class="field-tip">社招必填工作经验；校招必填毕业年份与招聘对象</span>
        </el-form-item>

        <el-form-item label="职位名称" prop="title">
          <el-input v-model="form.title" maxlength="120" show-word-limit style="max-width: 460px" />
        </el-form-item>

        <div class="grid">
          <el-form-item label="职位类别" prop="categoryId">
            <el-select v-model="form.categoryId" placeholder="请选择" style="width: 100%">
              <el-option
                v-for="item in categories"
                :key="item.id"
                :label="item.name + (item.status === 'DISABLED' ? '（已停用）' : '')"
                :value="item.id"
                :disabled="item.status === 'DISABLED' && item.id !== originalCategoryId"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="工作地点" prop="locationId">
            <el-select v-model="form.locationId" placeholder="请选择" style="width: 100%">
              <el-option
                v-for="item in locations"
                :key="item.id"
                :label="item.name + (item.status === 'DISABLED' ? '（已停用）' : '')"
                :value="item.id"
                :disabled="item.status === 'DISABLED' && item.id !== originalLocationId"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="招聘人数" prop="headcount">
            <el-input-number v-model="form.headcount" :min="1" :max="999" style="width: 100%" />
          </el-form-item>

          <el-form-item label="学历要求" prop="education">
            <el-select v-model="form.education" placeholder="请选择" style="width: 100%">
              <el-option v-for="item in options.educations" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>

          <el-form-item v-if="isSocial" label="工作经验" prop="experience">
            <el-select v-model="form.experience" placeholder="请选择" style="width: 100%">
              <el-option v-for="item in options.experiences" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>

          <template v-else>
            <el-form-item label="毕业年份" prop="graduationYear">
              <el-input v-model="form.graduationYear" placeholder="如 2027届" />
            </el-form-item>
            <el-form-item label="招聘对象" prop="targetAudience">
              <el-select v-model="form.targetAudience" placeholder="请选择" style="width: 100%">
                <el-option label="应届生" value="GRADUATE" />
                <el-option label="实习生" value="INTERN" />
              </el-select>
            </el-form-item>
          </template>

          <el-form-item v-if="auth.isAdmin" label="职位负责人">
            <el-select v-model="form.ownerHrId" placeholder="默认为自己" clearable style="width: 100%">
              <el-option
                v-for="item in hrOptions"
                :key="item.id"
                :label="item.name + (item.hrAdmin ? '（管理员）' : '')"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
        </div>

        <el-form-item label="岗位职责" prop="duty">
          <el-input v-model="form.duty" type="textarea" :rows="6" maxlength="5000" show-word-limit />
        </el-form-item>

        <el-form-item label="任职要求" prop="requirement">
          <el-input v-model="form.requirement" type="textarea" :rows="6" maxlength="5000" show-word-limit />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="saving" @click="submit">
            {{ isEdit ? '保存修改' : '保存为草稿' }}
          </el-button>
          <el-button v-if="!isEdit" :loading="saving" @click="submitAndPublish">保存并发布</el-button>
        </el-form-item>
        <p v-if="isEdit" class="save-note">
          编辑招聘中的职位不会影响已提交投递看到的内容，历史投递保留投递时的职位快照。
        </p>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { hrApi, publicApi, adminApi } from '@/api'
import { toast } from '@/api/http'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const formRef = ref()
const loading = ref(false)
const saving = ref(false)
const categories = ref([])
const locations = ref([])
const hrOptions = ref([])
const options = reactive({ educations: [], experiences: [] })
const originalCategoryId = ref(null)
const originalLocationId = ref(null)
const currentVersion = ref(0)

const isEdit = computed(() => !!route.params.id)
const isSocial = computed(() => form.recruitmentType === 'SOCIAL')

const form = reactive({
  title: '',
  recruitmentType: 'SOCIAL',
  categoryId: null,
  locationId: null,
  headcount: 1,
  education: '',
  experience: '',
  graduationYear: '',
  targetAudience: null,
  duty: '',
  requirement: '',
  ownerHrId: null
})

const rules = {
  title: [{ required: true, message: '请填写职位名称', trigger: 'blur' }],
  recruitmentType: [{ required: true, message: '请选择招聘类型', trigger: 'change' }],
  categoryId: [{ required: true, message: '请选择职位类别', trigger: 'change' }],
  locationId: [{ required: true, message: '请选择工作地点', trigger: 'change' }],
  headcount: [{ required: true, message: '请填写招聘人数', trigger: 'blur' }],
  education: [{ required: true, message: '请选择学历要求', trigger: 'change' }],
  experience: [
    {
      validator: (_r, value, cb) =>
        isSocial.value && !value ? cb(new Error('社会招聘必须填写工作经验要求')) : cb(),
      trigger: 'change'
    }
  ],
  graduationYear: [
    {
      validator: (_r, value, cb) =>
        !isSocial.value && !value ? cb(new Error('校园招聘必须填写毕业年份要求')) : cb(),
      trigger: 'blur'
    }
  ],
  targetAudience: [
    {
      validator: (_r, value, cb) =>
        !isSocial.value && !value ? cb(new Error('校园招聘必须选择招聘对象')) : cb(),
      trigger: 'change'
    }
  ],
  duty: [{ required: true, message: '请填写岗位职责', trigger: 'blur' }],
  requirement: [{ required: true, message: '请填写任职要求', trigger: 'blur' }]
}

async function loadOptions() {
  try {
    const [opts, hrList] = await Promise.all([
      publicApi.jobOptions(),
      auth.isHr ? hrApi.hrOptions() : Promise.resolve([])
    ])
    options.educations = opts?.educations || []
    options.experiences = opts?.experiences || []
    hrOptions.value = hrList || []

    // 管理员能看到停用项以便沿用历史值，普通 HR 只取启用项
    if (auth.isAdmin) {
      const [cats, locs] = await Promise.all([adminApi.categories(), adminApi.locations()])
      categories.value = cats || []
      locations.value = locs || []
    } else {
      const [cats, locs] = await Promise.all([publicApi.categories(), publicApi.locations()])
      categories.value = cats || []
      locations.value = locs || []
    }
  } catch (error) {
    toast(error, '表单选项加载失败')
  }
}

async function loadJob() {
  if (!isEdit.value) return
  loading.value = true
  try {
    const data = await hrApi.job(route.params.id)
    const job = data.job || {}
    Object.assign(form, {
      title: job.title,
      recruitmentType: job.recruitmentType,
      categoryId: job.categoryId,
      locationId: job.locationId,
      headcount: job.headcount,
      education: job.education,
      experience: job.experience || '',
      graduationYear: job.graduationYear || '',
      targetAudience: job.targetAudience || null,
      duty: data.duty || '',
      requirement: data.requirement || '',
      ownerHrId: job.ownerHrId
    })
    originalCategoryId.value = job.categoryId
    originalLocationId.value = job.locationId
    currentVersion.value = job.version
    if (!data.canWrite) {
      ElMessage.warning('你不是该职位的负责人，无法保存修改')
    }
  } catch (error) {
    toast(error, '职位加载失败')
    router.push({ name: 'hr-jobs' })
  } finally {
    loading.value = false
  }
}

async function submit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (isEdit.value) {
      await hrApi.updateJob(route.params.id, { ...form })
      ElMessage.success('已保存修改')
    } else {
      await hrApi.createJob({ ...form })
      ElMessage.success('职位已保存为草稿')
    }
    router.push({ name: 'hr-jobs' })
  } catch (error) {
    toast(error, '保存失败')
  } finally {
    saving.value = false
  }
}

async function submitAndPublish() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const created = await hrApi.createJob({ ...form })
    await hrApi.updateJobStatus(created.id, { targetStatus: 'PUBLISHED', version: 0 })
    ElMessage.success('职位已发布')
    router.push({ name: 'hr-jobs' })
  } catch (error) {
    toast(error, '发布失败')
  } finally {
    saving.value = false
  }
}

// 切换招聘类型时清掉互斥字段，避免残留值造成困惑
watch(isSocial, (social) => {
  if (social) {
    form.graduationYear = ''
    form.targetAudience = null
  } else {
    form.experience = ''
  }
})

onMounted(async () => {
  await loadOptions()
  await loadJob()
})
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

.card {
  border-radius: var(--zk-radius);
  border: 1px solid var(--zk-border);
}

.grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 24px;
  max-width: 900px;
}

.field-tip {
  margin-left: 12px;
  font-size: 12px;
  color: var(--zk-text-muted);
}

.save-note {
  margin: 4px 0 0 120px;
  font-size: 12px;
  color: var(--zk-text-muted);
}

@media (max-width: 900px) {
  .grid {
    grid-template-columns: 1fr;
  }
}
</style>
