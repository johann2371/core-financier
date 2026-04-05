<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import MainLayout from '../components/MainLayout.vue'
import { useAuditStore } from '../stores/audit.store'
import jsPDF from 'jspdf'
import autoTable from 'jspdf-autotable'

const auditStore = useAuditStore()
const currentPage = ref(1)

const filters = ref({
  search: '',
  action: '',
  role: '',
  dateFrom: '',
  dateTo: ''
})

const actions = [
  { value: '', label: 'All Action Types' },
  { value: 'CREATE', label: 'Création' },
  { value: 'UPDATE', label: 'Modification' },
  { value: 'DELETE', label: 'Suppression' },
  { value: 'LOGIN', label: 'Connexion' },
  { value: 'VALIDATE', label: 'Validation' },
  { value: 'APPROVE', label: 'Approbation' },
  { value: 'EXECUTE', label: 'Exécution' },
  { value: 'REJECT', label: 'Rejet' }
]

const fetchLogs = async () => {
  await auditStore.fetchLogs(currentPage.value - 1, auditStore.pageSize, filters.value.search, filters.value.action)
}

onMounted(fetchLogs)

watch(currentPage, fetchLogs)

const filteredLogs = computed(() => {
  let logs = auditStore.logs
  if (filters.value.role) {
    logs = logs.filter(l => l.utilisateur?.role === filters.value.role)
  }
  if (filters.value.dateFrom) {
    const from = new Date(filters.value.dateFrom)
    from.setHours(0, 0, 0, 0)
    logs = logs.filter(l => new Date(l.dateAction) >= from)
  }
  if (filters.value.dateTo) {
    const to = new Date(filters.value.dateTo)
    to.setHours(23, 59, 59, 999)
    logs = logs.filter(l => new Date(l.dateAction) <= to)
  }
  return logs
})

// === EXPORT PDF ===
const exportPDF = () => {
  const doc = new jsPDF({ orientation: 'landscape' })
  doc.setFontSize(16)
  doc.text('Journal d\'Audit - SODICA', 14, 15)
  doc.setFontSize(9)
  doc.text(`Généré le ${new Date().toLocaleString('fr-FR')}`, 14, 22)

  const rows = filteredLogs.value.map(l => [
    new Date(l.dateAction).toLocaleString('fr-FR'),
    l.utilisateur ? `${l.utilisateur.prenom} ${l.utilisateur.nom}` : 'Système',
    l.action || '',
    l.entite || '',
    l.entiteId || '',
    l.adresseIp || 'N/A'
  ])

  autoTable(doc, {
    startY: 28,
    head: [['Date', 'Utilisateur', 'Action', 'Module', 'ID Entité', 'Adresse IP']],
    body: rows,
    styles: { fontSize: 8 },
    headStyles: { fillColor: [59, 130, 246] }
  })

  doc.save(`audit_${new Date().toISOString().slice(0, 10)}.pdf`)
}

