package com.example.aftersales.controller;

import com.example.aftersales.auth.AuthContext;
import com.example.aftersales.auth.CurrentUser;
import com.example.aftersales.dto.LoginRequest;
import com.example.aftersales.service.AuthService;
import com.example.aftersales.vo.UserView;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 登录相关接口。
 *
 * <p>前端登录后拿到 token，之后每个请求带 {@code Authorization: Bearer <token>}；
 * 拦截器据此把身份放进 AuthContext，Service 里的岗位校验与单据归属校验继续生效。
 * 登录页需要的演示账号仍由公开接口 /api/demo/users 提供（不含密码）。</p>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * POST /api/auth/login
     *
     * <p>成功返回 {@code {"token": "...", "user": {...}}}；失败统一 401，提示「账号或密码不正确」。</p>
     */
    @PostMapping("/login")
    public AuthService.LoginResult login(@RequestBody LoginRequest request) {
        return authService.login(request == null ? null : request.getUsername(),
                request == null ? null : request.getPassword());
    }

    /**
     * GET /api/auth/me
     *
     * <p>前端刷新页面后用本地 token 换回「我是谁」，换不到就回登录页。</p>
     */
    @GetMapping("/me")
    public UserView me() {
        CurrentUser user = AuthContext.require();
        return new UserView(user.id(), user.username(), user.displayName(), user.role(), user.roleLabel());
    }

    /**
     * POST /api/auth/logout
     *
     * <p>作废令牌。前端随后清掉本地 token 并回到登录页。</p>
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            authService.logout(authorization.substring(BEARER_PREFIX.length()).trim());
        }
        return ResponseEntity.ok(Map.of("message", "已退出登录"));
    }
}
