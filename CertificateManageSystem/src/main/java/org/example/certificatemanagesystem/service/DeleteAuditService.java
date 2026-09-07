package org.example.certificatemanagesystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.certificatemanagesystem.common.dto.AuditDTO;
import org.example.certificatemanagesystem.common.dto.DeleteRequestDTO;
import org.example.certificatemanagesystem.common.dto.PageResult;
import org.example.certificatemanagesystem.common.vo.AuditMsgVO;
import org.example.certificatemanagesystem.entity.DeleteAudit;

public interface DeleteAuditService extends IService<DeleteAudit> {
    void deleteStatusUpdate(DeleteRequestDTO deleteRequestDTO, Long requesterId);

    PageResult<AuditMsgVO> getAuditMessages(int current, int size);

    void audit(AuditDTO auditDTO, Long approverId);

    /** 未读消息数：createTime 晚于 since 的消息条数（不限状态），用于侧边栏角标 */
    long countNewSince(java.time.LocalDateTime since);
}
