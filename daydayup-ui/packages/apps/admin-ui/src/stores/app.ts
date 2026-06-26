import { defineStore } from 'pinia';
import { ref } from 'vue';

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(false);
  const theme = ref<'light' | 'dark'>('light');
  const locale = ref('zh-CN');

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value;
  }

  function setSidebarCollapsed(collapsed: boolean) {
    sidebarCollapsed.value = collapsed;
  }

  function setTheme(newTheme: 'light' | 'dark') {
    theme.value = newTheme;
    if (newTheme === 'dark') {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }

  function setLocale(newLocale: string) {
    locale.value = newLocale;
  }

  return {
    sidebarCollapsed,
    theme,
    locale,
    toggleSidebar,
    setSidebarCollapsed,
    setTheme,
    setLocale,
  };
});
