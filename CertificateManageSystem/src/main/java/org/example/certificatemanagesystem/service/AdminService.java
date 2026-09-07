package org.example.certificatemanagesystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.certificatemanagesystem.common.dto.AdminCreateDTO;
import org.example.certificatemanagesystem.common.dto.AdminDeleteDTO;
import org.example.certificatemanagesystem.common.dto.AdminPageDTO;
import org.example.certificatemanagesystem.common.dto.AdminUpdateDTO;
import org.example.certificatemanagesystem.common.dto.LoginFromDTO;
import org.example.certificatemanagesystem.common.dto.PageResult;
import org.example.certificatemanagesystem.common.vo.AdminVO;
import org.example.certificatemanagesystem.common.vo.LoginUserInfoVO;
import org.example.certificatemanagesystem.entity.Admin;

public interface AdminService extends IService<Admin> {
    LoginUserInfoVO loginUser(LoginFromDTO loginFromDTO);

    /** 分页查询管理员（用户名/真实姓名模糊搜索），返回不含密码 */
    PageResult<AdminVO> pageAdmins(AdminPageDTO dto);

    /** 新增普通管理员（角色固定 ADMIN） */
    void addAdmin(AdminCreateDTO dto);

    /** 更新真实姓名 / 重置密码（仅限 ADMIN 账号） */
    void updateAdmin(AdminUpdateDTO dto);

    /** 删除普通管理员（仅限 ADMIN 账号，且不能有待审核的删除申请） */
    void deleteAdmin(AdminDeleteDTO dto);
}
