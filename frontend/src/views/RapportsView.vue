<script setup>
import { ref, computed, onMounted } from 'vue'
import { useLangStore } from '../stores/lang.store'
import MainLayout from '../components/MainLayout.vue'
import api from '../services/api'
import { useCompteStore } from '../stores/compte.store'

const compteStore = useCompteStore()

const selectedMonth = ref(new Date().getMonth() + 1)
const selectedYear = ref(new Date().getFullYear())
const loading = ref(false)
const bilanData = ref(null)
const error = ref(null)

const months = [
  { val: 1, label: 'Janvier' }, { val: 2, label: 'Février' }, { val: 3, label: 'Mars' },
  { val: 4, label: 'Avril' }, { val: 5, label: 'Mai' }, { val: 6, label: 'Juin' },
  { val: 7, label: 'Juillet' }, { val: 8, label: 'Août' }, { val: 9, label: 'Septembre' },
  { val: 10, label: 'Octobre' }, { val: 11, label: 'Novembre' }, { val: 12, label: 'Décembre' }
]

const years = computed(() => {
  const curr = new Date().getFullYear()
  return [curr - 2, curr - 1, curr]
})

onMounted(() => {
  compteStore.fetchComptes()
})

const selectedPeriodLabel = computed(() => {
  const m = months.find(m => m.val === selectedMonth.value)
  return `${m?.label || ''} ${selectedYear.value}`
})

const fetchBilan = async () => {
  loading.value = true
  error.value = null
  try {
    const res = await api.get('/rapports/bilan-mensuel', {
      params: { mois: selectedMonth.value, annee: selectedYear.value }
    })
    bilanData.value = res.data
  } catch (err) {
    error.value = 'Erreur lors de la récupération du bilan.'
    console.error(err)
  } finally {
    loading.value = false
  }
}

const downloadPdf = async () => {
  try {
    const res = await api.get('/rapports/bilan-mensuel/pdf', {
      params: { mois: selectedMonth.value, annee: selectedYear.value },
      responseType: 'blob'
    })
    const url = window.URL.createObjectURL(new Blob([res.data], { type: 'application/pdf' }))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `bilan_${selectedMonth.value}_${selectedYear.value}.pdf`)
    document.body.appendChild(link)
    link.click()
    link.remove()
  } catch (err) {
    console.error("Erreur PDF:", err)
    error.value = 'Erreur lors de la génération du PDF.'
  }
}

const formatCurrency = (val) => {
  if (!val) return '0'
  return new Intl.NumberFormat('fr-FR').format(val)
}

const resultatNet = computed(() => {
  if (!bilanData.value) return 0
  return (bilanData.value.totalCA || 0) - (bilanData.value.totalDepenses || 0)
})

const resultatClass = computed(() => resultatNet.value >= 0 ? 'positive' : 'negative')

const langStore = useLangStore()
const t = computed(() => langStore.t)
</script>

