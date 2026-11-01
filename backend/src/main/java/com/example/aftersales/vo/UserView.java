package com.example.aftersales.vo;

import com.example.aftersales.entity.UserEntity;
import com.example.aftersales.enums.UserRole;

/**
 * 登录后返回给前端的账号信息。
 *
 * <p>只包含页面需要展示的字段，password 不在其中；
 * 同时把岗位的中文标签一并给出，前端不用再查字典就能说明「你是谁、能做什么」。</p>
 */
public record UserView(Long id, String username, String displayName, UserRole role, String roleLabel) {

    public static UserView of(UserEntity entity) {
        return new UserView(entity.getId(), entity.getUsername(), entity.getDisplayName(),
                entity.getRole(), entity.getRole().getLabel());
    }
}
