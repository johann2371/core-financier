<script setup>
import { ref, onMounted, computed } from "vue";
import { useLangStore } from '../stores/lang.store'
import MainLayout from "../components/MainLayout.vue";
import { useTierStore } from "../stores/tier.store";
import { useRoute, useRouter } from "vue-router";
import api from "../services/api";
import {
  FunnelIcon,
  UserPlusIcon,
  MagnifyingGlassIcon,
  XMarkIcon,
  ArrowUpTrayIcon,
  ArrowLeftIcon,
  PlusIcon,
  ExclamationTriangleIcon
} from '@heroicons/vue/24/outline'

const store = useTierStore();
const route = useRoute();
const router = useRouter();
const showModal = ref(false);
const formError = ref('');
const selectedTier = ref(null);
const showDetailPanel = ref(false);
const photoFile = ref(null);

const onPhotoChange = (event) => {
  photoFile.value = event.target.files[0];
};

// Onglets pour filtrer la vue (Tous, Clients, Fournisseurs)
const activeTab = ref("TOUS");
const searchQuery = ref("");
const showMobileFilters = ref(false);

// Formulaire de création complet
const form = ref({
  type: "CLIENT",
  raisonSociale: "",
  email: "",
  telephone: "",
  adresse: "",
  ville: "",
  pays: "Cameroun",
  typeClient: "ENTREPRISE",
  creditLimite: 0,
  delaiPaiement: 30,
  numeroCompte: "",
  iban: "",
  nui: "",
  rccm: "",
  cni: "",
  photoUrl: "",
});

onMounted(async () => {
  await store.fetchTiers();

  if (route.query.create) {
    showModal.value = true;
    if (route.query.create === "FOURNISSEUR") {
      form.value.type = "FOURNISSEUR";
    } else {
      form.value.type = "CLIENT";
    }
    // Nettoyer l'URL
    router.replace({ path: "/tiers" });
  }
});

const submitForm = async () => {
  formError.value = "";
  if (!form.value.raisonSociale) {
    formError.value = "La Raison Sociale / Nom est obligatoire.";
    return;
  }
  try {
    const dataToSend = { ...form.value };
    // Nettoyage conditionnel avant envoi au backend
    if (dataToSend.type === "CLIENT") {
      delete dataToSend.numeroCompte;
      delete dataToSend.iban;
    } else {
      delete dataToSend.typeClient;
      delete dataToSend.creditLimite;
      delete dataToSend.delaiPaiement;
      delete dataToSend.cni;
      delete dataToSend.photoUrl;
    }

    if (photoFile.value) {
      const formData = new FormData();
      formData.append('file', photoFile.value);
      const uploadRes = await store.uploadTierPhoto(formData);
      dataToSend.photoUrl = uploadRes.url;
    }

    await store.createTier(dataToSend);
    showModal.value = false;
    photoFile.value = null;

    // Reset Form
    form.value = {
      type: "CLIENT",
      raisonSociale: "",
      email: "",
      telephone: "",
      adresse: "",
      ville: "",
      pays: "Cameroun",
      typeClient: "ENTREPRISE",
      creditLimite: 0,
      delaiPaiement: 30,
      numeroCompte: "",
      iban: "",
      nui: "",
      rccm: "",
      cni: "",
      photoUrl: "",
    };
  } catch (e) {
    console.error(e);
    formError.value = e.response?.data?.error || e.response?.data?.message || e.message || 'Erreur lors de la création du tier.';
  }
};

const filteredTiers = computed(() => {
  let list = store.tiers.filter(t => activeTab.value === 'TOUS' || t.type === activeTab.value);
  
  if (searchQuery.value) {
    const s = searchQuery.value.toLowerCase();
    list = list.filter(t => 
      (t.raisonSociale && t.raisonSociale.toLowerCase().includes(s)) ||
      (t.code && t.code.toLowerCase().includes(s)) ||
      (t.email && t.email.toLowerCase().includes(s)) ||
      (t.telephone && t.telephone.toLowerCase().includes(s))
    );
  }
  return list;
});

