<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useLangStore } from '../stores/lang.store'
import { useAuthStore } from '../stores/auth.store'
import { EyeIcon, EyeSlashIcon, ShieldCheckIcon, ArrowRightIcon, ExclamationTriangleIcon } from '@heroicons/vue/24/outline'
import logoFull from '../assets/images/logo-sodica.png'

const email = ref('')
const password = ref('')
const showPassword = ref(false)
const authStore = useAuthStore()
const langStore = useLangStore()
const t = computed(() => langStore.t)
const router = useRouter()

const togglePassword = () => {
  showPassword.value = !showPassword.value
}

const handleLogin = async () => {
  await authStore.login(email.value, password.value)
  if (!authStore.error) {
    if (authStore.userRole === 'ADMINISTRATEUR') {
      router.push('/admin')
    } else {
      router.push('/')
    }
  }
}
</script>

<template>
  <div class="login-wrapper">
    <!-- Carte de connexion -->
    <div class="login-card">
      <div class="card-header">
        <div class="login-logo-vector">
          <img :src="logoFull" alt="SODICA" />
        </div>
        <h2>{{ t("login.connexion") }}</h2>
        <p>{{ t("login.accesSecurise") }}</p>
      </div>

      <form @submit.prevent="handleLogin" class="login-form">
        <div class="input-group">
          <label for="email">{{ t("login.identifiant") }}</label>
          <input 
            id="email"
            v-model="email" 
            type="email" 
            required 
            :placeholder="t('login.placeholder')" 
          />
        </div>
        
        <div class="input-group">
          <div class="label-row">
            <label for="password">{{ t("login.motDePasse") }}</label>
            <a href="#" class="forgot-link">{{ t("login.motDePasseOublie") }}</a>
          </div>
          <div class="password-wrapper">
            <input 
              id="password"
              v-model="password" 
              :type="showPassword ? 'text' : 'password'" 
              required 
              placeholder="••••••••"
            />
            <button type="button" class="eye-btn" @click="togglePassword">
              <EyeSlashIcon v-if="!showPassword" class="w-5 h-5" />
              <EyeIcon v-else class="w-5 h-5" />
            </button>
          </div>
        </div>

        <div v-if="authStore.error" class="error-msg">
          <ExclamationTriangleIcon class="w-4 h-4" />
          <span>{{ authStore.error }}</span>
        </div>

        <button :disabled="authStore.loading" type="submit" class="btn-primary">
          <span v-if="authStore.loading" class="spinner"></span>
          <span>{{ authStore.loading ? t('login.enCours') : t('login.seConnecter') }}</span>
          <ArrowRightIcon v-if="!authStore.loading" class="w-5 h-5 ms-2" />
        </button>
      </form>

      <div class="secure-footer">
        <ShieldCheckIcon class="w-4 h-4" />
        <span>{{ t("login.securise") }}</span>
      </div>
    </div>

    <!-- Liens du bas -->
    <div class="page-footer">
      <a href="#">{{ t("login.aide") }}</a>
      <a href="#">{{ t("login.confidentialite") }}</a>
      <a href="#">{{ t("login.conditions") }}</a>
    </div>

    <!-- Statut du système en bas à droite -->
    <div class="system-status">
      <div class="status-dot"></div>
      <span>{{ t("login.systemStatus") }}</span>
    </div>
  </div>
</template>

<style scoped>
.login-wrapper {
  position: relative;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  /* Fond avec des points délicats (Dot grid) */
  background-color: var(--c-bg); /* Couleur 1 */
  background-image: radial-gradient(color-mix(in srgb, var(--c-text) 8%, transparent) 1px, transparent 1px);
  background-size: 24px 24px;
}

/* ==== EN-TÊTE LOGO ==== */
.brand-header {
  text-align: center;
  margin-bottom: 2rem;
}

.login-logo-vector {
  width: 260px;
  height: auto;
  margin: 0 auto 1.5rem auto;
  filter: drop-shadow(0 4px 12px rgba(37, 99, 235, 0.2));
}
.login-logo-vector img {
  width: 100%;
  height: auto;
  display: block;
}

.brand-header h1 {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--c-text); /* Couleur 4 */
  letter-spacing: -0.025em;
  margin-bottom: 0.25rem;
}

.brand-header p {
  font-size: 0.875rem;
  color: color-mix(in srgb, var(--c-text) 60%, transparent);
}

/* ==== CARTE PRINCIPALE ==== */
.login-card {
  width: 100%;
  max-width: 440px;
  background: var(--c-surface); /* Couleur 2 */
  border-radius: 12px;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.05), 0 0 0 1px rgba(0, 0, 0, 0.03);
  overflow: hidden;
}

