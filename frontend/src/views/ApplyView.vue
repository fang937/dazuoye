<script setup>
/**
 * 发起售后申请页。
 *
 * 关键约定：失败时保留输入并说明原因——提交被后端拒绝后不重建表单，
 * 只把 HTTP 状态码与后端中文原因显示在表单里，用户改完可以直接重交。
 */
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import { api } from '@/api/client';
import StateBox from '@/components/StateBox.vue';
import { auth } from '@/stores/auth';
import { hydrate, store } from '@/stores/identity';
import { notify } from '@/stores/notice';
import { formatMoney } from '@/utils/format';

const route = useRoute();
const router = useRouter();

const item = ref(null);
const state = reactive({ loading: true, error: '', blocked: '' });
const form = reactive({ type: 'RETURN', reasonCode: 'SIZE_MISMATCH', quantity: 1, description: '', evidenceUrl: '' });
const errorText = ref('');
const busy = ref(false);

const itemId = computed(() => Number(route.query.itemId));

async function loadItem() {
  await hydrate();
  state.loading = true;
  state.error = '';
  state.blocked = '';
  item.value = null;

  if (!itemId.value) {
    state.blocked = '没有指定订单商品：请从消费者工作台点某一行的「申请售后」进入本页';
    state.loading = false;
    return;
  }

  const result = await api.myOrderItems();
  state.loading = false;
  if (!result.ok) {
    state.error = result.status === 403
      ? `只有消费者能发起售后申请：${result.message}`
      : result.message || `读取失败（HTTP ${result.status}）`;
    return;
  }

  const found = result.data.find(candidate => candidate.orderItemId === itemId.value);
  if (!found) {
    state.blocked = '这行商品不属于当前身份，或已经不存在：切换身份或返回工作台重新选择';
    return;
  }
  if (!found.canApply) {
    state.blocked = `这行商品现在不能申请售后：${found.blockReason}`;
    return;
  }
  item.value = found;
  form.quantity = 1;
}

async function submit() {
  errorText.value = '';
  if (busy.value) {
    errorText.value = '上一次提交还在处理中，请等它返回';
    return;
  }
  if (!form.quantity || form.quantity < 1) {
    errorText.value = '申请数量至少为 1 件';
    return;
  }

  busy.value = true;
  const result = await api.createAfterSale({
    orderItemId: item.value.orderItemId,
    type: form.type,
    reasonCode: form.reasonCode,
    quantity: Number(form.quantity),
    description: form.description.trim(),
    evidenceUrl: form.evidenceUrl.trim()
  });
  busy.value = false;

  if (!result.ok) {
    errorText.value = `提交失败（HTTP ${result.status}）：${result.message || '后端未说明原因'}`;
    return;
  }

  notify.success(`售后单 ${result.data.afterSaleNo} 已写入数据库，正在跳转详情。`);
  router.replace({ name: 'detail', query: { id: result.data.id } });
}

onMounted(loadItem);
watch(itemId, loadItem);
</script>

<template>
  <header class="hero hero-compact">
    <div class="hero-left">
      <p class="eyebrow">APPLY · 消费者</p>
      <h1>发起退货 / 换货申请</h1>
      <p class="hero-description">
        一次申请只针对<strong>一行订单商品</strong>。提交后进入「待客服审核」，客服会先确认订单归属、
        售后期限和可售后数量，再决定同意退回、要求补充材料还是拒绝。
      </p>
    </div>
  </header>

  <section class="panel">
    <div class="panel-heading">
      <div>
        <p class="panel-eyebrow">FORM · 申请信息</p>
        <h2>填写售后申请</h2>
      </div>
      <RouterLink class="secondary-button compact" :to="{ name: 'consumer' }">返回工作台</RouterLink>
    </div>

    <div class="list-container">
      <StateBox v-if="state.loading" kind="loading" tip="正在读取要售后的商品..." />
      <StateBox v-else-if="state.error" kind="error" title="读不到订单商品" :tip="state.error" />
      <StateBox v-else-if="state.blocked" kind="error" title="现在不能提交申请" :tip="state.blocked" />

      <template v-else-if="item">
        <p class="form-context">
          商品：<strong>{{ item.productName }} / {{ item.spec }}</strong>　
          订单 {{ item.orderNo }}　购买 {{ item.quantity }} 件　单价 {{ formatMoney(item.unitPrice) }}　
          剩余可售后 <strong>{{ item.remainingQuantity }}</strong> 件　售后截止 {{ item.afterSaleDeadline }}
        </p>

        <form class="action-form" novalidate @submit.prevent="submit">
          <label class="form-label">
            <span class="label-text">售后类型 <span class="required-star">*</span></span>
            <select v-model="form.type">
              <option v-for="option in store.meta.types" :key="option.name" :value="option.name">{{ option.label }}</option>
            </select>
          </label>

          <label class="form-label">
            <span class="label-text">申请原因 <span class="required-star">*</span></span>
            <select v-model="form.reasonCode">
              <option v-for="option in store.meta.reasons" :key="option.name" :value="option.name">{{ option.label }}</option>
            </select>
          </label>

          <label class="form-label">
            <span class="label-text">申请数量 <span class="required-star">*</span></span>
            <input v-model="form.quantity" type="number" min="1" :max="item.remainingQuantity" step="1" />
          </label>

          <label class="form-label">
            <span class="label-text">问题说明</span>
            <textarea v-model="form.description" rows="3" maxlength="500" placeholder="例如：M 码上身偏小，希望换成 L 码，吊牌还在"></textarea>
          </label>

          <label class="form-label">
            <span class="label-text">图片凭证地址（模拟路径，可先不填）</span>
            <input v-model="form.evidenceUrl" type="text" placeholder="/mock/evidence/shirt-m.jpg" />
          </label>

          <p v-if="errorText" class="form-error">{{ errorText }}</p>

          <div class="form-actions">
            <button type="submit" class="primary-button" :disabled="busy">
              <span class="btn-text">提交售后申请</span>
              <span v-if="busy" class="btn-spinner" style="display:inline-block"></span>
            </button>
            <RouterLink class="secondary-button" :to="{ name: 'consumer' }">取消</RouterLink>
          </div>

          <p class="form-footnote">
            提交后跳到售后单详情，那里显示它此刻能做的操作和已经发生的时间线。
            单号与初始状态由后端生成，前端不传也改不了。
          </p>
        </form>
      </template>
    </div>
  </section>
</template>
