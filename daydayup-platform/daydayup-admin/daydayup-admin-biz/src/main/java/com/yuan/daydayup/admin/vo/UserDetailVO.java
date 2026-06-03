package com.yuan.daydayup.admin.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
    private String avatar;
    private Integer status;
    private LocalDateTime lastLoginAt;
    private String remark;
}
