<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '../stores/auth.store'
import MainLayout from '../components/MainLayout.vue'

const authStore = useAuthStore()
const loading = ref(false)
const uploading = ref(false)
const fileInput = ref(null)
const message = ref({ type: '', text: '' })

const form = ref({
  nom: '',
  prenom: '',
  email: '',
  photoUrl: ''
})

onMounted(() => {
  if (authStore.user) {
    form.value = {
      nom: authStore.user.nom || '',
      prenom: authStore.user.prenom || '',
      email: authStore.user.email || '',
      photoUrl: authStore.user.photoUrl || ''
    }
  }
})

const triggerFileInput = () => {
  fileInput.value.click()
}

const handleFileUpload = async (event) => {
  const file = event.target.files[0]
  if (!file) return

  // Validation basique
  if (!file.type.startsWith('image/')) {
    message.value = { type: 'error', text: 'Veuillez sélectionner une image valide.' }
    return
  }

  uploading.value = true
  try {
    const res = await authStore.uploadProfilePhoto(file)
    // Mettre à jour l'URL dans le formulaire local pour qu'elle soit envoyée lors du prochain updateProfile
    if (res && res.photoUrl) {
      form.value.photoUrl = res.photoUrl
    }
    message.value = { type: 'success', text: 'Photo de profil mise à jour !' }
  } catch (error) {
    message.value = { type: 'error', text: "Erreur lors de l'envoi de la photo." }
  } finally {
    uploading.value = false
    setTimeout(() => { message.value = { type: '', text: '' } }, 3000)
  }
}

const handleSubmit = async () => {
  loading.value = true
  message.value = { type: '', text: '' }
  try {
    await authStore.updateProfile(form.value)
    message.value = { type: 'success', text: 'Informations mises à jour !' }
  } catch (error) {
    message.value = { type: 'error', text: error.response?.data?.error || 'Une erreur est survenue.' }
  } finally {
    loading.value = false
    setTimeout(() => { message.value = { type: '', text: '' } }, 5000)
  }
}
</script>

