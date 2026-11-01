<script setup>
/**
 * 仓库记录面板：收货、验货结论、异常原因、补发单号、模拟退款、客服结论。
 * 这张表的存在就是为了说明「收到退货」「验货通过」「售后完成」是三件不同的事。
 */
import { computed } from 'vue';
import { inspectionLabel } from '@/utils/labels';
import { formatMoney, formatTime } from '@/utils/format';

const props = defineProps({
  warehouse: { type: Object, default: null }
});

const hasRecord = computed(() => Boolean(props.warehouse));
</script>

<template>
  <div v-if="!hasRecord">
    <p class="warehouse-empty">仓库还没有登记这条单据的收货信息。</p>
  </div>
  <div v-else>
    <p class="section-label list-divider">仓库记录（warehouse_record）</p>
    <dl class="detail-grid">
      <div><dt>登记收货</dt><dd>{{ formatTime(props.warehouse.receivedAt) }}</dd></div>
      <div><dt>验货结果</dt><dd>{{ inspectionLabel(props.warehouse.inspectionResult) }}</dd></div>
      <div class="span-2"><dt>异常原因</dt><dd>{{ props.warehouse.abnormalReason || '—' }}</dd></div>
      <div><dt>补发单号</dt><dd class="mono">{{ props.warehouse.reissueNo || '—' }}</dd></div>
      <div>
        <dt>模拟退款</dt>
        <dd>
          <template v-if="props.warehouse.refundAmount">
            {{ formatMoney(props.warehouse.refundAmount) }}（{{ props.warehouse.refundResult }}）
          </template>
          <template v-else>—</template>
        </dd>
      </div>
      <div class="span-2"><dt>客服结论</dt><dd>{{ props.warehouse.finalNote || '—' }}</dd></div>
    </dl>
  </div>
</template>
