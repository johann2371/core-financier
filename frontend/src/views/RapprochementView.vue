<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useCompteStore } from '../stores/compte.store'
import { useRapprochementStore } from '../stores/rapprochement.store'
import { useLangStore } from '../stores/lang.store'
import MainLayout from '../components/MainLayout.vue'
import api from '../services/api'
import { 
  BuildingLibraryIcon, 
  SparklesIcon, 
  CheckCircleIcon, 
  ArrowUpOnSquareIcon, 
  ArrowPathIcon, 
  LinkIcon, 
  ChevronRightIcon,
  XMarkIcon,
  DocumentTextIcon,
  InformationCircleIcon
} from '@heroicons/vue/24/outline'

const compteStore = useCompteStore()
const rapprochementStore = useRapprochementStore()

const selectedCompteId = ref(null)
const showCreateModal = ref(false)
const sessionForm = ref({
  dateDebut: '',
  dateFin: '',
  soldeInitial: 0
})

const currentStep = ref(1) // 1: List/Selection, 2: Import, 3: Matching, 4: Summary

const fileInput = ref(null)
const selectedFile = ref(null)

const systemTransactions = ref([])
const activeSession = ref(null)

onMounted(async () => {
  await compteStore.fetchComptes()
})

const banques = computed(() => compteStore.banques)

const selectCompte = async (compteId) => {
  selectedCompteId.value = compteId
  await rapprochementStore.fetchSessions(compteId)
  currentStep.value = 1
}

const openCreateModal = () => {
  const lastSession = rapprochementStore.sessions[0]
  if (lastSession) {
    sessionForm.value.soldeInitial = lastSession.soldeFinalReleve
    sessionForm.value.dateDebut = lastSession.dateFin
  }
  showCreateModal.value = true
}

const handleCreateSession = async () => {
  try {
    const session = await rapprochementStore.creerSession({
      compteId: selectedCompteId.value,
      ...sessionForm.value
    })
    activeSession.value = session
    showCreateModal.value = false
    currentStep.value = 2
  } catch (err) {
    console.error(err)
  }
}

const triggerFileInput = () => fileInput.value.click()
const onFileSelected = (e) => selectedFile.value = e.target.files[0]

const handleImport = async () => {
  if (!selectedFile.value) return
  await rapprochementStore.importerReleve(activeSession.value.id, selectedFile.value)
  await fetchSystemTransactions()
  currentStep.value = 3
}

const fetchSystemTransactions = async () => {
  const res = await api.get(`/api/rapprochements/transactions/compte/${selectedCompteId.value}`)
  systemTransactions.value = res.data
}

const handleAutoMatch = async () => {
  await rapprochementStore.autoMatch(activeSession.value.id)
}

const selectedLigne = ref(null)
const selectedSystemTx = ref(null)

const selectLigne = (ligne) => {
  if (ligne.matched) return
  selectedLigne.value = ligne
}

const selectSystemTx = (tx) => {
  selectedSystemTx.value = tx
}

const confirmMatch = async () => {
  if (!selectedLigne.value || !selectedSystemTx.value) return
  
  const type = selectedSystemTx.value.referenceExecution !== undefined ? 'DECAISSEMENT' : 'ENCAISSEMENT'
  await rapprochementStore.matchManuel(selectedLigne.value.id, selectedSystemTx.value.id, type)
  
  // Refresh data
  selectedLigne.value = null
  selectedSystemTx.value = null
  await fetchSystemTransactions()
}

const soldeCalculé = computed(() => {
  if (!rapprochementStore.lignesReleve.length) return 0
  let total = activeSession.value?.soldeInitialReleve || 0
  rapprochementStore.lignesReleve.forEach(l => {
    total += (l.credit || 0) - (l.debit || 0)
  })
  return total
})

const handleValidate = async () => {
  await rapprochementStore.validerSession(activeSession.value.id, soldeCalculé.value)
  currentStep.value = 1
  await rapprochementStore.fetchSessions(selectedCompteId.value)
}

const formatAmount = (val) => new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'XAF' }).format(val)
const formatDate = (d) => new Date(d).toLocaleDateString('fr-FR')

const langStore = useLangStore()
const t = computed(() => langStore.t)
</script>

