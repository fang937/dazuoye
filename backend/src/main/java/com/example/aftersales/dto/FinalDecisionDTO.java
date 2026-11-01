package com.example.aftersales.dto;

import com.example.aftersales.enums.AfterSaleAction;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 客服终定入参：验货之后决定换货、退款，还是拒绝。
 *
 * <p>这一步才真正把售后单关闭。金额用 BigDecimal，和课堂 Demo 里
 * 「涉及钱不用 double」的约定保持一致。</p>
 */
@Data
public class FinalDecisionDTO {

    /** CONFIRM_EXCHANGE / CONFIRM_REFUND / REJECT 三选一。 */
    private AfterSaleAction action;

    /** 换货时必填：模拟补发单号。 */
    private String reissueNo;

    /** 退货时必填：模拟退款金额，不能超过该行商品的成交单价 × 申请数量。 */
    private BigDecimal refundAmount;

    /** 结论说明；验货异常后处理时必填。 */
    private String note;
}
