package com.logistics.track17.mapper;

import com.logistics.track17.entity.CompanyMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CompanyMemberMapper {

    int insert(CompanyMember member);

    int update(CompanyMember member);

    int deleteById(@Param("id") Long id);

    int deleteByCompanyAndUser(@Param("companyId") Long companyId, @Param("userId") Long userId);

    CompanyMember selectById(@Param("id") Long id);

    /**
     * 查询公司所有成员
     */
    List<CompanyMember> selectByCompanyId(@Param("companyId") Long companyId);

    /**
     * 查询用户在某公司的成员记录
     */
    CompanyMember selectByCompanyAndUser(@Param("companyId") Long companyId, @Param("userId") Long userId);

    /**
     * 查询用户所有的公司成员关系
     */
    List<CompanyMember> selectByUserId(@Param("userId") Long userId);

    /**
     * 计数：用户在某公司的成员数（用于快速判断是否为成员）
     */
    int countByCompanyAndUser(@Param("companyId") Long companyId, @Param("userId") Long userId);
}
