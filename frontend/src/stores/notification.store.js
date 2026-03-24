import { defineStore } from 'pinia'
import api from '../services/api'

export const useNotificationStore = defineStore('notification', {
  state: () => ({
    unreadNotifications: [],
    loading: false
  }),

  actions: {
    async fetchUnread(role) {
      if (!role) return
      this.loading = true
      try {
        const response = await api.get(`/notifications/role/${role}/non-lues`)
        this.unreadNotifications = response.data
      } catch (err) {
        console.error('Erreur notifications:', err)
      } finally {
        this.loading = false
      }
    },

    async markAsRead(id) {
      try {
        await api.put(`/notifications/${id}/lue`)
        this.unreadNotifications = this.unreadNotifications.filter(n => n.id !== id)
      } catch (err) {
        console.error('Erreur marquage notification:', err)
      }
    }
  }
})
