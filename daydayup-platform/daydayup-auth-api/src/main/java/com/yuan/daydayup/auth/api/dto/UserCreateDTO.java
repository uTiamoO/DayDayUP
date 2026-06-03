package com.yuan.daydayup.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class UserCreateDTO {
    @NotBlank
    @Size(max = 64)
    private String username;

    @NotBlank
    @Size(max = 255)
    private String password;

    private String nickname;
    private String email;
    private String mobile;
    private List<Long> roleIds;
}
