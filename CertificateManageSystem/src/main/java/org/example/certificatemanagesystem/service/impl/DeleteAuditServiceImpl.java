package org.example.certificatemanagesystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.example.certificatemanagesystem.common.dto.AuditDTO;
import org.example.certificatemanagesystem.common.dto.DeleteRequestDTO;
import org.example.certificatemanagesystem.common.dto.PageResult;
import org.example.certificatemanagesystem.common.exception.BusinessException;
import org.example.certificatemanagesystem.common.vo.AuditMsgVO;
import org.example.certificatemanagesystem.entity.Admin;
import org.example.certificatemanagesystem.entity.Certificate;
import org.example.certificatemanagesystem.entity.DeleteAudit;
import org.example.certificatemanagesystem.mapper.AdminMapper;
import org.example.certificatemanagesystem.mapper.CertificateMapper;
import org.example.certificatemanagesystem.mapper.DeleteAuditMapper;
import org.example.certificatemanagesystem.service.DeleteAuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class DeleteAuditServiceImpl extends ServiceImpl<DeleteAuditMapper, DeleteAudit> implements DeleteAuditService {
    private final CertificateMapper certificateMapper;
    private final AdminMapper adminMapper;

    @Override
    public void deleteStatusUpdate(DeleteRequestDTO deleteRequestDTO, Long requesterId) {
        Certificate certificate = certificateMapper.selectById(deleteRequestDTO.getCertificateId());
        if (certificate == null) {
            throw new BusinessException("奖状不存在，ID:" + deleteRequestDTO.getCertificateId());
        }

        // 按角色分流：超管拥有最终权限，直接删除并留痕（单超管系统无同级制衡）；普通管理员走申请-审核流
        Admin requester = adminMapper.selectById(requesterId);
        if (requester != null && "SUPER_ADMIN".equals(requester.getRole())) {
            if (certificate.getStatus() != 0) {
                throw new BusinessException("该奖状已有待审核的删除申请，请先在消息栏处理该申请");
            }
            DeleteAudit audit = new DeleteAudit();
            audit.setCertificateId(certificate.getId());
            audit.setCertificateTitle(certificate.getTitle());
            audit.setRequesterId(requesterId);
            audit.setRequesterName(
                    StringUtils.hasText(requester.getRealName()) ? requester.getRealName() : requester.getUsername());
            audit.setReason(deleteRequestDTO.getReason());
            audit.setStatus(3); // 3-超管直接删除
            audit.setApproverId(requesterId);
            audit.setApproveTime(LocalDateTime.now());
            this.save(audit);
            certificateMapper.deleteById(certificate.getId());
            return;
        }

        LambdaUpdateWrapper<Certificate> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Certificate::getId, deleteRequestDTO.getCertificateId())
                .eq(Certificate::getStatus, 0) // 关键：只在状态为0时才更新
                .set(Certificate::getStatus, 1); // 改为待审核

        int updateRows = certificateMapper.update(null, updateWrapper);
        if (updateRows == 0) {
            throw new BusinessException("该奖状当前状态不允许删除（可能已被提交）");
        }

        DeleteAudit audit = new DeleteAudit();
        audit.setCertificateId(deleteRequestDTO.getCertificateId());
        audit.setCertificateTitle(certificate.getTitle());
        audit.setRequesterId(requesterId);
        // 姓名快照：提交人账号日后被删除，消息栏仍能显示是谁提交的
        if (requester != null) {
            audit.setRequesterName(
                    StringUtils.hasText(requester.getRealName()) ? requester.getRealName() : requester.getUsername());
        }
        audit.setReason(deleteRequestDTO.getReason());
        audit.setStatus(0);

        this.save(audit);
    }

    @Override
    public PageResult<AuditMsgVO> getAuditMessages(int current, int size) {
        // 防刷钳制：与奖状分页同一策略（size≤50，current≤1000）
        current = Math.min(Math.max(current, 1), 1000);
        size = Math.min(Math.max(size, 1), 50);
        Page<AuditMsgVO> page = new Page<>(current, size);
        // 自定义 SQL 传入 Page 参数，由分页插件自动追加 LIMIT 与 COUNT
        Page<AuditMsgVO> result = baseMapper.selectAuditMessages(page);

        PageResult<AuditMsgVO> pageResult = new PageResult<>();
        pageResult.setRecords(result.getRecords());
        pageResult.setTotal(result.getTotal());
        pageResult.setCurrent((int) result.getCurrent());
        pageResult.setSize((int) result.getSize());
        return pageResult;
    }

    @Override
    public long countNewSince(LocalDateTime since) {
        LambdaQueryWrapper<DeleteAudit> wrapper = new LambdaQueryWrapper<>();
        if (since != null) {
            wrapper.gt(DeleteAudit::getCreateTime, since);
        }
        return this.count(wrapper);
    }

    @Override
    public void audit(AuditDTO auditDTO, Long approverId) {
        boolean approved = Boolean.TRUE.equals(auditDTO.getApproved());
        if (!approved && !StringUtils.hasText(auditDTO.getRejectReason())) {
            throw new BusinessException("驳回原因不能为空");
        }

        // 条件更新 status 0→1/2：两个超管并发审核同一条时，第二个 UPDATE 匹配不到 status=0，行数为 0
        LambdaUpdateWrapper<DeleteAudit> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(DeleteAudit::getId, auditDTO.getAuditId())
                .eq(DeleteAudit::getStatus, 0)
                .set(DeleteAudit::getStatus, approved ? 1 : 2)
                .set(DeleteAudit::getApproverId, approverId)
                .set(DeleteAudit::getApproveTime, LocalDateTime.now());
        if (!approved) {
            wrapper.set(DeleteAudit::getRejectReason, auditDTO.getRejectReason());
        }
        if (!this.update(wrapper)) {
            throw new BusinessException("该申请已被审核，请刷新后查看");
        }

        DeleteAudit audit = this.getById(auditDTO.getAuditId());
        if (approved) {
            // 通过：物理删除奖状（delete_audit 保留审计快照，消息不受影响）
            certificateMapper.deleteById(audit.getCertificateId());
        } else {
            // 驳回：奖状状态恢复为正常（仅当仍处于待审核状态时）
            LambdaUpdateWrapper<Certificate> certWrapper = new LambdaUpdateWrapper<>();
            certWrapper.eq(Certificate::getId, audit.getCertificateId())
                    .eq(Certificate::getStatus, 1)
                    .set(Certificate::getStatus, 0);
            certificateMapper.update(null, certWrapper);
        }
    }
}
