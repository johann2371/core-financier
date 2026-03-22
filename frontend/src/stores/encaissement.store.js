import { defineStore } from 'pinia'
import api from '../services/api'

export const useEncaissementStore = defineStore('encaissement', {
  state: () => ({
    encaissements: [],
    loading: false,
    error: null
  }),
  
  actions: {
    async fetchEncaissements() {
      this.loading = true
      this.error = null
      try {
        const response = await api.get('/encaissements')
        this.encaissements = response.data
      } catch (err) {
        this.error = err.response?.data?.error || 'Erreur lors du chargement des encaissements'
      } finally {
        this.loading = false
      }
    },

    async createEncaissement(data) {
      this.loading = true
      this.error = null
      try {
        const response = await api.post('/encaissements', data)
        this.encaissements.push(response.data)
        return response.data
      } catch (err) {
        this.error = err.response?.data?.error || 'Erreur lors de la création de l\'encaissement'
        throw err
      } finally {
        this.loading = false
      }
    },

    async downloadReceipt(id) {
      try {
        const response = await api.get(`/encaissements/${id}/recu/pdf`, { responseType: 'blob' })
        const url = window.URL.createObjectURL(new Blob([response.data]))
        const link = document.createElement('a')
        link.href = url
        link.setAttribute('download', `Recu_Encaissement_${id}.pdf`)
        document.body.appendChild(link)
        link.click()
        link.remove()
      } catch (err) {
        console.error('Erreur téléchargement PDF: ', err)
      }
    }
  }
})