<template>
  <MainLayout>
    <template #title>{{ t("rapports.titre") }}</template>
    <template #subtitle>Génération de bilans périodiques et analyses financières</template>

    <div class="rapports-container">
      <!-- Header -->
      <div class="rapports-header">
        <div class="period-selector">
          <div class="select-group">
            <label>Mois</label>
            <select v-model="selectedMonth">
              <option v-for="m in months" :key="m.val" :value="m.val">{{ m.label }}</option>
            </select>
          </div>
          <div class="select-group">
            <label>Année</label>
            <select v-model="selectedYear">
              <option v-for="y in years" :key="y" :value="y">{{ y }}</option>
            </select>
          </div>
          <button @click="fetchBilan" class="btn-generate" :disabled="loading">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
            {{ loading ? 'Chargement...' : 'Générer le bilan' }}
          </button>
        </div>
      </div>

      <div v-if="error" class="error-banner">{{ error }}</div>

      <!-- Contenu du bilan -->
      <div v-if="bilanData" class="bilan-content fade-in">
        <!-- Titre du bilan -->
        <div class="bilan-title-bar">
          <div>
            <h2>Bilan Mensuel — {{ selectedPeriodLabel }}</h2>
            <p class="bilan-subtitle">Synthèse financière de la période</p>
          </div>
          <button @click="downloadPdf" class="btn-pdf">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
            Télécharger PDF
          </button>
        </div>

        <!-- KPIs du bilan -->
        <div class="bilan-kpis">
          <div class="bilan-kpi ca">
            <div class="bk-label">Chiffre d'Affaires</div>
            <div class="bk-value">{{ formatCurrency(bilanData.totalCA) }} <span>FCFA</span></div>
          </div>
          <div class="bilan-kpi depenses">
            <div class="bk-label">Total Dépenses</div>
            <div class="bk-value">{{ formatCurrency(bilanData.totalDepenses) }} <span>FCFA</span></div>
          </div>
          <div class="bilan-kpi resultat" :class="resultatClass">
            <div class="bk-label">Résultat Net</div>
            <div class="bk-value">{{ resultatNet >= 0 ? '+' : '' }}{{ formatCurrency(resultatNet) }} <span>FCFA</span></div>
          </div>
          <div class="bilan-kpi operations">
            <div class="bk-label">Opérations</div>
            <div class="bk-value">{{ bilanData.nbEncaissements + bilanData.nbDecaissements }}</div>
            <div class="bk-detail">{{ bilanData.nbEncaissements }} enc. / {{ bilanData.nbDecaissements }} déc.</div>
          </div>
        </div>

        <!-- Top 10 Clients + Répartition Dépenses -->
        <div class="bilan-details-row">
          <!-- Top Clients -->
          <div class="bilan-card">
            <h3>Top 10 Clients</h3>
            <div class="top-list">
              <div v-for="(client, idx) in bilanData.topClients" :key="idx" class="top-item">
                <span class="top-rank">#{{ idx + 1 }}</span>
                <span class="top-name">{{ client.nom }}</span>
                <span class="top-amount">{{ formatCurrency(client.total) }} FCFA</span>
              </div>
              <div v-if="!bilanData.topClients?.length" class="empty-hint">Aucun encaissement client ce mois.</div>
            </div>
          </div>

          <!-- Répartition -->
          <div class="bilan-card">
            <h3>Répartition des Dépenses</h3>
            <div class="dep-chart-container">
              <div v-for="(val, cat) in bilanData.repartitionDepenses" :key="cat" class="dep-row-bilan">
                <span class="dep-cat-bilan">{{ cat }}</span>
                <div class="dep-track-bilan">
                  <div class="dep-fill-bilan" :style="{ width: depPct(val, bilanData.repartitionDepenses) + '%' }"></div>
                </div>
                <span class="dep-val-bilan">{{ formatCurrency(val) }}</span>
              </div>
              <div v-if="!Object.keys(bilanData.repartitionDepenses || {}).length" class="empty-hint">Aucune dépense ce mois.</div>
            </div>
          </div>
        </div>
      </div>

      <!-- État initial -->
      <div v-else-if="!loading" class="empty-state">
        <div class="empty-icon">📊</div>
        <h3>Générez votre bilan mensuel</h3>
        <p>Sélectionnez un mois et une année, puis cliquez sur "Générer le bilan" pour obtenir la synthèse financière complète.</p>
      </div>
    </div>
  </MainLayout>
</template>

<script>
export default {
  methods: {
    depPct(val, rep) {
      const vals = Object.values(rep || {})
      if (!vals.length) return 0
      const max = Math.max(...vals.map(v => Number(v)))
      return max > 0 ? (Number(val) / max * 100) : 0
    }
  }
}

const langStore = useLangStore()
const t = computed(() => langStore.t)
</script>

<style scoped>
.rapports-container { display: flex; flex-direction: column; gap: 1.5rem; }

