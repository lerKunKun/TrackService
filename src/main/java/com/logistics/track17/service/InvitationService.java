package com.logistics.track17.service;

import com.logistics.track17.entity.CompanyMember;
import com.logistics.track17.entity.Invitation;
import com.logistics.track17.mapper.CompanyMemberMapper;
import com.logistics.track17.mapper.InvitationMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 邀请服务
 * 管理邮箱邀请流程：创建邀请 → 生成 Token → 接受邀请 → 加入公司/店铺
 */
@Service
@Slf4j
public class InvitationService {

    @Autowired
    private InvitationMapper invitationMapper;

    @Autowired
    private CompanyMemberMapper companyMemberMapper;

    /**
     * 创建邀请
     *
     * @param companyId     公司 ID
     * @param shopId        店铺 ID（可选）
     * @param email         被邀请人邮箱
     * @param roleId        分配的角色 ID
     * @param invitedBy     邀请人 user_id
     * @param expiresInDays 过期天数（默认 7 天）
     * @return 邀请记录
     */
    @Transactional(rollbackFor = Exception.class)
    public Invitation createInvitation(Long companyId, Long shopId, String email,
            Long roleId, Long invitedBy, int expiresInDays) {
        // 生成唯一 Token
        String token = UUID.randomUUID().toString().replace("-", "");

        Invitation invitation = new Invitation();
        invitation.setCompanyId(companyId);
        invitation.setShopId(shopId);
        invitation.setEmail(email);
        invitation.setRoleId(roleId);
        invitation.setToken(token);
        invitation.setStatus("PENDING");
        invitation.setExpiresAt(LocalDateTime.now().plusDays(expiresInDays > 0 ? expiresInDays : 7));
        invitation.setInvitedBy(invitedBy);

        invitationMapper.insert(invitation);
        log.info("创建邀请成功: email={}, companyId={}, shopId={}, token={}", email, companyId, shopId, token);

        return invitation;
    }

    /**
     * 接受邀请（通过 Token）
     */
    @Transactional(rollbackFor = Exception.class)
    public void acceptInvitation(String token, Long userId) {
        Invitation invitation = invitationMapper.selectByToken(token);
        if (invitation == null) {
            throw new RuntimeException("邀请不存在");
        }

        if (!"PENDING".equals(invitation.getStatus())) {
            throw new RuntimeException("邀请已处理或已过期");
        }

        if (invitation.getExpiresAt() != null && invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            invitationMapper.updateStatus(invitation.getId(), "EXPIRED");
            throw new RuntimeException("邀请已过期");
        }

        // 将用户添加为公司成员
        CompanyMember existing = companyMemberMapper.selectByCompanyAndUser(
                invitation.getCompanyId(), userId);
        if (existing == null || existing.getStatus() != 1) {
            CompanyMember member = new CompanyMember();
            member.setCompanyId(invitation.getCompanyId());
            member.setUserId(userId);
            member.setRoleId(invitation.getRoleId());
            member.setInvitedBy(invitation.getInvitedBy());
            member.setJoinedAt(LocalDateTime.now());
            member.setStatus(1);
            companyMemberMapper.insert(member);
        }

        // 更新邀请状态
        invitationMapper.updateStatus(invitation.getId(), "ACCEPTED");
        log.info("接受邀请成功: token={}, userId={}, companyId={}", token, userId, invitation.getCompanyId());
    }

    /**
     * 撤销邀请
     */
    public void revokeInvitation(Long invitationId) {
        invitationMapper.updateStatus(invitationId, "REVOKED");
        log.info("撤销邀请: id={}", invitationId);
    }

    /**
     * 查询公司的所有邀请
     */
    public List<Invitation> getInvitationsByCompany(Long companyId) {
        return invitationMapper.selectByCompanyId(companyId);
    }

    /**
     * 查询待处理邀请（用于用户登录后检查）
     */
    public List<Invitation> getPendingInvitations(String email) {
        return invitationMapper.selectPendingByEmail(email);
    }

    /**
     * 通过 Token 获取邀请详情
     */
    public Invitation getByToken(String token) {
        return invitationMapper.selectByToken(token);
    }
}
