<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import api from '../services/api'
import { useLangStore } from '../stores/lang.store'
import MainLayout from '../components/MainLayout.vue'
import Pagination from '../components/Pagination.vue'
import { useEncaissementStore } from '../stores/encaissement.store'
import { useTierStore } from '../stores/tier.store'
import { useCompteStore } from '../stores/compte.store'
import { useRoute, useRouter } from 'vue-router'
import { 
  FunnelIcon, 
  PlusIcon, 
  MagnifyingGlassIcon, 
  XMarkIcon, 
  BanknotesIcon, 
  EyeIcon, 
  ArrowDownTrayIcon,
  ExclamationTriangleIcon,
  InformationCircleIcon,
  CheckBadgeIcon,
  ArrowsRightLeftIcon,
  DocumentTextIcon,
  DevicePhoneMobileIcon,
  CreditCardIcon,
  ClockIcon
} from '@heroicons/vue/24/outline'

const route = useRoute()
const router = useRouter()
const store = useEncaissementStore()
const tierStore = useTierStore()
const compteStore = useCompteStore()
const showModal = ref(false)
const showPreview = ref(false)
const previewUrl = ref(null)
const selectedMode = ref('ESPECES')
const formError = ref('')

const openPreview = async (id) => {
  try {
    const response = await api.get(`/encaissements/${id}/recu/pdf`, { responseType: 'blob' })

    if (response.status === 204 || !response.data || response.data.size === 0) {
      alert("Erreur: Le serveur n'a renvoyé aucune donnée pour ce reçu.")
      return
    }

    const blob = new Blob([response.data], { type: 'application/pdf' })
    previewUrl.value = URL.createObjectURL(blob)
    showPreview.value = true
  } catch (err) {
    console.error('Erreur lors de la prévisualisation:', err)
  }
}

const closePreview = () => {
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
  }
  showPreview.value = false
  previewUrl.value = null
}

// Pagination
const currentPage = ref(1)
const itemsPerPage = 8

// Filtres
const filters = ref({
  search: '', // Référence ou Client
  clientId: '',
  dateDebut: '',
  dateFin: ''
})

const resetFilters = () => {
  filters.value = { search: '', clientId: '', dateDebut: '', dateFin: '' }
}

const filteredEncaissements = computed(() => {
  let list = store.encaissements
  
  if (filters.value.search) {
    const s = filters.value.search.toLowerCase()
    list = list.filter(e => 
      (e.reference && e.reference.toLowerCase().includes(s)) ||
      (e.nomClient && e.nomClient.toLowerCase().includes(s)) ||
      (e.id && e.id.toString().includes(s))
    )
  }

  if (filters.value.clientId) {
    list = list.filter(e => e.clientId == filters.value.clientId)
  }

  if (filters.value.dateDebut) {
    list = list.filter(e => e.dateEncaissement && e.dateEncaissement >= filters.value.dateDebut)
  }
  if (filters.value.dateFin) {
    list = list.filter(e => e.dateEncaissement && e.dateEncaissement <= filters.value.dateFin)
  }

  return list
})

const paginatedList = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage
  return filteredEncaissements.value.slice(start, start + itemsPerPage)
})
// Champs du formulaire dynamique
const form = ref({
  motif: '',
  montant: '',
  numeroFacture: '',
  clientId: '',
  moyenPaiement: 'VIREMENT',
  compteFinancierId: 1, // TODO: Dynamic later
  banqueEmettrice: '',
  numeroOperation: '',
  dateOperation: '',
  telephone: '',
  fraisTransaction: 0,
  datePrevisionnelleCompensation: ''
})

const selectedClientObj = computed(() => {
  if (!form.value.clientId) return null
  return tierStore.clients.find(c => c.id === form.value.clientId)
})

onMounted(async () => {
  await store.fetchEncaissements()
  await tierStore.fetchTiers()
  await compteStore.fetchComptes()

  // Gestion du pré-remplissage depuis Factures
  if (route.query.invoiceId) {
    form.value.clientId = parseInt(route.query.clientId)
    form.value.montant = parseFloat(route.query.amount)
    form.value.numeroFacture = route.query.numero
    form.value.motif = `Paiement facture ${route.query.numero}`
    
    showModal.value = true
    
    // Nettoyer l'URL
    router.replace({ path: '/encaissements' })
  }
})

