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

const goToPage = (page) => {
  if (page !== props.currentPage && page >= 1 && page <= totalPages.value) {
    emit('update:currentPage', page)
  }
}

// Calcule les pages à afficher (pour 8 pages on peut tout afficher, sinon on coupe, mais restons simple)
const visiblePages = computed(() => {
  const pages = []
  for (let i = 1; i <= totalPages.value; i++) {
    pages.push(i)
  }
  return pages
})
</script>

<template>
  <div class="pagination-container" v-if="totalItems > itemsPerPage">
    <div class="pagination-actions">
      <!-- MOCKUP STYLE NUMERIC PAGINATION -->
      <button class="page-btn nav-btn" @click="prevPage" :disabled="currentPage === 1">
        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"></polyline></svg>
      </button>
      
      <button 
        v-for="page in visiblePages" 
        :key="page"
        class="page-btn"
        :class="{ active: page === currentPage }"
        @click="goToPage(page)"
      >
        {{ page }}
      </button>

      <button class="page-btn nav-btn" @click="nextPage" :disabled="currentPage === totalPages">
        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"></polyline></svg>
      </button>
    </div>
  </div>
</template>

<style scoped>
.pagination-container {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 1.25rem 0;
  margin-top: 0.5rem;
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
  gap: 0.5rem;
}

.page-btn {
  width: 38px;
  height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #e5e7eb;
  background: white;
  border-radius: 8px; /* Pavés carrés arrondis comme la maquette */
  font-size: 0.875rem;
  font-weight: 600;
  color: #4b5563;
  cursor: pointer;
  transition: all 0.15s;
}

.page-btn:hover:not(:disabled):not(.active) {
  background: #f9fafb;
  border-color: #d1d5db;
  color: #111827;
}

.page-btn:active:not(:disabled):not(.active) {
  transform: scale(0.96);
}

.page-btn.active {
  background: #3b82f6; /* Bleu style iOS / Tailwind Blue 500 */
  border-color: #3b82f6;
  color: white;
  box-shadow: 0 4px 6px -1px rgba(59, 130, 246, 0.2), 0 2px 4px -1px rgba(59, 130, 246, 0.1);
}

.nav-btn {
  color: #6b7280;
}

.nav-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
  background: #f9fafb;
}

.nav-btn:hover:not(:disabled) {
  color: #111827;
}
</style>
