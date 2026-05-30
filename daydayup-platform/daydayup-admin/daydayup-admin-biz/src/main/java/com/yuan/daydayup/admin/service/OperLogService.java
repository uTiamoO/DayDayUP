package com.yuan.daydayup.admin.service;

import com.yuan.daydayup.admin.dto.OperLogPageQueryDTO;
import com.yuan.daydayup.admin.vo.OperLogVO;
import com.yuan.daydayup.common.core.page.PageResult;

public interface OperLogService {
    PageResult<OperLogVO> page(OperLogPageQueryDTO query);
    OperLogVO detail(Long id);
}
