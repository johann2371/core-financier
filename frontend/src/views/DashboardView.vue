<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth.store'
import MainLayout from '../components/MainLayout.vue'
import api from '../services/api'

const authStore = useAuthStore()
const router = useRouter()

const kpis = ref({
  soldeTotalCaisses: 0,
  soldeTotalBanques: 0,
  decaissementsEnAttente: 0,
  totalCreancesClients: 0,
  totalDettesFournisseurs: 0,
  activitesRecentes: []
})
const loading = ref(true)

const fetchKpis = async () => {
  loading.value = true
  const start = Date.now()
  try {
    const response = await api.get('/tableau-bord/kpis')
    kpis.value = response.data
  } catch (error) {
    console.error('Erreur lors de la récupération des KPIs:', error)
  } finally {
    // Garantie de 1s de rotation pour le feedback visuel
    const elapsed = Date.now() - start
    const delay = Math.max(0, 1000 - elapsed)
    setTimeout(() => {
      loading.value = false
    }, delay)
  }
}

let refreshInterval = null

onMounted(() => {
  fetchKpis()
  // Refresh every 30 seconds
  refreshInterval = setInterval(fetchKpis, 30000)
})

import { onUnmounted } from 'vue'
onUnmounted(() => {
  if (refreshInterval) clearInterval(refreshInterval)
})

const formatTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })
}

const formatDateLabel = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const today = new Date()
  if (date.toDateString() === today.toDateString()) return "Aujourd'hui"
  return date.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' })
}

const getActivityIconClass = (type) => {
  switch (type) {
    case 'FACTURE': return 'bg-green'
    case 'ENCAISSEMENT': return 'bg-blue'
    case 'DECAISSEMENT': return 'bg-indigo'
    case 'TIERS': return 'bg-yellow'
    case 'AUTH': return 'bg-indigo'
    default: return 'bg-gray'
  }
}
</script>

