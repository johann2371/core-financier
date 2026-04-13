<template>
  <div class="journal-caisse-container">
    <!-- Header with Stats -->
    <header class="header-section">
      <div class="title-group">
        <h1>Journal de Caisse</h1>
        <p class="subtitle text-secondary">Gérez vos sessions de caisse et suivez les mouvements d'espèces en temps réel.</p>
      </div>
      
      <div v-if="activeSession" class="active-status-card">
        <div class="status-indicator ripple"></div>
        <div class="session-info">
          <span class="label">Session Ouverte</span>
          <span class="value">{{ formatDate(activeSession.dateOuverture) }}</span>
        </div>
        <div class="balance-badge">
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12V7H5a2 2 0 0 1 0-4h14v4"></path><path d="M3 5v14a2 2 0 0 0 2 2h16v-5"></path><path d="M18 12a2 2 0 0 0 0 4h4v-4Z"></path></svg>
          <span>Théorique: {{ formatXAF(soldeTheorique) }}</span>
        </div>
      </div>
    </header>

    <!-- Feedback Banner -->
    <div v-if="feedback.message" :class="['feedback-banner', feedback.type]" class="fade-in">
      <div class="banner-content">
        <svg v-if="feedback.type === 'success'" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>
        <svg v-else xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="16" x2="12" y2="12"></line><line x1="12" y1="8" x2="12.01" y2="8"></line></svg>
        <span>{{ feedback.message }}</span>
      </div>
      <button @click="feedback.message = ''" class="close-btn">&times;</button>
    </div>

    <!-- Main Content -->
    <main class="main-content">
      
      <!-- Scenario 1: No active session -->
      <div v-if="!activeSession && !loading" class="empty-state-container">
        <div class="opening-form card-premium fade-in">
          <div class="card-header">
            <div class="icon-circle">
              <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 10 0v4"></path></svg>
            </div>
            <h2>Ouverture de Caisse</h2>
          </div>
          
          <div class="form-body">
            <div class="form-group">
              <label>Choix de la caisse</label>
              <select v-model="openingData.caisseId" class="modern-input">
                <option v-for="c in caisses" :key="c.id" :value="c.id">{{ c.libelle }}</option>
              </select>
            </div>
            <div class="form-group mt-3">
              <label>Solde Initial (XAF)</label>
              <input 
                type="number" 
                v-model="openingData.soldeInitial" 
                class="modern-input highlight" 
                placeholder="0.00"
              />
            </div>
            <button @click="ouvrirCaisse" class="btn-primary-large mt-4" :disabled="submitting">
              <span v-if="submitting" class="spinner-small mr-2"></span>
              {{ submitting ? 'Ouverture...' : 'Ouvrir la Session' }}
            </button>
          </div>
        </div>
      </div>

      <!-- Scenario 2: Active Session -->
      <div v-else-if="activeSession" class="session-layout grid">
        
        <!-- Left Column: Movements -->
        <div class="col-8">
          <div class="card-premium h-full overflow-hidden">
            <div class="card-header-with-tabs">
              <h3>Mouvements de Caisse</h3>
              <div class="tabs">
                <button 
                  v-for="t in ['TOUS','ENTREES','SORTIES']" 
                  :key="t" 
                  @click="filterType = t"
                  :class="{ active: filterType === t }"
                >
                  {{ t === 'TOUS' ? 'Tous' : t === 'ENTREES' ? 'Recettes' : 'Dépenses' }}
                </button>
              </div>
            </div>
            
            <div class="movements-list custom-scrollbar">
              <div v-if="filteredMovements.length === 0" class="no-data">
                <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="mb-2 opacity-20"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="16" x2="12" y2="12"></line><line x1="12" y1="8" x2="12.01" y2="8"></line></svg>
                <p>Aucun mouvement enregistré pour cette session.</p>
              </div>
              
              <div v-for="m in filteredMovements" :key="m.id" class="movement-item hover-effect">
                <div class="move-icon" :class="m.type === 'ENTREE' ? 'in' : 'out'">
                  <svg v-if="m.type === 'ENTREE'" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"></line><polyline points="19 12 12 19 5 12"></polyline></svg>
                  <svg v-else xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="19" x2="12" y2="5"></line><polyline points="5 12 12 5 19 12"></polyline></svg>
                </div>
                <div class="move-details">
                  <span class="label">{{ m.reference }} — {{ m.tiersNom }}</span>
                  <span class="time">{{ formatTime(m.date) }}</span>
                </div>
                <div class="move-amount" :class="m.type === 'ENTREE' ? 'text-success' : 'text-danger'">
                  {{ m.type === 'ENTREE' ? '+' : '-' }} {{ formatXAF(m.montant) }}
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Right Column: Closing & Summary -->
        <div class="col-4">
          <div class="card-premium sticky-top">
            <div class="card-header">
              <h3>Arrêté de Caisse</h3>
            </div>
            
            <div class="form-body">
              <div class="summary-box mb-4">
                <div class="summary-line">
                  <span>Initial</span>
                  <span>{{ formatXAF(activeSession.soldeInitial) }}</span>
                </div>
                <div class="summary-line">
                  <span>+ Recettes</span>
                  <span class="text-success">{{ formatXAF(totalEntrees) }}</span>
                </div>
                <div class="summary-line">
                  <span>- Dépenses</span>
                  <span class="text-danger">{{ formatXAF(totalSorties) }}</span>
                </div>
                <hr />
                <div class="summary-line total">
                  <span>Solde Théorique</span>
                  <span class="highlight">{{ formatXAF(soldeTheorique) }}</span>
                </div>
              </div>

              <div class="form-group mb-3">
                <label>Solde Réel Compté (XAF)</label>
                <input 
                  type="number" 
                  v-model="closingData.soldeFinalReel" 
                  class="modern-input highlight"
                  placeholder="Comptez les espèces..."
                />
              </div>

              <div v-if="ecartVal !== 0" class="ecart-warning fade-in">
                <div class="ecart-header">
                  <span>Écart constaté: </span>
                  <span :class="ecartVal > 0 ? 'text-success' : 'text-warning'">{{ formatXAF(ecartVal) }}</span>
                </div>
                <textarea 
                  v-model="closingData.motifEcart" 
                  class="modern-textarea mt-2" 
                  placeholder="Justifiez l'écart (obligatoire)..."
                ></textarea>
              </div>

              <button @click="fermerCaisse" class="btn-danger-large mt-4" :disabled="submitting || (ecartVal !== 0 && !closingData.motifEcart)">
                <span v-if="submitting" class="spinner-small mr-2"></span>
                {{ submitting ? 'Calcul...' : 'Fermer la Session' }}
              </button>
            </div>
          </div>
        </div>
      </div>
 
      <!-- SECTION HISTORIQUE DES SESSIONS -->
      <section v-if="history.length > 0" class="history-section mt-5 fade-in">
        <div class="section-header">
          <div class="header-icon">
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
          </div>
          <h2 class="text-xl font-bold">Historique de mes sessions</h2>
        </div>

        <div class="table-container shadow-sm mt-3">
          <table class="modern-table">
            <thead>
              <tr>
                <th>Date Ouverture</th>
                <th>Date Fermeture</th>
                <th>Solde Initial</th>
                <th>Solde Théorique</th>
                <th>Solde Réel</th>
                <th>Écart</th>
                <th class="text-center">Rapport</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="sess in history" :key="sess.id">
                <td>{{ formatDate(sess.dateOuverture) }}</td>
                <td>{{ sess.dateFermeture ? formatDate(sess.dateFermeture) : 'Session en cours' }}</td>
                <td class="font-mono">{{ formatXAF(sess.soldeInitial) }}</td>
                <td class="font-mono">{{ sess.soldeFinalTheorique ? formatXAF(sess.soldeFinalTheorique) : '-' }}</td>
                <td class="font-mono">{{ sess.soldeFinalReel ? formatXAF(sess.soldeFinalReel) : '-' }}</td>
                <td>
                  <span v-if="sess.statut === 'FERMEE'" :class="sess.ecart === 0 ? 'status-pill success' : 'status-pill warning'">
                    {{ formatXAF(sess.ecart) }}
                  </span>
                  <span v-else>-</span>
                </td>
                <td class="text-center">
                  <button v-if="sess.statut === 'FERMEE'" @click="downloadReport(sess.id)" class="icon-btn-action" title="Télécharger le rapport détaillé">
                    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"></path><polyline points="7 10 12 15 17 10"></polyline><line x1="12" y1="15" x2="12" y2="3"></line></svg>
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

    </main>
  </div>
