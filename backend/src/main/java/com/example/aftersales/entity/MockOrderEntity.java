package com.example.aftersales.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 模拟订单实体，对应 mock_order 表。
 *
 * <p>项目不开发商品浏览、购物车和真实下单支付，这张表的数据由 init.sql 直接造好，
 * 系统只读取它来判断「是不是本人已完成的订单」。</p>
 */
@Data
public class MockOrderEntity {

    private Long id;
    private String orderNo;
    private Long consumerId;
    private BigDecimal totalAmount;
    private LocalDateTime placedAt;
    /** 确认收货时间，售后期限从这天开始算。 */
    private LocalDateTime completedAt;
    private String status;
}
