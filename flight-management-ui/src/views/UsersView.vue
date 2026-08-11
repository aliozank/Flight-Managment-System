<script setup lang="ts">
import { ref, onMounted } from 'vue'
import flightApi from '@/services/api'
import StatusTag from '@/components/common/StatusTag.vue'
import UserFormModal from '@/components/users/UserFormModal.vue'
import type { UserResponse } from '@/types/user'

const users = ref<UserResponse[]>([])
const loading = ref(false)
const modalVisible = ref(false)
const selectedUserToEdit = ref<UserResponse | null>(null)

const fetchUsers = async () => {
  loading.value = true
  try {
    const response = await flightApi.get<UserResponse[]>('/api/users')
    users.value = response.data || []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchUsers()
})

const handleOpenCreate = () => {
  selectedUserToEdit.value = null
  modalVisible.value = true
}

const handleOpenEdit = (user: UserResponse) => {
  selectedUserToEdit.value = user
  modalVisible.value = true
}
</script>

<template>
  <div class="users-view">
    <div class="header-bar">
      <div>
        <h2>Sistem Kullanıcıları Yönetimi</h2>
        <p>Sistemdeki tüm yetkili hesapların rol ve durum yönetimi</p>
      </div>

      <el-button type="primary" @click="handleOpenCreate">
        👤 Yeni Kullanıcı Oluştur
      </el-button>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="users" stripe style="width: 100%">
        <el-table-column prop="userId" label="ID" width="80" />
        <el-table-column prop="userName" label="Kullanıcı Adı" min-width="160" />
        <el-table-column prop="userEmail" label="E-Posta" min-width="200" />
        <el-table-column label="Durum" width="130">
          <template #default="{ row }">
            <StatusTag :status="row.userStatus" />
          </template>
        </el-table-column>

        <el-table-column label="Roller" min-width="220">
          <template #default="{ row }">
            <div class="roles-tags">
              <el-tag
                v-for="role in row.userRoleNames"
                :key="role"
                size="small"
                type="info"
              >
                {{ role }}
              </el-tag>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="Son Giriş" min-width="180">
          <template #default="{ row }">
            {{ row.userLastLoginAt ? new Date(row.userLastLoginAt).toLocaleString('tr-TR') : '-' }}
          </template>
        </el-table-column>

        <el-table-column label="İşlemler" width="120" align="center">
          <template #default="{ row }">
            <el-button size="small" type="primary" class="action-btn-edit" @click="handleOpenEdit(row)">
              Düzenle
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <UserFormModal
      v-if="modalVisible"
      v-model:visible="modalVisible"
      :user-to-edit="selectedUserToEdit"
      @saved="fetchUsers"
    />
  </div>
</template>

<style scoped>
.users-view {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-bar h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
}

.header-bar p {
  margin: 4px 0 0 0;
  font-size: 13px;
  color: #64748b;
}

.roles-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
</style>
