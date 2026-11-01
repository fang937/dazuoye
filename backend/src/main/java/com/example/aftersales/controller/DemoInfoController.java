package com.example.aftersales.controller;

import com.example.aftersales.enums.AfterSaleAction;
import com.example.aftersales.enums.AfterSaleReason;
import com.example.aftersales.enums.AfterSaleStatus;
import com.example.aftersales.enums.AfterSaleType;
import com.example.aftersales.enums.Labeled;
import com.example.aftersales.enums.UserRole;
import com.example.aftersales.service.AuthService;
import com.example.aftersales.vo.UserView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 公开的系统信息接口（不需要登录）。
 *
 * <p>登录页需要知道有哪些演示账号、每个账号是什么岗位；
 * 页面还需要枚举的中文标签，避免前端再抄一份中文。</p>
 */
@RestController
@RequestMapping("/api")
public class DemoInfoController {

    private final AuthService authService;

    public DemoInfoController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "message", "服装网店退换货处理系统后端已启动");
    }

    /**
     * 演示账号列表（不含密码）。登录页用它渲染「一键填入」按钮。
     */
    @GetMapping("/demo/users")
    public List<UserView> listDemoUsers() {
        return authService.listDemoAccounts();
    }

    /**
     * 枚举字典：name 是提交给后端的值，label 是显示给人的中文。
     */
    @GetMapping("/demo/meta")
    public Map<String, Object> meta() {
        return Map.of(
                "roles", labelList(UserRole.values()),
                "types", labelList(AfterSaleType.values()),
                "reasons", labelList(AfterSaleReason.values()),
                "statuses", labelList(AfterSaleStatus.values()),
                "actions", labelList(AfterSaleAction.values())
        );
    }

    private List<Map<String, String>> labelList(Labeled[] values) {
        return Arrays.stream(values)
                .map(value -> Map.of("name", value.name(), "label", value.getLabel()))
                .toList();
    }
}
