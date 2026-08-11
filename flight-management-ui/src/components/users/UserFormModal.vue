<script setup lang="ts">
import { ref, reactive, watch, computed } from 'vue'
import { ElMessage, type FormInstance } from 'element-plus'
import flightApi from '@/services/api'
import type { UserResponse, RegisterRequest, UserUpdateRequest, UserStatus } from '@/types/user'
import type { RoleName } from '@/types/auth'

const props = defineProps<{
  visible: boolean
  userToEdit?: UserResponse | null
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'saved'): void
}>()

const formRef = ref<FormInstance>()
const saving = ref(false)

const isEditMode = computed(() => !!props.userToEdit)

const form = reactive({
  userName: '',
  userEmail: '',
  userPassword: '',
  userStatus: 'ACTIVE' as UserStatus,
  userRoleNames: ['OPERATIONS'] as RoleName[]
})

const resetForm = () => {
  if (props.userToEdit) {
    form.userName = props.userToEdit.userName
    form.userEmail = props.userToEdit.userEmail
    form.userPassword = ''
    form.userStatus = props.userToEdit.userStatus
    form.userRoleNames = [...props.userToEdit.userRoleNames]
  } else {
    form.userName = ''
    form.userEmail = ''
    form.userPassword = ''
    form.userStatus = 'ACTIVE'
    form.userRoleNames = ['OPERATIONS']
  }
}

watch(
  () => props.visible,
  (val) => {
    if (val) resetForm()
  }
)

const handleSave = async () => {
  saving.value = true
  try {
    if (isEditMode.value && props.userToEdit) {
      const payload: UserUpdateRequest = {
        userStatus: form.userStatus,
        userRoleNames: form.userRoleNames
      }
      await flightApi.put(`/api/users/${props.userToEdit.userId}`, payload)
      ElMessage.success('Kullanıcı güncellendi')
    } else {
      const payload: RegisterRequest = {
        userName: form.userName,
        userEmail: form.userEmail,
        userPassword: form.userPassword,
        userRoleNames: form.userRoleNames
      }
      await flightApi.post('/api/users', payload)
      ElMessage.success('Kullanıcı oluşturuldu')
    }
    emit('saved')
    emit('update:visible', false)
  } catch {
    // Handled in interceptor
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    :title="isEditMode ? 'Kullanıcı Düzenle' : 'Yeni Kullanıcı Oluştur'"
    width="520px"
    align-center
    :append-to-body="true"
    @update:model-value="emit('update:visible', $event)"
    destroy-on-close
  >
    <el-form ref="formRef" :model="form" label-position="top">
      <el-form-item label="Kullanıcı Adı" required>
        <el-input v-model="form.userName" :disabled="isEditMode" placeholder="Örn: ahmet.yilmaz" />
      </el-form-item>

      <el-form-item label="E-Posta Adresi" required>
        <el-input v-model="form.userEmail" :disabled="isEditMode" type="email" placeholder="ahmet@airline.com" />
      </el-form-item>

      <el-form-item v-if="!isEditMode" label="Parola (En az 1 büyük, 1 küçük harf, 1 rakam)" required>
        <el-input v-model="form.userPassword" type="password" show-password placeholder="••••••••" />
      </el-form-item>

      <el-form-item v-if="isEditMode" label="Kullanıcı Durumu" required>
        <el-select v-model="form.userStatus" style="width: 100%">
          <el-option label="Aktif (ACTIVE)" value="ACTIVE" />
          <el-option label="Pasif (INACTIVE)" value="INACTIVE" />
          <el-option label="Kilitli (LOCKED)" value="LOCKED" />
        </el-select>
      </el-form-item>

      <el-form-item label="Roller" required>
        <el-checkbox-group v-model="form.userRoleNames">
          <el-checkbox value="ADMIN">ADMIN</el-checkbox>
          <el-checkbox value="OPERATIONS">OPERATIONS</el-checkbox>
          <el-checkbox value="BI_ANALYST">BI_ANALYST</el-checkbox>
          <el-checkbox value="DEVOPS">DEVOPS</el-checkbox>
        </el-checkbox-group>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="emit('update:visible', false)">Vazgeç</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">Kaydet</el-button>
    </template>
  </el-dialog>
</template>