</template>

<script>
import { ref, onMounted, computed, reactive } from 'vue';
import { useCompteStore } from '@/stores/compte.store';
import { sessionCaisseService } from '@/services/sessionCaisseService';
import api from '@/services/api';

export default {
  setup() {
    const compteStore = useCompteStore();
    const feedback = reactive({ message: '', type: 'success' });
    const loading = ref(true);
    const submitting = ref(false);
    const activeSession = ref(null);
    const history = ref([]);
    const movements = ref([]);
    const filterType = ref('TOUS');

    const openingData = ref({
      caisseId: null,
      soldeInitial: 0
    });

    const closingData = ref({
      soldeFinalReel: 0,
      motifEcart: ''
    });

    const loadContext = async () => {
      loading.value = true;
      try {
        // 1. Charger la session active si elle existe
        const res = await sessionCaisseService.getActive();
        if (res.status === 200) {
          activeSession.value = res.data;
          await loadMovements(res.data.id);
        }
        
        // 2. Charger l'historique
        await loadHistory();

        // 3. Charger les caisses via le store
        await compteStore.fetchComptes();
        if (compteStore.caisses.length > 0) openingData.value.caisseId = compteStore.caisses[0].id;

      } catch (err) {
        console.error(err);
      } finally {
        loading.value = false;
      }
    };

    const loadHistory = async () => {
      try {
        const res = await sessionCaisseService.getMonHistorique();
        history.value = res.data;
      } catch (err) {
        console.error("Erreur historique", err);
      }
    };

    const loadMovements = async (sessionId) => {
      try {
        // Récupérer encaissements et décaissements liés à la session
        const [enc, dec] = await Promise.all([
          api.get(`/encaissements`), // On filtrera par sessionId côté front si l'API backend n'a pas encore le endpoint dédié
          api.get(`/decaissements`)
        ]);

        const sessionEnc = enc.data.filter(e => e.sessionId === sessionId);
        const sessionDec = dec.data.filter(d => d.sessionId === sessionId);

        const all = [
          ...sessionEnc.map(e => ({ type: 'ENTREE', id: e.id, reference: e.numero, montant: e.montant, date: e.dateEncaissement, tiersNom: e.nomClient })),
          ...sessionDec.map(d => ({ type: 'SORTIE', id: d.id, reference: d.numero, montant: d.montant, date: d.dateDecaissement, tiersNom: d.fournisseurNom || d.beneficiaire }))
        ];

        movements.value = all.sort((a, b) => new Date(b.date) - new Date(a.date));
      } catch (err) {
        console.error("Erreur lors du chargement des mouvements", err);
      }
    };

    const openingDataFiltered = computed(() => {
        // Logic filters
    })

    const totalEntrees = computed(() => movements.value.filter(m => m.type === 'ENTREE').reduce((acc, m) => acc + m.montant, 0));
    const totalSorties = computed(() => movements.value.filter(m => m.type === 'SORTIE').reduce((acc, m) => acc + m.montant, 0));
    const soldeTheorique = computed(() => {
        if (!activeSession.value) return 0;
        return activeSession.value.soldeInitial + totalEntrees.value - totalSorties.value;
    });

    const ecartVal = computed(() => {
        if (!activeSession.value) return 0;
        return closingData.value.soldeFinalReel - soldeTheorique.value;
    });

    const filteredMovements = computed(() => {
      if (filterType.value === 'ENTREES') return movements.value.filter(m => m.type === 'ENTREE');
      if (filterType.value === 'SORTIES') return movements.value.filter(m => m.type === 'SORTIE');
      return movements.value;
    });

    const ouvrirCaisse = async () => {
      submitting.value = true;
      try {
        const res = await sessionCaisseService.ouvrir(openingData.value);
        activeSession.value = res.data;
        feedback.message = 'Votre session a été ouverte avec succès.';
        feedback.type = 'success';
      } catch (err) {
        feedback.message = err.response?.data?.message || 'Impossible d\'ouvrir la caisse.';
        feedback.type = 'error';
      } finally {
        submitting.value = false;
      }
    };

    const fermerCaisse = async () => {
      submitting.value = true;
      try {
        const res = await sessionCaisseService.fermer(activeSession.value.id, closingData.value);
        activeSession.value = null;
        movements.value = [];
        feedback.message = 'Session clôturée avec succès.';
        feedback.type = 'success';
        await loadHistory(); // Rafraîchir l'historique après fermeture
      } catch (err) {
        feedback.message = err.response?.data?.message || 'Impossible de fermer la caisse.';
        feedback.type = 'error';
      } finally {
        submitting.value = false;
      }
    };

    const downloadReport = async (sessionId) => {
      try {
        await sessionCaisseService.downloadReport(sessionId);
      } catch (err) {
        console.error("Erreur PDF", err);
        feedback.message = "Erreur lors du téléchargement du rapport.";
        feedback.type = 'error';
      }
    };

    const formatXAF = (val) => new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'XAF' }).format(val);
    const formatDate = (date) => new Date(date).toLocaleDateString('fr-FR', { day: '2-digit', month: 'long', year: 'numeric', hour: '2-digit', minute: '2-digit' });
    const formatTime = (date) => new Date(date).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });

    onMounted(loadContext);

    return {
      activeSession, caisses: computed(() => compteStore.caisses), history, loading, submitting, openingData, closingData, feedback,
      movements, filterType, filteredMovements, totalEntrees, totalSorties, soldeTheorique, ecartVal,
      ouvrirCaisse, fermerCaisse, downloadReport, formatXAF, formatDate, formatTime
    };
  }
}
</script>

