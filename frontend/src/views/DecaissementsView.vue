<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import api from '../services/api'
import MainLayout from '../components/MainLayout.vue'
import Pagination from '../components/Pagination.vue'
import { useDecaissementStore } from '../stores/decaissement.store'
import { useTierStore } from '../stores/tier.store'
import { useAuthStore } from '../stores/auth.store'
import { useCompteStore } from '../stores/compte.store'

const store = useDecaissementStore()
const tierStore = useTierStore()
const authStore = useAuthStore()
const compteStore = useCompteStore()

// Modals State
const showCreateModal = ref(false)
const showApproveModal = ref(false)
const showExecuteModal = ref(false)
const showRejectModal = ref(false)
const formError = ref('')
const activeDecaissement = ref(null)
const showPreview = ref(false)
const previewUrl = ref(null)

const openPreview = async (id) => {
  try {
    const response = await api.get(`/decaissements/${id}/recu/pdf`, { responseType: 'blob' })

    if (response.status === 204 || !response.data || response.data.size === 0) {
      alert("Erreur: Le serveur n'a renvoyé aucune donnée pour ce bon.")
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
  search: '', // Motif, Beneficiaire, TXN
  fournisseurId: '',
  statut: '',
  dateDebut: '',
  dateFin: ''
})

const resetFilters = () => {
  filters.value = { search: '', fournisseurId: '', statut: '', dateDebut: '', dateFin: '' }
  currentPage.value = 1
}

watch(filters, () => {
  currentPage.value = 1
}, { deep: true })

const filteredDecaissements = computed(() => {
  let list = store.decaissements
  
  if (filters.value.search) {
    const s = filters.value.search.toLowerCase()
    list = list.filter(d => 
      (d.motif && d.motif.toLowerCase().includes(s)) ||
      (d.beneficiaire && d.beneficiaire.toLowerCase().includes(s)) ||
      (d.fournisseurNom && d.fournisseurNom.toLowerCase().includes(s)) ||
      (d.id && d.id.toString().includes(s))
    )
  }

  if (filters.value.fournisseurId) {
    list = list.filter(d => d.fournisseurId == filters.value.fournisseurId)
  }

  if (filters.value.statut) {
    list = list.filter(d => d.statut === filters.value.statut)
  }

  if (filters.value.dateDebut) {
    list = list.filter(d => (d.dateCreation || d.dateDecaissement) >= filters.value.dateDebut)
  }
  if (filters.value.dateFin) {
    list = list.filter(d => (d.dateCreation || d.dateDecaissement) <= filters.value.dateFin)
  }

  return list
})

const paginatedList = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage
  return filteredDecaissements.value.slice(start, start + itemsPerPage)
})

// Forms State
const defaultCreateForm = { motif: '', montant: '', fournisseurId: '', beneficiaire: '', mode: 'VIREMENT', banqueEmettrice: '', numeroOperation: '', dateOperation: '', telephone: '', categorie: 'PAIEMENT_FOURNISSEUR' }
const createForm = ref({ ...defaultCreateForm })
const rejectForm = ref({ reason: 'missing_docs', comments: '' })
const approveForm = ref({ checks: [false, false, false, false] })
const executeForm = ref({ compteFinancierId: 1 })
const uploadFiles = ref([])

// Catégories de décaissement
const categories = [
  { value: 'PAIEMENT_FOURNISSEUR', label: 'Paiement Fournisseur', icon: '🏢' },
  { value: 'SALAIRES', label: 'Salaires', icon: '💰' },
  { value: 'FRAIS_FONCTIONNEMENT', label: 'Frais de Fonctionnement', icon: '⚡' },
  { value: 'MISSION_DEPLACEMENT', label: 'Mission / Déplacement', icon: '✈️' },
  { value: 'ACHAT_MATERIEL', label: 'Achat Matériel', icon: '📦' },
  { value: 'AUTRE', label: 'Autre', icon: '📋' }
]

