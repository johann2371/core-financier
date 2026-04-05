import { defineStore } from 'pinia';
import api from '../services/api';

export const useUtilisateurStore = defineStore('utilisateur', {
  state: () => ({
    utilisateurs: [],
    loading: false,
    error: null
  }),
  actions: {
    async fetchUtilisateurs() {
      this.loading = true;
      this.error = null;
      try {
        const { data } = await api.get('/utilisateurs');
        this.utilisateurs = data;
      } catch (err) {
        this.error = err.response?.data?.message || "Erreur lors du chargement des utilisateurs";
        console.error("Erreur fetchUtilisateurs:", err);
      } finally {
        this.loading = false;
      }
    },
    async creerUtilisateur(payload) {
      this.error = null;
      try {
         await api.post('/utilisateurs', payload);
         await this.fetchUtilisateurs();
      } catch (err) { 
         this.error = err.response?.data?.message || "Erreur lors de la création";
         throw err; 
      }
    },
    async mettreAJour(id, payload) {
      this.error = null;
      try {
         await api.put(`/utilisateurs/${id}`, payload);
         await this.fetchUtilisateurs();
      } catch (err) { 
         this.error = err.response?.data?.message || "Erreur lors de la mise à jour";
         throw err; 
      }
    },
    async bloquerOuDebloquer(id) {
       this.error = null;
       try {
          await api.delete(`/utilisateurs/${id}`);
          await this.fetchUtilisateurs();
       } catch (err) {
          this.error = err.response?.data?.message || "Erreur lors de la désactivation";
          throw err;
       }
    },
    async reactiverUtilisateur(id) {
       this.error = null;
       try {
          await api.put(`/utilisateurs/${id}/reactiver`);
          await this.fetchUtilisateurs();
       } catch (err) {
          this.error = err.response?.data?.message || "Erreur lors de la réactivation";
          throw err;
       }
    }
  }
});