<template>
  <MainLayout>
    <template #title>{{ t("rapprochement.titre") }}</template>
    <template #subtitle>Assurer la concordance entre vos comptes et la banque</template>

    <div class="rapprochement-container">
      
      <!-- Selection de compte (Sidebar gauche interne) -->
      <aside class="bank-selector">
        <h3>Comptes Bancaires</h3>
        <div class="bank-list">
          <div v-for="bank in banques" :key="bank.id" 
               class="bank-card" :class="{ active: selectedCompteId === bank.id }"
               @click="selectCompte(bank.id)">
            <div class="bank-icon">
              <BuildingLibraryIcon class="w-6 h-6 text-slate-400" />
            </div>
            <div class="bank-info">
              <span class="bank-name">{{ bank.libelle }}</span>
              <span class="bank-number">{{ bank.numero }}</span>
            </div>
            <div class="bank-balance">{{ formatAmount(bank.solde) }}</div>
          </div>
        </div>
      </aside>

      <!-- Zone d'action de droite -->
      <main class="rap-content" v-if="selectedCompteId">
        
        <!-- STEP 1: Liste des sessions -->
        <div v-if="currentStep === 1" class="step-fade">
          <div class="content-header">
            <h3>Historique des rapprochements</h3>
            <button @click="openCreateModal" class="btn-primary">Nouvelle Session</button>
          </div>

          <div class="sessions-grid">
            <div v-if="rapprochementStore.sessions.length === 0" class="empty-state">
              Aucune session trouvée pour ce compte. Commencez votre premier rapprochement !
            </div>
            <div v-for="session in rapprochementStore.sessions" :key="session.id" class="session-card">
              <div class="session-status" :class="{ validated: session.valide }">
                {{ session.valide ? 'Validé' : 'En cours' }}
              </div>
              <div class="session-dates">Période du {{ formatDate(session.dateDebut) }} au {{ formatDate(session.dateFin) }}</div>
              <div class="session-stats">
                <div class="stat"><span>Initial:</span> {{ formatAmount(session.soldeInitialReleve) }}</div>
                <div class="stat"><span>Final:</span> {{ formatAmount(session.soldeFinalReleve || 0) }}</div>
              </div>
              <button class="btn-outline-sm" @click="activeSession = session; currentStep = 3; rapprochementStore.importerReleve(session.id, null); fetchSystemTransactions()">Consulter</button>
            </div>
          </div>
        </div>

        <!-- STEP 2: Import CSV -->
        <div v-if="currentStep === 2" class="step-fade">
          <div class="import-zone">
            <h3>Importer le relevé bancaire</h3>
            <p>Veuillez télécharger le fichier CSV exporté depuis votre interface bancaire.</p>
            
            <div class="drop-zone" @click="triggerFileInput">
              <input type="file" ref="fileInput" class="hidden" @change="onFileSelected" accept=".csv">
              <div class="drop-icon">
                <ArrowUpOnSquareIcon class="w-12 h-12 text-slate-300" />
              </div>
              <span v-if="!selectedFile">Cliquez pour choisir un fichier</span>
              <span v-else class="file-name">{{ selectedFile.name }}</span>
            </div>

            <div class="import-actions">
              <button class="btn-primary-large" :disabled="!selectedFile" @click="handleImport">
                Lancer l'importation
              </button>
            </div>

            <div class="csv-help">
              <strong>Format attendu :</strong> CSV avec séparateur point-virgule (;) <br>
              Colonnes : <code>Date (JJ/MM/AAAA); Libellé; Montant (négatif pour débit)</code>
            </div>
          </div>
        </div>

        <!-- STEP 3: Matching Interface -->
        <div v-if="currentStep === 3" class="matching-layout step-fade">
          <div class="matching-toolbar">
            <div class="session-info-bar">
              <span><strong>Session :</strong> {{ formatDate(activeSession.dateDebut) }} au {{ formatDate(activeSession.dateFin) }}</span>
              <span class="badge-blue">Progression : {{ rapprochementStore.lignesReleve.filter(l => l.matched).length }} / {{ rapprochementStore.lignesReleve.length }}</span>
              <span><strong>Solde relevé :</strong> {{ formatAmount(soldeCalculé) }}</span>
            </div>
            <div class="toolbar-actions">
              <button @click="handleAutoMatch" class="btn-magic">
                <SparklesIcon class="w-4 h-4 mr-2" />
                Magic Auto-Match
              </button>
              <button @click="handleValidate" class="btn-success">
                <CheckCircleIcon class="w-4 h-4 mr-2" />
                Finaliser & Valider
              </button>
            </div>
          </div>

          <div class="dual-list-container">
            <!-- Colonne 1: Relevé Bancaire -->
            <div class="side-panel">
              <div class="panel-header">Relevé Bancaire (Reçu)</div>
              <div class="scroll-list">
                <div v-for="ligne in rapprochementStore.lignesReleve" :key="ligne.id" 
                     class="match-item" :class="{ matched: ligne.matched, selected: selectedLigne?.id === ligne.id }"
                     @click="selectLigne(ligne)">
                  <div class="item-main">
                    <span class="item-date">{{ formatDate(ligne.dateOperation) }}</span>
                    <span class="item-label">{{ ligne.libelle }}</span>
                  </div>
                  <div class="item-amount" :class="{ debit: ligne.debit > 0 }">
                    {{ ligne.debit > 0 ? '-' + formatAmount(ligne.debit) : '+' + formatAmount(ligne.credit) }}
                  </div>
                  <div v-if="ligne.matched" class="match-check">
                    <CheckCircleIcon class="w-5 h-5 text-green-500" />
                  </div>
                </div>
              </div>
            </div>

            <!-- Colonne centrale: Link Actions -->
            <div class="match-bridge" v-if="selectedLigne">
              <div v-if="selectedSystemTx" class="match-preview bounce-in">
                <span>Appairer ?</span>
                <button class="btn-primary" @click="confirmMatch">
                  <LinkIcon class="w-4 h-4 mr-2" />
                  Vailder le lien
                </button>
              </div>
              <div v-else class="match-hint">
                <ChevronRightIcon class="w-6 h-6 mx-auto mb-2 opacity-20" />
                Sélectionnez une écriture système à droite
              </div>
            </div>

            <!-- Colonne 2: Système CoreFi -->
            <div class="side-panel">
              <div class="panel-header">Écritures Système (CoreFi)</div>
              <div class="scroll-list">
                <div v-for="tx in systemTransactions" :key="tx.id" 
                     class="match-item" :class="{ selected: selectedSystemTx?.id === tx.id }"
                     @click="selectSystemTx(tx)">
                  <div class="item-main">
                    <span class="item-date">{{ formatDate(tx.dateEncaissement || tx.dateDecaissement) }}</span>
                    <span class="item-label">
                      <strong>{{ tx.numero }}</strong><br>
                      {{ tx.client?.raisonSociale || tx.fournisseur?.raisonSociale || tx.beneficiaire }}
                    </span>
                  </div>
                  <div class="item-amount">
                    {{ formatAmount(tx.montant) }}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

      </main>

      <div v-else class="no-compte-selected">
        <div class="illustration">
          <BuildingLibraryIcon class="w-16 h-16" />
        </div>
        <h3>Sélectionnez un compte bancaire</h3>
        <p>Pour commencer ou consulter un rapprochement, choisissez un compte dans la liste de gauche.</p>
      </div>

    </div>

    <!-- Modale création session -->
    <div v-if="showCreateModal" class="modal-backdrop" @click.self="showCreateModal = false">
      <div class="modal-content">
        <div class="modal-header">
          <h3>Nouvelle session de rapprochement</h3>
          <button @click="showCreateModal = false" class="close-btn"><XMarkIcon class="w-6 h-6" /></button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>Date de début</label>
            <input type="date" v-model="sessionForm.dateDebut">
          </div>
          <div class="form-group">
            <label>Date de fin</label>
            <input type="date" v-model="sessionForm.dateFin">
          </div>
          <div class="form-group">
            <label>Solde initial du relevé (XAF)</label>
            <input type="number" v-model="sessionForm.soldeInitial">
          </div>
        </div>
        <div class="modal-footer">
          <button @click="showCreateModal = false" class="btn-text">{{ t("common.annuler") }}</button>
          <button @click="handleCreateSession" class="btn-primary">Créer la session</button>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