const availableComptes = computed(() => {
  if (selectedMode.value === 'ESPECES') {
    return compteStore.caisses
  } else {
    return compteStore.banques
  }
})

// Auto-select first available account when mode changes
const watchMode = computed(() => selectedMode.value)

watch(watchMode, (newMode) => {
  const list = newMode === 'ESPECES' ? compteStore.caisses : compteStore.banques
  if (list.length > 0) {
    form.value.compteFinancierId = list[0].id
  } else {
    form.value.compteFinancierId = null
  }
})

const submitForm = async () => {
  formError.value = ''
  if (!form.value.montant || form.value.montant <= 0) {
    formError.value = "Le Montant est obligatoire et doit être supérieur à 0."
    return
  }
  if (!form.value.clientId) {
    formError.value = "Veuillez sélectionner un Client."
    return
  }
  if (!form.value.compteFinancierId) {
    formError.value = "Veuillez sélectionner le compte de destination (Banque/Caisse)."
    return
  }

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
      telephone: form.value.telephone || null,
      fraisTransaction: form.value.fraisTransaction || 0,
      datePrevisionnelleCompensation: form.value.datePrevisionnelleCompensation || null
    }

    // Gestion de l'affectation automatique par numéro de facture
    if (form.value.numeroFacture) {
      dataToSend.affectations = [{
        numeroFacture: form.value.numeroFacture,
        montantAffecte: form.value.montant
      }]
    }

    const newlyCreated = await store.createEncaissement(dataToSend)
    showModal.value = false
    form.value = { motif: '', montant: '', numeroFacture: '', clientId: '', moyenPaiement: 'VIREMENT', compteFinancierId: 1, banqueEmettrice: '', numeroOperation: '', dateOperation: '', telephone: '', fraisTransaction: 0, datePrevisionnelleCompensation: '' }
    
    // Auto-téléchargement du reçu pour marquer l'acte
    if (newlyCreated && newlyCreated.id) {
      await store.downloadReceipt(newlyCreated.id)
    }

  } catch(e) {
    console.error(e)
    formError.value = e.response?.data?.error || e.response?.data?.message || e.message || 'Une erreur est survenue lors de l\'enregistrement.'
  }
}

const getStatusClass = (statut) => {
  if (!statut) return ''
  const s = statut.toUpperCase()
  if (s.includes('VALIDEE') || s.includes('PAYE')) return 'badge-success'
  if (s.includes('REJETE') || s.includes('ANNULE')) return 'badge-danger'
  if (s.includes('ATTENTE') || s.includes('SOUMIS')) return 'badge-warning'
  return 'badge-info'
}

const langStore = useLangStore()
const t = computed(() => langStore.t)
</script>

