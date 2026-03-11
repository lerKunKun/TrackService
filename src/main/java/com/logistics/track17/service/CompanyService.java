package com.logistics.track17.service;

import com.logistics.track17.entity.Company;
import com.logistics.track17.entity.CompanyMember;
import com.logistics.track17.mapper.CompanyMapper;
import com.logistics.track17.mapper.CompanyMemberMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 公司服务
 * 管理公司CRUD和公司成员关系
 */
@Service
@Slf4j
public class CompanyService {

    @Autowired
    private CompanyMapper companyMapper;

    @Autowired
    private CompanyMemberMapper companyMemberMapper;

    /**
     * 创建公司并将创建者设为所有者（SUPER_ADMIN）
     */
    @Transactional(rollbackFor = Exception.class)
    public Company createCompany(String name, String code, Long ownerUserId, Long superAdminRoleId) {
        // 检查编码唯一性
        Company existing = companyMapper.selectByCode(code);
        if (existing != null) {
            throw new RuntimeException("公司编码已存在: " + code);
        }

        Company company = new Company();
        company.setName(name);
        company.setCode(code);
        company.setOwnerUserId(ownerUserId);
        company.setStatus(1);
        companyMapper.insert(company);

        // 创建者自动成为公司成员（SUPER_ADMIN 角色）
        CompanyMember member = new CompanyMember();
        member.setCompanyId(company.getId());
        member.setUserId(ownerUserId);
        member.setRoleId(superAdminRoleId);
        member.setJoinedAt(LocalDateTime.now());
        member.setStatus(1);
        companyMemberMapper.insert(member);

        log.info("创建公司成功: {} ({}), 所有者: {}", name, code, ownerUserId);
        return company;
    }

    public Company getCompanyById(Long id) {
        return companyMapper.selectById(id);
    }

    public Company getCompanyByCode(String code) {
        return companyMapper.selectByCode(code);
    }

    /**
     * 获取用户所属的所有公司
     */
    public List<Company> getCompaniesByUserId(Long userId) {
        return companyMapper.selectByUserId(userId);
    }

    /**
     * 获取公司所有成员
     */
    public List<CompanyMember> getCompanyMembers(Long companyId) {
        return companyMemberMapper.selectByCompanyId(companyId);
    }

    /**
     * 检查用户是否为某公司成员
     */
    public boolean isCompanyMember(Long companyId, Long userId) {
        return companyMemberMapper.countByCompanyAndUser(companyId, userId) > 0;
    }

    /**
     * 添加成员到公司
     */
    @Transactional(rollbackFor = Exception.class)
    public void addMember(Long companyId, Long userId, Long roleId, Long invitedBy) {
        // 检查是否已是成员
        CompanyMember existing = companyMemberMapper.selectByCompanyAndUser(companyId, userId);
        if (existing != null && existing.getStatus() == 1) {
            throw new RuntimeException("用户已是公司成员");
        }

        CompanyMember member = new CompanyMember();
        member.setCompanyId(companyId);
        member.setUserId(userId);
        member.setRoleId(roleId);
        member.setInvitedBy(invitedBy);
        member.setJoinedAt(LocalDateTime.now());
        member.setStatus(1);
        companyMemberMapper.insert(member);

        log.info("添加公司成员: companyId={}, userId={}, roleId={}", companyId, userId, roleId);
    }

    /**
     * 移除公司成员
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeMember(Long companyId, Long userId) {
        companyMemberMapper.deleteByCompanyAndUser(companyId, userId);
        log.info("移除公司成员: companyId={}, userId={}", companyId, userId);
    }

    /**
     * 更新公司信息
     */
    public Company updateCompany(Company company) {
        companyMapper.update(company);
        log.info("更新公司信息: {}", company.getId());
        return company;
    }
}
