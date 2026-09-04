<template>
  <router-view v-if="auth.loaded" />
  <div v-else class="boot">
    <el-icon class="is-loading"><Loading /></el-icon>
    <span>正在加载…</span>
  </div>
  <AuthModal />
</template>

<script setup>
import { onMounted } from 'vue'
import AuthModal from '@/components/AuthModal.vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()

onMounted(() => {
  if (!auth.loaded) {
    auth.bootstrap()
  }
})
</script>

<style scoped>
.boot {
  height: 60vh;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: var(--zk-text-muted);
}
</style>
