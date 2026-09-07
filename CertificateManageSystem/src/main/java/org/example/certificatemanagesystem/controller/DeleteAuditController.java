package org.example.certificatemanagesystem.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.certificatemanagesystem.common.dto.AuditDTO;
import org.example.certificatemanagesystem.common.dto.DeleteRequestDTO;
import org.example.certificatemanagesystem.common.dto.PageResult;
import org.example.certificatemanagesystem.common.enums.ResultCodeEnum;
import org.example.certificatemanagesystem.common.exception.BusinessException;
import org.example.certificatemanagesystem.common.utils.JwtUtil;
import org.example.certificatemanagesystem.common.vo.AuditMsgVO;
import org.example.certificatemanagesystem.common.vo.ResultVO;
import org.example.certificatemanagesystem.entity.Admin;
import org.example.certificatemanagesystem.mapper.AdminMapper;
import org.example.certificatemanagesystem.service.DeleteAuditService;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/certificate")
@RequiredArgsConstructor
public class DeleteAuditController {
    private final DeleteAuditService deleteAuditService;
    private final JwtUtil jwtUtil;
    private final AdminMapper adminMapper;

    /**
     * 从请求头解析当前登录用户 ID，未登录返回 null
     */
    private Long parseUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        try {
            return Long.parseLong(jwtUtil.getUserIdFromToken(authHeader.substring(7)));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 普通管理员提交删除申请（原接口保留，token 解析复用 parseUserId）
     */
    @PostMapping("/delete-request")
    public ResultVO<String> deleteRequest(@RequestBody DeleteRequestDTO deleteRequestDTO, HttpServletRequest request) {
        Long userId = parseUserId(request);
        if (userId == null) {
            return ResultVO.error(401, "未登录或 token 无效");
        }
        deleteAuditService.deleteStatusUpdate(deleteRequestDTO, userId);
        return ResultVO.success("等待审核");
    }

    /**
     * 消息栏列表：所有管理员可见（广播），status=0 为未读待审核，1/2 为已读结果
     */
    @PostMapping("/audit-list")
    public ResultVO<PageResult<AuditMsgVO>> auditList(@RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size) {
        return ResultVO.success(deleteAuditService.getAuditMessages(current, size));
    }

    /**
     * 未读消息数：createTime 晚于 since（epoch 毫秒）的消息条数，用于侧边栏角标。
     * 已读状态由前端 localStorage 记录"最后查看消息栏时间"实现，无需消息表。
     */
    @GetMapping("/audit-pending-count")
    public ResultVO<Long> auditPendingCount(
            @RequestParam(value = "since", required = false) Long sinceMillis) {
        LocalDateTime since = sinceMillis == null ? null
                : LocalDateTime.ofInstant(Instant.ofEpochMilli(sinceMillis), ZoneId.systemDefault());
        return ResultVO.success(deleteAuditService.countNewSince(since));
    }

    /**
     * 超管审核删除申请（通过/驳回）
     */
    @PostMapping("/audit")
    public ResultVO<String> audit(@RequestBody AuditDTO auditDTO, HttpServletRequest request) {
        Long userId = parseUserId(request);
        if (userId == null) {
            return ResultVO.error(401, "未登录或 token 无效");
        }
        Admin admin = adminMapper.selectById(userId);
        if (admin == null || !"SUPER_ADMIN".equals(admin.getRole())) {
            return ResultVO.error(403, "仅超级管理员可执行审核操作");
        }
        deleteAuditService.audit(auditDTO, userId);
        return ResultVO.success(Boolean.TRUE.equals(auditDTO.getApproved()) ? "已通过并删除奖状" : "已驳回该申请");
    }
}
