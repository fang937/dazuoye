package com.example.aftersales.auth;

import com.example.aftersales.enums.UserRole;

/**
 * 当前请求的操作人身份。
 *
 * <p>登录接口校验账号密码后发放令牌，之后每个请求带
 * {@code Authorization: Bearer <token>}，拦截器把它换成这个对象。</p>
 *
 * @param id          sys_user 主键，用于判断单据归属
 * @param username    演示账号
 * @param displayName 工作台上显示的人名
 * @param role        岗位，权限判断的依据
 */
public record CurrentUser(Long id, String username, String displayName, UserRole role) {

    public boolean isConsumer() {
        return role == UserRole.CONSUMER;
    }

    /**
     * 生成写入时间线的操作人快照。
     */
    public String roleLabel() {
        return role.getLabel();
    }
}
