package com.example.aftersales.dto;

import com.example.aftersales.enums.AfterSaleAction;
import lombok.Data;

/**
 * 客服审核阶段的动作入参。
 *
 * <p>要求补充凭证和拒绝都必须填写原因（关键业务规则第 5 条），
 * 是否为空由 Service 判断，页面只做轻提醒。</p>
 */
@Data
public class ReviewDecisionDTO {

    /** REQUEST_EVIDENCE / APPROVE_RETURN / REJECT 三选一。 */
    private AfterSaleAction action;

    /** 审核说明；要求补充和拒绝时必填。 */
    private String reason;
}
