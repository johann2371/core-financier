import axios from 'axios';

async function testAudit() {
  try {
    // 1. Login
    console.log("Tentative de connexion...");
    const loginRes = await axios.post('http://localhost:8080/api/auth/login', {
      email: 'admin@corefi.com',
      password: 'password123'
    });
    
    const token = loginRes.data.token;
    console.log("Token reçu:", token.slice(0, 15) + "...");
    
    // 2. Fetch Audit Logs
    console.log("Récupération des logs d'audit...");
    const auditRes = await axios.get('http://localhost:8080/api/journal-audit', {
      headers: { Authorization: `Bearer ${token}` }
    });
    
    console.log("Status:", auditRes.status);
    console.log("Data:", JSON.stringify(auditRes.data).slice(0, 200));
    console.log("Total Elements:", auditRes.data.totalElements);
    console.log("Content Length:", auditRes.data.content ? auditRes.data.content.length : 'undefined');
    
  } catch (err) {
    if (err.response) {
      console.error("Erreur Backend:", err.response.status, err.response.data);
    } else {
      console.error("Erreur d'Axios:", err.message);
    }
  }
}

testAudit();
