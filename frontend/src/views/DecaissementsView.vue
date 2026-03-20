<script setup>
import { ref, onMounted } from 'vue'
import MainLayout from '../components/MainLayout.vue'
import { useDecaissementStore } from '../stores/decaissement.store'
import { useTierStore } from '../stores/tier.store'

const store = useDecaissementStore()
const tierStore = useTierStore()
// Modals State
const showCreateModal = ref(false)
const showApproveModal = ref(false)
const showRejectModal = ref(false)
const activeDecaissement = ref(null)

// Forms State
const createForm = ref({ motif: '', montant: '', fournisseurId: '', beneficiaire: '', mode: 'VIREMENT' })
const rejectForm = ref({ reason: 'missing_docs', comments: '' })
const approveForm = ref({ checks: [false, false, false, false] })

// Bank balances synthétique pour l'UI
const bankBalance = ref(14250000)

onMounted(async () => {
  await store.fetchDecaissements()
  await tierStore.fetchTiers()
})

const getStatusClass = (statut) => {
  const s = statut ? statut.toLowerCase() : ''
  if (s.includes('valide') || s.includes('paye')) return 'badge-success'
  if (s.includes('rejete')) return 'badge-danger'
  return 'badge-warning'
}

const submitCreate = async () => {
  try {
    const selectedFou = tierStore.fournisseurs.find(f => f.id === createForm.value.fournisseurId)
    const beneficiaireStr = selectedFou ? selectedFou.raisonSociale : 'Fournisseur Inconnu'

    const dataToSend = {
      motif: `${createForm.value.mode.toUpperCase()} - ${createForm.value.motif}`,
      montant: createForm.value.montant,
      beneficiaire: beneficiaireStr,
      fournisseurId: createForm.value.fournisseurId,
      deviseId: 1
    }
    await store.createDecaissement(dataToSend)
    showCreateModal.value = false
    createForm.value = { motif: '', montant: '', fournisseurId: '', beneficiaire: '', mode: 'VIREMENT' }
  } catch(e) { console.error(e) }
}

const openApprove = (item) => {
  activeDecaissement.value = item
  approveForm.value.checks = [false, false, false, false]
  showApproveModal.value = true
}

const openReject = (item) => {
  activeDecaissement.value = item
  rejectForm.value = { reason: 'missing_docs', comments: '' }
  showRejectModal.value = true
}

const submitApprove = async () => {
  try {
    await store.updateStatut(activeDecaissement.value.id, 'VALIDE_FINANCES', 'Approuvé via Checklist')
    showApproveModal.value = false
  } catch(e) { console.error(e) }
}

const submitReject = async () => {
  try {
    const comment = `Raison: ${rejectForm.value.reason}. ${rejectForm.value.comments}`
    await store.updateStatut(activeDecaissement.value.id, 'REJETE', comment)
    showRejectModal.value = false
  } catch(e) { console.error(e) }
}
</script>

