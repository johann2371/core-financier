<script setup>
import { ref, computed, onMounted } from 'vue'
import MainLayout from '../components/MainLayout.vue'
import Pagination from '../components/Pagination.vue'
import { useFactureStore } from '../stores/facture.store'
import { useTierStore } from '../stores/tier.store'
import { useRoute, useRouter } from 'vue-router'
import { useLangStore } from '../stores/lang.store'
import { useAuthStore } from '../stores/auth.store'
import api from '../services/api'
import {
  FunnelIcon,
  PlusIcon,
  DocumentPlusIcon,
  MagnifyingGlassIcon,
  XMarkIcon,
  BanknotesIcon,
  EyeIcon,
  ArrowDownTrayIcon,
  DocumentTextIcon,
  TrashIcon,
  ExclamationTriangleIcon
} from '@heroicons/vue/24/outline'

const store = useFactureStore()
const tierStore = useTierStore()
const authStore = useAuthStore()
const langStore = useLangStore()
const t = computed(() => langStore.t)
const route = useRoute()
const router = useRouter()

const showModal = ref(false)
const showPreview = ref(false)
const previewUrl = ref(null)
const formError = ref('')

const openPreview = async (id) => {
  try {
    const response = await api.get(`/factures/${id}/pdf`, { responseType: 'blob' })
    
    // Vérifier si le blob est bien un PDF (parfois le serveur renvoie du JSON/HTML en cas d'erreur)
    const contentType = response.headers['content-type'] || ''
    if (!contentType.includes('application/pdf')) {
      // La réponse n'est pas un PDF, c'est probablement un message d'erreur
      const text = await response.data.text()
      console.error('Réponse non-PDF reçue:', text)
      alert("Le serveur n'a pas renvoyé de PDF. Réponse : " + text.substring(0, 200))
      return
    }

    if (!response.data || response.data.size === 0) {
      alert("Erreur: Le serveur n'a renvoyé aucune donnée pour ce PDF.")
      return
    }

    const blob = new Blob([response.data], { type: 'application/pdf' })
    previewUrl.value = URL.createObjectURL(blob)
    showPreview.value = true
  } catch (err) {
    console.error('Erreur lors de la prévisualisation:', err)
    let errorMsg = 'Erreur lors de la prévisualisation du PDF.'
    if (err.response) {
      if (err.response.status === 403) {
        errorMsg = "Accès refusé. Vous n'avez pas les droits pour voir ce PDF."
      } else if (err.response.status === 401) {
        errorMsg = "Session expirée. Reconnectez-vous et réessayez."
      } else if (err.response.data instanceof Blob) {
        try {
          const text = await err.response.data.text()
          errorMsg = 'Erreur serveur: ' + text.substring(0, 200)
        } catch(e) { /* ignored */ }
      }
    } else if (!err.response) {
      errorMsg = 'Le serveur backend ne répond pas. Vérifiez qu\'il est démarré.'
    }
    alert(errorMsg)
  }
}

const closePreview = () => {
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
  }
  showPreview.value = false
  previewUrl.value = null
}
const activeTab = ref('TOUS') // TOUS, VENTE, ACHAT

// Pagination
const currentPage = ref(1)
const itemsPerPage = 10

// Filtres
const filters = ref({
  search: '',
  tiersId: '',
  statut: '',
  dateDebut: '',
  dateFin: ''
})
const showMobileFilters = ref(false)

const resetFilters = () => {
  filters.value = {
    search: '',
    tiersId: '',
    statut: '',
    dateDebut: '',
    dateFin: ''
  }
}

const setTab = (tab) => {
  activeTab.value = tab
  currentPage.value = 1
}

// Formulaire Dymanique Facture
const form = ref({
  type: 'VENTE',
  tiersId: '',
  lignes: [
    { designation: '', quantite: '', prixUnitaire: '' }
  ]
})

// Déclencheurs au montage
onMounted(async () => {
  await store.fetchFactures()
  await tierStore.fetchTiers()

  // Si l'URL demande l'ouverture de la modale directement
  if (route.query.create) {
    showModal.value = true
    form.value.type = route.query.create === 'ACHAT' ? 'ACHAT' : 'VENTE'
    router.replace({ path: '/factures' })
  }
})

