import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue')
    },
    {
      path: '/',
      name: 'dashboard',
      component: () => import('../views/DashboardView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/decaissements',
      name: 'decaissements',
      component: () => import('../views/DecaissementsView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/encaissements',
      name: 'encaissements',
      component: () => import('../views/EncaissementsView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/tiers',
      name: 'tiers',
      component: () => import('../views/TiersView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/factures',
      name: 'factures',
      component: () => import('../views/FacturesView.vue'),
      meta: { requiresAuth: true }
    }
  ]
})

// Gardien de navigation (Navigation Guard) pour protéger les routes
router.beforeEach((to, from, next) => {
  const publicPages = ['/login'];
  const authRequired = !publicPages.includes(to.path);
  const loggedIn = localStorage.getItem('token'); // A remplacer par le store Pinia plus tard

  if (authRequired && !loggedIn) {
    return next('/login');
  }

  next();
})

export default router
