<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/authStore'
import { useReferenceStore } from '@/stores/referenceStore'
import { useWebSocketStore } from '@/stores/webSocketStore'
import WebSocketBadge from '@/components/common/WebSocketBadge.vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const referenceStore = useReferenceStore()
const wsStore = useWebSocketStore()

const drawerVisible = ref(false)

onMounted(async () => {
  if (authStore.isAuthenticated) {
    referenceStore.fetchAllReferences()
    wsStore.connect()
  }
})

onUnmounted(() => {
  wsStore.disconnect()
})

const handleOpenNotifications = () => {
  drawerVisible.value = true
  wsStore.markAllAsRead()
}

const handleLogout = async () => {
  wsStore.disconnect()
  authStore.logout()
  ElMessage.success('Çıkış yapıldı')
  await router.push('/login')
}
</script>

<template>
  <div class="app-layout">
    <!-- Sidebar -->
    <aside class="app-sidebar">
      <div class="brand">
        <div class="logo-box">
          <span class="logo-icon">✈️</span>
        </div>
        <div class="logo-text">
          <h2>AIR-OPS</h2>
          <span class="sub-badge">CONTROL CENTER</span>
        </div>
      </div>

      <nav class="sidebar-nav">
        <router-link to="/dashboard" class="nav-item" :class="{ active: route.path === '/dashboard' }">
          <span class="nav-icon">📊</span>
          <span class="nav-text">Dashboard</span>
        </router-link>

        <router-link to="/flights" class="nav-item" :class="{ active: route.path.startsWith('/flights') }">
          <span class="nav-icon">🛫</span>
          <span class="nav-text">Uçuş Yönetimi</span>
        </router-link>

        <router-link to="/radar" class="nav-item" :class="{ active: route.path.startsWith('/radar') }">
          <span class="nav-icon">🛰️</span>
          <span class="nav-text">Canlı Radarı</span>
        </router-link>

        <router-link to="/reference-data" class="nav-item" :class="{ active: route.path.startsWith('/reference-data') }">
          <span class="nav-icon">📚</span>
          <span class="nav-text">Referans Veriler</span>
        </router-link>

        <router-link
          v-if="authStore.canManageUsers"
          to="/users"
          class="nav-item"
          :class="{ active: route.path.startsWith('/users') }"
        >
          <span class="nav-icon">👥</span>
          <span class="nav-text">Kullanıcılar</span>
        </router-link>

        <router-link to="/archive" class="nav-item" :class="{ active: route.path.startsWith('/archive') }">
          <span class="nav-icon">🗄️</span>
          <span class="nav-text">Uçuş Arşivi</span>
        </router-link>

        <router-link to="/monitoring" class="nav-item" :class="{ active: route.path.startsWith('/monitoring') }">
          <span class="nav-icon">🖥️</span>
          <span class="nav-text">Sistem İzleme</span>
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <WebSocketBadge />
      </div>
    </aside>

    <!-- Main Content Wrapper -->
    <div class="app-main">
      <header class="app-header">
        <div class="header-left">
          <h1 class="page-title">{{ route.meta.title || 'Uçuş Yönetim Sistemi' }}</h1>
        </div>

        <div class="header-right">
          <!-- STOMP Live Notification Bell -->
          <div class="notification-bell" @click="handleOpenNotifications">
            <span class="bell-icon">🔔</span>
            <el-badge
              v-if="wsStore.unreadCount > 0"
              :value="wsStore.unreadCount"
              class="bell-badge"
              type="danger"
            />
          </div>

          <div class="user-profile" v-if="authStore.user">
            <div class="user-avatar">{{ authStore.user.userName.charAt(0).toUpperCase() }}</div>
            <div class="user-info">
              <span class="user-name">{{ authStore.user.userName }}</span>
              <div class="user-roles">
                <el-tag
                  v-for="role in authStore.roles"
                  :key="role"
                  size="small"
                  type="success"
                  effect="dark"
                  class="role-badge"
                >
                  {{ role.replace('ROLE_', '') }}
                </el-tag>
              </div>
            </div>
          </div>

          <el-button type="danger" plain size="default" @click="handleLogout">
            Çıkış Yap
          </el-button>
        </div>
      </header>

      <main class="app-content">
        <div class="content-container fade-in">
          <router-view />
        </div>
      </main>
    </div>

    <!-- Live STOMP Notifications Drawer -->
    <el-drawer
      v-if="drawerVisible"
      v-model="drawerVisible"
      title="🔔 Canlı STOMP Olay Akışı (Realtime Feed)"
      size="420px"
      :append-to-body="true"
      destroy-on-close
    >
      <div class="notif-drawer-content">
        <div class="drawer-actions">
          <span class="notif-count">{{ wsStore.notifications.length }} Kayıtlı Olay</span>
          <el-button size="small" type="danger" plain @click="wsStore.clearNotifications">
            Temizle
          </el-button>
        </div>

        <div v-if="wsStore.notifications.length === 0" class="empty-notif">
          <div class="empty-icon">📡</div>
          <p>Henüz yeni bir canlı STOMP uçuş olayı alınmadı.</p>
          <span class="sub">Uçuş eklendiğinde veya güncellendiğinde burada anında görünecektir.</span>
        </div>

        <div v-else class="notif-list">
          <div
            v-for="item in wsStore.notifications"
            :key="item.id"
            class="notif-item"
            :class="item.type"
          >
            <div class="notif-header">
              <span class="notif-title">{{ item.title }}</span>
              <span class="notif-time">{{ item.timestamp }}</span>
            </div>
            <p class="notif-msg">{{ item.message }}</p>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.app-layout {
  display: flex;
  min-height: 100vh;
  width: 100vw;
  background-color: #f8fafc;
  color: #0f172a;
}

