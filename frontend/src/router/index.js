import { createRouter, createWebHashHistory } from 'vue-router';

import HomeView from '@/views/HomeView.vue';
import LoginView from '@/views/LoginView.vue';
import ConsumerView from '@/views/ConsumerView.vue';
import ApplyView from '@/views/ApplyView.vue';
import CsView from '@/views/CsView.vue';
import WarehouseView from '@/views/WarehouseView.vue';
import DetailView from '@/views/DetailView.vue';
import { auth, ensureSession, homeRouteFor } from '@/stores/auth';

/**
 * 路由用 hash 模式（地址形如 /#/detail?id=6）。
 * 原因：构建产物由 Spring Boot 当静态文件托管，刷新 /detail 这种真实路径后端没有对应
 * 静态文件会 404；hash 模式把路由放在 # 后面，任何刷新都由前端自己接管，不需要后端做转发。
 *
 * meta.role 标注这一页属于哪个岗位：导航栏据此只显示当前账号能用的入口，
 * 但直接访问别人的页面不会被前端硬拦，而是由后端 403 给出原因（便于演示越权拦截）。
 */
const routes = [
  { path: '/login', name: 'login', component: LoginView, meta: { title: '登录', role: null, nav: false } },
  { path: '/', name: 'home', component: HomeView, meta: { title: '项目首页', role: null, nav: true } },
  { path: '/consumer', name: 'consumer', component: ConsumerView, meta: { title: '消费者工作台', role: 'CONSUMER', nav: true } },
  { path: '/apply', name: 'apply', component: ApplyView, meta: { title: '发起售后申请', role: 'CONSUMER', nav: false } },
  { path: '/cs', name: 'cs', component: CsView, meta: { title: '售后客服工作台', role: 'CS', nav: true } },
  { path: '/warehouse', name: 'warehouse', component: WarehouseView, meta: { title: '仓库工作台', role: 'WAREHOUSE', nav: true } },
  { path: '/detail', name: 'detail', component: DetailView, meta: { title: '售后单详情', role: null, nav: false } },
  { path: '/:pathMatch(.*)*', redirect: '/' }
];

const router = createRouter({
  history: createWebHashHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
});

/**
 * 登录守卫：没登录只能待在登录页，登录后访问登录页会被送回本岗位工作台。
 * 这里只判断「有没有登录」，不判断「这个岗位能不能看这一页」——
 * 后者是业务规则，由后端 Service 负责（前端硬拦反而看不出越权被拒绝的过程）。
 */
router.beforeEach(async to => {
  await ensureSession();

  if (to.name === 'login') {
    return auth.user ? { name: homeRouteFor(auth.user) } : true;
  }
  if (!auth.user) {
    return { name: 'login', query: { redirect: to.fullPath } };
  }
  return true;
});

export default router;
