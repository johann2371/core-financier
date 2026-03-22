<script setup>
import { computed } from 'vue'

const props = defineProps({
  currentPage: { type: Number, required: true },
  totalItems: { type: Number, required: true },
  itemsPerPage: { type: Number, default: 10 }
})

const emit = defineEmits(['update:currentPage'])

const totalPages = computed(() => Math.ceil(props.totalItems / props.itemsPerPage) || 1)

const prevPage = () => {
  if (props.currentPage > 1) emit('update:currentPage', props.currentPage - 1)
}

const nextPage = () => {
  if (props.currentPage < totalPages.value) emit('update:currentPage', props.currentPage + 1)
}
</script>

<template>
  <div class="pagination-container" v-if="totalItems > itemsPerPage">
    <div class="pagination-info">
      Affichage de <strong>{{ (currentPage - 1) * itemsPerPage + 1 }}</strong> à <strong>{{ Math.min(currentPage * itemsPerPage, totalItems) }}</strong> sur <strong>{{ totalItems }}</strong> résultats
    </div>
    <div class="pagination-actions">
      <button class="btn-page" @click="prevPage" :disabled="currentPage === 1">
        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"></polyline></svg>
        Précédent
      </button>
      <span class="page-indicator">Page {{ currentPage }} sur {{ totalPages }}</span>
      <button class="btn-page" @click="nextPage" :disabled="currentPage === totalPages">
        Suivant
        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"></polyline></svg>
      </button>
    </div>
  </div>
</template>

<style scoped>
.pagination-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.5rem;
  border-top: 1px solid #f3f4f6;
  background: white;
}
.pagination-info {
  font-size: 0.85rem;
  color: #6b7280;
}
.pagination-info strong {
  color: #111827;
}
.pagination-actions {
  display: flex;
  align-items: center;
  gap: 1.25rem;
}
.btn-page {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.4rem 0.875rem;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background: white;
  color: #374151;
  font-size: 0.85rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
}
.btn-page:hover:not(:disabled) {
  background: #f9fafb;
  color: #111827;
  border-color: #9ca3af;
}
.btn-page:active:not(:disabled) {
  transform: scale(0.98);
}
.btn-page:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  background: #f9fafb;
}
.page-indicator {
  font-size: 0.85rem;
  font-weight: 600;
  color: #111827;
}
</style>
