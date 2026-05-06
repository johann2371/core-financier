<script setup>
import { ref, onMounted, computed } from 'vue'
import { useLangStore } from '../stores/lang.store'
import MainLayout from '../components/MainLayout.vue'
import api from '../services/api'

// === BUDGETS ===
const budgets = ref([])
const loadingBudgets = ref(false)
const showBudgetModal = ref(false)
const budgetForm = ref({ categorie: 'PAIEMENT_FOURNISSEUR', annee: new Date().getFullYear(), mois: new Date().getMonth() + 1, montantPlafond: 0, seuilAlertePourcent: 80 })
const editBudgetId = ref(null)

const categories = ['PAIEMENT_FOURNISSEUR', 'SALAIRES', 'FRAIS_FONCTIONNEMENT', 'MISSION_DEPLACEMENT', 'ACHAT_MATERIEL', 'AUTRE']
const categoryLabels = {
  'PAIEMENT_FOURNISSEUR': 'Fournisseurs',
  'SALAIRES': 'Salaires',
  'FRAIS_FONCTIONNEMENT': 'Frais Fonctionnement',
  'MISSION_DEPLACEMENT': 'Missions & Déplacements',
  'ACHAT_MATERIEL': 'Matériel',
  'AUTRE': 'Autres'
}

const moisOptions = [
  { value: 0, label: 'Annuel (toute l\'année)' },
  { value: 1, label: 'Janvier' },
  { value: 2, label: 'Février' },
  { value: 3, label: 'Mars' },
  { value: 4, label: 'Avril' },
  { value: 5, label: 'Mai' },
  { value: 6, label: 'Juin' },
  { value: 7, label: 'Juillet' },
  { value: 8, label: 'Août' },
  { value: 9, label: 'Septembre' },
  { value: 10, label: 'Octobre' },
  { value: 11, label: 'Novembre' },
  { value: 12, label: 'Décembre' }
]

const getMoisLabel = (m) => {
  const found = moisOptions.find(o => o.value === m)
  return found ? found.label : 'Mois ' + m
}

const fetchBudgets = async () => {
  loadingBudgets.value = true
  try {
    const res = await api.get('/gestion/budgets')
    budgets.value = res.data
  } catch (e) { console.error(e) }
  finally { loadingBudgets.value = false }
}

const submitBudget = async () => {
  try {
    if (editBudgetId.value) {
      await api.put(`/gestion/budgets/${editBudgetId.value}`, budgetForm.value)
    } else {
      await api.post('/gestion/budgets', budgetForm.value)
    }
    showBudgetModal.value = false
    editBudgetId.value = null
    await fetchBudgets()
  } catch (e) { console.error(e) }
}

const openEditBudget = (b) => {
  editBudgetId.value = b.id
  budgetForm.value = { ...b }
  showBudgetModal.value = true
}

const deleteBudget = async (id) => {
  if (!confirm('Supprimer ce budget ?')) return
  try {
    await api.delete(`/gestion/budgets/${id}`)
    await fetchBudgets()
  } catch (e) { console.error(e) }
}

const getBudgetPercent = (b) => {
  if (!b.montantPlafond || b.montantPlafond <= 0) return 0
  return Math.min(100, Math.round((b.montantConsomme / b.montantPlafond) * 100))
}

const getBudgetStatus = (b) => {
  const p = getBudgetPercent(b)
  if (p >= 100) return 'danger'
  if (p >= b.seuilAlertePourcent) return 'warning'
  return 'ok'
}


// === TIERS (pour les selects) ===
const tiers = ref([])
const fetchTiers = async () => {
  try {
    const res = await api.get('/tiers')
    tiers.value = res.data
  } catch (e) { console.error(e) }
}

// Active tab
const activeTab = ref('budgets')

onMounted(() => {
  fetchBudgets()
  fetchTiers()
})

const formatCurrency = (v) => v ? new Intl.NumberFormat('fr-FR').format(v) : '0'

const langStore = useLangStore()
const t = computed(() => langStore.t)
</script>

