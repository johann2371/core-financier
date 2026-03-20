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
        this.decaissements = response.data
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
        this.decaissements.push(response.data)
        return response.data
      } catch (err) {
        this.error = err.response?.data?.error || 'Erreur lors de la création du décaissement'
        throw err
      } finally {
        this.loading = false
      }
    },

    async updateStatut(id, nouveauStatut, commentaire) {
      this.loading = true
      this.error = null
      try {
        const response = await api.put(`/decaissements/${id}/statut`, null, {
          params: { statut: nouveauStatut, commentaire: commentaire }
        })
        const index = this.decaissements.findIndex(d => d.id === id)
        if (index !== -1) {
          this.decaissements[index] = response.data
        }
        return response.data
      } catch (err) {
        this.error = err.response?.data?.error || 'Erreur lors de la mise à jour du statut'
        throw err
      } finally {
        this.loading = false
      }
    }
  }
})
