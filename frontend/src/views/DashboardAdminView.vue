<script setup>
import { ref, onMounted, computed } from 'vue'
import MainLayout from '../components/MainLayout.vue'
import { useUtilisateurStore } from '../stores/utilisateur.store'
import { useAuthStore } from '../stores/auth.store'
import { useAuditStore } from '../stores/audit.store'

const authStore = useAuthStore()
const utilisateurStore = useUtilisateurStore()
const auditStore = useAuditStore()

onMounted(async () => {
  await utilisateurStore.fetchUtilisateurs()
  await auditStore.fetchLogs(0, 200, '', '')
})

// === GESTION UTILISATEURS ===
const searchUser = ref('')
const usersFiltered = computed(() => {
  if (!searchUser.value) return utilisateurStore.utilisateurs
  const s = searchUser.value.toLowerCase()
  return utilisateurStore.utilisateurs.filter(u => 
    u.nom?.toLowerCase().includes(s) || 
    u.prenom?.toLowerCase().includes(s) || 
    u.email?.toLowerCase().includes(s)
  )
})

// === PAGINATION ===
const pageSize = 8
const currentPage = ref(1)
const totalPages = computed(() => Math.ceil(usersFiltered.value.length / pageSize))
const paginatedUsers = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return usersFiltered.value.slice(start, start + pageSize)
})

const getAvatarColor = (name) => {
  if(!name) return '#cbd5e1'
  const colors = ['#0f766e', '#3b82f6', '#f59e0b', '#8b5cf6', '#ec4899']
  let hash = 0;
  for (let i = 0; i < name.length; i++) hash = name.charCodeAt(i) + ((hash << 5) - hash)
  return colors[Math.abs(hash) % colors.length]
}

const showUserModal = ref(false)
const editUserId = ref(null)
const userForm = ref({ nom: '', prenom: '', email: '', role: 'CAISSIER', password: '123456' })

const openCreateUser = () => {
  editUserId.value = null
  userForm.value = { nom: '', prenom: '', email: '', role: 'CAISSIER', password: '123456' }
  showUserModal.value = true
}

const openEditUser = (u) => {
  editUserId.value = u.id
  userForm.value = { nom: u.nom, prenom: u.prenom, email: u.email, role: u.role, password: '' }
  showUserModal.value = true
}

const submitUser = async () => {
  try {
    if (editUserId.value) {
      await utilisateurStore.mettreAJour(editUserId.value, userForm.value)
    } else {
      await utilisateurStore.creerUtilisateur(userForm.value)
    }
    showUserModal.value = false
  } catch (e) {
    console.error("Erreur utilisateur", e)
  }
}

const bloquerInfo = async (id) => {
  if(confirm("Confirmez-vous la désactivation de ce compte ?")) {
    try {
      await utilisateurStore.bloquerOuDebloquer(id)
    } catch (e) {
      alert(e.response?.data?.message || "Erreur lors de la désactivation")
    }
  }
}

const reactiverInfo = async (id) => {
  if(confirm("Confirmez-vous la réactivation de ce compte ?")) {
    try {
      await utilisateurStore.reactiverUtilisateur(id)
    } catch (e) {
      alert(e.response?.data?.message || "Erreur lors de la réactivation")
    }
  }
}

// === KPIs ===
const totalUsers = computed(() => utilisateurStore.utilisateurs.length)
const activeUsers = computed(() => utilisateurStore.utilisateurs.filter(u => u.actif !== false).length)
const blockedUsers = computed(() => utilisateurStore.utilisateurs.filter(u => u.actif === false).length)

// === GRAPHIQUE RÉPARTITION DES RÔLES ===
const roleColors = {
  CAISSIER: '#3b82f6',
  COMPTABLE: '#8b5cf6',
  RESPONSABLE_FINANCIER: '#f59e0b',
  PDG: '#ef4444',
  ADMINISTRATEUR: '#10b981'
}

