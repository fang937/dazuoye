<script setup>
/**
 * 消费者工作台：可申请的商品行 + 我发起的售后单。
 * 「能不能申请、为什么不能」由后端算好返回（canApply / blockReason），页面只显示。
 */
import { reactive, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { api } from '@/api/client';
import AfterSaleCard from '@/components/AfterSaleCard.vue';
import StateBox from '@/components/StateBox.vue';
import { auth } from '@/stores/auth';
import { hydrate } from '@/stores/identity';
import { formatMoney } from '@/utils/format';
import { usePageLoader } from '@/utils/useLoader';

const items = ref([]);
const mine = ref([]);
const state = reactive({ loading: true, error: '' });

async function load() {
  await hydrate();
  state.loading = true;
  state.error = '';

  const [itemsResult, mineResult] = await Promise.all([api.myOrderItems(), api.myAfterSales()]);
  state.loading = false;

  if (!itemsResult.ok) {
    state.error = itemsResult.status === 403
      ? `这一页只允许消费者查看：${itemsResult.message}`
      : itemsResult.message || `读取失败（HTTP ${itemsResult.status}）`;
    return;
  }

  items.value = itemsResult.data;
  mine.value = mineResult.ok ? mineResult.data : [];
}

usePageLoader(load);
</script>

<template>
  <header class="hero hero-compact">
    <div class="hero-left">
      <p class="eyebrow">CONSUMER WORKBENCH</p>
      <h1>消费者工作台</h1>
      <p class="hero-description">
        只能看到<strong>本人的</strong>模拟订单与售后单。申请时系统会校验：订单是否已完成、是否仍在售后期限内、
        申请数量是否超出可售后数量、是否已有进行中的同一申请。
      </p>
    </div>
    <div class="hero-right">
      <span class="count-badge">{{ (auth.user || {}).displayName }} · {{ items.length }} 行商品 / {{ mine.length }} 张售后单</span>
    </div>
  </header>

  <section class="panel">
    <div class="panel-heading">
      <div>
        <p class="panel-eyebrow">STEP 1 · 选择商品</p>
        <h2>我的模拟订单商品</h2>
      </div>
      <button class="icon-refresh-btn" @click="load">刷新</button>
    </div>

    <div class="list-container">
      <StateBox v-if="state.loading" kind="loading" tip="正在读取订单商品..." />
      <StateBox v-else-if="state.error" kind="error" title="读不到订单商品" :tip="state.error" />
      <StateBox v-else-if="!items.length" kind="empty" title="这个账号名下没有已完成的订单" tip="切换演示身份为小红 hong 看看另一批订单" />
      <template v-else>
        <article
          v-for="item in items"
          :key="item.orderItemId"
          class="task-card"
          :class="{ 'task-card-disabled': !item.canApply }"
        >
          <div class="task-main">
            <p class="task-title">{{ item.productName }}<span class="task-sub">{{ item.spec }}</span></p>
            <p class="task-meta">
              订单 {{ item.orderNo }} · 购买 {{ item.quantity }} 件 · 剩余可售后
              <strong>{{ item.remainingQuantity }}</strong> 件 · 单价 {{ formatMoney(item.unitPrice) }}
            </p>
            <p class="task-meta">售后申请截止：{{ item.afterSaleDeadline }}</p>
            <p class="task-note" :class="{ 'task-note-ok': item.canApply }">
              {{ item.canApply ? '可以申请售后' : (item.blockReason || '不可申请') }}
            </p>
          </div>
          <div class="task-side">
            <RouterLink
              v-if="item.canApply"
              class="primary-button compact"
              :to="{ name: 'apply', query: { itemId: item.orderItemId } }"
            >申请售后</RouterLink>
            <span v-else class="chip chip-plain">不可申请</span>
          </div>
        </article>
      </template>
    </div>
  </section>

  <section class="panel panel-spaced">
    <div class="panel-heading">
      <div>
        <p class="panel-eyebrow">STEP 2 · 跟踪进度</p>
        <h2>我发起的售后单</h2>
      </div>
      <span class="count-badge">{{ mine.length }} 张</span>
    </div>

    <div class="list-container">
      <StateBox v-if="state.loading" kind="loading" tip="正在读取售后单..." />
      <StateBox v-else-if="state.error" kind="error" title="读不到售后单" :tip="state.error" />
      <StateBox v-else-if="!mine.length" kind="empty" title="还没有发起过售后申请" tip="在上方选一行商品点「申请售后」" />
      <AfterSaleCard v-for="view in mine" v-else :key="view.id" :view="view" />
    </div>
  </section>
</template>
