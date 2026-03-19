package com.logistics.track17.mapper;

import com.logistics.track17.entity.LoginLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface LoginLogMapper {

    void insert(LoginLog loginLog);

    List<LoginLog> selectByPage(
            @Param("offset") int offset,
            @Param("limit") int limit,
            @Param("username") String username,
            @Param("loginType") String loginType,
            @Param("loginResult") String loginResult,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    Long countAll(
            @Param("username") String username,
            @Param("loginType") String loginType,
            @Param("loginResult") String loginResult,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}
