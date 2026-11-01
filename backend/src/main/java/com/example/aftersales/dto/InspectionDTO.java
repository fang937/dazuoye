package com.example.aftersales.dto;

import com.example.aftersales.enums.InspectionResult;
import lombok.Data;

/**
 * 仓库登记验货结果入参。
 *
 * <p>PASSED 只需给结论；ABNORMAL 必须写清楚异常原因，
 * 因为这条原因会直接决定客服后续是拒绝还是有条件放行。</p>
 */
@Data
public class InspectionDTO {

    /** PASSED 或 ABNORMAL。 */
    private InspectionResult result;

    private String abnormalReason;
}