// Logique ajout/suppression de lignes
const addLigne = () => {
  form.value.lignes.push({ designation: '', quantite: '', prixUnitaire: '' })
}
const removeLigne = (index) => {
  if (form.value.lignes.length > 1) {
    form.value.lignes.splice(index, 1)
  }
}

// Données calculées
const tiersDisponibles = computed(() => {
  return form.value.type === 'VENTE' ? tierStore.clients : tierStore.fournisseurs
})

const totalHT = computed(() => {
  return form.value.lignes.reduce((sum, ligne) => sum + (ligne.quantite * ligne.prixUnitaire), 0)
})

const totalTTC = computed(() => {
  return totalHT.value * 1.1925 // TVA 19.25% hardcodée selon backend
})

const filteredFactures = computed(() => {
  let list = store.factures
  
  // Restriction Caissier : Uniquement les ventes
  if (authStore.userRole === 'CAISSIER') {
    list = list.filter(f => f.type === 'VENTE')
  }

  if (activeTab.value !== 'TOUS') {
    list = list.filter(f => f.type === activeTab.value)
  }
  
  // Recherche Numero
  if (filters.value.search) {
    const s = filters.value.search.toLowerCase()
    list = list.filter(f => f.numero && f.numero.toLowerCase().includes(s))
  }

  // Tiers
  if (filters.value.tiersId) {
    list = list.filter(f => f.tiersId == filters.value.tiersId)
  }

  // Statut
  if (filters.value.statut) {
    list = list.filter(f => f.statut === filters.value.statut)
  }

  // Dates
  if (filters.value.dateDebut) {
    list = list.filter(f => f.dateFacture && f.dateFacture >= filters.value.dateDebut)
  }
  if (filters.value.dateFin) {
    list = list.filter(f => f.dateFacture && f.dateFacture <= filters.value.dateFin)
  }

  return list
})

const paginatedList = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage
  return filteredFactures.value.slice(start, start + itemsPerPage)
})

