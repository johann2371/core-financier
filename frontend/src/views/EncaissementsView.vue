<script setup>
import { ref, computed, onMounted } from 'vue'
import MainLayout from '../components/MainLayout.vue'
import Pagination from '../components/Pagination.vue'
import { useEncaissementStore } from '../stores/encaissement.store'
import { useTierStore } from '../stores/tier.store'

const store = useEncaissementStore()
const tierStore = useTierStore()
const showModal = ref(false)
const selectedMode = ref('VIREMENT') // Default helper

// Pagination
const currentPage = ref(1)
const itemsPerPage = 8

const paginatedList = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage
  return store.encaissements.slice(start, start + itemsPerPage)
})
// Champs du formulaire dynamique
const form = ref({
  motif: '',
  montant: '',
  factureId: null,
  clientId: '',
  moyenPaiement: 'VIREMENT',
  compteFinancierId: 1, // TODO: Dynamic later
  banqueEmettrice: '',
  numeroOperation: '',
  dateOperation: '',
  telephone: ''
})

onMounted(async () => {
  await store.fetchEncaissements()
  await tierStore.fetchTiers()
})

const submitForm = async () => {
  try {
    const dataToSend = {
      clientId: form.value.clientId,
      montant: form.value.montant,
      moyenPaiement: selectedMode.value,
      compteFinancierId: form.value.compteFinancierId,
      reference: form.value.motif || '',
      banqueEmettrice: form.value.banqueEmettrice || null,
      numeroOperation: form.value.numeroOperation || null,
      dateOperation: form.value.dateOperation || null,
      telephone: form.value.telephone || null
    }
    const newlyCreated = await store.createEncaissement(dataToSend)
    showModal.value = false
    form.value = { motif: '', montant: '', factureId: null, clientId: '', moyenPaiement: 'VIREMENT', compteFinancierId: 1, banqueEmettrice: '', numeroOperation: '', dateOperation: '', telephone: '' }
    
    // Auto-téléchargement du reçu pour marquer l'acte
    if (newlyCreated && newlyCreated.id) {
      await store.downloadReceipt(newlyCreated.id)
    }

  } catch(e) {
    console.error(e)
  }
}
</script>

