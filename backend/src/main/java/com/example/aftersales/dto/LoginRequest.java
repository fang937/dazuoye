package com.example.aftersales.dto;

import lombok.Data;

/**
 * 登录入参。只放账号密码两个字段，别的一律不收。
 */
@Data
public class LoginRequest {

    private String username;
    private String password;
}
