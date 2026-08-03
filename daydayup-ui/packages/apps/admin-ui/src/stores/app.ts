import { defineStore } from 'pinia';
import { ref } from 'vue';
import { localStorageHelper } from '@daydayup/shared';
import { type AppLocale, applyAppLocale, normalizeAppLocale } from '@/i18n';

const THEME_STORAGE_KEY = 'app.theme';
const LOCALE_STORAGE_KEY = 'app.locale';

type ThemeMode = 'light' | 'dark';

function applyThemeClass(mode: ThemeMode) {
  if (mode === 'dark') {
    document.documentElement.classList.add('dark');
  } else {
    document.documentElement.classList.remove('dark');
  }
}

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(false);
  const theme = ref<ThemeMode>(localStorageHelper.getItem<ThemeMode>(THEME_STORAGE_KEY) ?? 'light');
  const locale = ref<AppLocale>(
    normalizeAppLocale(localStorageHelper.getItem<string>(LOCALE_STORAGE_KEY) ?? 'zh-CN')
  );

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value;
  }

  function setSidebarCollapsed(collapsed: boolean) {
    sidebarCollapsed.value = collapsed;
  }

  function setTheme(newTheme: ThemeMode) {
    theme.value = newTheme;
    localStorageHelper.setItem(THEME_STORAGE_KEY, newTheme);
    applyThemeClass(newTheme);
  }

  function setLocale(newLocale: string) {
    const normalizedLocale = applyAppLocale(newLocale);
    locale.value = normalizedLocale;
    localStorageHelper.setItem(LOCALE_STORAGE_KEY, normalizedLocale);
  }

  /** 应用启动时按持久化偏好恢复主题与语言。 */
  function initPreferences() {
    applyThemeClass(theme.value);
    locale.value = applyAppLocale(locale.value);
  }

  return {
    sidebarCollapsed,
    theme,
    locale,
    toggleSidebar,
    setSidebarCollapsed,
    setTheme,
    setLocale,
    initPreferences,
  };
});
