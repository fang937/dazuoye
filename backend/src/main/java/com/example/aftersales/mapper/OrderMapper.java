package com.example.aftersales.mapper;

import com.example.aftersales.entity.MockOrderEntity;
import com.example.aftersales.entity.OrderItemEntity;
import com.example.aftersales.vo.OrderItemCandidateVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 模拟订单与订单商品行的数据访问接口。
 */
@Mapper
public interface OrderMapper {

    /**
     * 查某个消费者所有已完成订单下的商品行，并带上已被售后占用的数量。
     * 这是消费者工作台「可以申请售后的商品」列表的数据来源。
     */
    List<OrderItemCandidateVO> findCandidateItems(@Param("consumerId") Long consumerId);

    OrderItemEntity findItemById(@Param("id") Long id);

    MockOrderEntity findOrderById(@Param("id") Long id);
}
