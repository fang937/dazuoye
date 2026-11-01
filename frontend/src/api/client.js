/**
 * 接口层：所有对后端的请求都从这里出去（底层 Axios）。
 *
 * 四件事集中在这里：
 * 1. 用请求拦截器带上登录令牌 Authorization: Bearer <token>；
 * 2. 用响应拦截器把 HTTP 状态码和后端返回的中文原因整理成统一结构
 *    { ok, status, data, message }，页面因此能区分「成功 / 未登录 401 /
 *    越权 403 / 规则拦截 400 或 409 / 后端不可达 0」；
 * 3. 收到 401 时清掉本地令牌并跳回登录页，避免页面停在一个永远失败的界面上；
 * 4. 每个方法与 README 第 5 节的接口清单一一对应。
 *
 * 开发模式：Vite 把 /api 代理到 8082；交付模式：构建产物由后端同源托管，
 * 两种情况下 baseURL 都只写相对路径 /api。
 */
import axios from 'axios';

const TOKEN_KEY = 'aftersales.token';

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || '';
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY);
}

export const http = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: { Accept: 'application/json' }
});

// 请求拦截器：登录令牌是「每次请求都要带」的东西，放这里就不用每个方法各写一遍
http.interceptors.request.use(config => {
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

/**
 * 把 Axios 的两种结果（成功响应 / 抛出的错误）收敛成同一个结构，
 * 页面只判断 ok 和 status，不需要认识 AxiosError。
 */
async function toResult(promise) {
  try {
    const response = await promise;
    return { ok: true, status: response.status, data: response.data ?? null, message: null };
  } catch (error) {
    // 没有 response：DNS、连接被拒、超时（开发时后端没启动最常见）
    if (!error.response) {
      if (error.code === 'ECONNABORTED') {
        return { ok: false, status: 0, data: null, message: '请求超时：后端 10 秒内没有响应，请确认服务是否卡住' };
      }
      return {
        ok: false,
        status: 0,
        data: null,
        message: '连不上后端：请确认 Spring Boot 已启动，并且后端是在 init.sql 建库之后启动的（先建库再启动）'
      };
    }

    const response = error.response;
    const data = response.data && typeof response.data === 'object' ? response.data : null;
    let message = data && data.message ? data.message : null;

    if (response.status === 401) {
      // 只清「发出这个请求时用的那个令牌」：
      // 退出后马上换账号登录时，旧令牌的在途请求可能晚一步返回 401，
      // 如果无条件清空会把刚拿到的新令牌一起清掉。
      const usedToken = String((error.config && error.config.headers && error.config.headers.Authorization) || '')
        .replace('Bearer ', '')
        .trim();
      if (usedToken && usedToken === getToken()) {
        clearToken();
        if (!window.location.hash.startsWith('#/login')) {
          window.location.hash = '#/login';
        }
      }
    }

    // 后端的失败响应一定带 message；没带 message 的非 2xx 多半是链路问题
    // （例如开发模式下 Vite 代理找不到 8082，会直接回一个 500 空响应）
    if (!message) {
      message = response.status === 500 || response.status === 502
        ? '后端没有响应：请确认 Spring Boot 已启动，并且是在 init.sql 建库之后启动的（开发模式下 Vite 会把 /api 代理到 8082）'
        : `请求失败（HTTP ${response.status}）`;
    }

    return { ok: false, status: response.status, data, message };
  }
}

export const api = {
  // ---- 登录与公开信息 ----
  login: (username, password) => toResult(http.post('/auth/login', { username, password })),
  logout: () => toResult(http.post('/auth/logout')),
  me: () => toResult(http.get('/auth/me')),
  health: () => toResult(http.get('/health')),
  demoUsers: () => toResult(http.get('/demo/users')),
  meta: () => toResult(http.get('/demo/meta')),

  // ---- 消费者 ----
  myOrderItems: () => toResult(http.get('/orders/my-items')),
  myAfterSales: () => toResult(http.get('/after-sales/mine')),
  createAfterSale: payload => toResult(http.post('/after-sales', payload)),
  supplementEvidence: (id, payload) => toResult(http.post(`/after-sales/${id}/evidence`, payload)),

  // ---- 售后客服 ----
  csQueue: () => toResult(http.get('/after-sales/cs-queue')),
  allAfterSales: () => toResult(http.get('/after-sales/all')),
  review: (id, payload) => toResult(http.post(`/after-sales/${id}/review`, payload)),
  finalize: (id, payload) => toResult(http.post(`/after-sales/${id}/finalize`, payload)),

  // ---- 仓库人员 ----
  warehouseQueue: () => toResult(http.get('/after-sales/warehouse-queue')),
  receive: (id, payload) => toResult(http.post(`/after-sales/${id}/receive`, payload)),
  inspect: (id, payload) => toResult(http.post(`/after-sales/${id}/inspect`, payload)),

  // ---- 共用 ----
  detail: id => toResult(http.get(`/after-sales/${id}`))
};
