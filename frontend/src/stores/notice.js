/**
 * 顶部通知条：任何页面调用 notify.success / notify.error 都能显示，
 * 成功提示 5 秒自动收起，错误提示留在原地供截图。
 */
import { reactive } from 'vue';

export const notice = reactive({ type: 'success', message: '', visible: false });

let timer = null;

function show(type, message) {
  notice.type = type;
  notice.message = message;
  notice.visible = true;
  if (timer) clearTimeout(timer);
  if (type === 'success') {
    timer = setTimeout(() => {
      notice.visible = false;
    }, 5000);
  }
}

export const notify = {
  success: message => show('success', message),
  error: message => show('error', message),
  hide: () => {
    notice.visible = false;
  }
};
