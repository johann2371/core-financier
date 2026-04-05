<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth.store'
import { useUiStore } from '../stores/ui.store'
import { useNotificationStore } from '../stores/notification.store'
import { useSearchStore } from '../stores/search.store'

const authStore = useAuthStore()
const uiStore = useUiStore()
const notificationStore = useNotificationStore()
const searchStore = useSearchStore()
const router = useRouter()
const route = useRoute()

const searchInput = ref('')
let searchTimeout = null

const handleSearch = (e) => {
  searchInput.value = e.target.value
  clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => {
    searchStore.performSearch(searchInput.value)
  }, 400)
}

const goToResult = (result) => {
  searchStore.closeResults()
  searchInput.value = ''
  router.push(result.url)
}

const showLogoutModal = ref(false)
const showNotifications = ref(false)
const expandedNotifId = ref(null)
const isSidebarOpen = ref(false)
const isDarkMode = ref(localStorage.getItem('darkMode') === 'true')

// Appliquer le mode sombre au chargement
if (isDarkMode.value) document.body.classList.add('dark-mode')

const toggleDarkMode = () => {
  isDarkMode.value = !isDarkMode.value
  document.body.classList.toggle('dark-mode', isDarkMode.value)
  localStorage.setItem('darkMode', isDarkMode.value)
}

const searchRef = ref(null)

const handleClickOutside = (e) => {
  if (searchRef.value && !searchRef.value.contains(e.target)) {
    searchStore.closeResults()
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

import { onUnmounted } from 'vue'
onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})

const toggleSidebar = () => {
  isSidebarOpen.value = !isSidebarOpen.value
}

const toggleNotifications = () => {
  showNotifications.value = !showNotifications.value
  if (!showNotifications.value) expandedNotifId.value = null
}

const toggleExpand = (id) => {
  expandedNotifId.value = expandedNotifId.value === id ? null : id
}

const markAsRead = async (id) => {
  await notificationStore.markAsRead(id)
  if (expandedNotifId.value === id) expandedNotifId.value = null
}

const markAllAsRead = async () => {
  for (const notif of notificationStore.unreadNotifications) {
    await notificationStore.markAsRead(notif.id)
  }
}

const handleLogout = () => {
  showLogoutModal.value = true
}

const confirmLogout = () => {
  showLogoutModal.value = false
  authStore.logout()
  router.push('/login')
}

const cancelLogout = () => {
  showLogoutModal.value = false
}

// Fermeture au clic extérieur
onMounted(() => {
  window.addEventListener('click', (e) => {
    if (showNotifications.value && !e.target.closest('.notifications-wrapper')) {
      showNotifications.value = false
    }
  })
})

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

  // Fetch notifications initiales et polling
  const loadNotifications = () => {
    if (authStore.userRole) {
      notificationStore.fetchUnread(authStore.userRole)
    }
  }
  loadNotifications()
  setInterval(loadNotifications, 60000)
})
</script>

