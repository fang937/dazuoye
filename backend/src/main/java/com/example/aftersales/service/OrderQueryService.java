package com.example.aftersales.service;

import com.example.aftersales.vo.OrderItemCandidateVO;

import java.util.List;

/**
 * 模拟订单查询服务。
 */
public interface OrderQueryService {

    /**
     * 当前消费者可以发起售后的订单商品行。
     *
     * <p>返回结果里已经带上「剩余可售后数量」「能否申请」「不能申请的原因」，
     * 页面只负责显示，规则判断不放到浏览器里做。</p>
     */
    List<OrderItemCandidateVO> listMyCandidateItems();
}