<style scoped>
.rapprochement-container {
  display: flex;
  height: calc(100vh - 120px);
  gap: 1.5rem;
  padding: 0 1.5rem 1.5rem;
}

.bank-selector {
  width: 320px;
  background: white;
  border-radius: 16px;
  padding: 1.5rem;
  box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05);
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.bank-selector h3 { font-size: 1rem; color: #111827; font-weight: 700; margin-bottom: 0.5rem; }

.bank-list { display: flex; flex-direction: column; gap: 0.75rem; }

.bank-card {
  padding: 1rem;
  border-radius: 12px;
  border: 1px solid #f3f4f6;
  cursor: pointer;
  transition: all 0.2s;
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 0.75rem;
  align-items: center;
}

.bank-card:hover { transform: translateY(-2px); border-color: #d1d5db; }
.bank-card.active { border-color: var(--c-primary); background: #eff6ff; }

.bank-icon { font-size: 1.25rem; }
.bank-info { display: flex; flex-direction: column; min-width: 0; }
.bank-name { font-weight: 600; font-size: 0.9rem; color: #111827; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.bank-number { font-size: 0.75rem; color: #6b7280; }
.bank-balance { font-weight: 700; font-size: 0.85rem; color: var(--c-primary); }

.rap-content { flex: 1; min-width: 0; background: white; border-radius: 16px; padding: 2rem; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05); overflow-y: auto; }

.content-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 2rem; }

.sessions-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 1.25rem; }

.session-card {
  padding: 1.5rem;
  border-radius: 16px;
  border: 1px solid #f3f4f6;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  position: relative;
}

.session-status {
  position: absolute; top: 1.25rem; right: 1.25rem;
  padding: 4px 8px; border-radius: 6px; font-size: 0.7rem; font-weight: 700; text-transform: uppercase;
  background: #fef3c7; color: #92400e;
}
.session-status.validated { background: #d1fae5; color: #065f46; }

.session-dates { font-weight: 600; font-size: 0.95rem; color: #111827; }
.session-stats { display: flex; gap: 1.5rem; font-size: 0.85rem; }
.stat span { color: #6b7280; margin-right: 4px; }

/* IMPORT ZONE */
.import-zone { max-width: 600px; margin: 4rem auto; text-align: center; display: flex; flex-direction: column; gap: 1.5rem; }
.drop-zone {
  border: 2px dashed #d1d5db; border-radius: 20px; padding: 4rem 2rem;
  cursor: pointer; transition: all 0.2s;
}
.drop-zone:hover { border-color: var(--c-primary); background: #f9fafb; }
.drop-icon { font-size: 3rem; margin-bottom: 1rem; }
.file-name { font-weight: 600; color: var(--c-primary); }

.csv-help { font-size: 0.8rem; color: #6b7280; background: #f9fafb; padding: 1rem; border-radius: 8px; margin-top: 1rem; }

/* MATCHING LAYOUT */
.matching-layout { display: flex; flex-direction: column; height: 100%; gap: 1.5rem; }
.matching-toolbar { display: flex; justify-content: space-between; align-items: center; padding-bottom: 1rem; border-bottom: 1px solid #f3f4f6; }
.session-info-bar { display: flex; gap: 2rem; font-size: 0.85rem; color: #4b5563; }
.toolbar-actions { display: flex; gap: 0.75rem; }

.dual-list-container { display: flex; flex: 1; gap: 1rem; min-height: 500px; overflow: hidden; }
.side-panel { flex: 1; border: 1px solid #f3f4f6; border-radius: 12px; display: flex; flex-direction: column; background: #f9fafb; }
.panel-header { padding: 1rem; font-weight: 700; font-size: 0.9rem; text-align: center; background: white; border-bottom: 1px solid #f3f4f6; border-radius: 12px 12px 0 0; }
.scroll-list { flex: 1; overflow-y: auto; padding: 0.75rem; display: flex; flex-direction: column; gap: 0.5rem; }

.match-item {
  background: white; padding: 1rem; border-radius: 10px; border: 1px solid #e5e7eb;
  display: flex; justify-content: space-between; align-items: center; cursor: pointer; transition: all 0.1s;
}
.match-item:hover { transform: scale(1.02); box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05); }
.match-item.selected { border-width: 2px; border-color: var(--c-primary); box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.1); }
.match-item.matched { opacity: 0.5; background: #f3f4f6; pointer-events: none; }

.item-main { display: flex; flex-direction: column; gap: 0.25rem; font-size: 0.8rem; }
.item-date { color: #6b7280; }
.item-label { font-weight: 500; color: #111827; }
.item-amount { font-weight: 700; color: #059669; }
.item-amount.debit { color: #dc2626; }

.match-bridge { width: 180px; display: flex; align-items: center; justify-content: center; text-align: center; }
.match-preview { display: flex; flex-direction: column; gap: 1rem; align-items: center; font-size: 0.9rem; font-weight: 700; color: var(--c-primary); }

.btn-magic { background: linear-gradient(135deg, #6366f1, #a855f7); color: white; border: none; padding: 8px 16px; border-radius: 8px; font-weight: 600; cursor: pointer; }
.btn-success { background: #10b981; color: white; border: none; padding: 8px 16px; border-radius: 8px; font-weight: 600; cursor: pointer; }

.no-compte-selected { display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%; color: #9ca3af; text-align: center; }
.illustration { font-size: 4rem; opacity: 0.2; margin-bottom: 1.5rem; }

.hidden { display: none; }
.badge-blue { background: #dbeafe; color: #1e40af; padding: 2px 8px; border-radius: 9999px; font-size: 0.75rem; font-weight: 600; }

.form-group { margin-bottom: 1rem; }
.form-group label { display: block; font-size: 0.85rem; font-weight: 600; color: #374151; margin-bottom: 0.5rem; }
.form-group input { width: 100%; padding: 0.625rem; border: 1px solid #d1d5db; border-radius: 8px; }

</style>