// Checklist dynamique selon la catégorie
const getChecklistItems = (categorie) => {
  switch (categorie) {
    case 'PAIEMENT_FOURNISSEUR': return [
      'La facture fournisseur est jointe et conforme',
      'Le bon de commande est présent',
      'Les biens/services ont été réceptionnés',
      'Le montant correspond au bon de commande'
    ]
    case 'SALAIRES': return [
      'La fiche de paie est jointe',
      'Le montant correspond au bulletin de salaire',
      'Le bénéficiaire est bien un employé de l\'entreprise'
    ]
    case 'FRAIS_FONCTIONNEMENT': return [
      'Le justificatif de la dépense est joint (facture, reçu)',
      'Le montant est conforme au justificatif',
      'La dépense est autorisée / budgétisée'
    ]
    case 'MISSION_DEPLACEMENT': return [
      'L\'ordre de mission est validé',
      'Les justificatifs de frais sont joints',
      'Le montant correspond aux frais engagés'
    ]
    case 'ACHAT_MATERIEL': return [
      'Le bon de commande est présent',
      'Le devis/proforma est joint',
      'Le matériel a été réceptionné'
    ]
    default: return [
      'Un justificatif est fourni',
      'Le montant est justifié'
    ]
  }
}

const approveChecklist = computed(() => {
  if (!activeDecaissement.value) return []
  return getChecklistItems(activeDecaissement.value.categorie || 'PAIEMENT_FOURNISSEUR')
})

const onFilesSelected = (event) => {
  uploadFiles.value = Array.from(event.target.files)
}

const removeFile = (index) => {
  uploadFiles.value.splice(index, 1)
}

