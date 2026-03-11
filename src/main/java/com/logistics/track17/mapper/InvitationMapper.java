package com.logistics.track17.mapper;

import com.logistics.track17.entity.Invitation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface InvitationMapper {

    int insert(Invitation invitation);

    int update(Invitation invitation);

    Invitation selectById(@Param("id") Long id);

    Invitation selectByToken(@Param("token") String token);

    List<Invitation> selectByCompanyId(@Param("companyId") Long companyId);

    List<Invitation> selectByEmail(@Param("email") String email);

    /**
     * 查询待处理的邀请
     */
    List<Invitation> selectPendingByEmail(@Param("email") String email);

    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
