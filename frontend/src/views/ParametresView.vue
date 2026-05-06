<script setup>
import { ref, onMounted, computed } from 'vue'
import MainLayout from '../components/MainLayout.vue'
import { useParametrageStore } from '../stores/parametrage.store'
import { useLangStore } from '../stores/lang.store'
import { useAuthStore } from '../stores/auth.store'
import api from '../services/api'
import { 
  BuildingOfficeIcon, 
  CurrencyDollarIcon, 
  ShieldCheckIcon, 
  Cog6ToothIcon,
  BanknotesIcon,
  LockClosedIcon,
  ClockIcon,
  MapPinIcon,
  PhoneIcon,
  EnvelopeIcon,
  PhotoIcon,
  BellIcon,
  TagIcon,
  CogIcon
} from '@heroicons/vue/24/outline'

const authStore = useAuthStore()
const langStore = useLangStore()
const t = computed(() => langStore.t)
const parametrageStore = useParametrageStore()

const isAdmin = computed(() => authStore.userRole === 'ADMINISTRATEUR')

onMounted(() => {
  parametrageStore.fetchParametres()
})

const activeTab = ref('IDENTITE')
const categories = computed(() => [
  { id: 'IDENTITE', label: t('parametres.identiteLogo'), icon: BuildingOfficeIcon, desc: t('parametres.identiteDesc') },
  { id: 'FINANCE', label: t('parametres.gestionFinanciere'), icon: CurrencyDollarIcon, desc: t('parametres.financeDesc') },
  { id: 'SECURITE', label: t('parametres.securiteAcces'), icon: ShieldCheckIcon, desc: t('parametres.securiteDesc') },
  { id: 'SYSTEME', label: t('parametres.systeme'), icon: Cog6ToothIcon, desc: t('parametres.systemeDesc') }
])

const getCategory = (cle) => {
  const c = cle.toUpperCase()
  if (c.includes('INFO_SOCIETE') || c.includes('LOGO')) return 'IDENTITE'
  if (c.includes('SEUIL') || c.includes('MONTANT') || c.includes('DEVISE') || c.includes('APPROBATION') || c.includes('PIECE')) return 'FINANCE'
  if (c.includes('TENTATIV') || c.includes('BLOCAGE') || c.includes('SESSION') || c.includes('DUREE_SESSION')) return 'SECURITE'
  return 'SYSTEME'
}

const filteredParametres = computed(() => {
  return (parametrageStore.parametres || []).filter(p => getCategory(p.cle) === activeTab.value)
})

const getParamIconInfo = (cle) => {
  const c = cle.toUpperCase()
  if (c.includes('SEUIL') || c.includes('MONTANT')) return { component: CurrencyDollarIcon, class: 'icon-dollar' }
  if (c.includes('DEVISE')) return { component: BanknotesIcon, class: 'icon-currency' }
  if (c.includes('TENTATIV') || c.includes('BLOCAGE')) return { component: LockClosedIcon, class: 'icon-lock' }
  if (c.includes('SESSION') || c.includes('DUREE')) return { component: ClockIcon, class: 'icon-clock' }
  if (c.includes('SOCIETE_NOM')) return { component: BuildingOfficeIcon, class: 'icon-building' }
  if (c.includes('SOCIETE_ADRESSE')) return { component: MapPinIcon, class: 'icon-map' }
  if (c.includes('SOCIETE_TEL')) return { component: PhoneIcon, class: 'icon-phone' }
  if (c.includes('SOCIETE_EMAIL')) return { component: EnvelopeIcon, class: 'icon-mail' }
  if (c.includes('LOGO')) return { component: PhotoIcon, class: 'icon-image' }
  if (c.includes('NOTIF')) return { component: BellIcon, class: 'icon-bell' }
  if (c.includes('VERSION')) return { component: TagIcon, class: 'icon-tag' }
  return { component: CogIcon, class: 'icon-settings' }
}

// === GESTION CONFIGURATION ===
const editConfigMode = ref(null)
const editConfigValue = ref('')

const startEditConfig = (param) => {
  editConfigMode.value = param.cle
  editConfigValue.value = param.valeur
}