<style scoped>
.journal-caisse-container {
  padding: 2rem;
  background: #f8fafc;
  min-height: calc(100vh - 80px);
}

.feedback-banner {
  margin-bottom: 2rem;
  padding: 1rem 1.5rem;
  border-radius: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-left: 5px solid transparent;
}

.feedback-banner.success {
  background: #d1fae5;
  color: #065f46;
  border-left-color: #10b981;
}

.feedback-banner.error {
  background: #fee2e2;
  color: #991b1b;
  border-left-color: #ef4444;
}

.banner-content {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-weight: 500;
}

.feedback-banner .close-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  opacity: 0.6;
  color: currentColor;
}

.header-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2.5rem;
}

h1 { font-size: 2.2rem; font-weight: 800; color: #1e293b; margin: 0; }
.subtitle { font-size: 1rem; margin-top: 0.5rem; }

.active-status-card {
  background: white;
  padding: 1rem 1.5rem;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 1.5rem;
  box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05);
  border: 1px solid #e2e8f0;
}

.status-indicator {
  width: 12px;
  height: 12px;
  background: #10b981;
  border-radius: 50%;
  position: relative;
}

.ripple::after {
  content: '';
  position: absolute;
  top: -2px; left: -2px; right: -2px; bottom: -2px;
  border-radius: 50%;
  border: 4px solid rgba(16, 185, 129, 0.4);
  animation: ripple 1.5s infinite;
}

