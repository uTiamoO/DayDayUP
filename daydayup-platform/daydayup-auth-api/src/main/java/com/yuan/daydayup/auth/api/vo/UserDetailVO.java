package com.yuan.daydayup.auth.api.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailVO {
    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String mobile;
    private Integer status;
    private LocalDateTime lastLoginAt;
    private String lastLoginIp;
    private List<Long> roleIds;
    private List<String> roleCodes;
}
