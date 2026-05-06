import { defineStore } from 'pinia'
import api from '../services/api'

export const useDashboardStore = defineStore('dashboard', {
  state: () => ({
    kpis: null,
    loading: false,
    error: null
  }),

  actions: {
    async fetchKpis(forecastDays = 30) {
      this.loading = true
      this.error = null
      try {
        const response = await api.get(`/tableau-bord/kpis?forecastDays=${forecastDays}`)
        this.kpis = response.data
      } catch (err) {
        this.error = "Erreur lors du chargement des indicateurs."
        console.error(err)
      } finally {
        this.loading = false
      }
    }
  }
})
