package com.example.aftersales.auth;

import com.example.aftersales.exception.ApiException;

/**
 * 用 ThreadLocal 保存「这一次请求是谁发起的」。
 *
 * <p>这样 Controller 的方法签名不必每个都加身份参数，Service 内部随手就能拿到操作人。
 * 代价是必须记得在请求结束时清理，否则线程复用会串号——清理逻辑写在拦截器的
 * afterCompletion 里。</p>
 */
public final class AuthContext {

    private static final ThreadLocal<CurrentUser> CURRENT = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void set(CurrentUser user) {
        CURRENT.set(user);
    }

    public static CurrentUser getOrNull() {
        return CURRENT.get();
    }

    /**
     * 取当前操作人，没登录就直接报错。
     *
     * @return 当前登录账号
     * @throws ApiException 401：请求没带有效令牌
     */
    public static CurrentUser require() {
        CurrentUser user = CURRENT.get();
        if (user == null) {
            throw ApiException.unauthorized("未登录或登录已失效，请重新登录");
        }
        return user;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
