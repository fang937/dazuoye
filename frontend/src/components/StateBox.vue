<script setup>
/**
 * 三种非正常状态统一用这一个组件：加载中 / 空数据 / 错误（含无权限）。
 * 第三章设计要求「正常、空数据、异常、无权限」四种状态都要有明确表现。
 */
defineProps({
  kind: { type: String, default: 'loading' }, // loading | empty | error
  title: { type: String, default: '' },
  tip: { type: String, default: '' }
});
</script>

<template>
  <div v-if="kind === 'loading'" class="state-box loading-box">
    <span class="spinner-large"></span>
    <p>{{ tip || title || '正在读取数据...' }}</p>
  </div>

  <div v-else-if="kind === 'error'" class="state-box error-box">
    <span class="state-icon state-icon-error">!</span>
    <p class="error-title">{{ title }}</p>
    <p class="error-detail">{{ tip }}</p>
  </div>

  <div v-else class="state-box empty-box">
    <span class="state-icon state-icon-empty">空</span>
    <p class="empty-title">{{ title }}</p>
    <p class="empty-tip">{{ tip }}</p>
  </div>
</template>
