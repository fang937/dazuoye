package com.example.aftersales.service;

import com.example.aftersales.vo.UserView;

import java.util.List;

/**
 * 登录与演示账号服务。
 *
 * <p>本项目不做完整认证体系（没有 Spring Security、没有刷新令牌、没有权限注解），
 * 用「登录换令牌 + 拦截器解令牌」把「你是谁」这件事讲清楚，
 * 角色判断仍然落在 Service 的业务规则里。</p>
 */
public interface AuthService {

    /** 登录成功返回令牌，失败抛 401。 */
    LoginResult login(String username, String password);

    /** 用令牌换当前账号；令牌无效或过期返回 null。 */
    UserView findByToken(String token);

    /** 退出登录，作废令牌。 */
    void logout(String token);

    /** 登录页展示的演示账号（不含密码）。 */
    List<UserView> listDemoAccounts();

    /**
     * @param token 登录令牌
     * @param user  当前账号
     */
    record LoginResult(String token, UserView user) {
    }
}