<template>
  <MainLayout>
    <template #title>Mon Profil</template>
    <template #subtitle>Gérez vos informations personnelles et votre photo</template>

    <div class="profile-container">
      <div class="profile-card fade-in">
        <div class="profile-header">
          <div class="avatar-manager">
            <div class="large-avatar" :style="{ backgroundImage: authStore.user?.photoUrl ? 'url(' + authStore.user.photoUrl + ')' : 'url(https://ui-avatars.com/api/?name=' + (form.prenom || 'A') + '&background=e0e7ff&color=1d4ed8)' }">
              <div class="avatar-overlay" @click="triggerFileInput">
                <span v-if="uploading" class="spinner small"></span>
                <svg v-else xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"></path><circle cx="12" cy="13" r="4"></circle></svg>
              </div>
            </div>
            <input type="file" ref="fileInput" @change="handleFileUpload" accept="image/*" style="display: none" />
            <button class="btn-upload-text" @click="triggerFileInput" :disabled="uploading">
              {{ uploading ? 'Envoi...' : 'Changer la photo' }}
            </button>
          </div>
          <div class="header-info">
            <h2>{{ form.prenom }} {{ form.nom }}</h2>
            <span class="role-badge">{{ authStore.userRole?.toLowerCase() }}</span>
          </div>
        </div>

        <form @submit.prevent="handleSubmit" class="profile-form">
          <div class="form-section">
            <h3 class="section-title">Informations Personnelles</h3>
            <div class="form-grid">
              <div class="form-group">
                <label>Nom</label>
                <input v-model="form.nom" type="text" placeholder="Gouaffo" required />
              </div>
              <div class="form-group">
                <label>Prénom</label>
                <input v-model="form.prenom" type="text" placeholder="Johann" required />
              </div>
              <div class="form-group full">
                <label>Adresse E-mail</label>
                <input v-model="form.email" type="email" placeholder="johann@example.com" required />
              </div>
            </div>
          </div>

          <div v-if="message.text" :class="['alert', message.type]">
            <svg v-if="message.type === 'success'" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"></polyline></svg>
            <svg v-else xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>
            {{ message.text }}
          </div>

          <div class="form-actions">
            <button type="submit" class="btn-save" :disabled="loading">
              <span v-if="loading" class="spinner"></span>
              {{ loading ? 'Enregistrement...' : 'Enregistrer les modifications' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </MainLayout>
</template>

<style scoped>
.profile-container {
  max-width: 600px;
  margin: 0 auto;
}

.profile-card {
  background: white;
  border-radius: 20px;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.05), 0 8px 10px -6px rgba(0, 0, 0, 0.05);
  overflow: hidden;
  border: 1px solid #f3f4f6;
}

.profile-header {
  background: linear-gradient(135deg, #f9fafb 0%, #f3f4f6 100%);
  padding: 2rem 2rem; /* Réduit de 3rem */
  display: flex;
  align-items: center;
  gap: 1.5rem; /* Réduit de 2rem */
  border-bottom: 1px solid #e5e7eb;
}

.avatar-manager {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
}

.large-avatar {
  width: 120px;
  height: 120px;
  border-radius: 50%; /* Changé de 30px à 50% pour cercle parfait */
  background-size: cover;
  background-position: center;
  position: relative;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
  border: 4px solid white;
}

.btn-upload-text {
  background: none;
  border: none;
  color: #2563eb;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
}

.btn-upload-text:hover {
  text-decoration: underline;
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.4); /* Un peu plus sombre pour meilleur contraste */
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  opacity: 0;
  transition: opacity 0.3s;
  cursor: pointer;
}

.large-avatar:hover .avatar-overlay {
  opacity: 1;
}

.header-info h2 {
  font-size: 1.875rem;
  font-weight: 800;
  color: #111827;
  margin: 0 0 0.5rem 0;
  letter-spacing: -0.025em;
}

.role-badge {
  display: inline-block;
  padding: 0.375rem 1rem;
  background: #e0e7ff;
  color: #4338ca;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.profile-form {
  padding: 2.5rem;
}

.form-section {
  margin-bottom: 2.5rem;
}

.section-title {
  font-size: 0.875rem;
  font-weight: 700;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 1.5rem;
  padding-bottom: 0.75rem;
  border-bottom: 1px solid #f3f4f6;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.5rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-group.full {
  grid-column: span 2;
}

.form-group label {
  font-size: 0.875rem;
  font-weight: 600;
  color: #374151;
}

.form-group input {
  padding: 0.75rem 1rem;
  border: 1.5px solid #e5e7eb;
  border-radius: 12px;
  font-size: 0.95rem;
  transition: all 0.2s;
  background: #f9fafb;
}

.form-group input:focus {
  outline: none;
  border-color: #3b82f6;
  background: white;
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.1);
}

.input-with-hint {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.input-hint {
  font-size: 0.75rem;
  color: #9ca3af;
  font-style: italic;
}

.alert {
  padding: 1rem 1.25rem;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1.5rem;
  font-size: 0.9rem;
  font-weight: 500;
  animation: slideIn 0.3s ease-out;
}

.alert.success {
  background: #ecfdf5;
  color: #047857;
  border: 1px solid #d1fae5;
}

.alert.error {
  background: #fef2f2;
  color: #b91c1c;
  border: 1px solid #fee2e2;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 1rem;
}

.btn-save {
  background: #2563eb;
  color: white;
  padding: 0.875rem 2rem;
  border-radius: 12px;
  font-size: 0.95rem;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 0.75rem;
  box-shadow: 0 4px 6px -1px rgba(37, 99, 235, 0.2);
}

.btn-save:hover:not(:disabled) {
  background: #1d4ed8;
  transform: translateY(-1px);
  box-shadow: 0 10px 15px -3px rgba(37, 99, 235, 0.3);
}

.btn-save:active:not(:disabled) {
  transform: translateY(0);
}

.btn-save:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.spinner {
  width: 18px;
  height: 18px;
  border: 2.5px solid rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  border-top-color: white;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@keyframes slideIn {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}

.fade-in {
  animation: fadeIn 0.5s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@media (max-width: 640px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
  .form-group.full {
    grid-column: span 1;
  }
  .profile-header {
    flex-direction: column;
    text-align: center;
    padding: 2rem 1.5rem;
  }
  .profile-form {
    padding: 1.5rem;
  }
}
</style>
