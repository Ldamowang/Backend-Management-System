import { createI18n } from 'vue-i18n'

// 中文：默认语言，同步加载
import zhCommon from './zh-CN/common.json'
import zhMenu from './zh-CN/menu.json'
import zhLogin from './zh-CN/login.json'
import zhDashboard from './zh-CN/dashboard.json'
import zhSystem from './zh-CN/system.json'
import zhProfile from './zh-CN/profile.json'

const LOCALE_KEY = 'app-locale'

function getStoredLocale(): string {
  try {
    return localStorage.getItem(LOCALE_KEY) || 'zh-CN'
  } catch {
    return 'zh-CN'
  }
}

export function setStoredLocale(locale: string) {
  localStorage.setItem(LOCALE_KEY, locale)
}

const i18n = createI18n({
  legacy: false,
  locale: getStoredLocale(),
  fallbackLocale: 'zh-CN',
  messages: {
    'zh-CN': {
      common: zhCommon,
      menu: zhMenu,
      login: zhLogin,
      dashboard: zhDashboard,
      system: zhSystem,
      profile: zhProfile
    }
  }
})

// 英文：懒加载
const loadedLanguages = new Set(['zh-CN'])

export async function loadLanguage(locale: string) {
  if (loadedLanguages.has(locale)) {
    ;(i18n.global.locale as unknown as { value: string }).value = locale
    setStoredLocale(locale)
    return
  }

  if (locale === 'en-US') {
    const [common, menu, login, dashboard, system, profile] = await Promise.all([
      import('./en-US/common.json'),
      import('./en-US/menu.json'),
      import('./en-US/login.json'),
      import('./en-US/dashboard.json'),
      import('./en-US/system.json'),
      import('./en-US/profile.json')
    ])
    i18n.global.setLocaleMessage('en-US', {
      common: common.default,
      menu: menu.default,
      login: login.default,
      dashboard: dashboard.default,
      system: system.default,
      profile: profile.default
    })
  }

  loadedLanguages.add(locale)
  ;(i18n.global.locale as unknown as { value: string }).value = locale
  setStoredLocale(locale)
}

export const availableLocales = [
  { code: 'zh-CN', name: '中文' },
  { code: 'en-US', name: 'English' }
]

export default i18n
