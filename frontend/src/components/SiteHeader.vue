<template>
  <header :class="['site-header', { scrolled: scrolled || !isHome }]">
    <div class="zk-container inner">
      <router-link :to="{ name: 'home' }" class="brand">
        <img class="brand-logo" src="/favicon.svg" alt="熵基科技" />
        <span class="brand-text">· 招聘</span>
      </router-link>

      <!-- 导航栏只保留品牌标识、首页、社会招聘、校园招聘与登录入口（§7.1） -->
      <nav class="nav">
        <router-link :to="{ name: 'home' }" class="nav-item">首页</router-link>
        <router-link :to="{ name: 'social-jobs' }" class="nav-item">社会招聘</router-link>
        <router-link :to="{ name: 'campus-jobs' }" class="nav-item">校园招聘</router-link>
      </nav>

      <div class="actions">
        <el-button v-if="!auth.isLoggedIn" type="primary" round @click="auth.openModal('login')">
          登录
        </el-button>
        <el-dropdown v-else trigger="click" @command="handleCommand">
          <span class="account">
            <el-avatar :size="30" class="avatar">{{ initial }}</el-avatar>
            <span class="account-name">{{ auth.user.name }}</span>
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <template v-if="auth.isCandidate">
                <el-dropdown-item command="resume">我的简历</el-dropdown-item>
                <el-dropdown-item command="applications">我的投递</el-dropdown-item>
              </template>
              <el-dropdown-item v-if="auth.isHr" command="hr">进入 HR 后台</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
  </header>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const scrolled = ref(false)

const isHome = computed(() => route.name === 'home')
const initial = computed(() => (auth.user?.name || '?').slice(0, 1))

function onScroll() {
  scrolled.value = window.scrollY > 40
}

onMounted(() => {
  window.addEventListener('scroll', onScroll, { passive: true })
  onScroll()
})
onUnmounted(() => window.removeEventListener('scroll', onScroll))

async function handleCommand(command) {
  if (command === 'resume') router.push({ name: 'my-resume' })
  if (command === 'applications') router.push({ name: 'my-applications' })
  if (command === 'hr') router.push({ name: 'hr-dashboard' })
  if (command === 'logout') {
    await auth.logout()
    ElMessage.success('已退出登录')
    if (route.path.startsWith('/my') || route.path.startsWith('/hr')) {
      router.push({ name: 'home' })
    }
  }
}
</script>

<style scoped>
.site-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: var(--zk-header-height);
  z-index: 100;
  transition: background 0.3s ease, box-shadow 0.3s ease, backdrop-filter 0.3s ease;
}

/* 滚动后切换为毛玻璃背景，提高文字可读性（§7.1） */
.site-header.scrolled {
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: saturate(180%) blur(14px);
  box-shadow: 0 4px 20px rgba(13, 19, 48, 0.08);
}

.inner {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 700;
  letter-spacing: 0.5px;
  color: #fff;
}

.scrolled .brand {
  color: var(--zk-ink);
}

.brand-logo {
  height: 22px;
  width: auto;
  display: block;
}

.scrolled .brand-logo {
  filter: brightness(0.12);
}

.brand-text {
  font-size: 16px;
}

.nav {
  display: flex;
  gap: 30px;
}

.nav-item {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.9);
  padding: 6px 2px;
  border-bottom: 2px solid transparent;
  transition: color 0.2s, border-color 0.2s;
}

.scrolled .nav-item {
  color: var(--zk-text);
}

.nav-item:hover,
.nav-item.router-link-exact-active {
  color: var(--zk-cyan);
  border-color: var(--zk-cyan);
}

.scrolled .nav-item:hover,
.scrolled .nav-item.router-link-exact-active {
  color: var(--zk-primary);
  border-color: var(--zk-primary);
}

.account {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: #fff;
  font-size: 14px;
}

.scrolled .account {
  color: var(--zk-text);
}

.avatar {
  background: linear-gradient(135deg, var(--zk-primary), var(--zk-accent));
  color: #fff;
  font-size: 13px;
}

@media (max-width: 768px) {
  .brand-text {
    display: none;
  }
  .nav {
    gap: 18px;
  }
  .account-name {
    display: none;
  }
}
</style>
