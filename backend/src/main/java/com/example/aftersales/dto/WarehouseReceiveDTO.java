package com.example.aftersales.dto;

import lombok.Data;

/**
 * 仓库登记收货入参。
 */
@Data
public class WarehouseReceiveDTO {

    /** 收货备注，例如「外包装完整，吊牌在」，会写入时间线。 */
    private String note;
}
