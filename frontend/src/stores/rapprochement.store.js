import { defineStore } from 'pinia'
import api from '../services/api'

export const useRapprochementStore = defineStore('rapprochement', {
  state: () => ({
    sessions: [],
    currentSession: null,
    lignesReleve: [],
    transactionsSysteme: [], // Encaissements & Décaissements non rapprochés
    loading: false,
    error: null
  }),

  actions: {
    async fetchSessions(compteId) {
      this.loading = true
      try {
        const res = await api.get(`/api/rapprochements/compte/${compteId}`)
        this.sessions = res.data
      } catch (err) {
        this.error = "Erreur lors de la récupération des sessions"
      } finally {
        this.loading = false
      }
    },

    async creerSession(payload) {
      this.loading = true
      try {
        const res = await api.post('/api/rapprochements/sessions', null, { params: payload })
        this.currentSession = res.data
        return res.data
      } catch (err) {
        this.error = "Erreur lors de la création de la session"
        throw err
      } finally {
        this.loading = false
      }
    },

    async importerReleve(sessionId, file) {
      this.loading = true
      const formData = new FormData()
      formData.append('file', file)
      try {
        const res = await api.post(`/api/rapprochements/sessions/${sessionId}/importer`, formData)
        this.lignesReleve = res.data
      } catch (err) {
        this.error = "Erreur lors de l'importation du relevé"
      } finally {
        this.loading = false
      }
    },

    async autoMatch(sessionId) {
      this.loading = true
      try {
        await api.post(`/api/rapprochements/sessions/${sessionId}/auto-match`)
        // Recharger les lignes pour voir les changements
        const res = await api.get(`/api/rapprochements/sessions/${sessionId}/lignes`) // J'ai oublié cet endpoint dans le controller, je vais le rajouter
        this.lignesReleve = res.data
      } catch (err) {
        this.error = "Erreur lors de l'auto-match"
      } finally {
        this.loading = false
      }
    },

    async matchManuel(ligneId, transactionId, typeTransaction) {
      try {
        await api.post('/api/rapprochements/match-manuel', null, { 
          params: { ligneId, transactionId, typeTransaction } 
        })
        // Mettre à jour l'état local
        const index = this.lignesReleve.findIndex(l => l.id === ligneId)
        if (index !== -1) this.lignesReleve[index].matched = true
      } catch (err) {
        this.error = "Erreur lors du rapprochement manuel"
      }
    },

    async validerSession(sessionId, soldeFinal) {
      try {
        await api.post(`/api/rapprochements/sessions/${sessionId}/valider`, null, { 
          params: { soldeFinal } 
        })
        if (this.currentSession) this.currentSession.valide = true
      } catch (err) {
        this.error = "Erreur lors de la validation"
      }
    }
  }
})
