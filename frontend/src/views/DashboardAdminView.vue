<script setup>
import { ref, onMounted, computed } from 'vue'
import MainLayout from '../components/MainLayout.vue'
import { useUtilisateurStore } from '../stores/utilisateur.store'
import { useAuthStore } from '../stores/auth.store'
import { useAuditStore } from '../stores/audit.store'
import { useDashboardStore } from '../stores/dashboard.store'
import { useParametrageStore } from '../stores/parametrage.store'
import { useLangStore } from '../stores/lang.store'
import { useUiStore } from '../stores/ui.store'
import { Line } from 'vue-chartjs'
import { 
  Chart as ChartJS, 
  Title, 
  Tooltip, 
  Legend, 
  LineElement, 
  CategoryScale, 
  LinearScale, 
  PointElement, 
  Filler 
} from 'chart.js'
import { 
  BanknotesIcon, 
  ClockIcon, 
  ExclamationTriangleIcon, 
  CreditCardIcon, 
  UserGroupIcon, 
  ArrowTrendingUpIcon, 
  ArrowTrendingDownIcon,
  CurrencyDollarIcon,
  CheckIcon,
  WalletIcon,
  PencilSquareIcon,
  UserMinusIcon,
  UserPlusIcon,
  ShieldCheckIcon,
  MagnifyingGlassIcon,
  PlusIcon,
  XMarkIcon,
  IdentificationIcon,
  ChartBarIcon,
  BuildingOfficeIcon,
  NoSymbolIcon,
  ArrowPathIcon,
  DocumentTextIcon,
  UserIcon
} from '@heroicons/vue/24/outline'

ChartJS.register(Title, Tooltip, Legend, LineElement, CategoryScale, LinearScale, PointElement, Filler)

const authStore = useAuthStore()
const utilisateurStore = useUtilisateurStore()
const auditStore = useAuditStore()
const dashboardStore = useDashboardStore()
const parametrageStore = useParametrageStore()
const langStore = useLangStore()
const uiStore = useUiStore()
const t = computed(() => langStore.t)

const forecastPeriod = ref(30)

const updateForecast = async () => {
  await dashboardStore.fetchKpis(forecastPeriod.value)
}

onMounted(() => {
  // Chargement en parallèle pour éviter qu'un échec ne bloque les autres
  dashboardStore.fetchKpis(forecastPeriod.value)
  utilisateurStore.fetchUtilisateurs()
  auditStore.fetchLogs(0, 200, '', '')
  parametrageStore.fetchParametres()
})

const kpis = computed(() => dashboardStore.kpis || {})

const selectedCurrency = computed(() => {
  const param = parametrageStore.parametres?.find(p => p.cle === 'DEVISE_BASE_CODE')
  return param ? param.valeur : 'XAF'
})
const usdRate = 600 // 1 USD = 600 XAF

const formatCurrency = (val) => {
  if (val === undefined || val === null) return '0'
  if (selectedCurrency.value === 'USD') {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(val / usdRate)
  }
  return new Intl.NumberFormat('fr-FR').format(val) + ' XAF'
}

