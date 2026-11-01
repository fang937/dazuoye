package com.example.aftersales.enums;

/**
 * 允许发生的售后动作。
 *
 * <p>动作名同时用于三个地方，保证「页面按钮 = 接口入参 = Service 分支」是同一套词：</p>
 * <ol>
 *     <li>后端算出某张单当前允许哪些动作，返回给前端渲染按钮；</li>
 *     <li>前端提交动作时把名字原样传回来；</li>
 *     <li>Service 按动作校验岗位与状态，并写入时间线的 action 列。</li>
 * </ol>
 */
public enum AfterSaleAction implements Labeled {

    APPLY("提交售后申请"),
    SUPPLEMENT_EVIDENCE("补充凭证"),
    REQUEST_EVIDENCE("要求补充凭证"),
    APPROVE_RETURN("同意退回并给出售后指引"),
    REJECT("拒绝申请"),
    RECEIVE("登记收货"),
    INSPECT("登记验货结果"),
    CONFIRM_EXCHANGE("确认换货并记录补发单号"),
    CONFIRM_REFUND("确认退款并记录模拟退款金额");

    private final String label;

    AfterSaleAction(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
