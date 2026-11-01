<script setup>
/**
 * 处理进度条：五个业务环节。
 * 每个环节的完成时间直接取时间线里第一次进入该状态的事件记录（after_sale_event），
 * 不是前端按当前状态倒推出来的，答辩时可以逐条对到数据库。
 */
import { computed } from 'vue';
import { formatTime } from '@/utils/format';

const props = defineProps({
  view: { type: Object, required: true },
  timeline: { type: Array, default: () => [] }
});

const STEPS = [
  { label: '发起申请', markers: ['PENDING_REVIEW'] },
  { label: '客服审核并同意退回', markers: ['RETURN_GUIDED'] },
  { label: '仓库登记收货', markers: ['WAREHOUSE_RECEIVED'] },
  { label: '仓库给出验货结论', markers: ['INSPECTION_PASSED', 'INSPECTION_FAILED'] },
  { label: '客服确认结果并关闭', markers: ['EXCHANGE_DONE', 'REFUND_DONE', 'REJECTED'] }
];

const steps = computed(() => {
  let currentTaken = false;
  return STEPS.map(step => {
    const reached = props.timeline.find(event => step.markers.includes(event.toStatus));
    if (reached) {
      return { label: step.label, cls: 'step-done', note: formatTime(reached.createdAt) };
    }
    if (props.view.terminal) {
      return { label: step.label, cls: 'step-skipped', note: '未走此环节' };
    }
    if (!currentTaken) {
      currentTaken = true;
      return { label: step.label, cls: 'step-current', note: '正在此处' };
    }
    return { label: step.label, cls: 'step-todo', note: '待处理' };
  });
});
</script>

<template>
  <div>
    <p class="section-label">处理进度（时间取自 after_sale_event）</p>
    <ol class="progress-track">
      <li v-for="step in steps" :key="step.label" class="progress-step" :class="step.cls">
        <span class="progress-label">{{ step.label }}</span>
        <span class="progress-time">{{ step.note }}</span>
      </li>
    </ol>
  </div>
</template>
