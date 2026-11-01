package com.example.aftersales.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 「可发起售后的订单商品行」查询结果。
 *
 * <p>SQL 负责把原始事实取出来（购买数量、已被占用的售后数量、进行中的售后单号、截止日期），
 * 判断能不能申请、为什么不能申请则交给 Service 计算。这样页面拿到的是结论，
 * 不用自己去猜业务规则。</p>
 */
@Data
public class OrderItemCandidateVO {

    // ---- 订单信息 ----
    private Long orderId;
    private String orderNo;
    private String orderStatus;
    private LocalDateTime completedAt;

    // ---- 商品行信息 ----
    private Long orderItemId;
    private String productName;
    private String spec;
    private BigDecimal unitPrice;
    private Integer quantity;
    private LocalDate afterSaleDeadline;

    // ---- SQL 聚合出来的事实 ----
    /** 已被售后单占用的数量（被拒绝的申请不占用）。 */
    private Integer occupiedQuantity;
    /** 该行商品正在进行中的售后单号，没有则为空。 */
    private String inProgressAfterSaleNo;

    // ---- Service 计算的结论 ----
    /** 剩余可售后数量 = quantity - occupiedQuantity。 */
    private Integer remainingQuantity;
    private Boolean canApply;
    /** 不能申请时的原因，用于页面直接展示。 */
    private String blockReason;
}