const telechargerReleve = async (tiersId) => {
  try {
    const response = await api.get(`/tiers/${tiersId}/releve-pdf`, { responseType: 'blob' });
    const blob = new Blob([response.data], { type: 'application/pdf' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `releve_tiers_${tiersId}.pdf`;
    a.click();
    window.URL.revokeObjectURL(url);
  } catch (e) {
    console.error('Erreur téléchargement relevé:', e);
    alert('Erreur lors du téléchargement du relevé.');
  }
};

const langStore = useLangStore()
const t = computed(() => langStore.t)
</script>

<template>
  <MainLayout>
    <template #title>{{ t("tiers.titre") }}</template>

    <template #actions>
      <button class="icon-btn show-on-mobile" @click="showMobileFilters = !showMobileFilters" title="Filtrer">
        <FunnelIcon class="w-5 h-5" />
      </button>
      <button @click="showModal = true; isEditing = false" class="btn-primary hide-on-mobile">
        <UserPlusIcon class="w-5 h-5" />
        Nouveau Tier
      </button>
    </template>

    <div class="show-on-mobile w-100" style="margin-top: 1.5rem; margin-bottom: 1.5rem;">
      <button @click="showModal = true; isEditing = false" class="btn-primary w-100" style="justify-content: center; padding: 0.75rem;">
        <UserPlusIcon class="w-5 h-5" />
        Nouveau Tier
      </button>
    </div>

    <!-- Navigation par Onglets -->
    <div class="tabs-nav">
      <button
        class="tab-btn"
        :class="{ active: activeTab === 'TOUS' }"
        @click="activeTab = 'TOUS'"
      >
        Tous
      </button>
      <button
        class="tab-btn"
        :class="{ active: activeTab === 'CLIENT' }"
        @click="activeTab = 'CLIENT'"
      >
        Clients ({{ store.clients.length }})
      </button>
      <button
        class="tab-btn"
        :class="{ active: activeTab === 'FOURNISSEUR' }"
        @click="activeTab = 'FOURNISSEUR'"
      >
        Fournisseurs ({{ store.fournisseurs.length }})
      </button>
    </div>


    <!-- Barre de Recherche -->
    <div class="search-bar-container" :class="{ 'mobile-collapsed': !showMobileFilters }">
      <div class="search-input-wrapper">
        <MagnifyingGlassIcon class="search-icon w-5 h-5 text-slate-400" />
        <input 
          v-model="searchQuery" 
          type="text" 
          placeholder="Rechercher par nom, code, email ou téléphone..." 
          class="search-input"
        />
        <button v-if="searchQuery" @click="searchQuery = ''" class="clear-search">
          <XMarkIcon class="w-4 h-4" />
        </button>
      </div>
    </div>

    <!-- Tableau -->
    <div class="table-card">
      <div
        v-if="store.loading && store.tiers.length === 0"
        class="loading-state"
      >
        Chargement...
      </div>

      <div v-else-if="store.error" class="error-state">
        {{ store.error }}
        <button @click="store.fetchTiers" class="btn-outline">Réessayer</button>
      </div>

      <div v-else class="table-scroll-container">
        <table class="data-table">
          <thead>
            <tr>
              <th>Code</th>
              <th>Type</th>
              <th>Désignation</th>
              <th class="text-right">Dette Totale</th>
              <th class="text-right">Reste à payer</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-if="filteredTiers.length === 0"
              class="empty-row text-center"
            >
              <td colspan="5">Aucun tier trouvé.</td>
            </tr>

            <tr
              v-for="item in filteredTiers"
              :key="item.id"
            >
              <td class="font-semibold text-dark">{{ item.code }}</td>
              <td>
                <span
                  class="badge"
                  :class="
                    item.type === 'CLIENT' ? 'badge-client' : 'badge-fournisseur'
                  "
                >
                  {{ item.type }}
                </span>
              </td>
              <td>
                <div class="tier-designation-cell" @click="selectedTier = item; showDetailPanel = true">
                  <div class="tier-avatar" :style="{ backgroundImage: item.photoUrl ? 'url(' + item.photoUrl + ')' : 'url(https://ui-avatars.com/api/?name=' + encodeURIComponent(item.raisonSociale || 'T') + '&background=random&color=fff&size=128)' }"></div>
                  <div class="tier-info-text">
                    <span class="motif-text clickable-name">{{ item.raisonSociale }}</span>
                    <span class="text-muted" style="font-size: 0.75rem">{{ item.ville || item.pays }}</span>
                  </div>
                </div>
              </td>
              <td class="text-right text-muted">
                {{ item.totalDette?.toLocaleString() || "0" }}
              </td>
              <td class="text-right font-semibold" :class="{'text-danger': item.solde > 0}">
                {{ item.solde?.toLocaleString() || "0" }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Modale de Création -->
    <div v-if="showModal" class="modal-backdrop fade-in">
      <div class="modal">
        <div class="modal-header">
          <h3>{{ isEditing ? "Éditer Tier" : "Nouveau Tier" }}</h3>
          <button @click="showModal = false" class="close-btn">
            <XMarkIcon class="w-6 h-6" />
          </button>
        </div>

        <!-- Bandeau d'erreur métier -->
        <div v-if="formError" class="form-error-banner" style="margin: 1rem 1.5rem 0;">
          <ExclamationTriangleIcon class="w-5 h-5" />
          <span>{{ formError }}</span>
          <button type="button" @click="formError = ''" class="close-error-btn">&times;</button>
        </div>

        <form @submit.prevent="submitForm" class="modal-body complex-body">
          <div class="form-row">
            <div class="form-group half">
              <label>Type de Tier <span class="req">*</span></label>
              <select v-model="form.type" required class="input-std">
                <option value="CLIENT">Client</option>
                <option value="FOURNISSEUR">Fournisseur</option>
              </select>
            </div>

            <div class="form-group half" v-if="form.type === 'CLIENT'">
              <label>Catégorie</label>
              <select v-model="form.typeClient" class="input-std">
                <option value="ENTREPRISE">Entreprise</option>
                <option value="PARTICULIER">Particulier</option>
              </select>
            </div>
          </div>
          
          <!-- Nouveaux champs d'identification -->
          <div class="section-divider">Identification</div>
          <div class="form-row">
            <div class="form-group half">
              <label>NUI (Identifiant Unique) <span class="req">*</span></label>
              <input v-model="form.nui" type="text" class="input-std" placeholder="M0..." />
            </div>
            <div class="form-group half" v-if="form.type === 'CLIENT' && form.typeClient === 'PARTICULIER'">
              <label>CNI (Carte d'Identité)</label>
              <input v-model="form.cni" type="text" class="input-std" placeholder="112..." />
            </div>
            <div class="form-group half" v-else>
              <label>RCCM (Registre Commerce)</label>
              <input v-model="form.rccm" type="text" class="input-std" placeholder="RC/DLA/..." />
            </div>
          </div>

          <div class="form-group" v-if="form.type === 'CLIENT' && form.typeClient === 'PARTICULIER'">
            <label>Photo du client / CNI</label>
            <div class="file-upload-wrapper">
              <input type="file" @change="onPhotoChange" accept="image/*" class="file-input-hidden" id="tierPhoto" />
              <label for="tierPhoto" class="file-upload-label">
                <ArrowUpTrayIcon class="w-5 h-5" />
                <span>{{ photoFile ? photoFile.name : 'Choisir une image...' }}</span>
              </label>
            </div>
          </div>

          <div class="section-divider">Contact</div>
          <div class="form-group">
            <label>Désignation <span class="req">*</span></label>
            <input
              v-model="form.raisonSociale"
              type="text"
              required
              class="input-std"
              placeholder="Nom de l'entreprise ou personne"
            />
          </div>

          <div class="form-row">
            <div class="form-group half">
              <label>Téléphone</label>
              <input
                v-model="form.telephone"
                type="text"
                class="input-std"
                placeholder="+237 6..."
              />
            </div>
            <div class="form-group half">
              <label>Email</label>
              <input
                v-model="form.email"
                type="email"
                class="input-std"
                placeholder="contact@..."
              />
            </div>
          </div>

          <div class="section-divider">Localisation</div>
          <div class="form-group">
            <label>Adresse</label>
            <input
              v-model="form.adresse"
              type="text"
              class="input-std"
              placeholder="Localisation, Rue..."
            />
          </div>
          <div class="form-row">
            <div class="form-group half">
              <label>Ville</label>
              <input
                v-model="form.ville"
                type="text"
                class="input-std"
                placeholder="Douala"
              />
            </div>
            <div class="form-group half">
              <label>Pays</label>
              <input
                v-model="form.pays"
                type="text"
                class="input-std"
                placeholder="Cameroun"
              />
            </div>
          </div>

          <!-- Champs Spécifiques Client -->
          <template v-if="form.type === 'CLIENT'">
            <div class="section-divider">Infos Financières</div>
            <div class="form-row">
              <div class="form-group half">
                <label>Crédit Limite (XAF)</label>
                <input
                  v-model.number="form.creditLimite"
                  type="number"
                  step="1000"
                  class="input-std"
                />
              </div>
              <div class="form-group half">
                <label>Délai Paiement (Jours)</label>
                <input
                  v-model.number="form.delaiPaiement"
                  type="number"
                  class="input-std"
                />
              </div>
            </div>
          </template>

          <!-- Champs Spécifiques Fournisseur -->
          <template v-if="form.type === 'FOURNISSEUR'">
            <div class="section-divider">Coordonnées Bancaires</div>
            <div class="form-row">
              <div class="form-group half">
                <label>Numéro de Compte</label>
                <input
                  v-model="form.numeroCompte"
                  type="text"
                  class="input-std"
                  placeholder="00010-..."
                />
              </div>
              <div class="form-group half">
                <label>IBAN (Optionnel)</label>
                <input
                  v-model="form.iban"
                  type="text"
                  class="input-std"
                  placeholder="CM..."
                />
              </div>
            </div>
          </template>

          <div class="modal-footer pt-4 pb-0">
            <button type="button" class="btn-text" @click="showModal = false">
              Annuler
            </button>
            <button
              type="submit"
              class="btn-primary"
              :disabled="store.loading"
            >
              {{ store.loading ? "Enregistrement..." : (isEditing ? "Enregistrer" : "Créer le Tier") }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Panneau de Détail du Tier (Side Drawer) -->
    <Transition name="slide-right">
      <div v-if="showDetailPanel && selectedTier" class="detail-drawer-backdrop" @click.self="showDetailPanel = false">
        <div class="detail-drawer">
          <div class="drawer-header">
            <button @click="showDetailPanel = false" class="back-btn">
              <ArrowLeftIcon class="w-6 h-6" />
            </button>
            <h3>Profil du Tier</h3>
          </div>

          <div class="drawer-body custom-scrollbar">
            <div class="profile-summary">
              <div class="profile-avatar-large" :style="{ backgroundImage: selectedTier.photoUrl ? 'url(' + selectedTier.photoUrl + ')' : 'url(https://ui-avatars.com/api/?name=' + encodeURIComponent(selectedTier.raisonSociale || 'T') + '&background=random&color=fff&size=256)' }"></div>
              <h4>{{ selectedTier.raisonSociale }}</h4>
              <span class="profile-code">{{ selectedTier.code }}</span>
              <span class="badge" :class="selectedTier.type === 'CLIENT' ? 'badge-client' : 'badge-fournisseur'">{{ selectedTier.type }}</span>
            </div>

            <div class="detail-section">
              <h5 class="section-title">Informations d'identification</h5>
              <div class="detail-grid">
                <div class="detail-item" v-if="selectedTier.nui">
                  <span class="label">NUI</span>
                  <span class="value">{{ selectedTier.nui }}</span>
                </div>
                <div class="detail-item" v-if="selectedTier.rccm">
                  <span class="label">RCCM</span>
                  <span class="value">{{ selectedTier.rccm }}</span>
                </div>
                <div class="detail-item" v-if="selectedTier.cni">
                  <span class="label">No CNI</span>
                  <span class="value">{{ selectedTier.cni }}</span>
                </div>
                <div class="detail-item">
                  <span class="label">Catégorie</span>
                  <span class="value">{{ selectedTier.typeClient || 'Standard' }}</span>
                </div>
              </div>
            </div>

            <div class="detail-section">
              <h5 class="section-title">Contact & Localisation</h5>
              <div class="detail-grid">
                <div class="detail-item">
                  <span class="label">Email</span>
                  <span class="value">{{ selectedTier.email || 'Non renseigné' }}</span>
                </div>
                <div class="detail-item">
                  <span class="label">Téléphone</span>
                  <span class="value">{{ selectedTier.telephone || 'Non renseigné' }}</span>
                </div>
                <div class="detail-item block">
                  <span class="label">Adresse</span>
                  <span class="value">{{ selectedTier.adresse || 'N/A' }}, {{ selectedTier.ville || 'N/A' }}, {{ selectedTier.pays || 'N/A' }}</span>
                </div>
              </div>
            </div>

            <div class="detail-section">
              <h5 class="section-title">Situation Financière</h5>
              <div class="detail-grid">
                <div class="detail-item">
                  <span class="label">Dette Totale</span>
                  <span class="value text-dark font-bold">{{ selectedTier.totalDette?.toLocaleString() }} XAF</span>
                </div>
                <div class="detail-item">
                  <span class="label">Reste à payer</span>
                  <span class="value text-danger font-bold">{{ selectedTier.solde?.toLocaleString() }} XAF</span>
                </div>
                <div class="detail-item" v-if="selectedTier.type === 'CLIENT'">
                  <span class="label">Crédit Limite</span>
                  <span class="value">{{ selectedTier.creditLimite?.toLocaleString() }} XAF</span>
                </div>
                <div class="detail-item" v-if="selectedTier.type === 'FOURNISSEUR'">
                  <span class="label">Coordonnées Bancaires</span>
                  <span class="value">{{ selectedTier.numeroCompte || 'N/A' }}</span>
                </div>
              </div>
              
              <!-- Bouton Relevé de Compte -->
              <button class="btn-releve" @click="telechargerReleve(selectedTier.id)">
                <ArrowUpTrayIcon class="w-5 h-5" />
                Télécharger le relevé de compte
              </button>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </MainLayout>
</template>

<style scoped>
/* MAIN LAYOUT ELEMENTS */
.table-card {
  background: white;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}
.data-table {
  width: 100%;
  border-collapse: collapse;
}
.data-table th,
.data-table td {
  padding: 1.15rem 1.5rem;
  text-align: left;
  border-bottom: 1px solid #f3f4f6;
}
.data-table th {
  background-color: #f9fafb;
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
  color: #6b7280;
  letter-spacing: 0.05em;
}
.data-table td {
  font-size: 0.9rem;
  color: #4b5563;
  vertical-align: middle;
}

.font-semibold {
  font-weight: 600;
}
.text-dark {
  color: #111827;
}
.text-muted {
  color: #6b7280;
}
.text-right {
  text-align: right !important;
}
.text-center {
  text-align: center !important;
}

.motif-cell {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}
.motif-text {
  color: #111827;
  font-weight: 500;
}

.badge {
  display: inline-flex;
  padding: 0.25rem 0.625rem;
  border-radius: 20px;
  font-size: 0.7rem;
  font-weight: 600;
  text-transform: uppercase;
}
.badge-client {
  background: #eff6ff;
  color: #2563eb;
}
.badge-fournisseur {
  background: #fef3c7;
  color: #d97706;
}

/* Tiers Table Specifics */
.tier-designation-cell {
  display: flex;
  align-items: center;
  gap: 1rem;
  cursor: pointer;
}

.tier-avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background-size: cover;
  background-position: center;
  background-color: #f3f4f6;
  flex-shrink: 0;
  border: 2px solid white;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
}

.tier-info-text {
  display: flex;
  flex-direction: column;
}

.clickable-name {
  color: #111827;
  font-weight: 600;
  font-size: 0.95rem;
}

.tier-designation-cell:hover .clickable-name {
  color: #2563eb;
  text-decoration: underline;
}

/* TABS NAV */
.tabs-nav {
  display: flex;
  gap: 1rem;
  border-bottom: 1px solid #e5e7eb;
  margin-bottom: 1.5rem;
  border-radius: 0;
}
.tab-btn {
  background: none;
  border: none;
  padding: 0.75rem 0.5rem;
  font-size: 0.875rem;
  font-weight: 600;
  color: #6b7280;
  border-bottom: 2px solid transparent;
  cursor: pointer;
  transition: color 0.15s;
}
.tab-btn:hover {
  color: #111827;
}
.tab-btn.active {
  color: #2563eb;
  border-color: #2563eb;
}

/* SEARCH BAR */
.search-bar-container {
  margin-bottom: 1.5rem;
}
.search-input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  max-width: 500px;
}
.search-icon {
  position: absolute;
  left: 1rem;
  color: #9ca3af;
}
.search-input {
  width: 100%;
  padding: 0.75rem 1rem 0.75rem 3rem;
  font-size: 0.95rem;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  outline: none;
  background: white;
  transition: all 0.2s;
}
.search-input:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}
.clear-search {
  position: absolute;
  right: 0.75rem;
  background: #f3f4f6;
  border: none;
  padding: 4px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6b7280;
  cursor: pointer;
}
.clear-search:hover {
  background: #e5e7eb;
  color: #111827;
}

.icon-btn { background: #f3f4f6; border: none; padding: 0.4rem; border-radius: 6px; color: #4b5563; cursor: pointer; transition: 0.15s;}
.icon-btn:hover { background: #e5e7eb; color: #111827; }

/* ACTIONS */
.btn-primary {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  background-color: #2563eb;
  color: white;
  padding: 0.625rem 1rem;
  border-radius: 8px;
  border: none;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;
}
.btn-primary:hover {
  background-color: #1d4ed8;
}


/* MODAL */
.modal-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(17, 24, 39, 0.6);
  backdrop-filter: blur(2px);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem;
}
.fade-in {
  animation: fadeIn 0.2s ease-out;
}
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: scale(0.98);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.modal {
  background: white;
  border-radius: 12px;
  width: 100%;
  max-width: 550px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  display: flex;
  flex-direction: column;
  max-height: 90vh;
}
.modal-header {
  padding: 1.5rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #f3f4f6;
}
.modal-header h3 {
  font-size: 1.125rem;
  font-weight: 600;
  color: #111827;
  margin: 0;
}
.close-btn {
  background: none;
  border: none;
  color: #9ca3af;
  cursor: pointer;
}
.modal-body {
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  overflow-y: auto;
}
.section-divider {
  margin-top: 0.5rem;
  border-bottom: 1px solid #e5e7eb;
  padding-bottom: 0.5rem;
  font-size: 0.8rem;
  font-weight: 700;
  color: #4b5563;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}
.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.form-row {
  display: flex;
  gap: 1rem;
}
.half {
  flex: 1;
}
.form-group label {
  font-size: 0.85rem;
  font-weight: 600;
  color: #111827;
  margin: 0;
}
.req {
  color: #ef4444;
}
.input-std {
  width: 100%;
  padding: 0.75rem 1rem;
  font-size: 0.95rem;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  outline: none;
  transition: border-color 0.15s;
  background: white;
  color: #111827;
}
.input-std:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}
.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
}
.btn-text {
  background: none;
  border: none;
  font-size: 0.875rem;
  color: #6b7280;
  font-weight: 600;
  cursor: pointer;
}
.form-error {
  color: #dc2626;
  font-size: 0.875rem;
  padding: 0.5rem;
  background: #fee2e2;
  border-radius: 6px;
}

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
/* Profile Side Drawer */
.detail-drawer-backdrop {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(2px);
  z-index: 2000;
  display: flex;
  justify-content: flex-end;
}

.detail-drawer {
  width: 480px;
  height: 100%;
  background: white;
  box-shadow: -10px 0 30px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  animation: slideIn 0.3s ease-out;
}

@keyframes slideIn {
  from { transform: translateX(100%); }
  to { transform: translateX(0); }
}

.drawer-header {
  padding: 1.5rem; border-bottom: 1px solid #f3f4f6;
  display: flex; align-items: center; gap: 1rem;
}

.drawer-header h3 { font-size: 1.125rem; font-weight: 700; color: #111827; }

.back-btn {
  background: none; border: none; color: #6b7280; cursor: pointer;
  padding: 0.5rem; border-radius: 50%; display: flex; align-items: center; justify-content: center;
}
.back-btn:hover { background: #f3f4f6; color: #111827; }

.drawer-body { flex: 1; overflow-y: auto; padding: 2.5rem 2rem; }

.profile-summary {
  display: flex; flex-direction: column; align-items: center;
  text-align: center; margin-bottom: 3rem;
}

.profile-avatar-large {
  width: 120px; height: 120px; border-radius: 50%;
  background-size: cover; background-position: center;
  border: 5px solid white; box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
  margin-bottom: 1.25rem;
}

.profile-summary h4 { font-size: 1.5rem; font-weight: 700; color: #111827; margin-bottom: 0.25rem; }

.profile-code {
  font-family: monospace; background: #f3f4f6; padding: 2px 8px;
  border-radius: 4px; color: #4b5563; font-size: 0.8rem; margin-bottom: 1rem;
}

.detail-section { margin-bottom: 2.5rem; }

.section-title {
  font-size: 0.75rem; font-weight: 800; text-transform: uppercase;
  color: #9ca3af; letter-spacing: 0.1em; margin-bottom: 1.25rem;
  border-bottom: 1px solid #f3f4f6; padding-bottom: 0.5rem;
}

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


.detail-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 1.5rem; }

.detail-item { display: flex; flex-direction: column; gap: 0.4rem; }
.detail-item.block { grid-column: span 2; }
.detail-item .label { font-size: 0.75rem; color: #6b7280; font-weight: 500; }
.detail-item .value { font-size: 0.95rem; font-weight: 600; color: #111827; }

.text-danger { color: #ef4444; }
.font-bold { font-weight: 700; }

/* File Upload Style */
.file-upload-wrapper { margin-top: 0.5rem; }
.file-input-hidden { position: absolute; width: 1px; height: 1px; padding: 0; margin: -1px; overflow: hidden; clip: rect(0, 0, 0, 0); border: 0; }
.file-upload-label {
  display: flex; align-items: center; gap: 0.75rem;
  padding: 0.75rem 1rem; background: #f9fafb; border: 2px dashed #d1d5db;
  border-radius: 10px; cursor: pointer; transition: all 0.2s;
  color: #4b5563; font-size: 0.9rem;
}
.file-upload-label:hover { border-color: #3b82f6; background: #eff6ff; color: #1d4ed8; }
.file-upload-label svg { color: #9ca3af; }

/* Transitions */
.slide-right-enter-active, .slide-right-leave-active { transition: all 0.3s ease; }
.slide-right-enter-from, .slide-right-leave-to { transform: translateX(100%); opacity: 0; }

/* Bouton Relevé */
.btn-releve {
  display: flex; align-items: center; gap: 0.5rem; justify-content: center;
  width: 100%; margin-top: 1rem; padding: 0.75rem 1rem;
  background: linear-gradient(135deg, #2563eb, #1d4ed8); color: white;
  border: none; border-radius: 8px; font-size: 0.85rem; font-weight: 600;
  cursor: pointer; transition: all 0.2s;
}
.btn-releve:hover { background: linear-gradient(135deg, #1d4ed8, #1e40af); transform: translateY(-1px); box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3); }
</style>