const downloadJustificatif = async (id, originalName) => {
  try {
    const response = await api.get(`/decaissements/justificatifs/${id}/download`, { responseType: 'blob' })
    const url = window.URL.createObjectURL(new Blob([response.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', originalName)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  } catch (err) {
    console.error('Erreur lors du téléchargement du justificatif:', err)
    alert('Impossible de télécharger le fichier.')
  }
}

const selectedFournisseurObj = computed(() => {
  if (!createForm.value.fournisseurId) return null
  return tierStore.fournisseurs.find(f => f.id === createForm.value.fournisseurId)
})

const isSubmitting = ref(false)

onMounted(async () => {
  await store.fetchDecaissements()
  await tierStore.fetchTiers()
  await compteStore.fetchComptes()
})

const availableComptes = computed(() => {
  if (!activeDecaissement.value) return []
  if (activeDecaissement.value.moyenPaiement === 'ESPECES') {
    return compteStore.caisses
  } else {
    return compteStore.banques
  }
})


const submitCreate = async () => {
  formError.value = ''
  try {
    // Bénéficiaire selon la catégorie
    let beneficiaireStr = createForm.value.beneficiaire || ''
    if (createForm.value.categorie === 'PAIEMENT_FOURNISSEUR') {
      const selectedFou = tierStore.fournisseurs.find(f => f.id === createForm.value.fournisseurId)
      beneficiaireStr = selectedFou ? selectedFou.raisonSociale : beneficiaireStr || 'Fournisseur Inconnu'
    }

    const dataToSend = {
      motif: createForm.value.motif || '',
      montant: createForm.value.montant,
      beneficiaire: beneficiaireStr,
      fournisseurId: createForm.value.categorie === 'PAIEMENT_FOURNISSEUR' ? createForm.value.fournisseurId : null,
      moyenPaiement: createForm.value.mode,
      categorie: createForm.value.categorie,
      banqueEmettrice: createForm.value.banqueEmettrice || null,
      numeroOperation: createForm.value.numeroOperation || null,
      dateOperation: createForm.value.dateOperation || null,
      telephone: createForm.value.telephone || null,
      deviseId: 1
    }
    const newlyCreated = await store.createDecaissement(dataToSend)
    
    // Upload des justificatifs si des fichiers sont sélectionnés
    if (newlyCreated && newlyCreated.id && uploadFiles.value.length > 0) {
      const formData = new FormData()
      uploadFiles.value.forEach(f => formData.append('files', f))
      try {
        await api.post(`/decaissements/${newlyCreated.id}/justificatifs`, formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        })
      } catch (uploadErr) {
        console.warn('Upload justificatifs échoué:', uploadErr)
      }
    }

    showCreateModal.value = false
    createForm.value = { ...defaultCreateForm }
    uploadFiles.value = []
    
    // Auto-téléchargement du bon
    if (newlyCreated && newlyCreated.id) {
      await store.downloadReceipt(newlyCreated.id)
    }

  } catch(e) {
    console.error(e)
    formError.value = e.response?.data?.error || e.response?.data?.message || e.message || 'Une erreur est survenue lors de l\'enregistrement.'
  }
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
    if (activeDecaissement.value.statut === 'EN_ATTENTE' || activeDecaissement.value.statut === 'SOUMIS') {
      await store.validerRF(activeDecaissement.value.id, { checks: approveForm.value.checks })
    } else if (activeDecaissement.value.statut === 'EN_ATTENTE_PDG' || activeDecaissement.value.statut === 'VALIDEE_RF') {
       // Note: VALIDEE_RF peut être approuvé par PDG si montant élevé, ou passé au caissier
       await store.approuverPDG(activeDecaissement.value.id, { approved: true })
    }
    showApproveModal.value = false
  } catch(e) {
    console.error(e)
    alert(e.response?.data?.error || e.response?.data?.message || e.message || 'Erreur lors de la validation.')
  }
}

const openExecute = (item) => {
  activeDecaissement.value = item
  const list = item.moyenPaiement === 'ESPECES' ? compteStore.caisses : compteStore.banques
  executeForm.value.compteFinancierId = list.length > 0 ? list[0].id : null
  showExecuteModal.value = true
}

const submitExecute = async () => {
  try {
    await store.executer(activeDecaissement.value.id, { 
      compteFinancierId: executeForm.value.compteFinancierId 
    })
    showExecuteModal.value = false
  } catch(e) {
    console.error(e)
    alert(e.response?.data?.error || e.response?.data?.message || e.message || 'Erreur lors de l\'exécution.')
  }
}

const submitReject = async () => {
  try {
    const comment = `Raison: ${rejectForm.value.reason}. ${rejectForm.value.comments}`
    await store.updateStatut(activeDecaissement.value.id, 'REJETE', comment)
    showRejectModal.value = false
  } catch(e) {
    console.error(e)
    alert(e.response?.data?.error || e.response?.data?.message || e.message || 'Erreur lors du rejet.')
  }
}

const getStatusClass = (statut) => {
  if (!statut) return ''
  const s = statut.toUpperCase()
  if (s === 'EXECUTEE') return 'badge-success'
  if (s.startsWith('VALIDEE')) return 'badge-success-light'
  if (s.includes('REJETEE') || s.includes('ANNULEE')) return 'badge-danger'
  if (s.includes('ATTENTE') || s.includes('SOUMIS')) return 'badge-warning'
  return 'badge-info'
}

const bankBalance = computed(() => {
  if (!activeDecaissement.value || !activeDecaissement.value.compteFinancierId) return 0
  const c = compteStore.comptes.find(acc => acc.id === activeDecaissement.value.compteFinancierId)
  return c ? c.solde : 0
})
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

    <!-- Barre de Filtres -->
    <div class="filter-bar">
      <div class="filter-group group-search">
        <div class="input-with-icon-left">
          <svg class="icon" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
          <input v-model="filters.search" type="text" placeholder="Motif ou Bénéficiaire..." class="filter-input-std" />
        </div>
      </div>
      
      <div class="filter-group">
        <select v-model="filters.fournisseurId" class="filter-input-std">
          <option value="">Tous les fournisseurs</option>
          <option v-for="f in tierStore.fournisseurs" :key="f.id" :value="f.id">{{ f.raisonSociale }}</option>
        </select>
      </div>

      <div class="filter-group">
        <select v-model="filters.statut" class="filter-input-std">
          <option value="">Tous les statuts</option>
          <option value="EN_ATTENTE">Attente RF</option>
          <option value="EN_ATTENTE_PDG">Attente PDG</option>
          <option value="VALIDEE_RF">Validé RF</option>
          <option value="VALIDEE_PDG">Approuvé PDG</option>
          <option value="EXECUTEE">Exécutée (Payée)</option>
          <option value="REJETEE_RF">Rejeté RF</option>
          <option value="REJETEE_PDG">Rejeté PDG</option>
        </select>
      </div>

      <div class="filter-group-range">
        <input v-model="filters.dateDebut" type="date" class="filter-input-std" title="Date début" />
        <span class="to-text">à</span>
        <input v-model="filters.dateFin" type="date" class="filter-input-std" title="Date fin" />
      </div>

      <button @click="resetFilters" class="btn-clear-filters" title="Réinitialiser">
        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"></path></svg>
      </button>
    </div>

    <div class="table-card">
      <div v-if="store.loading && store.decaissements.length === 0" class="loading-state">
        Chargement...
      </div>
      
      <div v-else-if="store.error" class="error-state">
        {{ store.error }}
        <button @click="store.fetchDecaissements" class="btn-outline mt-2">Réessayer</button>
      </div>

      <div v-else class="table-scroll-container">
        <table class="data-table">
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
            <tr v-if="filteredDecaissements.length === 0" class="empty-row text-center">
              <td colspan="6">
                <div class="empty-state">
                  <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>
                  <p>Aucune demande de décaissement trouvée.</p>
                </div>
              </td>
            </tr>
            <tr v-for="item in paginatedList" :key="item.id">
              <td class="font-semibold text-dark">{{ item.numero }}</td>
              <td class="text-muted">{{ new Date(item.dateCreation || item.dateDecaissement).toLocaleDateString() }}</td>
              <td>
                <div class="cell-stack">
                  <strong class="text-dark">{{ item.beneficiaire || item.fournisseurNom || 'N/A' }}</strong>
                  <span class="text-muted text-sm">{{ item.motif || 'Aucun motif renseigné' }}</span>
                </div>
              </td>
              <td class="text-right font-semibold text-dark">{{ item.montant?.toLocaleString() }} XAF</td>
              <td>
                <span class="badge" :class="getStatusClass(item.statut)">
                  {{ item.statut }}
                </span>
              </td>
              <td class="text-center">
                <div class="actions-cell">
                  <template v-if="item.statut === 'SOUMIS' || item.statut === 'EN_ATTENTE'">
                    <button class="btn-icon text-green" @click="openApprove(item)" title="Valider (RF)">
                      <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"></polyline></svg>
                    </button>
                    <button class="btn-icon text-red" @click="openReject(item)" title="Rejeter">
                      <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
                    </button>
                  </template>

                  <button v-if="item.statut === 'VALIDEE_RF' && (authStore.userRole === 'PDG' || authStore.userRole === 'ADMINISTRATEUR')" @click="openApprove(item)" class="icon-btn text-green" title="Approuver (PDG)">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>
                  </button>

                  <button v-if="(item.statut === 'VALIDEE_PDG' || (item.statut === 'VALIDEE_RF' && item.montant < 500000)) && (authStore.userRole === 'CAISSIER' || authStore.userRole === 'ADMINISTRATEUR')" @click="openExecute(item)" class="icon-btn text-blue" title="Exécuter Paiement">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-3-3.87"></path><path d="M1 21v-2a4 4 0 0 1 4-4h8a4 4 0 0 1 4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 7l-7 7-3-3"></path></svg>
                  </button>
                  
                  <!-- Preview Button -->
                  <button @click="openPreview(item.id)" class="icon-btn preview-btn" title="Aperçu">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path><circle cx="12" cy="12" r="3"></circle></svg>
                  </button>

                  <button @click="store.downloadReceipt(item.id)" class="icon-btn download-btn" title="Télécharger">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="7 10 12 15 17 10"></polyline><line x1="12" y1="15" x2="12" y2="3"></line></svg>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Info Pagination -->
      <div class="table-footer-info" v-if="filteredDecaissements.length > 0">
        Affichage de {{ paginatedList.length }} sur {{ filteredDecaissements.length }} décaissement(s)
        <span v-if="filteredDecaissements.length < store.decaissements.length" class="text-blue italic">(Filtré)</span>
      </div>
    </div>

    <!-- Composant de Pagination Détaché -->
    <Pagination 
      v-if="filteredDecaissements.length > 0"
      :currentPage="currentPage" 
      :totalItems="filteredDecaissements.length" 
      :itemsPerPage="itemsPerPage" 
      @update:currentPage="currentPage = $event" 
    />

    <!-- MODAL : NOUVELLE DEMANDE (SAISIE RAPIDE AVANCÉE) -->
    <div v-if="showCreateModal" class="modal-backdrop">
      <div class="modal modal-lg">
        <div class="modal-header">
          <h3>Nouveau Décaissement</h3>
          <button @click="showCreateModal = false" class="close-btn"><svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg></button>
        </div>
        
        <div class="modal-split">
          <!-- Formulaire Principal -->
          <form id="create-decaissement-form" @submit.prevent="submitCreate" class="modal-left">

            <!-- Bandeau d'erreur métier -->
            <div v-if="formError" class="form-error-banner">
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>
              <span>{{ formError }}</span>
              <button type="button" @click="formError = ''" class="close-error-btn">&times;</button>
            </div>

            <!-- Sélecteur de catégorie -->
            <div class="form-group">
              <label>Catégorie <span class="req">*</span></label>
              <div class="category-grid">
                <label v-for="cat in categories" :key="cat.value" class="category-card" :class="{ active: createForm.categorie === cat.value }">
                  <input type="radio" v-model="createForm.categorie" :value="cat.value" class="hidden-radio" />
                  <span class="cat-icon">{{ cat.icon }}</span>
                  <span class="cat-label">{{ cat.label }}</span>
                </label>
              </div>
            </div>

            <!-- Fournisseur (visible uniquement si PAIEMENT_FOURNISSEUR) -->
            <div v-if="createForm.categorie === 'PAIEMENT_FOURNISSEUR'" class="form-group input-with-icon">
              <label>Fournisseur <span class="req">*</span></label>
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
              <div class="payment-modes" style="grid-template-columns: repeat(4, 1fr); gap: 0.5rem;">
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
                <label class="mode-card" :class="{ active: createForm.mode === 'ORANGE_MONEY' }">
                  <input type="radio" v-model="createForm.mode" value="ORANGE_MONEY" class="hidden-radio"/>
                  <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="5" y="2" width="14" height="20" rx="2" ry="2"></rect><line x1="12" y1="18" x2="12.01" y2="18"></line></svg>
                  <span>Or. Money</span>
                </label>
              </div>
            </div>

            <div class="dynamic-fields fade-in-fast" v-if="createForm.mode !== 'ESPECES'">
              <div v-if="createForm.mode === 'VIREMENT' || createForm.mode === 'CHEQUE'" class="form-row mt-2">
                <div class="form-group half">
                  <label>Banque Destinataire <span class="req">*</span></label>
                  <input v-model="createForm.banqueEmettrice" type="text" class="input-large" :placeholder="createForm.mode === 'CHEQUE' ? 'Banque tirée...' : 'Banque destinataire...'" required />
                </div>
                <div class="form-group half">
                  <label>N° {{ createForm.mode === 'CHEQUE' ? 'du Chèque' : 'Opération' }} <span class="req">*</span></label>
                  <input v-model="createForm.numeroOperation" type="text" class="input-large" placeholder="Saisir la référence..." required />
                </div>
              </div>

              <div v-if="createForm.mode === 'CHEQUE'" class="form-group mt-2">
                <label>Date sur le Chèque <span class="req">*</span></label>
                <input v-model="createForm.dateOperation" type="date" class="input-large" required />
              </div>

              <div v-if="createForm.mode === 'ORANGE_MONEY'" class="form-row mt-2">
                <div class="form-group half">
                  <label>Téléphone Bénéficiaire <span class="req">*</span></label>
                  <input v-model="createForm.telephone" type="text" class="input-large" placeholder="Ex: 6XX XX XX XX" required />
                </div>
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

            <!-- Zone d'upload de justificatifs -->
            <div class="form-group">
              <label>Pièces justificatives</label>
              <div class="upload-zone">
                <input type="file" id="justificatif-upload" multiple accept=".pdf,.jpg,.jpeg,.png,.doc,.docx" @change="onFilesSelected" class="upload-input" />
                <label for="justificatif-upload" class="upload-label">
                  <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="17 8 12 3 7 8"></polyline><line x1="12" y1="3" x2="12" y2="15"></line></svg>
                  <span>Cliquer pour ajouter des fichiers</span>
                  <span class="upload-hint">PDF, Images, Documents (max 10 Mo)</span>
                </label>
              </div>
              <div v-if="uploadFiles.length > 0" class="uploaded-files">
                <div v-for="(file, index) in uploadFiles" :key="index" class="file-item">
                  <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline></svg>
                  <span class="file-name">{{ file.name }}</span>
                  <span class="file-size">{{ (file.size / 1024).toFixed(0) }} Ko</span>
                  <button type="button" @click="removeFile(index)" class="file-remove">&times;</button>
                </div>
              </div>
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

                <div class="alert-box alert-success" v-if="createForm.montant && (createForm.fournisseurId || createForm.beneficiaire)">
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
          <button type="submit" form="create-decaissement-form" class="btn-primary-large" :disabled="!createForm.montant || (createForm.categorie === 'PAIEMENT_FOURNISSEUR' && !createForm.fournisseurId) || (createForm.categorie !== 'PAIEMENT_FOURNISSEUR' && !createForm.beneficiaire)">
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
            <span>Security validation required for transaction #TXN-{{ activeDecaissement?.id?.toString().padStart(4, '0') }}-BK</span>
          </div>
        </div>

        <div class="checklist-section">
          <span class="section-label">CHECKLIST DE SÉCURITÉ ({{ activeDecaissement?.categorie }})</span>
          <div class="checklist">
            <label v-for="(item, idx) in approveChecklist" :key="idx" class="check-item">
              <input type="checkbox" v-model="approveForm.checks[idx]" />
              <div class="custom-check"></div>
              <span>{{ item }}</span>
            </label>
          </div>
        </div>

        <div v-if="activeDecaissement?.justificatifs?.length > 0" class="justificatifs-display-section">
          <span class="section-label">PIÈCES JUSTIFICATIVES JOINTES</span>
          <div class="justificatif-list">
            <div v-for="j in activeDecaissement.justificatifs" :key="j.id" class="justif-item">
              <div class="justif-info">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M13 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z"></path><polyline points="13 2 13 9 20 9"></polyline></svg>
                <div class="justif-text">
                  <span class="justif-name">{{ j.nomOriginal }}</span>
                  <span class="justif-size">{{ (j.tailleFichier / 1024).toFixed(1) }} Ko</span>
                </div>
              </div>
              <button @click="downloadJustificatif(j.id, j.nomOriginal)" class="btn-justif-view" title="Ouvrir le document">
                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path><circle cx="12" cy="12" r="3"></circle></svg>
                Ouvrir
              </button>
            </div>
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
              <strong class="val text-blue">{{ (bankBalance - (activeDecaissement?.montant || 0)).toLocaleString() }} XAF</strong>
            </div>
          </div>
          <div class="divider"></div>
          <div class="balance-row amounts-row">
            <span class="label">Transaction Amount:</span>
            <strong class="val-dark">-{{ activeDecaissement?.montant?.toLocaleString() }} XAF</strong>
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
          <button @click="submitApprove" class="btn-confirm-execute" :disabled="!approveForm.checks.every(c => c) && (activeDecaissement?.statut === 'EN_ATTENTE' || activeDecaissement?.statut === 'SOUMIS')">
            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>
            Confirm Approval
          </button>
        </div>
      </div>
    </div>

    <!-- MODAL : EXÉCUTION PAIEMENT (CAISSIER) -->
    <div v-if="showExecuteModal" class="modal-backdrop fade-in">
      <div class="modal modal-approve">
        <div class="approve-header-group">
          <div class="icon-warning-rounded" style="background: #dbeafe;">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#2563eb" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-3-3.87"></path><path d="M1 21v-2a4 4 0 0 1 4-4h8a4 4 0 0 1 4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 7l-7 7-3-3"></path></svg>
          </div>
          <div class="title-block">
            <h3>Exécuter Paiement</h3>
            <span>Transaction #{{ activeDecaissement?.numero }}</span>
          </div>
        </div>

        <div class="modal-body" style="padding: 0;">
          <div class="form-group">
            <label>Compte Financier <span class="req">*</span></label>
            <select v-model="executeForm.compteFinancierId" class="input-std" required>
              <option value="" disabled>Choisir un compte...</option>
              <option v-for="c in availableComptes" :key="c.id" :value="c.id">
                {{ c.type === 'CAISSE' ? 'Compte de Caisse' : 'Compte de Banque' }}
              </option>
            </select>
            <div v-if="availableComptes.length === 0" class="alert-box alert-error">
               Aucun compte compatible trouvé.
            </div>
          </div>

          <div class="balance-calc-box">
             <div class="balance-row">
               <span>Montant à décaisser :</span>
               <strong class="text-red">-{{ activeDecaissement?.montant?.toLocaleString() }} XAF</strong>
             </div>
          </div>
        </div>

        <div class="approve-footer">
          <button @click="showExecuteModal = false" class="btn-outline-wide">Annuler</button>
          <button @click="submitExecute" class="btn-confirm-execute" style="background: #2563eb;" :disabled="!executeForm.compteFinancierId">
            Confirmer le Paiement
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
            <span>Transaction ID: TXN-{{ activeDecaissement?.id?.toString().padStart(4, '0') }}-BK</span>
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

    <!-- Modale de Prévisualisation (iFrame) -->
    <div v-if="showPreview" class="modal-backdrop-preview" @click.self="closePreview">
      <div class="preview-container">
        <div class="preview-header">
          <h3>Aperçu du Bon de Décaissement</h3>
          <button @click="closePreview" class="close-btn-preview">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
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
/* MAIN LAYOUT ELEMENTS */
.cell-stack { display: flex; flex-direction: column; gap: 0.15rem; }

/* ACTIONS */
.btn-primary { display: flex; align-items: center; gap: 0.5rem; background-color: #2563eb; color: white; padding: 0.625rem 1rem; border-radius: 8px; border: none; font-size: 0.875rem; font-weight: 600; cursor: pointer; transition: background 0.15s; }
.btn-primary:hover { background-color: #1d4ed8; }

.actions-cell { display: flex; gap: 0.5rem; justify-content: center; }
.btn-icon { background: none; border: none; padding: 6px; border-radius: 6px; cursor: pointer; transition: background 0.15s; }
.btn-icon:hover { background: #f3f4f6; }
.icon-btn { background: #f3f4f6; border: none; padding: 0.4rem; border-radius: 6px; color: #4b5563; cursor: pointer; transition: 0.15s;}
.icon-btn:hover { background: #e5e7eb; color: #111827; }
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
.modal-lg { max-width: 950px; display: flex; flex-direction: column; max-height: 90vh; }
.modal-header h3 { font-size: 1.25rem; font-weight: 700; color: #111827; margin: 0; }
.modal-split { display: grid; grid-template-columns: 1.5fr 1fr; border-bottom: 1px solid #f3f4f6; overflow-y: auto; flex: 1; min-height: 0; }
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

/* CATEGORY SELECTOR */
.category-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 0.75rem; margin-top: 0.25rem; }
.category-card { border: 1px solid #e5e7eb; border-radius: 10px; padding: 1rem; display: flex; flex-direction: column; align-items: center; gap: 0.5rem; cursor: pointer; transition: all 0.2s; background: white; text-align: center; }
.category-card:hover { border-color: #3b82f6; background: #f0f7ff; }
.category-card.active { border-color: #2563eb; background: #eff6ff; box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1); }
.cat-icon { font-size: 1.5rem; }
.cat-label { font-size: 0.75rem; font-weight: 600; color: #374151; line-height: 1.2; }
.category-card.active .cat-label { color: #1d4ed8; }

/* UPLOAD ZONE */
.upload-zone { margin-top: 0.25rem; }
.upload-input { display: none; }
.upload-label { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 1.5rem; border: 2px dashed #d1d5db; border-radius: 10px; cursor: pointer; transition: all 0.2s; background: #f9fafb; color: #6b7280; gap: 0.5rem; }
.upload-label:hover { border-color: #3b82f6; background: #f0f7ff; color: #2563eb; }
.upload-label svg { opacity: 0.6; }
.upload-label span { font-size: 0.875rem; font-weight: 500; }
.upload-hint { font-size: 0.7rem; opacity: 0.7; }

.uploaded-files { margin-top: 0.75rem; display: flex; flex-wrap: wrap; gap: 0.5rem; }
.file-item { display: flex; align-items: center; gap: 0.5rem; padding: 0.4rem 0.75rem; background: #f3f4f6; border-radius: 6px; border: 1px solid #e5e7eb; font-size: 0.75rem; color: #374151; }
.file-name { max-width: 150px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-weight: 500; }
.file-size { color: #6b7280; font-size: 0.65rem; }
.file-remove { background: none; border: none; color: #9ca3af; cursor: pointer; font-size: 1.125rem; line-height: 1; padding: 0; margin-left: 0.25rem; }
.file-remove:hover { color: #ef4444; }

/* CUSTOM SCROLLBAR FOR MODAL */
.modal-split::-webkit-scrollbar { width: 6px; }
.modal-split::-webkit-scrollbar-track { background: transparent; }
.modal-split::-webkit-scrollbar-thumb { background: #d1d5db; border-radius: 10px; }
.modal-split::-webkit-scrollbar-thumb:hover { background: #9ca3af; }
/* JUSTIFICATIFS DISPLAY (APPROVE MODAL) */
.justificatifs-display-section { margin-top: 1rem; }
.justificatif-list { display: flex; flex-direction: column; gap: 0.5rem; margin-top: 0.5rem; }
.justif-item { display: flex; align-items: center; justify-content: space-between; padding: 0.75rem; background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; }
.justif-info { display: flex; align-items: center; gap: 0.75rem; }
.justif-info svg { color: #64748b; }
.justif-text { display: flex; flex-direction: column; }
.justif-name { font-size: 0.8rem; font-weight: 600; color: #1e293b; max-width: 250px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.justif-size { font-size: 0.7rem; color: #64748b; }
.btn-justif-view { display: flex; align-items: center; gap: 0.4rem; padding: 0.4rem 0.75rem; background: white; border: 1px solid #d1d5db; border-radius: 6px; font-size: 0.75rem; font-weight: 600; color: #475569; text-decoration: none; transition: all 0.2s; }
.btn-justif-view:hover { background: #f1f5f9; border-color: #94a3b8; color: #1e293b; }
</style>
