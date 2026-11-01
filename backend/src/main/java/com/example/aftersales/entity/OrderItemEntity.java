package com.example.aftersales.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 订单商品行实体，对应 order_item 表。
 *
 * <p>售后申请针对的是「某一行商品的若干件」，不是整张订单，
 * 所以可售后数量和售后期限都挂在这一层。</p>
 */
@Data
public class OrderItemEntity {

    private Long id;
    private Long orderId;
    private String productName;
    /** 颜色/尺码等规格。 */
    private String spec;
    private BigDecimal unitPrice;
    private Integer quantity;
    /** 售后申请截止日期，超过就不能发起。 */
    private LocalDate afterSaleDeadline;
}
