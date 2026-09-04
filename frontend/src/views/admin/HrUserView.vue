<template>
  <div class="hr-user">
    <div class="head">
      <h2>HR 账号管理</h2>
      <div>
        <el-button @click="candidateResetDialog = true">重置求职者密码</el-button>
        <el-button type="primary" @click="openCreate">新增 HR</el-button>
      </div>
    </div>

    <el-alert
      class="notice"
      type="info"
      show-icon
      :closable="false"
      title="账号不支持删除，只能停用；停用前必须先转移其名下所有职位。系统必须始终保留至少一个启用状态的管理员。新建与重置密码后请线下告知本人，系统不发送任何通知。"
    />

    <el-card shadow="never" class="card">
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="email" label="邮箱（登录账号）" min-width="200" />
        <el-table-column label="角色" width="120">
          <template #default="{ row }">
            <el-tag :type="row.hrAdmin ? 'warning' : 'info'" size="small" effect="light">
              {{ row.hrAdmin ? '管理员 HR' : '普通 HR' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'danger'" size="small" effect="light">
              {{ row.status === 'ENABLED' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ownedJobCount" label="负责职位" width="100" align="center" />
        <el-table-column prop="lastLoginAt" label="最后登录" width="160">
          <template #default="{ row }">{{ row.lastLoginAt || '未登录' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button text type="warning" @click="resetPassword(row)">重置密码</el-button>
            <el-button
              v-if="row.status === 'ENABLED'"
              text
              type="danger"
              @click="changeStatus(row, 'DISABLED')"
            >
              停用
            </el-button>
            <el-button v-else text type="success" @click="changeStatus(row, 'ENABLED')">启用</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pager"
        layout="prev, pager, next, total"
        :total="total"
        :page-size="pageSize"
        :current-page="page"
        @current-change="handlePage"
      />
    </el-card>

    <el-dialog v-model="formDialog" :title="editingId ? '编辑 HR 账号' : '新增 HR 账号'" width="440px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item v-if="!editingId" label="初始密码" prop="password">
          <el-input v-model="form.password" placeholder="留空则由系统生成临时密码" show-password />
        </el-form-item>
        <el-form-item label="管理员">
          <el-switch v-model="form.hrAdmin" />
          <span class="switch-tip">管理员可管理账号、字典与职位归属</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="candidateResetDialog" title="重置求职者密码" width="420px">
      <el-alert
        type="info"
        show-icon
        :closable="false"
        title="第一版不提供自助找回，求职者忘记密码时由管理员在此重置，并线下告知临时密码。"
      />
      <el-input v-model="candidateEmail" placeholder="求职者邮箱" class="reset-input" />
      <template #footer>
        <el-button @click="candidateResetDialog = false">取消</el-button>
        <el-button type="primary" :loading="resetting" @click="doCandidateReset">重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api'
import { toast } from '@/api/http'

const rows = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const loading = ref(false)

const formDialog = ref(false)
const formRef = ref()
const saving = ref(false)
const editingId = ref(null)
const form = reactive({ name: '', email: '', password: '', hrAdmin: false })

const candidateResetDialog = ref(false)
const candidateEmail = ref('')
const resetting = ref(false)

const rules = {
  name: [{ required: true, min: 2, max: 20, message: '姓名长度需为 2-20 位', trigger: 'blur' }],
  email: [
    { required: true, message: '请填写邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  password: [{ min: 8, max: 20, message: '密码长度需为 8-20 位', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    const data = await adminApi.hrUsers({ page: page.value, size: pageSize })
    rows.value = data?.list || []
    total.value = data?.total || 0
  } catch (error) {
    toast(error, '账号列表加载失败')
  } finally {
    loading.value = false
  }
}

function handlePage(value) {
  page.value = value
  load()
}

function openCreate() {
  editingId.value = null
  Object.assign(form, { name: '', email: '', password: '', hrAdmin: false })
  formDialog.value = true
}

function openEdit(row) {
  editingId.value = row.id
  Object.assign(form, { name: row.name, email: row.email, password: '', hrAdmin: row.hrAdmin })
  formDialog.value = true
}

async function submit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (editingId.value) {
      await adminApi.updateHrUser(editingId.value, {
        name: form.name,
        email: form.email,
        hrAdmin: form.hrAdmin
      })
      ElMessage.success('已保存')
    } else {
      const created = await adminApi.createHrUser({ ...form })
      formDialog.value = false
      // 临时密码只返回一次，必须提示管理员立即记录（§10.9）
      await ElMessageBox.alert(
        `账号：${created.email}\n临时密码：${created.temporaryPassword}\n\n请立即记录并线下告知本人，系统不会发送通知，密码不会再次显示。`,
        'HR 账号已创建',
        { type: 'success', customClass: 'pre-wrap-box' }
      )
    }
    formDialog.value = false
    load()
  } catch (error) {
    toast(error, '保存失败')
  } finally {
    saving.value = false
  }
}

async function changeStatus(row, status) {
  if (status === 'DISABLED') {
    try {
      await ElMessageBox.confirm(
        row.ownedJobCount > 0
          ? `该账号仍负责 ${row.ownedJobCount} 个职位，需要先转移归属才能停用。`
          : '停用后该账号无法登录，但历史操作记录会完整保留。',
        '确认停用？',
        { type: 'warning' }
      )
    } catch {
      return
    }
  }
  try {
    await adminApi.updateHrUserStatus(row.id, status)
    ElMessage.success(status === 'ENABLED' ? '已启用' : '已停用')
    load()
  } catch (error) {
    toast(error, '操作失败')
  }
}

async function resetPassword(row) {
  try {
    await ElMessageBox.confirm('重置后原密码立即失效，需线下告知本人新的临时密码。', '确认重置密码？', {
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    const result = await adminApi.resetHrPassword(row.id)
    await ElMessageBox.alert(
      `账号：${row.email}\n临时密码：${result.temporaryPassword}\n\n密码只显示一次，请立即记录。`,
      '密码已重置',
      { type: 'success', customClass: 'pre-wrap-box' }
    )
  } catch (error) {
    toast(error, '重置失败')
  }
}

async function doCandidateReset() {
  if (!candidateEmail.value.trim()) {
    ElMessage.warning('请填写求职者邮箱')
    return
  }
  resetting.value = true
  try {
    const result = await adminApi.resetCandidatePassword(candidateEmail.value.trim())
    candidateResetDialog.value = false
    await ElMessageBox.alert(
      `账号：${candidateEmail.value}\n临时密码：${result.temporaryPassword}\n\n密码只显示一次，请立即记录并告知本人。`,
      '密码已重置',
      { type: 'success', customClass: 'pre-wrap-box' }
    )
    candidateEmail.value = ''
  } catch (error) {
    toast(error, '重置失败')
  } finally {
    resetting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.head h2 {
  margin: 0;
  font-size: 22px;
}

.notice {
  margin-bottom: 14px;
}

.card {
  border-radius: var(--zk-radius);
  border: 1px solid var(--zk-border);
}

.switch-tip {
  margin-left: 12px;
  font-size: 12px;
  color: var(--zk-text-muted);
}

.reset-input {
  margin-top: 14px;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>

<style>
/* 临时密码需要保留换行显示 */
.pre-wrap-box .el-message-box__message {
  white-space: pre-wrap;
}
</style>
