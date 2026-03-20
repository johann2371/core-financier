<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth.store'

const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()

const handleLogout = () => {
  authStore.logout()
  router.push('/login')
}

// Vérifie si la route est active pour colorer le menu
const isActive = (path) => route.path === path

const currentDate = ref('')
const currentTime = ref('')

onMounted(() => {
  const updateTime = () => {
    const now = new Date()
    currentDate.value = now.toLocaleDateString('fr-FR', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })
    currentTime.value = now.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' })
  }
  updateTime()
  setInterval(updateTime, 60000)
})
</script>

<template>
  <div class="dashboard-layout">
    <!-- Sidebar Réutilisable -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <div class="logo-icon">
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><ellipse cx="12" cy="5" rx="9" ry="3"></ellipse><path d="M21 12c0 1.66-4 3-9 3s-9-1.34-9-3"></path><path d="M3 5v14c0 1.66 4 3 9 3s9-1.34 9-3V5"></path></svg>
        </div>
        <h2>Core Financier</h2>
      </div>

      <nav class="nav-menu">
        <router-link to="/" class="nav-item" :class="{ active: isActive('/') }">
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7"></rect><rect x="14" y="3" width="7" height="7"></rect><rect x="14" y="14" width="7" height="7"></rect><rect x="3" y="14" width="7" height="7"></rect></svg>
          Tableau de bord
        </router-link>
        
        <router-link to="/decaissements" class="nav-item" :class="{ active: isActive('/decaissements') }">
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2v20M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"></path></svg>
          Décaissements
        </router-link>

        <router-link to="/encaissements" class="nav-item" :class="{ active: isActive('/encaissements') }">
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12V7H5a2 2 0 0 1 0-4h14v4"></path><path d="M3 5v14a2 2 0 0 0 2 2h16v-5"></path><path d="M18 12a2 2 0 0 0 0 4h4v-4Z"></path></svg>
          Encaissements
        </router-link>

        <a href="#" class="nav-item">
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16c0 1.1.9 2 2 2h12a2 2 0 0 0 2-2V8l-6-6z"/><path d="M14 3v5h5M16 13H8M16 17H8M10 9H8"/></svg>
          Factures
        </a>
        
        <a href="#" class="nav-item">
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>
          Clients
        </a>

        <div class="nav-section-title">SYSTÈME</div>
        
        <a href="#" class="nav-item">
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"></circle><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path></svg>
          Paramètres
        </a>
      </nav>

      <div class="sidebar-footer">
        <div class="user-info">
          <div class="user-avatar" :style="{ backgroundImage: 'url(https://ui-avatars.com/api/?name=' + (authStore.user?.prenom || 'A') + '&background=e0e7ff&color=1d4ed8)' }"></div>
          <div class="user-details">
            <span class="user-name">{{ authStore.user?.prenom || 'Alex' }} {{ authStore.user?.nom || 'Henderson' }}</span>
            <span class="user-role">{{ authStore.userRole?.toLowerCase() || 'Comptable Senior' }}</span>
          </div>
        </div>
        <button @click="handleLogout" class="sidebar-logout" title="Déconnexion">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path><polyline points="16 17 21 12 16 7"></polyline><line x1="21" y1="12" x2="9" y2="12"></line></svg>
        </button>
      </div>
    </aside>

    <!-- Main Content Area -->
    <main class="main-content">
      <header class="topbar">
        <div class="topbar-left">
          <h1 class="page-title"><slot name="title">Bonjour, {{ authStore.user?.prenom || 'Alex' }}</slot></h1>
          <p class="page-subtitle"><slot name="subtitle"><span class="capitalize">{{ currentDate }}</span> | {{ currentTime }}</slot></p>
        </div>
        
        <div class="topbar-right">
          <slot name="actions"></slot>
          
          <div class="search-box">
            <svg class="search-icon" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
            <input type="text" placeholder="Rechercher (Ctrl + K)..." />
          </div>
          
          <button class="action-btn notifications-btn">
            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path><path d="M13.73 21a2 2 0 0 1-3.46 0"></path></svg>
            <span class="badge"></span>
          </button>
        </div>
      </header>

      <div class="page-content">
        <slot></slot>
      </div>
    </main>
  </div>
