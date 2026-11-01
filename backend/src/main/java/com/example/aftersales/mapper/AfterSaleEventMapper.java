package com.example.aftersales.mapper;

import com.example.aftersales.entity.AfterSaleEventEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 售后时间线数据访问接口。
 */
@Mapper
public interface AfterSaleEventMapper {

    int insert(AfterSaleEventEntity event);

    List<AfterSaleEventEntity> listByAfterSaleId(@Param("afterSaleId") Long afterSaleId);
}
