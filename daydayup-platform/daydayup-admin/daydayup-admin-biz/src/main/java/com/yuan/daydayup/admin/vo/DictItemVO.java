package com.yuan.daydayup.admin.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DictItemVO {
    private Long id;
    private String dictCode;
    private String value;
    private String label;
    private Integer sort;
    private Integer status;
    private String remark;
}
