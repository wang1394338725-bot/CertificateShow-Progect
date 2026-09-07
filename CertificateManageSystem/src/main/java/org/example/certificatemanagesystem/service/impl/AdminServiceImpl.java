package org.example.certificatemanagesystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.example.certificatemanagesystem.common.dto.AdminCreateDTO;
import org.example.certificatemanagesystem.common.dto.AdminDeleteDTO;
import org.example.certificatemanagesystem.common.dto.AdminPageDTO;
import org.example.certificatemanagesystem.common.dto.AdminUpdateDTO;
import org.example.certificatemanagesystem.common.dto.LoginFromDTO;
import org.example.certificatemanagesystem.common.dto.PageResult;
import org.example.certificatemanagesystem.common.enums.ResultCodeEnum;
import org.example.certificatemanagesystem.common.exception.BusinessException;
import org.example.certificatemanagesystem.common.vo.AdminVO;
import org.example.certificatemanagesystem.common.vo.LoginUserInfoVO;
import org.example.certificatemanagesystem.entity.Admin;
import org.example.certificatemanagesystem.mapper.AdminMapper;
import org.example.certificatemanagesystem.service.AdminService;
import org.example.certificatemanagesystem.common.utils.JwtUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements AdminService {

    private final AdminMapper adminMapper;
    private final JwtUtil jwtUtil;
    // final + 初始化器 → 不参与 @RequiredArgsConstructor 注入，全局复用一个实例
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public LoginUserInfoVO loginUser(LoginFromDTO loginFromDTO) {
        // 获取信息
        Admin admin = adminMapper.selectByUsername(loginFromDTO.getUsername());
        if (admin == null) {
            throw new BusinessException(ResultCodeEnum.USER_ERROR);
        }

        // 密码校验：BCrypt 哈希比对；兼容历史明文（不以 $2a 开头），比对通过后自动升级为 BCrypt
        String raw = loginFromDTO.getPassword();
        if (admin.getPassword().startsWith("$2a")) {
            if (!passwordEncoder.matches(raw, admin.getPassword())) {
                throw new BusinessException(ResultCodeEnum.USER_ERROR);
            }
        } else {
            if (!raw.equals(admin.getPassword())) {
                throw new BusinessException(ResultCodeEnum.USER_ERROR);
            }
            admin.setPassword(passwordEncoder.encode(raw)); // 本次 lastLoginTime 更新时一并落库
        }

        // 更新账号登录时间（含密码自动升级）
        admin.setLastLoginTime(LocalDateTime.now());
        adminMapper.updateById(admin);

        // 设置登录 token
        String token = jwtUtil.generateToken(admin.getId().toString());

        return new LoginUserInfoVO(admin.getId(), admin.getUsername(), admin.getRealName(), admin.getRole(), token);
    }

    @Override
    public PageResult<AdminVO> pageAdmins(AdminPageDTO dto) {
        Page<Admin> page = new Page<>(dto.getCurrent(), dto.getSize());
        LambdaQueryWrapper<Admin> wrapper = new LambdaQueryWrapper<>();
        if (dto.getKeyword() != null && !dto.getKeyword().isBlank()) {
            wrapper.and(w -> w.like(Admin::getUsername, dto.getKeyword())
                    .or().like(Admin::getRealName, dto.getKeyword()));
        }
        wrapper.orderByAsc(Admin::getId);
        Page<Admin> result = adminMapper.selectPage(page, wrapper);

        List<AdminVO> records = result.getRecords().stream().map(a -> {
            AdminVO vo = new AdminVO();
            vo.setId(a.getId());
            vo.setUsername(a.getUsername());
            vo.setRealName(a.getRealName());
            vo.setRole(a.getRole());
            vo.setCreateTime(a.getCreateTime());
            return vo;
        }).toList();
        PageResult<AdminVO> pageResult = new PageResult<>();
        pageResult.setRecords(records);
        pageResult.setTotal(result.getTotal());
        pageResult.setCurrent(dto.getCurrent());
        pageResult.setSize(dto.getSize());
        return pageResult;
    }

    @Override
    public void addAdmin(AdminCreateDTO dto) {
        if (adminMapper.selectByUsername(dto.getUsername()) != null) {
            throw new BusinessException("用户名已存在");
        }
        Admin admin = new Admin();
        admin.setUsername(dto.getUsername());
        admin.setRealName(dto.getRealName());
        admin.setPassword(passwordEncoder.encode(dto.getPassword()));
        // 超管只能创建普通管理员，角色不接受前端指定
        admin.setRole("ADMIN");
        adminMapper.insert(admin);
    }

    @Override
    public void updateAdmin(AdminUpdateDTO dto) {
        Admin target = adminMapper.selectById(dto.getId());
        if (target == null) {
            throw new BusinessException("该管理员不存在");
        }
        if (!"ADMIN".equals(target.getRole())) {
            throw new BusinessException("不能修改超级管理员账号");
        }
        if (dto.getRealName() != null && !dto.getRealName().isBlank()) {
            target.setRealName(dto.getRealName());
        }
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            target.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        adminMapper.updateById(target);
    }

    @Override
    public void deleteAdmin(AdminDeleteDTO dto) {
        Admin target = adminMapper.selectById(dto.getId());
        if (target == null) {
            throw new BusinessException("该管理员不存在");
        }
        if (!"ADMIN".equals(target.getRole())) {
            throw new BusinessException("不能删除超级管理员账号");
        }
        // 其提交过的删除申请已通过 requester_name 快照自包含，账号删除不影响消息栏展示与审核
        adminMapper.deleteById(dto.getId());
    }
}
