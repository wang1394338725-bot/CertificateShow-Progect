package org.example.certificatemanagesystem.common.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员信息更新：realName 与 password 均可选，只更新传入的字段。
 * 重置密码复用本 DTO（仅传 id + password）。
 */
@Data
public class AdminUpdateDTO {
    @NotNull(message = "管理员ID不能为空")
    private Long id;

    private String realName;

    @Size(min = 6, max = 20, message = "密码长度需在 6-20 位之间")
    private String password;
}
