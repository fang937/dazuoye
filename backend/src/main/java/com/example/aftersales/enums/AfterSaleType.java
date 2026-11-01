package com.example.aftersales.enums;

/**
 * 售后类型：首版只做退货和换货两种。
 */
public enum AfterSaleType implements Labeled {

    RETURN("退货"),
    EXCHANGE("换货");

    private final String label;

    AfterSaleType(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
