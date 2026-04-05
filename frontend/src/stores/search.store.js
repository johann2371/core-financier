import { defineStore } from 'pinia'
import api from '../services/api'

export const useSearchStore = defineStore('search', {
  state: () => ({
    query: '',
    results: [],
    loading: false,
    error: null,
    showResults: false
  }),

  actions: {
    async performSearch(q) {
      if (!q || q.trim().length < 2) {
        this.results = []
        this.showResults = false
        return
      }

      this.query = q
      this.loading = true
      this.error = null
      this.showResults = true

      try {
        const response = await api.get(`/search?q=${encodeURIComponent(q)}`)
        this.results = response.data
      } catch (err) {
        console.error("Global search error:", err)
        this.error = "Erreur lors de la recherche"
        this.results = []
      } finally {
        this.loading = false
      }
    },

    clearSearch() {
      this.query = ''
      this.results = []
      this.showResults = false
    },

    closeResults() {
      this.showResults = false
    }
  }
})