<template>
  <MainLayout>
    <template #title>Encaissements</template>

    <template #actions>
      <button class="icon-btn show-on-mobile" @click="showMobileFilters = !showMobileFilters" title="Filtrer">
        <FunnelIcon class="w-5 h-5" />
      </button>
      <button @click="showModal = true" class="btn-primary hide-on-mobile">
        <PlusIcon class="w-4 h-4" />
        Nouveau Encaissement
      </button>
    </template>
    
    <div class="show-on-mobile w-100" style="margin-top: 1.5rem; margin-bottom: 1.5rem;">
      <button @click="showModal = true" class="btn-primary w-100" style="justify-content: center; padding: 0.75rem;">
        <PlusIcon class="w-4 h-4" />
        Nouveau Encaissement
      </button>
    </div>

    <!-- Barre de Filtres -->
    <div class="filter-bar" :class="{ 'mobile-collapsed': !showMobileFilters }">
      <div class="filter-group group-search">
        <div class="input-with-icon-left">
          <MagnifyingGlassIcon class="icon w-5 h-5 text-slate-400" />
          <input v-model="filters.search" type="text" placeholder="Référence ou Client..." class="filter-input-std" />
        </div>
      </div>
      
      <div class="filter-group">
        <select v-model="filters.clientId" class="filter-input-std">
          <option value="">Tous les clients</option>
          <option v-for="c in tierStore.clients" :key="c.id" :value="c.id">{{   c.raisonSociale   }}</option>
        </select>
      </div>

      <div class="filter-group-range">
        <input v-model="filters.dateDebut" type="date" class="filter-input-std" title="Date début" />
        <span class="to-text">à</span>
        <input v-model="filters.dateFin" type="date" class="filter-input-std" title="Date fin" />
      </div>

      <button @click="resetFilters" class="btn-clear-filters" title="Réinitialiser">
        <XMarkIcon class="w-4 h-4" />
      </button>
    </div>

    <div class="table-card">
      <div v-if="store.loading && store.encaissements.length === 0" class="loading-state">
        <span class="loader"></span> Chargement...
      </div>
      
      <div v-else-if="store.error" class="error-state">
        {{   store.error   }}
        <button @click="store.fetchEncaissements" class="btn-outline">Réessayer</button>
      </div>

      <div v-else class="table-scroll-container">
        <table class="data-table">
          <thead>
            <tr>
              <th>Référence</th>
              <th>Date</th>
              <th>Client</th>
              <th>Moyen</th>
              <th class="text-right">Montant (XAF)</th>
              <th>Saisi par</th>
              <th class="text-center">Statut</th>
              <th class="text-center">Action</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="filteredEncaissements.length === 0" class="empty-row text-center">
              <td colspan="7">
                <div class="empty-state">
                  <BanknotesIcon class="w-12 h-12 text-slate-300" />
                  <p>Aucun encaissement trouvé.</p>
                </div>
              </td>
            </tr>
            <tr v-for="e in paginatedList" :key="e.id">
              <td>
                <div class="motif-cell">
                  <span class="motif-text">{{   e.reference || 'N/A'   }}</span>
                  <span class="motif-sub">{{   e.numero   }}</span>
                </div>
              </td>
              <td>{{   new Date(e.dateEncaissement).toLocaleDateString()   }}</td>
              <td class="font-semibold text-dark">{{   e.nomClient   }}</td>
              <td>
                <div class="motif-cell">
                  <span>{{   e.moyenPaiement   }}</span>
                  <span v-if="e.fraisTransaction > 0" class="text-xs text-orange-500">Frais: {{   e.fraisTransaction   }}</span>
                </div>
              </td>
              <td class="text-right font-semibold">{{   e.montant?.toLocaleString()   }}</td>
              <td>
                <div class="validation-timeline">
                  <!-- Saisie -->
                  <div v-if="e.saisiParNom" class="val-pill val-saisie">
                    <ClockIcon class="w-3 h-3" />
                    <span>SAISIE PAR</span>
                    <div class="val-tooltip">
                      <div class="tooltip-header">Enregistrement Transaction</div>
                      <div class="tooltip-body">
                        <p><strong>Opérateur:</strong> {{ e.saisiParNom }}</p>
                        <p v-if="e.dateSaisie"><strong>Le:</strong> {{ new Date(e.dateSaisie).toLocaleString() }}</p>
                      </div>
                    </div>
                  </div>

                  <!-- Validation (si applicable) -->
                  <div v-if="e.valideParNom" class="val-pill val-rf">
                    <CheckBadgeIcon class="w-3 h-3" />
                    <span>VALIDÉ</span>
                    <div class="val-tooltip">
                      <div class="tooltip-header">Validation Transaction</div>
                      <div class="tooltip-body">
                        <p><strong>Validé par:</strong> {{ e.valideParNom }}</p>
                        <p v-if="e.dateValidation"><strong>Le:</strong> {{ new Date(e.dateValidation).toLocaleString() }}</p>
                      </div>
                    </div>
                  </div>
                </div>
              </td>
              <td class="text-center">
                <span class="badge" :class="getStatusClass(e.statut)">
                  {{   e.statut   }}
                </span>
              </td>
              <td class="text-center">
                <div class="actions-cell">
                  <button @click="openPreview(e.id)" class="icon-btn preview-btn" title="Aperçu">
                    <EyeIcon class="w-5 h-5" />
                  </button>
                  <button @click="store.downloadReceipt(e.id)" class="icon-btn download-btn" title="Télécharger">
                    <ArrowDownTrayIcon class="w-5 h-5" />
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Info Pagination -->
      <div class="table-footer-info" v-if="filteredEncaissements.length > 0">
        Affichage de {{   paginatedList.length   }} sur {{   filteredEncaissements.length   }} encaissement(s)
        <span v-if="filteredEncaissements.length < store.encaissements.length" class="text-blue italic">(Filtré)</span>
      </div>
    </div>

    <!-- Composant de Pagination Détaché -->
    <Pagination 
      v-if="filteredEncaissements.length > 0"
      :currentPage="currentPage" 
      :totalItems="filteredEncaissements.length" 
      :itemsPerPage="itemsPerPage" 
      @update:currentPage="currentPage = $event" 
    />

    <!-- Modal "SAISIE RAPIDE" experte -->
    <div v-if="showModal" class="modal-backdrop">
      <div class="modal modal-lg">
        
        <!-- Header Mode App -->
        <div class="modal-header">
          <h3>Nouveau Encaissement</h3>
          <button @click="showModal = false" class="close-btn"><XMarkIcon class="w-6 h-6" /></button>
        </div>

        <div class="modal-split">
          <!-- Colonne Gauche -->
          <form id="encaissement-form" @submit.prevent="submitForm" class="modal-left">

            <!-- Bandeau d'erreur métier -->
            <div v-if="formError" class="form-error-banner">
              <ExclamationTriangleIcon class="w-5 h-5" />
              <span>{{   formError   }}</span>
              <button type="button" @click="formError = ''" class="close-error-btn">&times;</button>
            </div>
            
            <div class="form-group search-group">
              <div class="label-row">
                <label>Client</label>
                
              </div>
              <div class="input-with-icon">
                <select v-model="form.clientId" class="input-huge" required autofocus>
                  <option value="" disabled>Sélectionner un Client...</option>
                  <option v-for="client in tierStore.clients" :key="client.id" :value="client.id">
                    {{   client.raisonSociale   }}
                  </option>
                </select>
                <MagnifyingGlassIcon class="input-icon w-5 h-5" />
              </div>

              <div v-if="selectedClientObj" class="p-2 border border-blue-100 bg-blue-50 rounded-md mt-1 flex justify-between items-center">
                <span class="text-xs font-semibold text-blue-700 uppercase">Reste à payer :</span>
                <span class="text-sm font-bold text-blue-800">{{   selectedClientObj.solde?.toLocaleString()   }} XAF</span>
              </div>
              
              <div class="alert-box alert-error mt-2" v-if="tierStore.clients.length === 0">
                <ExclamationTriangleIcon class="w-4 h-4" />
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
                  <ArrowsRightLeftIcon class="w-6 h-6" />
                  <span>Virement</span>
                </label>
                <label class="mode-card" :class="{ active: selectedMode === 'CHEQUE' }">
                  <input type="radio" v-model="selectedMode" value="CHEQUE" class="hidden-radio"/>
                  <DocumentTextIcon class="w-6 h-6" />
                  <span>Chèque</span>
                </label>
                <label class="mode-card" :class="{ active: selectedMode === 'ESPECES' }">
                  <input type="radio" v-model="selectedMode" value="ESPECES" class="hidden-radio"/>
                  <BanknotesIcon class="w-6 h-6" />
                  <span>Espèces</span>
                </label>
                <label class="mode-card" :class="{ active: selectedMode === 'ORANGE_MONEY' }">
                  <input type="radio" v-model="selectedMode" value="ORANGE_MONEY" class="hidden-radio"/>
                  <DevicePhoneMobileIcon class="w-6 h-6" />
                  <span>OM</span>
                </label>
                <label class="mode-card" :class="{ active: selectedMode === 'CARTE_BANCAIRE' }">
                  <input type="radio" v-model="selectedMode" value="CARTE_BANCAIRE" class="hidden-radio"/>
                  <CreditCardIcon class="w-6 h-6" />
                  <span>Carte</span>
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
                  <label>N° {{   selectedMode === 'CHEQUE' ? 'du Chèque' : 'Opération'   }} <span class="req">*</span></label>
                  <input v-model="form.numeroOperation" type="text" class="input-large" placeholder="Saisir la référence..." required />
                </div>
              </div>

              <div v-if="selectedMode === 'CHEQUE'" class="form-group mt-2">
                <label>Date sur le Chèque <span class="req">*</span></label>
                <input v-model="form.dateOperation" type="date" class="input-large" required />
              </div>

              <div v-if="selectedMode === 'ORANGE_MONEY' || selectedMode === 'CARTE_BANCAIRE'" class="form-row mt-2">
                <div class="form-group half" v-if="selectedMode === 'ORANGE_MONEY'">
                  <label>Téléphone <span class="req">*</span></label>
                  <input v-model="form.telephone" type="text" class="input-large" placeholder="Ex: 6XX XX XX XX" required />
                </div>
                <div class="form-group half">
                  <label>ID Transaction <span class="req">*</span></label>
                  <input v-model="form.numeroOperation" type="text" class="input-large" placeholder="Référence..." required />
                </div>
              </div>

              <div v-if="selectedMode === 'ORANGE_MONEY' || selectedMode === 'CARTE_BANCAIRE'" class="form-group mt-2">
                <label>Frais de transaction (XAF)</label>
                <input v-model="form.fraisTransaction" type="number" class="input-large" placeholder="0" />
                <span class="text-xs text-muted" v-if="form.montant > 0">
                  Montant Net: <strong>{{   (form.montant - (form.fraisTransaction || 0)).toLocaleString()   }} XAF</strong>
                </span>
              </div>

              <div v-if="selectedMode === 'CHEQUE'" class="form-group mt-2">
                <label>Date prévisionnelle de compensation</label>
                <input v-model="form.datePrevisionnelleCompensation" type="date" class="input-large" />
              </div>
            </div>

            <div class="form-row">
              <div class="form-group half">
                <label>Montant (XAF) <span class="req">*</span></label>
                <input v-model="form.montant" type="number" required class="input-large text-right font-semibold" placeholder="0.00" />
              </div>
              <div class="form-group half">
                <label>Lier à la Facture N° (Ex: FAC-...)</label>
                <input v-model="form.numeroFacture" type="text" class="input-large" placeholder="Saisir le code facture (Optionnel)" />
              </div>
            </div>

            <div class="form-row">
              <div class="form-group half" v-if="selectedMode">
                <label>Compte Financier <span class="req">*</span></label>
                <select v-model="form.compteFinancierId" class="input-std" required>
                  <option v-for="c in availableComptes" :key="c.id" :value="c.id">
                    {{   c.type === 'CAISSE' ? 'Compte de Caisse' : 'Compte de Banque'   }}
                  </option>
                </select>
                <div v-if="availableComptes.length === 0" class="text-xs text-red mt-1">
                  Aucun compte de ce type disponible.
                </div>
              </div>
            </div>

            <div class="form-group">
              <div class="label-row">
                <label>Note / Référence Interne</label>
              </div>
              <textarea v-model="form.motif" rows="3" placeholder="Ajouter un commentaire sur cet encaissement..." required></textarea>
            </div>

          </form>

          <!-- Colonne Droite: Contexte -->
          <div class="modal-right">
            
            <div class="right-section">
              <div class="section-title-row">
                <h4>CLIENTS RÉCENTS</h4>
                <ClockIcon class="w-4 h-4 text-slate-400" />
              </div>
              
              <div class="recent-list" v-if="tierStore.clients.length > 0">
                <div class="recent-item" v-for="client in tierStore.clients.slice(0, 3)" :key="client.id" @click="form.clientId = client.id">
                  <div>
                    <strong>{{   client.raisonSociale   }}</strong>
                    <span>Solde : {{   client.solde?.toLocaleString()   }} XAF</span>
                  </div>
                  <span class="text-muted" style="font-size: 0.75rem;">Choisir</span>
                </div>
              </div>
            </div>

            <div class="right-section validation-section">
              <div class="section-title-row">
                <h4>VALIDATION TEMPS RÉEL</h4>
                <CheckBadgeIcon class="w-4 h-4 text-blue-500" />
              </div>
              
              <div class="alert-box alert-error" v-if="!form.montant">
                <ExclamationTriangleIcon class="w-4 h-4" />
                <div>
                  <strong>Montant requis</strong>
                  <span>Veuillez indiquer la somme reçue.</span>
                </div>
              </div>

              <div class="alert-box alert-info" v-if="!form.factureId">
                <InformationCircleIcon class="w-4 h-4" />
                <div>
                  <strong>Recherche Facture</strong>
                  <span>Entrez un ID pour associer une facture.</span>
                </div>
              </div>

              <div class="alert-box alert-success" v-if="form.montant && form.clientId">
                <CheckBadgeIcon class="w-4 h-4" />
                <div>
                  <strong>Prêt à valider</strong>
                  <span>Les données minimales sont réunies.</span>
                </div>
              </div>
            </div>

          </div>
        </div>

        <!-- Footer Actions -->
        <div class="modal-footer pt-0 justify-end">
          <div class="actions-group">
            <button type="submit" form="encaissement-form" class="btn-primary-large" :disabled="store.loading">
              {{   store.loading ? 'En cours...' : 'Confirmer et Enregistrer'   }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Modale de Prévisualisation (iFrame) Globalisé -->
    <div v-if="showPreview" class="modal-backdrop-preview" @click.self="closePreview">
      <div class="preview-container">
        <div class="preview-header">
          <h3>Aperçu du Reçu d'Encaissement</h3>
          <button @click="closePreview" class="close-btn-preview">
            <XMarkIcon class="w-7 h-7" />
          </button>
        </div>
        <div class="preview-body">
          <iframe :src="previewUrl" width="100%" height="100%" frameborder="0"></iframe>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

<style scoped>
/* GENERAL TABLE LIST */
.motif-cell { display: flex; flex-direction: column; gap: 0.25rem; }
.motif-text { color: #111827; font-weight: 500;}
.facture-badge { font-size: 0.65rem; background: #eff6ff; color: #2563eb; padding: 2px 6px; border-radius: 4px; display: inline-block; align-self: flex-start; font-weight: 600;}

/* BOUTON ACTIONS LIST */
.icon-btn { background: #f3f4f6; border: none; padding: 0.4rem; border-radius: 6px; color: #4b5563; cursor: pointer; transition: 0.15s;}
.icon-btn:hover { background: #e5e7eb; color: #111827; }

.btn-primary { display: flex; align-items: center; gap: 0.5rem; background-color: #2563eb; color: white; padding: 0.625rem 1rem; border-radius: 8px; border: none; font-size: 0.875rem; font-weight: 600; cursor: pointer; transition: background 0.15s, transform 0.1s; }
.btn-primary:hover { background-color: #1d4ed8; }
.btn-primary:active { transform: scale(0.98); }


.actions-cell {
  display: flex;
  justify-content: center;
  gap: 1rem;
}

/* MODAL SAISIE RAPIDE EXPERTE */
.modal-backdrop { position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; background: rgba(17, 24, 39, 0.6); backdrop-filter: blur(4px); z-index: 1000; display: flex; align-items: center; justify-content: center; padding: 2rem;}
.modal-lg { width: 100%; max-width: 950px; background: white; border-radius: 12px; box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25); display: flex; flex-direction: column; max-height: 90vh;}
.modal-header { padding: 1.5rem; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #f3f4f6;}

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
.modal-footer { padding: 1.5rem 2rem; border-top: 1px solid #e5e7eb; display: flex; justify-content: flex-end; align-items: center; background: white; border-radius: 0 0 16px 16px; }
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

/* Bandeau d'erreur métier dans la modale */
.form-error-banner {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.875rem 1rem;
  background: linear-gradient(135deg, #fef2f2, #fee2e2);
  border: 1px solid #fecaca;
  border-radius: 10px;
  color: #b91c1c;
  font-size: 0.875rem;
  font-weight: 500;
  animation: shakeIn 0.3s ease-out;
}
.form-error-banner svg { flex-shrink: 0; color: #ef4444; }
.form-error-banner span { flex: 1; }
.close-error-btn {
  background: none; border: none; color: #b91c1c; font-size: 1.25rem;
  cursor: pointer; padding: 0 0.25rem; opacity: 0.6; transition: opacity 0.15s;
}
.close-error-btn:hover { opacity: 1; }

@keyframes shakeIn {
  0% { transform: translateX(-8px); opacity: 0; }
  50% { transform: translateX(4px); }
  100% { transform: translateX(0); opacity: 1; }
}

/* RESPONSIVE DESIGN */
@media (max-width: 768px) {
  .modal-lg {
    width: 95vw;
    margin: 1rem;
    max-height: 95vh;
  }
  .modal-split {
    grid-template-columns: 1fr;
    display: flex;
    flex-direction: column;
  }
  .modal-left, .modal-right {
    padding: 1.5rem 1rem;
  }
  .payment-modes {
    grid-template-columns: 1fr 1fr;
  }
  .form-row {
    flex-direction: column;
  }
  .actions-group {
    flex-direction: column;
    width: 100%;
  }
  .btn-primary-large {
    width: 100%;
    justify-content: center;
  }
}

/* VALIDATION TOOLTIPS & PILLS (COMMON) */
.validation-timeline {
  display: flex;
  gap: 0.4rem;
  flex-wrap: wrap;
  align-items: center;
}

.val-pill {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.2rem 0.5rem;
  border-radius: 9999px;
  font-size: 0.65rem;
  font-weight: 700;
  cursor: help;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid transparent;
}

.val-pill span {
  line-height: 1;
}

.val-saisie {
  background-color: #f8fafc;
  color: #64748b;
  border-color: #e2e8f0;
}

.val-rf {
  background-color: #ecfdf5;
  color: #059669;
  border-color: #a7f3d0;
}

.val-pill:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

/* Tooltip Styling */
.val-tooltip {
  visibility: hidden;
  opacity: 0;
  position: absolute;
  bottom: 125%;
  left: 50%;
  transform: translateX(-50%) translateY(10px);
  background: rgba(30, 41, 59, 0.95);
  backdrop-filter: blur(8px);
  color: white;
  padding: 0;
  border-radius: 0.75rem;
  width: max-content;
  min-width: 180px;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.3), 0 10px 10px -5px rgba(0, 0, 0, 0.2);
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  z-index: 100;
  overflow: hidden;
  pointer-events: none;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.val-pill:hover .val-tooltip {
  visibility: visible;
  opacity: 1;
  transform: translateX(-50%) translateY(0);
}

.tooltip-header {
  background: rgba(255, 255, 255, 0.1);
  padding: 0.5rem 0.75rem;
  font-size: 0.75rem;
  font-weight: 700;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  color: #93c5fd;
}

.tooltip-body {
  padding: 0.6rem 0.75rem;
  font-size: 0.7rem;
  font-weight: 400;
  line-height: 1.4;
}

.tooltip-body p {
  margin: 0;
  display: flex;
  justify-content: space-between;
  gap: 1rem;
}

.tooltip-body p strong {
  color: #cbd5e1;
}

.val-tooltip::after {
  content: "";
  position: absolute;
  top: 100%;
  left: 50%;
  margin-left: -6px;
  border-width: 6px;
  border-style: solid;
  border-color: #1e293b transparent transparent transparent;
}

/* DARK MODE OVERRIDES */
body.dark-mode .val-pill-empty {
  color: #475569;
}

body.dark-mode .val-saisie {
  background-color: rgba(148, 163, 184, 0.1);
  color: #94a3b8;
  border-color: rgba(148, 163, 184, 0.2);
}

body.dark-mode .val-rf {
  background-color: rgba(16, 185, 129, 0.1);
  color: #34d399;
  border-color: rgba(16, 185, 129, 0.2);
}

body.dark-mode .val-tooltip {
  background: #0f172a;
  border: 1px solid #1e293b;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
}

body.dark-mode .val-tooltip::after {
  border-color: #0f172a transparent transparent transparent;
}

body.dark-mode .tooltip-header {
  background: rgba(255, 255, 255, 0.03);
  border-bottom-color: #1e293b;
}
</style>