<template>
  <MainLayout>
    <template #title>Encaissements</template>

    <template #actions>
      <button @click="showModal = true" class="btn-primary">
        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="7 10 12 15 17 10"></polyline><line x1="12" y1="15" x2="12" y2="3"></line></svg>
        Saisie Rapide
        <span class="shortcut">Ctrl + E</span>
      </button>
    </template>

    <div class="table-card">
      <div v-if="store.loading && store.encaissements.length === 0" class="loading-state">
        <span class="loader"></span> Chargement...
      </div>
      
      <div v-else-if="store.error" class="error-state">
        {{ store.error }}
        <button @click="store.fetchEncaissements" class="btn-outline">Réessayer</button>
      </div>

      <table v-else class="data-table">
        <thead>
          <tr>
            <th>Référence</th>
            <th>Date</th>
            <th>Détails & Mode</th>
            <th class="text-right">Montant</th>
            <th class="text-center">Action</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="store.encaissements.length === 0" class="empty-row text-center">
            <td colspan="5">
              <div class="empty-state">
                <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="empty-icon"><rect x="2" y="6" width="20" height="12" rx="2"></rect><path d="M12 12h.01"></path><path d="M17 12h.01"></path><path d="M7 12h.01"></path></svg>
                <p>Aucun encaissement récent.</p>
                <span>Utilisez "Saisie Rapide" pour déclarer une entrée.</span>
              </div>
            </td>
          </tr>
          
          <tr v-for="item in paginatedList" :key="item.id">
            <td class="font-semibold text-dark">#ENC-{{ item.id?.toString().padStart(4, '0') }}</td>
            <td class="text-muted">{{ new Date(item.dateEncaissement).toLocaleDateString() }}</td>
            <td>
              <div class="motif-cell">
                <span class="motif-text">{{ item.motif }}</span>
                <span class="facture-badge" v-if="item.factureId">Lié à Fac-{{ item.factureId }}</span>
              </div>
            </td>
            <td class="text-right font-semibold text-dark">{{ item.montant?.toLocaleString() }} XAF</td>
            <td class="text-center">
              <button class="btn-icon" title="Télécharger le Reçu PDF">
                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2v4a2 2 0 0 0 2 2h4"></path><path d="M10.4 12.6a2 2 0 1 1 3-3L8 14l-5 1.5L4.5 9l5.5-5.5a2 2 0 1 1 3 3L8.5 11l-3 1"></path><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path></svg>
              </button>
            </td>
          </tr>
        </tbody>
      </table>
      
      <!-- Composant de Pagination -->
      <Pagination 
        v-if="store.encaissements.length > 0"
        :currentPage="currentPage" 
        :totalItems="store.encaissements.length" 
        :itemsPerPage="itemsPerPage" 
        @update:currentPage="currentPage = $event" 
      />
    </div>

    <!-- Modal "SAISIE RAPIDE" experte -->
    <div v-if="showModal" class="modal-backdrop">
      <div class="modal modal-lg">
        
        <!-- Header Mode App -->
        <div class="modal-header">
          <div class="modal-title-group">
            <div class="modal-icon bg-blue-light">
              <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#2563eb" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="7 10 12 15 17 10"></polyline><line x1="12" y1="15" x2="12" y2="3"></line></svg>
            </div>
            <div>
              <h3>Saisie Rapide : Nouvel Encaissement</h3>
              <span class="subtitle">MODE PERFORMANCE • <strong class="text-blue">ENC-2026-MOD</strong></span>
            </div>
          </div>
          <div class="modal-close-group">
            <span class="shortcut-tip">Quitter <kbd>Echap</kbd></span>
            <button @click="showModal = false" class="close-btn"><svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg></button>
          </div>
        </div>

        <div class="modal-split">
          <!-- Colonne Gauche -->
          <form id="encaissement-form" @submit.prevent="submitForm" class="modal-left">
            
            <div class="form-group search-group">
              <div class="label-row">
                <label>Client <kbd>ALT+C</kbd></label>
                <span class="status-text blue-text">Auto-complétion active...</span>
              </div>
              <div class="input-with-icon">
                <select v-model="form.clientId" class="input-huge" required autofocus>
                  <option value="" disabled>Sélectionner un Client...</option>
                  <option v-for="client in tierStore.clients" :key="client.id" :value="client.id">
                    {{ client.raisonSociale }}
                  </option>
                </select>
                <svg class="input-icon" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
              </div>
              
              <div class="alert-box alert-error mt-2" v-if="tierStore.clients.length === 0">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>
                <div>
                  <strong>Aucun client disponible</strong>
                  <span>Veuillez créer un Client dans l'annuaire des Tiers d'abord.</span>
                </div>
              </div>
            </div>

            <div class="form-group">
              <label>Mode d'Encaissement</label>
              <div class="payment-modes" style="grid-template-columns: repeat(4, 1fr); gap: 0.5rem;">
                <label class="mode-card" :class="{ active: selectedMode === 'VIREMENT' }">
                  <input type="radio" v-model="selectedMode" value="VIREMENT" class="hidden-radio"/>
                  <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="5" width="20" height="14" rx="2"></rect><line x1="2" y1="10" x2="22" y2="10"></line></svg>
                  <span>Virement</span>
                </label>
                <label class="mode-card" :class="{ active: selectedMode === 'CHEQUE' }">
                  <input type="radio" v-model="selectedMode" value="CHEQUE" class="hidden-radio"/>
                  <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="6" width="20" height="12" rx="2"></rect><circle cx="12" cy="12" r="2"></circle><path d="M6 12h.01M18 12h.01"></path></svg>
                  <span>Chèque</span>
                </label>
                <label class="mode-card" :class="{ active: selectedMode === 'ESPECES' }">
                  <input type="radio" v-model="selectedMode" value="ESPECES" class="hidden-radio"/>
                  <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2v20M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"></path></svg>
                  <span>Espèces</span>
                </label>
                <label class="mode-card" :class="{ active: selectedMode === 'ORANGE_MONEY' }">
                  <input type="radio" v-model="selectedMode" value="ORANGE_MONEY" class="hidden-radio"/>
                  <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="5" y="2" width="14" height="20" rx="2" ry="2"></rect><line x1="12" y1="18" x2="12.01" y2="18"></line></svg>
                  <span>Or. Money</span>
                </label>
              </div>
            </div>

            <!-- Champs dynamiques selon Mode -->
            <div class="dynamic-fields fade-in-fast" v-if="selectedMode !== 'ESPECES'">
              <div v-if="selectedMode === 'VIREMENT' || selectedMode === 'CHEQUE'" class="form-row mt-2">
                <div class="form-group half">
                  <label>Banque <span class="req">*</span></label>
                  <input v-model="form.banqueEmettrice" type="text" class="input-large" :placeholder="selectedMode === 'CHEQUE' ? 'Banque émettrice...' : 'Banque d\'origine...'" required />
                </div>
                <div class="form-group half">
                  <label>N° {{ selectedMode === 'CHEQUE' ? 'du Chèque' : 'Opération' }} <span class="req">*</span></label>
                  <input v-model="form.numeroOperation" type="text" class="input-large" placeholder="Saisir la référence..." required />
                </div>
              </div>

              <div v-if="selectedMode === 'CHEQUE'" class="form-group mt-2">
                <label>Date sur le Chèque <span class="req">*</span></label>
                <input v-model="form.dateOperation" type="date" class="input-large" required />
              </div>

              <div v-if="selectedMode === 'ORANGE_MONEY'" class="form-row mt-2">
                <div class="form-group half">
                  <label>Téléphone <span class="req">*</span></label>
                  <input v-model="form.telephone" type="text" class="input-large" placeholder="Ex: 6XX XX XX XX" required />
                </div>
                <div class="form-group half">
                  <label>ID Transaction <span class="req">*</span></label>
                  <input v-model="form.numeroOperation" type="text" class="input-large" placeholder="ID OM..." required />
                </div>
              </div>
            </div>

            <div class="form-row">
              <div class="form-group half">
                <label>Montant (XAF) <span class="req">*</span></label>
                <input v-model="form.montant" type="number" required class="input-large text-right font-semibold" placeholder="0.00" />
              </div>
              <div class="form-group half">
                <label>Lier Facture N°</label>
                <input v-model="form.factureId" type="number" class="input-large" placeholder="Optionnel" />
              </div>
            </div>

            <div class="form-group">
              <div class="label-row">
                <label>Note / Référence Interne <kbd>ALT+N</kbd></label>
              </div>
              <textarea v-model="form.motif" rows="3" placeholder="Ajouter un commentaire sur cet encaissement..." required></textarea>
            </div>

          </form>

          <!-- Colonne Droite: Contexte -->
          <div class="modal-right">
            
            <div class="right-section">
              <div class="section-title-row">
                <h4>CLIENTS RÉCENTS</h4>
                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>
              </div>
              
              <div class="recent-list" v-if="tierStore.clients.length > 0">
                <div class="recent-item" v-for="client in tierStore.clients.slice(0, 3)" :key="client.id" @click="form.clientId = client.id">
                  <div>
                    <strong>{{ client.raisonSociale }}</strong>
                    <span>Solde : {{ client.solde?.toLocaleString() }} XAF</span>
                  </div>
                  <kbd>Select</kbd>
                </div>
              </div>
            </div>

            <div class="right-section validation-section">
              <div class="section-title-row">
                <h4>VALIDATION TEMPS RÉEL</h4>
                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>
              </div>
              
              <div class="alert-box alert-error" v-if="!form.montant">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>
                <div>
                  <strong>Montant requis</strong>
                  <span>Veuillez indiquer la somme reçue.</span>
                </div>
              </div>

              <div class="alert-box alert-info" v-if="!form.factureId">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="16" x2="12" y2="12"></line><line x1="12" y1="8" x2="12.01" y2="8"></line></svg>
                <div>
                  <strong>Recherche Facture</strong>
                  <span>Entrez un ID pour associer une facture.</span>
                </div>
              </div>

              <div class="alert-box alert-success" v-if="form.montant && form.clientId">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>
                <div>
                  <strong>Prêt à valider</strong>
                  <span>Les données minimales sont réunies.</span>
                </div>
              </div>
            </div>

          </div>
        </div>

        <!-- Footer Actions -->
        <div class="modal-footer pt-0">
          <button type="button" class="btn-text-icon">
            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 6 2 18 2 18 9"></polyline><path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"></path><rect x="6" y="14" width="12" height="8"></rect></svg>
            Aperçu du reçu <kbd>ALT+P</kbd>
          </button>
          
          <div class="actions-group">
            <button type="button" class="btn-outline">
              Ajouter et Nouveau <kbd>Ctrl+N</kbd>
            </button>
            <button type="submit" form="encaissement-form" class="btn-primary-large" :disabled="store.loading || !form.montant">
              {{ store.loading ? 'En cours...' : 'Confirmer et Enregistrer' }}
              <kbd class="dark-kbd">Ctrl+Entrée</kbd>
            </button>
          </div>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

