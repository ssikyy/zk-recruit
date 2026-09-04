import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'

const routes = [
  {
    path: '/',
    component: () => import('@/layouts/PublicLayout.vue'),
    children: [
      { path: '', name: 'home', component: () => import('@/views/public/HomeView.vue') },
      {
        path: 'jobs',
        name: 'all-jobs',
        component: () => import('@/views/public/JobListView.vue'),
        props: { type: null }
      },
      {
        path: 'jobs/social',
        name: 'social-jobs',
        component: () => import('@/views/public/JobListView.vue'),
        props: { type: 'SOCIAL' }
      },
      {
        path: 'jobs/campus',
        name: 'campus-jobs',
        component: () => import('@/views/public/JobListView.vue'),
        props: { type: 'CAMPUS' }
      },
      {
        path: 'jobs/:id',
        name: 'job-detail',
        component: () => import('@/views/public/JobDetailView.vue')
      },
      {
        path: 'my/resume',
        name: 'my-resume',
        component: () => import('@/views/candidate/MyResumeView.vue'),
        meta: { requiresCandidate: true }
      },
      {
        path: 'my/applications',
        name: 'my-applications',
        component: () => import('@/views/candidate/MyApplicationsView.vue'),
        meta: { requiresCandidate: true }
      }
    ]
  },
  {
    path: '/hr',
    component: () => import('@/layouts/HrLayout.vue'),
    meta: { requiresHr: true },
    children: [
      { path: '', redirect: '/hr/dashboard' },
      { path: 'dashboard', name: 'hr-dashboard', component: () => import('@/views/hr/DashboardView.vue') },
      { path: 'jobs', name: 'hr-jobs', component: () => import('@/views/hr/JobManageView.vue') },
      { path: 'jobs/new', name: 'hr-job-new', component: () => import('@/views/hr/JobEditView.vue') },
      { path: 'jobs/:id/edit', name: 'hr-job-edit', component: () => import('@/views/hr/JobEditView.vue') },
      {
        path: 'applications',
        name: 'hr-applications',
        component: () => import('@/views/hr/ApplicationManageView.vue')
      },
      {
        path: 'applications/:id',
        name: 'hr-application-detail',
        component: () => import('@/views/hr/ApplicationDetailView.vue')
      },
      {
        path: 'config/dict',
        name: 'admin-dict',
        component: () => import('@/views/admin/DictView.vue'),
        meta: { requiresAdmin: true }
      },
      {
        path: 'config/hr-users',
        name: 'admin-hr-users',
        component: () => import('@/views/admin/HrUserView.vue'),
        meta: { requiresAdmin: true }
      }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (!auth.loaded) {
    await auth.bootstrap()
  }

  if (to.meta.requiresCandidate) {
    if (!auth.isLoggedIn) {
      auth.openModal('login')
      return { name: 'home' }
    }
    if (!auth.isCandidate) {
      ElMessage.warning('该页面仅求职者可访问')
      return { name: 'hr-dashboard' }
    }
  }

  if (to.meta.requiresHr) {
    if (!auth.isLoggedIn) {
      auth.openModal('login')
      return { name: 'home' }
    }
    if (!auth.isHr) {
      ElMessage.error('无权访问 HR 后台')
      return { name: 'home' }
    }
  }

  // 系统配置菜单对普通 HR 完全不可用（§6.2）
  if (to.meta.requiresAdmin && !auth.isAdmin) {
    ElMessage.error('需要管理员 HR 权限')
    return { name: 'hr-dashboard' }
  }

  return true
})

export default router
