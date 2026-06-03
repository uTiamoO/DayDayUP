package com.yuan.daydayup.auth.api.dto;

import lombok.Data;
import java.util.List;

@Data
public class UserUpdateDTO {
    private String nickname;
    private String email;
    private String mobile;
    private List<Long> roleIds;
}
