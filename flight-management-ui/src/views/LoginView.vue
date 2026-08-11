<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/authStore'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const loginSuccess = ref(false)

const loginForm = reactive({
  userName: '',
  userPassword: ''
})

const handleLogin = async () => {
  if (!loginForm.userName || !loginForm.userPassword) {
    ElMessage.warning('Lütfen kullanıcı adı ve parolanızı girin')
    return
  }

  loading.value = true
  try {
    await authStore.login({
      userName: loginForm.userName,
      userPassword: loginForm.userPassword
    })
    loginSuccess.value = true
    ElMessage.success('Yetkilendirme Başarılı! Uçuş Kontrol Merkezine Bağlanılıyor...')
    setTimeout(async () => {
      await router.push('/dashboard')
    }, 600)
  } catch {
    // Interceptor notification handles error messages
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-wrapper" :class="{ 'success-flash': loginSuccess }">
    <!-- Background Animated Particles -->
    <div class="bg-orb orb-1" />
    <div class="bg-orb orb-2" />
    <div class="bg-orb orb-3" />

    <!-- LEFT HERO PANEL: Animated Radar & Flight Systems -->
    <div class="hero-panel">
      <div class="radar-container">
        <div class="radar-grid" />
        <div class="radar-sweep" />
        <div class="radar-center-dot" />
        <div class="blip blip-1"><span>TK1984</span></div>
        <div class="blip blip-2"><span>LH1302</span></div>
        <div class="blip blip-3"><span>PC2022</span></div>
      </div>

      <div class="hero-content">
        <div class="system-badge glow-emerald">
          <span class="pulse-dot-green" />
          <span>LIVE AIRSPACE TELEMETRY v2.4</span>
        </div>

        <div class="brand-title">
          <div class="logo-box">
            <span class="logo-icon">✈️</span>
          </div>
          <h1>AIR-OPS</h1>
          <p class="subtitle">Realtime Flight Management & Fleet Control System</p>
        </div>

        <div class="hero-stats">
          <div class="stat-pill">
            <span class="stat-num glow-txt">99.9%</span>
            <span class="stat-txt">Uptime</span>
          </div>
          <div class="stat-pill">
            <span class="stat-num glow-txt">STOMP</span>
            <span class="stat-txt">Live WS</span>
          </div>
          <div class="stat-pill">
            <span class="stat-num glow-txt">Kafka</span>
            <span class="stat-txt">Archived Audit</span>
          </div>
        </div>

        <div class="hero-footer">
          <p>© 2026 Flight Management Systems • Havacılık Operasyon Standartları (Bölüm 12)</p>
        </div>
      </div>
    </div>

    <!-- RIGHT PANEL: Glassmorphic Auth Card -->
    <div class="auth-panel">
      <div class="auth-card fade-in" :class="{ 'card-success': loginSuccess }">
        <div class="card-header">
          <div class="header-badge">SECURE ACCESS</div>
          <h2>Kontrol Merkezine Giriş</h2>
          <p>Operasyon sistemine erişmek için kimlik bilgilerinizi doğrulayın</p>
        </div>

        <el-form :model="loginForm" label-position="top" @keyup.enter="handleLogin">
          <el-form-item label="Kullanıcı Adı (Username)">
            <el-input
              v-model="loginForm.userName"
              placeholder="Kullanıcı adınızı girin"
              size="large"
              clearable
              class="custom-input"
            />
          </el-form-item>

          <el-form-item label="Parola (Password)">
            <el-input
              v-model="loginForm.userPassword"
              type="password"
              placeholder="••••••••"
              size="large"
              show-password
              class="custom-input"
            />
          </el-form-item>

          <button
            type="button"
            class="emerald-login-btn"
            :class="{ loading: loading, success: loginSuccess }"
            :disabled="loading"
            @click="handleLogin"
          >
            <span v-if="!loading && !loginSuccess" class="btn-text">
              Giriş Yap <span class="arrow">➔</span>
            </span>
            <span v-else-if="loading" class="btn-spinner">
              <span class="spinner-ring" />
              Doğrulanıyor...
            </span>
            <span v-else-if="loginSuccess" class="btn-success-txt">
              ✓ Giriş Başarılı!
            </span>
          </button>
        </el-form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-wrapper {
  display: flex;
  min-height: 100vh;
  width: 100vw;
  background-color: #060911;
  position: relative;
  overflow: hidden;
  transition: all 0.5s ease;
}

.login-wrapper.success-flash {
  box-shadow: inset 0 0 100px rgba(16, 185, 129, 0.3);
}

/* Floating Background Orbs */
.bg-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.15;
  pointer-events: none;
  animation: floatOrb 12s infinite alternate ease-in-out;
}

.orb-1 {
  width: 400px;
  height: 400px;
  background: #10b981;
  top: -100px;
  left: -100px;
}

.orb-2 {
  width: 500px;
  height: 500px;
  background: #0284c7;
  bottom: -150px;
  right: -100px;
  animation-delay: -4s;
}

.orb-3 {
  width: 300px;
  height: 300px;
  background: #059669;
  top: 40%;
  left: 30%;
  animation-delay: -8s;
}

@keyframes floatOrb {
  0% { transform: translate(0, 0) scale(1); }
  100% { transform: translate(40px, 60px) scale(1.15); }
}

/* LEFT HERO PANEL */
.hero-panel {
  flex: 1.2;
  position: relative;
  background: radial-gradient(circle at 30% 40%, #0f172a 0%, #090d16 70%, #04070d 100%);
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 60px;
  border-right: 1px solid rgba(16, 185, 129, 0.15);
  overflow: hidden;
}

/* Radar Animation */
.radar-container {
  position: absolute;
  right: -60px;
  top: 50%;
  transform: translateY(-50%);
  width: 550px;
  height: 550px;
  border-radius: 50%;
  border: 1px solid rgba(16, 185, 129, 0.15);
  pointer-events: none;
}

.radar-grid {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background:
    radial-gradient(circle, transparent 30%, rgba(16, 185, 129, 0.05) 31%, transparent 32%),
    radial-gradient(circle, transparent 60%, rgba(16, 185, 129, 0.05) 61%, transparent 62%);
}

.radar-sweep {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: conic-gradient(from 0deg, rgba(16, 185, 129, 0.3) 0deg, rgba(16, 185, 129, 0) 60deg, transparent 360deg);
  animation: radarSweep 6s linear infinite;
}

@keyframes radarSweep {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.radar-center-dot {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 10px;
  height: 10px;
  background-color: #10b981;
  border-radius: 50%;
  box-shadow: 0 0 15px #10b981;
}

.blip {
  position: absolute;
  padding: 4px 8px;
  background: rgba(16, 185, 129, 0.2);
  border: 1px solid #10b981;
  border-radius: 6px;
  color: #34d399;
  font-size: 10px;
  font-weight: 800;
  animation: blipPulse 2s infinite ease-in-out;
}

.blip-1 { top: 25%; left: 35%; animation-delay: 0.5s; }
.blip-2 { top: 65%; left: 60%; animation-delay: 1.2s; }
.blip-3 { top: 40%; left: 75%; animation-delay: 1.8s; }

@keyframes blipPulse {
  0%, 100% { opacity: 0.3; transform: scale(0.95); }
  50% { opacity: 1; transform: scale(1.05); box-shadow: 0 0 12px rgba(16, 185, 129, 0.5); }
}

.hero-content {
  position: relative;
  z-index: 10;
  max-width: 540px;
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.system-badge.glow-emerald {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 16px;
  background: rgba(16, 185, 129, 0.12);
  border: 1px solid rgba(16, 185, 129, 0.35);
  border-radius: 30px;
  font-size: 11px;
  font-weight: 800;
  color: #34d399;
  letter-spacing: 1.5px;
  width: fit-content;
  box-shadow: 0 0 20px rgba(16, 185, 129, 0.15);
}

.pulse-dot-green {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #10b981;
  box-shadow: 0 0 10px #10b981;
  animation: pulseDot 1.5s infinite;
}

@keyframes pulseDot {
  0% { transform: scale(0.9); opacity: 0.8; }
  50% { transform: scale(1.3); opacity: 1; }
  100% { transform: scale(0.9); opacity: 0.8; }
}

.brand-title {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.logo-box {
  width: 60px;
  height: 60px;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.2) 0%, rgba(6, 182, 212, 0.2) 100%);
  border: 1px solid rgba(16, 185, 129, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 0 25px rgba(16, 185, 129, 0.2);
}

.logo-icon {
  font-size: 32px;
}

.brand-title h1 {
  margin: 0;
  font-size: 52px;
  font-weight: 900;
  letter-spacing: 3px;
  background: linear-gradient(135deg, #ffffff 0%, #34d399 50%, #38bdf8 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.subtitle {
  margin: 0;
  font-size: 16px;
  color: #94a3b8;
  font-weight: 400;
}

.hero-stats {
  display: flex;
  gap: 20px;
}

.stat-pill {
  display: flex;
  flex-direction: column;
  padding: 14px 22px;
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid rgba(16, 185, 129, 0.2);
  border-radius: 14px;
  min-width: 120px;
  backdrop-filter: blur(10px);
}

.stat-num.glow-txt {
  font-size: 20px;
  font-weight: 900;
  color: #34d399;
}

.stat-txt {
  font-size: 11px;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-top: 2px;
}

.hero-footer {
  margin-top: 10px;
  font-size: 12px;
  color: #475569;
}

/* RIGHT AUTH PANEL */
.auth-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  position: relative;
  z-index: 10;
}

.auth-card {
  width: 100%;
  max-width: 440px;
  padding: 44px;
  background: #ffffff;
  border-radius: 24px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}

.auth-card.card-success {
  border-color: #10b981;
  box-shadow: 0 0 40px rgba(16, 185, 129, 0.3);
}

.header-badge {
  display: inline-block;
  padding: 4px 10px;
  background-color: #ecfdf5;
  border: 1px solid #a7f3d0;
  color: #059669;
  font-size: 10px;
  font-weight: 800;
  border-radius: 6px;
  letter-spacing: 1px;
  margin-bottom: 12px;
}

.card-header h2 {
  margin: 0;
  font-size: 26px;
  font-weight: 800;
  color: #0f172a;
}

.card-header p {
  margin: 6px 0 28px 0;
  font-size: 14px;
  color: #64748b;
}

/* CUSTOM EMERALD BUTTON */
.emerald-login-btn {
  width: 100%;
  height: 52px;
  margin-top: 18px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, #10b981 0%, #059669 50%, #0284c7 100%);
  background-size: 200% 200%;
  color: #ffffff;
  font-size: 16px;
  font-weight: 800;
  letter-spacing: 0.5px;
  cursor: pointer;
  box-shadow: 0 8px 24px rgba(16, 185, 129, 0.35);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.emerald-login-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 12px 32px rgba(16, 185, 129, 0.5);
  background-position: right center;
}

.emerald-login-btn:active:not(:disabled) {
  transform: translateY(0);
  box-shadow: 0 4px 16px rgba(16, 185, 129, 0.3);
}

.emerald-login-btn.loading {
  background: #059669;
  cursor: wait;
}

.emerald-login-btn.success {
  background: #10b981;
  box-shadow: 0 0 30px rgba(16, 185, 129, 0.7);
}

.btn-text {
  display: flex;
  align-items: center;
  gap: 10px;
}

.arrow {
  transition: transform 0.2s ease;
}

.emerald-login-btn:hover .arrow {
  transform: translateX(4px);
}

.btn-spinner {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
}

.spinner-ring {
  width: 18px;
  height: 18px;
  border: 3px solid rgba(255, 255, 255, 0.3);
  border-top-color: #ffffff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.btn-success-txt {
  font-size: 16px;
  font-weight: 800;
}

@media (max-width: 960px) {
  .login-wrapper {
    flex-direction: column;
  }
  .hero-panel {
    display: none;
  }
}
</style>