import { defineStore } from 'pinia'
import api from '../services/api'

export const useFactureStore = defineStore('facture', {
  state: () => ({
    factures: [],
    loading: false,
    error: null
  }),
  
  getters: {
    ventes: (state) => state.factures.filter(f => f.type === 'VENTE'),
    achats: (state) => state.factures.filter(f => f.type === 'ACHAT'),
    enAttente: (state) => state.factures.filter(f => f.statut === 'EN_ATTENTE_PAIEMENT')
  },
  
  actions: {
    async fetchFactures() {
      this.loading = true
      this.error = null
      try {
        const response = await api.get('/factures')
        this.factures = response.data
      } catch (err) {
        this.error = err.response?.data?.error || 'Erreur lors du chargement des factures'
      } finally {
        this.loading = false
      }
    },

    async createFacture(factureData) {
      this.loading = true
      this.error = null
      try {
        const response = await api.post('/factures', factureData)
        this.factures.unshift(response.data) // Ajouter au début pour voir la plus récente
        return response.data
      } catch (err) {
        this.error = err.response?.data?.error || 'Erreur lors de la création de la facture'
        throw err
      } finally {
        this.loading = false
      }
    },

    async downloadPdf(id) {
      try {
        // Optionnel: On peut utiliser window.open si c'est géré en REST GET simple
        const response = await api.get(`/factures/${id}/pdf`, { responseType: 'blob' })
        const url = window.URL.createObjectURL(new Blob([response.data]))
        const link = document.createElement('a')
        link.href = url
        link.setAttribute('download', `Facture_${id}.pdf`)
        document.body.appendChild(link)
        link.click()
        link.remove()
      } catch (err) {
        console.error('Erreur téléchargement PDF: ', err)
      }
    }
  }
})
