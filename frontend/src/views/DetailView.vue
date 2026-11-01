<script setup>
/**
 * 售后单详情页：单据信息 + 处理进度 + 仓库记录 + 时间线 + 当前身份能做的动作。
 *
 * 单据 id 挂在地址上（/#/detail?id=6），刷新或重新进入都重新向后端读取，
 * 所以「刷新后看到的状态与历史」永远和数据库一致。
 */
import { computed, reactive, ref, watch } from 'vue';
import { RouterLink, useRoute } from 'vue-router';
import { api } from '@/api/client';
import ActionForm from '@/components/ActionForm.vue';
import ProgressTrack from '@/components/ProgressTrack.vue';
import StateBox from '@/components/StateBox.vue';
import TimelineList from '@/components/TimelineList.vue';
import WarehouseRecordPanel from '@/components/WarehouseRecordPanel.vue';
import { auth } from '@/stores/auth';
import { hydrate, roleLabel } from '@/stores/identity';
import { formatMoney, formatTime } from '@/utils/format';
import { statusChipClass } from '@/utils/labels';
import { usePageLoader } from '@/utils/useLoader';

const WORKBENCH_BY_ROUTE = {
  CONSUMER: 'consumer',
  CS: 'cs',
  WAREHOUSE: 'warehouse'
};

const route = useRoute();
const view = ref(null);
const state = reactive({ loading: true, error: '', status: 0 });
const selectedAction = ref('');

const errorTitle = computed(() => (state.status === 403 ? '没有权限查看这张售后单' : '读不到这张售后单'));

const docId = computed(() => Number(route.query.id));
const actions = computed(() => (view.value && view.value.allowedActions) || []);
const backRouteName = computed(() => {
  const user = auth.user;
  return user && WORKBENCH_BY_ROUTE[user.role] ? WORKBENCH_BY_ROUTE[user.role] : 'home';
});
const backLabel = computed(() => {
  const user = auth.user;
  return user ? `回${roleLabel(user.role)}工作台` : '回首页';
});

async function load() {
  await hydrate();
  state.loading = true;
  state.error = '';
  selectedAction.value = '';

  if (!docId.value) {
    state.loading = false;
    state.status = 0;
    state.error = '没有指定售后单：请从某个岗位工作台点「查看并处理」进入本页';
    return;
  }

  const result = await api.detail(docId.value);
  state.loading = false;

  if (!result.ok) {
    view.value = null;
    state.status = result.status;
    state.error = result.message || `读取失败（HTTP ${result.status}）`;
    return;
  }
  state.status = 0;
  view.value = result.data;
}

function onActionUpdated(latest) {
  view.value = latest;
  selectedAction.value = '';
}

usePageLoader(load);
watch(docId, load);
</script>