<template>
  <MainLayout>
    <!-- ACTIONS RAPIDES -->
    <div class="dashboard-section">
      <h3 class="section-title">ACTIONS RAPIDES</h3>
      <div class="quick-actions-grid">
        <router-link to="/encaissements" class="action-card">
          <div class="action-icon light-blue">
            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="7 10 12 15 17 10"></polyline><line x1="12" y1="15" x2="12" y2="3"></line></svg>
          </div>
          <h4>Nouvel<br/>Encaissement</h4>
        </router-link>
        
        <router-link to="/factures?create=VENTE" class="action-card">
          <div class="action-icon light-indigo">
            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16c0 1.1.9 2 2 2h12a2 2 0 0 0 2-2V8l-6-6z"/><path d="M14 3v5h5M16 13H8M16 17H8M10 9H8"/></svg>
          </div>
          <h4>Nouvelle<br/>Facture</h4>
        </router-link>

        <router-link to="/decaissements" class="action-card">
          <div class="action-icon light-blue">
            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
          </div>
          <h4>Nouveau<br/>Décaissement</h4>
        </router-link>

        <router-link to="/tiers?create=CLIENT" class="action-card">
          <div class="action-icon light-indigo">
            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="8.5" cy="7" r="4"></circle><line x1="20" y1="8" x2="20" y2="14"></line><line x1="23" y1="11" x2="17" y2="11"></line></svg>
          </div>
          <h4>Nouveau<br/>Tier</h4>
        </router-link>
      </div>
    </div>

    <!-- MAIN GRID SPLIT -->
    <div class="main-dashboard-grid">
      <!-- LEFTSIDE COL -->
      <div class="left-col">
        <!-- STATISTIQUES GLOBALES -->
        <div class="dashboard-section">
          <h3 class="section-title">ÉTAT FINANCIER GLOBAL</h3>
          <div class="kpi-grid">
            <div class="kpi-card">
              <span class="kpi-label">Solde Caisses</span>
              <div class="kpi-body">
                <span class="kpi-value">{{ kpis.soldeTotalCaisses?.toLocaleString() }}<span class="currency">XAF</span></span>
              </div>
            </div>

            <div class="kpi-card">
              <span class="kpi-label">Solde Banques</span>
              <div class="kpi-body">
                <span class="kpi-value">{{ kpis.soldeTotalBanques?.toLocaleString() }}<span class="currency">XAF</span></span>
              </div>
            </div>

            <div class="kpi-card">
              <span class="kpi-label">Créances Clients</span>
              <div class="kpi-body">
                <span class="kpi-value success">{{ kpis.totalCreancesClients?.toLocaleString() }}<span class="currency">XAF</span></span>
              </div>
            </div>

            <div class="kpi-card">
              <span class="kpi-label">Dettes Fournisseurs</span>
              <div class="kpi-body">
                <span class="kpi-value danger">{{ kpis.totalDettesFournisseurs?.toLocaleString() }}<span class="currency">XAF</span></span>
              </div>
            </div>

            <div class="kpi-card">
              <span class="kpi-label">Décaissements en attente</span>
              <div class="kpi-body">
                <span class="kpi-value" :class="{ 'warning': kpis.decaissementsEnAttente > 0 }">{{ kpis.decaissementsEnAttente }}</span>
                <span class="kpi-trend attention" v-if="kpis.decaissementsEnAttente > 0">Action requise</span>
              </div>
            </div>
          </div>
        </div>

        <!-- A FAIRE AUJOURD'HUI -->
        <div class="dashboard-section">
          <div class="section-header-row">
            <h3 class="section-title">À FAIRE AUJOURD'HUI</h3>
            <a href="#" class="view-all-link">Voir toutes les tâches</a>
          </div>
          
          <div class="table-container">
            <table class="tasks-table">
              <thead>
                <tr>
                  <th>PRIORITÉ</th>
                  <th>ÉLÉMENT</th>
                  <th>MONTANT</th>
                  <th>ACTION</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="kpis.decaissementsEnAttente > 0">
                  <td><span class="badge badge-urgent">Urgent</span></td>
                  <td>
                    <div class="task-info">
                      <strong>{{ kpis.decaissementsEnAttente }} décaissement(s) à valider</strong>
                      <span>Plusieurs demandes en attente de traitement</span>
                    </div>
                  </td>
                  <td class="task-amount">--</td>
                  <td><router-link to="/decaissements" class="task-action">Traiter</router-link></td>
                </tr>
                <tr>
                  <td><span class="badge badge-normal">Normale</span></td>
                  <td>
                    <div class="task-info">
                      <strong>Rapprochement bancaire</strong>
                      <span>Vérifier les flux de la veille</span>
                    </div>
                  </td>
                  <td class="task-amount">--</td>
                  <td><a href="#" class="task-action">Réviser</a></td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- RIGHT COL -->
      <div class="right-col">
        <!-- ACTIVITÉ RÉCENTE -->
        <div class="dashboard-section right-panel">
          <div class="section-header-row">
            <h3 class="section-title">ACTIVITÉ RÉCENTE</h3>
            <button @click="fetchKpis" class="refresh-btn" :class="{ 'spinning': loading }" title="Rafraîchir">
               <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M23 4v6h-6M1 20v-6h6M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.63 4.36A9 9 0 0 0 20.49 15"/></svg>
            </button>
          </div>
          
          <div class="timeline" v-if="kpis.activitesRecentes?.length > 0">
            <div class="timeline-item" v-for="(act, idx) in kpis.activitesRecentes" :key="idx">
              <div class="timeline-icon" :class="getActivityIconClass(act.type)">
                <svg v-if="act.type === 'FACTURE'" xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>
                <svg v-else-if="act.type === 'ENCAISSEMENT'" xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12V7H5a2 2 0 0 1 0-4h14v4"></path><path d="M3 5v14a2 2 0 0 0 2 2h16v-5"></path><path d="M18 12a2 2 0 0 0 0 4h4v-4Z"></path></svg>
                <svg v-else xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>
              </div>
              <div class="timeline-content">
                <h4>{{ act.action === 'CREATE' ? 'Création' : act.action }} {{ act.type.toLowerCase() }}</h4>
                <p>{{ act.message }}</p>
                <div class="timeline-meta">
                  <span class="user">{{ act.utilisateur }}</span>
                  <span class="time">{{ formatDateLabel(act.date) }} • {{ formatTime(act.date) }}</span>
                </div>
              </div>
            </div>
          </div>
          <div class="empty-activity" v-else>
            <p>Aucune activité récente enregistrée.</p>
          </div>
        </div>

        <!-- SCORE D'EFFICACITE -->
        <div class="dashboard-section right-panel score-panel">
          <h3 class="section-title">SCORE D'EFFICACITÉ</h3>
          <div class="score-header">
            <span class="score-label">Vitesse de Traitement</span>
            <span class="score-percent">94%</span>
          </div>
          <div class="progress-bar-bg">
            <div class="progress-bar-fill" style="width: 94%"></div>
          </div>
          <div class="score-stats">
            <div class="stat-col">
              <strong>1.2m</strong>
              <span>TEMPS DE TRAITEMENT<br/>MOYEN</span>
            </div>
            <div class="stat-col">
              <strong>0.02%</strong>
              <span>TAUX D'ERREUR</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