@keyframes ripple {
  0% { transform: scale(1); opacity: 1; }
  100% { transform: scale(1.8); opacity: 0; }
}

.balance-badge {
  background: #f1f5f9;
  padding: 0.5rem 1rem;
  border-radius: 8px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: #334155;
  border: 1px solid #cbd5e1;
}

.card-premium {
  background: white;
  border-radius: 20px;
  border: 1px solid #eaedef;
  box-shadow: 0 10px 25px -5px rgba(0,0,0,0.05);
}

.spinner-small {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: white;
  border-radius: 50%;
  display: inline-block;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.opening-form {
  max-width: 450px;
  margin: 5rem auto;
  padding: 2.5rem;
  text-align: center;
}

.icon-circle {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, #3b82f610, #3b82f630);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 1.5rem;
  color: #3b82f6;
  font-size: 2rem;
}

.modern-input {
  width: 100%;
  padding: 0.8rem 1.2rem;
  border: 2px solid #f1f5f9;
  border-radius: 10px;
  font-size: 1rem;
  transition: all 0.2s;
  background: #f8fafc;
}

.modern-input:focus {
  outline: none;
  border-color: #3b82f6;
  background: white;
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.1);
}

.modern-input.highlight {
  font-size: 1.5rem;
  font-weight: 800;
  text-align: center;
  color: #1e293b;
}

