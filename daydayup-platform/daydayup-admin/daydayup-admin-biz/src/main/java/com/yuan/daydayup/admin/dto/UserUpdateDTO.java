package com.yuan.daydayup.admin.dto;

import com.yuan.daydayup.common.core.dto.BaseRequestDTO;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserUpdateDTO extends BaseRequestDTO {
    @NotBlank
    private String username;
    private String nickname;
    private String email;
    private String mobile;
    private String avatar;
    private String remark;
}
