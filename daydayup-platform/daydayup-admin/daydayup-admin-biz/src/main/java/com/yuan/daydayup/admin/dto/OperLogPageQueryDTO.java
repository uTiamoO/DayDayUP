package com.yuan.daydayup.admin.dto;

import com.yuan.daydayup.common.core.dto.BasePageQueryDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class OperLogPageQueryDTO extends BasePageQueryDTO {
    private String username;
    private String module;
    private Integer success;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
