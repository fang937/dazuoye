<script setup>
/**
 * 单个动作的表单。
 *
 * 三条约定：
 * 1. 提交期间整个表单禁用，避免连点发出两次请求；
 * 2. 失败时不重建表单，已填写内容留在原处，只把 HTTP 状态码与后端中文原因显示出来；
 * 3. 成功后由父组件用后端返回的最新单据重绘，页面显示的内容永远来自数据库。
 */
import { computed, reactive, ref } from 'vue';
import { api } from '@/api/client';
import { notify } from '@/stores/notice';
import { resolveActionConfig } from '@/utils/actions';
import { actionLabel } from '@/utils/labels';

const props = defineProps({
  view: { type: Object, required: true },
  action: { type: String, required: true }
});
const emit = defineEmits(['updated', 'cancelled']);

const config = computed(() => resolveActionConfig(props.action, props.view));
const model = reactive({});
const errorText = ref('');
const busy = ref(false);

function fieldValue(field) {
  const raw = model[field.key];
  if (raw === undefined || raw === null) return null;
  if (field.type === 'number') return raw === '' ? null : Number(raw);
  return raw.trim() === '' ? null : raw.trim();
}

async function submit() {
  errorText.value = '';
  if (busy.value) {
    errorText.value = '上一次操作还在处理中，请等它返回';
    return;
  }

  const payload = {};
  for (const field of config.value.fields) {
    const value = fieldValue(field);
    if (field.required && (value === null || value === undefined)) {
      errorText.value = `「${field.label}」是必填项，后端同样会拒绝这次操作`;
      return;
    }
    payload[field.key] = value;
  }

  // 审核与终定要把具体动作名告诉后端，后端按动作分支校验岗位与状态
  if (config.value.endpoint === '/review' || config.value.endpoint === '/finalize') {
    payload.action = props.action;
  }
  if (config.value.endpoint === '/inspect' && payload.result === 'PASSED') {
    payload.abnormalReason = null;
  }

  busy.value = true;
  const result = await api[config.value.api](props.view.id, payload);
  busy.value = false;

  if (!result.ok) {
    errorText.value = `操作被拦截（HTTP ${result.status}）：${result.message || '后端未说明原因'}`;
    if (result.status === 409) {
      notify.error('这张单据的状态已经变了（可能另一个岗位刚处理过）。填写内容保留在原处，可修改后再提交。');
    }
    return;
  }

  notify.success(`操作已记录，状态变为「${result.data.statusLabel}」。`);
  emit('updated', result.data);
}

function cancel() {
  emit('cancelled');
}
</script>

<template>
  <form class="action-form inline-form" novalidate @submit.prevent="submit">
    <p class="form-context">
      动作：<strong>{{ actionLabel(props.action) }}</strong>
      <code>POST /api/after-sales/{{ props.view.id }}{{ config.endpoint }}</code>
    </p>

    <label v-for="field in config.fields" :key="field.key" class="form-label">
      <span class="label-text">
        {{ field.label }}
        <span v-if="field.required" class="required-star">*</span>
      </span>

      <textarea
        v-if="field.type === 'textarea'"
        v-model="model[field.key]"
        rows="3"
        :placeholder="field.placeholder || ''"
      ></textarea>

      <select v-else-if="field.type === 'select'" v-model="model[field.key]">
        <option v-for="option in field.options" :key="option.name" :value="option.name">{{ option.label }}</option>
      </select>

      <input
        v-else
        v-model="model[field.key]"
        :type="field.type === 'number' ? 'number' : 'text'"
        :step="field.step || ''"
        :min="field.type === 'number' ? '0' : ''"
        :placeholder="field.placeholder || ''"
      />
    </label>

    <p v-if="errorText" class="form-error">{{ errorText }}</p>

    <div class="form-actions">
      <button type="submit" class="primary-button" :disabled="busy">
        <span class="btn-text">确认执行</span>
        <span v-if="busy" class="btn-spinner" style="display:inline-block"></span>
      </button>
      <button type="button" class="secondary-button" :disabled="busy" @click="cancel">取消</button>
    </div>

    <p class="form-footnote">提交后页面会重新读取这张单据：状态、仓库记录和时间线都来自数据库最新结果。</p>
  </form>
</template>