.rapports-header { background: white; border-radius: 16px; padding: 1.5rem 2rem; border: 1px solid #f1f5f9; box-shadow: 0 1px 3px rgba(0,0,0,0.02); }

.period-selector { display: flex; align-items: flex-end; gap: 1.25rem; flex-wrap: wrap; }

.select-group { display: flex; flex-direction: column; gap: 0.5rem; }
.select-group label { font-size: 0.75rem; font-weight: 700; color: #64748b; text-transform: uppercase; letter-spacing: 0.05em; }
.select-group select {
  padding: 0.6rem 1rem; border: 1.5px solid #e2e8f0; border-radius: 10px;
  font-size: 0.9rem; color: #1e293b; background: white; cursor: pointer; min-width: 160px;
}
.select-group select:focus { border-color: #3b82f6; outline: none; box-shadow: 0 0 0 3px rgba(59,130,246,0.1); }

.btn-generate {
  display: flex; align-items: center; gap: 8px;
  padding: 0.7rem 1.5rem; border: none; border-radius: 10px;
  background: linear-gradient(135deg, #3b82f6, #2563eb); color: white;
  font-weight: 700; font-size: 0.9rem; cursor: pointer;
  box-shadow: 0 4px 6px -1px rgba(59,130,246,0.3); transition: all 0.2s;
}
.btn-generate:hover { transform: translateY(-2px); box-shadow: 0 8px 16px -4px rgba(59,130,246,0.4); }
.btn-generate:disabled { opacity: 0.6; cursor: not-allowed; transform: none; }

.error-banner { background: #fef2f2; border: 1px solid #fecaca; color: #b91c1c; padding: 0.75rem 1.25rem; border-radius: 10px; font-size: 0.85rem; font-weight: 600; }

.bilan-content { animation: slideUp 0.4s ease; }
@keyframes slideUp { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }

.bilan-title-bar {
  display: flex; justify-content: space-between; align-items: center;
  background: white; border-radius: 16px; padding: 1.5rem 2rem;
  border: 1px solid #f1f5f9; box-shadow: 0 1px 3px rgba(0,0,0,0.02);
}
.bilan-title-bar h2 { font-size: 1.25rem; font-weight: 800; color: #1e293b; margin: 0; }
.bilan-subtitle { font-size: 0.85rem; color: #94a3b8; margin-top: 0.25rem; }

.btn-pdf {
  display: flex; align-items: center; gap: 8px;
  padding: 0.6rem 1.25rem; border: 1.5px solid #e2e8f0; border-radius: 10px;
  background: white; color: #dc2626; font-weight: 700; font-size: 0.85rem; cursor: pointer; transition: all 0.2s;
}
.btn-pdf:hover { background: #fef2f2; border-color: #fecaca; }

/* KPIs du bilan */
.bilan-kpis { display: grid; grid-template-columns: repeat(4, 1fr); gap: 1.25rem; }

.bilan-kpi {
  background: white; border-radius: 16px; padding: 1.5rem; position: relative;
  border: 1px solid #f1f5f9; box-shadow: 0 1px 3px rgba(0,0,0,0.02); overflow: hidden;
}
.bilan-kpi.ca { border-left: 4px solid #10b981; }
.bilan-kpi.depenses { border-left: 4px solid #f59e0b; }
.bilan-kpi.resultat.positive { border-left: 4px solid #10b981; }
.bilan-kpi.resultat.negative { border-left: 4px solid #ef4444; }
.bilan-kpi.operations { border-left: 4px solid #6366f1; }

.bk-label { font-size: 0.75rem; font-weight: 700; color: #64748b; text-transform: uppercase; letter-spacing: 0.05em; margin-bottom: 0.5rem; }
.bk-value { font-size: 1.5rem; font-weight: 800; color: #1e293b; }
.bk-value span { font-size: 0.85rem; font-weight: 600; color: #94a3b8; }

.bk-detail { font-size: 0.8rem; color: #94a3b8; margin-top: 0.25rem; }

.resultat.positive .bk-value { color: #059669; }
.resultat.negative .bk-value { color: #dc2626; }

/* Détails */
.bilan-details-row { display: grid; grid-template-columns: 1fr 1fr; gap: 1.25rem; }

.bilan-card {
  background: white; border-radius: 16px; padding: 1.5rem; border: 1px solid #f1f5f9;
  box-shadow: 0 1px 3px rgba(0,0,0,0.02);
}
.bilan-card h3 { font-size: 0.9rem; font-weight: 700; color: #1e293b; margin-bottom: 1rem; }

.top-list { display: flex; flex-direction: column; gap: 0.5rem; }
.top-item { display: flex; align-items: center; gap: 0.75rem; padding: 0.5rem 0; border-bottom: 1px solid #f8fafc; }
.top-rank { font-size: 0.75rem; font-weight: 800; color: #94a3b8; width: 28px; }
.top-name { flex: 1; font-size: 0.85rem; font-weight: 600; color: #1e293b; }
.top-amount { font-size: 0.8rem; font-weight: 700; color: #059669; }

.dep-row-bilan { display: flex; align-items: center; gap: 0.75rem; margin-bottom: 0.5rem; }
.dep-cat-bilan { width: 120px; font-size: 0.8rem; font-weight: 600; color: #475569; flex-shrink: 0; }
.dep-track-bilan { flex: 1; height: 10px; background: #f1f5f9; border-radius: 5px; overflow: hidden; }
.dep-fill-bilan { height: 100%; background: linear-gradient(90deg, #f59e0b, #fbbf24); border-radius: 5px; transition: width 0.6s; min-width: 4px; }
.dep-val-bilan { font-size: 0.8rem; font-weight: 700; color: #1e293b; width: 100px; text-align: right; flex-shrink: 0; }

.empty-hint { font-size: 0.85rem; color: #94a3b8; padding: 1rem; text-align: center; }

.empty-state { text-align: center; padding: 5rem 2rem; color: #94a3b8; }
.empty-icon { font-size: 4rem; opacity: 0.3; margin-bottom: 1.5rem; }
.empty-state h3 { font-size: 1.2rem; font-weight: 700; color: #475569; margin-bottom: 0.5rem; }
.empty-state p { font-size: 0.9rem; max-width: 400px; margin: 0 auto; }

@media (max-width: 768px) {
  .bilan-kpis { grid-template-columns: repeat(2, 1fr); }
  .bilan-details-row { grid-template-columns: 1fr; }
  .period-selector { flex-direction: column; align-items: stretch; }
}
</style>
