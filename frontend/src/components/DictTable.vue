<template>
  <el-card shadow="never" class="card">
    <div class="toolbar">
      <el-input v-model="newName" placeholder="新增项名称" style="width: 200px" maxlength="20" />
      <el-input-number v-model="newSort" :min="0" :max="9999" placeholder="排序值" style="width: 140px" />
      <el-button type="primary" @click="handleCreate">新增</el-button>
    </div>

    <el-table :data="rows" :loading="loading" stripe>
      <el-table-column prop="name" label="名称" min-width="160">
        <template #default="{ row }">
          <el-input
            v-if="editingId === row.id"
            v-model="editForm.name"
            size="small"
            maxlength="20"
            style="width: 160px"
          />
          <span v-else>{{ row.name }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序值" width="120">
        <template #default="{ row }">
          <el-input-number
            v-if="editingId === row.id"
            v-model="editForm.sortOrder"
            size="small"
            :min="0"
            :max="9999"
            style="width: 110px"
          />
          <span v-else>{{ row.sortOrder }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'" size="small" effect="light">
            {{ row.status === 'ENABLED' ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="referenceCount" label="被职位引用" width="120" align="center" />
      <el-table-column label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <template v-if="editingId === row.id">
            <el-button text type="primary" @click="submitEdit(row)">保存</el-button>
            <el-button text @click="editingId = null">取消</el-button>
          </template>
          <template v-else>
            <el-button text type="primary" @click="startEdit(row)">编辑</el-button>
            <el-button
              v-if="row.status === 'ENABLED'"
              text
              type="warning"
              @click="$emit('status', row.id, 'DISABLED')"
            >
              停用
            </el-button>
            <el-button v-else text type="success" @click="$emit('status', row.id, 'ENABLED')">启用</el-button>
            <!-- 引用数 > 0 时删除不可用（§10.7、错误码 5001） -->
            <el-tooltip
              :content="row.referenceCount > 0 ? '已被职位引用，不可删除' : '第一版不提供删除，请使用停用'"
              placement="top"
            >
              <span><el-button text disabled>删除</el-button></span>
            </el-tooltip>
          </template>
        </template>
      </el-table-column>
      <template #empty>{{ emptyText }}</template>
    </el-table>
  </el-card>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

defineProps({
  rows: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  emptyText: { type: String, default: '暂无数据' }
})

const emit = defineEmits(['create', 'update', 'status'])

const newName = ref('')
const newSort = ref(undefined)
const editingId = ref(null)
const editForm = reactive({ name: '', sortOrder: 0 })

function handleCreate() {
  if (!newName.value.trim()) {
    ElMessage.warning('请填写名称')
    return
  }
  emit('create', { name: newName.value.trim(), sortOrder: newSort.value })
  newName.value = ''
  newSort.value = undefined
}

function startEdit(row) {
  editingId.value = row.id
  editForm.name = row.name
  editForm.sortOrder = row.sortOrder
}

function submitEdit(row) {
  if (!editForm.name.trim()) {
    ElMessage.warning('请填写名称')
    return
  }
  emit('update', row.id, { name: editForm.name.trim(), sortOrder: editForm.sortOrder })
  editingId.value = null
}
</script>

<style scoped>
.card {
  border-radius: var(--zk-radius);
  border: 1px solid var(--zk-border);
}

.toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 14px;
}
</style>
