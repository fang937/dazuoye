package com.example.aftersales.mapper;

import com.example.aftersales.entity.AfterSaleEntity;
import com.example.aftersales.vo.AfterSaleView;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 售后单数据访问接口：只负责「怎么读写 after_sale」，不判断业务规则。
 *
 * <p>状态相关的入参统一传字符串（枚举 name()），避免动态 SQL 里的类型转换歧义；
 * Java 侧仍然是枚举，规则判断集中在 Service。</p>
 */
@Mapper
public interface AfterSaleMapper {

    int insert(AfterSaleEntity afterSale);

    AfterSaleEntity findById(@Param("id") Long id);

    /** 列表接口用：售后单 + 商品行 + 下单人，一次连接取全。 */
    AfterSaleView findViewById(@Param("id") Long id);

    List<AfterSaleView> listViewsByConsumer(@Param("consumerId") Long consumerId);

    /**
     * 按状态集合查队列，供客服工作台和仓库工作台使用。
     *
     * @param statuses AfterSaleStatus 的 name 列表
     */
    List<AfterSaleView> listViewsByStatuses(@Param("statuses") List<String> statuses);

    List<AfterSaleView> listAllViews();

    /** 状态机唯一入口：改状态，必要时写入关闭时间。 */
    int updateStatus(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("closedAt") LocalDateTime closedAt);

    /** 消费者补充凭证时更新凭证与说明。 */
    int updateEvidence(@Param("id") Long id,
                       @Param("evidenceUrl") String evidenceUrl,
                       @Param("description") String description);

    /** 该行商品已被占用（未被拒绝）的售后数量，用于「申请数量不能超过可售后数量」。 */
    int sumOccupiedQuantity(@Param("orderItemId") Long orderItemId);

    /** 该行商品正在进行中的售后单，用于拦截重复申请。 */
    AfterSaleEntity findInProgressByItemId(@Param("orderItemId") Long orderItemId);
}
