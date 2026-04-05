import { defineStore } from 'pinia';
import api from '../services/api';

export const useParametrageStore = defineStore('parametrage', {
  state: () => ({
    parametres: [],
    loading: false,
    error: null
  }),
  actions: {
    async fetchParametres() {
      this.loading = true;
      this.error = null;
      try {
        const { data } = await api.get('/parametrage');
        this.parametres = data;
      } catch (err) {
        this.error = "Erreur lors du chargement des paramètres système";
        console.error("Erreur fetchParametres:", err);
      } finally {
        this.loading = false;
      }
    },
    async updateParametre(cle, payload) {
      try {
         await api.put(`/parametrage/${cle}`, payload);
         await this.fetchParametres();
      } catch (err) { 
         this.error = err.response?.data?.message || "Échec de la mise à jour";
         throw err; 
      }
    }
  }
});
