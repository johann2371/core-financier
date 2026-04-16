import api from './api';

export const sessionCaisseService = {
  ouvrir(data) {
    return api.post('/sessions-caisse/ouvrir', data);
  },
  fermer(id, data) {
    return api.post(`/sessions-caisse/${id}/fermer`, data);
  },
  getActive() {
    return api.get('/sessions-caisse/active');
  },
  getHistorique(caissierId) {
    return api.get(`/sessions-caisse/historique/${caissierId}`);
  },
  getById(id) {
    return api.get(`/sessions-caisse/${id}`);
  },
  getMonHistorique() {
    return api.get('/sessions-caisse/mon-historique');
  },
  getHistoriqueGlobal() {
    return api.get('/sessions-caisse/historique-global');
  },
  async downloadReport(id) {
      const response = await api.get(`/sessions-caisse/${id}/rapport-pdf`, { responseType: 'blob' });
      const blob = new Blob([response.data], { type: 'application/pdf' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `rapport_cloture_${id}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
  }
};

export default sessionCaisseService;
