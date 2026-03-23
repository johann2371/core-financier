<script setup>
import { ref, computed, onMounted } from 'vue'
import MainLayout from '../components/MainLayout.vue'
import Pagination from '../components/Pagination.vue'
import { useFactureStore } from '../stores/facture.store'
import { useTierStore } from '../stores/tier.store'
import { useRoute, useRouter } from 'vue-router'

const store = useFactureStore()
const tierStore = useTierStore()
const route = useRoute()
const router = useRouter()

const showModal = ref(false)
const activeTab = ref('TOUS') // TOUS, VENTE, ACHAT

// Pagination
const currentPage = ref(1)
const itemsPerPage = 8

const setTab = (tab) => {
  activeTab.value = tab
  currentPage.value = 1
}

// Formulaire Dymanique Facture
const form = ref({
  type: 'VENTE',
  tiersId: '',
  lignes: [
    { designation: '', quantite: 1, prixUnitaire: 0 }
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
  form.value.lignes.push({ designation: '', quantite: 1, prixUnitaire: 0 })
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
  if (activeTab.value === 'TOUS') return store.factures
  return store.factures.filter(f => f.type === activeTab.value)
})

const paginatedList = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage
  return filteredFactures.value.slice(start, start + itemsPerPage)
})

const submitForm = async () => {
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
  } catch(e) { console.error('Erreur création facture', e) }
}
</script>

<template>
  <MainLayout>
    <template #title>Gestion des Factures</template>

    <template #actions>
      <button @click="showModal = true" class="btn-primary">
        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16c0 1.1.9 2 2 2h12a2 2 0 0 0 2-2V8l-6-6z"/><path d="M14 3v5h5M16 13H8M16 17H8M10 9H8"/></svg>
        Nouvelle Facture
        <span class="shortcut">Ctrl + N</span>
      </button>
    </template>

    <!-- Navigation par Onglets -->
    <div class="tabs-nav">
      <button class="tab-btn" :class="{ active: activeTab === 'TOUS' }" @click="setTab('TOUS')">Toutes ({{ store.factures.length }})</button>
      <button class="tab-btn" :class="{ active: activeTab === 'VENTE' }" @click="setTab('VENTE')">Ventes ({{ store.ventes.length }})</button>
      <button class="tab-btn" :class="{ active: activeTab === 'ACHAT' }" @click="setTab('ACHAT')">Achats ({{ store.achats.length }})</button>
    </div>

    <!-- Tableau -->
    <div class="table-card">
      <div v-if="store.loading && store.factures.length === 0" class="loading-state">Chargement...</div>
      
      <div v-else-if="store.error" class="error-state">
        {{ store.error }}
        <button @click="store.fetchFactures" class="btn-outline">Réessayer</button>
      </div>

      <table v-else class="data-table">
        <thead>
          <tr>
            <th>N° Facture</th>
            <th>Type</th>
            <th>Tiers Associé</th>
            <th>Date</th>
            <th>Statut</th>
            <th class="text-right">Total TTC (XAF)</th>
            <th class="text-center">Action</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="filteredFactures.length === 0" class="empty-row text-center">
            <td colspan="7">Aucune facture trouvée.</td>
          </tr>
          
          <tr v-for="item in paginatedList" :key="item.id">
            <td class="font-semibold text-dark">{{ item.numero }}</td>
            <td>
              <span class="badge" :class="item.type === 'VENTE' ? 'badge-vente' : 'badge-achat'">
                {{ item.type }}
              </span>
            </td>
            <td>
              <div class="motif-cell">
                <span class="motif-text">{{ item.tiersNom || 'Inconnu' }}</span>
              </div>
            </td>
            <td>{{ item.dateFacture ? new Date(item.dateFacture).toLocaleDateString() : 'Non définie' }}</td>
            <td>
              <span class="badge" :class="{
                'badge-attente': item.statut === 'EN_ATTENTE_PAIEMENT',
                'badge-paye': item.statut === 'SOLDEE',
                'badge-partiel': item.statut === 'PARTIELLEMENT_PAYEE'
              }">
                {{ item.statut.replace(/_/g, ' ') }}
              </span>
            </td>
            <td class="text-right font-semibold text-dark">{{ item.montantTotalTtc?.toLocaleString() || '0' }}</td>
            <td class="text-center">
               <button class="icon-btn" @click="store.downloadPdf(item.id)" title="Télécharger PDF">
                 <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="7 10 12 15 17 10"></polyline><line x1="12" y1="15" x2="12" y2="3"></line></svg>
               </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Composant de Pagination Détaché -->
    <Pagination 
      v-if="filteredFactures.length > 0"
      :currentPage="currentPage" 
      :totalItems="filteredFactures.length" 
      :itemsPerPage="itemsPerPage" 
      @update:currentPage="currentPage = $event" 
    />

    <!-- Modale Création Facture (Complexe: Lignes) -->
    <div v-if="showModal" class="modal-backdrop fade-in">
      <div class="modal modal-large">
        <div class="modal-header">
          <h3>Nouvelle Facture</h3>
          <button @click="showModal = false" class="close-btn"><svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg></button>
        </div>
        
        <form @submit.prevent="submitForm" class="modal-body complex-body">
          <div class="form-row">
            <div class="form-group half">
               <label>Type de Facture <span class="req">*</span></label>
               <select v-model="form.type" required class="input-std" @change="form.tiersId = ''"> <!-- Reset Tiers on type switch -->
                 <option value="VENTE">Facture de Vente (Client)</option>
                 <option value="ACHAT">Facture d'Achat (Fournisseur)</option>
               </select>
            </div>
            <div class="form-group half input-with-icon">
               <label>{{ form.type === 'VENTE' ? 'Client Associé' : 'Fournisseur Associé' }} <span class="req">*</span></label>
               <select v-model="form.tiersId" required class="input-std">
                 <option value="" disabled>Sélectionner...</option>
                 <option v-for="t in tiersDisponibles" :key="t.id" :value="t.id">{{ t.raisonSociale }}</option>
               </select>
            </div>
          </div>

          <div class="section-divider mt-2">Détails des Articles/Lignes</div>
          
          <div class="lines-container">
            <div class="line-header form-row">
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
                 <input v-model.number="ligne.quantite" type="number" required min="1" step="1" class="input-std text-center" />
               </div>
               <div class="line-col price-col">
                 <input v-model.number="ligne.prixUnitaire" type="number" required min="0" step="25" class="input-std text-right" />
               </div>
               <div class="line-col total-col v-center">
                 <span class="font-semibold text-dark">{{ (ligne.quantite * ligne.prixUnitaire).toLocaleString() }}</span>
               </div>
               <div class="line-col act-col v-center">
                 <button type="button" class="icon-btn-danger" @click="removeLigne(index)" :disabled="form.lignes.length === 1">
                   <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path></svg>
                 </button>
               </div>
            </div>
            
            <button type="button" class="btn-outline-dashed mt-2" @click="addLigne">
              <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
              Ajouter une Ligne
            </button>
          </div>

          <!-- Totalisation -->
          <div class="totaux-card mt-3">
             <div class="tot-row"><span>Total HT</span> <span>{{ totalHT.toLocaleString() }} XAF</span></div>
             <div class="tot-row"><span>TVA (19.25%)</span> <span>{{ (totalHT * 0.1925).toLocaleString() }} XAF</span></div>
             <div class="tot-row total-ttc"><span>TTC Estimé</span> <span>{{ totalTTC.toLocaleString() }} XAF</span></div>
          </div>
          
          <div v-if="store.error" class="form-error">{{ store.error }}</div>

          <div class="modal-footer pt-3 pb-0">
             <button type="button" class="btn-text" @click="showModal = false">Annuler</button>
             <button type="submit" class="btn-primary" :disabled="!form.tiersId || form.lignes.length === 0 || store.loading">
               {{ store.loading ? 'Création...' : 'Générer la Facture' }}
             </button>
          </div>
        </form>
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
.text-right { text-align: right !important; }
.text-center { text-align: center !important; }
.v-center { display: flex; align-items: center; justify-content: center; }

