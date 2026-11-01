package com.example.aftersales.auth;

import com.example.aftersales.service.AuthService;
import com.example.aftersales.vo.UserView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录令牌拦截器。
 *
 * <p>把 {@code Authorization: Bearer <token>} 换成当前账号放进 {@link AuthContext}。
 * 令牌缺失、伪造或过期时直接返回 401，不进入 Controller——
 * 前端收到 401 会清掉本地 token 并跳回登录页。</p>
 *
 * <p>公开接口（健康检查、演示账号、登录、枚举字典）不要求令牌。</p>
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;

    public AuthInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 浏览器跨域预检不带自定义头，交给 CORS 处理
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            writeUnauthorized(response, "未登录或登录已失效，请重新登录");
            return false;
        }

        UserView user = authService.findByToken(authorization.substring(BEARER_PREFIX.length()).trim());
        if (user == null) {
            writeUnauthorized(response, "登录已失效，请重新登录");
            return false;
        }

        AuthContext.set(new CurrentUser(user.id(), user.username(), user.displayName(), user.role()));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"message\":\"" + message.replace("\"", "'") + "\",\"status\":\"401\"}");
    }
}
