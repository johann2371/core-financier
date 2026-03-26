import { defineStore } from 'pinia'
import api from '../services/api'

export const useCompteStore = defineStore('compte', {
  state: () => ({
    comptes: [],
    loading: false,
    error: null
  }),

  getters: {
    caisses: (state) => state.comptes.filter(c => c.type === 'CAISSE' && c.actif),
    banques: (state) => state.comptes.filter(c => c.type === 'BANQUE' && c.actif),
    totalSolde: (state) => state.comptes.reduce((sum, c) => sum + (c.actif ? c.solde : 0), 0)
  },

  actions: {
    async fetchComptes() {
      this.loading = true
      this.error = null
      try {
        const response = await api.get('/comptes-financiers')
        this.comptes = response.data
      } catch (err) {
        this.error = 'Erreur lors du chargement des comptes financiers'
        console.error(err)
      } finally {
        this.loading = false
      }
    }
  }
})
