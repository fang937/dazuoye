<script setup>
/**
 * 应用外壳：导航 + 通知条 + 岗位不匹配提示 + 路由视图 + 页脚。
 *
 * 岗位不匹配时不拦截跳转，只做提示：页面照常请求，后端拒绝后把原因显示出来，
 * 这样越权拦截可以在答辩现场直接演示。
 */
import { computed } from 'vue';
import { RouterView, useRoute } from 'vue-router';
import AppNav from '@/components/AppNav.vue';
import NoticeBar from '@/components/NoticeBar.vue';
import { API_BASE_LABEL } from '@/utils/env';
import { auth } from '@/stores/auth';
import { roleLabel } from '@/stores/identity';

const route = useRoute();

const isNarrowPage = computed(() => ['detail', 'apply', 'login'].includes(route.name));

const mismatchText = computed(() => {
  const pageRole = route.meta && route.meta.role;
  const user = auth.user;
  if (!pageRole || !user || user.role === pageRole) return '';
  return `本页属于「${roleLabel(pageRole)}」岗位，你登录的账号是「${user.displayName}」。`
    + '页面照常发起请求，被后端拒绝时会把原因显示出来——这就是要演示的越权拦截。';
});
</script>

<template>
  <AppNav />

  <main class="page-shell" :class="{ 'page-shell-narrow': isNarrowPage }">
    <NoticeBar />
    <p v-if="mismatchText" class="role-mismatch">{{ mismatchText }}</p>

    <RouterView />

    <footer class="page-footer">
      <div class="footer-item"><span class="footer-label">前端：</span><span>Vue 3 单文件组件 + Vite + Vue Router + Axios</span></div>
      <div class="footer-item"><span class="footer-label">后端：</span><span>Spring Boot 3 + MyBatis + MySQL</span></div>
      <div class="footer-item"><span class="footer-label">接口：</span><code>{{ API_BASE_LABEL }}</code></div>
      <div class="footer-item"><span class="footer-label">登录：</span><code>Authorization: Bearer &lt;token&gt;</code></div>
    </footer>
  </main>
</template>