// === GESTION UTILISATEURS ===
const searchUser = ref('')
const usersFiltered = computed(() => {
  const users = utilisateurStore.utilisateurs || []
  if (!searchUser.value) return users
  const s = searchUser.value.toLowerCase()
  return users.filter(u => 
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
const totalUsers = computed(() => (utilisateurStore.utilisateurs || []).length)
const activeUsers = computed(() => (utilisateurStore.utilisateurs || []).filter(u => u.actif !== false).length)
const blockedUsers = computed(() => (utilisateurStore.utilisateurs || []).filter(u => u.actif === false).length)

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
  const users = utilisateurStore.utilisateurs || []
  users.forEach(u => {
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

// === GRAPHIQUE ÉVOLUTION TRÉSORERIE (6 MOIS) ===
const evolutionChartData = computed(() => {
  const data = kpis.value.evolutionMensuelle || []
  return {
    labels: data.map(d => d.mois),
    datasets: [
      {
        label: 'Encaissements',
        data: data.map(d => d.encaissements),
        borderColor: '#10b981',
        backgroundColor: (context) => {
          const chart = context.chart
          const { ctx, chartArea } = chart
          if (!chartArea) return null
          const gradient = ctx.createLinearGradient(0, chartArea.top, 0, chartArea.bottom)
          gradient.addColorStop(0, 'rgba(16, 185, 129, 0.2)')
          gradient.addColorStop(1, 'rgba(16, 185, 129, 0)')
          return gradient
        },
        fill: true,
        tension: 0.4,
        pointBackgroundColor: '#10b981',
        pointBorderColor: '#fff',
        pointHoverRadius: 6,
        pointRadius: 4
      },
      {
        label: 'Décaissements',
        data: data.map(d => d.decaissements),
        borderColor: '#f59e0b',
        backgroundColor: (context) => {
          const chart = context.chart
          const { ctx, chartArea } = chart
          if (!chartArea) return null
          const gradient = ctx.createLinearGradient(0, chartArea.top, 0, chartArea.bottom)
          gradient.addColorStop(0, 'rgba(245, 158, 11, 0.2)')
          gradient.addColorStop(1, 'rgba(245, 158, 11, 0)')
          return gradient
        },
        fill: true,
        tension: 0.4,
        pointBackgroundColor: '#f59e0b',
        pointBorderColor: '#fff',
        pointHoverRadius: 6,
        pointRadius: 4
      }
    ]
  }
})

const evolutionChartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      display: false
    },
    tooltip: {
      mode: 'index',
      intersect: false,
      backgroundColor: uiStore.isDarkMode ? '#0f172a' : 'rgba(30, 41, 59, 0.9)',
      titleColor: '#fff',
      bodyColor: '#fff',
      padding: 12,
      cornerRadius: 8,
      borderColor: uiStore.isDarkMode ? '#1e293b' : 'transparent',
      borderWidth: 1,
      callbacks: {
        label: function(context) {
          let label = context.dataset.label || '';
          if (label) label += ': ';
          if (context.parsed.y !== null) {
            const val = selectedCurrency.value === 'USD' ? context.parsed.y / usdRate : context.parsed.y;
            const formatted = selectedCurrency.value === 'USD' 
              ? new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(val)
              : new Intl.NumberFormat('fr-FR').format(val) + ' FCFA';
            label += formatted;
          }
          return label;
        }
      }
    }
  },
  scales: {
    y: {
      beginAtZero: true,
      grid: {
        color: uiStore.isDarkMode ? 'rgba(255, 255, 255, 0.03)' : 'rgba(226, 232, 240, 0.5)',
        drawBorder: false
      },
      ticks: {
        color: uiStore.isDarkMode ? '#475569' : '#94a3b8',
        font: { size: 11 },
        callback: function(value) {
          if (value >= 1000000) return (value / 1000000).toFixed(1) + 'M'
          if (value >= 1000) return (value / 1000).toFixed(0) + 'k'
          return value
        }
      }
    },
    x: {
      grid: {
        display: false
      },
      ticks: {
        color: uiStore.isDarkMode ? '#475569' : '#94a3b8',
        font: { size: 11, weight: '600' }
      }
    }
  }
}

// === FORMAT DATE ET ACTIVITÉS ===
const formatLastLogin = (dateStr) => {
  if (!dateStr) return t.value('adminDashboard.jamais')
  const d = new Date(dateStr)
  const now = new Date()
  const diff = Math.floor((now - d) / 1000)
  if (diff < 60) return t.value('adminDashboard.aLinstant')
  if (diff < 3600) return `Il y a ${Math.floor(diff / 60)} min`
  if (diff < 86400) return `Il y a ${Math.floor(diff / 3600)}h`
  return d.toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' })
}

// === FORMAT DATE ET ACTIVITÉS (Aligné sur DashboardView) ===
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
    case 'CONNEXION': return 'bg-blue'
    default: return 'bg-gray'
  }
}

// === PRÉVISION TRÉSORERIE 30j ===
const forecastTrend = computed(() => {
  const prev = kpis.value.soldePrevisionnel30j
  const actuel = kpis.value.soldeTresorerieTotal
  if (!prev || !actuel) return 0
  return prev - actuel
})

const forecastChartData = computed(() => {
  const pts = kpis.value.pointsPrevisionnels || []
  return {
    labels: pts.map(p => p.date),
    datasets: [{
      label: 'Solde prévisionnel',
      data: pts.map(p => p.solde),
      borderColor: '#6366f1',
      backgroundColor: (context) => {
        const chart = context.chart
        const { ctx, chartArea } = chart
        if (!chartArea) return null
        const gradient = ctx.createLinearGradient(0, chartArea.top, 0, chartArea.bottom)
        gradient.addColorStop(0, 'rgba(99, 102, 241, 0.2)')
        gradient.addColorStop(1, 'rgba(99, 102, 241, 0)')
        return gradient
      },
      fill: true,
      tension: 0.4,
      pointBackgroundColor: '#6366f1',
      pointBorderColor: '#fff',
      pointHoverRadius: 7,
      pointRadius: 5,
      borderWidth: 3
    }]
  }
})

const forecastChartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { display: false },
    tooltip: {
      backgroundColor: 'rgba(30, 41, 59, 0.9)',
      titleColor: '#fff',
      bodyColor: '#fff',
      padding: 12,
      cornerRadius: 8,
      callbacks: {
        label: function(context) {
          const val = selectedCurrency.value === 'USD' ? context.parsed.y / usdRate : context.parsed.y;
          return selectedCurrency.value === 'USD' 
            ? new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(val)
            : new Intl.NumberFormat('fr-FR').format(val) + ' FCFA'
        }
      }
    }
  },
  scales: {
    y: {
      beginAtZero: false,
      grid: { color: 'rgba(226, 232, 240, 0.5)', drawBorder: false },
      ticks: {
        color: '#94a3b8',
        font: { size: 11 },
        callback: function(value) {
          if (value >= 1000000) return (value / 1000000).toFixed(1) + 'M'
          if (value >= 1000) return (value / 1000).toFixed(0) + 'k'
          return value
        }
      }
    },
    x: {
      grid: { display: false },
      ticks: { color: '#94a3b8', font: { size: 11, weight: '600' } }
    }
  }
}

