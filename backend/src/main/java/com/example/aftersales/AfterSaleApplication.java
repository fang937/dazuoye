package com.example.aftersales;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 后端启动类。
 *
 * <p>Spring Boot 从这里启动内置 Web 服务器；MapperScan 自动把 mapper 包中的接口
 * 交给 MyBatis 管理，运行时生成代理对象，不需要手写 MapperImpl。</p>
 */
@SpringBootApplication
@MapperScan("com.example.aftersales.mapper")
public class AfterSaleApplication {

    public static void main(String[] args) {
        SpringApplication.run(AfterSaleApplication.class, args);
    }
}
