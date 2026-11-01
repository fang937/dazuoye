package com.example.aftersales.enums;

/**
 * 申请原因编码。
 *
 * <p>数据库里存英文编码而不是中文句子，好处是统计「尺码不合适占比多少」时不用做文本匹配；
 * 前端下拉框展示中文标签，标签来源就是这个枚举。</p>
 */
public enum AfterSaleReason implements Labeled {

    SIZE_MISMATCH("尺码不合适"),
    QUALITY_ISSUE("商品质量问题"),
    WRONG_ITEM("发错商品"),
    DAMAGED("收到时已破损"),
    NO_LONGER_NEED("不想要了");

    private final String label;

    AfterSaleReason(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