<style scoped>
/* TYPO & TITLES */
.section-title {
  font-size: 0.75rem;
  font-weight: 600;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 1rem;
}

.dashboard-section {
  margin-bottom: 2rem;
}

/* ACTIONS RAPIDES GRID */
.quick-actions-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1.25rem;
}

.action-card {
  background: white;
  border-radius: 12px;
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
  border: 1px solid #f3f4f6;
  text-decoration: none;
  transition: transform 0.15s, box-shadow 0.15s;
  height: 140px;
}

.action-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1), 0 2px 4px -1px rgba(0,0,0,0.06);
}

.action-icon {
  width: 32px; height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 1rem;
}
.light-blue { background: #eff6ff; color: #2563eb; }
.light-indigo { background: #e0e7ff; color: #4f46e5; }

.action-card h4 {
  font-size: 1rem;
  font-weight: 600;
  color: #111827;
  line-height: 1.2;
}



/* MAIN SPLIT */
.main-dashboard-grid {
  display: grid;
  grid-template-columns: 2.5fr 1fr;
  gap: 2rem;
}
.left-col { display: flex; flex-direction: column; }
.right-col { display: flex; flex-direction: column; gap: 2rem; }

/* KPI GRID */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1.25rem;
}

.kpi-card {
  background: white;
  border-radius: 12px;
  padding: 1.25rem;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
  border: 1px solid #f3f4f6;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.kpi-label {
  font-size: 0.8rem;
  font-weight: 500;
  color: #6b7280;
  margin-bottom: 0.75rem;
}

.kpi-body {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}

.kpi-value {
  font-size: 1.75rem;
  font-weight: 700;
  color: #111827;
  letter-spacing: -0.025em;
}

.kpi-value.warning { color: #f59e0b; }
.kpi-value.success { color: #10b981; }
.kpi-value.danger { color: #ef4444; }
.currency { font-size: 1rem; color: #4b5563; font-weight: 600; margin-left: 2px; }

.kpi-trend {
  font-size: 0.8rem;
  font-weight: 600;
}
.kpi-trend.positive { color: #10b981; }
.kpi-trend.attention { color: #f59e0b; font-size: 0.7rem; }

.kpi-subtrend {
  text-align: right;
  font-size: 0.75rem;
  font-weight: 600;
  color: #4b5563;
}
.kpi-subtrend .light { color: #9ca3af; font-weight: 500; }

/* TABLE TO DO */
.section-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}
.section-header-row .section-title { margin-bottom: 0; }

.view-all-link {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--c-primary);
  text-decoration: none;
}
.view-all-link:hover { text-decoration: underline; }

.table-container {
  background: white;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
  border: 1px solid #f3f4f6;
  overflow: hidden;
}

.tasks-table {
  width: 100%;
  border-collapse: collapse;
}

.tasks-table th {
  text-align: left;
  padding: 1rem 1.25rem;
  font-size: 0.7rem;
  font-weight: 600;
  color: #6b7280;
  background: #f9fafb;
  border-bottom: 1px solid #e5e7eb;
}

.tasks-table td {
  padding: 1.25rem;
  border-bottom: 1px solid #f3f4f6;
  vertical-align: middle;
}

.tasks-table tr:last-child td { border-bottom: none; }

.badge {
  padding: 0.25rem 0.6rem;
  border-radius: 4px;
  font-size: 0.7rem;
  font-weight: 600;
}
.badge-urgent { background: #fee2e2; color: #ef4444; }
.badge-high { background: #fef3c7; color: #d97706; }
.badge-normal { background: #eff6ff; color: #3b82f6; }

.task-info { display: flex; flex-direction: column; gap: 0.25rem; }
.task-info strong { font-size: 0.875rem; color: #111827; }
.task-info span { font-size: 0.75rem; color: #9ca3af; }

.task-amount { font-weight: 600; color: #111827; font-size: 0.9rem; }
.task-action { color: var(--c-primary); font-size: 0.85rem; font-weight: 500; text-decoration: none; }
.task-action:hover { text-decoration: underline; }


/* RIGHT PANELS */
.right-panel {
  background: white;
  border-radius: 12px;
  padding: 1.5rem;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
  border: 1px solid #f3f4f6;
  margin-bottom: 0;
}

/* TIMELINE */
.timeline {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  max-height: 480px; /* Environ 5-6 activités */
  overflow-y: auto;
  padding-right: 0.5rem; /* Espace pour le scrollbar */
}

/* Scrollbar styling */
.timeline::-webkit-scrollbar {
  width: 4px;
}
.timeline::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 10px;
}
.timeline::-webkit-scrollbar-thumb {
  background: #d1d5db;
  border-radius: 10px;
}
.timeline::-webkit-scrollbar-thumb:hover {
  background: #9ca3af;
}

.timeline-item {
  display: flex;
  gap: 1rem;
  position: relative;
}

/* Barre de connexion verticale optionnelle si on veut reproduire la timeline */
/* .timeline-item:not(:last-child)::before {
  content: ''; position: absolute; left: 12px; top: 24px; bottom: -1.5rem; width: 2px; background: #e5e7eb;
} */

.timeline-icon {
  width: 24px; height: 24px;
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  color: white;
  flex-shrink: 0;
  margin-top: 2px;
}
.bg-green { background: #10b981; }
.bg-blue { background: #3b82f6; }
.bg-indigo { background: #6366f1; }
.bg-yellow { background: #f59e0b; color: white; }

.timeline-content { display: flex; flex-direction: column; }
.timeline-content h4 { font-size: 0.85rem; font-weight: 600; color: #111827; margin-bottom: 0.1rem; }
.timeline-content p { font-size: 0.8rem; color: #6b7280; margin-bottom: 0.25rem; }
.timeline-content .time { font-size: 0.7rem; color: #9ca3af; font-weight: 500; }

.timeline-meta { display: flex; justify-content: space-between; align-items: center; margin-top: 0.25rem; }
.timeline-meta .user { font-size: 0.7rem; font-weight: 600; color: #4b5563; }
.timeline-meta .time { font-size: 0.65rem; color: #9ca3af; }

.empty-activity { text-align: center; padding: 2rem 0; color: #9ca3af; font-size: 0.875rem; }

/* SCORE BOARD */
.score-header { display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 0.75rem;}
.score-label { font-size: 0.875rem; font-weight: 600; color: #111827; }
.score-percent { font-size: 1rem; font-weight: 700; color: var(--c-primary); }

.progress-bar-bg { width: 100%; height: 6px; background: #e5e7eb; border-radius: 3px; overflow: hidden; margin-bottom: 1.5rem; }
.progress-bar-fill { height: 100%; background: var(--c-primary); border-radius: 3px; }

.score-stats { display: flex; justify-content: space-between; }
.stat-col { display: flex; flex-direction: column; align-items: center; text-align: center; gap: 0.25rem; }
.stat-col strong { font-size: 1.25rem; font-weight: 700; color: #111827; }
.stat-col span { font-size: 0.6rem; font-weight: 600; color: #9ca3af; letter-spacing: 0.05em; line-height: 1.2;}

.refresh-btn { 
  background: none; 
  border: none; 
  color: #9ca3af; 
  cursor: pointer; 
  display: flex; 
  align-items: center; 
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  padding: 4px;
  border-radius: 50%;
}
.refresh-btn:hover { 
  color: #2563eb; 
  background: #eff6ff;
  transform: scale(1.15) rotate(15deg);
}
.refresh-btn:active {
  transform: scale(0.95);
}
.spinning { 
  animation: spin 0.8s linear infinite; 
}
@keyframes spin { 
  from { transform: rotate(0deg); } 
  to { transform: rotate(360deg); } 
}
</style>
