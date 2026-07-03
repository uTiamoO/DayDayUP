<template>
  <router-view />
</template>

<script setup lang="ts">
import { onMounted } from 'vue';

onMounted(() => {
  console.log('DayDayUP Admin Portal initialized');
});
</script>

