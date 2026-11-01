<script setup>
/**
 * 客服工作台：待处理队列（可按状态筛选）+ 全部售后单。
 * 队列只包含轮到自己处理的三种状态，其余单据在「全部」里可追溯。
 */
import { computed, reactive, ref } from 'vue';
import { api } from '@/api/client';
import AfterSaleCard from '@/components/AfterSaleCard.vue';
import StateBox from '@/components/StateBox.vue';
import { hydrate, store } from '@/stores/identity';
import { usePageLoader } from '@/utils/useLoader';

const QUEUE_STATUSES = ['PENDING_REVIEW', 'INSPECTION_PASSED', 'INSPECTION_FAILED'];

const queue = ref([]);
const all = ref([]);
const filter = ref('ALL');
const state = reactive({ loading: true, error: '' });

const filterOptions = computed(() =>
  store.meta.statuses
    .filter(status => QUEUE_STATUSES.includes(status.name))
    .map(status => ({ name: status.name, label: status.label }))
);

const visibleQueue = computed(() =>
  filter.value === 'ALL' ? queue.value : queue.value.filter(view => view.status === filter.value)
);

async function load() {
  await hydrate();
  state.loading = true;
  state.error = '';

  const [queueResult, allResult] = await Promise.all([api.csQueue(), api.allAfterSales()]);
  state.loading = false;

  if (!queueResult.ok) {
    state.error = queueResult.status === 403
      ? `这一页只允许售后客服处理：${queueResult.message}`
      : queueResult.message || `读取失败（HTTP ${queueResult.status}）`;
    return;
  }

  queue.value = queueResult.data;
  all.value = allResult.ok ? allResult.data : [];
}

usePageLoader(load);
</script>

<template>
  <header class="hero hero-compact">
    <div class="hero-left">
      <p class="eyebrow">CS WORKBENCH</p>
      <h1>售后客服工作台</h1>
      <p class="hero-description">
        客服负责两处判断：<strong>申请条件是否满足</strong>（是否本人订单、是否在售后期限内、数量是否超出），
        以及<strong>验货之后给什么结果</strong>（换货补发、模拟退款还是拒绝）。收到退货不等于退款完成。
      </p>
    </div>
    <div class="hero-right">
      <span class="count-badge">{{ queue.length }} 条待处理</span>
    </div>
  </header>

  <section class="panel">
    <div class="panel-heading">
      <div>
        <p class="panel-eyebrow">QUEUE · 轮到我处理的</p>
        <h2>待处理队列</h2>
      </div>
      <div class="heading-side">
        <select v-model="filter" class="filter-select">
          <option value="ALL">全部待处理状态</option>
          <option v-for="option in filterOptions" :key="option.name" :value="option.name">{{ option.label }}</option>
        </select>
        <button class="icon-refresh-btn" @click="load">刷新</button>
      </div>
    </div>

    <div class="list-container">
      <StateBox v-if="state.loading" kind="loading" tip="正在读取待处理队列..." />
      <StateBox v-else-if="state.error" kind="error" title="读不到待处理队列" :tip="state.error" />
      <StateBox
        v-else-if="!visibleQueue.length"
        kind="empty"
        title="此刻没有等你处理的售后单"
        tip="队列只包含待客服审核、验货通过待确认、验货异常待处理三种状态"
      />
      <AfterSaleCard v-for="view in visibleQueue" v-else :key="view.id" :view="view" />
    </div>
  </section>

  <section class="panel panel-spaced">
    <div class="panel-heading">
      <div>
        <p class="panel-eyebrow">ALL · 仅客服可查</p>
        <h2>全部售后单</h2>
      </div>
      <span class="count-badge">{{ all.length }} 张</span>
    </div>
    <div class="list-container">
      <StateBox v-if="state.loading" kind="loading" tip="正在读取全部售后单..." />
      <AfterSaleCard v-for="view in all" v-else :key="view.id" :view="view" compact />
    </div>
  </section>
</template>
