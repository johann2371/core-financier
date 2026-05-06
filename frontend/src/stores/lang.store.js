import { defineStore } from 'pinia'
import fr from '../i18n/fr'
import en from '../i18n/en'

const translations = { fr, en }

export const useLangStore = defineStore('lang', {
  state: () => ({
    locale: localStorage.getItem('app_locale') || 'fr'
  }),

  getters: {
    t: (state) => {
      const dict = translations[state.locale] || translations.fr
      // Returns a function t('nav.dashboard') that navigates through nested keys
      return (key) => {
        const keys = key.split('.')
        let result = dict
        for (const k of keys) {
          if (result && typeof result === 'object' && k in result) {
            result = result[k]
          } else {
            return key // fallback: return the key itself
          }
        }
        return result
      }
    },

    isFr: (state) => state.locale === 'fr',
    isEn: (state) => state.locale === 'en',

    flagEmoji: (state) => state.locale === 'fr' ? '🇫🇷' : '🇬🇧'
  },

  actions: {
    setLocale(locale) {
      this.locale = locale
      localStorage.setItem('app_locale', locale)
    },
    toggle() {
      this.setLocale(this.locale === 'fr' ? 'en' : 'fr')
    }
  }
})
