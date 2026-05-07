<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth.store'
import { useParametrageStore } from '../stores/parametrage.store'
import { useLangStore } from '../stores/lang.store'
import MainLayout from '../components/MainLayout.vue'
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
  ArrowDownTrayIcon,
  CheckCircleIcon,
  DocumentPlusIcon,
  PlusIcon,
  UsersIcon,
  ClockIcon,
  ArrowPathIcon,
  CheckIcon,
  WalletIcon,
  ChevronRightIcon
} from '@heroicons/vue/24/outline'

ChartJS.register(Title, Tooltip, Legend, LineElement, CategoryScale, LinearScale, PointElement, Filler)
import api from '../services/api'

const authStore = useAuthStore()
const parametrageStore = useParametrageStore()
const langStore = useLangStore()
const t = computed(() => langStore.t)

const selectedCurrency = computed(() => {
  const param = parametrageStore.parametres?.find(p => p.cle === 'DEVISE_BASE_CODE')
  return param ? param.valeur : 'XAF'
})
const usdRate = 600

const formatCurrency = (val) => {
  if (val === undefined || val === null) return '0'
  if (selectedCurrency.value === 'USD') {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(val / usdRate)
  }
  return new Intl.NumberFormat('fr-FR').format(val) + ' XAF'
}

const formatCurrencyPlain = (val) => {
  if (val === undefined || val === null) return '0'
  if (selectedCurrency.value === 'USD') {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(val / usdRate)
  }
  return new Intl.NumberFormat('fr-FR').format(val)
}

const router = useRouter()

const kpis = ref({
  soldeTotalCaisses: 0,
  soldeTotalBanques: 0,
  decaissementsEnAttente: 0,
  decaissementsEnAttenteRF: 0,
  decaissementsEnAttentePDG: 0,
  totalCreancesClients: 0,
  totalDettesFournisseurs: 0,
  repartitionDecaissementsParCategorie: {},
  activitesRecentes: [],
  dernierMouvementCaisse: 0,
  dernierMouvementBanque: 0,
  derniereCreanceClient: 0,
  derniereDetteFournisseur: 0,
  evolutionMensuelle: [],
  topFournisseurs: [],
  burnRateMensuel: 0,
  seuilApprobationActuel: 500000,
  decaissementsAExecuter: 0,
  montantTotalAExecuter: 0,
  encaissementsDuJour: 0,
  decaissementsExecutesDuJour: 0,
  operationsDuJour: 0
})
const loading = ref(true)
const forecastPeriod = ref(30)