const roleDistribution = computed(() => {
  const roles = {}
  utilisateurStore.utilisateurs.forEach(u => {
    const r = u.role || 'INCONNU'
    roles[r] = (roles[r] || 0) + 1
  })
  return Object.entries(roles).map(([role, count]) => ({
    role,
    count,
    percent: totalUsers.value ? Math.round((count / totalUsers.value) * 100) : 0,
    color: roleColors[role] || '#94a3b8'
  }))
})

const donutSegments = computed(() => {
  let offset = 0
  return roleDistribution.value.map(r => {
    const seg = { ...r, offset }
    offset += r.percent
    return seg
  })
})

// === GRAPHIQUE ACTIVITÉ 7 DERNIERS JOURS ===
const activityData = computed(() => {
  const days = []
  for (let i = 6; i >= 0; i--) {
    const d = new Date()
    d.setDate(d.getDate() - i)
    d.setHours(0, 0, 0, 0)
    const dayStr = d.toLocaleDateString('fr-FR', { weekday: 'short' })
    const count = (auditStore.logs || []).filter(l => {
      const ld = new Date(l.dateAction)
      return ld.toDateString() === d.toDateString()
    }).length
    days.push({ label: dayStr, count })
  }
  return days
})

const maxActivity = computed(() => Math.max(...activityData.value.map(d => d.count), 1))

// === FORMAT DATE ===
const formatLastLogin = (dateStr) => {
  if (!dateStr) return 'Jamais'
  const d = new Date(dateStr)
  const now = new Date()
  const diff = Math.floor((now - d) / 1000)
  if (diff < 60) return 'À l\'instant'
  if (diff < 3600) return `Il y a ${Math.floor(diff / 60)} min`
  if (diff < 86400) return `Il y a ${Math.floor(diff / 3600)}h`
  return d.toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' })
}
</script>

