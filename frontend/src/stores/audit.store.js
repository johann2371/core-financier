import { defineStore } from 'pinia'
import api from '../services/api'

export const useAuditStore = defineStore('audit', {
  state: () => ({
    logs: [],
    totalElements: 0,
    totalPages: 0,
    currentPage: 0,
    pageSize: 15,
    loading: false,
    error: null
  }),

  actions: {
    async fetchLogs(page = 0, size = 15, search = '', action = '') {
      this.loading = true
      this.error = null
      try {
        const response = await api.get('/journal-audit', {
          params: {
            page: page,
            size: size,
            sort: 'dateAction,desc',
            search: search,
            action: action
          }
        })
        this.logs = response.data.content
        this.totalElements = response.data.totalElements
        this.totalPages = response.data.totalPages
        this.currentPage = response.data.number
        this.pageSize = response.data.size
      } catch (err) {
        console.error('Audit Fetch Error:', err)
        this.error = err.response?.data?.message || 'Erreur lors du chargement du journal d\'audit'
      } finally {
        this.loading = false
      }
    }
  }
})
