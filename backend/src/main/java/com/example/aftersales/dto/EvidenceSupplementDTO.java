package com.example.aftersales.dto;

import lombok.Data;

/**
 * 消费者补充凭证入参，只有被客服退回（NEED_EVIDENCE）时才允许提交。
 */
@Data
public class EvidenceSupplementDTO {

    private String evidenceUrl;

    /** 补充说明，会一并更新到售后单的说明字段。 */
    private String description;
}