<template>
  <header class="hero hero-compact">
    <div class="hero-left">
      <p class="eyebrow">AFTER-SALES DETAIL</p>
      <h1>售后单详情、处理进度与操作</h1>
      <p class="hero-description">
        这一页把<strong>单据信息、仓库记录、操作时间线、当前身份能做的动作</strong>放在一起。
        动作按钮由后端返回的 allowedActions 渲染：按钮出现，就代表后端确认这个岗位此刻允许这个操作。
      </p>
    </div>
    <div class="hero-right">
      <span class="step-badge">{{ view ? view.statusLabel : (state.error ? '未读取' : '加载中') }}</span>
    </div>
  </header>

  <section class="panel">
    <div class="panel-heading">
      <div>
        <p class="panel-eyebrow">地址参数 · /#/detail?id=单据号</p>
        <h2>单据内容</h2>
      </div>
      <div class="heading-side">
        <RouterLink class="secondary-button compact" :to="{ name: backRouteName }">{{ backLabel }}</RouterLink>
        <button class="icon-refresh-btn" @click="load">重新读取</button>
      </div>
    </div>

    <div class="list-container">
      <StateBox v-if="state.loading" kind="loading" tip="正在读取售后单详情..." />
      <StateBox
        v-else-if="state.error"
        kind="error"
        :title="errorTitle"
        :tip="state.error"
      />

      <template v-else-if="view">
        <div class="status-banner" :class="statusChipClass(view.status)">
          <div class="banner-main">
            <p class="banner-title">{{ view.statusLabel }}</p>
            <p class="banner-sub">
              {{ view.terminal ? '售后流程已关闭，以下为完整处理记录' : `下一步由「${view.ownerRoleLabel}」处理` }}
            </p>
          </div>
          <div class="banner-side">
            <span>{{ view.afterSaleNo }}</span>
            <span>{{ view.terminal ? '关闭于 ' + formatTime(view.closedAt) : '更新于 ' + formatTime(view.updatedAt) }}</span>
          </div>
        </div>

        <div class="detail-head">
          <p class="detail-no">
            {{ view.productName }}<span class="detail-spec">/ {{ view.spec }}</span>
          </p>
          <p class="detail-sub">
            {{ view.typeLabel }} {{ view.quantity }} 件 · {{ view.reasonLabel }} · 申请人 {{ view.consumerName }}
          </p>
        </div>

        <ProgressTrack :view="view" :timeline="view.timeline || []" />

        <p class="section-label list-divider">单据信息（after_sale + order_item + mock_order）</p>
        <dl class="detail-grid">
          <div><dt>申请人</dt><dd>{{ view.consumerName }}</dd></div>
          <div><dt>订单号</dt><dd>{{ view.orderNo }}</dd></div>
          <div><dt>商品</dt><dd>{{ view.productName }} / {{ view.spec }}</dd></div>
          <div><dt>成交单价</dt><dd>{{ formatMoney(view.unitPrice) }}</dd></div>
          <div><dt>售后截止</dt><dd>{{ view.afterSaleDeadline || '—' }}</dd></div>
          <div><dt>剩余可售后</dt><dd>{{ view.remainingQuantity === null || view.remainingQuantity === undefined ? '—' : view.remainingQuantity + ' 件' }}</dd></div>
          <div class="span-2"><dt>消费者说明</dt><dd class="multiline">{{ view.description || '（未填写）' }}</dd></div>
          <div class="span-2"><dt>图片凭证</dt><dd class="mono">{{ view.evidenceUrl || '（未上传）' }}</dd></div>
          <div><dt>申请时间</dt><dd>{{ formatTime(view.createdAt) }}</dd></div>
          <div><dt>关闭时间</dt><dd>{{ formatTime(view.closedAt) }}</dd></div>
        </dl>

        <WarehouseRecordPanel :warehouse="view.warehouse" />

        <p class="section-label list-divider">操作时间线（after_sale_event，共 {{ (view.timeline || []).length }} 条）</p>
        <TimelineList :timeline="view.timeline || []" />

        <p class="section-label list-divider">当前身份此刻可执行的操作</p>
        <p v-if="view.terminal" class="form-footnote">
          这张单据已进入终态，售后流程已关闭，不能再发生状态变化。上面的时间线就是完整处理记录。
        </p>
        <p v-else-if="!actions.length" class="form-footnote">
          当前身份在这张单据的「{{ view.statusLabel }}」状态下没有可执行操作，请等下一个岗位处理。
        </p>
        <template v-else>
          <div class="action-buttons">
            <button
              v-for="(action, index) in actions"
              :key="action.name"
              type="button"
              class="compact"
              :class="[index === 0 ? 'primary-button' : 'secondary-button', { 'btn-active': selectedAction === action.name }]"
              @click="selectedAction = action.name"
            >{{ action.label }}</button>
          </div>
          <div v-if="selectedAction" class="action-form-slot">
            <ActionForm
              :key="`${view.id}-${selectedAction}-${view.status}`"
              :view="view"
              :action="selectedAction"
              @updated="onActionUpdated"
              @cancelled="selectedAction = ''"
            />
          </div>
        </template>
      </template>
    </div>
  </section>
</template>