// === DSO / DPO ===
const dsoClass = computed(() => {
  const v = kpis.value.dso || 0
  if (v <= 30) return 'good'
  if (v <= 60) return 'warn'
  return 'danger'
})

const dsoMessage = computed(() => {
  const v = kpis.value.dso || 0
  if (v <= 30) return t.value('dashboard.dsoExcellent')
  if (v <= 60) return t.value('dashboard.dsoAttention')
  return t.value('dashboard.dsoCritique')
})

const dpoMessage = computed(() => {
  const v = kpis.value.dpo || 0
  if (v <= 15) return t.value('dashboard.dpoRapide')
  if (v <= 45) return t.value('dashboard.dpoRaisonnable')
  return t.value('dashboard.dpoEleve')
})

// === RÉPARTITION DÉPENSES ===
const depPercent = (val) => {
  const rep = kpis.value.repartitionDepensesParCategorie || {}
  const vals = Object.values(rep)
  if (!vals.length) return 0
  const max = Math.max(...vals.map(v => Number(v)))
  return max > 0 ? (Number(val) / max * 100) : 0
}
</script>

<template>
  <MainLayout>
    <template #title>{{   t("adminDashboard.titre")   }}</template>
    <template #subtitle>{{   t("adminDashboard.sousTitre")   }}</template>

    <div class="admin-dashboard">
      <div v-if="utilisateurStore.error" class="error-banner mb-4">{{   utilisateurStore.error   }}</div>

      <!-- HEADER SECTION: Main Financial KPIs (2x2) + Recent Activities -->
      <div class="dashboard-header-row">
        <!-- 2x2 Financial Grid -->
        <div class="main-kpis-wrapper">
          <div class="kpi-card premium highlight-primary">
            <div class="kpi-inner">
              <div class="kpi-label">{{   t("adminDashboard.soldeTresorerie")   }}</div>
              <div class="kpi-value text-blue">{{   formatCurrency(kpis.soldeTresorerieTotal)   }}</div>
              <div class="kpi-desc">{{   selectedCurrency === 'XAF' ? 'FCFA' : 'USD'   }} {{   t("adminDashboard.disponibles")   }}</div>
            </div>
            <div class="kpi-icon-circ blue">
              <BanknotesIcon class="w-6 h-6" />
            </div>
          </div>

          <div class="kpi-card">
            <div class="kpi-inner">
              <div class="kpi-label">{{   t("adminDashboard.decaissementsAttente")   }}</div>
              <div class="kpi-value">{{   kpis.decaissementsEnAttente || 0   }}</div>
              <div class="kpi-desc">{{   t("adminDashboard.dontAttentePDG")   }} <span class="fw-700">{{   kpis.decaissementsEnAttentePDG || 0   }}</span> {{   t("adminDashboard.enAttentePDG")   }}</div>
            </div>
            <div class="kpi-icon-circ orange">
              <ClockIcon class="w-6 h-6" />
            </div>
          </div>

          <div class="kpi-card">
            <div class="kpi-inner">
              <div class="kpi-label">{{   t("adminDashboard.facturesImpayees")   }}</div>
              <div class="kpi-value text-red">{{   kpis.facturesImpayeesCount || 0   }}</div>
              <div class="kpi-desc" :class="{'text-red fw-700': kpis.facturesEnRetardCount > 0}">{{   kpis.facturesEnRetardCount || 0   }} {{   t("adminDashboard.enRetard")   }}</div>
            </div>
            <div class="kpi-icon-circ red">
               <ExclamationTriangleIcon class="w-6 h-6" />
            </div>
          </div>

          <div class="kpi-card">
            <div class="kpi-inner">
              <div class="kpi-label">{{   t("adminDashboard.dettesFournisseurs")   }}</div>
              <div class="kpi-value text-purple">{{   formatCurrency(kpis.totalDettesFournisseurs)   }}</div>
              <div class="kpi-desc">{{   t("adminDashboard.totalFacturesDues")   }}</div>
            </div>
            <div class="kpi-icon-circ purple">
              <CreditCardIcon class="w-6 h-6" />
            </div>
          </div>
        </div>

        <!-- Recent Activities Card (Style DashboardView) -->
        <div class="activities-card">
          <div class="activities-head">
            <h3 class="activities-title">{{   t("dashboard.activiteRecente")   }}</h3>
            <div class="activities-badge">{{   kpis.activitesRecentes?.length || 0   }}</div>
          </div>
          
          <div class="timeline" v-if="kpis.activitesRecentes?.length > 0">
            <div class="timeline-item" v-for="(act, idx) in kpis.activitesRecentes" :key="idx">
              <div class="timeline-icon" :class="getActivityIconClass(act.type)">
                <CheckIcon v-if="act.type === 'FACTURE'" class="w-3 h-3" />
                <WalletIcon v-else-if="act.type === 'ENCAISSEMENT'" class="w-3 h-3" />
                <ClockIcon v-else class="w-3 h-3" />
              </div>
              <div class="timeline-content">
                <h4>{{   act.action === 'CREATE' ? 'Création' : act.action   }} {{   act.type.toLowerCase()   }}</h4>
                <p>{{   act.message   }}</p>
                <div class="timeline-meta">
                  <span class="user">{{   act.utilisateur   }}</span>
                  <span class="time">{{   formatDateLabel(act.date)   }} • {{   formatTime(act.date)   }}</span>
                </div>
              </div>
            </div>
          </div>
          <div class="empty-activities" v-else>
            <p>{{   t("dashboard.aucuneActivite")   }}</p>
          </div>
        </div>
      </div>

      <!-- SECONDARY KPIs (Horizontal Row) -->
      <div class="kpi-grid secondary-kpis-row">
        <div class="kpi-card">
          <div class="kpi-inner">
            <div class="kpi-label">{{   t("adminDashboard.utilisateursActifs")   }}</div>
            <div class="kpi-value text-green">{{   kpis.utilisateursActifs || 0   }}</div>
            <div class="kpi-desc text-danger" v-if="kpis.utilisateursBloques > 0">{{   kpis.utilisateursBloques   }} {{   t("adminDashboard.compteBloque")   }}</div>
            <div class="kpi-desc" v-else>{{   t("adminDashboard.aucunCompteBloque")   }}</div>
          </div>
          <div class="kpi-icon-circ green">
            <UserGroupIcon class="w-6 h-6" />
          </div>
        </div>

        <div class="kpi-card">
          <div class="kpi-inner">
            <div class="kpi-label">{{   t("adminDashboard.encaissementsCeMois")   }}</div>
            <div class="kpi-value">{{   formatCurrency(kpis.encaissementsMoisActuel)   }}</div>
            <div class="kpi-desc">
              {{   selectedCurrency === 'XAF' ? 'FCFA' : 'USD'   }} · <span :class="kpis.progressionEncaissements >= 0 ? 'text-green' : 'text-red'">
                {{   kpis.progressionEncaissements > 0 ? '+' : ''   }}{{   kpis.progressionEncaissements?.toFixed(1)   }}% {{   t("adminDashboard.vsMoisDernier")   }}
              </span>
            </div>
          </div>
        </div>

        <div class="kpi-card">
          <div class="kpi-inner">
            <div class="kpi-label">{{   t("adminDashboard.decaissementsCeMois")   }}</div>
            <div class="kpi-value">{{   formatCurrency(kpis.decaissementsMoisActuel)   }}</div>
            <div class="kpi-desc">
              {{   selectedCurrency === 'XAF' ? 'FCFA' : 'USD'   }} · <span :class="kpis.progressionDecaissements <= 0 ? 'text-green' : 'text-red'">
                {{   kpis.progressionDecaissements > 0 ? '+' : ''   }}{{   kpis.progressionDecaissements?.toFixed(1)   }}% {{   t("adminDashboard.vsMoisDernier")   }}
              </span>
            </div>
          </div>
        </div>

        <div class="kpi-card">
          <div class="kpi-inner">
            <div class="kpi-label">{{   t("adminDashboard.operationsAujourdhui")   }}</div>
            <div class="kpi-value">{{   kpis.operationsDuJour || 0   }}</div>
            <div class="kpi-desc">
              <span class="text-green fw-600">{{   kpis.encaissementsDuJourCount || 0   }} {{   t("adminDashboard.enc")   }}</span> 
              · <span class="text-blue fw-600">{{   kpis.decaissementsDuJourCount || 0   }} {{   t("adminDashboard.dec")   }}</span>
            </div>
          </div>
          <div class="kpi-icon-circ cyan">
             <CurrencyDollarIcon class="w-6 h-6" />
          </div>
        </div>
      </div>

      <!-- GRAPHIQUES ROW -->
      <div class="charts-row">
        <!-- Donut Chart: Répartition des rôles -->
        <div class="chart-card">
          <div class="chart-title">
            Répartition des rôles
            <span v-if="utilisateurStore.loading" class="loading-inline">(Chargement...)</span>
          </div>
          
          <div v-if="utilisateurStore.error" class="chart-error">
             <ExclamationTriangleIcon class="w-5 h-5" />
             <span>{{ utilisateurStore.error }}</span>
          </div>
          
          <div v-else class="donut-container">
            <svg v-if="donutSegments.length > 0" viewBox="0 0 36 36" class="donut-chart">
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
            <div v-else class="empty-donut">
               <UserGroupIcon class="w-8 h-8 text-slate-200" />
               <span>Aucun utilisateur</span>
            </div>
            <div class="donut-legend" v-if="donutSegments.length > 0">
              <div v-for="seg in donutSegments" :key="seg.role" class="legend-item">
                <span class="legend-dot" :style="{ background: seg.color }"></span>
                <span class="legend-label">{{   seg.role.replace('_', ' ')   }}</span>
                <span class="legend-count">{{   seg.count   }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Line Chart: Évolution Trésorerie (6 mois) -->
        <div class="chart-card">
          <div class="chart-header">
            <div class="chart-title">Évolution de la trésorerie — 6 derniers mois</div>
            <div class="chart-legend-custom">
              <div class="legend-item-inline">
                <span class="dot green"></span>
                <span>Encaissements</span>
              </div>
              <div class="legend-item-inline">
                <span class="dot orange"></span>
                <span>Décaissements</span>
              </div>
            </div>
          </div>
          <div class="chart-body" style="height: 220px; position: relative;">
            <Line :data="evolutionChartData" :options="evolutionChartOptions" />
          </div>
        </div>
      </div>

      <!-- STRATEGIC KPIs ROW: DSO/DPO + Prévision 30j -->
      <div class="charts-row">
        <!-- Prévision de Trésorerie -->
        <div class="chart-card">
          <div class="chart-header" style="align-items: center; display: flex;">
            <div class="chart-title" style="margin-right: 1rem;">Prévision de Trésorerie</div>
            <select v-model="forecastPeriod" @change="updateForecast" style="padding: 0.2rem 0.5rem; border-radius: 4px; border: 1px solid #e2e8f0; font-size: 0.8rem; background-color: #f8fafc; color: #334155; cursor: pointer; outline: none;">
              <option :value="30">30 jours</option>
              <option :value="60">60 jours</option>
              <option :value="90">90 jours</option>
            </select>
            <div class="forecast-badge" style="margin-left: auto;">
              <span :class="forecastTrend >= 0 ? 'text-green' : 'text-red'">
                {{   forecastTrend >= 0 ? '↑' : '↓'   }} {{   formatCurrency(kpis.soldePrevisionnel30j)   }} à J+{{   forecastPeriod   }}
              </span>
            </div>
          </div>
          <div class="chart-body" style="height: 220px; position: relative;">
            <Line :data="forecastChartData" :options="forecastChartOptions" />
          </div>
        </div>

        <!-- DSO / DPO Cards -->
        <div class="dso-dpo-container">
          <div class="dso-card">
            <div class="dso-header">
              <span class="dso-label">DSO</span>
              <span class="dso-sublabel">Délai moyen encaissement clients</span>
            </div>
            <div class="dso-value">
              <span class="dso-number">{{   kpis.dso || 0   }}</span>
              <span class="dso-unit">jours</span>
            </div>
            <div class="dso-bar">
              <div class="dso-fill" :class="dsoClass" :style="{ width: Math.min(kpis.dso || 0, 90) / 90 * 100 + '%' }"></div>
            </div>
            <div class="dso-hint">{{   dsoMessage   }}</div>
          </div>

          <div class="dso-card">
            <div class="dso-header">
              <span class="dso-label">DPO</span>
              <span class="dso-sublabel">Délai moyen paiement fournisseurs</span>
            </div>
            <div class="dso-value">
              <span class="dso-number">{{   kpis.dpo || 0   }}</span>
              <span class="dso-unit">jours</span>
            </div>
            <div class="dso-bar">
              <div class="dso-fill dpo" :style="{ width: Math.min(kpis.dpo || 0, 90) / 90 * 100 + '%' }"></div>
            </div>
            <div class="dso-hint">{{   dpoMessage   }}</div>
          </div>

          <!-- Répartition Dépenses -->
          <div class="depenses-card" v-if="Object.keys(kpis.repartitionDepensesParCategorie || {}).length > 0">
            <div class="chart-title" style="margin-bottom: 0.75rem;">Dépenses du mois par catégorie</div>
            <div class="dep-bars">
              <div v-for="(val, cat) in kpis.repartitionDepensesParCategorie" :key="cat" class="dep-row">
                <span class="dep-cat">{{   cat   }}</span>
                <div class="dep-track">
                  <div class="dep-fill" :style="{ width: depPercent(val) + '%' }"></div>
                </div>
                <span class="dep-amount">{{   formatCurrency(val)   }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Action Bar -->
      <div class="action-bar mb-4">
        <div class="input-with-icon">
           <MagnifyingGlassIcon class="w-4 h-4 text-slate-400" />
           <input v-model="searchUser" type="text" :placeholder="t('adminDashboard.rechercherUtilisateur')" />
        </div>
        <button @click="openCreateUser" class="btn-primary" style="background-color: #3b82f6; color: white; border-radius: 8px; padding: 0.75rem 1.25rem; border: none; font-weight: 600; display: flex; align-items: center; gap: 8px; box-shadow: 0 4px 6px -1px rgba(59, 130, 246, 0.4); cursor: pointer;">
          <UserPlusIcon class="w-5 h-5" />
          Nouvel Utilisateur
        </button>
      </div>

      <div v-if="utilisateurStore.error" class="error-banner mb-4">{{   utilisateurStore.error   }}</div>

      <!-- Table Utilisateurs -->
      <div class="feature-card">
        <div class="table-scroll">
          <table class="data-table">
            <thead>
              <tr>
                <th>{{   t("adminDashboard.acteur")   }}</th>
                <th>{{   t("adminDashboard.idConnexion")   }}</th>
                <th>{{   t("adminDashboard.roleSysteme")   }}</th>
                <th>{{   t("adminDashboard.derniereConnexion")   }}</th>
                <th>{{   t("adminDashboard.statut")   }}</th>
                <th class="text-center">{{   t("adminDashboard.actions")   }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="u in paginatedUsers" :key="u.id">
                <td>
                  <div class="user-cell">
                    <div class="user-avatar" :style="{ backgroundColor: getAvatarColor(u.prenom), color: 'white' }">
                      {{   u.prenom ? u.prenom.charAt(0) + (u.nom ? u.nom.charAt(0) : '') : '?'   }}
                    </div>
                    <div class="user-info">
                      <strong>{{   u.prenom   }} {{   u.nom   }}</strong>
                    </div>
                  </div>
                </td>
                <td><span class="text-mono">{{   u.email   }}</span></td>
                <td>
                  <span class="role-badge">{{   u.role?.replace('_', ' ')   }}</span>
                </td>
                <td>
                  <span class="last-login" :class="{ 'never': !u.dernierAcces }">
                    {{   formatLastLogin(u.dernierAcces)   }}
                  </span>
                </td>
                <td class="status-badge-cell">
                  <span class="status-badge" :class="u.actif !== false ? 'active' : 'inactive'">
                    <template v-if="u.actif !== false">
                      <CheckIcon class="w-3 h-3" /> Actif
                    </template>
                    <template v-else>
                      <XMarkIcon class="w-3 h-3" /> Inactif
                    </template>
                  </span>
                </td>
                <td class="cell-actions text-center">
                  <button @click="openEditUser(u)" class="btn-icon" title="Modifier" :disabled="u.email === authStore.userName" style="color:#3b82f6;">
                    <PencilSquareIcon class="w-4 h-4" />
                  </button>
                  <button v-if="u.actif !== false" @click="bloquerInfo(u.id)" class="btn-icon danger" title="Désactiver" :disabled="u.email === authStore.userName">
                    <NoSymbolIcon class="w-4 h-4" />
                  </button>
                  <button v-else @click="reactiverInfo(u.id)" class="btn-icon" title="Réactiver le compte" style="color:#16a34a;">
                    <ArrowPathIcon class="w-4 h-4" />
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Pagination -->
        <div v-if="totalPages > 1" class="table-pagination">
          <span class="pag-info">Affichage {{   (currentPage - 1) * pageSize + 1   }}–{{   Math.min(currentPage * pageSize, usersFiltered.length)   }} sur {{   usersFiltered.length   }}</span>
          <div class="pag-buttons">
            <button @click="currentPage = Math.max(1, currentPage - 1)" :disabled="currentPage === 1" class="pag-btn">‹</button>
            <button 
              v-for="p in totalPages" :key="p" 
              @click="currentPage = p" 
              :class="['pag-btn', { active: p === currentPage }]"
            >{{   p   }}</button>
            <button @click="currentPage = Math.min(totalPages, currentPage + 1)" :disabled="currentPage === totalPages" class="pag-btn">›</button>
          </div>
        </div>
      </div>
    </div>

    <!-- Modale Utilisateur -->
    <div v-if="showUserModal" class="user-modal-overlay" @click.self="showUserModal = false">
      <div class="user-modal-box">
        <div class="user-modal-head">
          <h3>{{   editUserId ? 'Modifier Utilisateur' : 'Nouvel Utilisateur'   }}</h3>
          <button @click="showUserModal = false" class="user-modal-close">
            <XMarkIcon class="w-6 h-6" />
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
                    <WalletIcon v-if="r === 'CAISSIER'" class="w-5 h-5" />
                    <DocumentTextIcon v-else-if="r === 'COMPTABLE'" class="w-5 h-5" />
                    <IdentificationIcon v-else-if="r === 'RESPONSABLE_FINANCIER'" class="w-5 h-5" />
                    <UserIcon v-else-if="r === 'PDG'" class="w-5 h-5" />
                    <ShieldCheckIcon v-else class="w-5 h-5" />
                  </span>
                  <span class="um-role-name">{{   r.replace('_', ' ')   }}</span>
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
              <label class="um-label">Mot de passe {{   editUserId ? '(laisser vide pour ne pas changer)' : 'par défaut'   }} <span v-if="!editUserId" style="color:#ef4444;">*</span></label>
              <input v-model="userForm.password" type="text" :required="!editUserId" class="um-input" :placeholder="editUserId ? 'Laisser vide pour conserver l\'ancien' : 'Définir un mot de passe...'" />
            </div>
          </form>

          <div class="user-modal-sidebar">
            <div class="um-sidebar-title">
              <ShieldCheckIcon class="w-4 h-4" />
              SÉCURITÉ &amp; ACCÈS
            </div>
            
            <div class="um-alert um-alert-blue" v-if="userForm.role">
              <strong>Permissions {{   userForm.role.replace('_', ' ')   }}</strong>
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
          <button type="button" @click="showUserModal = false" class="um-btn-cancel">{{   t("common.annuler")   }}</button>
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

/* KPI GRID & LAYOUT SYSTEM */
.dashboard-header-row {
  display: flex;
  gap: 1.25rem;
  margin-bottom: 1.25rem;
  align-items: stretch;
}

.main-kpis-wrapper {
  flex: 2;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1.25rem;
}

.activities-card {
  flex: 1;
  background: white;
  border-radius: 16px;
  border: 1px solid #f1f5f9;
  display: flex;
  flex-direction: column;
  box-shadow: 0 1px 3px rgba(0,0,0,0.02);
  min-width: 320px;
}

.activities-head {
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid #f8fafc;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.activities-title {
  font-size: 0.85rem;
  font-weight: 700;
  color: #334155;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.activities-badge {
  background: #f1f5f9;
  color: #64748b;
  font-size: 0.75rem;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 12px;
}

/* TIMELINE STYLE (Copied from DashboardView) */
.timeline {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  padding: 1.25rem 1.5rem;
  max-height: 280px;
  overflow-y: auto;
}

.timeline::-webkit-scrollbar { width: 4px; }
.timeline::-webkit-scrollbar-track { background: #f1f1f1; border-radius: 10px; }
.timeline::-webkit-scrollbar-thumb { background: #d1d5db; border-radius: 10px; }

.timeline-item {
  display: flex;
  gap: 1rem;
  position: relative;
}

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
.bg-gray { background: #94a3b8; }

.timeline-content { display: flex; flex-direction: column; flex: 1; min-width: 0; }
.timeline-content h4 { font-size: 0.825rem; font-weight: 700; color: #1e293b; margin-bottom: 0.2rem; text-transform: capitalize; }
.timeline-content p { font-size: 0.8rem; color: #64748b; margin-bottom: 0.4rem; line-height: 1.4; word-break: break-all; }

.timeline-meta { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.timeline-meta .user { font-size: 0.68rem; font-weight: 700; color: #475569; }
.timeline-meta .time { font-size: 0.65rem; color: #94a3b8; font-weight: 600; }

.empty-activities {
  padding: 2rem;
  text-align: center;
  color: #94a3b8;
  font-size: 0.85rem;
}

.secondary-kpis-row {
  grid-template-columns: repeat(4, 1fr) !important;
  margin-bottom: 1.25rem;
}

.kpi-grid {
  display: grid;
  gap: 1.25rem;
}

.kpi-card {
  background: white;
  border-radius: 16px;
  border: 1px solid #f1f5f9;
  padding: 1.25rem 1.5rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 1px 3px rgba(0,0,0,0.02);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.kpi-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 20px -8px rgba(0,0,0,0.08);
  border-color: #e2e8f0;
}

.kpi-card.highlight-primary {
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
  border-left: 4px solid #3b82f6;
}

.kpi-inner {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.kpi-label {
  font-size: 0.75rem;
  font-weight: 700;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.kpi-value {
  font-size: 1.75rem;
  font-weight: 800;
  color: #1e293b;
  line-height: 1.2;
}

.kpi-desc {
  font-size: 0.8rem;
  color: #94a3b8;
  font-weight: 500;
}

.kpi-icon-circ {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.kpi-icon-circ.blue { background: #eff6ff; color: #3b82f6; }
.kpi-icon-circ.green { background: #f0fdf4; color: #16a34a; }
.kpi-icon-circ.orange { background: #fff7ed; color: #f97316; }
.kpi-icon-circ.red { background: #fef2f2; color: #ef4444; }
.kpi-icon-circ.cyan { background: #ecfeff; color: #0891b2; }
.kpi-icon-circ.purple { background: #f5f3ff; color: #8b5cf6; }

/* Utilities */
.text-blue { color: #2563eb !important; }
.text-green { color: #166534 !important; }
.text-red { color: #dc2626 !important; }
.text-purple { color: #7c3aed !important; }
.text-danger { color: #ef4444 !important; }
.fw-700 { font-weight: 700 !important; }
.fw-600 { font-weight: 600 !important; }

/* CHARTS ROW */
.charts-row { display: grid; grid-template-columns: 1fr 1fr; gap: 1.25rem; }
.chart-card { background: white; border-radius: 14px; border: 1px solid #f1f5f9; padding: 1.25rem 1.5rem; box-shadow: 0 1px 3px rgba(0,0,0,0.03); display: flex; flex-direction: column; }
.chart-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 1.5rem; }
.chart-title { font-size: 0.85rem; font-weight: 700; color: #334155; margin: 0; }
.chart-legend-custom { display: flex; gap: 1rem; }
.legend-item-inline { display: flex; align-items: center; gap: 6px; font-size: 0.725rem; font-weight: 600; color: #64748b; }
.dot { width: 8px; height: 8px; border-radius: 50%; }
.dot.green { background: #10b981; box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.1); }
.dot.orange { background: #f59e0b; box-shadow: 0 0 0 2px rgba(245, 158, 11, 0.1); }

/* Donut Chart */
.donut-container { display: flex; align-items: center; gap: 1.5rem; min-height: 120px; }
.empty-donut { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px; color: #94a3b8; font-size: 0.8rem; font-weight: 600; border: 2px dashed #f1f5f9; border-radius: 12px; padding: 20px; }
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

/* ========== DSO/DPO & FORECAST ========== */
.forecast-badge { font-size: 0.8rem; font-weight: 700; }

.dso-dpo-container { display: flex; flex-direction: column; gap: 1rem; }

.dso-card {
  background: white; border-radius: 14px; border: 1px solid #f1f5f9;
  padding: 1.25rem 1.5rem; box-shadow: 0 1px 3px rgba(0,0,0,0.02);
}
.dso-header { display: flex; align-items: baseline; gap: 0.75rem; margin-bottom: 0.5rem; }
.dso-label { font-size: 0.85rem; font-weight: 800; color: #1e293b; letter-spacing: 0.02em; }
.dso-sublabel { font-size: 0.7rem; color: #94a3b8; font-weight: 500; }
.dso-value { display: flex; align-items: baseline; gap: 0.5rem; margin-bottom: 0.75rem; }
.dso-number { font-size: 2rem; font-weight: 800; color: #1e293b; }
.dso-unit { font-size: 0.85rem; color: #64748b; font-weight: 600; }
.dso-bar { height: 8px; background: #f1f5f9; border-radius: 4px; overflow: hidden; margin-bottom: 0.5rem; }
.dso-fill { height: 100%; border-radius: 4px; transition: width 0.8s ease; }
.dso-fill.good { background: linear-gradient(90deg, #10b981, #34d399); }
.dso-fill.warn { background: linear-gradient(90deg, #f59e0b, #fbbf24); }
.dso-fill.danger { background: linear-gradient(90deg, #ef4444, #f87171); }
.dso-fill.dpo { background: linear-gradient(90deg, #6366f1, #818cf8); }
.dso-hint { font-size: 0.75rem; color: #94a3b8; font-style: italic; }

/* Dépenses par catégorie */
.depenses-card {
  background: white; border-radius: 14px; border: 1px solid #f1f5f9;
  padding: 1.25rem 1.5rem; box-shadow: 0 1px 3px rgba(0,0,0,0.02);
}
.dep-bars { display: flex; flex-direction: column; gap: 0.5rem; }
.dep-row { display: flex; align-items: center; gap: 0.75rem; }
.dep-cat { width: 100px; font-size: 0.75rem; font-weight: 600; color: #475569; text-overflow: ellipsis; overflow: hidden; white-space: nowrap; flex-shrink: 0; }
.dep-track { flex: 1; height: 10px; background: #f1f5f9; border-radius: 5px; overflow: hidden; }
.dep-fill { height: 100%; background: linear-gradient(90deg, #3b82f6, #60a5fa); border-radius: 5px; transition: width 0.6s ease; min-width: 4px; }
.dep-amount { font-size: 0.75rem; font-weight: 700; color: #1e293b; width: 90px; text-align: right; flex-shrink: 0; }

/* ========== RESPONSIVE ========== */
@media (max-width: 1200px) {
  .dashboard-header-row { flex-direction: column; }
  .activities-card { min-width: 0; }
  .secondary-kpis-row { grid-template-columns: repeat(2, 1fr) !important; }
}

@media (max-width: 1024px) {
  .charts-row { grid-template-columns: 1fr; }
  .donut-container { flex-direction: column; align-items: center; }
  .donut-chart { width: 100px; height: 100px; }
}

@media (max-width: 768px) {
  .admin-dashboard { gap: 1rem; }
  .main-kpis-wrapper { grid-template-columns: 1fr; }
  .secondary-kpis-row { grid-template-columns: 1fr !important; }
  .kpi-card { padding: 1rem; }
  .kpi-value { font-size: 1.5rem; }
  .charts-row { grid-template-columns: 1fr; gap: 0.75rem; }
  .chart-card { padding: 1rem; }
  
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
  .kpi-icon-circ { width: 36px; height: 36px; border-radius: 50%; }
  .donut-chart { width: 80px; height: 80px; }
  .user-avatar { width: 28px; height: 28px; font-size: 0.7rem; }
  .pag-btn { width: 28px; height: 28px; font-size: 0.75rem; }
}
/* DARK MODE OVERRIDES */
body.dark-mode .kpi-card,
body.dark-mode .activities-card,
body.dark-mode .chart-card,
body.dark-mode .feature-card,
body.dark-mode .dso-card,
body.dark-mode .depenses-card {
  background: #151b2d;
  border-color: #1e293b;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.3);
}

body.dark-mode .kpi-label { color: #94a3b8; }
body.dark-mode .kpi-value { color: #ffffff; }
body.dark-mode .kpi-desc { color: #64748b; }
body.dark-mode .activities-title { color: #ffffff; }
body.dark-mode .timeline-content h4 { color: #f1f5f9; }
body.dark-mode .timeline-content p { color: #94a3b8; }
body.dark-mode .timeline-meta .user { color: #cbd5e1; }
body.dark-mode .chart-title { color: #ffffff; }
body.dark-mode .legend-label { color: #94a3b8; }
body.dark-mode .legend-count { color: #f1f5f9; }
body.dark-mode .dso-label { color: #f1f5f9; }
body.dark-mode .dso-number { color: #ffffff; }
body.dark-mode .dso-bar { background: #0b0f1a; }
body.dark-mode .dep-cat { color: #94a3b8; }
body.dark-mode .dep-track { background: #0b0f1a; }
body.dark-mode .dep-amount { color: #f1f5f9; }
body.dark-mode .data-table th { background: #0b0f1a; color: #94a3b8; border-color: #1e293b; }
body.dark-mode .data-table td { border-color: #1e293b; color: #cbd5e1; }
body.dark-mode .user-info strong { color: #f1f5f9; }
body.dark-mode .input-with-icon input { background: #0b0f1a; border-color: #1e293b; color: #f1f5f9; }
body.dark-mode .role-badge { background: #1e293b; color: #94a3b8; }
</style>

