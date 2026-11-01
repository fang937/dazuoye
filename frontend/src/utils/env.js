/**
 * 接口基地址说明：开发模式由 Vite 代理 /api 到后端 8082，
 * 构建产物由 Spring Boot 同源托管，所以页面里始终只写相对路径 /api。
 * 这个常量只用于页脚展示，方便答辩时说清请求发到哪里。
 */
export const API_BASE_LABEL = '/api（开发：Vite 代理到 8082；交付：后端同源托管）';
