package com.example.aftersales.mapper;

import com.example.aftersales.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 演示账号数据访问接口。
 */
@Mapper
public interface UserMapper {

    List<UserEntity> findAll();

    UserEntity findByUsername(@Param("username") String username);

    UserEntity findById(@Param("id") Long id);
}