</template>

<style scoped>
.dashboard-layout {
  display: flex;
  min-height: 100vh;
  background-color: #f9fafb; /* Très léger gris fond principal */
  color: var(--c-text);
  font-family: var(--font-family);
}

/* ====== SIDEBAR ====== */
.sidebar {
  width: 250px;
  background-color: var(--c-surface); /* Blanc */
  border-right: 1px solid #e5e7eb; /* Bordure discrète */
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 1.5rem 1.5rem 2rem 1.5rem;
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.logo-icon {
  width: 32px; height: 32px;
  background-color: var(--c-primary);
  color: var(--c-surface);
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.sidebar-header h2 {
  font-size: 1.125rem;
  font-weight: 700;
  color: var(--c-primary);
  letter-spacing: -0.01em;
}

.nav-menu {
  flex: 1;
  padding: 0 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  padding: 0.625rem 1rem;
  border-radius: 6px;
  color: #4b5563; /* Gris moyen */
  font-size: 0.9rem;
  font-weight: 500;
  transition: all 0.15s ease;
  text-decoration: none;
}

.nav-item:hover {
  background-color: #f3f4f6;
  color: #111827;
}

.nav-item.active {
  background-color: #eff6ff; /* Bleu très léger */
  color: var(--c-primary);
  font-weight: 600;
}

.nav-section-title {
  margin-top: 1.5rem;
  margin-bottom: 0.5rem;
  padding-left: 1rem;
  font-size: 0.7rem;
  font-weight: 600;
  color: #9ca3af;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

/* Sidebar Footer (User info) */
.sidebar-footer {
  padding: 1.25rem 1rem;
  border-top: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.user-avatar {
  width: 36px; height: 36px;
  border-radius: 50%;
  background-size: cover;
  background-color: #e0e7ff;
}

.user-details {
  display: flex;
  flex-direction: column;
}

.user-name {
  font-size: 0.85rem;
  font-weight: 600;
  color: #111827;
}

.user-role {
  font-size: 0.7rem;
  font-weight: 400;
  color: #6b7280;
}

.sidebar-logout {
  background: none; border: none;
  color: #9ca3af;
  cursor: pointer;
  padding: 0.5rem;
  border-radius: 6px;
  transition: color 0.15s, background-color 0.15s;
}
.sidebar-logout:hover { color: #ef4444; background: #fee2e2; }

/* ====== MAIN CONTENT ====== */
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

.topbar {
  padding: 2rem 2.5rem 1.5rem;
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
}

.topbar-left {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.page-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: #111827;
  letter-spacing: -0.025em;
}

.page-subtitle {
  font-size: 0.875rem;
  color: #6b7280;
}
.capitalize { text-transform: capitalize; }

.topbar-right {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.search-box {
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 10px;
  color: #9ca3af;
}

.search-box input {
  padding: 0.5rem 1rem 0.5rem 2.25rem;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 0.875rem;
  background: white;
  color: #111827;
  width: 250px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.01);
}
.search-box input:focus {
  outline: none; border-color: var(--c-primary);
  box-shadow: 0 0 0 2px color-mix(in srgb, var(--c-primary) 10%, transparent);
}

.action-btn {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  width: 36px; height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #4b5563;
  cursor: pointer;
  position: relative;
  box-shadow: 0 1px 2px rgba(0,0,0,0.01);
}

.action-btn:hover { border-color: #d1d5db; color: #111827; }

.badge {
  position: absolute;
  top: -2px; right: -2px;
  background-color: #ef4444; 
  width: 8px; height: 8px;
  border-radius: 50%;
}

.page-content {
  padding: 0 2.5rem 3rem;
  display: flex;
  flex-direction: column;
  gap: 2rem;
}
</style>
