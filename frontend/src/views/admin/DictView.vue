<template>
  <div class="dict-view">
    <h2>职位类别 / 工作地点</h2>
    <el-alert
      class="notice"
      type="info"
      show-icon
      :closable="false"
      title="字典项只支持停用，不提供物理删除。停用后不再出现在官网筛选与职位新建下拉中，但已引用该项的历史职位仍正常展示。"
    />

    <el-tabs v-model="activeTab">
      <el-tab-pane label="职位类别" name="category">
        <DictTable
          :rows="categories"
          :loading="loading.category"
          empty-text="暂无职位类别"
          @create="(payload) => create('category', payload)"
          @update="(id, payload) => update('category', id, payload)"
          @status="(id, status) => changeStatus('category', id, status)"
        />
      </el-tab-pane>
      <el-tab-pane label="工作地点" name="location">
        <DictTable
          :rows="locations"
          :loading="loading.location"
          empty-text="暂无工作地点"
          @create="(payload) => create('location', payload)"
          @update="(id, payload) => update('location', id, payload)"
          @status="(id, status) => changeStatus('location', id, status)"
        />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api'
import { toast } from '@/api/http'
import DictTable from '@/components/DictTable.vue'

const activeTab = ref('category')
const categories = ref([])
const locations = ref([])
const loading = reactive({ category: false, location: false })

async function loadCategories() {
  loading.category = true
  try {
    categories.value = await adminApi.categories()
  } catch (error) {
    toast(error, '职位类别加载失败')
  } finally {
    loading.category = false
  }
}

async function loadLocations() {
  loading.location = true
  try {
    locations.value = await adminApi.locations()
  } catch (error) {
    toast(error, '工作地点加载失败')
  } finally {
    loading.location = false
  }
}

async function create(kind, payload) {
  try {
    if (kind === 'category') {
      await adminApi.createCategory(payload)
      await loadCategories()
    } else {
      await adminApi.createLocation(payload)
      await loadLocations()
    }
    ElMessage.success('已新增')
  } catch (error) {
    toast(error, '新增失败')
  }
}

async function update(kind, id, payload) {
  try {
    if (kind === 'category') {
      await adminApi.updateCategory(id, payload)
      await loadCategories()
    } else {
      await adminApi.updateLocation(id, payload)
      await loadLocations()
    }
    ElMessage.success('已保存')
  } catch (error) {
    toast(error, '保存失败')
  }
}

async function changeStatus(kind, id, status) {
  if (status === 'DISABLED') {
    try {
      await ElMessageBox.confirm(
        '停用后该项不再出现在新建职位与官网筛选中，已引用它的历史职位不受影响。',
        '确认停用？',
        { type: 'warning' }
      )
    } catch {
      return
    }
  }
  try {
    if (kind === 'category') {
      await adminApi.updateCategoryStatus(id, status)
      await loadCategories()
    } else {
      await adminApi.updateLocationStatus(id, status)
      await loadLocations()
    }
    ElMessage.success(status === 'ENABLED' ? '已启用' : '已停用')
  } catch (error) {
    toast(error, '操作失败')
  }
}

onMounted(() => {
  loadCategories()
  loadLocations()
})
</script>

<style scoped>
h2 {
  margin: 0 0 14px;
  font-size: 22px;
}

.notice {
  margin-bottom: 14px;
}
</style>
