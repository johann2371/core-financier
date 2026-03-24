import { defineStore } from 'pinia'
import api from '../services/api'

export const useDecaissementStore = defineStore('decaissement', {
  state: () => ({
    decaissements: [],
    loading: false,
    error: null,
    currentDecaissement: null
  }),
  
  actions: {
    async fetchDecaissements() {
      this.loading = true
      this.error = null
      try {
        const response = await api.get('/decaissements')
        this.decaissements = response.data.sort((a, b) => b.id - a.id)
      } catch (err) {
        this.error = err.response?.data?.error || 'Erreur lors du chargement des décaissements'
      } finally {
        this.loading = false
      }
    },

    async createDecaissement(data) {
      this.loading = true
      this.error = null
      try {
        const response = await api.post('/decaissements', data)
        this.decaissements.unshift(response.data)
        return response.data
      } catch (err) {
        this.error = err.response?.data?.error || 'Erreur lors de la création du décaissement'
        throw err
      } finally {
        this.loading = false
      }
    },

    async soumettre(id) {
      this.loading = true
      try {
        const response = await api.put(`/decaissements/${id}/soumettre`)
        this._updateList(response.data)
        return response.data
      } finally {
        this.loading = false
      }
    },

    async validerRF(id, data) {
      this.loading = true
      try {
        const response = await api.put(`/decaissements/${id}/valider-rf`, data)
        this._updateList(response.data)
        return response.data
      } finally {
        this.loading = false
      }
    },

    async approuverPDG(id, data) {
      this.loading = true
      try {
        const response = await api.put(`/decaissements/${id}/approuver-pdg`, data)
        this._updateList(response.data)
        return response.data
      } finally {
        this.loading = false
      }
    },

    async executer(id, data) {
      this.loading = true
      try {
        const response = await api.put(`/decaissements/${id}/executer`, data)
        this._updateList(response.data)
        return response.data
      } finally {
        this.loading = false
      }
    },

    async updateStatut(id, nouveauStatut, commentaire) {
      // Pour compatibilité ou actions génériques si besoin
      this.loading = true
      this.error = null
      try {
        const response = await api.put(`/decaissements/${id}/statut`, null, {
          params: { statut: nouveauStatut, commentaire: commentaire }
        })
        this._updateList(response.data)
        return response.data
      } catch (err) {
        this.error = err.response?.data?.error || 'Erreur lors de la mise à jour du statut'
        throw err
      } finally {
        this.loading = false
      }
    },

    _updateList(item) {
      const index = this.decaissements.findIndex(d => d.id === item.id)
      if (index !== -1) {
        this.decaissements[index] = item
      }
    },

    async downloadReceipt(id) {
      try {
        const response = await api.get(`/decaissements/${id}/recu/pdf`, { responseType: 'blob' })
        const url = window.URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }))
        const link = document.createElement('a')
        link.href = url
        link.setAttribute('download', `Bon_Decaissement_${id}.pdf`)
        document.body.appendChild(link)
        link.click()
        link.remove()
      } catch (err) {
        console.error('Erreur téléchargement PDF: ', err)
      }
    }
  }
})
