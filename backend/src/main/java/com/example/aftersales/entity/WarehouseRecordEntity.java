package com.example.aftersales.entity;

import com.example.aftersales.enums.InspectionResult;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 仓库记录实体，对应 warehouse_record 表，与售后单一对一。
 *
 * <p>把它单独建表正是本项目想说明的业务事实：
 * 「仓库收到货」「验货有没有问题」「换货补发单号」「模拟退款金额」是四件不同的事，
 * 发生时间不同、责任岗位也不同（前两件属于仓库，后两件由客服确认）。</p>
 */
@Data
public class WarehouseRecordEntity {

    private Long id;
    private Long afterSaleId;

    // ---- 仓库岗位写入 ----
    private Long receivedBy;
    private LocalDateTime receivedAt;
    private InspectionResult inspectionResult;
    private Long inspectorId;
    private LocalDateTime inspectedAt;
    private String abnormalReason;

    // ---- 客服终定写入 ----
    /** 换货时记录的模拟补发单号。 */
    private String reissueNo;
    /** 模拟退款金额，不接真实资金接口。 */
    private BigDecimal refundAmount;
    /** 模拟退款结果，例如 MOCK_SUCCESS。 */
    private String refundResult;
    private Long operatorId;
    private LocalDateTime finalizedAt;
    private String finalNote;
}
