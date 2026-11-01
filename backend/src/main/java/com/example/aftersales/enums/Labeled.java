package com.example.aftersales.enums;

/**
 * 带中文标签的枚举共用的约定。
 *
 * <p>有了这个接口，字典接口就能用同一套代码把岗位、售后类型、申请原因、状态
 * 一次性输出成 {@code {name, label}} 给前端，不用写反射，也不用每个枚举配一个转换类。</p>
 */
public interface Labeled {

    /**
     * 枚举本身的英文名，提交给接口的值。
     */
    String name();

    /**
     * 显示给人的中文标签。
     */
    String getLabel();
}
