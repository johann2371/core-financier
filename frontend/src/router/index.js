import { createRouter, createWebHistory } from 'vue-router'
import { useUiStore } from '../stores/ui.store'
import { useAuthStore } from '../stores/auth.store'

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
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('../views/ProfileView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/audit',
      name: 'audit',
      component: () => import('../views/AuditView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/parametres',
      name: 'parametres',
      component: () => import('../views/ParametresView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/admin',
      name: 'admin',
      component: () => import('../views/DashboardAdminView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/journal-caisse',
      name: 'journal-caisse',
      component: () => import('../views/JournalCaisseView.vue'),
      meta: { requiresAuth: true }
    }
  ]
})

// Gardien de navigation (Navigation Guard) pour protéger les routes et gérer le chargement
router.beforeEach((to, from, next) => {
  const uiStore = useUiStore()
  uiStore.setLoading(true)

  const publicPages = ['/login'];
  const authRequired = !publicPages.includes(to.path);
  const loggedIn = localStorage.getItem('token');

  if (authRequired && !loggedIn) {
    uiStore.setLoading(false)
    return next('/login');
  }

  const authStore = useAuthStore()
  // L'Administrateur a désormais accès au métier suite à la demande utilisateur
  next();
})

router.afterEach(() => {
  const uiStore = useUiStore()
  // Un léger délai pour assurer que l'utilisateur voit la transition même sur des pages rapides
  setTimeout(() => {
    uiStore.setLoading(false)
  }, 400)
})

export default router
