import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUiStore = defineStore('ui', () => {
  const isLoading = ref(false)

  const setLoading = (value) => {
    isLoading.value = value
  }

  return {
    isLoading,
    setLoading
  }
})