<template>
  <div class="dashboard-layout">
    <!-- Overlay Sidebar Mobile -->
    <Transition name="fade">
      <div v-if="isSidebarOpen" class="sidebar-backdrop" @click="isSidebarOpen = false"></div>
    </Transition>

    <!-- Sidebar Réutilisable -->
    <aside class="sidebar" :class="{ 'sidebar-open': isSidebarOpen }">
      <div class="sidebar-header">
        <div class="app-logo-vector">
          <svg viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">
            <path d="M50 5 C25 5 5 25 5 50 C5 75 25 95 50 95 C75 95 95 75 95 50 C95 25 75 5 50 5 Z" fill="none" stroke="currentColor" stroke-width="2" opacity="0.1" />
            <path d="M30 40 C30 25 70 25 70 40 C70 50 30 50 30 60 C30 75 70 75 70 60" fill="none" stroke="currentColor" stroke-width="10" stroke-linecap="round" />
            <path d="M40 40 C40 35 60 35 60 40 C60 45 40 45 40 50 C40 55 60 55 60 50" fill="none" stroke="currentColor" stroke-width="4" stroke-linecap="round" opacity="0.3" />
          </svg>
        </div>
        <h2>SODICA</h2>
      </div>

      <nav class="nav-menu">
        <router-link v-if="authStore.userRole === 'ADMINISTRATEUR'" to="/admin" class="nav-item" :class="{ active: isActive('/admin') }">
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7"></rect><rect x="14" y="3" width="7" height="7"></rect><rect x="14" y="14" width="7" height="7"></rect><rect x="3" y="14" width="7" height="7"></rect></svg>
          Dashboard
        </router-link>

        <router-link v-if="authStore.userRole !== 'ADMINISTRATEUR'" to="/" class="nav-item" :class="{ active: isActive('/') }">
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7"></rect><rect x="14" y="3" width="7" height="7"></rect><rect x="14" y="14" width="7" height="7"></rect><rect x="3" y="14" width="7" height="7"></rect></svg>
          Tableau de bord
        </router-link>
        
        <router-link to="/decaissements" class="nav-item" :class="{ active: isActive('/decaissements') }">
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2v20M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"></path></svg>
          Décaissements
        </router-link>

        <router-link v-if="['COMPTABLE', 'CAISSIER', 'ADMINISTRATEUR'].includes(authStore.userRole)" to="/encaissements" class="nav-item" :class="{ active: isActive('/encaissements') }">
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12V7H5a2 2 0 0 1 0-4h14v4"></path><path d="M3 5v14a2 2 0 0 0 2 2h16v-5"></path><path d="M18 12a2 2 0 0 0 0 4h4v-4Z"></path></svg>
          Encaissements
        </router-link>

        <router-link v-if="['COMPTABLE', 'ADMINISTRATEUR'].includes(authStore.userRole)" to="/factures" class="nav-item" :class="{ active: isActive('/factures') }">
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16c0 1.1.9 2 2 2h12a2 2 0 0 0 2-2V8l-6-6z"/><path d="M14 3v5h5M16 13H8M16 17H8M10 9H8"/></svg>
          Factures
        </router-link>
        
        <router-link v-if="['COMPTABLE', 'RESPONSABLE_FINANCIER', 'PDG', 'ADMINISTRATEUR'].includes(authStore.userRole)" to="/tiers" class="nav-item" :class="{ active: isActive('/tiers') }">
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>
          Annuaire Tiers
        </router-link>

        <div class="nav-section-title">SYSTÈME</div>
        
        <router-link v-if="['ADMINISTRATEUR', 'RESPONSABLE_FINANCIER', 'PDG', 'COMPTABLE'].includes(authStore.userRole)" to="/audit" class="nav-item" :class="{ active: isActive('/audit') }">
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg>
          Audit Système
        </router-link>

        <router-link v-if="['ADMINISTRATEUR', 'RESPONSABLE_FINANCIER', 'PDG'].includes(authStore.userRole)" to="/parametres" class="nav-item" :class="{ active: isActive('/parametres') }">
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"></circle><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path></svg>
          Paramètres
        </router-link>

        <!-- Profil déplacé juste après Paramètres -->
        <div class="sidebar-user-block">
          <router-link to="/profile" class="user-info-link">
            <div class="user-info">
              <div class="user-avatar" :style="{ backgroundImage: authStore.user?.photoUrl ? 'url(' + authStore.user.photoUrl + ')' : 'url(https://ui-avatars.com/api/?name=' + (authStore.user?.prenom || 'A') + '&background=e0e7ff&color=1d4ed8)' }"></div>
              <div class="user-details">
                <span class="user-name">{{ authStore.user?.prenom || 'Alex' }} {{ authStore.user?.nom || 'Henderson' }}</span>
                <span class="user-role">{{ authStore.userRole?.toLowerCase() || 'Utilisateur' }}</span>
              </div>
            </div>
          </router-link>
          <button @click="handleLogout" class="sidebar-logout" title="Déconnexion">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path><polyline points="16 17 21 12 16 7"></polyline><line x1="21" y1="12" x2="9" y2="12"></line></svg>
          </button>
        </div>
      </nav>
    </aside>

    <!-- Main Content Area -->
    <main class="main-content">
      <header class="topbar">
        <div class="topbar-left">
          <button class="hamburger-btn show-mobile" @click="toggleSidebar">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="3" y1="12" x2="21" y2="12"></line><line x1="3" y1="6" x2="21" y2="6"></line><line x1="3" y1="18" x2="21" y2="18"></line></svg>
          </button>
          <div class="topbar-titles">
            <h1 class="page-title"><slot name="title"><span class="hide-on-mobile">Bonjour, {{ authStore.user?.prenom || 'Alex' }}</span></slot></h1>
            <p class="page-subtitle hide-on-mobile"><slot name="subtitle"><span class="capitalize">{{ currentDate }}</span> | {{ currentTime }}</slot></p>
          </div>
        </div>
        
        <div class="topbar-right">
          <slot name="actions"></slot>
          
          <div class="search-box hide-on-mobile" v-if="authStore.userRole === 'ADMINISTRATEUR'" ref="searchRef">
            <svg class="search-icon" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
            <input 
              type="text" 
              placeholder="Rechercher..." 
              @input="handleSearch"
              v-model="searchInput"
              @focus="searchStore.showResults = searchStore.results.length > 0"
            />
            <button v-if="searchInput" class="search-clear-btn" @click="searchInput = ''; searchStore.clearSearch()">
              &times;
            </button>
            
            <!-- Dropdown Résultats -->
            <div v-if="searchStore.showResults" class="search-results-dropdown fade-in">
              <div v-if="searchStore.loading" class="search-loading">
                <div class="spinner-small"></div> Chargement...
              </div>
              <div v-else-if="searchStore.results.length === 0" class="search-empty">
                Aucun résultat pour "{{ searchStore.query }}"
              </div>
              <template v-else>
                <div 
                  v-for="res in searchStore.results" 
                  :key="res.id + res.type" 
                  class="search-result-item" 
                  @click="goToResult(res)"
                >
                  <div class="result-icon" :class="res.type.toLowerCase()">
                    <span v-if="res.type === 'USER'">👤</span>
                    <span v-else-if="res.type === 'TIER'">🏢</span>
                    <span v-else-if="res.type === 'INVOICE'">📄</span>
                    <span v-else>📑</span>
                  </div>
                  <div class="result-body">
                    <div class="result-title">{{ res.title }}</div>
                    <div class="result-subtitle">{{ res.subtitle }}</div>
                  </div>
                </div>
              </template>
            </div>
          </div>

          <div class="topbar-icons">
            <!-- Toggle Dark Mode -->
            <button @click="toggleDarkMode" class="action-btn" :title="isDarkMode ? 'Mode clair' : 'Mode sombre'">
              <svg v-if="!isDarkMode" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"></path></svg>
              <svg v-else xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="5"></circle><line x1="12" y1="1" x2="12" y2="3"></line><line x1="12" y1="21" x2="12" y2="23"></line><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"></line><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"></line><line x1="1" y1="12" x2="3" y2="12"></line><line x1="21" y1="12" x2="23" y2="12"></line><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"></line><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"></line></svg>
            </button>

          <div class="notifications-wrapper">
            <button @click="toggleNotifications" class="action-btn notifications-btn" :class="{ active: showNotifications }" :title="notificationStore.unreadNotifications.length + ' notification(s) non lue(s)'">
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path><path d="M13.73 21a2 2 0 0 1-3.46 0"></path></svg>
              <span v-if="notificationStore.unreadNotifications.length > 0" class="badge">
                {{ notificationStore.unreadNotifications.length > 9 ? '9+' : notificationStore.unreadNotifications.length }}
              </span>
            </button>

            <!-- Dropdown Notifications -->
            <Transition name="slide-up">
              <div v-if="showNotifications" class="notifications-dropdown">
                <div class="notif-header">
                  <h3>Notifications</h3>
                  <button v-if="notificationStore.unreadNotifications.length > 0" @click="markAllAsRead" class="btn-text">Tout marquer comme lu</button>
                </div>
                
                <div class="notif-list custom-scrollbar">
                  <div v-if="notificationStore.unreadNotifications.length === 0" class="notif-empty">
                    <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path><path d="M13.73 21a2 2 0 0 1-3.46 0"></path></svg>
                    <p>Aucune nouvelle notification</p>
                  </div>
                  
                  <div v-for="notif in notificationStore.unreadNotifications" 
                       :key="notif.id" 
                       class="notif-item" 
                       :class="{ 'expanded': expandedNotifId === notif.id }"
                       @click="toggleExpand(notif.id)">
                    <div class="notif-icon" :class="notif.categorie?.toLowerCase() || 'info'">
                      <svg v-if="notif.type === 'SUCCESS'" xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>
                      <svg v-else xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="16" x2="12" y2="12"></line><line x1="12" y1="8" x2="12.01" y2="8"></line></svg>
                    </div>
                    <div class="notif-content">
                      <p class="notif-message" :class="{ 'full-text': expandedNotifId === notif.id }">{{ notif.message }}</p>
                      <div class="notif-meta">
                        <span class="notif-time">{{ new Date(notif.dateCreation).toLocaleString('fr-FR', { hour: '2-digit', minute: '2-digit', day: 'numeric', month: 'short' }) }}</span>
                        <button v-if="expandedNotifId === notif.id" 
                                @click.stop="markAsRead(notif.id)" 
                                class="btn-mark-read">
                          <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>
                          Lu
                        </button>
                      </div>
                    </div>
                    <div class="notif-dot" v-if="expandedNotifId !== notif.id"></div>
                  </div>
                </div>
              </div>
            </Transition>
          </div>

          </div><!-- /topbar-icons -->

          <router-link to="/profile" class="topbar-profile-link">
            <div class="topbar-avatar" :style="{ backgroundImage: authStore.user?.photoUrl ? 'url(' + authStore.user.photoUrl + ')' : 'url(https://ui-avatars.com/api/?name=' + (authStore.user?.prenom || 'A') + '&background=e0e7ff&color=1d4ed8)' }"></div>
          </router-link>
        </div>
      </header>

      <div class="page-content">
        <slot></slot>
      </div>
    </main>

    <!-- Overlay de chargement Global -->
    <Transition name="fade">
      <div v-if="uiStore.isLoading" class="page-loader-overlay">
        <div class="loader-content">
          <div class="sodica-spinner">
            <svg viewBox="0 0 100 100" class="clockwise-svg">
               <circle cx="50" cy="50" r="45" fill="none" stroke="currentColor" stroke-width="2" opacity="0.1" />
               <circle cx="50" cy="50" r="45" fill="none" stroke="currentColor" stroke-width="6" stroke-linecap="round" stroke-dasharray="283" class="spinner-circle" />
            </svg>
          </div>
          <span>Chargement...</span>
        </div>
      </div>
    </Transition>

    <!-- Modale de confirmation de déconnexion -->
    <Transition name="fade">
      <div v-if="showLogoutModal" class="logout-modal-backdrop" @click.self="cancelLogout">
        <div class="logout-modal">
          <div class="logout-modal-icon">
            <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path><polyline points="16 17 21 12 16 7"></polyline><line x1="21" y1="12" x2="9" y2="12"></line></svg>
          </div>
          <h3 class="logout-modal-title">Déconnexion</h3>
          <p class="logout-modal-text">Êtes-vous sûr de vouloir vous déconnecter ?</p>
          <div class="logout-modal-actions">
            <button @click="cancelLogout" class="btn-cancel">Annuler</button>
            <button @click="confirmLogout" class="btn-confirm-logout">Se déconnecter</button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.dashboard-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
  background-color: #f9fafb;
  color: var(--c-text);
  font-family: var(--font-family);
}