.app-sidebar {
  width: 260px;
  background-color: #090d16;
  color: #f8fafc;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  box-shadow: 4px 0 20px rgba(15, 23, 42, 0.08);
  z-index: 20;
  border-right: 1px solid rgba(255, 255, 255, 0.06);
}

.brand {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 22px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.logo-box {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.25) 0%, rgba(2, 132, 199, 0.25) 100%);
  border: 1px solid rgba(16, 185, 129, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-icon {
  font-size: 20px;
}

.logo-text h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 900;
  letter-spacing: 1.5px;
  background: linear-gradient(135deg, #ffffff 0%, #34d399 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.sub-badge {
  font-size: 9px;
  color: #10b981;
  font-weight: 800;
  letter-spacing: 1px;
}

.sidebar-nav {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 20px 12px;
  flex: 1;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 10px;
  color: #94a3b8;
  text-decoration: none;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.nav-item:hover {
  background-color: rgba(255, 255, 255, 0.06);
  color: #ffffff;
  transform: translateX(4px);
}

.nav-item.active {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: #ffffff;
  font-weight: 800;
  box-shadow: 0 4px 16px rgba(16, 185, 129, 0.4);
}

.nav-icon {
  font-size: 18px;
}

.sidebar-footer {
  padding: 18px 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.app-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  height: 100vh;
  overflow: hidden;
}

.app-header {
  height: 72px;
  background-color: #ffffff;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 36px;
  flex-shrink: 0;
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.03);
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 800;
  color: #0f172a;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.notification-bell {
  position: relative;
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background-color: #f1f5f9;
  border: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
}

.notification-bell:hover {
  background-color: #e0f2fe;
  border-color: #38bdf8;
  transform: scale(1.05);
}

.bell-icon {
  font-size: 18px;
}

.bell-badge {
  position: absolute;
  top: -4px;
  right: -4px;
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 900;
  font-size: 17px;
  box-shadow: 0 0 15px rgba(16, 185, 129, 0.3);
  border: 2px solid #ffffff;
}

.user-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.user-name {
  font-size: 14px;
  font-weight: 800;
  color: #0f172a;
}

.user-roles {
  display: flex;
  gap: 4px;
}

.role-badge {
  font-size: 10px;
  padding: 0 6px;
  height: 18px;
  line-height: 16px;
  border-radius: 6px !important;
}

.app-content {
  flex: 1;
  padding: 32px;
  overflow-y: auto;
  background-color: #f8fafc;
}

.content-container {
  max-width: 1600px;
  margin: 0 auto;
  width: 100%;
}

/* Notification Drawer */
.notif-drawer-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
}

.drawer-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 12px;
  border-bottom: 1px solid #e2e8f0;
}

.notif-count {
  font-size: 12px;
  font-weight: 700;
  color: #64748b;
  text-transform: uppercase;
}

.empty-notif {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.empty-notif p {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.empty-notif .sub {
  margin-top: 6px;
  font-size: 12px;
  color: #94a3b8;
}

.notif-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  overflow-y: auto;
}

.notif-item {
  padding: 14px;
  border-radius: 10px;
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  gap: 6px;
  transition: all 0.2s ease;
}

.notif-item:hover {
  transform: translateX(2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.notif-item.danger {
  border-left: 4px solid #ef4444;
  background-color: #fef2f2;
}

.notif-item.warning {
  border-left: 4px solid #f59e0b;
  background-color: #fffbeb;
}

.notif-item.success {
  border-left: 4px solid #10b981;
  background-color: #f0fdf4;
}

.notif-item.info {
  border-left: 4px solid #0284c7;
  background-color: #f0f9ff;
}

.notif-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.notif-title {
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
}

.notif-time {
  font-size: 11px;
  color: #94a3b8;
}

.notif-msg {
  margin: 0;
  font-size: 12px;
  color: #475569;
  line-height: 1.4;
}
</style>
