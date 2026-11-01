<script setup>
/**
 * 仓库工作台：待收货 / 已收货待验货两组。
 * 这两组直接对应状态机里的两个状态，任务只在客服同意退回之后才出现。
 */
import { computed, reactive, ref } from 'vue';
import { api } from '@/api/client';
import AfterSaleCard from '@/components/AfterSaleCard.vue';
import StateBox from '@/components/StateBox.vue';
import { auth } from '@/stores/auth';
import { hydrate } from '@/stores/identity';
import { usePageLoader } from '@/utils/useLoader';

const queue = ref([]);
const state = reactive({ loading: true, error: '' });

const waitingReceive = computed(() => queue.value.filter(view => view.status === 'RETURN_GUIDED'));
const waitingInspect = computed(() => queue.value.filter(view => view.status === 'WAREHOUSE_RECEIVED'));

async function load() {
  await hydrate();
  state.loading = true;
  state.error = '';

  const queueResult = await api.warehouseQueue();
  state.loading = false;

  if (!queueResult.ok) {
    state.error = queueResult.status === 403
      ? `这一页只允许仓库人员处理：${queueResult.message}`
      : queueResult.message || `读取失败（HTTP ${queueResult.status}）`;
    return;
  }
  queue.value = queueResult.data;
}

usePageLoader(load);
</script>

<template>
  <header class="hero hero-compact">
    <div class="hero-left">
      <p class="eyebrow">WAREHOUSE WORKBENCH</p>
      <h1>仓库工作台</h1>
      <p class="hero-description">
        仓库只做两件事：<strong>登记收到退回包裹</strong>和<strong>录入验货结果</strong>。
        客服没有同意退回之前，这里不会出现任务；验货通过也不等于退款或换货完成，结果要由客服确认。
      </p>
    </div>
    <div class="hero-right">
      <span class="count-badge">{{ (auth.user || {}).displayName }} · 待收货 {{ waitingReceive.length }} / 待验货 {{ waitingInspect.length }}</span>
    </div>
  </header>

  <section class="panel">
    <div class="panel-heading">
      <div>
        <p class="panel-eyebrow">STEP 1 · 包裹签收</p>
        <h2>待收货</h2>
      </div>
      <span class="count-badge">{{ waitingReceive.length }} 件</span>
    </div>
    <div class="list-container">
      <StateBox v-if="state.loading" kind="loading" tip="正在读取仓库任务..." />
      <StateBox v-else-if="state.error" kind="error" title="读不到仓库任务队列" :tip="state.error" />
      <StateBox
        v-else-if="!waitingReceive.length"
        kind="empty"
        title="现在没有待收货的退回包裹"
        tip="只有客服点了「同意退回并给出售后指引」，任务才会出现在这里"
      />
      <AfterSaleCard v-for="view in waitingReceive" v-else :key="view.id" :view="view" />
    </div>
  </section>

  <section class="panel panel-spaced">
    <div class="panel-heading">
      <div>
        <p class="panel-eyebrow">STEP 2 · 商品检查</p>
        <h2>已收货待验货</h2>
      </div>
      <div class="heading-side">
        <span class="count-badge">{{ waitingInspect.length }} 件</span>
        <button class="icon-refresh-btn" @click="load">刷新</button>
      </div>
    </div>
    <div class="list-container">
      <StateBox v-if="state.loading" kind="loading" tip="正在读取仓库任务..." />
      <StateBox
        v-else-if="!waitingInspect.length && !state.error"
        kind="empty"
        title="现在没有待验货的商品"
        tip="登记收货之后，包裹会进入这一组等你录验货结论"
      />
      <AfterSaleCard v-for="view in waitingInspect" v-else :key="view.id" :view="view" />
    </div>
  </section>
</template>
