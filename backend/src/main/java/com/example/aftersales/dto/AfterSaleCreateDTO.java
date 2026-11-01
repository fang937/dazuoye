package com.example.aftersales.dto;

import com.example.aftersales.entity.AfterSaleEntity;
import com.example.aftersales.enums.AfterSaleReason;
import com.example.aftersales.enums.AfterSaleStatus;
import com.example.aftersales.enums.AfterSaleType;
import lombok.Data;

/**
 * 消费者发起售后申请的入参。
 *
 * <p>为什么不直接用 AfterSaleEntity 接请求：entity 里有 id、afterSaleNo、status、closedAt，
 * 前端如果能提交这些字段，就能伪造单号或把单据直接标成已完成。DTO 只暴露业务允许填写的字段，
 * 单号和初始状态由 Service 生成。</p>
 */
@Data
public class AfterSaleCreateDTO {

    /** 针对哪一行订单商品。 */
    private Long orderItemId;

    private AfterSaleType type;

    private AfterSaleReason reasonCode;

    /** 申请数量，必须落在 1 到该商品行剩余可售后数量之间。 */
    private Integer quantity;

    private String description;

    /** 图片凭证地址，模拟输入，不上传真实文件。 */
    private String evidenceUrl;

    /**
     * DTO 转 Entity：只复制用户能填的字段，id 留给数据库自增，
     * 单号和初始状态由 Service 填。
     */
    public AfterSaleEntity toEntity() {
        AfterSaleEntity entity = new AfterSaleEntity();
        entity.setOrderItemId(this.orderItemId);
        entity.setType(this.type);
        entity.setReasonCode(this.reasonCode);
        entity.setQuantity(this.quantity);
        entity.setDescription(this.description == null ? "" : this.description.trim());
        entity.setEvidenceUrl(this.evidenceUrl == null ? "" : this.evidenceUrl.trim());
        entity.setStatus(AfterSaleStatus.PENDING_REVIEW);
        return entity;
    }
}