<template>
  <MainLayout>
    <template #title>Décaissements</template>

    <template #actions>
      <button @click="showCreateModal = true" class="btn-primary">
        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
        Nouvelle Demande
      </button>
    </template>

    <div class="table-card">
      <div v-if="store.loading && store.decaissements.length === 0" class="loading-state">
        Chargement...
      </div>
      
      <div v-else-if="store.error" class="error-state">
        {{ store.error }}
        <button @click="store.fetchDecaissements" class="btn-outline mt-2">Réessayer</button>
      </div>

      <table v-else class="data-table">
        <thead>
          <tr>
            <th>Réf. TXN</th>
            <th>Date</th>
            <th>Bénéficiaire & Motif</th>
            <th class="text-right">Montant (XAF)</th>
            <th>Statut</th>
            <th class="text-center">Contrôle</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="store.decaissements.length === 0" class="empty-row text-center">
            <td colspan="6">
              <div class="empty-state">
                <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>
                <p>Aucune demande de décaissement.</p>
              </div>
            </td>
          </tr>
          
          <tr v-for="item in store.decaissements" :key="item.id">
            <td class="font-semibold text-dark">#TXN-{{ item.id?.toString().padStart(4, '0') }}-BK</td>
            <td class="text-muted">{{ new Date(item.dateDemande).toLocaleDateString() }}</td>
            <td>
              <div class="cell-stack">
                <strong class="text-dark">{{ item.beneficiaire || 'N/A' }}</strong>
                <span class="text-muted text-sm">{{ item.motif }}</span>
              </div>
            </td>
            <td class="text-right font-semibold text-dark">{{ item.montant?.toLocaleString() }} XAF</td>
            <td>
              <span class="badge" :class="getStatusClass(item.statut)">
                {{ item.statut }}
              </span>
            </td>
            <td class="actions-cell">
              <!-- Si statut est SOUMIS ou en attente on gère l'approbation -->
               <template v-if="item.statut === 'SOUMIS' || item.statut === 'EN_ATTENTE'">
                <button class="btn-icon text-green" @click="openApprove(item)" title="Approuver">
                  <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"></polyline></svg>
                </button>
                <button class="btn-icon text-red" @click="openReject(item)" title="Rejeter">
                  <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
                </button>
               </template>
               <template v-else>
                 <span class="text-muted text-sm italic">Traité</span>
               </template>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- MODAL : NOUVELLE DEMANDE (SAISIE RAPIDE AVANCÉE) -->
    <div v-if="showCreateModal" class="modal-backdrop">
      <div class="modal modal-lg">
        <div class="modal-header">
          <div class="modal-title-group">
            <div class="modal-icon bg-blue-light">
               <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#2563eb" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
            </div>
            <div>
              <h3>Nouvelle Demande de Décaissement</h3>
              <span class="subtitle">CRÉATION SÉCURISÉE • <strong class="text-blue">NOUVEAU FLUX</strong></span>
            </div>
          </div>
          <button @click="showCreateModal = false" class="close-btn"><svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg></button>
        </div>
        
        <div class="modal-split">
          <!-- Formulaire Principal -->
          <form id="create-decaissement-form" @submit.prevent="submitCreate" class="modal-left">
            <div class="form-group input-with-icon">
              <label>Fournisseur</label>
              <select v-model="createForm.fournisseurId" required class="input-huge" autofocus>
                 <option value="" disabled>Sélectionner un Fournisseur...</option>
                 <option v-for="fou in tierStore.fournisseurs" :key="fou.id" :value="fou.id">
                   {{ fou.raisonSociale }}
                 </option>
              </select>
              <svg class="input-icon" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>
            </div>
            
            <div class="alert-box alert-error mt-2" v-if="tierStore.fournisseurs.length === 0">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>
                <div>
                  <strong>Aucun fournisseur disponible</strong>
                  <span>Veuillez créer un Fournisseur dans l'annuaire des Tiers d'abord.</span>
                </div>
            </div>

            <div class="form-group mt-2">
              <label>Mode de Décaissement</label>
              <div class="payment-modes">
                <label class="mode-card" :class="{ active: createForm.mode === 'VIREMENT' }">
                  <input type="radio" v-model="createForm.mode" value="VIREMENT" class="hidden-radio"/>
                  <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="5" width="20" height="14" rx="2"></rect><line x1="2" y1="10" x2="22" y2="10"></line></svg>
                  <span>Virement</span>
                </label>
                <label class="mode-card" :class="{ active: createForm.mode === 'CHEQUE' }">
                  <input type="radio" v-model="createForm.mode" value="CHEQUE" class="hidden-radio"/>
                  <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="6" width="20" height="12" rx="2"></rect><circle cx="12" cy="12" r="2"></circle><path d="M6 12h.01M18 12h.01"></path></svg>
                  <span>Chèque</span>
                </label>
                <label class="mode-card" :class="{ active: createForm.mode === 'ESPECES' }">
                  <input type="radio" v-model="createForm.mode" value="ESPECES" class="hidden-radio"/>
                  <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2v20M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"></path></svg>
                  <span>Espèces</span>
                </label>
              </div>
            </div>

            <div class="form-row">
              <div class="form-group half">
                <label>Montant (XAF) <span class="req">*</span></label>
                <input v-model="createForm.montant" type="number" required class="input-large font-semibold text-right" placeholder="0.00" />
              </div>
            </div>

            <div class="form-group">
              <label>Motif de la dépense <span class="req">*</span></label>
              <textarea v-model="createForm.motif" rows="3" required placeholder="Description détaillée du décaissement..."></textarea>
            </div>
          </form>

          <!-- Panneau Latéral (Validation Temp Réel) -->
          <div class="modal-right">
             <div class="right-section validation-section">
                <div class="section-title-row">
                  <h4>VÉRIFICATION SÉCURITÉ</h4>
                  <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path></svg>
                </div>
                
                <div class="alert-box alert-info" v-if="createForm.montant < 500000">
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="16" x2="12" y2="12"></line><line x1="12" y1="8" x2="12.01" y2="8"></line></svg>
                  <div>
                    <strong>Flux Standard</strong>
                    <span>Validation DAF uniquement.</span>
                  </div>
                </div>

                <div class="alert-box alert-error" v-if="createForm.montant >= 500000">
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>
                  <div>
                    <strong>Seuil Dépassé (> 500k)</strong>
                    <span>Validation PDG obligatoire requise.</span>
                  </div>
                </div>

                <div class="alert-box alert-success" v-if="createForm.montant && createForm.fournisseurId">
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>
                  <div>
                    <strong>Formulaire complet</strong>
                    <span>Prêt pour soumission.</span>
                  </div>
                </div>
             </div>
          </div>
        </div>
        
        <div class="modal-footer bottom-bar">
          <button type="button" class="btn-text" @click="showCreateModal = false">Annuler</button>
          <button type="submit" form="create-decaissement-form" class="btn-primary-large" :disabled="!createForm.montant || !createForm.fournisseurId">
            Soumettre Demande
          </button>
        </div>
      </div>
    </div>

    <!-- MODAL : APPROBATION (INSPIRÉ MAQUETTE) -->
    <div v-if="showApproveModal" class="modal-backdrop fade-in">
      <div class="modal modal-approve">
        <div class="approve-header-group">
          <div class="icon-warning-rounded">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#d97706" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path><line x1="12" y1="9" x2="12" y2="13"></line><line x1="12" y1="17" x2="12.01" y2="17"></line></svg>
          </div>
          <div class="title-block">
            <h3>Confirm Disbursement</h3>
            <span>Security validation required for transaction #TXN-{{ activeDecaissement.id?.toString().padStart(4, '0') }}-BK</span>
          </div>
        </div>

        <div class="checklist-section">
          <span class="section-label">MANDATORY SECURITY CHECKLIST</span>
          <div class="checklist">
            <label class="check-item">
              <input type="checkbox" v-model="approveForm.checks[0]" />
              <div class="custom-check"></div>
              <span>I have verified the supporting documents</span>
            </label>
            <label class="check-item">
              <input type="checkbox" v-model="approveForm.checks[1]" />
              <div class="custom-check"></div>
              <span>I have verified the supplier identity</span>
            </label>
            <label class="check-item">
              <input type="checkbox" v-model="approveForm.checks[2]" />
              <div class="custom-check"></div>
              <span>Amounts match the invoice exactly</span>
            </label>
            <label class="check-item">
              <input type="checkbox" v-model="approveForm.checks[3]" />
              <div class="custom-check"></div>
              <span>Bank balance will remain sufficient after debit</span>
            </label>
          </div>
        </div>

        <div class="balance-calc-box">
          <div class="balance-row">
            <div class="b-col">
              <span class="label">CURRENT BALANCE</span>
              <strong class="val">{{ bankBalance.toLocaleString() }} XAF</strong>
            </div>
            <div class="arrow-ext">
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#9ca3af" stroke-width="2"><line x1="5" y1="12" x2="19" y2="12"></line><polyline points="12 5 19 12 12 19"></polyline></svg>
            </div>
            <div class="b-col text-right">
              <span class="label">POST-TRANSACTION</span>
              <strong class="val text-blue">{{ (bankBalance - activeDecaissement.montant).toLocaleString() }} XAF</strong>
            </div>
          </div>
          <div class="divider"></div>
          <div class="balance-row amounts-row">
            <span class="label">Transaction Amount:</span>
            <strong class="val-dark">-{{ activeDecaissement.montant.toLocaleString() }} XAF</strong>
          </div>
        </div>

        <div class="alert-box alert-danger-light">
          <div class="alert-icon-wrap">
            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="16" x2="12" y2="12"></line><line x1="12" y1="8" x2="12.01" y2="8"></line></svg>
          </div>
          <p>
            <strong>Attention:</strong> This action is irreversible. The account will be debited immediately and the funds will be queued for transfer. Ensure all details are accurate before executing.
          </p>
        </div>

        <div class="approve-footer">
          <button @click="showApproveModal = false" class="btn-outline-wide">Cancel Request</button>
          <button @click="submitApprove" class="btn-confirm-execute" :disabled="!approveForm.checks.every(c => c)">
            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>
            Confirm and Execute
          </button>
        </div>
      </div>
    </div>


    <!-- MODAL : REJET (INSPIRÉ MAQUETTE) -->
    <div v-if="showRejectModal" class="modal-backdrop fade-in">
      <div class="modal modal-reject">
        <div class="reject-header-group">
          <div class="icon-danger-rounded">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
          </div>
          <div class="title-block">
            <h3>Reject Disbursement</h3>
            <span>Transaction ID: TXN-{{ activeDecaissement.id?.toString().padStart(4, '0') }}-BK</span>
          </div>
        </div>

        <div class="radios-section">
          <span class="section-label">Select Rejection Reason <span class="req">*</span></span>
          <div class="radio-list">
            <label class="radio-item" :class="{ 'active': rejectForm.reason === 'missing_docs' }">
              <input type="radio" v-model="rejectForm.reason" value="missing_docs" class="hidden-radio" />
              <div class="custom-radio"></div>
              <span>Missing/Invalid Documents</span>
            </label>
            <label class="radio-item" :class="{ 'active': rejectForm.reason === 'insufficient_funds' }">
              <input type="radio" v-model="rejectForm.reason" value="insufficient_funds" class="hidden-radio" />
              <div class="custom-radio"></div>
              <span>Insufficient Funds</span>
            </label>
            <label class="radio-item" :class="{ 'active': rejectForm.reason === 'non_compliant' }">
              <input type="radio" v-model="rejectForm.reason" value="non_compliant" class="hidden-radio" />
              <div class="custom-radio"></div>
              <span>Non-compliant Supplier</span>
            </label>
            <label class="radio-item" :class="{ 'active': rejectForm.reason === 'incorrect_amount' }">
              <input type="radio" v-model="rejectForm.reason" value="incorrect_amount" class="hidden-radio" />
              <div class="custom-radio"></div>
              <span>Incorrect Amount</span>
            </label>
            <label class="radio-item" :class="{ 'active': rejectForm.reason === 'other' }">
              <input type="radio" v-model="rejectForm.reason" value="other" class="hidden-radio" />
              <div class="custom-radio"></div>
              <span>Other</span>
            </label>
          </div>
        </div>

        <div class="comments-section">
          <div class="label-row-mb">
            <span class="section-label">Additional Comments <span class="req">*</span></span>
            <span class="charlimit">MIN. 20 CHARACTERS</span>
          </div>
          <textarea v-model="rejectForm.comments" rows="3" placeholder="Please provide a detailed explanation for this rejection..." class="input-std"></textarea>
          <span class="help-text">This feedback will be shared directly with the Accountant to rectify the submission.</span>
        </div>

        <div class="alert-box alert-danger-light mt-1">
          <div class="alert-icon-wrap solid-red text-white">
            <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="4"><line x1="12" y1="16" x2="12" y2="12"></line><line x1="12" y1="8" x2="12.01" y2="8"></line></svg>
          </div>
          <p class="text-xs">
            <strong class="text-red">SYSTEM ACTION:</strong> Upon confirmation, the Accountant will be notified via email and the status will be set to <strong class="text-red underline">CANCELLED</strong>.
          </p>
        </div>

        <div class="approve-footer border-none">
          <button @click="showRejectModal = false" class="btn-text font-semibold">Go Back</button>
          <button @click="submitReject" class="btn-danger-solid" :disabled="rejectForm.comments.length < 20">Confirm Rejection</button>
        </div>
      </div>
    </div>

  </MainLayout>