<style scoped>
/* GENERAL TABLE LIST */
.table-card {
  background: white; border-radius: 12px;
  border: 1px solid #e5e7eb; box-shadow: 0 1px 3px rgba(0,0,0,0.05); overflow: hidden;
}
.data-table { width: 100%; border-collapse: collapse; }
.data-table th, .data-table td { padding: 1.25rem 1.5rem; text-align: left; border-bottom: 1px solid #f3f4f6; }
.data-table th { background-color: #f9fafb; font-size: 0.75rem; font-weight: 600; text-transform: uppercase; color: #6b7280; letter-spacing: 0.05em; }
.data-table td { font-size: 0.9rem; color: #4b5563; vertical-align: middle; }
.text-right { text-align: right !important; }
.text-center { text-align: center !important; }
.font-semibold { font-weight: 600; }
.text-dark { color: #111827; }
.text-muted { color: #6b7280; }

.motif-cell { display: flex; flex-direction: column; gap: 0.25rem; }
.motif-text { color: #111827; font-weight: 500;}
.facture-badge { font-size: 0.65rem; background: #eff6ff; color: #2563eb; padding: 2px 6px; border-radius: 4px; display: inline-block; align-self: flex-start; font-weight: 600;}

/* BOUTON ACTIONS LIST */
.btn-primary { display: flex; align-items: center; gap: 0.5rem; background-color: #2563eb; color: white; padding: 0.625rem 1rem; border-radius: 8px; border: none; font-size: 0.875rem; font-weight: 600; cursor: pointer; transition: background 0.15s, transform 0.1s; }
.btn-primary:hover { background-color: #1d4ed8; }
.btn-primary:active { transform: scale(0.98); }
.btn-primary .shortcut { background: rgba(255,255,255,0.2); border-radius: 4px; padding: 2px 6px; font-size: 0.65rem; font-weight: 500;}

/* MODAL SAISIE RAPIDE EXPERTE */
.modal-backdrop { position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; background: rgba(17, 24, 39, 0.6); backdrop-filter: blur(4px); z-index: 1000; display: flex; align-items: center; justify-content: center; padding: 2rem;}
.modal-lg { width: 100%; max-width: 950px; background: white; border-radius: 16px; box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25); display: flex; flex-direction: column; max-height: 90vh;}

.modal-header { padding: 1.5rem 2rem; border-bottom: 1px solid #e5e7eb; background: #f9fafb; display: flex; justify-content: space-between; align-items: center; border-radius: 16px 16px 0 0; }
.modal-title-group { display: flex; align-items: center; gap: 1rem; }
.bg-blue-light { background: #eff6ff; padding: 0.5rem; border-radius: 8px; }
.modal-title-group h3 { font-size: 1.125rem; font-weight: 700; color: #111827; margin-bottom: 0.1rem; }
.modal-title-group .subtitle { font-size: 0.7rem; color: #6b7280; font-weight: 600; letter-spacing: 0.05em; }
.text-blue { color: #2563eb; }

.modal-close-group { display: flex; align-items: center; gap: 1rem; }
.shortcut-tip { font-size: 0.75rem; color: #6b7280; font-weight: 500; display: flex; align-items: center; gap: 0.5rem; }
kbd { font-family: inherit; background: #e5e7eb; color: #4b5563; padding: 2px 6px; border-radius: 4px; font-size: 0.65rem; font-weight: 600; border-bottom: 1px solid #d1d5db; }
.close-btn { background: none; border: none; color: #9ca3af; cursor: pointer; transition: color 0.15s; }
.close-btn:hover { color: #111827; }

.modal-split { display: grid; grid-template-columns: 1.5fr 1fr; overflow-y: auto; flex: 1; min-height: 0; }

/* LEFT COL (FORM) */
.modal-left { padding: 2rem; display: flex; flex-direction: column; gap: 1.5rem; }

.form-group { display: flex; flex-direction: column; gap: 0.5rem; position: relative;}
.form-row { display: flex; gap: 1rem; }
.half { flex: 1; }

.label-row { display: flex; justify-content: space-between; align-items: baseline; }
.label-row label { font-size: 0.85rem; font-weight: 600; color: #111827; display: flex; align-items: center; gap: 0.5rem; }
.blue-text { color: #2563eb; font-size: 0.75rem; font-weight: 500; font-style: italic; }
.req { color: #ef4444; }

.input-with-icon { position: relative; }
.input-icon { position: absolute; left: 1rem; top: 50%; transform: translateY(-50%); color: #9ca3af; }

.input-huge { width: 100%; padding: 1rem 1rem 1rem 3rem; font-size: 1.25rem; border: 2px solid #3b82f6; border-radius: 8px; outline: none; box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.1); color: #111827; font-weight: 500; }
.input-large { width: 100%; padding: 0.875rem 1rem; font-size: 1rem; border: 1px solid #d1d5db; border-radius: 8px; outline: none; color: #111827; font-weight: 500; transition: border-color 0.15s; }
.input-large:focus { border-color: #3b82f6; box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1); }
textarea { width: 100%; padding: 0.875rem; font-size: 0.9rem; border: 1px solid #d1d5db; border-radius: 8px; outline: none; resize: vertical; color: #111827;}
textarea:focus { border-color: #3b82f6; box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1); }

/* Autocomplete UX Dropdown */
.autocomplete-dropdown { background: #eff6ff; border: 1px solid #bfdbfe; border-radius: 8px; overflow: hidden; margin-top: 0.25rem; animation: slideDown 0.2s ease-out; }
@keyframes slideDown { from { opacity: 0; transform: translateY(-10px); } to { opacity: 1; transform: translateY(0); } }
.ac-header { display: flex; justify-content: space-between; padding: 0.5rem 1rem; background: #dbeafe; font-size: 0.75rem; font-weight: 600; color: #1e40af; }
.ac-header kbd { background: #3b82f6; color: white; border-bottom: none; }
.ac-item { padding: 1rem; display: flex; justify-content: space-between; align-items: center; cursor: pointer; }
.ac-info { display: flex; flex-direction: column; gap: 0.25rem; }
.ac-info strong { font-size: 1rem; color: #111827; }
.ac-info span { font-size: 0.75rem; color: #6b7280; }
.ac-due { color: #2563eb; font-size: 0.875rem; font-weight: 600; }

/* Payment Modes Cards */
.payment-modes { display: grid; grid-template-columns: repeat(3, 1fr); gap: 1rem; }
.mode-card { border: 1px solid #e5e7eb; border-radius: 8px; padding: 1rem; display: flex; flex-direction: column; align-items: center; gap: 0.5rem; cursor: pointer; color: #4b5563; transition: all 0.15s; position: relative; }
.mode-card.active { border-color: #2563eb; background: #eff6ff; color: #2563eb; box-shadow: 0 0 0 2px #bfdbfe; }
.mode-card span:nth-child(3) { font-size: 0.9rem; font-weight: 600; }
.mode-card .shortcut { font-size: 0.65rem; color: #9ca3af; font-weight: 500; }
.hidden-radio { position: absolute; opacity: 0; }

/* RIGHT COL (CONTEXT) */
.modal-right { padding: 2rem; background: #f9fafb; border-left: 1px solid #e5e7eb; display: flex; flex-direction: column; gap: 2.5rem; }
.section-title-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; color: #6b7280; }
.section-title-row h4 { font-size: 0.75rem; font-weight: 700; letter-spacing: 0.05em; }

.recent-list { display: flex; flex-direction: column; gap: 0.5rem; }
.recent-item { background: white; border: 1px solid #e5e7eb; border-radius: 8px; padding: 0.875rem 1rem; display: flex; justify-content: space-between; align-items: center; cursor: pointer; transition: border-color 0.15s; }
.recent-item:hover { border-color: #d1d5db; }
.recent-item div { display: flex; flex-direction: column; gap: 0.2rem; }
.recent-item strong { font-size: 0.85rem; color: #111827; font-weight: 600; }
.recent-item span { font-size: 0.7rem; color: #9ca3af; }

.validation-section { display: flex; flex-direction: column; gap: 0.75rem; }
.alert-box { display: flex; gap: 0.75rem; padding: 1rem; border-radius: 8px; font-size: 0.85rem; }
.alert-box div { display: flex; flex-direction: column; }
.alert-box strong { font-weight: 600; margin-bottom: 0.125rem; }
.alert-box span { font-style: italic; font-size: 0.75rem; }

.alert-error { background: #fee2e2; color: #b91c1c; border: 1px solid #fecaca; }
.alert-error svg { color: #ef4444; margin-top: 2px;}
.alert-info { background: #eff6ff; color: #1d4ed8; border: 1px solid #bfdbfe; }
.alert-info svg { color: #3b82f6; margin-top: 2px;}
.alert-success { background: #d1fae5; color: #047857; border: 1px solid #a7f3d0; }
.alert-success svg { color: #10b981; margin-top: 2px;}


/* FOOTER */
.modal-footer { padding: 1.5rem 2rem; border-top: 1px solid #e5e7eb; display: flex; justify-content: space-between; align-items: center; background: white; border-radius: 0 0 16px 16px; }
.pt-0 { padding-top: 1.5rem; }

.btn-text-icon { background: none; border: none; display: flex; align-items: center; gap: 0.5rem; color: #4b5563; font-size: 0.85rem; font-weight: 600; cursor: pointer; transition: color 0.15s; }
.btn-text-icon:hover { color: #111827; }

.actions-group { display: flex; gap: 1rem; align-items: center; }

.btn-outline { padding: 0.75rem 1rem; border: 1px solid #d1d5db; background: white; color: #4b5563; font-weight: 600; border-radius: 8px; cursor: pointer; font-size: 0.875rem; display: flex; align-items: center; gap: 0.5rem;}
.btn-outline:hover { background: #f9fafb; color: #111827; }

.btn-primary-large { padding: 0.75rem 1.25rem; background: #2563eb; color: white; border: none; border-radius: 8px; font-weight: 600; font-size: 0.875rem; cursor: pointer; display: flex; align-items: center; gap: 0.5rem; box-shadow: 0 1px 2px rgba(37, 99, 235, 0.5); transition: background 0.15s;}
.btn-primary-large:hover { background: #1d4ed8; }
.btn-primary-large:disabled { opacity: 0.6; cursor: not-allowed; }
.dark-kbd { background: rgba(0,0,0,0.2) !important; color: white !important; border-bottom: none !important;}
</style>