.btn-primary-large {
  width: 100%;
  padding: 1rem;
  background: #3b82f6;
  color: white;
  border: none;
  border-radius: 12px;
  font-weight: 700;
  font-size: 1.1rem;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary-large:hover { background: #2563eb; transform: translateY(-2px); box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3); }

.card-header-with-tabs {
  padding: 1.5rem;
  border-bottom: 1px solid #f1f5f9;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tabs {
  background: #f1f5f9;
  padding: 4px;
  border-radius: 10px;
  display: flex;
  gap: 4px;
}

.tabs button {
  padding: 0.5rem 1rem;
  border: none;
  background: transparent;
  border-radius: 8px;
  font-weight: 600;
  font-size: 0.9rem;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s;
}

.tabs button.active {
  background: white;
  color: #3b82f6;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
}

.movements-list {
  max-height: 600px;
  overflow-y: auto;
  padding: 1rem;
}

.movement-item {
  display: flex;
  align-items: center;
  padding: 1rem;
  border-radius: 12px;
  margin-bottom: 0.5rem;
  transition: all 0.2s;
}

.movement-item:hover { background: #f8fafc; }

.move-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 1rem;
}

.move-icon.in { background: #dcfce7; color: #16a34a; }
.move-icon.out { background: #fee2e2; color: #dc2626; }

.move-details { flex: 1; display: flex; flex-direction: column; }
.move-details .label { font-weight: 700; color: #1e293b; }
.move-details .time { font-size: 0.85rem; color: #94a3b8; }

.move-amount { font-weight: 800; font-size: 1.1rem; }

.summary-box {
  background: #f8fafc;
  padding: 1.5rem;
  border-radius: 15px;
  border: 1px dashed #cbd5e1;
}

.summary-line {
  display: flex;
  justify-content: space-between;
  margin-bottom: 0.8rem;
  font-size: 1rem;
  color: #475569;
}

.summary-line.total {
  margin-top: 1rem;
  font-size: 1.2rem;
  font-weight: 800;
  color: #0f172a;
}

.ecart-warning {
  background: #fffbeb;
  border: 1px solid #fde68a;
  padding: 1rem;
  border-radius: 12px;
  margin-top: 1.5rem;
}

.modern-textarea {
  width: 100%;
  border: 1px solid #fde68a;
  border-radius: 8px;
  padding: 0.8rem;
  min-height: 80px;
  font-family: inherit;
}

.btn-danger-large {
  width: 100%;
  padding: 1rem;
  background: #ef4444;
  color: white;
  border: none;
  border-radius: 12px;
  font-weight: 700;
  font-size: 1.1rem;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-danger-large:hover:not(:disabled) { background: #dc2626; transform: translateY(-2px); box-shadow: 0 4px 12px rgba(239, 68, 68, 0.3); }
.btn-danger-large:disabled { opacity: 0.6; cursor: not-allowed; }

.sticky-top { position: sticky; top: 2rem; }
.text-success { color: #16a34a; }
.text-danger { color: #dc2626; }
.text-warning { color: #d97706; }
.mt-3 { margin-top: 1rem; }
.mt-4 { margin-top: 1.5rem; }
.mb-4 { margin-bottom: 1.5rem; }

.grid { display: flex; gap: 2rem; }
.col-8 { flex: 8; }
.col-4 { flex: 4; }

.custom-scrollbar::-webkit-scrollbar { width: 6px; }
.custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: #e2e8f0; border-radius: 10px; }

/* HISTORY SECTION STYLES */
.history-section {
  background: white;
  padding: 2rem;
  border-radius: 20px;
  border: 1px solid #eaedef;
  box-shadow: 0 10px 25px -5px rgba(0,0,0,0.05);
}

.section-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.header-icon {
  background: #f1f5f9;
  color: #64748b;
  padding: 0.6rem;
  border-radius: 10px;
}

.table-container {
  overflow-x: auto;
  border-radius: 12px;
  border: 1px solid #f1f5f9;
}

.modern-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}

.modern-table th {
  background: #f8fafc;
  padding: 1rem;
  font-weight: 700;
  color: #475569;
  font-size: 0.85rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  border-bottom: 2px solid #f1f5f9;
}

.modern-table td {
  padding: 1rem;
  border-bottom: 1px solid #f1f5f9;
  color: #1e293b;
  font-size: 0.95rem;
}

.modern-table tr:hover {
  background: #f8fafc;
}

.font-mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
  font-weight: 600;
}

.status-pill {
  padding: 0.25rem 0.75rem;
  border-radius: 20px;
  font-size: 0.85rem;
  font-weight: 700;
}

.status-pill.success {
  background: #dcfce7;
  color: #16a34a;
}

.status-pill.warning {
  background: #fee2e2;
  color: #dc2626;
}

.icon-btn-action {
  background: #eff6ff;
  color: #3b82f6;
  border: none;
  padding: 0.5rem;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.icon-btn-action:hover {
  background: #3b82f6;
  color: white;
  transform: scale(1.1);
}

.fade-in {
  animation: fadeIn 0.5s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
