package com.example.aftersales.mapper;

import com.example.aftersales.entity.WarehouseRecordEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 仓库记录数据访问接口。
 *
 * <p>写操作拆成三段，正好对应三个业务事实：登记收货、录验货结论、客服终定。
 * 每一段只由对应岗位调用，谁在什么时候动了哪个字段一目了然。</p>
 */
@Mapper
public interface WarehouseRecordMapper {

    WarehouseRecordEntity findByAfterSaleId(@Param("afterSaleId") Long afterSaleId);

    /** 仓库登记收货时创建这条记录，之后验货和终定都在同一条记录上更新。 */
    int insertReceived(@Param("afterSaleId") Long afterSaleId,
                       @Param("receivedBy") Long receivedBy,
                       @Param("receivedAt") LocalDateTime receivedAt);

    int updateInspection(@Param("afterSaleId") Long afterSaleId,
                         @Param("result") String result,
                         @Param("inspectorId") Long inspectorId,
                         @Param("inspectedAt") LocalDateTime inspectedAt,
                         @Param("abnormalReason") String abnormalReason);

    int updateFinalization(@Param("afterSaleId") Long afterSaleId,
                           @Param("reissueNo") String reissueNo,
                           @Param("refundAmount") BigDecimal refundAmount,
                           @Param("refundResult") String refundResult,
                           @Param("operatorId") Long operatorId,
                           @Param("finalizedAt") LocalDateTime finalizedAt,
                           @Param("finalNote") String finalNote);
}
