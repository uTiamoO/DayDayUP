<template>
  <el-config-provider :locale="elementPlusLocale">
    <router-view />
  </el-config-provider>
</template>

<script setup lang="ts">
import { computed, onMounted, watch } from 'vue';
import { useRoute } from 'vue-router';
import { i18n, resolveElementPlusLocale } from '@/i18n';
import { useAppStore } from '@/stores/app';

const appStore = useAppStore();
const route = useRoute();
const elementPlusLocale = computed(() => resolveElementPlusLocale(appStore.locale));

watch(
  [() => appStore.locale, () => route.fullPath],
  () => {
    const routeTitleKey = route.meta.titleKey as string | undefined;
    const routeTitle = routeTitleKey ? i18n.global.t(routeTitleKey) : ((route.meta.title as string) || '');
    const appTitle = i18n.global.t('app.name');
    document.title = routeTitle ? `${routeTitle} - ${appTitle}` : appTitle;
  },
  { immediate: true }
);

onMounted(() => {
  appStore.initPreferences();
});
</script>