/* ====== SIDEBAR ====== */
.sidebar {
  width: 250px;
  background-color: var(--c-surface);
  border-right: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  z-index: 100;
  transition: transform 0.3s ease;
}

@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    top: 0;
    left: 0;
    height: 100vh;
    transform: translateX(-100%);
    box-shadow: 4px 0 15px rgba(0,0,0,0.05);
  }
  .sidebar.sidebar-open {
    transform: translateX(0);
  }
}

.sidebar-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(17, 24, 39, 0.5);
  z-index: 90;
  backdrop-filter: blur(2px);
}

.sidebar-header {
  padding: 1.5rem 1.5rem 2rem 1.5rem;
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.app-logo-vector {
  width: 42px;
  height: 42px;
  color: var(--c-primary);
  display: flex;
  align-items: center;
  justify-content: center;
}
.app-logo-vector svg {
  width: 100%;
  height: 100%;
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
  color: #4b5563;
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
  background-color: #eff6ff;
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

/* Sidebar User Block (remonté) */
.sidebar-user-block {
  margin-top: 2rem;
  padding: 1rem 0;
  border-top: 1px solid #f3f4f6;
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
  height: 100vh;
  overflow: hidden;
}

.topbar {
  padding: 2rem 2.5rem 1.5rem;
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  flex-shrink: 0;
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.topbar-titles {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.hamburger-btn {
  background: none;
  border: none;
  color: #111827;
  cursor: pointer;
  padding: 0.5rem;
  display: none; /* hidden by default, shown by .show-mobile utility */
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

.topbar-left {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.topbar-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background-size: cover;
  background-position: center;
  border: 2px solid white;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: transform 0.2s;
}
.topbar-avatar:hover {
  transform: scale(1.05);
}

.user-info-link {
  text-decoration: none;
  flex: 1;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex: 1;
  justify-content: flex-end;
}

.topbar-icons {
  display: flex;
  align-items: center;
  gap: 6px;
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
  top: -5px; 
  right: -5px;
  background-color: #ef4444; 
  color: white;
  min-width: 18px; 
  height: 18px;
  padding: 0 4px;
  border-radius: 10px;
  border: 2px solid white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: 700;
  line-height: 1;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

/* ====== NOTIFICATIONS DROPDOWN ====== */
.notifications-wrapper {
  position: relative;
}

.notifications-dropdown {
  position: absolute;
  top: calc(100% + 12px);
  right: 0;
  width: 320px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1);
  border: 1px solid #f3f4f6;
  z-index: 1000;
  overflow: hidden;
  animation: dropdownSlide 0.2s ease-out;
}

@keyframes dropdownSlide {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}

.notif-header {
  padding: 1rem 1.25rem;
  border-bottom: 1px solid #f3f4f6;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.notif-header h3 {
  font-size: 0.95rem;
  font-weight: 700;
  color: #111827;
}

.btn-text {
  background: none; border: none;
  font-size: 0.75rem;
  color: var(--c-primary);
  font-weight: 600;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
}
.btn-text:hover { background: #eff6ff; }

.notif-list {
  max-height: 400px;
  overflow-y: auto;
}

.notif-empty {
  padding: 3rem 2rem;
  text-align: center;
  color: #9ca3af;
}
.notif-empty svg { margin-bottom: 0.75rem; opacity: 0.5; }
.notif-empty p { font-size: 0.85rem; }

.notif-item {
  padding: 1rem 1.25rem;
  display: flex;
  gap: 1rem;
  cursor: pointer;
  transition: background 0.2s;
  position: relative;
  border-bottom: 1px solid #f9fafb;
}
.notif-item:hover { background: #f9fafb; }
.notif-item:last-child { border-bottom: none; }

.notif-icon {
  width: 32px; height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.notif-icon.info { background: #eff6ff; color: #3b82f6; }
.notif-icon.success { background: #ecfdf5; color: #10b981; }
.notif-icon.warning { background: #fffbeb; color: #f59e0b; }

.notif-content { flex: 1; min-width: 0; }

.notif-message {
  font-size: 0.85rem;
  color: #374151;
  line-height: 1.4;
  margin-bottom: 0.25rem;
  display: -webkit-box;
  line-clamp: 2;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  transition: all 0.3s ease;
}

.notif-message.full-text {
  display: block;
  line-clamp: none;
  -webkit-line-clamp: none;
  overflow: visible;
}

.notif-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 0.5rem;
}

.notif-time {
  font-size: 0.7rem;
  color: #9ca3af;
}

.btn-mark-read {
  background: #ecfdf5;
  color: #10b981;
  border: 1px solid #10b981;
  border-radius: 4px;
  padding: 2px 8px;
  font-size: 0.7rem;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  transition: all 0.2s;
}

.btn-mark-read:hover {
  background: #10b981;
  color: white;
}

.notif-item.expanded {
  background: #fdfdfd;
  box-shadow: inset 0 0 10px rgba(0,0,0,0.02);
  cursor: default;
}

.notif-dot {
  width: 8px; height: 8px;
  background: var(--c-primary);
  border-radius: 50%;
  margin-top: 4px;
}

.page-content {
  padding: 0 2.5rem 3rem;
  display: flex;
  flex-direction: column;
  gap: 2rem;
  flex: 1;
  overflow-y: auto;
}

/* ====== GLOBAL LOADER ====== */
.page-loader-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.loader-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1.5rem;
}

.sodica-spinner {
  width: 64px;
  height: 64px;
  color: var(--c-primary);
}

.clockwise-svg {
  transform: rotate(-90deg);
}

.spinner-circle {
  animation: progress-clockwise 1.5s ease-in-out infinite;
  transform-origin: center;
}

@keyframes progress-clockwise {
  0% { stroke-dashoffset: 283; }
  50% { stroke-dashoffset: 70; }
  100% { stroke-dashoffset: 283; transform: rotate(360deg); }
}

.loader-content span {
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--c-primary);
  letter-spacing: 0.05em;
  text-transform: uppercase;
}

/* Transition Vue (fade) */
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s ease, filter 0.3s ease;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
/* === Modale de confirmation de déconnexion === */
.logout-modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.5);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.logout-modal {
  background: white;
  border-radius: 20px;
  padding: 2.5rem 2rem 2rem;
  width: 380px;
  max-width: 90vw;
  text-align: center;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  animation: modalSlideUp 0.25s ease-out;
}

@keyframes modalSlideUp {
  from { opacity: 0; transform: translateY(20px) scale(0.95); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

.logout-modal-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: linear-gradient(135deg, #fef2f2, #fee2e2);
  color: #dc2626;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 1.25rem;
}

.logout-modal-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: #111827;
  margin: 0 0 0.5rem;
}

.logout-modal-text {
  font-size: 0.9rem;
  color: #6b7280;
  margin: 0 0 1.75rem;
  line-height: 1.5;
}

.logout-modal-actions {
  display: flex;
  gap: 0.75rem;
}

.btn-cancel {
  flex: 1;
  padding: 0.75rem;
  border-radius: 12px;
  border: 1.5px solid #e5e7eb;
  background: white;
  color: #374151;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-cancel:hover {
  background: #f9fafb;
  border-color: #d1d5db;
}

.btn-confirm-logout {
  flex: 1;
  padding: 0.75rem;
  border-radius: 12px;
  border: none;
  background: linear-gradient(135deg, #dc2626, #b91c1c);
  color: white;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 4px 6px -1px rgba(220, 38, 38, 0.25);
}

.btn-confirm-logout:hover {
  background: linear-gradient(135deg, #b91c1c, #991b1b);
  transform: translateY(-1px);
  box-shadow: 0 10px 15px -3px rgba(220, 38, 38, 0.3);
}

.btn-confirm-logout:active {
  transform: translateY(0);
}

@media (max-width: 768px) {
  .topbar {
    padding-left: 0.25rem;
    padding-right: 0.25rem;
  }
  .topbar-left {
    gap: 0.25rem;
  }
  .topbar-right {
    gap: 0.5rem;
  }
  .topbar-icons {
    gap: 2px;
  }
  .topbar-avatar {
    width: 36px;
    height: 36px;
  }
  .action-btn {
    padding: 6px;
  }
  .notifications-dropdown {
    right: -1.25rem;
    width: 300px;
  }
}

/* === GLOBAL SEARCH DROPDOWN === */
.search-box { position: relative; }
.search-results-dropdown {
  position: absolute;
  top: calc(100% + 10px);
  left: 0;
  right: 0;
  background: white;
  border-radius: 12px;
  box-shadow: 0 10px 25px -5px rgba(0,0,0,0.1), 0 8px 10px -6px rgba(0,0,0,0.1);
  border: 1px solid #e2e8f0;
  z-index: 1000;
  max-height: 400px;
  overflow-y: auto;
  min-width: 300px;
}
.dark-mode .search-results-dropdown {
  background: #1e293b;
  border-color: #334155;
  box-shadow: 0 10px 25px -5px rgba(0,0,0,0.5);
}
.search-loading, .search-empty {
  padding: 1rem;
  text-align: center;
  color: #64748b;
  font-size: 0.875rem;
}
.search-result-item {
  display: flex;
  align-items: center;
  gap: 0.85rem;
  padding: 0.75rem 1rem;
  cursor: pointer;
  transition: all 0.15s;
  border-bottom: 1px solid #f1f5f9;
}
.dark-mode .search-result-item { border-bottom-color: #334155; }
.search-result-item:last-child { border-bottom: none; }
.search-result-item:hover { background: #f8fafc; }
.dark-mode .search-result-item:hover { background: #334155; }

.result-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1rem;
  flex-shrink: 0;
}
.result-icon.user { background: #eff6ff; }
.result-icon.tier { background: #f0fdf4; }
.result-icon.invoice { background: #fff7ed; }
.result-icon.audit { background: #f1f5f9; }

.result-body { flex: 1; min-width: 0; }
.result-title { font-weight: 600; color: #1e293b; font-size: 0.875rem; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.dark-mode .result-title { color: #f1f5f9; }
.result-subtitle { font-size: 0.75rem; color: #64748b; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.search-clear-btn {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  color: #94a3b8;
  font-size: 1.25rem;
  cursor: pointer;
  padding: 0 4px;
  line-height: 1;
}
.search-clear-btn:hover { color: #64748b; }

.spinner-small {
  width: 14px;
  height: 14px;
  border: 2px solid #e2e8f0;
  border-top-color: #3b82f6;
  border-radius: 50%;
  display: inline-block;
  animation: rotate 0.8s linear infinite;
  vertical-align: middle;
  margin-right: 5px;
}
@keyframes rotate { to { transform: rotate(360deg); } }
</style>
