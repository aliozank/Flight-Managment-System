import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import MainLayout from '@/layouts/MainLayout.vue'
import LoginView from '@/views/LoginView.vue'
import DashboardView from '@/views/DashboardView.vue'
import FlightsView from '@/views/FlightsView.vue'
import ReferenceDataView from '@/views/ReferenceDataView.vue'
import UsersView from '@/views/UsersView.vue'
import ArchiveView from '@/views/ArchiveView.vue'
import MonitoringView from '@/views/MonitoringView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { title: 'Giriş Yap' }
    },
    {
      path: '/',
      component: MainLayout,
      redirect: '/dashboard',
      meta: { requiresAuth: true },
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          component: DashboardView,
          meta: { title: 'Operasyon Paneli', roles: ['ADMIN', 'OPERATIONS'] }
        },
        {
          path: 'flights',
          name: 'flights',
          component: FlightsView,
          meta: { title: 'Uçuş Yönetimi', roles: ['ADMIN', 'OPERATIONS'] }
        },
        {
          path: 'reference-data',
          name: 'reference-data',
          component: ReferenceDataView,
          meta: { title: 'Referans Veriler', roles: ['ADMIN', 'OPERATIONS', 'BI_ANALYST'] }
        },
        {
          path: 'users',
          name: 'users',
          component: UsersView,
          meta: { title: 'Kullanıcı Yönetimi', roles: ['ADMIN'] }
        },
        {
          path: 'archive',
          name: 'archive',
          component: ArchiveView,
          meta: { title: 'Uçuş Arşivi', roles: ['ADMIN', 'OPERATIONS', 'BI_ANALYST'] }
        },
        {
          path: 'radar',
          name: 'radar',
          component: () => import('@/views/LiveRadarView.vue'),
          meta: { title: 'Canlı Uçuş Radarı', roles: ['ADMIN', 'OPERATIONS'] }
        },
        {
          path: 'monitoring',
          name: 'monitoring',
          component: MonitoringView,
          meta: { title: 'Sistem İzleme', roles: ['ADMIN', 'DEVOPS'] }
        }
      ]
    }
  ]
})

router.beforeEach((to) => {
  const token = localStorage.getItem('accessToken')
  const authStore = useAuthStore()

  if (to.meta.requiresAuth && !token) {
    return {
      path: '/login',
      query: { redirect: to.fullPath }
    }
  }

  if (to.path === '/login' && token && authStore.defaultRoute !== '/login') {
    return authStore.defaultRoute
  }

  if (to.meta.roles && Array.isArray(to.meta.roles)) {
    const hasPermission = to.meta.roles.some((r) => authStore.hasRole(r))
    if (!hasPermission) {
      return authStore.defaultRoute
    }
  }
})

export default router
