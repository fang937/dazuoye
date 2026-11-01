package com.example.aftersales.entity;

import com.example.aftersales.enums.UserRole;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 账号实体，对应 sys_user 表。
 *
 * <p>password 只用于登录校验，绝不出现在任何返回给前端的对象里——
 * 对外一律用 {@link com.example.aftersales.vo.UserView}。
 * 演示项目为了能一键复现，密码是明文存储并在 README 中公开；
 * 真实系统必须换成 BCrypt 等慢哈希，这一点写在已知限制里。</p>
 */
@Data
public class UserEntity {

    private Long id;
    private String username;
    private String password;
    private String displayName;
    private UserRole role;
    private LocalDateTime createdAt;
}
