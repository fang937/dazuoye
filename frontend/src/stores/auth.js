/**
 * 登录状态：令牌 + 当前账号 + 岗位首页映射。
 *
 * 令牌存在 localStorage，刷新页面后先调 /auth/me 换回账号信息；
 * 换不回来（过期、被作废）就清掉，由路由守卫送去登录页。
 */
import { reactive } from 'vue';
import { api, clearToken, getToken, setToken } from '@/api/client';

export const auth = reactive({
  user: null,
  ready: false
});

/** 各岗位登录后默认进入的页面。 */
const HOME_BY_ROLE = {
  CONSUMER: 'consumer',
  CS: 'cs',
  WAREHOUSE: 'warehouse'
};

export function homeRouteFor(user) {
  return (user && HOME_BY_ROLE[user.role]) || 'home';
}

let restoreTask = null;

/**
 * 恢复会话：每个页面加载只需要真正执行一次，重复调用复用同一个 Promise。
 */
export function ensureSession() {
  if (!restoreTask) {
    restoreTask = (async () => {
      if (!getToken()) {
        auth.ready = true;
        return null;
      }
      const result = await api.me();
      if (result.ok) {
        auth.user = result.data;
      } else {
        clearToken();
        auth.user = null;
      }
      auth.ready = true;
      return auth.user;
    })();
  }
  return restoreTask;
}

export async function login(username, password) {
  const result = await api.login(username, password);
  if (!result.ok) {
    return result;
  }
  setToken(result.data.token);
  auth.user = result.data.user;
  auth.ready = true;
  restoreTask = Promise.resolve(auth.user);
  return result;
}

export async function logout() {
  await api.logout();
  clearToken();
  auth.user = null;
  restoreTask = null;
}
