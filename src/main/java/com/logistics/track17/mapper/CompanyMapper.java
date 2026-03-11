package com.logistics.track17.mapper;

import com.logistics.track17.entity.Company;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CompanyMapper {

    int insert(Company company);

    int update(Company company);

    int deleteById(@Param("id") Long id);

    Company selectById(@Param("id") Long id);

    Company selectByCode(@Param("code") String code);

    List<Company> selectAll();

    /**
     * 查询用户所属的所有公司
     */
    List<Company> selectByUserId(@Param("userId") Long userId);
}
