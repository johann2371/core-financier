<script setup>
import { ref, onMounted, computed } from 'vue'
import MainLayout from '../components/MainLayout.vue'
import { useParametrageStore } from '../stores/parametrage.store'
import { useAuthStore } from '../stores/auth.store'

const authStore = useAuthStore()
const parametrageStore = useParametrageStore()

const isPDG = computed(() => authStore.userRole === 'PDG')
const isAdmin = computed(() => authStore.userRole === 'ADMINISTRATEUR')

onMounted(() => {
  parametrageStore.fetchParametres()
})

// === GESTION CONFIGURATION ===
const editConfigMode = ref(null)
const editConfigValue = ref('')

const startEditConfig = (param) => {
  if (isPDG.value && param.cle !== 'SEUIL_APPROBATION_PDG') return;
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
</script>

<template>
  <MainLayout>
    <template #title>Paramètres</template>
    <template #subtitle>Configuration globale du système.</template>

    <div class="settings-container">

      <div v-if="parametrageStore.error" class="error-banner mb-4">{{ parametrageStore.error }}</div>

      <div class="feature-card config-list">
        <div class="config-header">Variables Environnement & Métier</div>
        
        <div v-for="param in parametrageStore.parametres" :key="param.cle" class="config-item">
          <div class="config-info">
            <strong class="config-key">{{ param.cle }}</strong>
            <span class="config-desc">{{ param.description || 'Paramètre système interne.' }}</span>
          </div>
          
          <div class="config-action">
            <template v-if="editConfigMode === param.cle">
              <input v-model="editConfigValue" type="text" class="config-input" />
              <button @click="saveConfig(param)" class="btn-primary sm-btn">Sauver</button>
              <button @click="cancelEditConfig" class="btn-outline sm-btn">Annuler</button>
            </template>
            <template v-else>
              <span class="config-val">{{ param.valeur }}</span>
              <button 
                v-if="isAdmin || (isPDG && param.cle === 'SEUIL_APPROBATION_PDG')" 
                @click="startEditConfig(param)" 
                class="btn-outline sm-btn"
              >Modifier</button>
            </template>
          </div>
        </div>
      </div>

    </div>
  </MainLayout>
</template>

<style scoped>
.settings-container {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.feature-card {
  background: white;
  border-radius: 12px;
  border: 1px solid #f1f5f9;
  box-shadow: 0 1px 3px rgba(0,0,0,0.02);
  overflow: hidden;
}

/* Config Style */
.config-header { padding: 1.25rem 1.5rem; background: #f8fafc; font-weight: 700; color: #334155; border-bottom: 1px solid #e2e8f0; }
.config-item { display: flex; justify-content: space-between; align-items: center; padding: 1.25rem 1.5rem; border-bottom: 1px solid #f1f5f9; }
.config-item:last-child { border-bottom: none; }
.config-info { display: flex; flex-direction: column; gap: 0.25rem; }
.config-key { font-family: ui-monospace, SFMono-Regular, monospace; color: #0f172a; font-size: 0.95rem; }
.config-desc { color: #64748b; font-size: 0.85rem; }
.config-action { display: flex; align-items: center; gap: 1rem; }
.config-val { font-weight: 700; color: #2563eb; padding: 0.25rem 0.5rem; background: #eff6ff; border-radius: 6px; }
.config-input { border: 1px solid #cbd5e1; padding: 6px 12px; border-radius: 6px; outline: none; }
.config-input:focus { border-color: #3b82f6; }
.sm-btn { padding: 6px 12px; font-size: 0.85rem; }
</style>
