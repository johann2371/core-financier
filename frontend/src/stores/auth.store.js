import { defineStore } from 'pinia';
import api from '../services/api';
import router from '../router';

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: JSON.parse(localStorage.getItem('user')) || null,
    token: localStorage.getItem('token') || null,
    error: null,
    loading: false
  }),

  getters: {
    isAuthenticated: (state) => !!state.token,
    userRole: (state) => state.user ? state.user.role : null,
  },

  actions: {
    async login(email, password) {
      this.loading = true;
      this.error = null;
      try {
        const response = await api.post('/auth/login', { email, password });
        
        this.token = response.data.token;
        this.user = {
          nom: response.data.nom,
          prenom: response.data.prenom,
          email: response.data.email,
          role: response.data.role,
          photoUrl: response.data.photoUrl
        };
        
        localStorage.setItem('token', this.token);
        localStorage.setItem('refreshToken', response.data.refreshToken);
        localStorage.setItem('user', JSON.stringify(this.user));
        
        router.push('/');
      } catch (error) {
        this.error = error.response?.data?.error || 'Erreur de connexion';
      } finally {
        this.loading = false;
      }
    },

    async updateProfile(profileData) {
      this.loading = true;
      try {
        const response = await api.put('/utilisateurs/profile', profileData);
        this.user = {
          ...this.user,
          nom: response.data.nom,
          prenom: response.data.prenom,
          email: response.data.email,
          photoUrl: response.data.photoUrl
        };
        localStorage.setItem('user', JSON.stringify(this.user));
        return response.data;
      } catch (error) {
        this.error = error.response?.data?.error || 'Erreur de mise à jour';
        throw error;
      } finally {
        this.loading = false;
      }
    },

    async uploadProfilePhoto(file) {
      this.loading = true;
      try {
        const formData = new FormData();
        formData.append('file', file);
        
        const response = await api.post('/utilisateurs/profile/photo', formData, {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
        });
        
        this.user = {
          ...this.user,
          photoUrl: response.data.photoUrl
        };
        localStorage.setItem('user', JSON.stringify(this.user));
        return response.data;
      } catch (error) {
        this.error = error.response?.data?.error || "Erreur lors de l'envoi de la photo";
        throw error;
      } finally {
        this.loading = false;
      }
    },

    logout() {
      this.user = null;
      this.token = null;
      localStorage.removeItem('token');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
      router.push('/login');
    }
  }
});