const saveConfig = async (param) => {
  try {
    await parametrageStore.updateParametre(param.cle, { valeur: editConfigValue.value, description: param.description })
    editConfigMode.value = null
  } catch(e) {
    console.error("Erreur maj var", e)
  }
}

const cancelEditConfig = () => { editConfigMode.value = null }

// === GESTION LOGOS ===
const uploadingLogo = ref(null) // 'app' ou 'invoice'

const getLogoUrl = (type) => {
  const cle = type === 'app' ? 'APP_LOGO_URL' : 'INVOICE_LOGO_URL'
  const param = (parametrageStore.parametres || []).find(p => p.cle === cle)
  return param?.valeur || ''
}

const triggerLogoUpload = (type) => {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/png,image/jpeg,image/svg+xml'
  input.onchange = async (e) => {
    const file = e.target.files[0]
    if (!file) return
    uploadingLogo.value = type
    try {
      const formData = new FormData()
      formData.append('file', file)
      await api.post(`/parametrage/logo/${type}`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      await parametrageStore.fetchParametres()
    } catch (err) {
      console.error('Erreur upload logo:', err)
    } finally {
      uploadingLogo.value = null
    }
  }
  input.click()
}

const isLogoParam = (cle) => cle.includes('LOGO_URL')
const isReadOnly = (cle) => cle === 'APP_VERSION'
</script>

<template>
  <MainLayout>
    <template #title>{{ t("parametres.titre") }}</template>
    <template #subtitle>{{ t("parametres.sousTitre") }}</template>

    <div class="settings-layout">
      
      <!-- SIDEBAR NAVIGATION -->
      <aside class="settings-sidebar">
        <div class="sidebar-info">
          <div class="sidebar-icon">⚙️</div>
          <div class="sidebar-text">
            <h3>{{ t("parametres.configuration") }}</h3>
            <p>{{ t("parametres.gerezReglages") }}</p>
          </div>
        </div>
        
        <nav class="settings-nav">
          <button 
            v-for="cat in categories" 
            :key="cat.id"
            @click="activeTab = cat.id"
            class="nav-item"
            :class="{ active: activeTab === cat.id }"
          >
            <span class="nav-icon"><component :is="cat.icon" class="w-5 h-5" /></span>
            <span class="nav-label">{{ cat.label }}</span>
            <div v-if="activeTab === cat.id" class="nav-active-indicator"></div>
          </button>
        </nav>
      </aside>

      <!-- MAIN CONTENT AREA -->
      <main class="settings-content">
        <div v-if="parametrageStore.error" class="error-banner">
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>
          <span>{{ parametrageStore.error }}</span>
        </div>

        <div class="content-header">
          <h2>{{ categories.find(c => c.id === activeTab)?.label }}</h2>
          <p>{{ categories.find(c => c.id === activeTab)?.desc }}</p>
        </div>

        <!-- ======= SECTION IDENTITÉ & LOGOS ======= -->
        <div v-if="activeTab === 'IDENTITE'" class="identity-section">
          <!-- Logo Management Cards -->
          <div class="logo-cards-row">
            <div class="logo-card">
              <div class="logo-card-header">
                <h4>{{ t("parametres.logoPrincipal") }}</h4>
                <span class="logo-hint">{{ t("parametres.logoAffiche") }} et l'en-tete</span>
              </div>
              <div class="logo-preview-area" @click="triggerLogoUpload('app')">
                <img v-if="getLogoUrl('app')" :src="getLogoUrl('app')" alt="Logo principal" class="logo-preview-img" />
                <div v-else class="logo-placeholder">
                  <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect><circle cx="8.5" cy="8.5" r="1.5"></circle><polyline points="21 15 16 10 5 21"></polyline></svg>
                  <span>{{ t("parametres.cliquerUploader") }}</span>
                </div>
                <div v-if="uploadingLogo === 'app'" class="logo-uploading">
                  <div class="spinner"></div>
                </div>
              </div>
              <button class="btn-upload" @click="triggerLogoUpload('app')" :disabled="uploadingLogo === 'app'">
                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="17 8 12 3 7 8"></polyline><line x1="12" y1="3" x2="12" y2="15"></line></svg>
                {{ uploadingLogo === 'app' ? t('parametres.envoi') : t('parametres.changerLogo') }}
              </button>
            </div>

            <div class="logo-card">
              <div class="logo-card-header">
                <h4>{{ t("parametres.logoFacture") }}</h4>
                <span class="logo-hint">{{ t("parametres.logoFactureDesc") }}</span>
              </div>
              <div class="logo-preview-area" @click="triggerLogoUpload('invoice')">
                <img v-if="getLogoUrl('invoice')" :src="getLogoUrl('invoice')" alt="Logo facture" class="logo-preview-img" />
                <div v-else class="logo-placeholder">
                  <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M14 2H6a2 2 0 0 0-2 2v16c0 1.1.9 2 2 2h12a2 2 0 0 0 2-2V8l-6-6z"/><path d="M14 3v5h5"/></svg>
                  <span>{{ t("parametres.cliquerUploader") }}</span>
                </div>
                <div v-if="uploadingLogo === 'invoice'" class="logo-uploading">
                  <div class="spinner"></div>
                </div>
              </div>
              <button class="btn-upload" @click="triggerLogoUpload('invoice')" :disabled="uploadingLogo === 'invoice'">
                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="17 8 12 3 7 8"></polyline><line x1="12" y1="3" x2="12" y2="15"></line></svg>
                {{ uploadingLogo === 'invoice' ? t('parametres.envoi') : t('parametres.changerLogo') }}
              </button>
            </div>
          </div>

          <!-- Society info params below logos -->
          <div class="section-divider">
            <span>{{ t("parametres.coordonnees") }}</span>
          </div>
        </div>

        <!-- ======= PARAMS GRID ======= -->
        <div class="params-grid">
          <div 
            v-for="param in filteredParametres" 
            :key="param.cle"
            class="param-card"
            :class="{ editing: editConfigMode === param.cle, readonly: isReadOnly(param.cle) }"
            v-show="!isLogoParam(param.cle)"
          >
            <div class="param-card-icon" :class="getParamIconInfo(param.cle).class">
              <component :is="getParamIconInfo(param.cle).component" class="w-5 h-5" />
            </div>
            
            <div class="param-card-body">
              <div class="param-header">
                <div class="param-title">
                  <span class="param-key">{{ param.cle.replace(/_/g, ' ') }}</span>
                  <span class="param-desc">{{ param.description || 'Parametre systeme interne.' }}</span>
                </div>
              </div>

              <div class="param-control">
                <template v-if="editConfigMode === param.cle">
                  <div class="edit-group">
                    <input 
                      v-model="editConfigValue" 
                      type="text" 
                      class="premium-input" 
                      :placeholder="t('parametres.nouvelleValeur')"
                      @keyup.enter="saveConfig(param)"
                      @keyup.esc="cancelEditConfig"
                    />
                    <div class="edit-actions">
                      <button @click="saveConfig(param)" class="btn-save" title="Enregistrer">
                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><polyline points="20 6 9 17 4 12"></polyline></svg>
                      </button>
                      <button @click="cancelEditConfig" class="btn-cancel" title="Annuler">
                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
                      </button>
                    </div>
                  </div>
                </template>
                <template v-else>
                  <div class="value-display">
                    <div class="current-value">{{ param.valeur }}</div>
                    <button 
                      v-if="isAdmin && !isReadOnly(param.cle)" 
                      @click="startEditConfig(param)" 
                      class="btn-edit-inline"
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path></svg>
                      Modifier
                    </button>
                    <span v-if="isReadOnly(param.cle)" class="readonly-badge">{{ t("parametres.lectureSeule") }}</span>
                  </div>
                </template>
              </div>
            </div>
          </div>

          <div v-if="!filteredParametres.filter(p => !isLogoParam(p.cle)).length && activeTab !== 'IDENTITE'" class="empty-params">
            <div class="empty-icon">📂</div>
            <p>{{ t("parametres.aucunParametre") }}</p>
          </div>
        </div>
      </main>
    </div>
  </MainLayout>
</template>

<style scoped>
.settings-layout {
  display: flex;
  gap: 2rem;
  align-items: flex-start;
  margin-top: 1rem;
}

/* SIDEBAR */
.settings-sidebar {
  width: 280px;
  background: white;
  border-radius: 16px;
  border: 1px solid #f1f5f9;
  padding: 1.5rem;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  position: sticky;
  top: 1.5rem;
  flex-shrink: 0;
}

.sidebar-info {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 1.5rem;
  margin-bottom: 1rem;
  border-bottom: 1px solid #f1f5f9;
}

.sidebar-icon {
  width: 42px;
  height: 42px;
  background: linear-gradient(135deg, #eff6ff, #dbeafe);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.15rem;
}

.sidebar-text h3 {
  margin: 0;
  font-size: 1rem;
  color: #1e293b;
  font-weight: 700;
}

.sidebar-text p {
  margin: 2px 0 0 0;
  font-size: 0.72rem;
  color: #94a3b8;
}

.settings-nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0.85rem 1rem;
  border: none;
  background: transparent;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.15s;
  position: relative;
  width: 100%;
  text-align: left;
}

.nav-icon { font-size: 1.05rem; }
.nav-label { font-size: 0.85rem; font-weight: 600; color: #64748b; }

.nav-item:hover {
  background: #f8fafc;
}

.nav-item.active {
  background: #eff6ff;
}

.nav-item.active .nav-label { color: #2563eb; font-weight: 700; }

.nav-active-indicator {
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 18px;
  background: #3b82f6;
  border-radius: 0 3px 3px 0;
}

/* CONTENT AREA */
.settings-content {
  flex: 1;
  min-width: 0;
}

.content-header {
  margin-bottom: 1.5rem;
}

.content-header h2 {
  margin: 0 0 0.35rem 0;
  font-size: 1.35rem;
  color: #0f172a;
  font-weight: 800;
}

.content-header p {
  margin: 0;
  color: #64748b;
  font-size: 0.9rem;
}

/* ======= IDENTITY SECTION ======= */
.identity-section {
  margin-bottom: 1.5rem;
}

.logo-cards-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.25rem;
  margin-bottom: 1.5rem;
}

.logo-card {
  background: white;
  border-radius: 16px;
  border: 1px solid #f1f5f9;
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  transition: all 0.2s;
}

.logo-card:hover {
  border-color: #e2e8f0;
  box-shadow: 0 4px 6px -1px rgba(0,0,0,0.04);
}

.logo-card-header h4 {
  margin: 0;
  font-size: 1rem;
  font-weight: 700;
  color: #1e293b;
}

.logo-hint {
  font-size: 0.78rem;
  color: #94a3b8;
}

.logo-preview-area {
  background: #f8fafc;
  border: 2px dashed #e2e8f0;
  border-radius: 12px;
  height: 140px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
  overflow: hidden;
}

.logo-preview-area:hover {
  border-color: #3b82f6;
  background: #f0f7ff;
}

.logo-preview-img {
  max-height: 120px;
  max-width: 90%;
  object-fit: contain;
}

.logo-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #94a3b8;
}

.logo-placeholder span {
  font-size: 0.8rem;
  font-weight: 600;
}

.logo-uploading {
  position: absolute;
  inset: 0;
  background: rgba(255,255,255,0.8);
  display: flex;
  align-items: center;
  justify-content: center;
}

.spinner {
  width: 28px;
  height: 28px;
  border: 3px solid #e2e8f0;
  border-top-color: #3b82f6;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.btn-upload {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 0.6rem 1rem;
  font-size: 0.85rem;
  font-weight: 600;
  color: #475569;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-upload:hover {
  background: #eff6ff;
  border-color: #3b82f6;
  color: #3b82f6;
}

.btn-upload:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.section-divider {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin: 0.5rem 0 0 0;
}

.section-divider span {
  font-size: 0.75rem;
  font-weight: 700;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  white-space: nowrap;
}

.section-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: #f1f5f9;
}

