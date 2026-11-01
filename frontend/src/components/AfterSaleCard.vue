<script setup>
/**
 * 售后单列表卡片，三个岗位的工作台共用。
 * 左侧色条按状态着色（待处理黄 / 流程中蓝 / 完成绿 / 异常红），方便一眼扫出队列结构。
 */
import { RouterLink } from 'vue-router';
import { statusChipClass } from '@/utils/labels';
import { formatTime } from '@/utils/format';

const props = defineProps({
  view: { type: Object, required: true },
  compact: { type: Boolean, default: false }
});
</script>

<template>
  <article class="task-card with-stripe" :class="statusChipClass(props.view.status)">
    <div class="task-main">
      <p class="task-title">
        <RouterLink class="link-strong" :to="{ name: 'detail', query: { id: props.view.id } }">
          {{ props.view.afterSaleNo }}
        </RouterLink>
        <span class="chip" :class="statusChipClass(props.view.status)">{{ props.view.statusLabel }}</span>
      </p>
      <p class="task-meta">
        {{ props.view.typeLabel }} {{ props.view.quantity }} 件 ·
        {{ props.view.productName }}（{{ props.view.spec }}）· {{ props.view.reasonLabel }}
      </p>
      <p v-if="!props.compact" class="task-meta">
        申请人：{{ props.view.consumerName }} · 最近更新 {{ formatTime(props.view.updatedAt) }}
      </p>
      <p class="task-note">
        <span class="next-owner">{{ props.view.terminal ? '已关闭' : '下一步' }}</span>
        <span v-if="!props.view.terminal">由「{{ props.view.ownerRoleLabel }}」处理</span>
      </p>
    </div>
    <div class="task-side">
      <RouterLink
        class="secondary-button compact"
        :to="{ name: 'detail', query: { id: props.view.id } }"
      >查看并处理</RouterLink>
    </div>
  </article>
</template>
