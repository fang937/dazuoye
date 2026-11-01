package com.example.aftersales.entity;

import com.example.aftersales.enums.AfterSaleReason;
import com.example.aftersales.enums.AfterSaleStatus;
import com.example.aftersales.enums.AfterSaleType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 售后单实体，对应 after_sale 表，是整个系统的核心单据。
 *
 * <p>{@code status} 字段就是状态机所在地：它只能沿着 Service 里允许的路线前进，
 * 每次变化都会同步写一条 after_sale_event 时间线。</p>
 */
@Data
public class AfterSaleEntity {

    private Long id;
    private String afterSaleNo;
    /** 针对哪一行订单商品。 */
    private Long orderItemId;
    /** 发起人，决定谁能看到这张单。 */
    private Long consumerId;
    private AfterSaleType type;
    private AfterSaleReason reasonCode;
    private Integer quantity;
    private String description;
    /** 图片凭证地址，模拟路径，不接真实文件存储。 */
    private String evidenceUrl;
    private AfterSaleStatus status;
    /** 进入终态的时间，未关闭为 null。 */
    private LocalDateTime closedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
