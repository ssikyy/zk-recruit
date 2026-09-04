<template>
  <el-container class="hr-layout">
    <el-aside width="216px" class="aside">
      <div class="logo">
        <span class="mark">ZK</span>
        <span>招聘管理后台</span>
      </div>
      <el-menu :default-active="activeMenu" router class="menu">
        <el-menu-item :index="'/hr/dashboard'">
          <el-icon><Odometer /></el-icon><span>工作台</span>
        </el-menu-item>
        <el-menu-item :index="'/hr/jobs'">
          <el-icon><Briefcase /></el-icon><span>职位管理</span>
        </el-menu-item>
        <el-menu-item :index="'/hr/applications'">
          <el-icon><Tickets /></el-icon><span>投递管理</span>
        </el-menu-item>

        <!-- 系统配置对普通 HR 完全不渲染（§6.2） -->
        <el-sub-menu v-if="auth.isAdmin" index="config">
          <template #title>
            <el-icon><Setting /></el-icon><span>系统配置</span>
          </template>
          <el-menu-item :index="'/hr/config/dict'">职位类别 / 工作地点</el-menu-item>
          <el-menu-item :index="'/hr/config/hr-users'">HR 账号管理</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-tag v-if="auth.isAdmin" type="warning" effect="plain" size="small">管理员 HR</el-tag>
          <el-tag v-else type="info" effect="plain" size="small">普通 HR</el-tag>
          <span class="tip">读全局、写按归属：你只能操作自己负责职位的投递</span>
        </div>
        <div class="header-right">
          <el-button text @click="goSite">查看招聘官网</el-button>
          <el-dropdown trigger="click" @command="handleCommand">
            <span class="account">
              <el-avatar :size="28" class="avatar">{{ initial }}</el-avatar>
              {{ auth.user?.name }}
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const activeMenu = computed(() => {
  if (route.path.startsWith('/hr/jobs')) return '/hr/jobs'
  if (route.path.startsWith('/hr/applications')) return '/hr/applications'
  return route.path
})
const initial = computed(() => (auth.user?.name || '?').slice(0, 1))

function goSite() {
  window.open('/', '_blank')
}

async function handleCommand(command) {
  if (command === 'logout') {
    await auth.logout()
    ElMessage.success('已退出登录')
    router.push({ name: 'home' })
  }
}
</script>

<style scoped>
.hr-layout {
  min-height: 100vh;
}

.aside {
  background: var(--zk-ink);
  color: #fff;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 60px;
  padding: 0 18px;
  color: #fff;
  font-weight: 700;
  font-size: 15px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 9px;
  background: linear-gradient(135deg, var(--zk-primary), var(--zk-accent));
  font-size: 13px;
}

.menu {
  background: transparent;
  border-right: none;
}

.menu :deep(.el-menu-item),
.menu :deep(.el-sub-menu__title) {
  color: rgba(255, 255, 255, 0.75);
}

.menu :deep(.el-menu-item:hover),
.menu :deep(.el-sub-menu__title:hover) {
  background: rgba(255, 255, 255, 0.06);
  color: #fff;
}

.menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, rgba(28, 72, 216, 0.55), transparent);
  color: #fff;
}

.menu :deep(.el-sub-menu .el-menu) {
  background: rgba(0, 0, 0, 0.2);
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid var(--zk-border);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.tip {
  font-size: 12px;
  color: var(--zk-text-muted);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.account {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 14px;
}

.avatar {
  background: linear-gradient(135deg, var(--zk-primary), var(--zk-accent));
  color: #fff;
  font-size: 12px;
}

.main {
  background: var(--zk-bg-soft);
  padding: 22px;
}

@media (max-width: 900px) {
  .tip {
    display: none;
  }
}
</style>
