/**
 * 全局只读字典：枚举中文标签 + 登录页的演示账号列表。
 *
 * 这些接口不需要登录，所以和登录状态分开管理：
 * 登录页要先用它们渲染「一键填入」的账号卡片。
 */
import { reactive } from 'vue';
import { api } from '@/api/client';

export const store = reactive({
  meta: { roles: [], types: [], reasons: [], statuses: [], actions: [] },
  demoAccounts: [],
  online: null,
  hydrated: false
});

let hydrateTask = null;

export function hydrate() {
  if (!hydrateTask) {
    hydrateTask = (async () => {
      const [metaResult, usersResult] = await Promise.all([api.meta(), api.demoUsers()]);
      if (metaResult.ok && metaResult.data) {
        store.meta = { ...store.meta, ...metaResult.data };
      }
      if (usersResult.ok) {
        store.demoAccounts = usersResult.data;
      }
      store.online = metaResult.ok;
      store.hydrated = true;
    })();
  }
  return hydrateTask;
}

export function roleLabel(roleName) {
  const role = store.meta.roles.find(item => item.name === roleName);
  return role ? role.label : roleName;
}