<template>
  <MainLayout>
    <template #title>{{ t("gestion.titre") }}</template>
    <template #subtitle>Budget, factures récurrentes et automatisations</template>

    <div class="gestion-page">
      <!-- Tabs -->
      <div class="tabs-nav">
        <button class="tab-btn" :class="{ active: activeTab === 'budgets' }" @click="activeTab = 'budgets'">
          📊 Gestion Budgétaire
        </button>

        <button class="tab-btn" :class="{ active: activeTab === 'automatisations' }" @click="activeTab = 'automatisations'">
          ⚡ Automatisations
        </button>
      </div>

      <!-- ═══════════════════ TAB BUDGETS ═══════════════════ -->
      <div v-if="activeTab === 'budgets'" class="tab-content">
        <div class="section-header">
          <h3>Budgets par Catégorie de Dépense</h3>
          <button class="btn-primary" @click="showBudgetModal = true; editBudgetId = null; budgetForm = { categorie: 'PAIEMENT_FOURNISSEUR', annee: new Date().getFullYear(), mois: new Date().getMonth() + 1, montantPlafond: 0, seuilAlertePourcent: 80 }">
            + Nouveau Budget
          </button>
        </div>

        <div class="budget-grid" v-if="budgets.length > 0">
          <div v-for="b in budgets" :key="b.id" class="budget-card" :class="'status-' + getBudgetStatus(b)">
            <div class="budget-header">
              <span class="budget-cat">{{ categoryLabels[b.categorie] || b.categorie }}</span>
              <span class="budget-period">{{ getMoisLabel(b.mois) }} {{ b.annee }}</span>
            </div>
            <div class="budget-amounts">
              <span class="consumed">{{ formatCurrency(b.montantConsomme) }}</span>
              <span class="separator">/</span>
              <span class="total">{{ formatCurrency(b.montantPlafond) }} XAF</span>
            </div>
            <div class="budget-bar-track">
              <div class="budget-bar-fill" :class="'fill-' + getBudgetStatus(b)" :style="{ width: getBudgetPercent(b) + '%' }"></div>
            </div>
            <div class="budget-footer">
              <span class="budget-pct">{{ getBudgetPercent(b) }}% utilisé</span>
              <span class="budget-alert" v-if="b.alerteEnvoyee">⚠️ Alerte envoyée</span>
              <div class="budget-actions">
                <button class="btn-icon" @click="openEditBudget(b)" title="Modifier">✏️</button>
                <button class="btn-icon danger" @click="deleteBudget(b.id)" title="Supprimer">🗑️</button>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="empty-state">Aucun budget défini. Créez votre premier budget pour suivre vos dépenses.</div>
      </div>



      <!-- ═══════════════════ TAB AUTOMATISATIONS ═══════════════════ -->
      <div v-if="activeTab === 'automatisations'" class="tab-content">
        <div class="auto-grid">
          <div class="auto-card">
            <div class="auto-icon" style="background: #dbeafe; color: #2563eb;">📧</div>
            <h4>Relances Automatiques</h4>
            <p>Les clients reçoivent un mail de rappel automatique lorsque leurs factures arrivent à échéance :</p>
            <ul class="auto-list">
              <li><span class="auto-badge j1">J+1</span> Premier rappel courtois</li>
              <li><span class="auto-badge j7">J+7</span> Deuxième rappel</li>
              <li><span class="auto-badge j30">J+30</span> Relance formelle avec mention de pénalités</li>
            </ul>
            <div class="auto-status active">✅ Actif — Exécution quotidienne à 7h00</div>
          </div>

          <div class="auto-card">
            <div class="auto-icon" style="background: #fef3c7; color: #d97706;">💰</div>
            <h4>Pénalités de Retard</h4>
            <p>Application automatique de pénalités sur les factures de vente en retard de paiement :</p>
            <ul class="auto-list">
              <li>Taux : <strong>1,5% / mois</strong> (proratisé au jour)</li>
              <li>Application tous les <strong>30 jours</strong> de retard</li>
              <li>Ajouté au solde débiteur du client</li>
            </ul>
            <div class="auto-status active">✅ Actif — Calcul quotidien</div>
          </div>

          <div class="auto-card">
            <div class="auto-icon" style="background: #dcfce7; color: #16a34a;">🔄</div>
            <h4>Factures Récurrentes</h4>
            <p>Génération automatique de factures pour les abonnements et loyers mensuels :</p>
            <ul class="auto-list">
              <li>Génération le <strong>jour configuré</strong> de chaque mois</li>
              <li>Statut « En Attente de Paiement » automatique</li>
              <li>Notification envoyée au comptable</li>
            </ul>
            <div class="auto-status active">✅ Actif — {{ recurrentes.filter(r => r.actif).length }} récurrence(s) configurée(s)</div>
          </div>

          <div class="auto-card">
            <div class="auto-icon" style="background: #fce7f3; color: #db2777;">📊</div>
            <h4>Alertes Budget</h4>
            <p>Notification automatique lorsqu'un budget atteint son seuil d'alerte :</p>
            <ul class="auto-list">
              <li>Seuil configurable par budget (défaut : <strong>80%</strong>)</li>
              <li>Notification au RF et au PDG</li>
              <li>Vérification quotidienne</li>
            </ul>
            <div class="auto-status active">✅ Actif — {{ budgets.length }} budget(s) surveillé(s)</div>
          </div>
        </div>
      </div>

      <!-- ═══════════════════ MODAL BUDGET ═══════════════════ -->
      <div v-if="showBudgetModal" class="modal-backdrop fade-in">
        <div class="modal">
          <div class="modal-header">
            <h3>{{ editBudgetId ? 'Modifier le Budget' : 'Nouveau Budget' }}</h3>
            <button @click="showBudgetModal = false" class="close-btn">&times;</button>
          </div>
          <form @submit.prevent="submitBudget" class="modal-body">
            <div class="form-group">
              <label>Catégorie</label>
              <select v-model="budgetForm.categorie" class="input-std">
                <option v-for="c in categories" :key="c" :value="c">{{ categoryLabels[c] || c }}</option>
              </select>
            </div>
            <div class="form-row">
              <div class="form-group half">
                <label>Année</label>
                <input type="number" v-model.number="budgetForm.annee" class="input-std" />
              </div>
              <div class="form-group half">
                <label>Période</label>
                <select v-model.number="budgetForm.mois" class="input-std">
                  <option v-for="m in moisOptions" :key="m.value" :value="m.value">{{ m.label }}</option>
                </select>
              </div>
            </div>
            <div class="form-row">
              <div class="form-group half">
                <label>Montant Plafond (XAF)</label>
                <input type="number" v-model.number="budgetForm.montantPlafond" class="input-std" />
              </div>
              <div class="form-group half">
                <label>Seuil Alerte (%)</label>
                <input type="number" v-model.number="budgetForm.seuilAlertePourcent" min="1" max="100" class="input-std" />
              </div>
            </div>
            <div class="modal-footer">
              <button type="button" class="btn-text" @click="showBudgetModal = false">{{ t("common.annuler") }}</button>
              <button type="submit" class="btn-primary">{{ editBudgetId ? 'Enregistrer' : 'Créer' }}</button>
            </div>
          </form>
        </div>
      </div>


    </div>
  </MainLayout>
