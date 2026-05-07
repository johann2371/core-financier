import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUiStore = defineStore('ui', () => {
  const isLoading = ref(false)
  const isDarkMode = ref(localStorage.getItem('darkMode') === 'true')

  const setLoading = (value) => {
    isLoading.value = value
  }

  const toggleDarkMode = () => {
    isDarkMode.value = !isDarkMode.value
    localStorage.setItem('darkMode', isDarkMode.value)
    document.body.classList.toggle('dark-mode', isDarkMode.value)
  }

  // Initialisation
  if (isDarkMode.value) {
    document.body.classList.add('dark-mode')
  }

  return {
    isLoading,
    isDarkMode,
    setLoading,
    toggleDarkMode
  }
})