.card-header {
  padding: 2.5rem 2.5rem 1.5rem 2.5rem;
  text-align: center;
}

.card-header h1 {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--c-primary);
  letter-spacing: -0.02em;
  margin-bottom: 0.5rem;
}

.card-header h2 {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--c-text); /* Couleur 4 */
  margin-bottom: 0.25rem;
}

.card-header p {
  font-size: 0.875rem;
  color: color-mix(in srgb, var(--c-text) 50%, transparent);
}

/* ==== FORMULAIRE ==== */
.login-form {
  padding: 0 2.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.input-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.label-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

label {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--c-text); /* Couleur 4 */
}

.forgot-link {
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--c-primary); /* Couleur 3 */
}

input {
  width: 100%;
  padding: 0.75rem 1rem;
  background-color: color-mix(in srgb, var(--c-bg) 50%, var(--c-surface)); /* Gris super léger */
  border: 1px solid color-mix(in srgb, var(--c-text) 15%, transparent);
  border-radius: 8px;
  color: var(--c-text);
  font-size: 0.95rem;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

input:focus {
  outline: none;
  border-color: var(--c-primary); /* Couleur 3 */
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--c-primary) 15%, transparent);
  background-color: var(--c-surface); /* Couleur 2 */
}

input::placeholder {
  color: color-mix(in srgb, var(--c-text) 35%, transparent);
}

.password-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.password-wrapper input {
  padding-right: 3rem;
}

.eye-btn {
  position: absolute;
  right: 12px;
  background: none;
  border: none;
  color: color-mix(in srgb, var(--c-text) 45%, transparent); /* Gris ardoise (Slate) */
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.2s ease;
}

.eye-btn:hover {
  color: var(--c-text);
}

/* ==== BOUTON PRIMAIRE ==== */
.btn-primary {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 0.5rem;
  width: 100%;
  padding: 0.875rem;
  margin-top: 0.5rem;
  background-color: var(--c-primary); /* Couleur 3 */
  color: var(--c-surface); /* Couleur 2 */
  border: none;
  border-radius: 8px;
  font-size: 0.95rem;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s ease, transform 0.1s ease;
}

.arrow-icon {
  margin-top: 1px;
}

.btn-primary:hover:not(:disabled) {
  background-color: color-mix(in srgb, var(--c-primary) 85%, black);
}

.btn-primary:active:not(:disabled) {
  transform: scale(0.98);
}

.btn-primary:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

/* ==== PIED DE CARTE (ACCÈS SÉCURISÉ) ==== */
.secure-footer {
  margin-top: 2.5rem;
  padding: 1rem 2.5rem;
  background-color: color-mix(in srgb, var(--c-bg) 50%, var(--c-surface));
  border-top: 1px solid color-mix(in srgb, var(--c-text) 5%, transparent);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  color: color-mix(in srgb, var(--c-text) 50%, transparent);
}

.secure-footer span {
  font-size: 0.7rem;
  font-weight: 600;
  letter-spacing: 0.05em;
  text-transform: uppercase;
}

/* ==== PIED DE PAGE & LIENS ==== */
.page-footer {
  display: flex;
  gap: 1.5rem;
  margin-top: 2.5rem;
}

.page-footer a {
  font-size: 0.8125rem;
  color: color-mix(in srgb, var(--c-text) 50%, transparent);
  font-weight: 500;
}

.page-footer a:hover {
  color: var(--c-text);
}

/* ==== SYSTEM STATUS ==== */
.system-status {
  position: absolute;
  bottom: 2rem;
  right: 2rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.status-dot {
  width: 8px; height: 8px;
  background-color: #10b981; /* Vert positif clair */
  border-radius: 50%;
  box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.2);
}

.system-status span {
  font-size: 0.65rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: color-mix(in srgb, var(--c-text) 40%, transparent);
}

/* ==== ERROR MSG ==== */
.error-msg {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  background: rgba(239, 68, 68, 0.05); 
  border-left: 2px solid #ef4444;
  border-radius: 6px;
  color: #ef4444;
  font-size: 0.8125rem;
  animation: shake 0.4s ease-in-out;
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-4px); }
  75% { transform: translateX(4px); }
}

/* === SPINNER === */
.spinner {
  width: 1.1rem;
  height: 1.1rem;
  border: 2px solid color-mix(in srgb, var(--c-surface) 30%, transparent);
  border-top-color: var(--c-surface);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