</template>

<style scoped>
.gestion-page { max-width: 1200px; }

.tabs-nav { display: flex; gap: 0.5rem; border-bottom: 1px solid #e5e7eb; margin-bottom: 1.5rem; }
.tab-btn { background: none; border: none; padding: 0.75rem 1rem; font-size: 0.875rem; font-weight: 600; color: #6b7280; border-bottom: 2px solid transparent; cursor: pointer; transition: 0.15s; }
.tab-btn:hover { color: #111827; }
.tab-btn.active { color: #2563eb; border-color: #2563eb; }

.tab-content { animation: fadeIn 0.3s ease; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(4px); } to { opacity: 1; transform: translateY(0); } }

.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem; flex-wrap: wrap; gap: 1rem; }
.section-header h3 { font-size: 1.1rem; font-weight: 700; color: #1e293b; }

/* Budget Grid */
.budget-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 1rem; }
.budget-card { background: white; border-radius: 12px; padding: 1.25rem; border: 1px solid #e5e7eb; transition: all 0.2s; }
.budget-card:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.06); }
.budget-card.status-danger { border-color: #fca5a5; background: #fef2f2; }
.budget-card.status-warning { border-color: #fcd34d; background: #fffbeb; }

.budget-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.75rem; }
.budget-cat { font-weight: 700; font-size: 0.95rem; color: #1e293b; }
.budget-period { font-size: 0.75rem; color: #64748b; background: #f1f5f9; padding: 0.2rem 0.5rem; border-radius: 4px; }

.budget-amounts { margin-bottom: 0.5rem; }
.budget-amounts .consumed { font-size: 1.25rem; font-weight: 700; color: #1e293b; }
.budget-amounts .separator { color: #94a3b8; margin: 0 0.25rem; }
.budget-amounts .total { color: #64748b; font-size: 0.9rem; }

.budget-bar-track { height: 6px; background: #e2e8f0; border-radius: 3px; overflow: hidden; margin-bottom: 0.75rem; }
.budget-bar-fill { height: 100%; border-radius: 3px; transition: width 0.5s ease; }
.fill-ok { background: #10b981; }
.fill-warning { background: #f59e0b; }
.fill-danger { background: #ef4444; }

.budget-footer { display: flex; align-items: center; justify-content: space-between; font-size: 0.8rem; color: #64748b; }
.budget-alert { color: #f59e0b; font-weight: 600; }
.budget-actions { display: flex; gap: 0.25rem; }

/* Auto Grid */
.auto-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 1rem; }
.auto-card { background: white; border-radius: 12px; padding: 1.5rem; border: 1px solid #e5e7eb; }
.auto-card h4 { margin: 0.75rem 0 0.5rem; font-size: 1rem; color: #1e293b; }
.auto-card p { font-size: 0.85rem; color: #64748b; margin-bottom: 0.75rem; }
.auto-icon { width: 40px; height: 40px; display: flex; align-items: center; justify-content: center; border-radius: 10px; font-size: 1.25rem; }
.auto-list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 0.5rem; font-size: 0.85rem; color: #475569; }
.auto-list li { display: flex; align-items: center; gap: 0.5rem; }
.auto-badge { padding: 0.15rem 0.5rem; border-radius: 4px; font-size: 0.7rem; font-weight: 700; }
.auto-badge.j1 { background: #dbeafe; color: #2563eb; }
.auto-badge.j7 { background: #fef3c7; color: #d97706; }
.auto-badge.j30 { background: #fee2e2; color: #dc2626; }
.auto-status { margin-top: 1rem; padding: 0.5rem 0.75rem; border-radius: 6px; font-size: 0.8rem; font-weight: 600; }
.auto-status.active { background: #dcfce7; color: #16a34a; }

/* Table */
.table-card { background: white; border-radius: 12px; border: 1px solid #e5e7eb; overflow: hidden; }
.data-table { width: 100%; border-collapse: collapse; }
.data-table th, .data-table td { padding: 1rem 1.25rem; text-align: left; border-bottom: 1px solid #f3f4f6; }
.data-table th { background: #f9fafb; font-size: 0.75rem; font-weight: 600; text-transform: uppercase; color: #6b7280; letter-spacing: 0.05em; }
.data-table td { font-size: 0.875rem; color: #4b5563; }
.text-right { text-align: right !important; }
.font-semibold { font-weight: 600; }

.badge { display: inline-flex; padding: 0.2rem 0.5rem; border-radius: 4px; font-size: 0.7rem; font-weight: 600; }
.badge-green { background: #dcfce7; color: #16a34a; }
.badge-orange { background: #fef3c7; color: #d97706; }
.badge-gray { background: #f1f5f9; color: #64748b; }

/* Buttons */
.btn-primary { display: flex; align-items: center; gap: 0.5rem; background: #2563eb; color: white; padding: 0.625rem 1rem; border-radius: 8px; border: none; font-size: 0.875rem; font-weight: 600; cursor: pointer; transition: 0.15s; }
.btn-primary:hover { background: #1d4ed8; }
.btn-text { background: none; border: none; color: #6b7280; font-weight: 500; cursor: pointer; }
.btn-icon { background: none; border: none; cursor: pointer; font-size: 1rem; padding: 0.25rem; border-radius: 4px; }
.btn-icon:hover { background: #f1f5f9; }
.btn-icon.danger:hover { background: #fee2e2; }

.empty-state { text-align: center; padding: 3rem; color: #94a3b8; font-size: 0.95rem; }

/* Modal */
.modal-backdrop { position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal { background: white; border-radius: 16px; width: 90%; max-width: 600px; max-height: 90vh; overflow-y: auto; box-shadow: 0 20px 60px rgba(0,0,0,0.15); }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 1.25rem 1.5rem; border-bottom: 1px solid #f3f4f6; }
.modal-header h3 { font-size: 1.1rem; font-weight: 700; }
.close-btn { background: none; border: none; font-size: 1.5rem; color: #94a3b8; cursor: pointer; }
.modal-body { padding: 1.5rem; display: flex; flex-direction: column; gap: 1rem; }
.modal-footer { display: flex; justify-content: flex-end; gap: 0.75rem; padding-top: 1rem; border-top: 1px solid #f3f4f6; }
.form-row { display: flex; gap: 1rem; }
.form-group { display: flex; flex-direction: column; gap: 0.35rem; flex: 1; }
.form-group.half { flex: 1; }
.form-group.third { flex: 1; }
.form-group label { font-size: 0.8rem; font-weight: 600; color: #475569; }
.input-std { padding: 0.625rem 0.75rem; border: 1px solid #e2e8f0; border-radius: 8px; font-size: 0.9rem; outline: none; transition: 0.2s; }
.input-std:focus { border-color: #3b82f6; box-shadow: 0 0 0 3px rgba(59,130,246,0.1); }

.fade-in { animation: fadeIn 0.2s ease; }
</style>
