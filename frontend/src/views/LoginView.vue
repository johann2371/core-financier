<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth.store'

const email = ref('')
const password = ref('')
const showPassword = ref(false)
const authStore = useAuthStore()
const router = useRouter()

const togglePassword = () => {
  showPassword.value = !showPassword.value
}

const handleLogin = async () => {
  await authStore.login(email.value, password.value)
  if (!authStore.error) {
    router.push('/')
  }
}
</script>

<template>
  <div class="login-wrapper">
    <!-- En-tête avec Logo -->
    <div class="brand-header">
      <div class="logo">
        <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><ellipse cx="12" cy="5" rx="9" ry="3"></ellipse><path d="M21 12c0 1.66-4 3-9 3s-9-1.34-9-3"></path><path d="M3 5v14c0 1.66 4 3 9 3s9-1.34 9-3V5"></path></svg>
      </div>
      <h1>Core Financier</h1>
      <p>Gestion financière de haute précision</p>
    </div>

    <!-- Carte de connexion -->
    <div class="login-card">
      <div class="card-header">
        <h2>Connexion</h2>
        <p>Accédez à votre espace sécurisé</p>
      </div>

      <form @submit.prevent="handleLogin" class="login-form">
        <div class="input-group">
          <label for="email">Identifiant ou Email</label>
          <input 
            id="email"
            v-model="email" 
            type="email" 
            required 
            placeholder="nom@exemple.fr" 
          />
        </div>
        
        <div class="input-group">
          <div class="label-row">
            <label for="password">Mot de passe</label>
            <a href="#" class="forgot-link">Mot de passe oublié ?</a>
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
              <svg v-if="!showPassword" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path><circle cx="12" cy="12" r="3"></circle></svg>
              <svg v-else xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path><line x1="1" y1="1" x2="23" y2="23"></line></svg>
            </button>
          </div>
        </div>

        <div v-if="authStore.error" class="error-msg">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>
          <span>{{ authStore.error }}</span>
        </div>

        <button :disabled="authStore.loading" type="submit" class="btn-primary">
          <span v-if="authStore.loading" class="spinner"></span>
          <span>{{ authStore.loading ? 'Connexion...' : 'Se connecter' }}</span>
          <svg v-if="!authStore.loading" class="arrow-icon" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="5" y1="12" x2="19" y2="12"></line><polyline points="12 5 19 12 12 19"></polyline></svg>
        </button>
      </form>

      <div class="secure-footer">
        <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path><polyline points="9 12 11 14 15 10"></polyline></svg>
        <span>ACCÈS SÉCURISÉ PAR CRYPTAGE AES-256</span>
      </div>
    </div>

    <!-- Liens du bas -->
    <div class="page-footer">
      <a href="#">Aide</a>
      <a href="#">Confidentialité</a>
      <a href="#">Conditions</a>
    </div>

    <!-- Statut du système en bas à droite -->
    <div class="system-status">
      <div class="status-dot"></div>
      <span>SYSTEM STATUS: OPERATIONAL</span>
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

.logo {
  width: 56px; height: 56px;
  margin: 0 auto 1rem auto;
  background-color: var(--c-primary); /* Couleur 3 */
  color: var(--c-surface); /* Couleur 2 */
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px -2px color-mix(in srgb, var(--c-primary) 30%, transparent);
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
