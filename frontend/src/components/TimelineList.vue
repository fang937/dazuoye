<script setup>
/**
 * 操作时间线：岗位、动作、变化前后状态、说明、时间。
 * 用序号代替小圆点，答辩时可以按「第 1 步、第 2 步」直接讲。
 */
import { roleLabel } from '@/stores/identity';
import { statusText } from '@/utils/labels';
import { formatTime } from '@/utils/format';

defineProps({
  timeline: { type: Array, default: () => [] }
});
</script>

<template>
  <ul v-if="timeline.length" class="timeline">
    <li v-for="(event, index) in timeline" :key="event.id" class="timeline-item">
      <span class="timeline-index">{{ index + 1 }}</span>
      <p class="timeline-head">
        <strong>{{ event.action }}</strong>
        <span class="chip chip-plain">{{ roleLabel(event.operatorRole) }}</span>
        <span class="timeline-who">{{ event.operatorName }}</span>
        <span class="timeline-time">{{ formatTime(event.createdAt) }}</span>
      </p>
      <p class="timeline-status">{{ statusText(event.fromStatus) }} → {{ statusText(event.toStatus) }}</p>
      <p class="timeline-note">{{ event.note || '（无补充说明）' }}</p>
    </li>
  </ul>
  <p v-else class="form-footnote">这张单据还没有任何操作记录。</p>
</template>
