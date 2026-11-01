<script setup>
/**
 * 顶部导航：品牌 + 当前岗位能用的入口 + 当前账号 + 退出登录。
 *
 * 不按岗位隐藏的页面（首页、详情）仍在路由里，直接改地址访问别人的工作台
 * 会由后端返回 403，页面把原因显示出来——保留这个演示价值。
 */
import { computed } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import { auth, logout } from '@/stores/auth';

const router = useRouter();

const NAV_ITEMS = [
  { name: 'home', label: '项目首页', roles: null },
  { name: 'consumer', label: '消费者工作台', roles: ['CONSUMER'] },
  { name: 'cs', label: '客服工作台', roles: ['CS'] },
  { name: 'warehouse', label: '仓库工作台', roles: ['WAREHOUSE'] }
];

const navItems = computed(() => NAV_ITEMS.filter(item => !item.roles || (auth.user && item.roles.includes(auth.user.role))));

async function onLogout() {
  await logout();
  router.replace({ name: 'login' });
}
</script>

<template>
  <nav class="page-nav">
    <div class="nav-inner">
      <RouterLink class="nav-brand" :to="{ name: 'home' }">
        <span class="brand-mark">售</span>
        <span class="brand-text">
          <strong>服装网店售后系统</strong>
          <small>退换货处理 · 三岗位协作</small>
        </span>
      </RouterLink>

      <div class="nav-links" v-if="auth.user">
        <RouterLink
          v-for="item in navItems"
          :key="item.name"
          :to="{ name: item.name }"
          class="nav-link"
          active-class="nav-link-active"
        >{{ item.label }}</RouterLink>
      </div>

      <div class="nav-identity" v-if="auth.user">
        <span class="nav-user">{{ auth.user.displayName }}</span>
        <span class="chip chip-info">{{ auth.user.roleLabel }}</span>
        <button type="button" class="secondary-button compact" @click="onLogout">退出登录</button>
      </div>
    </div>
  </nav>
</template>
