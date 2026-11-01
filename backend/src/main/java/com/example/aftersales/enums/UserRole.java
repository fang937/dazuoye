package com.example.aftersales.enums;

/**
 * 系统里的三类岗位。
 *
 * <p>权限控制的根据：同一个售后单，三个岗位能做的事情完全不同，
 * Service 里每次动作都要先确认「当前操作人是不是该做这件事的岗位」。</p>
 */
public enum UserRole implements Labeled {

    CONSUMER("消费者"),
    CS("售后客服"),
    WAREHOUSE("仓库人员");

    private final String label;

    UserRole(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
