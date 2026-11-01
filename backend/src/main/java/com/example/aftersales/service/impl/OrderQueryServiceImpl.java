package com.example.aftersales.service.impl;

import com.example.aftersales.auth.AuthContext;
import com.example.aftersales.auth.CurrentUser;
import com.example.aftersales.enums.UserRole;
import com.example.aftersales.exception.ApiException;
import com.example.aftersales.mapper.OrderMapper;
import com.example.aftersales.service.OrderQueryService;
import com.example.aftersales.vo.OrderItemCandidateVO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 模拟订单查询实现。
 */
@Service
public class OrderQueryServiceImpl implements OrderQueryService {

    private final OrderMapper orderMapper;

    public OrderQueryServiceImpl(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    @Override
    public List<OrderItemCandidateVO> listMyCandidateItems() {
        CurrentUser user = AuthContext.require();
        if (!user.isConsumer()) {
            throw ApiException.forbidden("只有消费者岗位能查看自己的模拟订单商品");
        }

        List<OrderItemCandidateVO> items = orderMapper.findCandidateItems(user.id());
        items.forEach(this::applyRules);
        return items;
    }

    /**
     * 把 SQL 取出的事实换算成「能不能申请」的结论。
     *
     * <p>判断顺序按客服实际检查申请的顺序来：先看过没过期，再看是不是重复申请，
     * 最后才看数量够不够。</p>
     */
    private void applyRules(OrderItemCandidateVO item) {
        int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
        int occupied = item.getOccupiedQuantity() == null ? 0 : item.getOccupiedQuantity();
        int remaining = Math.max(quantity - occupied, 0);
        item.setRemainingQuantity(remaining);

        if (item.getAfterSaleDeadline() != null && LocalDate.now().isAfter(item.getAfterSaleDeadline())) {
            item.setCanApply(false);
            item.setBlockReason("已超过售后期限（截止 " + item.getAfterSaleDeadline() + "）");
            return;
        }
        if (item.getInProgressAfterSaleNo() != null && !item.getInProgressAfterSaleNo().isBlank()) {
            item.setCanApply(false);
            item.setBlockReason("该行商品已有进行中的售后单 " + item.getInProgressAfterSaleNo());
            return;
        }
        if (remaining <= 0) {
            item.setCanApply(false);
            item.setBlockReason("可售后数量为 0（购买 " + quantity + " 件，已申请 " + occupied + " 件）");
            return;
        }

        item.setCanApply(true);
        item.setBlockReason(null);
    }
}
