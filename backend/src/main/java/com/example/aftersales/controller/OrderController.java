package com.example.aftersales.controller;

import com.example.aftersales.service.OrderQueryService;
import com.example.aftersales.vo.OrderItemCandidateVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 模拟订单接口。
 *
 * <p>系统不开发商品浏览、购物车和真实下单，只提供「本人的已完成订单及其商品行」，
 * 因为发起售后必须先确认这是消费者自己的订单、且仍在售后期限内。</p>
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderQueryService orderQueryService;

    public OrderController(OrderQueryService orderQueryService) {
        this.orderQueryService = orderQueryService;
    }

    /**
     * GET /api/orders/my-items
     *
     * <p>返回当前消费者所有已完成订单的商品行，附带剩余可售后数量、截止日期、
     * 能否申请以及不能申请的原因。</p>
     */
    @GetMapping("/my-items")
    public List<OrderItemCandidateVO> myItems() {
        return orderQueryService.listMyCandidateItems();
    }
}
