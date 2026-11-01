/**
 * 页面加载器：进入页面时执行一次。
 *
 * 登录后切换账号会重新登录并跳转，页面本身不需要监听身份变化；
 * 所以这里只做 onMounted，比之前「监听身份变化」更简单。
 */
import { onMounted } from 'vue';

export function usePageLoader(loader) {
  onMounted(loader);
}
