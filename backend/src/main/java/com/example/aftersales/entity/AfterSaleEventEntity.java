package com.example.aftersales.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 售后时间线事件实体，对应 after_sale_event 表。
 *
 * <p>关键业务规则第 6 条「每次状态变化都保留操作岗位、时间和说明」由这张表落实。
 * operatorName 是有意冗余存下来的快照：演示账号以后改名或删号，历史记录仍要说得清
 * 当时是谁做的。</p>
 */
@Data
public class AfterSaleEventEntity {

    private Long id;
    private Long afterSaleId;
    private Long operatorId;
    /** 操作岗位，例如 售后客服。 */
    private String operatorRole;
    private String operatorName;
    /** 动作名称，例如「同意退回并给出售后指引」。 */
    private String action;
    private String fromStatus;
    private String toStatus;
    private String note;
    private LocalDateTime createdAt;
}