// Soumission
const submitForm = async () => {
  formError.value = ''
  
  // Validation manuelle supplémentaire
  if (!form.value.tiersId) {
    formError.value = form.value.type === 'VENTE' ? "Veuillez sélectionner un Client valide." : "Veuillez sélectionner un Fournisseur valide."
    return
  }
  
  if (form.value.lignes.length === 0) {
    formError.value = "Veuillez ajouter au moins une ligne d'article à la facture."
    return
  }

  // Vérifier chaque ligne
  const ligneInvalide = form.value.lignes.find(l => !l.designation || !l.quantite || l.prixUnitaire === null || l.prixUnitaire === '')
  if (ligneInvalide) {
    formError.value = "Veuillez remplir correctement toutes les lignes d'articles (Description, Qté et Prix Unitaire requis)."
    return
  }

  try {
    const dataToSend = {
      type: form.value.type,
      tiersId: form.value.tiersId,
      lignes: form.value.lignes
    }
    const newlyCreated = await store.createFacture(dataToSend)
    showModal.value = false
    
    // Auto-téléchargement de la facture après génération
    if (newlyCreated && newlyCreated.id) {
      await store.downloadPdf(newlyCreated.id)
    }
    
    // Réinitialisation conditionnelle selon le besoin
    form.value = {
      type: 'VENTE',
      tiersId: '',
      lignes: [{ designation: '', quantite: 1, prixUnitaire: 0 }]
    }
  } catch(e) {
    console.error('Erreur création facture', e)
    formError.value = e.response?.data?.error || e.response?.data?.message || e.message || 'Erreur lors de la création de la facture.'
  }
}
const formatFullDate = (dateStr) => {
  if (!dateStr) return 'Date inconnue'
  const date = new Date(dateStr)
  return date.toLocaleString('fr-FR', {
    day: '2-digit',
    month: 'long',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<template>
  <MainLayout>
    <template #title>{{   t("factures.titre")   }}</template>

    <template #actions>
      <button class="icon-btn show-on-mobile" @click="showMobileFilters = !showMobileFilters" title="Filtrer">
        <FunnelIcon class="w-5 h-5" />
      </button>
      <button v-if="authStore.userRole !== 'CAISSIER'" @click="showModal = true" class="btn-primary hide-on-mobile">
        <DocumentPlusIcon class="w-5 h-5" />
        Nouvelle Facture
      </button>
    </template>

    <div class="show-on-mobile w-100" style="margin-top: 1.5rem; margin-bottom: 1.5rem;">
      <button @click="showModal = true" class="btn-primary w-100" style="justify-content: center; padding: 0.75rem;">
        <DocumentPlusIcon class="w-5 h-5" />
        Nouvelle Facture
      </button>
    </div>

    <!-- Navigation par Onglets -->
    <div v-if="authStore.userRole !== 'CAISSIER'" class="tabs-nav" :class="{ 'mobile-collapsed': !showMobileFilters }">
      <button class="tab-btn" :class="{ active: activeTab === 'TOUS' }" @click="setTab('TOUS')">Toutes ({{   store.factures.length   }})</button>
      <button class="tab-btn" :class="{ active: activeTab === 'VENTE' }" @click="setTab('VENTE')">Ventes ({{   store.ventes.length   }})</button>
      <button class="tab-btn" :class="{ active: activeTab === 'ACHAT' }" @click="setTab('ACHAT')">Achats ({{   store.achats.length   }})</button>
    </div>

    <!-- Barre de Filtres -->
    <div class="filter-bar" :class="{ 'mobile-collapsed': !showMobileFilters }">
      <div class="filter-group group-search">
        <div class="input-with-icon-left">
          <MagnifyingGlassIcon class="icon w-5 h-5 text-slate-400" />
          <input v-model="filters.search" type="text" placeholder="N° Facture..." class="filter-input-std" />
        </div>
      </div>
      
      <div class="filter-group">
        <select v-model="filters.tiersId" class="filter-input-std">
          <option value="">Tous les tiers</option>
          <option v-for="t in tierStore.tiers" :key="t.id" :value="t.id">{{   t.raisonSociale   }}</option>
        </select>
      </div>

      <div class="filter-group">
        <select v-model="filters.statut" class="filter-input-std">
          <option value="">Tous les statuts</option>
          <option value="EN_ATTENTE_PAIEMENT">En Attente</option>
          <option value="PARTIELLEMENT_PAYEE">Partiel</option>
          <option value="SOLDEE">Soldée</option>
          <option value="VALIDEE">Validée (Legacy)</option>
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

    <!-- Tableau -->
    <div class="table-card">
      <div v-if="store.loading && store.factures.length === 0" class="loading-state">Chargement...</div>
      
      <div v-else-if="store.error" class="error-state">
        {{   store.error   }}
        <button @click="store.fetchFactures" class="btn-outline">Réessayer</button>
      </div>

      <div v-else class="table-scroll-container">
        <table class="data-table">
          <thead>
            <tr>
              <th>N° Facture</th>
              <th>Type</th>
              <th>Tiers Associé</th>
              <th>Date</th>
              <th>Statut</th>
              <th>Opérateur</th>
              <th class="text-right">Total TTC (XAF)</th>
              <th class="text-center">Action</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="filteredFactures.length === 0" class="empty-row text-center">
              <td colspan="7">Aucune facture trouvée.</td>
            </tr>
            
            <tr v-for="item in paginatedList" :key="item.id">
              <td class="font-semibold text-dark">{{   item.numero   }}</td>
              <td>
                <span class="badge" :class="item.type === 'VENTE' ? 'badge-vente' : 'badge-achat'">
                  {{   item.type   }}
                </span>
              </td>
              <td>
                <div class="motif-cell">
                  <span class="motif-text">{{   item.tiersNom || 'Inconnu'   }}</span>
                </div>
              </td>
              <td>{{   item.dateFacture ? new Date(item.dateFacture).toLocaleDateString() : 'Non définie'   }}</td>
              <td>
                <span class="badge" :class="{
                  'badge-attente': item.statut === 'EN_ATTENTE_PAIEMENT' || item.statut === 'VALIDEE',
                  'badge-paye': item.statut === 'SOLDEE',
                  'badge-partiel': item.statut === 'PARTIELLEMENT_PAYEE'
                }">
                  {{   (item.statut === 'VALIDEE' ? 'EN ATTENTE PAIEMENT' : item.statut).replace(/_/g, ' ')   }}
                </span>
              </td>
              <td>
                <div class="operator-pills">
                  <!-- Pill Saisie -->
                  <div class="op-pill-wrapper">
                    <span class="op-pill-mini blue">
                      <span class="op-label">Saisie par</span>
                      <span class="op-name">{{ item.creeParNom || 'Système' }}</span>
                    </span>
                    <div class="op-tooltip">
                      <div class="tooltip-header">Détails de Saisie</div>
                      <div class="tooltip-row">
                        <span class="t-label">Opérateur :</span>
                        <span class="t-value">{{ item.creeParNom }}</span>
                      </div>
                      <div class="tooltip-row">
                        <span class="t-label">Date/Heure :</span>
                        <span class="t-value">{{ formatFullDate(item.dateSaisie) }}</span>
                      </div>
                    </div>
                  </div>

                  <!-- Pill Validation -->
                  <div v-if="item.valideParNom" class="op-pill-wrapper">
                    <span class="op-pill-mini green">
                      <span class="op-label">Validé par</span>
                      <span class="op-name">{{ item.valideParNom }}</span>
                    </span>
                    <div class="op-tooltip">
                      <div class="tooltip-header">Détails de Validation</div>
                      <div class="tooltip-row">
                        <span class="t-label">Validateur :</span>
                        <span class="t-value">{{ item.valideParNom }}</span>
                      </div>
                      <div class="tooltip-row">
                        <span class="t-label">Date/Heure :</span>
                        <span class="t-value">{{ formatFullDate(item.dateValidation) }}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </td>
              <td class="text-right font-semibold text-dark">{{   item.montantTtc?.toLocaleString() || '0'   }}</td>
              <td class="text-center">
                  <div class="actions-cell">
                    <button v-if="item.statut !== 'SOLDEE' && item.type === 'VENTE'" class="icon-btn" @click="router.push({ path: '/encaissements', query: { invoiceId: item.id, clientId: item.tiersId, amount: item.resteAPayer || item.montantTtc, numero: item.numero } })" title="Enregistrer le paiement" style="color: #16a34a; background: #dcfce7;">
                      <BanknotesIcon class="w-4 h-4" />
                    </button>
                    <button class="icon-btn preview-btn" @click="openPreview(item.id)" title="Aperçu">
                      <EyeIcon class="w-4 h-4" />
                    </button>
                    <button class="icon-btn download-btn" @click="store.downloadPdf(item.id)" title="Télécharger PDF">
                      <ArrowDownTrayIcon class="w-4 h-4" />
                    </button>
                  </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-if="filteredFactures.length > 0" class="table-footer-info">
        Affichage de {{   paginatedList.length   }} sur {{   filteredFactures.length   }} facture(s) 
        <span v-if="activeTab !== 'TOUS'"> (Filtre: {{   activeTab   }})</span>
      </div>
    </div>

    <!-- Composant de Pagination Détaché -->
    <Pagination 
      v-if="filteredFactures.length > 0"
      :currentPage="currentPage" 
      :totalItems="filteredFactures.length" 
      :itemsPerPage="itemsPerPage" 
      @update:currentPage="currentPage = $event" 
    />

    <!-- Modale de Prévisualisation (iFrame) Globalisée -->
    <div v-if="showPreview" class="modal-backdrop-preview" @click.self="closePreview">
      <div class="preview-container">
        <div class="preview-header">
          <h3>Prévisualisation de la Facture</h3>
          <button @click="closePreview" class="close-btn-preview">
            <XMarkIcon class="w-7 h-7" />
          </button>
        </div>
        <div class="preview-body">
          <iframe v-if="previewUrl" :src="previewUrl" width="100%" height="100%" frameborder="0"></iframe>
        </div>
      </div>
    </div>

    <!-- Modale Création Facture (Complexe: Lignes) -->
    <div v-if="showModal" class="modal-backdrop fade-in">
      <div class="modal modal-large">
        <div class="modal-header">
          <h3>Nouvelle Facture</h3>
          <button @click="showModal = false" class="close-btn"><XMarkIcon class="w-6 h-6" /></button>
        </div>
        
        <form @submit.prevent="submitForm" class="modal-body complex-body">

          <!-- Bandeau d'erreur métier -->
          <div v-if="formError" class="form-error-banner">
            <ExclamationTriangleIcon class="w-5 h-5" />
            <span>{{   formError   }}</span>
            <button type="button" @click="formError = ''" class="close-error-btn">&times;</button>
          </div>

          <div class="form-row">
            <div class="form-group half">
               <label class="hide-on-mobile">Type de Facture <span class="req">*</span></label>
               <select v-model="form.type" required class="input-std" @change="form.tiersId = ''">
                 <option value="VENTE">Facture de Vente (Client)</option>
                 <option value="ACHAT">Facture d'Achat (Fournisseur)</option>
               </select>
            </div>
            <div class="form-group half input-with-icon">
               <label class="hide-on-mobile">{{   form.type === 'VENTE' ? 'Client Associé' : 'Fournisseur Associé'   }} <span class="req">*</span></label>
               <select v-model="form.tiersId" required class="input-std">
                 <option value="" disabled>{{   form.type === 'VENTE' ? 'Sélectionner un Client...' : 'Sélectionner un Fournisseur...'   }}</option>
                 <option v-for="t in tiersDisponibles" :key="t.id" :value="t.id">{{   t.raisonSociale   }}</option>
               </select>
            </div>
          </div>

          <div class="section-divider mt-2">Détails des Articles/Lignes</div>
          
          <div class="lines-container">
            <div class="line-header form-row hide-on-mobile">
               <div class="line-col design-col"><label>Description</label></div>
               <div class="line-col qty-col"><label>Qté</label></div>
               <div class="line-col price-col"><label>Prix Unitaire (XAF)</label></div>
               <div class="line-col total-col"><label>Total HT</label></div>
               <div class="line-col act-col"></div>
            </div>

            <div v-for="(ligne, index) in form.lignes" :key="index" class="line-item form-row">
               <div class="line-col design-col">
                 <input v-model="ligne.designation" type="text" required class="input-std" placeholder="Nom de l'article ou service..." />
               </div>
               <div class="line-col qty-col">
                 <input v-model.number="ligne.quantite" type="number" required min="1" step="1" class="input-std text-center" placeholder="Qté" />
               </div>
               <div class="line-col price-col">
                 <input v-model.number="ligne.prixUnitaire" type="number" required min="0" step="25" class="input-std text-right" placeholder="Prix Unitaire (XAF)" />
               </div>
               <div class="line-col total-col v-center hide-on-mobile">
                 <span class="font-semibold text-dark">{{   (ligne.quantite * ligne.prixUnitaire).toLocaleString()   }}</span>
               </div>
                <div class="line-col act-col v-center">
                  <button type="button" class="icon-btn-danger" @click="removeLigne(index)" :disabled="form.lignes.length === 1">
                    <TrashIcon class="w-4 h-4" />
                  </button>
                </div>
            </div>
            
            <button type="button" class="btn-outline-dashed mt-2" @click="addLigne">
              <PlusIcon class="w-4 h-4" />
              Ajouter une Ligne
            </button>
          </div>

          <!-- Totalisation -->
          <div class="totaux-card mt-3">
             <div class="tot-row"><span>Total HT</span> <span>{{   totalHT.toLocaleString()   }} XAF</span></div>
             <div class="tot-row"><span>TVA (19.25%)</span> <span>{{   (totalHT * 0.1925).toLocaleString()   }} XAF</span></div>
             <div class="tot-row total-ttc"><span>TTC Estimé</span> <span>{{   totalTTC.toLocaleString()   }} XAF</span></div>
          </div>
          
          <div v-if="store.error" class="form-error">{{   store.error   }}</div>

          <div class="modal-footer pt-3 pb-0">
             <button type="button" class="btn-text" @click="showModal = false">{{   t("common.annuler")   }}</button>
             <button type="submit" class="btn-primary" :disabled="store.loading">
               {{   store.loading ? 'Création...' : 'Générer la Facture'   }}
             </button>
          </div>
        </form>
      </div>
    </div>

  </MainLayout>
</template>

<style scoped>
/* MAIN LAYOUT ELEMENTS */
.v-center { display: flex; align-items: center; justify-content: center; }

.icon-btn { background: #f3f4f6; border: none; padding: 0.4rem; border-radius: 6px; color: #4b5563; cursor: pointer; transition: 0.15s;}
.icon-btn:hover { background: #e5e7eb; color: #111827; }
.icon-btn-danger { background: none; border: none; color: #9ca3af; cursor: pointer; padding: 0.25rem; border-radius: 4px; transition: 0.1s;}
.icon-btn-danger:hover:not(:disabled) { background: #fee2e2; color: #ef4444; }
.icon-btn-danger:disabled { opacity: 0.4; cursor: not-allowed; }

.actions-cell { display: flex; gap: 1rem; justify-content: center; }
.preview-btn:hover { color: #2563eb; background: #eff6ff; }
.download-btn:hover { color: #059669; background: #ecfdf5; }

/* TABS NAV */
.tabs-nav { display: flex; gap: 1rem; border-bottom: 1px solid #e5e7eb; margin-bottom: 1.5rem; }
.tab-btn { background: none; border: none; padding: 0.75rem 0.5rem; font-size: 0.875rem; font-weight: 600; color: #6b7280; border-bottom: 2px solid transparent; cursor: pointer; }
.tab-btn:hover { color: #111827; }
.tab-btn.active { color: #2563eb; border-color: #2563eb; }

/* ACTIONS */
.btn-primary { display: flex; align-items: center; gap: 0.5rem; background-color: #2563eb; color: white; padding: 0.625rem 1rem; border-radius: 8px; border: none; font-size: 0.875rem; font-weight: 600; cursor: pointer; transition: background 0.15s; }
.btn-primary:hover { background-color: #1d4ed8; }


/* MODAL & LIGNES FACTURE */
.modal-backdrop { position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; background: rgba(17, 24, 39, 0.6); backdrop-filter: blur(2px); z-index: 1000; display: flex; align-items: center; justify-content: center; padding: 2rem;}
.fade-in { animation: fadeIn 0.15s ease-out; }
@keyframes fadeIn { from { opacity: 0; transform: scale(0.98); } to { opacity: 1; transform: scale(1); } }

.modal { background: white; border-radius: 12px; width: 100%; box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25); display: flex; flex-direction: column; max-height: 90vh; }
.modal-large { max-width: 800px; }
.modal-header { padding: 1.5rem; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #f3f4f6;}
.modal-header h3 { font-size: 1.125rem; font-weight: 600; color: #111827; margin:0;}
.close-btn { background: none; border: none; color: #9ca3af; cursor: pointer;}
.complex-body { padding: 1.5rem; display: flex; flex-direction: column; gap: 1rem; overflow-y: auto; }
.section-divider { border-bottom: 1px solid #e5e7eb; padding-bottom: 0.5rem; font-size: 0.8rem; font-weight: 700; color: #4b5563; text-transform: uppercase; letter-spacing: 0.05em; }
.mt-2 { margin-top: 1rem; }
.mt-3 { margin-top: 1.5rem; }

.form-group { display: flex; flex-direction: column; gap: 0.4rem; }
.form-row { display: flex; gap: 1rem; }
.half { flex: 1; }
.form-group label { font-size: 0.85rem; font-weight: 600; color: #111827; margin:0;}
.req { color: #ef4444; }
.input-std { width: 100%; padding: 0.75rem 1rem; font-size: 0.95rem; border: 1px solid #d1d5db; border-radius: 8px; outline: none; transition: border-color 0.15s; background:white; color:#111827;}
.input-std:focus { border-color: #3b82f6; box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1); }

/* GRID LIGNES FACTURE */
.lines-container { background: #f9fafb; border: 1px solid #e5e7eb; border-radius: 8px; padding: 1rem; }
.line-header label { font-size: 0.75rem; font-weight: 600; color: #6b7280; text-transform: uppercase; }
.line-item { margin-top: 0.5rem; align-items: center;}
.design-col { flex: 4; }
.qty-col { flex: 1; min-width: 80px;}
.price-col { flex: 1.5; min-width: 120px;}
.total-col { flex: 1.5; min-width: 120px; font-size: 0.9rem;}
.act-col { width: 32px; }

.btn-outline-dashed { display: flex; align-items: center; gap: 0.5rem; width: 100%; padding: 0.75rem; justify-content: center; background: none; border: 2px dashed #d1d5db; border-radius: 8px; color: #4b5563; font-weight: 600; cursor: pointer; transition: 0.15s; font-size: 0.875rem;}
.btn-outline-dashed:hover { border-color: #9ca3af; background: white; color: #111827; }

.totaux-card { background: #eff6ff; padding: 1rem 1.5rem; border-radius: 8px; display: flex; flex-direction: column; gap: 0.5rem; align-items: flex-end;}
.tot-row { display: flex; justify-content: space-between; width: 250px; font-size: 0.9rem; color: #4b5563;}
.total-ttc { font-size: 1.25rem; font-weight: 700; color: #1d4ed8; padding-top: 0.5rem; border-top: 1px solid #bfdbfe; margin-top: 0.25rem;}

.modal-footer { display: flex; justify-content: flex-end; gap: 1rem; }
.btn-text { background: none; border: none; font-size: 0.875rem; color: #6b7280; font-weight:600; cursor: pointer; }
.form-error { color: #dc2626; font-size: 0.875rem; padding: 0.5rem; background: #fee2e2; border-radius: 6px; }

/* Bandeau d'erreur métier dans la modale */
.form-error-banner {
  display: flex; align-items: center; gap: 0.75rem;
  padding: 0.875rem 1rem; background: linear-gradient(135deg, #fef2f2, #fee2e2);
  border: 1px solid #fecaca; border-radius: 10px; color: #b91c1c;
  font-size: 0.875rem; font-weight: 500; animation: shakeIn 0.3s ease-out;
}
.form-error-banner svg { flex-shrink: 0; color: #ef4444; }
.form-error-banner span { flex: 1; }
.close-error-btn { background: none; border: none; color: #b91c1c; font-size: 1.25rem; cursor: pointer; padding: 0 0.25rem; opacity: 0.6; transition: opacity 0.15s; }
.close-error-btn:hover { opacity: 1; }
@keyframes shakeIn { 0% { transform: translateX(-8px); opacity: 0; } 50% { transform: translateX(4px); } 100% { transform: translateX(0); opacity: 1; } }

/* RESPONSIVE DESIGN */
@media (max-width: 768px) {
  .modal-large {
    width: 95vw;
    margin: 1rem;
    max-height: 95vh;
  }
  .complex-body {
    padding: 1rem;
  }
  .form-row {
    flex-direction: column;
  }
  .lines-container {
    padding: 0.25rem;
    overflow: visible;
  }
  .line-item {
    min-width: 0 !important;
    display: grid !important;
    grid-template-areas: 
      "design design design"
      "qty price act";
    grid-template-columns: 1fr 1fr auto;
    gap: 0.5rem;
    padding-bottom: 0.75rem;
    margin-bottom: 0.75rem;
    border-bottom: 1px dotted #d1d5db;
  }
  .line-header {
    display: none !important;
  }
  .design-col { 
    grid-area: design; 
    width: 100%; 
  }
  .qty-col { 
    grid-area: qty; 
    width: 100%; 
    min-width: auto;
  }
  .price-col { 
    grid-area: price; 
    width: 100%; 
    min-width: auto;
  }
  .act-col { 
    grid-area: act;
    display: flex;
    justify-content: center;
    align-items: center;
  }
  .totaux-card {
    align-items: stretch;
  }
  .tot-row {
    width: 100%;
  }
  .tabs-nav {
    flex-wrap: wrap;
  }
}
/* OPERATOR PILLS & TOOLTIPS (Premium) */
.operator-pills {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.op-pill-wrapper {
  position: relative;
  display: inline-flex;
}

.op-pill-mini {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px;
  border-radius: 20px;
  font-size: 0.65rem;
  font-weight: 600;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: help;
  white-space: nowrap;
  border: 1px solid transparent;
}

.op-pill-mini.blue {
  background: #eff6ff;
  color: #2563eb;
  border-color: #dbeafe;
}

.op-pill-mini.green {
  background: #ecfdf5;
  color: #059669;
  border-color: #d1fae5;
}

.op-pill-mini:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
}

.op-label {
  opacity: 0.7;
  font-weight: 500;
  text-transform: uppercase;
  font-size: 0.6rem;
}

.op-name {
  font-weight: 700;
}

/* Tooltip Premium */
.op-tooltip {
  position: absolute;
  bottom: 125%;
  left: 50%;
  transform: translateX(-50%) translateY(10px);
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 12px;
  width: 240px;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
  z-index: 100;
  opacity: 0;
  visibility: hidden;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  pointer-events: none;
}

.op-tooltip::after {
  content: '';
  position: absolute;
  top: 100%;
  left: 50%;
  margin-left: -6px;
  border-width: 6px;
  border-style: solid;
  border-color: #ffffff transparent transparent transparent;
}

.op-pill-wrapper:hover .op-tooltip {
  opacity: 1;
  visibility: visible;
  transform: translateX(-50%) translateY(0);
}

.tooltip-header {
  font-size: 0.75rem;
  font-weight: 800;
  color: #1e293b;
  margin-bottom: 8px;
  padding-bottom: 6px;
  border-bottom: 1px solid #f1f5f9;
  text-transform: uppercase;
  letter-spacing: 0.025em;
}

.tooltip-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 4px;
}

.t-label {
  font-size: 0.7rem;
  color: #64748b;
  font-weight: 500;
}

.t-value {
  font-size: 0.7rem;
  color: #0f172a;
  font-weight: 700;
  text-align: right;
}

/* DARK MODE ADAPTATION */
body.dark-mode .op-pill-mini.blue {
  background: rgba(37, 99, 235, 0.1);
  color: #60a5fa;
  border-color: rgba(37, 99, 235, 0.2);
}

body.dark-mode .op-pill-mini.green {
  background: rgba(16, 185, 129, 0.1);
  color: #34d399;
  border-color: rgba(16, 185, 129, 0.2);
}

body.dark-mode .op-tooltip {
  background: #1e293b;
  border-color: #334155;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
}

body.dark-mode .op-tooltip::after {
  border-color: #1e293b transparent transparent transparent;
}

body.dark-mode .tooltip-header {
  color: #f1f5f9;
  border-color: #334155;
}

body.dark-mode .t-label {
  color: #94a3b8;
}

body.dark-mode .t-value {
  color: #f8fafc;
}

body.dark-mode .table-card { background: #151b2d; border-color: #1e293b; }
body.dark-mode .data-table th { background: #0b0f1a; color: #94a3b8; border-color: #1e293b; }
body.dark-mode .data-table td { border-color: #1e293b; color: #cbd5e1; }
body.dark-mode .text-dark { color: #f1f5f9 !important; }
body.dark-mode .motif-text { color: #cbd5e1; }
body.dark-mode .filter-bar { background: #151b2d; border-color: #1e293b; }
body.dark-mode .filter-input-std { background: #0b0f1a; border-color: #1e293b; color: #f1f5f9; }
body.dark-mode .tab-btn { color: #64748b; }
body.dark-mode .tab-btn:hover { color: #cbd5e1; }
body.dark-mode .tab-btn.active { color: #3b82f6; border-color: #3b82f6; }
</style>
