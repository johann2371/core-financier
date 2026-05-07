import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import './assets/main.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)

app.config.errorHandler = (err, vm, info) => {
  console.error("Vue Error:", err, info);
  const errorDiv = document.createElement('div');
  errorDiv.style.position = 'fixed';
  errorDiv.style.top = '0';
  errorDiv.style.left = '0';
  errorDiv.style.width = '100vw';
  errorDiv.style.backgroundColor = 'rgba(220, 38, 38, 0.95)';
  errorDiv.style.color = 'white';
  errorDiv.style.padding = '20px';
  errorDiv.style.zIndex = '99999';
  errorDiv.style.fontFamily = 'monospace';
  errorDiv.style.overflow = 'auto';
  errorDiv.style.maxHeight = '100vh';
  errorDiv.innerHTML = `<h3>CRITICAL VUE ERROR</h3><p><b>Context:</b> ${info}</p><pre style="white-space: pre-wrap;">${err.stack || err.message || err}</pre>`;
  document.body.appendChild(errorDiv);
};

console.log("Vite HMR Force Reload - " + Date.now());

app.mount('#app')
