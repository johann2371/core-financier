<script setup>
import { ref, onMounted } from "vue";
import MainLayout from "../components/MainLayout.vue";
import { useTierStore } from "../stores/tier.store";
import { useRoute, useRouter } from "vue-router";

const store = useTierStore();
const route = useRoute();
const router = useRouter();
const showModal = ref(false);

// Onglets pour filtrer la vue (Tous, Clients, Fournisseurs)
const activeTab = ref("TOUS");

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
    }

    await store.createTier(dataToSend);
    showModal.value = false;

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
    };
  } catch (e) {
    console.error(e);
  }
};
</script>

<template>
  <MainLayout>
    <template #title>Annuaire des Tiers</template>

    <template #actions>
      <button @click="showModal = true" class="btn-primary">
        <svg
          xmlns="http://www.w3.org/2000/svg"
          width="16"
          height="16"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
        >
          <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
          <circle cx="8.5" cy="7" r="4"></circle>
          <line x1="20" y1="8" x2="20" y2="14"></line>
          <line x1="23" y1="11" x2="17" y2="11"></line>
        </svg>
        Nouveau Tier
        <span class="shortcut">Ctrl + N</span>
      </button>
    </template>

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

      <table v-else class="data-table">
        <thead>
          <tr>
            <th>Code</th>
            <th>Type</th>
            <th>Raison Sociale</th>
            <th>Contact</th>
            <th class="text-right">Solde Actuel (XAF)</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-if="
              store.tiers.filter(
                (t) => activeTab === 'TOUS' || t.type === activeTab,
              ).length === 0
            "
            class="empty-row text-center"
          >
            <td colspan="5">Aucun tier trouvé.</td>
          </tr>

          <tr
            v-for="item in store.tiers.filter(
              (t) => activeTab === 'TOUS' || t.type === activeTab,
            )"
            :key="item.id"
          >
            <td class="font-semibold text-dark">{{ item.codeTiers }}</td>
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
              <div class="motif-cell">
                <span class="motif-text">{{ item.raisonSociale }}</span>
                <span class="text-muted" style="font-size: 0.75rem">{{
                  item.email || "Pas d'email"
                }}</span>
              </div>
            </td>
            <td>{{ item.telephone || "---" }}</td>
            <td class="text-right font-semibold text-dark">
              {{ item.solde?.toLocaleString() || "0" }} XAF
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Modale de Création -->
    <div v-if="showModal" class="modal-backdrop fade-in">
      <div class="modal">
        <div class="modal-header">
          <h3>Nouveau Tier</h3>
          <button @click="showModal = false" class="close-btn">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              width="20"
              height="20"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </button>
        </div>
        <form @submit.prevent="submitForm" class="modal-body">
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

          <div class="form-group">
            <label>Raison Sociale <span class="req">*</span></label>
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

          <div v-if="store.error" class="form-error">{{ store.error }}</div>

          <div class="modal-footer pt-4 pb-0">
            <button type="button" class="btn-text" @click="showModal = false">
              Annuler
            </button>
            <button
              type="submit"
              class="btn-primary"
              :disabled="!form.raisonSociale || store.loading"
            >
              {{ store.loading ? "Création..." : "Créer et Sauvegarder" }}
            </button>
          </div>
        </form>
      </div>
    </div>
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
.btn-primary .shortcut {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 4px;
  padding: 2px 6px;
  font-size: 0.65rem;
  font-weight: 500;
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
</style>