// === EXPORT CSV ===
const exportCSV = () => {
  const header = 'Date,Utilisateur,Role,Action,Module,ID Entité,Adresse IP\n'
  const rows = filteredLogs.value.map(l =>
    [
      new Date(l.dateAction).toLocaleString('fr-FR'),
      l.utilisateur ? `${l.utilisateur.prenom} ${l.utilisateur.nom}` : 'Système',
      l.utilisateur?.role || '',
      l.action || '',
      l.entite || '',
      l.entiteId || '',
      l.adresseIp || 'N/A'
    ].join(',')
  ).join('\n')

  const blob = new Blob([header + rows], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `audit_${new Date().toISOString().slice(0, 10)}.csv`
  link.click()
}

const formatValues = (values) => {
  if (!values) return 'N/A'
  try {
    // Si c'est déjà un objet, on le stringify proprement
    if (typeof values === 'object') return JSON.stringify(values, null, 2)
    // Si c'est un JSON string, on tente de le parser pour le rertourner formatté
    const parsed = JSON.parse(values)
    return JSON.stringify(parsed, null, 2)
  } catch (e) {
    return values
  }
}

const getActionBadgeClass = (action) => {
  switch (action) {
    case 'CREATE': return 'badge-success'
    case 'UPDATE': return 'badge-info'
    case 'DELETE': return 'badge-danger'
    case 'REJECT': return 'badge-danger'
    case 'VALIDATE': return 'badge-success-light'
    case 'APPROVE': return 'badge-success-light'
    case 'EXECUTE': return 'badge-primary'
    case 'LOGIN': return 'badge-warning'
    default: return 'badge-secondary'
  }
}

const showDetailsModal = ref(false)
const selectedLog = ref(null)

const openDetails = (log) => {
  selectedLog.value = log
  showDetailsModal.value = true
}

// MOCKUP HELPERS
const getAvatarColor = (name) => {
  if(!name) return '#cbd5e1'
  const colors = ['#0f766e', '#3b82f6', '#f59e0b', '#8b5cf6', '#ec4899']
  let hash = 0;
  for (let i = 0; i < name.length; i++) hash = name.charCodeAt(i) + ((hash << 5) - hash)
  return colors[Math.abs(hash) % colors.length]
}

const getMockModule = (entite) => {
  const map = {
    'Facture': 'Accounts Payable',
    'Decaissement': 'Treasury',
    'Encaissement': 'Receivables',
    'Utilisateur': 'System Auth',
    'Parametrage': 'Global Settings'
  }
  return map[entite] || 'Asset Mgmt'
}

const generateMockDetail = (log) => {
  if(log.action === 'VALIDATE' || log.action === 'APPROVE') return `Validated transaction ID-${log.entiteId || '992'} for payment`
  if(log.action === 'DELETE') return `Deleted draft record from ${log.entite}`
  if(log.action === 'CREATE') return `Created new record in ${log.entite} module`
  if(log.action === 'LOGIN') return `Admin console access granted (MFA Success)`
  return `Automated sync or modification on ${log.entite}`
}

// === DYNAMIC KPIS ===
const todaysLogsCount = computed(() => {
  const todayStr = new Date().toDateString()
  return auditStore.logs.filter(l => new Date(l.dateAction).toDateString() === todayStr).length
})

const criticalActionsCount = computed(() => {
  return auditStore.logs.filter(l => ['DELETE', 'REJECT', 'SUPPRESSION'].includes(l.action)).length
})

const validationsCount = computed(() => {
  return auditStore.logs.filter(l => ['VALIDATE', 'APPROVE', 'VALIDATION', 'APPROBATION'].includes(l.action)).length
})

const uniqueIpsCount = computed(() => {
  const ips = new Set()
  auditStore.logs.forEach(l => {
    if(l.adresseIp) ips.add(l.adresseIp)
  })
  return ips.size
})
</script>

<template>
  <MainLayout>
    <template #title>System Audit Log</template>
    <template #subtitle>Traceability and security monitoring for every system action.</template>
    
    <template #actions>
      <button @click="exportPDF" class="btn-header-export border-btn">
        <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg>
        Export PDF
      </button>
      <button @click="exportCSV" class="btn-header-export solid-btn">
        <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="7 10 12 15 17 10"></polyline><line x1="12" y1="15" x2="12" y2="3"></line></svg>
        Export CSV
      </button>
    </template>

    <div class="audit-dashboard">
      <!-- Filtres type Mockup -->
      <div class="mockup-filter-bar">
        <div class="filter-input-mock">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="2"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
          <input v-model="filters.search" @keyup.enter="fetchLogs" type="text" placeholder="Rechercher utilisateur, entité..." />
          <button v-if="filters.search" @click="filters.search = ''; fetchLogs()" style="background:none; border:none; cursor:pointer; color:#ef4444; margin-left:8px; display: flex; align-items: center;">
            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
          </button>
        </div>
        <div class="filter-input-mock date-range">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line><line x1="3" y1="10" x2="21" y2="10"></line></svg>
          <input v-model="filters.dateFrom" type="date" title="Date début" />
          <span style="color:#94a3b8; font-size:0.85rem;">→</span>
          <input v-model="filters.dateTo" type="date" title="Date fin" />
        </div>
        <div class="filter-input-mock select">
          <select v-model="filters.role">
            <option value="">Tous les rôles</option>
            <option value="CAISSIER">Caissier</option>
            <option value="COMPTABLE">Comptable</option>
            <option value="RESPONSABLE_FINANCIER">Resp. Financier</option>
            <option value="PDG">PDG</option>
            <option value="ADMINISTRATEUR">Administrateur</option>
          </select>
          <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"></polyline></svg>
        </div>
        <div class="filter-input-mock select">
          <select v-model="filters.action" @change="fetchLogs">
            <option v-for="a in actions" :key="a.value" :value="a.value">{{ a.label }}</option>
          </select>
          <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="2"><line x1="4" y1="21" x2="4" y2="14"></line><line x1="4" y1="10" x2="4" y2="3"></line><line x1="12" y1="21" x2="12" y2="12"></line><line x1="12" y1="8" x2="12" y2="3"></line><line x1="20" y1="21" x2="20" y2="16"></line><line x1="20" y1="12" x2="20" y2="3"></line><line x1="1" y1="14" x2="7" y2="14"></line><line x1="9" y1="8" x2="15" y2="8"></line><line x1="17" y1="16" x2="23" y2="16"></line></svg>
        </div>
        
        <button class="btn-apply-mock">Apply Filters</button>
        <button @click="fetchLogs" class="btn-refresh-mock">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#64748b" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21.5 2v6h-6M2.13 15.57a9 9 0 1 0 3.87-11.43L2.5 8"></path></svg>
        </button>
      </div>

      <!-- Table -->
      <div class="table-card-mock">
        <div v-if="auditStore.loading" class="loading-state">Loading audit logs...</div>
        
        <div v-else class="table-scroll">
          <table class="mock-table">
            <thead>
              <tr>
                <th>TIMESTAMP</th>
                <th>USER</th>
                <th>ACTION TYPE</th>
                <th>MODULE</th>
                <th>DETAILS</th>
                <th>IP ADDRESS</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="log in filteredLogs" :key="log.id">
                <!-- Timestamp -->
                <td class="log-date-col">
                  <div class="log-date">{{ new Date(log.dateAction).toLocaleDateString('en-US', {month:'short', day:'2-digit', year:'numeric'}) }}</div>
                  <div class="log-time">{{ new Date(log.dateAction).toLocaleTimeString('en-US', {hour12:false}) }} UTC</div>
                </td>
                
                <!-- USER -->
                <td>
                  <div class="mock-user-cell">
                    <div class="mock-avatar" :style="{ backgroundColor: getAvatarColor(log.utilisateur?.prenom), color: 'white' }">
                      {{ log.utilisateur?.prenom ? log.utilisateur.prenom.charAt(0) + (log.utilisateur.nom ? log.utilisateur.nom.charAt(0) : '') : 'SY' }}
                    </div>
                    <div class="mock-user-info">
                      <strong class="mock-user-name">{{ log.utilisateur ? `${log.utilisateur.prenom} ${log.utilisateur.nom}` : 'Système' }}</strong>
                      <span class="mock-role">{{ log.utilisateur?.role ? log.utilisateur.role.replace('_', ' ') : 'System Admin' }}</span>
                    </div>
                  </div>
                </td>
                
                <!-- ACTION TYPE -->
                <td>
                  <div class="mock-badge" :class="getActionBadgeClass(log.action)">
                    {{ log.action || 'INCONNU' }}
                  </div>
                </td>
                
                <!-- MODULE -->
                <td class="mock-module">{{ log.entite || 'Général' }}</td>
                
                <!-- DETAILS -->
                <td class="mock-details">
                  {{ log.entiteId ? `Action sur la ressource #${log.entiteId}` : 'Modification globale' }}
                  <button v-if="log.nouvellesValeurs || log.anciennesValeurs" class="btn-link" style="margin-left:8px; font-size:12px; color:#2563eb; background:none; border:none; cursor:pointer;" @click="openDetails(log)">Détails</button>
                </td>
                
                <!-- IP -->
                <td class="mock-ip">{{ log.adresseIp || 'N/A' }}</td>
              </tr>
              <tr v-if="filteredLogs.length === 0">
                <td colspan="6" class="text-center" style="padding: 2rem; color: #64748b;">Aucun événement d'audit trouvé.</td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- NEW MOCK PAGINATION ATTACHED TO TABLE -->
        <div class="mock-pagination-bar" v-if="auditStore.totalPages > 1">
          <div class="pag-left">Affichage de <strong>{{ (auditStore.currentPage * auditStore.pageSize) + 1 }} à {{ Math.min((auditStore.currentPage + 1) * auditStore.pageSize, auditStore.totalElements) }}</strong> sur <strong>{{ auditStore.totalElements }}</strong> événements</div>
          <div class="pag-center">
            <button class="pag-btn" :disabled="auditStore.currentPage === 0" @click="auditStore.fetchLogs(auditStore.currentPage - 1)">&lt;</button>
            <button 
                v-for="page in auditStore.totalPages" 
                :key="page" 
                class="pag-btn" 
                :class="{ active: auditStore.currentPage === (page - 1) }"
                @click="auditStore.fetchLogs(page - 1)"
            >
              {{ page }}
            </button>
            <button class="pag-btn" :disabled="auditStore.currentPage >= auditStore.totalPages - 1" @click="auditStore.fetchLogs(auditStore.currentPage + 1)">&gt;</button>
          </div>
          <div class="pag-right">
            Limiter à: <strong>{{ auditStore.pageSize }}</strong> / page
          </div>
        </div>
      </div>

      <!-- WIDGETS ROW -->
      <div class="mock-widgets-row">
        <div class="mock-widget">
          <div class="mw-head">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>
            <span>JOURNAL EN DIRECT</span>
          </div>
          <div class="mw-value">{{ auditStore.totalElements.toLocaleString() }}</div>
          <div class="mw-footer text-green">
            <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="23 6 13.5 15.5 8.5 10.5 1 18"></polyline><polyline points="17 6 23 6 23 12"></polyline></svg>
            Total logs enregistrés
          </div>
        </div>
        
        <div class="mock-widget">
          <div class="mw-head">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#ef4444" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path></svg>
            <span class="text-red">ACTIONS CRITIQUES</span>
          </div>
          <div class="mw-value">{{ criticalActionsCount }}</div>
          <div class="mw-footer">Sur la page actuelle</div>
        </div>

        <div class="mock-widget">
          <div class="mw-head">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#22c55e" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>
            <span class="text-green">VALIDATIONS</span>
          </div>
          <div class="mw-value">{{ validationsCount }}</div>
          <div class="mw-footer">Sur la page actuelle</div>
        </div>

        <div class="mock-widget">
          <div class="mw-head">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#3b82f6" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="2" width="20" height="8" rx="2" ry="2"></rect><rect x="2" y="14" width="20" height="8" rx="2" ry="2"></rect><line x1="6" y1="6" x2="6.01" y2="6"></line><line x1="6" y1="18" x2="6.01" y2="18"></line></svg>
            <span class="text-blue">IPS UNIQUES</span>
          </div>
          <div class="mw-value">{{ uniqueIpsCount }}</div>
          <div class="mw-footer">Sur la page actuelle</div>
        </div>
      </div>
    </div>

    <!-- Modal Détails -->
    <div v-if="showDetailsModal" class="modal-backdrop fade-in" @click.self="showDetailsModal = false">
      <div class="modal modal-lg">
        <div class="modal-header">
          <h3>Détails du Log #{{ selectedLog?.id }}</h3>
          <button @click="showDetailsModal = false" class="close-btn">&times;</button>
        </div>
        <div class="modal-body">
          <div class="audit-details-grid">
            <div class="detail-item">
              <label>Date</label>
              <span>{{ new Date(selectedLog?.dateAction).toLocaleString() }}</span>
            </div>
            <div class="detail-item">
              <label>Utilisateur</label>
              <span>{{ selectedLog?.utilisateur ? `${selectedLog.utilisateur.prenom} ${selectedLog.utilisateur.nom}` : 'SYSTEM' }}</span>
            </div>
            <div class="detail-item">
              <label>Action</label>
              <span class="badge" :class="getActionBadgeClass(selectedLog?.action)">{{ selectedLog?.action }}</span>
            </div>
            <div class="detail-item">
              <label>Entité</label>
              <span>{{ selectedLog?.entite }} (ID: {{ selectedLog?.entiteId }})</span>
            </div>
          </div>

          <div class="mt-4">
            <label class="section-label">ANCIEENNES VALEURS</label>
            <pre class="code-block">{{ formatValues(selectedLog?.anciennesValeurs) }}</pre>
          </div>

          <div class="mt-4">
            <label class="section-label">NOUVELLES VALEURS</label>
            <pre class="code-block">{{ formatValues(selectedLog?.nouvellesValeurs) }}</pre>
          </div>
        </div>
        <div class="modal-footer">
          <button @click="showDetailsModal = false" class="btn-primary">Fermer</button>
        </div>
      </div>
    </div>

  </MainLayout>
</template>

<style scoped>
.audit-dashboard {
  background: var(--bg-body);
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

/* Header Buttons */
.btn-header-export {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  font-size: 0.875rem;
  font-weight: 600;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}
.border-btn {
  background: white;
  border: 1px solid #e2e8f0;
  color: #334155;
}
.border-btn:hover { background: #f8fafc; }
.solid-btn {
  background: #3b82f6;
  border: 1px solid #3b82f6;
  color: white;
}
.solid-btn:hover { background: #2563eb; }

/* Filter Bar Mockup */
.mockup-filter-bar {
  display: flex;
  align-items: center;
  gap: 1rem;
  background: white;
  padding: 1rem 1.25rem;
  border-radius: 12px;
  border: 1px solid #f1f5f9;
  box-shadow: 0 1px 3px rgba(0,0,0,0.02);
}

.filter-input-mock {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #f8fafc;
  flex: 1;
}
.filter-input-mock input {
  border: none;
  background: transparent;
  outline: none;
  font-size: 0.875rem;
  color: #334155;
  width: 100%;
}
.filter-input-mock input::placeholder { color: #94a3b8; }
.text-gray { color: #64748b; font-size: 0.875rem; }

.filter-input-mock.select select {
  border: none;
  background: transparent;
  outline: none;
  appearance: none;
  font-size: 0.875rem;
  color: #334155;
  width: 100%;
  cursor: pointer;
}

.btn-apply-mock {
  background: #eff6ff;
  color: #2563eb;
  border: none;
  padding: 0.6rem 1.2rem;
  font-weight: 600;
  font-size: 0.875rem;
  border-radius: 6px;
  cursor: pointer;
  white-space: nowrap;
}
.btn-apply-mock:hover { background: #dbeafe; }

.btn-refresh-mock {
  background: transparent;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}
.btn-refresh-mock:hover { background: #f8fafc; }

/* Table Mockup */
.table-card-mock {
  background: white;
  border-radius: 12px;
  border: 1px solid #f1f5f9;
  box-shadow: 0 1px 3px rgba(0,0,0,0.02);
  display: flex;
  flex-direction: column;
}
.mock-table {
  width: 100%;
  border-collapse: collapse;
}
.mock-table th {
  padding: 1.25rem;
  text-align: left;
  font-size: 0.75rem;
  font-weight: 700;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  border-bottom: 1px solid #f1f5f9;
}
.mock-table td {
  padding: 1.25rem;
  border-bottom: 1px solid #f1f5f9;
  vertical-align: top;
}

.log-date-col { width: 140px; }
.log-date { font-weight: 600; color: #334155; font-size: 0.875rem; margin-bottom: 0.15rem; }
.log-time { color: #94a3b8; font-size: 0.75rem; }

.mock-user-cell {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}
.mock-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  font-weight: 700;
}
.mock-user-info { display: flex; flex-direction: column; }
.mock-user-name { font-size: 0.875rem; color: #1e293b; }
.mock-role { font-size: 0.75rem; color: #64748b; background: #f1f5f9; padding: 2px 6px; border-radius: 4px; display: inline-block; margin-top: 2px; }

/* Badges Action Types */
.mock-badge {
  display: inline-flex;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 0.7rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}
.badge-validate, .badge-validation, .badge-approve { background: #dcfce7; color: #166534; }
.badge-delete, .badge-deletion, .badge-reject { background: #fee2e2; color: #991b1b; }
.badge-create, .badge-creation { background: #dbeafe; color: #1e40af; }
.badge-login { background: #f3e8ff; color: #6b21a8; }
.badge-modification, .badge-update { background: #fef3c7; color: #92400e; }

.mock-module { font-size: 0.875rem; color: #64748b; }
.mock-details { font-size: 0.875rem; color: #475569; max-width: 320px; }
.mock-ip { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace; font-size: 0.75rem; color: #64748b; letter-spacing: -0.02em; }

/* Mock Pagination */
.mock-pagination-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.25rem;
  border-top: 1px solid #f1f5f9;
  font-size: 0.875rem;
  color: #64748b;
}
.mock-pagination-bar strong { color: #334155; }
.pag-center { display: flex; align-items: center; gap: 0.25rem; }
.pag-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #e2e8f0;
  background: white;
  border-radius: 6px;
  color: #475569;
  font-weight: 600;
  font-size: 0.875rem;
  cursor: pointer;
}
.pag-btn.active {
  background: #2563eb;
  color: white;
  border-color: #2563eb;
}
.pag-ellipsis { padding: 0 0.5rem; }
.pag-right { display: flex; align-items: center; gap: 0.5rem; }

/* Widgets */
.mock-widgets-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1.25rem;
}
.mock-widget {
  background: white;
  border-radius: 12px;
  padding: 1.25rem;
  border: 1px solid #f1f5f9;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  box-shadow: 0 1px 3px rgba(0,0,0,0.02);
}
.mw-head {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.75rem;
  font-weight: 700;
  color: #94a3b8;
  letter-spacing: 0.05em;
}
.text-red { color: #ef4444 !important; }
.text-green { color: #22c55e !important; }
.text-blue { color: #3b82f6 !important; }

.mw-value {
  font-size: 1.75rem;
  font-weight: 800;
  color: #0f172a;
}
.mw-footer {
  font-size: 0.75rem;
  color: #94a3b8;
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

/* === MODALE DÉTAILS AUDIT === */
.modal-backdrop { position: fixed; inset: 0; z-index: 9999; background: rgba(15,23,42,0.5); backdrop-filter: blur(4px); display: flex; align-items: center; justify-content: center; }
.modal-backdrop.fade-in { animation: fadeIn 0.2s ease; }
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
.modal { background: white; border-radius: 14px; width: 90%; max-width: 640px; max-height: 85vh; display: flex; flex-direction: column; box-shadow: 0 25px 50px -12px rgba(0,0,0,0.2); animation: slideUp 0.25s ease; overflow: hidden; }
@keyframes slideUp { from { transform: translateY(16px); opacity: 0; } to { transform: translateY(0); opacity: 1; } }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 1.25rem 1.5rem; border-bottom: 1px solid #f1f5f9; }
.modal-header h3 { margin: 0; font-size: 1.05rem; font-weight: 700; color: #1e293b; }
.close-btn { background: none; border: none; font-size: 1.5rem; color: #94a3b8; cursor: pointer; padding: 4px 8px; border-radius: 6px; line-height: 1; transition: all 0.15s; }
.close-btn:hover { color: #ef4444; background: #fef2f2; }
.modal-body { padding: 1.5rem; overflow-y: auto; flex: 1; }
.modal-footer { padding: 1rem 1.5rem; border-top: 1px solid #f1f5f9; display: flex; justify-content: flex-end; background: #f8fafc; }
.modal-footer .btn-primary { background: #3b82f6; color: white; border: none; padding: 0.5rem 1.25rem; border-radius: 8px; font-weight: 600; font-size: 0.85rem; cursor: pointer; transition: all 0.15s; }
.modal-footer .btn-primary:hover { background: #2563eb; }

.audit-details-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
.detail-item { display: flex; flex-direction: column; gap: 4px; }
.detail-item label { font-size: 0.7rem; font-weight: 700; color: #94a3b8; text-transform: uppercase; letter-spacing: 0.05em; }
.detail-item span { font-size: 0.9rem; color: #1e293b; font-weight: 500; }

.section-label { display: block; font-size: 0.7rem; font-weight: 700; color: #94a3b8; text-transform: uppercase; letter-spacing: 0.05em; margin-bottom: 0.5rem; }
.code-block { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 0.75rem 1rem; font-family: ui-monospace, SFMono-Regular, monospace; font-size: 0.8rem; color: #334155; white-space: pre-wrap; word-break: break-all; max-height: 200px; overflow-y: auto; margin: 0; }
.mt-4 { margin-top: 1rem; }

/* ========== RESPONSIVE ========== */
@media (max-width: 1024px) {
  .mock-widgets-row { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 768px) {
  .audit-dashboard { gap: 1rem; }

  /* Filtres en colonne */
  .mockup-filter-bar { flex-direction: column; gap: 0.5rem; align-items: stretch; }
  .filter-input-mock { width: 100%; }
  .filter-input-mock.date-range { flex-wrap: wrap; }
  .filter-input-mock.date-range input { flex: 1; min-width: 100px; }
  .filter-input-mock.select { width: 100%; }

  /* Table scrollable */
  .mock-table-wrap { overflow-x: auto; -webkit-overflow-scrolling: touch; }
  .mock-table { min-width: 700px; }
  .mock-table th, .mock-table td { padding: 0.75rem; font-size: 0.75rem; }
  .log-date-col { width: 110px; }

  /* Widgets KPIs */
  .mock-widgets-row { grid-template-columns: 1fr 1fr; gap: 0.75rem; }
  .mock-widget { padding: 0.85rem; }
  .mw-value { font-size: 1.25rem; }

  /* Pagination */
  .mock-pagination-bar { flex-direction: column; gap: 0.5rem; padding: 0.75rem; }
  .pag-left { font-size: 0.75rem; text-align: center; }
  .pag-right { justify-content: center; }

  /* Boutons export header */
  .btn-header-export { font-size: 0.75rem; padding: 0.4rem 0.75rem; }
}

@media (max-width: 480px) {
  .mock-widgets-row { grid-template-columns: 1fr; }
  .mock-table { min-width: 600px; }
  .mock-table th, .mock-table td { padding: 0.5rem; font-size: 0.7rem; }
  .mock-avatar { width: 26px; height: 26px; font-size: 0.65rem; }
  .mock-user-name { font-size: 0.75rem; }
  .pag-btn { width: 28px; height: 28px; font-size: 0.75rem; }
}
</style>