const fetchKpis = async () => {
  loading.value = true
  const start = Date.now()
  try {
    const response = await api.get(`/tableau-bord/kpis?forecastDays=${forecastPeriod.value}`)
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

import { onUnmounted } from 'vue'

let refreshInterval = null

onMounted(() => {
  parametrageStore.fetchParametres()
  fetchKpis()
  // Refresh every 30 seconds
  refreshInterval = setInterval(fetchKpis, 30000)
})

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

const isRF = computed(() => authStore.userRole === 'RESPONSABLE_FINANCIER')
const isAdmin = computed(() => authStore.userRole === 'ADMINISTRATEUR')
const isComptable = computed(() => authStore.userRole === 'COMPTABLE')
const isPDG = computed(() => authStore.userRole === 'PDG')
const isCaissier = computed(() => authStore.userRole === 'CAISSIER')

const categoryLabels = {
  'PAIEMENT_FOURNISSEUR': 'Fournisseurs',
  'SALAIRES': 'Salaires',
  'FRAIS_FONCTIONNEMENT': 'Frais Fonctionnement',
  'MISSION_DEPLACEMENT': 'Missions',
  'ACHAT_MATERIEL': 'Matériel',
  'AUTRE': 'Autres'
}

const sortedCategories = computed(() => {
  if (!kpis.value.repartitionDecaissementsParCategorie) return []
  return Object.entries(kpis.value.repartitionDecaissementsParCategorie)
    .map(([key, value]) => ({
      key,
      label: categoryLabels[key] || key,
      value
    }))
    .sort((a, b) => b.value - a.value)
})

const totalBudget = computed(() => {
  return sortedCategories.value.reduce((acc, cat) => acc + cat.value, 0)
})

const totalTresorerie = computed(() => {
  return (kpis.value.soldeTotalCaisses || 0) + (kpis.value.soldeTotalBanques || 0)
})

// === PRÉVISION TRÉSORERIE 30j ===
const forecastTrend = computed(() => {
  const prev = kpis.value.soldePrevisionnel30j
  const actuel = totalTresorerie.value
  if (!prev || !actuel) return 0
  return prev - actuel
})

const runwayMois = computed(() => {
  if (!kpis.value.burnRateMensuel || kpis.value.burnRateMensuel <= 0) return '∞'
  return (totalTresorerie.value / kpis.value.burnRateMensuel).toFixed(1)
})

// === GRAPHIQUE ÉVOLUTION TRÉSORERIE ===
const evolutionChartData = computed(() => {
  const data = [...(kpis.value.evolutionMensuelle || [])].reverse() // On affiche chronologiquement
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
      backgroundColor: 'rgba(30, 41, 59, 0.9)',
      titleColor: '#fff',
      bodyColor: '#fff',
      padding: 12,
      cornerRadius: 8,
      callbacks: {
        label: function(context) {
          let label = context.dataset.label || '';
          if (label) label += ': ';
          if (context.parsed.y !== null) {
            const val = selectedCurrency.value === 'USD' ? context.parsed.y / usdRate : context.parsed.y;
          label += selectedCurrency.value === 'USD' ? new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(val) : new Intl.NumberFormat('fr-FR').format(val) + ' FCFA';
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
        color: 'rgba(226, 232, 240, 0.5)',
        drawBorder: false
      },
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
      grid: {
        display: false
      },
      ticks: {
        color: '#94a3b8',
        font: { size: 11, weight: '600' }
      }
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
    <!-- ACTIONS RAPIDES -->
    <div class="dashboard-section">
      <h3 class="section-title">{{  t("dashboard.actionsRapides")  }}</h3>
      <div class="quick-actions-grid">
        <router-link v-if="isAdmin || isComptable || authStore.userRole === 'CAISSIER'" to="/encaissements" class="action-card">
          <div class="action-icon light-blue">
            <BanknotesIcon class="w-5 h-5" />
          </div>
          <h4>{{  t("dashboard.nouvelEncaissement")  }}</h4>
        </router-link>

        <router-link v-if="isCaissier" to="/decaissements" class="action-card highlight">
          <div class="action-icon light-orange">
            <CheckCircleIcon class="w-5 h-5" />
          </div>
          <h4>{{  t("dashboard.executerPaiement")  }}</h4>
        </router-link>

        <router-link v-if="isAdmin || isRF || isPDG" to="/decaissements" class="action-card highlight" :class="{ 'pdg-primary': isPDG }">
          <div class="action-icon light-orange">
            <CheckCircleIcon class="w-5 h-5" />
          </div>
          <h4>{{  isPDG ? t('dashboard.signerDecaissements') : t('dashboard.validerDemandes')  }}</h4>
        </router-link>
        
        <router-link v-if="isAdmin || isComptable" to="/factures?create=VENTE" class="action-card">
          <div class="action-icon light-indigo">
            <DocumentPlusIcon class="w-5 h-5" />
          </div>
          <h4>{{  t("dashboard.nouvelleFacture")  }}</h4>
        </router-link>

        <router-link v-if="isAdmin || isComptable" to="/decaissements" class="action-card">
          <div class="action-icon light-blue">
            <PlusIcon class="w-5 h-5" />
          </div>
          <h4>{{  t("dashboard.saisirDecaissement")  }}</h4>
        </router-link>

        <router-link to="/tiers" class="action-card">
          <div class="action-icon light-indigo">
            <UsersIcon class="w-5 h-5" />
          </div>
          <h4>{{  t("dashboard.consulterTiers")  }}</h4>
        </router-link>
      </div>
    </div>

    <!-- MAIN GRID SPLIT -->
    <div class="main-dashboard-grid">
      <!-- LEFTSIDE COL -->
      <div class="left-col">
        <!-- POSTE DE CAISSE (CAISSIER UNIQUEMENT) -->
        <div class="dashboard-section" v-if="isCaissier">
          <h3 class="section-title">{{  t("dashboard.posteCaisse")  }}</h3>
          <div class="kpi-grid">
            <div class="kpi-card tresorerie-globale-card">
              <span class="kpi-label">{{  t("dashboard.soldeDeCaisse")  }}</span>
              <div class="kpi-body">
                <span class="kpi-value tresorerie-value">{{  formatCurrencyPlain(kpis.soldeTotalCaisses)  }}<span class="currency light">{{  selectedCurrency === 'XAF' ? 'XAF' : ''  }}</span></span>
              </div>
            </div>

            <div class="kpi-card caissier-highlight">
              <span class="kpi-label">{{  t("dashboard.paiementsAExecuter")  }}</span>
              <div class="kpi-body">
                <span class="kpi-value" :class="{ 'urgent-gold': kpis.decaissementsAExecuter > 0 }">{{  kpis.decaissementsAExecuter  }}</span>
                <span class="kpi-trend attention" v-if="kpis.decaissementsAExecuter > 0">{{  formatCurrency(kpis.montantTotalAExecuter)  }}</span>
                <span class="kpi-trend" v-else>{{  t("dashboard.aucunDossier")  }}</span>
              </div>
              <router-link to="/decaissements" class="kpi-action-link" v-if="kpis.decaissementsAExecuter > 0">Traiter maintenant</router-link>
            </div>

            <div class="kpi-card">
              <span class="kpi-label">{{  t("dashboard.encaisseAujourdhui")  }}</span>
              <div class="kpi-body">
                <span class="kpi-value success">{{  formatCurrencyPlain(kpis.encaissementsDuJour)  }}<span class="currency">{{  selectedCurrency === 'XAF' ? 'XAF' : ''  }}</span></span>
              </div>
            </div>

            <div class="kpi-card">
              <span class="kpi-label">{{  t("dashboard.decaisseAujourdhui")  }}</span>
              <div class="kpi-body">
                <span class="kpi-value danger">{{  formatCurrencyPlain(kpis.decaissementsExecutesDuJour)  }}<span class="currency">{{  selectedCurrency === 'XAF' ? 'XAF' : ''  }}</span></span>
              </div>
            </div>

            <div class="kpi-card">
              <span class="kpi-label">{{  t("dashboard.operationsDuJour")  }}</span>
              <div class="kpi-body">
                <span class="kpi-value">{{  kpis.operationsDuJour  }}</span>
                <span class="kpi-trend">{{  t("dashboard.transactionsTraitees")  }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- ÉTAT FINANCIER GLOBAL (non-CAISSIER) -->
        <div class="dashboard-section" v-if="!isCaissier">
          <h3 class="section-title">{{  t("dashboard.etatFinancier")  }}</h3>
          <div class="kpi-grid">
            <!-- Widget Trésorerie Globale (PDG Uniquement) -->
            <div class="kpi-card tresorerie-globale-card" v-if="isPDG">
              <span class="kpi-label">{{  t("dashboard.tresorerieGlobale")  }}</span>
              <div class="kpi-body">
                <span class="kpi-value gold">{{  formatCurrencyPlain(totalTresorerie)  }}<span class="currency">{{  selectedCurrency === 'XAF' ? 'XAF' : ''  }}</span></span>
                <div class="tresorerie-split">
                  <span class="split-item">{{  t("dashboard.caisse")  }}: {{  kpis.soldeTotalCaisses?.toLocaleString()  }}</span>
                  <span class="split-item">{{  t("dashboard.banque")  }}: {{  kpis.soldeTotalBanques?.toLocaleString()  }}</span>
                </div>
              </div>
            </div>

            <div class="kpi-card" v-if="!isPDG">
              <span class="kpi-label">{{  t("dashboard.soldeCaisses")  }}</span>
              <div class="kpi-body">
                <span class="kpi-value">{{  formatCurrencyPlain(kpis.soldeTotalCaisses)  }}<span class="currency">{{  selectedCurrency === 'XAF' ? 'XAF' : ''  }}</span></span>
                <span v-if="kpis.dernierMouvementCaisse" class="kpi-trend" :class="kpis.dernierMouvementCaisse >= 0 ? 'positive' : 'negative'">
                  {{ kpis.dernierMouvementCaisse >= 0 ? '+' : '' }}{{ formatCurrency(kpis.dernierMouvementCaisse) }}
                </span>
              </div>
            </div>

            <div class="kpi-card" v-if="!isPDG">
              <span class="kpi-label">{{  t("dashboard.soldeBanques")  }}</span>
              <div class="kpi-body">
                <span class="kpi-value">{{  formatCurrencyPlain(kpis.soldeTotalBanques)  }}<span class="currency">{{  selectedCurrency === 'XAF' ? 'XAF' : ''  }}</span></span>
                <span v-if="kpis.dernierMouvementBanque" class="kpi-trend" :class="kpis.dernierMouvementBanque >= 0 ? 'positive' : 'negative'">
                  {{ kpis.dernierMouvementBanque >= 0 ? '+' : '' }}{{ formatCurrency(kpis.dernierMouvementBanque) }}
                </span>
              </div>
            </div>

            <div class="kpi-card">
              <span class="kpi-label">{{  t("dashboard.creancesClients")  }}</span>
              <div class="kpi-body">
                <span class="kpi-value success">{{  formatCurrencyPlain(kpis.totalCreancesClients)  }}<span class="currency">{{  selectedCurrency === 'XAF' ? 'XAF' : ''  }}</span></span>
                <span v-if="kpis.derniereCreanceClient" class="kpi-trend" :class="kpis.derniereCreanceClient >= 0 ? 'positive' : 'negative'">
                  {{ kpis.derniereCreanceClient >= 0 ? '+' : '' }}{{ formatCurrency(kpis.derniereCreanceClient) }}
                </span>
              </div>
            </div>

            <div class="kpi-card">
              <span class="kpi-label">{{  t("dashboard.dettesFournisseurs")  }}</span>
              <div class="kpi-body">
                <span class="kpi-value danger">{{  formatCurrencyPlain(kpis.totalDettesFournisseurs)  }}<span class="currency">{{  selectedCurrency === 'XAF' ? 'XAF' : ''  }}</span></span>
                <span v-if="kpis.derniereDetteFournisseur" class="kpi-trend" :class="kpis.derniereDetteFournisseur >= 0 ? 'negative' : 'positive'">
                   {{ kpis.derniereDetteFournisseur > 0 ? '+' : '' }}{{ formatCurrency(kpis.derniereDetteFournisseur) }}
                </span>
              </div>
            </div>

            <div class="kpi-card highlight-card" v-if="isRF">
              <span class="kpi-label">Pipeline de Validation</span>
              <div class="pipeline-display">
                <div class="pipeline-step">
                  <span class="step-count">{{  kpis.decaissementsEnAttenteRF  }}</span>
                  <span class="step-label">{{  t("dashboard.attenteRF")  }}</span>
                </div>
                <div class="pipeline-arrow">
                  <ChevronRightIcon class="w-4 h-4" />
                </div>
                <div class="pipeline-step">
                  <span class="step-count">{{  kpis.decaissementsEnAttentePDG  }}</span>
                  <span class="step-label">{{  t("dashboard.attentePDG")  }}</span>
                </div>
              </div>
              <router-link to="/decaissements" class="kpi-action-link">Gérer le flux</router-link>
            </div>

            <div class="kpi-card highlight-card pdg-alert-card" v-if="isPDG">
              <span class="kpi-label">{{  t("dashboard.approbationPDG")  }}</span>
              <div class="kpi-body">
                <span class="kpi-value" :class="{ 'urgent-gold': kpis.decaissementsEnAttentePDG > 0 }">{{  kpis.decaissementsEnAttentePDG  }}</span>
                <span class="kpi-trend attention" v-if="kpis.decaissementsEnAttentePDG > 0">{{  t("dashboard.signatureAttendue")  }}</span>
              </div>
              <router-link to="/decaissements" class="kpi-action-link">Ouvrir le parapheur</router-link>
            </div>

            <div class="kpi-card" v-if="!isRF && !isPDG">
              <span class="kpi-label">{{  t("dashboard.decaissementsEnAttente")  }}</span>
              <div class="kpi-body">
                <span class="kpi-value" :class="{ 'warning': kpis.decaissementsEnAttente > 0 }">{{  kpis.decaissementsEnAttente  }}</span>
                <span class="kpi-trend attention" v-if="kpis.decaissementsEnAttente > 0">{{  t("dashboard.actionRequise")  }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- PILOTAGE STRATÉGIQUE (PDG UNIQUEMENT) -->
        <div class="dashboard-section" v-if="isPDG">
          <h3 class="section-title">{{  t("dashboard.pilotageStrategique")  }}</h3>
          <div class="strategic-grid">
            <!-- Graphique de Flux -->
            <div class="strategic-card flux-chart-card">
              <div class="card-header">
                <div style="display: flex; justify-content: space-between; align-items: center;">
                  <div>
                    <h4>Flux de Trésorerie Mensuel</h4>
                    <span class="card-subtitle">Évolution des encaissements et décaissements sur 3 {{  t("dashboard.mois")  }}</span>
                  </div>
                  <div class="chart-legend-custom" style="display: flex; gap: 1rem; font-size: 0.8rem; font-weight: 600;">
                    <div style="display: flex; align-items: center; gap: 6px;">
                      <span style="width: 10px; height: 10px; border-radius: 50%; background: #10b981;"></span> Encaissements
                    </div>
                    <div style="display: flex; align-items: center; gap: 6px;">
                      <span style="width: 10px; height: 10px; border-radius: 50%; background: #f59e0b;"></span> Décaissements
                    </div>
                  </div>
                </div>
              </div>
              <div style="height: 250px; position: relative;">
                <Line :data="evolutionChartData" :options="evolutionChartOptions" />
              </div>
            </div>

            <!-- Top Fournisseurs & Burn Rate -->
            <div class="strategic-subgrid">
              <div class="strategic-card top-suppliers-card">
                <h4>Top 5 Fournisseurs</h4>
                <div class="suppliers-list">
                  <div v-for="sup in kpis.topFournisseurs" :key="sup.nom" class="supplier-row">
                    <div class="sup-info">
                      <span class="sup-name">{{  sup.nom  }}</span>
                      <span class="sup-amount">{{  formatCurrency(sup.total)  }}</span>
                    </div>
                    <div class="sup-progress">
                      <div class="sup-bar" :style="{ width: (sup.total / (kpis.topFournisseurs[0]?.total || 1) * 100) + '%' }"></div>
                    </div>
                  </div>
                  <div v-if="!kpis.topFournisseurs?.length" class="empty-mini">Aucune donnée</div>
                </div>
              </div>

              <div class="strategic-card burn-rate-card">
                <div class="burn-header">
                  <h4>Paramètres de Gestion</h4>
                </div>
                <div class="burn-body">
                  <div class="burn-item">
                    <span class="burn-label">{{  t("dashboard.depensesMoyennes")  }}</span>
                    <span class="burn-value">{{  kpis.burnRateMensuel?.toLocaleString()  }} <span class="unit">{{  selectedCurrency === 'XAF' ? 'XAF' : 'USD'  }} / {{  t("dashboard.mois")  }}</span></span>
                  </div>
                  <div class="divider"></div>
                  <div class="burn-item runway-item">
                    <div class="threshold-info">
                      <span class="burn-label">{{  t("dashboard.runway")  }}</span>
                      <span class="threshold-value">{{  runwayMois  }} <span class="unit">mois</span></span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- RÉPARTITION DU BUDGET (Uniquement pour RF/ADMIN) -->
        <div class="dashboard-section" v-if="isRF && sortedCategories.length > 0">
          <h3 class="section-title">{{  t("dashboard.repartitionBudget")  }}</h3>
          <div class="budget-chart-container">
            <div v-for="cat in sortedCategories" :key="cat.key" class="budget-row">
              <div class="budget-row-header">
                <span class="cat-label">{{  cat.label  }}</span>
                <span class="cat-amount">{{  formatCurrency(cat.value)  }}</span>
              </div>
              <div class="budget-progress-bg">
                <div class="budget-progress-fill" :style="{ width: (cat.value / totalBudget * 100) + '%' }"></div>
              </div>
            </div>
          </div>
        </div>

        <!-- INDICATEURS STRATÉGIQUES (PDG + RF) -->
        <div class="dashboard-section" v-if="isPDG || isRF">
          <h3 class="section-title">{{  t("dashboard.indicateursStrategiques")  }}</h3>
          <div class="strategic-indicators-grid">
            <!-- Prévision Trésorerie 30j -->
            <div class="strategic-ind-card forecast-card">
              <div class="ind-header" style="align-items: flex-start; flex-direction: column; gap: 0.5rem;">
                <div style="display: flex; justify-content: space-between; width: 100%;">
                  <h4>Prévision Trésorerie</h4>
                  <div class="period-selector">
                    <select v-model="forecastPeriod" @change="fetchKpis" class="forecast-select">
                      <option :value="30">30 jours</option>
                      <option :value="60">60 jours</option>
                      <option :value="90">90 jours</option>
                    </select>
                  </div>
                </div>
                <span class="ind-badge" :class="forecastTrend >= 0 ? 'badge-green' : 'badge-red'">
                  {{ forecastTrend >= 0 ? '↑' : '↓' }} {{ formatCurrency(Math.abs(forecastTrend)) }}
                </span>
              </div>
              <div class="forecast-points">
                <div v-for="pt in (kpis.pointsPrevisionnels || [])" :key="pt.date" class="forecast-point">
                  <span class="fp-date">{{  pt.date  }}</span>
                  <span class="fp-value">{{  formatCurrency(pt.solde)  }}</span>
                </div>
                <div v-if="!kpis.pointsPrevisionnels?.length" class="empty-mini">Aucune projection disponible</div>
              </div>
              <div class="forecast-summary" v-if="kpis.soldePrevisionnel30j">
                <strong>{{ t("dashboard.soldeEstime") }} ({{ forecastPeriod }}j) :</strong> {{ formatCurrency(kpis.soldePrevisionnel30j) }}
              </div>
            </div>

            <!-- DSO / DPO -->
            <div class="strategic-ind-card" title="DSO (Days Sales Outstanding) : Délai moyen que mettent vos clients à vous payer. DPO (Days Payables Outstanding) : Délai moyen que vous mettez pour acquitter une facture fournisseur une fois la demande de décaissement initiée.">
              <div class="dso-dpo-pair">
                <div class="dso-block">
                  <div class="dso-head">
                    <span class="dso-title" title="DSO = (Délai d'encaissement moyen des clients)">DSO</span>
                    <span class="dso-sub">{{  t("dashboard.delaiClients")  }}</span>
                  </div>
                  <div class="dso-big">{{  kpis.dso || 0  }} <span>jours</span></div>
                  <div class="dso-bar-mini">
                    <div class="dso-fill-mini" :class="dsoClass" :style="{ width: Math.min(kpis.dso || 0, 90) / 90 * 100 + '%' }"></div>
                  </div>
                  <div class="dso-msg">{{  dsoMessage  }}</div>
                </div>
                <div class="dso-divider"></div>
                <div class="dso-block">
                  <div class="dso-head">
                    <span class="dso-title" title="DPO = (Délai d'exécution moyen des fournisseurs)">DPO</span>
                    <span class="dso-sub">{{  t("dashboard.delaiFournisseurs")  }}</span>
                  </div>
                  <div class="dso-big">{{  kpis.dpo || 0  }} <span>jours</span></div>
                  <div class="dso-bar-mini">
                    <div class="dso-fill-mini dpo-color" :style="{ width: Math.min(kpis.dpo || 0, 90) / 90 * 100 + '%' }"></div>
                  </div>
                  <div class="dso-msg">{{  dpoMessage  }}</div>
                </div>
              </div>
            </div>

            <!-- Répartition dépenses du mois -->
            <div class="strategic-ind-card" v-if="Object.keys(kpis.repartitionDepensesParCategorie || {}).length > 0">
              <h4 style="margin-bottom: 1rem;">Dépenses du mois par catégorie</h4>
              <div v-for="(val, cat) in kpis.repartitionDepensesParCategorie" :key="cat" class="dep-row-db">
                <span class="dep-cat-db">{{  cat  }}</span>
                <div class="dep-track-db">
                  <div class="dep-fill-db" :style="{ width: depPercent(val) + '%' }"></div>
                </div>
                <span class="dep-val-db">{{  formatCurrency(val)  }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- A FAIRE AUJOURD'HUI -->
        <div class="dashboard-section">
          <div class="section-header-row">
            <h3 class="section-title">{{  t("dashboard.aFaireAujourdhui")  }}</h3>
            <a href="#" class="view-all-link">{{  t("dashboard.voirTaches")  }}</a>
          </div>
          
          <div class="table-container">
            <table class="tasks-table">
              <thead>
                <tr>
                  <th>{{  t("dashboard.priorite")  }}</th>
                  <th>{{  t("dashboard.element")  }}</th>
                  <th>{{  t("dashboard.montant")  }}</th>
                  <th>{{  t("dashboard.action")  }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="kpis.decaissementsEnAttenteRF > 0 && isRF">
                  <td><span class="badge badge-urgent">{{  t("dashboard.critique")  }}</span></td>
                  <td>
                    <div class="task-info">
                      <strong>{{  kpis.decaissementsEnAttenteRF  }} dossiers à VOTRE validation</strong>
                      <span>Délai moyen constaté : 2h</span>
                    </div>
                  </td>
                  <td class="task-amount">--</td>
                  <td><router-link to="/decaissements" class="task-action highlight">{{  t("dashboard.valider")  }}</router-link></td>
                </tr>
                <tr v-if="kpis.decaissementsEnAttentePDG > 0 && isPDG">
                  <td><span class="badge badge-urgent">{{  t("dashboard.direction")  }}</span></td>
                  <td>
                    <div class="task-info">
                      <strong>{{  kpis.decaissementsEnAttentePDG  }} dossier(s) en attente de signature</strong>
                      <span>Seuil de validation PDG atteint</span>
                    </div>
                  </td>
                  <td class="task-amount">--</td>
                  <td><router-link to="/decaissements" class="task-action highlight gold-btn">{{  t("dashboard.signer")  }}</router-link></td>
                </tr>
                <tr v-if="kpis.decaissementsEnAttente > 0 && !isRF && !isPDG">
                  <td><span class="badge badge-urgent">{{  t("dashboard.urgent")  }}</span></td>
                  <td>
                    <div class="task-info">
                      <strong>{{  kpis.decaissementsEnAttente  }} décaissement(s) en attente</strong>
                      <span>Plusieurs demandes en attente de traitement</span>
                    </div>
                  </td>
                  <td class="task-amount">--</td>
                  <td><router-link to="/decaissements" class="task-action">{{  t("dashboard.voir")  }}</router-link></td>
                </tr>
                <tr>
                  <td><span class="badge badge-normal">{{  t("dashboard.normale")  }}</span></td>
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
            <h3 class="section-title">{{  t("dashboard.activiteRecente")  }}</h3>
            <button @click="fetchKpis" class="refresh-btn" :class="{ 'spinning': loading }" title="Rafraîchir">
               <ArrowPathIcon class="w-4 h-4" />
            </button>
          </div>
          
          <div class="timeline" v-if="kpis.activitesRecentes?.length > 0">
            <div class="timeline-item" v-for="(act, idx) in kpis.activitesRecentes" :key="idx">
              <div class="timeline-icon" :class="getActivityIconClass(act.type)">
                <CheckIcon v-if="act.type === 'FACTURE'" class="w-3 h-3" />
                <WalletIcon v-else-if="act.type === 'ENCAISSEMENT'" class="w-3 h-3" />
                <ClockIcon v-else class="w-3 h-3" />
              </div>
              <div class="timeline-content">
                <h4>{{  act.action === 'CREATE' ? 'Création' : act.action  }} {{  act.type.toLowerCase()  }}</h4>
                <p>{{  act.message  }}</p>
                <div class="timeline-meta">
                  <span class="user">{{  act.utilisateur  }}</span>
                  <span class="time">{{  formatDateLabel(act.date)  }} • {{  formatTime(act.date)  }}</span>
                </div>
              </div>
            </div>
          </div>
          <div class="empty-activity" v-else>
            <p>{{  t("dashboard.aucuneActivite")  }}</p>
          </div>
        </div>

        <!-- SCORE D'EFFICACITE -->
        <div class="dashboard-section right-panel score-panel">
          <h3 class="section-title">{{  t("dashboard.scoreEfficacite")  }}</h3>
          <div class="score-header">
            <span class="score-label">{{  t("dashboard.vitesseTraitement")  }}</span>
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
              <span>{{  t("dashboard.tauxErreur")  }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
    <!-- MODAL MODIFICATION SEUIL (PDG) -->
    <div v-if="showSeuilModal" class="modal-overlay">
      <div class="modal-content mini-modal">
        <div class="modal-header">
          <h3>Réglage du Seuil</h3>
          <button @click="showSeuilModal = false" class="close-btn">&times;</button>
        </div>
        <div class="modal-body">
          <p class="modal-desc">Définissez le montant au-delà duquel votre signature est requise pour tout décaissement.</p>
          <div class="form-group">
            <label>Seuil d'approbation (XAF)</label>
            <div class="input-with-unit">
              <input type="number" v-model="newSeuil" class="form-input" placeholder="Ex: 1000000" />
              <span class="unit">XAF</span>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button @click="showSeuilModal = false" class="btn-secondary">{{  t("common.annuler")  }}</button>
          <button @click="updateSeuil" class="btn-primary gold-btn" :disabled="updatingSeuil">
            {{  updatingSeuil ? 'Confirmer' : 'Enregistrer'  }}
          </button>
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
  grid-template-columns: repeat(auto-fill, minmax(200px, 260px));
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
.light-orange { background: #fff7ed; color: #f97316; }

.action-card.highlight {
  border: 1px solid #fed7aa;
  background: linear-gradient(to bottom right, #ffffff, #fff7ed);
}

.action-card h4 {
  font-size: 1rem;
  font-weight: 600;
  color: #111827;
  line-height: 1.2;
}

/* KPI HIGHLIGHT & PDG SPECIFIC */
.highlight-card {
  background: linear-gradient(135deg, #ffffff, #f8fafc);
  border: 1px solid #e2e8f0;
}

.pdg-alert-card {
  border-left: 4px solid #f59e0b;
}

.caissier-highlight {
  border-left: 4px solid #f59e0b;
  background: linear-gradient(135deg, #fffbeb, #ffffff);
}

.tresorerie-globale-card {
  background: linear-gradient(135deg, #1e293b, #0f172a);
  color: white;
  border: none;
}

.tresorerie-globale-card .kpi-label { color: #94a3b8; }
.tresorerie-globale-card .currency { color: #64748b; }

.kpi-value.gold {
  color: #fbbf24;
  text-shadow: 0 0 20px rgba(251, 191, 36, 0.2);
}

.urgent-gold {
  color: #f59e0b;
}

.tresorerie-split {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  align-items: flex-end;
}

.split-item {
  font-size: 0.65rem;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.pdg-primary {
  background: linear-gradient(135deg, #fefce8, #fef9c3) !important;
  border: 1px solid #fde047 !important;
}

.gold-btn {
  background: #f59e0b !important;
  color: white !important;
}
.gold-btn:hover {
  background: #d97706 !important;
}

.pipeline-display {
  display: flex;
  align-items: center;
  justify-content: space-around;
  margin: 0.5rem 0;
}

.pipeline-step {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.step-count {
  font-size: 1.5rem;
  font-weight: 700;
  color: #111827;
}

.step-label {
  font-size: 0.65rem;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.025em;
}

.pipeline-arrow {
  color: #cbd5e1;
}

.kpi-action-link {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--c-primary);
  text-decoration: none;
  margin-top: 0.5rem;
  display: inline-block;
}

/* BUDGET CHART */
.budget-chart-container {
  background: white;
  border-radius: 12px;
  padding: 1.5rem;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
  border: 1px solid #f3f4f6;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.budget-row {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.budget-row-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
}

.cat-label {
  font-size: 0.85rem;
  font-weight: 600;
  color: #374151;
}

.cat-amount {
  font-size: 0.8rem;
  font-weight: 700;
  color: #111827;
}

.budget-progress-bg {
  width: 100%;
  height: 8px;
  background: #f1f5f9;
  border-radius: 4px;
  overflow: hidden;
}

.budget-progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #6366f1, #818cf8);
  border-radius: 4px;
  transition: width 0.6s cubic-bezier(0.16, 1, 0.3, 1);
}

.task-action.highlight {
  background: var(--c-primary);
  color: white;
  padding: 0.4rem 0.8rem;
  border-radius: 6px;
  text-decoration: none !important;
}
.task-action.highlight:hover {
  background: #1d4ed8;
}



/* MAIN SPLIT */
.main-dashboard-grid {
  display: grid;
  grid-template-columns: 2.4fr 1.1fr;
  gap: 3rem;
}
.left-col { display: flex; flex-direction: column; }
.right-col { display: flex; flex-direction: column; gap: 2rem; }

/* KPI GRID */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 320px));
  gap: 2rem;
}

.kpi-card {
  background: var(--c-surface);
  border-radius: 12px;
  padding: 1.5rem;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
  border: 1px solid #f3f4f6;
  display: flex;
  flex-direction: column;
}

.kpi-label {
  font-size: 0.8rem;
  font-weight: 500;
  color: #64748b;
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
  color: var(--c-text);
  letter-spacing: -0.025em;
}

.kpi-value.warning { color: #f59e0b; }
.kpi-value.success { color: #10b981; }
.kpi-value.danger { color: #ef4444; }
.currency { font-size: 1rem; color: #4b5563; font-weight: 600; margin-left: 2px; }

.kpi-trend {
  font-size: 0.7rem;
  font-weight: 700;
  margin-top: 2px;
  white-space: nowrap;
}
.kpi-trend.positive { color: #10b981; }
.kpi-trend.negative { color: #ef4444; }
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
  background: var(--c-surface);
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

/* STRATEGIC DASHBOARD (PDG) */
.strategic-grid {
  display: grid;
  grid-template-columns: 1.8fr 1.2fr;
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.strategic-card {
  background: white;
  border-radius: 12px;
  padding: 1.5rem;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
  border: 1px solid #f3f4f6;
  display: flex;
  flex-direction: column;
}

.strategic-card h4 {
  font-size: 0.9rem;
  font-weight: 600;
  color: #111827;
  margin-bottom: 0.5rem;
}

.card-subtitle {
  font-size: 0.75rem;
  color: #6b7280;
  display: block;
  margin-bottom: 1.5rem;
}

/* FLUX CHART */
.flux-chart {
  display: flex;
  justify-content: space-around;
  align-items: flex-end;
  height: 180px;
  padding: 1rem 0;
  border-bottom: 1px solid #f3f4f6;
  margin-bottom: 1rem;
}

.flux-bar-group {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  flex: 1;
}

.bar-pair {
  display: flex;
  align-items: flex-end;
  gap: 4px;
  height: 140px;
}

.bar {
  width: 14px;
  border-radius: 3px 3px 0 0;
  transition: height 1s cubic-bezier(0.16, 1, 0.3, 1);
  cursor: help;
}

.bar-enc { background: #10b981; }
.bar-dec { background: #ef4444; }

.mois-label {
  font-size: 0.7rem;
  font-weight: 600;
  color: #9ca3af;
  text-transform: uppercase;
}

.flux-legend {
  display: flex;
  justify-content: center;
  gap: 2rem;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.75rem;
  font-weight: 600;
  color: #4b5563;
}

.dot { width: 8px; height: 8px; border-radius: 2px; }
.dot.enc { background: #10b981; }
.dot.dec { background: #ef4444; }

/* TOP SUPPLIERS */
.strategic-subgrid {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.suppliers-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin-top: 1rem;
}

.supplier-row {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.sup-info {
  display: flex;
  justify-content: space-between;
  font-size: 0.8rem;
}

.sup-name { font-weight: 500; color: #374151; }
.sup-amount { font-weight: 700; color: #111827; }

.sup-progress {
  width: 100%;
  height: 6px;
  background: #f1f5f9;
  border-radius: 3px;
  overflow: hidden;
}

.sup-bar {
  height: 100%;
  background: #6366f1;
  border-radius: 3px;
  transition: width 0.8s ease;
}

/* BURN RATE & SETTINGS */
.burn-body {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  margin-top: 1rem;
}

.burn-label {
  font-size: 0.7rem;
  font-weight: 600;
  color: #6b7280;
  text-transform: uppercase;
  display: block;
}

.burn-value {
  font-size: 1.25rem;
  font-weight: 700;
  color: #111827;
}

.burn-value .unit { font-size: 0.8rem; color: #9ca3af; }

.divider { height: 1px; background: #f3f4f6; }

.threshold-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.threshold-value {
  font-size: 1.1rem;
  font-weight: 700;
  color: #f59e0b;
}

.btn-setup {
  width: 32px; height: 32px;
  border-radius: 8px;
  background: #fffbeb;
  color: #f59e0b;
  border: 1px solid #fef3c7;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-setup:hover { background: #fef3c7; transform: rotate(30deg); }

/* MODAL STYLES (Enrichis) */
.modal-overlay {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5); backdrop-filter: blur(4px);
  display: flex; align-items: center; justify-content: center;
  z-index: 2000;
}
.modal-content {
  background: white; padding: 2rem; border-radius: 16px; width: 500px;
  box-shadow: 0 20px 25px -5px rgba(0,0,0,0.1), 0 10px 10px -5px rgba(0,0,0,0.04);
}
.mini-modal { width: 400px; }
.modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem; }
.modal-header h3 { font-size: 1.25rem; font-weight: 700; color: #111827; }
.close-btn { background: none; border: none; font-size: 1.5rem; color: #9ca3af; cursor: pointer; }
.modal-desc { font-size: 0.875rem; color: #6b7280; line-height: 1.5; margin-bottom: 1.5rem; }
.input-with-unit { position: relative; display: flex; align-items: center; }
.input-with-unit .unit { position: absolute; right: 1rem; font-size: 0.875rem; font-weight: 600; color: #9ca3af; }
.btn-secondary { background: #f9fafb; border: 1px solid #e5e7eb; padding: 0.6rem 1.25rem; border-radius: 8px; cursor: pointer; font-weight: 600; color: #374151; }
.modal-footer { display: flex; justify-content: flex-end; gap: 1rem; margin-top: 2rem; }

@media (max-width: 1024px) {
  .strategic-grid { grid-template-columns: 1fr; }
  .main-dashboard-grid { grid-template-columns: 1fr; }
}

@media (max-width: 768px) {
  .quick-actions-grid { 
    grid-template-columns: 1fr 1fr; 
    gap: 0.75rem; 
  }
  .action-card { 
    height: auto; 
    padding: 1rem; 
  }
  .action-icon { 
    margin-bottom: 0.5rem; 
  }
  .kpi-grid { 
    grid-template-columns: 1fr; 
    gap: 1rem; 
  }
  
  .kpi-card {
    padding: 1.25rem;
  }
  
  .kpi-value {
    font-size: 1.5rem;
  }
  
  .kpi-label {
    font-size: 0.8rem;
    margin-bottom: 0.5rem;
  }

  .right-panel {
    padding: 1rem;
    max-width: 100%;
    overflow: hidden;
  }

  .table-container {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
    max-width: 100%;
  }

  .tasks-table th, .tasks-table td {
    padding: 0.75rem 0.5rem;
    white-space: normal;
    word-break: break-word;
  }

  .timeline-item {
    word-break: break-word;
    overflow-wrap: break-word;
  }

  .timeline-content {
    min-width: 0;
    max-width: 100%;
  }

  .flux-legend { 
    flex-wrap: wrap; 
    gap: 0.5rem; 
    margin-top: 1rem; 
  }
  
  .budget-chart-container {
    padding: 1rem;
  }
}

/* ========== INDICATEURS STRATÉGIQUES (PDG + RF) ========== */
.strategic-indicators-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.25rem;
}

.strategic-ind-card {
  background: white;
  border-radius: 14px;
  border: 1px solid #f1f5f9;
  padding: 1.5rem;
  box-shadow: 0 1px 3px rgba(0,0,0,0.03);
}

.strategic-ind-card h4 {
  font-size: 0.9rem; font-weight: 700; color: #1e293b; margin: 0;
}

.ind-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
.ind-badge { font-size: 0.75rem; font-weight: 700; padding: 4px 10px; border-radius: 20px; }
.badge-green { background: #d1fae5; color: #065f46; }
.badge-red { background: #fee2e2; color: #991b1b; }

.forecast-points { display: flex; flex-direction: column; gap: 0.5rem; margin-bottom: 1rem; }
.forecast-point { display: flex; justify-content: space-between; padding: 0.5rem 0.75rem; background: #f8fafc; border-radius: 8px; }
.fp-date { font-size: 0.8rem; font-weight: 600; color: #64748b; }
.fp-value { font-size: 0.85rem; font-weight: 700; color: #1e293b; }
.forecast-summary { font-size: 0.85rem; color: #475569; padding: 0.75rem; background: #eff6ff; border-radius: 8px; border: 1px solid #bfdbfe; }

.forecast-select {
  padding: 0.2rem 0.5rem;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
  font-size: 0.8rem;
  background-color: #f8fafc;
  color: #334155;
  cursor: pointer;
  outline: none;
}
.forecast-select:hover {
  border-color: #cbd5e1;
}

/* DSO/DPO pair */
.dso-dpo-pair { display: flex; gap: 1.5rem; }
.dso-block { flex: 1; }
.dso-divider { width: 1px; background: #f1f5f9; }
.dso-head { display: flex; align-items: baseline; gap: 0.5rem; margin-bottom: 0.5rem; }
.dso-title { font-size: 0.85rem; font-weight: 800; color: #1e293b; }
.dso-sub { font-size: 0.7rem; color: #94a3b8; }
.dso-big { font-size: 1.75rem; font-weight: 800; color: #1e293b; margin-bottom: 0.75rem; }
.dso-big span { font-size: 0.8rem; font-weight: 600; color: #64748b; }
.dso-bar-mini { height: 6px; background: #f1f5f9; border-radius: 3px; overflow: hidden; margin-bottom: 0.5rem; }
.dso-fill-mini { height: 100%; border-radius: 3px; transition: width 0.8s ease; }
.dso-fill-mini.good { background: linear-gradient(90deg, #10b981, #34d399); }
.dso-fill-mini.warn { background: linear-gradient(90deg, #f59e0b, #fbbf24); }
.dso-fill-mini.danger { background: linear-gradient(90deg, #ef4444, #f87171); }
.dso-fill-mini.dpo-color { background: linear-gradient(90deg, #6366f1, #818cf8); }
.dso-msg { font-size: 0.7rem; color: #94a3b8; font-style: italic; }

/* Dépenses par catégorie (Dashboard) */
.dep-row-db { display: flex; align-items: center; gap: 0.75rem; margin-bottom: 0.5rem; }
.dep-cat-db { width: 120px; font-size: 0.8rem; font-weight: 600; color: #475569; flex-shrink: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.dep-track-db { flex: 1; height: 8px; background: #f1f5f9; border-radius: 4px; overflow: hidden; }
.dep-fill-db { height: 100%; background: linear-gradient(90deg, #3b82f6, #60a5fa); border-radius: 4px; transition: width 0.6s ease; min-width: 4px; }
.dep-val-db { font-size: 0.75rem; font-weight: 700; color: #1e293b; width: 100px; text-align: right; flex-shrink: 0; }

@media (max-width: 768px) {
  .strategic-indicators-grid { grid-template-columns: 1fr; }
  .dso-dpo-pair { flex-direction: column; }
  .dso-divider { width: 100%; height: 1px; }
}
/* DARK MODE OVERRIDES */
body.dark-mode .section-title { color: #ffffff; }
body.dark-mode .kpi-card,
body.dark-mode .table-container,
body.dark-mode .right-panel,
body.dark-mode .strategic-card,
body.dark-mode .strategic-ind-card {
  background: #151b2d;
  border-color: #1e293b;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.3);
}

body.dark-mode .kpi-label { color: #94a3b8; }
body.dark-mode .kpi-value { color: #ffffff; }
body.dark-mode .tasks-table th { background: #0b0f1a; color: #94a3b8; border-color: #1e293b; }
body.dark-mode .tasks-table td { border-color: #1e293b; }
body.dark-mode .task-info strong { color: #f1f5f9; }
body.dark-mode .task-amount { color: #f1f5f9; }
body.dark-mode .timeline-content h4 { color: #f1f5f9; }
body.dark-mode .timeline-content p { color: #94a3b8; }
body.dark-mode .timeline-meta .user { color: #cbd5e1; }
body.dark-mode .score-label { color: #f1f5f9; }
body.dark-mode .stat-col strong { color: #f1f5f9; }
body.dark-mode .strategic-card h4,
body.dark-mode .strategic-ind-card h4 { color: #f1f5f9; }
body.dark-mode .card-subtitle { color: #64748b; }
body.dark-mode .sup-name { color: #cbd5e1; }
body.dark-mode .sup-amount { color: #f1f5f9; }
body.dark-mode .sup-progress { background: #0b0f1a; }
body.dark-mode .divider { background: #1e293b; }
body.dark-mode .forecast-point { background: #0b0f1a; }
body.dark-mode .fp-value { color: #f1f5f9; }
body.dark-mode .forecast-summary { background: rgba(59, 130, 246, 0.1); border-color: #1e3a8a; color: #93c5fd; }
body.dark-mode .forecast-select { background: #0b0f1a; border-color: #1e293b; color: #cbd5e1; }
body.dark-mode .dso-title { color: #f1f5f9; }
body.dark-mode .dso-big { color: #f1f5f9; }
body.dark-mode .dso-bar-mini { background: #0b0f1a; }
body.dark-mode .dep-cat-db { color: #94a3b8; }
body.dark-mode .dep-track-db { background: #0b0f1a; }
body.dark-mode .dep-val-db { color: #f1f5f9; }
body.dark-mode .action-card { background: #151b2d; border-color: #1e293b; }
body.dark-mode .action-card h4 { color: #f1f5f9; }
body.dark-mode .action-card:hover { border-color: #3b82f6; background: #1e293b; }
</style>
