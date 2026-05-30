package com.yuan.daydayup.admin.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DictVO {
    private Long id;
    private String code;
    private String name;
    private Integer status;
    private String remark;
}
