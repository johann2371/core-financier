import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api', // En production, utiliser l'URL du backend (VITE_API_URL)
  headers: {
    'Content-Type': 'application/json',
  },
});

// Intercepteur pour ajouter le token JWT à chaque requête
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Intercepteur pour gérer les erreurs 401 (Token expiré)
api.interceptors.response.use(
  (response) => response,
  (error) => {
    // On ne redirige pas si l'erreur 401 vient de la tentative de connexion elle-même
    const isLoginRequest = error.config && error.config.url && error.config.url.includes('/auth/login');
    // On ne redirige pas si c'est une requête blob (PDF) — le catch local gère l'erreur
    const isBlobRequest = error.config && error.config.responseType === 'blob';
    
    if (error.response && error.response.status === 401 && !isLoginRequest && !isBlobRequest) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default api;
