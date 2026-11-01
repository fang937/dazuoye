<script setup>
/**
 * 登录页：左侧说明这条链路怎么走，右侧登录并按岗位进入工作台。
 *
 * 演示账号来自公开接口 /api/demo/users（不含密码），点击即可填入；
 * 登录成功后跳到 redirect 指定页面，没有就按岗位进各自的工作台。
 */
import { onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { auth, homeRouteFor, login } from '@/stores/auth';
import { hydrate, roleLabel, store } from '@/stores/identity';
import { notify } from '@/stores/notice';

const route = useRoute();
const router = useRouter();

const form = reactive({ username: 'lin', password: '123456' });
const errorText = ref('');
const busy = ref(false);

const FLOW_STEPS = [
  { title: '消费者发起申请', desc: '选择本人的订单商品，填写原因、数量与凭证' },
  { title: '客服审核', desc: '确认售后期限与数量，同意退回、要求补材料或拒绝' },
  { title: '仓库收货验货', desc: '登记退回包裹，录入验货结论；异常必须写原因' },
  { title: '客服确认并关闭', desc: '换货记录补发单号，退货记录模拟退款，全程留时间线' }
];

function fill(account) {
  form.username = account.username;
  form.password = '123456';
  errorText.value = '';
}

async function submit() {
  errorText.value = '';
  if (!form.username.trim() || !form.password) {
    errorText.value = '请输入账号和密码';
    return;
  }
  if (busy.value) {
    return;
  }

  busy.value = true;
  const result = await login(form.username.trim(), form.password);
  busy.value = false;

  if (!result.ok) {
    // 失败时保留输入，只显示后端原因
    errorText.value = result.message || `登录失败（HTTP ${result.status}）`;
    return;
  }

  notify.success(`欢迎，${result.data.user.displayName}`);
  // 只有「被守卫拦下来的具体页面」才回跳；根路径与登录页一律按岗位进工作台
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '';
  const useRedirect = redirect && redirect !== '/' && redirect !== '/login';
  router.replace(useRedirect ? redirect : { name: homeRouteFor(result.data.user) });
}

onMounted(async () => {
  await hydrate();
  if (auth.user) {
    router.replace({ name: homeRouteFor(auth.user) });
  }
});
</script>

<template>
  <section class="login-shell">
    <aside class="login-brand">
      <p class="eyebrow">FASHION AFTER-SALES</p>
      <h1>登录售后协作系统</h1>
      <p class="brand-desc">
        退货、换货由<strong>消费者、售后客服、仓库人员</strong>三个岗位接力完成。
        系统按账号识别岗位，登录后只看到自己该处理的队列。
      </p>

      <ol class="flow-steps">
        <li v-for="(step, index) in FLOW_STEPS" :key="step.title">
          <span class="flow-index">{{ index + 1 }}</span>
          <div>
            <p class="flow-title">{{ step.title }}</p>
            <p class="flow-desc">{{ step.desc }}</p>
          </div>
        </li>
      </ol>

      <p class="brand-footnote">
        每次状态变化都会写入操作岗位、时间和说明，售后单详情里可以逐条追溯。
      </p>
    </aside>

    <section class="panel login-panel">
      <div class="panel-heading">
        <div>
          <p class="panel-eyebrow">ACCOUNT</p>
          <h2>账号登录</h2>
        </div>
        <span class="count-badge">{{ store.demoAccounts.length }} 个演示账号</span>
      </div>

      <form class="login-form" novalidate @submit.prevent="submit">
        <label class="form-label">
          <span class="label-text">账号</span>
          <input v-model="form.username" type="text" autocomplete="username" placeholder="例如 lin / kefu_wang" />
        </label>

        <label class="form-label">
          <span class="label-text">密码</span>
          <input v-model="form.password" type="password" autocomplete="current-password" placeholder="演示密码 123456" />
        </label>

        <p v-if="errorText" class="form-error">{{ errorText }}</p>

        <button type="submit" class="primary-button login-submit" :disabled="busy">
          <span class="btn-text">登录并进入工作台</span>
          <span v-if="busy" class="btn-spinner" style="display:inline-block"></span>
        </button>
      </form>

      <div class="login-divider"><span>或选择演示账号（密码 123456）</span></div>

      <div class="account-grid">
        <button
          v-for="account in store.demoAccounts"
          :key="account.username"
          type="button"
          class="account-card"
          @click="fill(account)"
        >
          <span class="account-name">{{ account.displayName }}</span>
          <span class="chip chip-info">{{ roleLabel(account.role) }}</span>
          <span class="account-username">{{ account.username }}</span>
        </button>
      </div>

      <p class="form-footnote login-hint">
        登录后拿到令牌，之后每个请求带 <code>Authorization: Bearer &lt;token&gt;</code>；
        刷新页面会先用令牌换回账号信息。
      </p>
    </section>
  </section>
</template>