/* ======= PARAMS GRID ======= */
.params-grid {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.param-card {
  background: white;
  border-radius: 14px;
  border: 1px solid #f1f5f9;
  padding: 1.25rem;
  display: flex;
  gap: 1.25rem;
  transition: all 0.2s;
  box-shadow: 0 1px 2px rgba(0,0,0,0.02);
}

.param-card:hover {
  border-color: #e2e8f0;
  box-shadow: 0 4px 6px -1px rgba(0,0,0,0.04);
}

.param-card.editing {
  border-color: #3b82f6;
  background: #fafbff;
}

.param-card.readonly {
  opacity: 0.7;
}

.param-card-icon {
  width: 44px;
  height: 44px;
  background: #f1f5f9;
  color: #64748b;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.param-card-icon.icon-dollar { background: #fff7ed; color: #f97316; }
.param-card-icon.icon-currency { background: #fff7ed; color: #f97316; }
.param-card-icon.icon-lock { background: #eef2ff; color: #6366f1; }
.param-card-icon.icon-clock { background: #eef2ff; color: #6366f1; }
.param-card-icon.icon-building { background: #ecfdf5; color: #10b981; }
.param-card-icon.icon-map { background: #ecfdf5; color: #10b981; }
.param-card-icon.icon-phone { background: #ecfdf5; color: #10b981; }
.param-card-icon.icon-mail { background: #ecfdf5; color: #10b981; }
.param-card-icon.icon-image { background: #eff6ff; color: #3b82f6; }
.param-card-icon.icon-bell { background: #fefce8; color: #eab308; }
.param-card-icon.icon-tag { background: #f1f5f9; color: #475569; }

.param-card-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  min-width: 0;
}

.param-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.param-title {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.param-key {
  font-size: 0.9rem;
  font-weight: 700;
  color: #1e293b;
  text-transform: capitalize;
}

.param-desc {
  font-size: 0.8rem;
  color: #94a3b8;
  line-height: 1.4;
}

.param-control {
  margin-top: auto;
}

.value-display {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.current-value {
  font-size: 0.95rem;
  font-weight: 700;
  color: #0f172a;
  background: #f8fafc;
  padding: 0.4rem 0.85rem;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.readonly-badge {
  font-size: 0.7rem;
  font-weight: 600;
  color: #94a3b8;
  background: #f8fafc;
  padding: 3px 8px;
  border-radius: 4px;
  border: 1px solid #e2e8f0;
}

.btn-edit-inline {
  display: flex;
  align-items: center;
  gap: 5px;
  border: none;
  background: transparent;
  color: #3b82f6;
  font-size: 0.82rem;
  font-weight: 700;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: all 0.15s;
}

.btn-edit-inline:hover {
  background: #eff6ff;
}

/* EDIT MODE */
.edit-group {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.premium-input {
  flex: 1;
  background: white;
  border: 2px solid #3b82f6;
  border-radius: 8px;
  padding: 0.5rem 0.85rem;
  font-size: 0.9rem;
  font-weight: 600;
  color: #1e293b;
  outline: none;
}

.edit-actions {
  display: flex;
  gap: 4px;
}

.btn-save, .btn-cancel {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-save { background: #3b82f6; color: white; }
.btn-save:hover { background: #2563eb; }

.btn-cancel { background: #fee2e2; color: #ef4444; }
.btn-cancel:hover { background: #fecaca; }

.empty-params {
  text-align: center;
  padding: 3rem 2rem;
  background: white;
  border-radius: 16px;
  border: 1px dashed #e2e8f0;
}

.empty-icon { font-size: 2.5rem; margin-bottom: 0.75rem; }
.empty-params p { color: #94a3b8; font-weight: 500; font-size: 0.9rem; }

.error-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #fef2f2;
  border: 1px solid #fee2e2;
  color: #dc2626;
  padding: 0.85rem 1rem;
  border-radius: 10px;
  font-size: 0.85rem;
  font-weight: 600;
  margin-bottom: 1.25rem;
}

/* RESPONSIVE */
@media (max-width: 1024px) {
  .settings-layout { flex-direction: column; }
  .settings-sidebar { width: 100%; position: static; }
  .settings-nav { flex-direction: row; flex-wrap: wrap; }
  .nav-item { width: auto; flex: 1; min-width: 160px; justify-content: center; }
  .nav-active-indicator { display: none; }
}

@media (max-width: 768px) {
  .logo-cards-row { grid-template-columns: 1fr; }
}

@media (max-width: 640px) {
  .param-card { flex-direction: column; align-items: flex-start; gap: 0.75rem; }
  .edit-group { flex-direction: column; align-items: stretch; width: 100%; }
  .edit-actions { justify-content: flex-end; }
  .settings-nav { flex-direction: column; }
  .nav-item { min-width: 0; }
}
</style>
