<script setup>
/** 全局通知条，内容由 stores/notice.js 驱动。 */
import { notice } from '@/stores/notice';
</script>

<template>
  <div
    id="globalNotice"
    class="notice"
    :class="[notice.type, { 'notice-hidden': !notice.visible }]"
    role="alert"
  >
    <span class="notice-badge">{{ notice.type === 'success' ? '成功' : '失败' }}</span>
    <span class="notice-message">{{ notice.message }}</span>
  </div>
</template>
