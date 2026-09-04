<template>
  <div class="public-layout">
    <SiteHeader />
    <main :class="{ 'with-offset': !isHome }">
      <router-view />
    </main>
    <SiteFooter />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import SiteHeader from '@/components/SiteHeader.vue'
import SiteFooter from '@/components/SiteFooter.vue'

const route = useRoute()
const isHome = computed(() => route.name === 'home')
</script>

<style scoped>
.public-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

main {
  flex: 1;
}

/* 首页首屏使用透明导航栏，其余页面需要留出头部高度（§7.1） */
main.with-offset {
  padding-top: var(--zk-header-height);
}
</style>