.badge { display: inline-flex; padding: 0.25rem 0.625rem; border-radius: 20px; font-size: 0.7rem; font-weight: 600; text-transform: uppercase;}
.badge-vente { background: #dcfce7; color: #166534; }
.badge-achat { background: #fee2e2; color: #991b1b; }
.badge-attente { background: #fef3c7; color: #d97706; }
.badge-paye { background: #dcfce7; color: #15803d; }
.badge-partiel { background: #dbeafe; color: #1d4ed8; }

.icon-btn { background: #f3f4f6; border: none; padding: 0.4rem; border-radius: 6px; color: #4b5563; cursor: pointer; transition: 0.15s;}
.icon-btn:hover { background: #e5e7eb; color: #111827; }
.icon-btn-danger { background: none; border: none; color: #9ca3af; cursor: pointer; padding: 0.25rem; border-radius: 4px; transition: 0.1s;}
.icon-btn-danger:hover:not(:disabled) { background: #fee2e2; color: #ef4444; }
.icon-btn-danger:disabled { opacity: 0.4; cursor: not-allowed; }

/* TABS NAV */
.tabs-nav { display: flex; gap: 1rem; border-bottom: 1px solid #e5e7eb; margin-bottom: 1.5rem; }
.tab-btn { background: none; border: none; padding: 0.75rem 0.5rem; font-size: 0.875rem; font-weight: 600; color: #6b7280; border-bottom: 2px solid transparent; cursor: pointer; }
.tab-btn:hover { color: #111827; }
.tab-btn.active { color: #2563eb; border-color: #2563eb; }

/* ACTIONS */
.btn-primary { display: flex; align-items: center; gap: 0.5rem; background-color: #2563eb; color: white; padding: 0.625rem 1rem; border-radius: 8px; border: none; font-size: 0.875rem; font-weight: 600; cursor: pointer; transition: background 0.15s; }
.btn-primary:hover { background-color: #1d4ed8; }
.btn-primary .shortcut { background: rgba(255,255,255,0.2); border-radius: 4px; padding: 2px 6px; font-size: 0.65rem; font-weight: 500;}

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
</style>
