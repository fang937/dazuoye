/**
 * 中文标签与状态样式。
 *
 * 标签一律来自后端 /api/demo/meta 的字典（枚举 name + label），
 * 前端不再自己抄一份中文，避免两边说法不一致。
 */
import { store } from '@/stores/identity';

export function labelOf(list, name) {
  const found = (list || []).find(item => item.name === name);
  return found ? found.label : name;
}

export function statusText(name) {
  if (!name) return '起点';
  return labelOf(store.meta.statuses, name);
}

export function actionLabel(name) {
  return labelOf(store.meta.actions, name);
}

export function typeLabel(name) {
  return labelOf(store.meta.types, name);
}

export function reasonLabel(name) {
  return labelOf(store.meta.reasons, name);
}

const INSPECTION_LABELS = [
  { name: 'PENDING', label: '待验货' },
  { name: 'PASSED', label: '验货通过' },
  { name: 'ABNORMAL', label: '验货异常' }
];

export function inspectionLabel(name) {
  return labelOf(INSPECTION_LABELS, name);
}

/** 状态颜色：终态绿、异常与拒绝红、待处理黄、流程中蓝。 */
export function statusChipClass(status) {
  switch (status) {
    case 'INSPECTION_FAILED':
    case 'REJECTED':
      return 'chip-danger';
    case 'EXCHANGE_DONE':
    case 'REFUND_DONE':
      return 'chip-success';
    case 'PENDING_REVIEW':
    case 'NEED_EVIDENCE':
      return 'chip-warn';
    default:
      return 'chip-info';
  }
}

export const INSPECTION_OPTIONS = INSPECTION_LABELS;
