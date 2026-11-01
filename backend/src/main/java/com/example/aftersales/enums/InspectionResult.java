package com.example.aftersales.enums;

/**
 * 仓库验货结果。
 *
 * <p>PENDING 表示包裹已登记收货但还没录验货结论，
 * 这个中间状态让「收货」和「验货」成为两个可分别追责的操作。</p>
 */
public enum InspectionResult implements Labeled {

    PENDING("待验货"),
    PASSED("验货通过"),
    ABNORMAL("验货异常");

    private final String label;

    InspectionResult(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
