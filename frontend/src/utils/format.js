/** 显示格式化工具：后端给的是 ISO 字符串与数字，页面统一在这里格式化。 */

export function formatTime(isoText) {
  if (!isoText) return '—';
  return isoText.replace('T', ' ').substring(0, 16);
}

export function formatMoney(amount) {
  if (amount === null || amount === undefined) return '—';
  return '¥ ' + Number(amount).toFixed(2);
}

export function formatDate(dateText) {
  return dateText || '—';
}