</template>

<style scoped>
/* MAIN LAYOUT ELEMENTS */
.table-card { background: white; border-radius: 12px; border: 1px solid #e5e7eb; box-shadow: 0 1px 3px rgba(0,0,0,0.05); overflow: hidden; }
.data-table { width: 100%; border-collapse: collapse; }
.data-table th, .data-table td { padding: 1.15rem 1.5rem; text-align: left; border-bottom: 1px solid #f3f4f6; }
.data-table th { background-color: #f9fafb; font-size: 0.75rem; font-weight: 600; text-transform: uppercase; color: #6b7280; letter-spacing: 0.05em; }
.data-table td { font-size: 0.9rem; color: #4b5563; vertical-align: middle; }

.font-semibold { font-weight: 600; }
.text-dark { color: #111827; }
.text-muted { color: #6b7280; }
.text-sm { font-size: 0.75rem; }
.text-xs { font-size: 0.7rem; }
.text-right { text-align: right !important; }
.text-center { text-align: center !important; }
.italic { font-style: italic; }

.cell-stack { display: flex; flex-direction: column; gap: 0.15rem; }

.badge { display: inline-flex; padding: 0.25rem 0.625rem; border-radius: 20px; font-size: 0.7rem; font-weight: 600; text-transform: uppercase;}
.badge-success { background: #d1fae5; color: #065f46; }
.badge-danger { background: #fee2e2; color: #991b1b; }
.badge-warning { background: #fef3c7; color: #92400e; }

/* ACTIONS */
.btn-primary { display: flex; align-items: center; gap: 0.5rem; background-color: #2563eb; color: white; padding: 0.625rem 1rem; border-radius: 8px; border: none; font-size: 0.875rem; font-weight: 600; cursor: pointer; transition: background 0.15s; }
.btn-primary:hover { background-color: #1d4ed8; }

.actions-cell { display: flex; gap: 0.5rem; justify-content: center; }
.btn-icon { background: none; border: none; padding: 6px; border-radius: 6px; cursor: pointer; transition: background 0.15s; }
.btn-icon:hover { background: #f3f4f6; }
.text-green { color: #10b981; }
.text-red { color: #ef4444; }


/* COMMON MODAL */
.modal-backdrop { position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; background: rgba(55, 48, 48, 0.8); backdrop-filter: blur(2px); z-index: 1000; display: flex; align-items: center; justify-content: center; padding: 2rem;}
.fade-in { animation: fadeIn 0.2s ease-out; }
@keyframes fadeIn { from { opacity: 0; transform: scale(0.98); } to { opacity: 1; transform: scale(1); } }

.modal { background: white; border-radius: 12px; width: 100%; max-width: 450px; box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25); }
.modal-header { padding: 1.5rem; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #f3f4f6;}
.modal-header h3 { font-size: 1.125rem; font-weight: 600; color: #111827; }
.close-btn { background: none; border: none; color: #9ca3af; cursor: pointer;}
.modal-body { padding: 1.5rem; display: flex; flex-direction: column; gap: 1.25rem; }
.form-group { display: flex; flex-direction: column; gap: 0.5rem; }
.form-group label { font-size: 0.85rem; font-weight: 600; color: #111827; }
.req { color: #ef4444; }
.input-std { width: 100%; padding: 0.75rem 1rem; font-size: 0.95rem; border: 1px solid #d1d5db; border-radius: 8px; outline: none; transition: border-color 0.15s; }
.input-std:focus { border-color: #3b82f6; box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1); }
.modal-footer { display: flex; justify-content: flex-end; gap: 1rem; }
.btn-text { background: none; border: none; font-size: 0.875rem; color: #6b7280; cursor: pointer; }


/* STYLE EXTRA POUR MODALE DE CREATION EXPERTE */
.modal-lg { max-width: 950px; }
.bg-blue-light { background: #eff6ff; padding: 0.5rem; border-radius: 8px; }
.text-blue { color: #2563eb; }
.modal-split { display: grid; grid-template-columns: 1.5fr 1fr; border-bottom: 1px solid #f3f4f6;}
.modal-left { padding: 2.5rem 2rem; display: flex; flex-direction: column; gap: 1.5rem; }
.modal-right { background: #f9fafb; padding: 2.5rem 2rem; border-left: 1px solid #e5e7eb; display: flex; flex-direction: column; gap: 1.5rem; }
.form-row { display: flex; gap: 1rem; }
.half { flex: 1; }
.input-with-icon { position: relative; }
.input-icon { position: absolute; left: 1rem; top: 50%; transform: translateY(-50%); color: #9ca3af; margin-top: 10px; }
.input-huge { width: 100%; padding: 1rem 1rem 1rem 3rem; font-size: 1.25rem; border: 2px solid #3b82f6; border-radius: 8px; outline: none; box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.1); color: #111827; font-weight: 500; }
.input-large { width: 100%; padding: 0.875rem 1rem; font-size: 1rem; border: 1px solid #d1d5db; border-radius: 8px; outline: none; color: #111827; font-weight: 500; transition: border-color 0.15s; }
.input-large:focus { border-color: #3b82f6; box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1); }
textarea { width: 100%; padding: 0.875rem; font-size: 0.9rem; border: 1px solid #d1d5db; border-radius: 8px; outline: none; resize: vertical; color: #111827;}
textarea:focus { border-color: #3b82f6; box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1); }
.bottom-bar { padding: 1.5rem 2rem; background: white; border-radius: 0 0 12px 12px;}
.btn-primary-large { padding: 0.75rem 1.25rem; background: #2563eb; color: white; border: none; border-radius: 8px; font-weight: 600; font-size: 0.875rem; cursor: pointer; transition: background 0.15s;}
.btn-primary-large:hover { background: #1d4ed8; }
.btn-primary-large:disabled { opacity: 0.6; cursor: not-allowed; }

/* Payment Modes Cards */
.payment-modes { display: grid; grid-template-columns: repeat(3, 1fr); gap: 1rem; }
.mode-card { border: 1px solid #e5e7eb; border-radius: 8px; padding: 1rem; display: flex; flex-direction: column; align-items: center; gap: 0.5rem; cursor: pointer; color: #4b5563; transition: all 0.15s; position: relative; }
.mode-card.active { border-color: #2563eb; background: #eff6ff; color: #2563eb; box-shadow: 0 0 0 2px #bfdbfe; }
.mode-card span { font-size: 0.9rem; font-weight: 600; }
.hidden-radio { position: absolute; opacity: 0; }

.section-title-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; color: #6b7280; }
.section-title-row h4 { font-size: 0.75rem; font-weight: 700; letter-spacing: 0.05em; margin:0; }
.alert-box { display: flex; gap: 0.75rem; padding: 1rem; border-radius: 8px; font-size: 0.85rem; margin-bottom: 0.75rem; }
.alert-box div { display: flex; flex-direction: column; }
.alert-box strong { font-weight: 600; margin-bottom: 0.125rem; }
.alert-box span { font-style: italic; font-size: 0.75rem; }
.alert-error { background: #fee2e2; color: #b91c1c; border: 1px solid #fecaca; }
.alert-error svg { color: #ef4444; margin-top: 2px;}
.alert-info { background: #eff6ff; color: #1d4ed8; border: 1px solid #bfdbfe; }
.alert-info svg { color: #3b82f6; margin-top: 2px;}
.alert-success { background: #d1fae5; color: #047857; border: 1px solid #a7f3d0; }
.alert-success svg { color: #10b981; margin-top: 2px;}

/* EXPERT MODAL : APPROVE (MOCKUP 4) */
.modal-approve { max-width: 500px; padding: 2rem; display: flex; flex-direction: column; gap: 1.5rem; }

.approve-header-group { display: flex; align-items: center; gap: 1rem; }
.icon-warning-rounded { width: 48px; height: 48px; border-radius: 50%; background: #fef3c7; display: flex; align-items: center; justify-content: center; }
.title-block { display: flex; flex-direction: column; gap: 0.1rem; }
.title-block h3 { font-size: 1.25rem; font-weight: 700; color: #111827; }
.title-block span { font-size: 0.75rem; color: #6b7280; font-weight: 500;}

.section-label { font-size: 0.65rem; font-weight: 700; color: #9ca3af; letter-spacing: 0.05em; text-transform: uppercase; margin-bottom: 0.5rem; display: block; }

.checklist { border: 1px solid #f3f4f6; border-radius: 8px; overflow: hidden; }
.check-item { padding: 1rem; border-bottom: 1px solid #f3f4f6; display: flex; align-items: center; gap: 1rem; cursor: pointer; background: white;}
.check-item:last-child { border-bottom: none; }
.check-item input { display: none; }
.custom-check { width: 18px; height: 18px; border: 2px solid #d1d5db; border-radius: 4px; display: flex; align-items: center; justify-content: center; transition: all 0.15s;}
.check-item input:checked ~ .custom-check { background: #10b981; border-color: #10b981;  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='white' stroke-width='3' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='20 6 9 17 4 12'%3E%3C/polyline%3E%3C/svg%3E"); background-size: 12px; background-position: center; background-repeat: no-repeat; }
.check-item span { font-size: 0.85rem; color: #111827; font-weight: 500; }

.balance-calc-box { background: #f9fafb; border: 1px solid #f3f4f6; border-radius: 8px; padding: 1.25rem; display: flex; flex-direction: column; gap: 1rem; }
.balance-row { display: flex; justify-content: space-between; align-items: center; }
.b-col { display: flex; flex-direction: column; gap: 0.25rem; }
.b-col .label { font-size: 0.65rem; font-weight: 700; color: #9ca3af; letter-spacing: 0.05em; }
.b-col .val { font-size: 1.125rem; font-weight: 700; color: #111827; }
.b-col .text-blue { color: #2563eb; }
.divider { height: 1px; background: #e5e7eb; width: 100%; }
.amounts-row .label { font-size: 0.8rem; color: #6b7280; font-weight: 500; }
.amounts-row .val-dark { font-size: 0.875rem; font-weight: 700; color: #111827; }

.alert-danger-light { background: #fef2f2; border: 1px solid #fecaca; display: flex; gap: 0.75rem; padding: 1rem; border-radius: 8px; }
.alert-icon-wrap { color: #ef4444; margin-top: 2px; }
.alert-icon-wrap.solid-red { background: #ef4444; width: 18px; height: 18px; border-radius: 50%; display: flex; align-items: center; justify-content: center; }
.alert-danger-light p { font-size: 0.75rem; color: #b91c1c; line-height: 1.4; }
.text-red { color: #ef4444; }
.text-white { color: white; }
.underline { text-decoration: underline; }

.approve-footer { display: flex; gap: 1rem; padding-top: 0.5rem; }
.border-none { border: none !important; }
.btn-outline-wide { flex: 1; padding: 0.875rem; background: white; border: 1px solid #d1d5db; border-radius: 8px; color: #4b5563; font-weight: 600; font-size: 0.9rem; cursor: pointer; }
.btn-outline-wide:hover { background: #f9fafb; }
.btn-confirm-execute { flex: 1.5; padding: 0.875rem; background: #9ca3af; color: white; border: none; border-radius: 8px; font-weight: 600; font-size: 0.9rem; display: flex; align-items: center; justify-content: center; gap: 0.5rem; cursor: pointer; transition: background 0.15s;}
.btn-confirm-execute:not(:disabled) { background: #2563eb; }
.btn-confirm-execute:not(:disabled):hover { background: #1d4ed8; }


/* EXPERT MODAL : REJECT (MOCKUP 1) */
.modal-reject { max-width: 450px; padding: 2rem; display: flex; flex-direction: column; gap: 1.5rem; background: #ffffff; }

.reject-header-group { display: flex; align-items: center; gap: 1rem; }
.icon-danger-rounded { width: 48px; height: 48px; border-radius: 50%; background: #fee2e2; display: flex; align-items: center; justify-content: center; }
.icon-danger-rounded svg { stroke: #ef4444; }

.radio-list { border: 1px solid #f3f4f6; border-radius: 8px; overflow: hidden; display: flex; flex-direction: column; }
.radio-item { padding: 1rem; border-bottom: 1px solid #f3f4f6; display: flex; align-items: center; gap: 1rem; cursor: pointer; background: white; transition: background 0.15s;}
.radio-item:last-child { border-bottom: none; }
.radio-item.active { background: #f9fafb; }
.hidden-radio { display: none; }
.custom-radio { width: 18px; height: 18px; border: 2px solid #d1d5db; border-radius: 50%; display: flex; align-items: center; justify-content: center; }
.radio-item.active .custom-radio { border-color: #ef4444; border-width: 5px; }
.radio-item span { font-size: 0.85rem; color: #111827; font-weight: 500; }

.label-row-mb { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem; }
.label-row-mb .section-label { margin-bottom: 0; }
.charlimit { font-size: 0.6rem; font-weight: 700; color: #9ca3af; letter-spacing: 0.05em; }

textarea.input-std { resize: vertical; min-height: 80px; }
.help-text { font-size: 0.7rem; color: #6b7280; margin-top: 0.5rem; display: block; line-height: 1.4; }

.btn-danger-solid { flex: 1.5; padding: 0.875rem; background: #e11d48; color: white; border: none; border-radius: 8px; font-weight: 600; font-size: 0.9rem; cursor: pointer;}
.btn-danger-solid:hover { background: #be123c; }
.btn-danger-solid:disabled { opacity: 0.5; cursor: not-allowed; }
</style>