<template>
  <MainLayout>
    <template #title>Dashboard</template>
    <template #subtitle>Vue d'ensemble de l'administration des utilisateurs et accès.</template>

    <div class="admin-dashboard">

      <!-- KPI Cards -->
      <div class="kpi-row">
        <div class="kpi-card">
          <div class="kpi-top">
            <div>
              <div class="kpi-label">Total Utilisateurs</div>
              <div class="kpi-value">{{ totalUsers }}</div>
            </div>
            <div class="kpi-icon kpi-icon-blue">
              <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="8.5" cy="7" r="4"></circle><line x1="20" y1="8" x2="20" y2="14"></line><line x1="23" y1="11" x2="17" y2="11"></line></svg>
            </div>
          </div>
          <div class="kpi-sub kpi-sub-green">Enregistrés dans le système</div>
        </div>

        <div class="kpi-card">
          <div class="kpi-top">
            <div>
              <div class="kpi-label">Comptes Actifs</div>
              <div class="kpi-value">{{ activeUsers }}</div>
            </div>
            <div class="kpi-icon kpi-icon-green">
              <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>
            </div>
          </div>
          <div class="kpi-sub kpi-sub-neutral">{{ totalUsers ? ((activeUsers / totalUsers) * 100).toFixed(0) : 0 }}% du total</div>
        </div>

        <div class="kpi-card">
          <div class="kpi-top">
            <div>
              <div class="kpi-label">Comptes Bloqués</div>
              <div class="kpi-value">{{ blockedUsers }}</div>
            </div>
            <div class="kpi-icon kpi-icon-red">
              <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><line x1="4.93" y1="4.93" x2="19.07" y2="19.07"></line></svg>
            </div>
          </div>
          <div class="kpi-sub kpi-sub-red" v-if="blockedUsers > 0">⚠ Sécurité à vérifier</div>
          <div class="kpi-sub kpi-sub-green" v-else>✓ Aucun compte bloqué</div>
        </div>
      </div>

      <!-- GRAPHIQUES ROW -->
      <div class="charts-row">
        <!-- Donut Chart: Répartition des rôles -->
        <div class="chart-card">
          <div class="chart-title">Répartition des rôles</div>
          <div class="donut-container">
            <svg viewBox="0 0 36 36" class="donut-chart">
              <circle cx="18" cy="18" r="15.9" fill="none" stroke="#f1f5f9" stroke-width="3" />
              <circle 
                v-for="seg in donutSegments" 
                :key="seg.role"
                cx="18" cy="18" r="15.9" 
                fill="none" 
                :stroke="seg.color" 
                stroke-width="3"
                :stroke-dasharray="`${seg.percent} ${100 - seg.percent}`"
                :stroke-dashoffset="25 - seg.offset"
                stroke-linecap="round"
              />
            </svg>
            <div class="donut-legend">
              <div v-for="seg in donutSegments" :key="seg.role" class="legend-item">
                <span class="legend-dot" :style="{ background: seg.color }"></span>
                <span class="legend-label">{{ seg.role.replace('_', ' ') }}</span>
                <span class="legend-count">{{ seg.count }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Bar Chart: Activité 7 Derniers Jours -->
        <div class="chart-card">
          <div class="chart-title">Activité des 7 derniers jours</div>
          <div class="bar-chart">
            <div v-for="day in activityData" :key="day.label" class="bar-col">
              <div class="bar-value">{{ day.count }}</div>
              <div class="bar-track">
                <div class="bar-fill" :style="{ height: (day.count / maxActivity * 100) + '%' }"></div>
              </div>
              <div class="bar-label">{{ day.label }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Action Bar -->
      <div class="action-bar mb-4">
        <div class="input-with-icon">
           <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="2"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
           <input v-model="searchUser" type="text" placeholder="Rechercher utilisateur..." />
        </div>
        <button @click="openCreateUser" class="btn-primary" style="background-color: #3b82f6; color: white; border-radius: 8px; padding: 0.75rem 1.25rem; border: none; font-weight: 600; display: flex; align-items: center; gap: 8px; box-shadow: 0 4px 6px -1px rgba(59, 130, 246, 0.4); cursor: pointer;">
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
          Nouvel Utilisateur
        </button>
      </div>

      <div v-if="utilisateurStore.error" class="error-banner mb-4">{{ utilisateurStore.error }}</div>

      <!-- Table Utilisateurs -->
      <div class="feature-card">
        <div class="table-scroll">
          <table class="data-table">
            <thead>
              <tr>
                <th>ACTEUR</th>
                <th>ID CONNEXION (EMAIL)</th>
                <th>RÔLE SYSTÈME</th>
                <th>DERNIÈRE CONNEXION</th>
                <th>STATUT</th>
                <th class="text-center">ACTIONS</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="u in paginatedUsers" :key="u.id">
                <td>
                  <div class="user-cell">
                    <div class="user-avatar" :style="{ backgroundColor: getAvatarColor(u.prenom), color: 'white' }">
                      {{ u.prenom ? u.prenom.charAt(0) + (u.nom ? u.nom.charAt(0) : '') : '?' }}
                    </div>
                    <div class="user-info">
                      <strong>{{ u.prenom }} {{ u.nom }}</strong>
                    </div>
                  </div>
                </td>
                <td><span class="text-mono">{{ u.email }}</span></td>
                <td>
                  <span class="role-badge">{{ u.role?.replace('_', ' ') }}</span>
                </td>
                <td>
                  <span class="last-login" :class="{ 'never': !u.dernierAcces }">
                    {{ formatLastLogin(u.dernierAcces) }}
                  </span>
                </td>
                <td>
                  <span class="status-badge" :class="u.actif !== false ? 'active' : 'inactive'">
                    <template v-if="u.actif !== false">
                      <svg xmlns="http://www.w3.org/2000/svg" width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg> Actif
                    </template>
                    <template v-else>
                      <svg xmlns="http://www.w3.org/2000/svg" width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg> Inactif
                    </template>
                  </span>
                </td>
                <td class="cell-actions text-center">
                  <button @click="openEditUser(u)" class="btn-icon" title="Modifier" :disabled="u.email === authStore.userName" style="color:#3b82f6;">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path></svg>
                  </button>
                  <button v-if="u.actif !== false" @click="bloquerInfo(u.id)" class="btn-icon danger" title="Désactiver" :disabled="u.email === authStore.userName">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="15" y1="9" x2="9" y2="15"></line><line x1="9" y1="9" x2="15" y2="15"></line></svg>
                  </button>
                  <button v-else @click="reactiverInfo(u.id)" class="btn-icon" title="Réactiver le compte" style="color:#16a34a;">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="23 4 23 10 17 10"></polyline><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"></path></svg>
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Pagination -->
        <div v-if="totalPages > 1" class="table-pagination">
          <span class="pag-info">Affichage {{ (currentPage - 1) * pageSize + 1 }}–{{ Math.min(currentPage * pageSize, usersFiltered.length) }} sur {{ usersFiltered.length }}</span>
          <div class="pag-buttons">
            <button @click="currentPage = Math.max(1, currentPage - 1)" :disabled="currentPage === 1" class="pag-btn">‹</button>
            <button 
              v-for="p in totalPages" :key="p" 
              @click="currentPage = p" 
              :class="['pag-btn', { active: p === currentPage }]"
            >{{ p }}</button>
            <button @click="currentPage = Math.min(totalPages, currentPage + 1)" :disabled="currentPage === totalPages" class="pag-btn">›</button>
          </div>
        </div>
      </div>
    </div>

    <!-- Modale Utilisateur -->
    <div v-if="showUserModal" class="user-modal-overlay" @click.self="showUserModal = false">
      <div class="user-modal-box">
        <div class="user-modal-head">
          <h3>{{ editUserId ? 'Modifier Utilisateur' : 'Nouvel Utilisateur' }}</h3>
          <button @click="showUserModal = false" class="user-modal-close">
            <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
          </button>
        </div>
        
        <div class="user-modal-content">
          <form id="create-user-form" @submit.prevent="submitUser" class="user-modal-form">
            <div class="um-form-group">
              <label class="um-label">Rôle Système <span style="color:#ef4444;">*</span></label>
              <div class="um-role-grid">
                <label v-for="r in ['CAISSIER', 'COMPTABLE', 'RESPONSABLE_FINANCIER', 'PDG', 'ADMINISTRATEUR']" :key="r" class="um-role-card" :class="{ active: userForm.role === r }">
                  <input type="radio" v-model="userForm.role" :value="r" style="display:none;" />
                  <span class="um-role-icon">
                    <svg v-if="r === 'CAISSIER'" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12V7H5a2 2 0 0 1 0-4h14v4"></path><path d="M3 5v14a2 2 0 0 0 2 2h16v-5"></path><path d="M18 12a2 2 0 0 0 0 4h4v-4Z"></path></svg>
                    <svg v-else-if="r === 'COMPTABLE'" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line></svg>
                    <svg v-else-if="r === 'RESPONSABLE_FINANCIER'" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="7" width="20" height="14" rx="2" ry="2"></rect><path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"></path></svg>
                    <svg v-else-if="r === 'PDG'" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="8" r="5"></circle><path d="M3 21v-2a7 7 0 0 1 14 0v2"></path></svg>
                    <svg v-else xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path></svg>
                  </span>
                  <span class="um-role-name">{{ r.replace('_', ' ') }}</span>
                </label>
              </div>
            </div>

            <div class="um-row">
              <div class="um-form-group um-half">
                <label class="um-label">Prénom <span style="color:#ef4444;">*</span></label>
                <input v-model="userForm.prenom" type="text" required class="um-input" placeholder="Saisissez le prénom..." />
              </div>
              <div class="um-form-group um-half">
                <label class="um-label">Nom de famille <span style="color:#ef4444;">*</span></label>
                <input v-model="userForm.nom" type="text" required class="um-input" placeholder="Saisissez le nom..." />
              </div>
            </div>
            
            <div class="um-form-group">
              <label class="um-label">Adresse E-mail <span style="color:#ef4444;">*</span></label>
              <input v-model="userForm.email" type="email" required class="um-input" placeholder="ex: j.dupont@sodica.com" />
            </div>

            <div class="um-form-group">
              <label class="um-label">Mot de passe {{ editUserId ? '(laisser vide pour ne pas changer)' : 'par défaut' }} <span v-if="!editUserId" style="color:#ef4444;">*</span></label>
              <input v-model="userForm.password" type="text" :required="!editUserId" class="um-input" :placeholder="editUserId ? 'Laisser vide pour conserver l\'ancien' : 'Définir un mot de passe...'" />
            </div>
          </form>

          <div class="user-modal-sidebar">
            <div class="um-sidebar-title">
              <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path></svg>
              SÉCURITÉ &amp; ACCÈS
            </div>
            
            <div class="um-alert um-alert-blue" v-if="userForm.role">
              <strong>Permissions {{ userForm.role.replace('_', ' ') }}</strong>
              <span v-if="userForm.role === 'CAISSIER'">Accès aux encaissements et paiements physiques.</span>
              <span v-else-if="userForm.role === 'COMPTABLE'">Saisie factures et demandes de décaissements.</span>
              <span v-else-if="userForm.role === 'RESPONSABLE_FINANCIER'">Approbation des dépenses &lt; 500k XAF.</span>
              <span v-else-if="userForm.role === 'PDG'">Approbation globale de trésorerie.</span>
              <span v-else>Accès total aux paramétrages du système.</span>
            </div>

            <div class="um-alert um-alert-orange">
              <strong>⚠ Action Requise</strong>
              <span>L'utilisateur devra modifier son mot de passe lors de sa première connexion.</span>
            </div>

            <div class="um-alert um-alert-green" v-if="userForm.prenom && userForm.nom && userForm.email">
              <strong>✓ Profil complet</strong>
              <span>Prêt pour l'intégration.</span>
            </div>
          </div>
        </div>
        
        <div class="user-modal-foot">
          <button type="button" @click="showUserModal = false" class="um-btn-cancel">Annuler</button>
          <button type="button" @click="submitUser" class="um-btn-submit" :disabled="!userForm.nom || !userForm.prenom || !userForm.email">
            Confirmer et Enregistrer
          </button>
        </div>
      </div>
    </div>

  </MainLayout>
</template>

<style scoped>
.admin-dashboard { display: flex; flex-direction: column; gap: 1.25rem; }

/* Action Bar */
.action-bar { display: flex; justify-content: space-between; align-items: center; }
.input-with-icon { position: relative; width: 300px; }
.input-with-icon svg:first-child { position: absolute; left: 12px; top: 50%; transform: translateY(-50%); z-index: 1; }
.input-with-icon input { width: 100%; padding: 0.6rem 1rem 0.6rem 2.2rem; border: 1px solid #e2e8f0; border-radius: 8px; font-size: 0.875rem; outline: none; }
.input-with-icon input:focus { border-color: #3b82f6; box-shadow: 0 0 0 3px rgba(59,130,246,0.1); }

.feature-card { background: white; border-radius: 12px; border: 1px solid #f1f5f9; box-shadow: 0 1px 3px rgba(0,0,0,0.02); overflow: hidden; }

/* Data Table */
.data-table { width: 100%; border-collapse: collapse; }
.data-table th { text-align: left; padding: 1rem 1.25rem; color: #94a3b8; font-size: 0.7rem; text-transform: uppercase; font-weight: 700; letter-spacing: 0.05em; border-bottom: 1px solid #f1f5f9; }
.data-table td { padding: 0.85rem 1.25rem; border-bottom: 1px solid #f1f5f9; vertical-align: middle; }
.user-cell { display: flex; align-items: center; gap: 0.75rem; }
.user-avatar { width: 34px; height: 34px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 0.8rem; flex-shrink: 0; }
.text-mono { font-family: ui-monospace, SFMono-Regular, monospace; font-size: 0.82rem; color: #475569; }
.role-badge { background: #f1f5f9; color: #475569; padding: 4px 10px; border-radius: 20px; font-size: 0.7rem; font-weight: 600; }
.status-badge { display: inline-flex; align-items: center; gap: 4px; font-size: 0.7rem; font-weight: 600; padding: 4px 10px; border-radius: 20px; }
.status-badge.active { background: #dcfce7; color: #166534; }
.status-badge.inactive { background: #fee2e2; color: #991b1b; }
.last-login { font-size: 0.82rem; color: #475569; }
.last-login.never { color: #94a3b8; font-style: italic; }
.btn-icon { background: none; border: none; padding: 6px; border-radius: 6px; cursor: pointer; color: #94a3b8; transition: all 0.2s; }
.btn-icon.danger:hover { background: #fee2e2; color: #ef4444; }

/* KPI CARDS */
.kpi-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 1.25rem; }
.kpi-card { background: white; border-radius: 14px; border: 1px solid #f1f5f9; padding: 1.25rem 1.5rem; box-shadow: 0 1px 3px rgba(0,0,0,0.03); transition: box-shadow 0.2s; }
.kpi-card:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.06); }
.kpi-top { display: flex; justify-content: space-between; align-items: flex-start; }
.kpi-label { font-size: 0.8rem; font-weight: 600; color: #64748b; text-transform: uppercase; letter-spacing: 0.03em; margin-bottom: 4px; }
.kpi-value { font-size: 2rem; font-weight: 800; color: #0f172a; line-height: 1.1; }
.kpi-icon { width: 44px; height: 44px; border-radius: 12px; display: flex; align-items: center; justify-content: center; }
.kpi-icon-blue { background: #eff6ff; color: #3b82f6; }
.kpi-icon-green { background: #f0fdf4; color: #16a34a; }
.kpi-icon-red { background: #fef2f2; color: #ef4444; }
.kpi-sub { font-size: 0.78rem; font-weight: 600; margin-top: 0.5rem; }
.kpi-sub-green { color: #16a34a; }
.kpi-sub-red { color: #ef4444; }
.kpi-sub-neutral { color: #64748b; }

/* CHARTS ROW */
.charts-row { display: grid; grid-template-columns: 1fr 1fr; gap: 1.25rem; }
.chart-card { background: white; border-radius: 14px; border: 1px solid #f1f5f9; padding: 1.25rem 1.5rem; box-shadow: 0 1px 3px rgba(0,0,0,0.03); }
.chart-title { font-size: 0.85rem; font-weight: 700; color: #334155; margin-bottom: 1rem; }

/* Donut Chart */
.donut-container { display: flex; align-items: center; gap: 1.5rem; }
.donut-chart { width: 120px; height: 120px; transform: rotate(-90deg); }
.donut-legend { display: flex; flex-direction: column; gap: 6px; flex: 1; }
.legend-item { display: flex; align-items: center; gap: 8px; font-size: 0.8rem; }
.legend-dot { width: 10px; height: 10px; border-radius: 3px; flex-shrink: 0; }
.legend-label { color: #475569; flex: 1; text-transform: capitalize; }
.legend-count { font-weight: 700; color: #1e293b; }

/* Bar Chart */
.bar-chart { display: flex; align-items: flex-end; gap: 0.75rem; height: 140px; padding-top: 10px; }
.bar-col { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 6px; }
.bar-value { font-size: 0.7rem; font-weight: 700; color: #334155; }
.bar-track { width: 100%; height: 100px; background: #f1f5f9; border-radius: 6px; position: relative; overflow: hidden; display: flex; align-items: flex-end; }
.bar-fill { width: 100%; background: linear-gradient(to top, #3b82f6, #60a5fa); border-radius: 6px; transition: height 0.5s ease; min-height: 4px; }
.bar-label { font-size: 0.7rem; color: #94a3b8; font-weight: 600; text-transform: capitalize; }

/* PAGINATION */
.table-pagination { display: flex; justify-content: space-between; align-items: center; padding: 0.75rem 1.25rem; border-top: 1px solid #f1f5f9; background: #fafbfc; }
.pag-info { font-size: 0.8rem; color: #64748b; }
.pag-buttons { display: flex; gap: 4px; }
.pag-btn { width: 32px; height: 32px; display: flex; align-items: center; justify-content: center; border: 1px solid #e2e8f0; border-radius: 6px; background: white; color: #475569; font-size: 0.85rem; font-weight: 600; cursor: pointer; transition: all 0.15s; }
.pag-btn:hover:not(:disabled) { border-color: #3b82f6; color: #3b82f6; }
.pag-btn.active { background: #3b82f6; color: white; border-color: #3b82f6; }
.pag-btn:disabled { opacity: 0.4; cursor: not-allowed; }

/* MODALE UTILISATEUR */
.user-modal-overlay { position: fixed; inset: 0; z-index: 9999; background: rgba(15, 23, 42, 0.45); backdrop-filter: blur(6px); display: flex; align-items: center; justify-content: center; animation: umFadeIn 0.2s ease; }
@keyframes umFadeIn { from { opacity: 0; } to { opacity: 1; } }
.user-modal-box { background: #fff; border-radius: 16px; width: 92%; max-width: 820px; max-height: 90vh; display: flex; flex-direction: column; box-shadow: 0 25px 50px -12px rgba(0,0,0,0.15); animation: umSlideUp 0.25s ease; overflow: hidden; }
@keyframes umSlideUp { from { transform: translateY(20px); opacity: 0; } to { transform: translateY(0); opacity: 1; } }
.user-modal-head { display: flex; justify-content: space-between; align-items: center; padding: 1.25rem 1.75rem; border-bottom: 1px solid #f1f5f9; }
.user-modal-head h3 { margin: 0; font-size: 1.15rem; font-weight: 700; color: #1e293b; }
.user-modal-close { background: none; border: none; cursor: pointer; color: #94a3b8; display: flex; align-items: center; padding: 4px; border-radius: 6px; transition: all 0.15s; }
.user-modal-close:hover { color: #0f172a; background: #f1f5f9; }
.user-modal-content { display: flex; flex: 1; overflow-y: auto; }
.user-modal-form { flex: 1; padding: 1.5rem 1.75rem; display: flex; flex-direction: column; gap: 1rem; }
.user-modal-sidebar { width: 280px; background: #f8fafc; border-left: 1px solid #f1f5f9; padding: 1.5rem; display: flex; flex-direction: column; gap: 1rem; }
.um-sidebar-title { display: flex; align-items: center; gap: 8px; font-size: 0.7rem; font-weight: 700; color: #64748b; letter-spacing: 0.1em; }
.um-form-group { display: flex; flex-direction: column; gap: 6px; }
.um-label { font-size: 0.85rem; font-weight: 600; color: #334155; }
.um-input { width: 100%; padding: 0.7rem 0.9rem; border: 1.5px solid #e2e8f0; border-radius: 10px; font-size: 0.9rem; color: #1e293b; outline: none; transition: all 0.2s; background: #fff; box-sizing: border-box; }
.um-input:focus { border-color: #3b82f6; box-shadow: 0 0 0 3px rgba(59,130,246,0.1); }
.um-input::placeholder { color: #94a3b8; }
.um-row { display: flex; gap: 1rem; }
.um-half { flex: 1; }
.um-role-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 0.5rem; }
.um-role-card { display: flex; flex-direction: column; align-items: center; gap: 4px; padding: 0.6rem 0.4rem; border: 1.5px solid #e2e8f0; border-radius: 10px; cursor: pointer; transition: all 0.15s; background: #fff; text-align: center; }
.um-role-card:hover { border-color: #93c5fd; background: #eff6ff; }
.um-role-card.active { border-color: #3b82f6; background: #eff6ff; box-shadow: 0 0 0 2px rgba(59,130,246,0.15); }
.um-role-card.active .um-role-icon { color: #2563eb; }
.um-role-icon { color: #94a3b8; display: flex; align-items: center; justify-content: center; }
.um-role-name { font-size: 0.65rem; font-weight: 600; color: #475569; text-transform: capitalize; line-height: 1.1; }
.um-alert { display: flex; flex-direction: column; gap: 4px; padding: 0.75rem; border-radius: 10px; font-size: 0.8rem; line-height: 1.4; }
.um-alert strong { font-size: 0.8rem; }
.um-alert span { font-size: 0.75rem; color: #475569; }
.um-alert-blue { background: #eff6ff; border: 1px solid #bfdbfe; }
.um-alert-blue strong { color: #1d4ed8; }
.um-alert-orange { background: #fffbeb; border: 1px solid #fde68a; }
.um-alert-orange strong { color: #b45309; }
.um-alert-green { background: #f0fdf4; border: 1px solid #bbf7d0; }
.um-alert-green strong { color: #166534; }
.user-modal-foot { display: flex; justify-content: flex-end; align-items: center; gap: 0.75rem; padding: 1rem 1.75rem; border-top: 1px solid #f1f5f9; background: #f8fafc; }
.um-btn-cancel { background: none; border: 1px solid #e2e8f0; padding: 0.6rem 1.25rem; border-radius: 8px; color: #475569; font-weight: 600; font-size: 0.875rem; cursor: pointer; transition: all 0.15s; }
.um-btn-cancel:hover { background: #f1f5f9; }
.um-btn-submit { background: #3b82f6; color: white; border: none; padding: 0.6rem 1.5rem; border-radius: 8px; font-weight: 600; font-size: 0.875rem; cursor: pointer; box-shadow: 0 4px 6px -1px rgba(59,130,246,0.3); transition: all 0.15s; }
.um-btn-submit:hover { background: #2563eb; box-shadow: 0 6px 12px -2px rgba(59,130,246,0.4); }
.um-btn-submit:disabled { background: #94a3b8; box-shadow: none; cursor: not-allowed; }

/* ========== RESPONSIVE ========== */
@media (max-width: 1024px) {
  .kpi-row { grid-template-columns: repeat(2, 1fr); }
  .charts-row { grid-template-columns: 1fr; }
  .donut-container { flex-direction: column; align-items: center; }
  .donut-chart { width: 100px; height: 100px; }
}

@media (max-width: 768px) {
  .admin-dashboard { gap: 1rem; }
  .kpi-row { grid-template-columns: 1fr; gap: 0.75rem; }
  .kpi-card { padding: 1rem; }
  .kpi-value { font-size: 1.5rem; }
  .charts-row { grid-template-columns: 1fr; gap: 0.75rem; }
  .chart-card { padding: 1rem; }
  .bar-chart { height: 110px; gap: 0.4rem; }
  .bar-track { height: 70px; }

  .action-bar { flex-direction: column; gap: 0.75rem; align-items: stretch; }
  .input-with-icon { width: 100%; }

  .feature-card { border-radius: 8px; }
  .table-scroll { overflow-x: auto; -webkit-overflow-scrolling: touch; }
  .data-table { min-width: 700px; }
  .data-table th, .data-table td { padding: 0.6rem 0.75rem; font-size: 0.75rem; }

  .table-pagination { flex-direction: column; gap: 0.5rem; text-align: center; }
  .pag-info { font-size: 0.75rem; }

  /* Modale */
  .user-modal-box { width: 98%; max-width: none; max-height: 95vh; }
  .user-modal-content { flex-direction: column; }
  .user-modal-sidebar { width: 100%; border-left: none; border-top: 1px solid #f1f5f9; }
  .user-modal-form { padding: 1rem; }
  .um-role-grid { grid-template-columns: repeat(2, 1fr); }
  .um-row { flex-direction: column; gap: 0.75rem; }
  .user-modal-foot { padding: 0.75rem 1rem; }
}

@media (max-width: 480px) {
  .kpi-card { padding: 0.75rem; }
  .kpi-value { font-size: 1.25rem; }
  .kpi-icon { width: 36px; height: 36px; border-radius: 8px; }
  .donut-chart { width: 80px; height: 80px; }
  .bar-chart { height: 90px; }
  .bar-track { height: 55px; }
  .bar-value { font-size: 0.6rem; }
  .user-avatar { width: 28px; height: 28px; font-size: 0.7rem; }
  .pag-btn { width: 28px; height: 28px; font-size: 0.75rem; }
}
</style>

