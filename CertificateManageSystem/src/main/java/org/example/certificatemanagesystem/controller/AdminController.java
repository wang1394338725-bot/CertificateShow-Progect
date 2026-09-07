package org.example.certificatemanagesystem.controller;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.certificatemanagesystem.common.dto.AdminCreateDTO;
import org.example.certificatemanagesystem.common.dto.AdminDeleteDTO;
import org.example.certificatemanagesystem.common.dto.AdminPageDTO;
import org.example.certificatemanagesystem.common.dto.AdminUpdateDTO;
import org.example.certificatemanagesystem.common.dto.LoginFromDTO;
import org.example.certificatemanagesystem.common.dto.PageResult;
import org.example.certificatemanagesystem.common.utils.JwtUtil;
import org.example.certificatemanagesystem.common.vo.AdminVO;
import org.example.certificatemanagesystem.common.vo.LoginUserInfoVO;
import org.example.certificatemanagesystem.common.vo.ResultVO;
import org.example.certificatemanagesystem.entity.Admin;
import org.example.certificatemanagesystem.mapper.AdminMapper;
import org.example.certificatemanagesystem.service.AdminService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RestController
@RequestMapping("/certificate")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;
    private final JwtUtil jwtUtil;
    private final AdminMapper adminMapper;

    /** 会话绝对上限（天）：自首次登录起超过该天数后拒绝续期，强制重新登录 */
    @Value("${jwt.max-session-days:3}")
    private Long maxSessionDays;

    @PostMapping("/login")
    public ResultVO<LoginUserInfoVO> login(@RequestBody @Valid LoginFromDTO loginFromDTO) {
        LoginUserInfoVO loginUserInfoVO = adminService.loginUser(loginFromDTO);
        return ResultVO.success(loginUserInfoVO);
    }

    /**
     * token 无感续期：前端在闲置超过 token 时限后的下一次操作前自动调用。
     * 签名有效即可换发新 token（含已过期的旧 token），但新 token 继承原 iat；
     * 自首次登录（iat）起超过 jwt.max-session-days 天后拒绝续期，强制重新登录。
     * 签名无效或账号已删除同样要求重新登录。
     */
    @PostMapping("/refresh-token")
    public ResultVO<String> refreshToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResultVO.error(401, "未登录");
        }
        Claims claims = jwtUtil.parseClaimsAllowExpired(authHeader.substring(7));
        if (claims == null) {
            return ResultVO.error(401, "登录已失效，请重新登录");
        }
        // 会话绝对上限：防无限续期，超期后必须重新登录（重新登录产生新的 iat）
        Date issuedAt = claims.getIssuedAt();
        if (issuedAt == null
                || System.currentTimeMillis() - issuedAt.getTime() > maxSessionDays * 86400_000L) {
            return ResultVO.error(401, "登录会话已超期，请重新登录");
        }
        String userId = claims.getSubject();
        Admin admin = adminMapper.selectById(Long.parseLong(userId));
        if (admin == null) {
            return ResultVO.error(401, "账号不存在，请重新登录");
        }
        // 继承原 iat：会话上限以"首次登录"为基准，续期不重置
        return ResultVO.success(jwtUtil.generateToken(Long.parseLong(userId), issuedAt));
    }

    // ==================== 管理员管理（仅 SUPER_ADMIN） ====================

    /** 校验请求者为有效登录的超管，否则返回 null（token 过期/无效/角色不符） */
    private Admin requireSuperAdmin(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        try {
            String userId = jwtUtil.getUserIdFromToken(authHeader.substring(7));
            Admin admin = adminMapper.selectById(Long.parseLong(userId));
            if (admin != null && "SUPER_ADMIN".equals(admin.getRole())) {
                return admin;
            }
        } catch (JwtException | IllegalArgumentException ignored) {
        }
        return null;
    }

    @PostMapping("/admin-list")
    public ResultVO<PageResult<AdminVO>> adminList(@RequestBody AdminPageDTO dto, HttpServletRequest request) {
        if (requireSuperAdmin(request) == null) {
            return ResultVO.error(403, "无权限操作");
        }
        return ResultVO.success(adminService.pageAdmins(dto));
    }

    @PostMapping("/admin/add")
    public ResultVO<String> addAdmin(@RequestBody @Valid AdminCreateDTO dto, HttpServletRequest request) {
        if (requireSuperAdmin(request) == null) {
            return ResultVO.error(403, "无权限操作");
        }
        adminService.addAdmin(dto);
        return ResultVO.success("新增管理员成功");
    }

    @PutMapping("/admin/update")
    public ResultVO<String> updateAdmin(@RequestBody @Valid AdminUpdateDTO dto, HttpServletRequest request) {
        if (requireSuperAdmin(request) == null) {
            return ResultVO.error(403, "无权限操作");
        }
        adminService.updateAdmin(dto);
        return ResultVO.success("更新成功");
    }

    @PostMapping("/admin/delete")
    public ResultVO<String> deleteAdmin(@RequestBody @Valid AdminDeleteDTO dto, HttpServletRequest request) {
        if (requireSuperAdmin(request) == null) {
            return ResultVO.error(403, "无权限操作");
        }
        adminService.deleteAdmin(dto);
        return ResultVO.success("删除成功");
    }
}
