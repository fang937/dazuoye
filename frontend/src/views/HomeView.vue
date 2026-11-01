<script setup>
/**
 * 首页：项目定位、链路说明、三个岗位入口、越权与异常自检。
 * 自检按钮直接调用接口并把 HTTP 状态码显示出来，是答辩时的「证据页」。
 */
import { onMounted, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { api } from '@/api/client';
import { auth } from '@/stores/auth';
import { hydrate, store } from '@/stores/identity';

const heroEntries = [
  { name: 'consumer', tag: 'CONSUMER', title: '消费者工作台', desc: '查看本人的模拟订单商品、发起退货或换货、补充凭证、跟踪处理进度。', accounts: '演示账号：小林 lin / 小红 hong' },
  { name: 'cs', tag: 'CS', title: '售后客服工作台', desc: '审核申请条件、要求补充材料、同意退回或拒绝，并在验货后确认换货与模拟退款。', accounts: '演示账号：王敏 kefu_wang' },
  { name: 'warehouse', tag: 'WAREHOUSE', title: '仓库工作台', desc: '登记收到退回的包裹、录入验货结果与异常原因；收货任务只在客服同意退回后出现。', accounts: '演示账号：李强 wh_li' }
];

const statusText = ref('正在检测后端连接...');
const statusClass = ref('status-badge status-checking');
const selfTest = ref({ ran: false, ok: false, status: 0, method: '', path: '', message: '' });

async function checkHealth() {
  const result = await api.health();
  if (result.ok) {
    statusClass.value = 'status-badge status-online';
    statusText.value = `后端已就绪，当前账号：${(auth.user || {}).displayName || ''}`;
  } else {
    statusClass.value = 'status-badge status-offline';
    statusText.value = result.message || '后端未连接，请先启动 Spring Boot';
  }
}

function fill(method, path, result) {
  selfTest.value = {
    ran: true,
    ok: result.ok,
    status: result.status,
    method,
    path,
    message: result.message
      || (Array.isArray(result.data) ? `返回 ${result.data.length} 条记录` : '无提示信息')
  };
}

/** 找一个不属于当前消费者的售后单，请求它应当被 403 拦下。 */
async function testOtherPeopleOrder() {
  const user = auth.user;
  if (!user || user.role !== 'CONSUMER') {
    selfTest.value = {
      ran: true,
      ok: true,
      status: 200,
      method: '说明',
      path: '',
      message: '当前岗位本来就允许查看全部单据。把身份切成小林或小红再点这个按钮，才会触发 403。'
    };
    return;
  }

  const mine = await api.myAfterSales();
  const mineIds = mine.ok ? mine.data.map(view => view.id) : [];
  for (let id = 1; id <= 12; id++) {
    if (mineIds.includes(id)) continue;
    const result = await api.detail(id);
    if (result.status === 403) {
      fill('GET', `/after-sales/${id}`, result);
      return;
    }
  }
  selfTest.value = { ran: true, ok: false, status: 0, method: '说明', path: '', message: '演示库里暂时找不到别人的单据，重新执行 init.sql 即可补足数据。' };
}

async function testCsOnlyApi() {
  fill('GET', '/after-sales/all', await api.allAfterSales());
}

/** 拿一张「待客服审核」的单，用当前身份直接调仓库收货接口：客服没同意，仓库不能提前收货。 */
async function testWrongTransition() {
  let targetId = null;
  for (let id = 1; id <= 12; id++) {
    const probe = await api.detail(id);
    if (probe.ok && probe.data.status === 'PENDING_REVIEW') {
      targetId = id;
      break;
    }
  }
  if (targetId === null) {
    selfTest.value = { ran: true, ok: false, status: 0, method: '说明', path: '', message: '演示数据里暂时没有「待客服审核」的单据，重新执行 init.sql 可恢复。' };
    return;
  }
  fill('POST', `/after-sales/${targetId}/receive`, await api.receive(targetId, { note: '自检：跳过客服审核直接收货' }));
}

async function init() {
  await hydrate();
  await checkHealth();
}

onMounted(init);
</script>

<template>
  <header class="hero">
    <div class="hero-left">
      <p class="eyebrow">FASHION AFTER-SALES · 贯穿式大作业 常规赛道</p>
      <h1>服装网店退换货处理系统</h1>
      <p class="hero-description">
        模拟一家服装网店的售后部门：<strong>消费者、售后客服、仓库人员</strong>共同处理一次退货或换货。
        每个人只看到自己该处理的队列，每一步状态变化都保留岗位、时间和说明。
      </p>
      <div class="flow-note">
        <span>申请售后</span><b>→</b><span>客服审核</span><b>→</b><span>仓库收货验货</span><b>→</b><span>客服确认换货/退款</span><b>→</b><span>售后关闭</span>
      </div>
    </div>
    <div class="hero-right">
      <div :class="statusClass">
        <span class="status-dot"></span>
        <span>{{ statusText }}</span>
        <button class="badge-action-btn" @click="init">重试</button>
      </div>
    </div>
  </header>

  <section class="panel">
    <div class="panel-heading">
      <div>
        <p class="panel-eyebrow">ENTRY · 三个岗位各自一页</p>
        <h2>选择岗位进入工作台</h2>
      </div>
      <span class="count-badge">3 个岗位</span>
    </div>
    <div class="entry-grid">
      <RouterLink v-for="entry in heroEntries" :key="entry.name" class="entry-card" :to="{ name: entry.name }">
        <p class="entry-tag">{{ entry.tag }}</p>
        <h3>{{ entry.title }}</h3>
        <p class="entry-desc">{{ entry.desc }}</p>
        <p class="entry-meta">{{ entry.accounts }}</p>
      </RouterLink>
    </div>
  </section>

  <section class="panel selftest-panel">
    <div class="panel-heading">
      <div>
        <p class="panel-eyebrow">SELF-TEST · 验收对照</p>
        <h2>越权与异常自检</h2>
      </div>
    </div>
    <div class="selftest-buttons">
      <button class="secondary-button compact" @click="testOtherPeopleOrder">用当前身份打开别人的售后单（期望 403）</button>
      <button class="secondary-button compact" @click="testCsOnlyApi">用当前身份请求客服全量列表（期望 403）</button>
      <button class="secondary-button compact" @click="testWrongTransition">用当前身份做一件不该做的事（期望 400 / 409）</button>
    </div>

    <div class="selftest-result">
      <template v-if="!selfTest.ran">
        点击任一按钮，页面会把后端返回的 HTTP 状态码和中文原因原样显示出来——
        这就是答辩时「一次校验在页面、接口、数据库中的对应证据」。
      </template>
      <template v-else>
        <p class="selftest-line">
          <span class="chip" :class="selfTest.ok ? 'chip-success' : 'chip-danger'">HTTP {{ selfTest.status }}</span>
          <code>{{ selfTest.method }} {{ selfTest.path }}</code>
        </p>
        <p class="selftest-message">{{ selfTest.message }}</p>
        <p class="selftest-tip">
          {{ selfTest.ok
            ? '这次请求被允许。换成另一个岗位身份再点一次，就能看到 403。'
            : '拦截发生在后端 Service，页面只负责把原因显示出来。数据库侧证据可在 after_sale.status 与 after_sale_event 中查看。' }}
        </p>
      </template>
    </div>
  </section>
</template>
