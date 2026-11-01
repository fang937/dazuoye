package com.example.aftersales.enums;

/**
 * 售后单状态机。
 *
 * <p>每个状态带两个元信息，页面和接口都直接复用，不再自己写一遍判断：</p>
 * <ul>
 *     <li>ownerRole：这个状态下轮到此岗位处理（终态为 null）；</li>
 *     <li>terminal：是否是终态，终态单据不再允许任何状态流转。</li>
 * </ul>
 *
 * <p>注意「仓库验货通过」和「售后完成」是两个不同状态：验货只说明退回的商品没问题，
 * 是否换货、是否退款要由客服确认，这是本项目最核心的一条业务规则。</p>
 */
public enum AfterSaleStatus implements Labeled {

    PENDING_REVIEW("待客服审核", UserRole.CS, false),
    NEED_EVIDENCE("待消费者补充凭证", UserRole.CONSUMER, false),
    RETURN_GUIDED("客服已同意，待仓库收货", UserRole.WAREHOUSE, false),
    WAREHOUSE_RECEIVED("仓库已收货，待验货", UserRole.WAREHOUSE, false),
    INSPECTION_PASSED("验货通过，待客服确认结果", UserRole.CS, false),
    INSPECTION_FAILED("验货异常，待客服处理", UserRole.CS, false),
    EXCHANGE_DONE("换货完成，已记录模拟补发单号", null, true),
    REFUND_DONE("退货完成，已记录模拟退款结果", null, true),
    REJECTED("申请已拒绝", null, true);

    private final String label;
    private final UserRole ownerRole;
    private final boolean terminal;

    AfterSaleStatus(String label, UserRole ownerRole, boolean terminal) {
        this.label = label;
        this.ownerRole = ownerRole;
        this.terminal = terminal;
    }

    @Override
    public String getLabel() {
        return label;
    }

    /**
     * @return 当前该由哪个岗位处理；终态返回 null
     */
    public UserRole getOwnerRole() {
        return ownerRole;
    }

    public boolean isTerminal() {
        return terminal;
    }
}
