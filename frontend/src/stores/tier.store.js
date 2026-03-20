import { defineStore } from 'pinia'
import api from '../services/api'

export const useTierStore = defineStore('tier', {
  state: () => ({
    tiers: [],
    loading: false,
    error: null
  }),
  
  getters: {
    clients: (state) => state.tiers.filter(t => t.type === 'CLIENT'),
    fournisseurs: (state) => state.tiers.filter(t => t.type === 'FOURNISSEUR')
  },
  
  actions: {
    async fetchTiers() {
      this.loading = true
      this.error = null
      try {
        const response = await api.get('/tiers')
        this.tiers = response.data
      } catch (err) {
        this.error = err.response?.data?.error || 'Erreur lors du chargement des tiers'
      } finally {
        this.loading = false
      }
    },

    async createTier(data) {
      this.loading = true
      this.error = null
      try {
        const response = await api.post('/tiers', data)
        this.tiers.push(response.data)
        return response.data
      } catch (err) {
        this.error = err.response?.data?.error || 'Erreur lors de la création du tiers'
        throw err
      } finally {
        this.loading = false
      }
    }
  }
})
