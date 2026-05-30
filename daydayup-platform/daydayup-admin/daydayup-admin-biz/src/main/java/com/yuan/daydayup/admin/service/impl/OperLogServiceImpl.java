package com.yuan.daydayup.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuan.daydayup.admin.dto.OperLogPageQueryDTO;
import com.yuan.daydayup.admin.entity.SysOperLog;
import com.yuan.daydayup.admin.mapper.SysOperLogMapper;
import com.yuan.daydayup.admin.service.OperLogService;
import com.yuan.daydayup.admin.vo.OperLogVO;
import com.yuan.daydayup.common.core.enums.ErrorCode;
import com.yuan.daydayup.common.core.exception.BizException;
import com.yuan.daydayup.common.core.page.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OperLogServiceImpl implements OperLogService {

    private final SysOperLogMapper mapper;

    @Override
    public PageResult<OperLogVO> page(OperLogPageQueryDTO query) {
        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(query.getUsername()), SysOperLog::getUsername, query.getUsername())
               .like(StringUtils.hasText(query.getModule()), SysOperLog::getModule, query.getModule())
               .eq(query.getSuccess() != null, SysOperLog::getSuccess, query.getSuccess())
               .ge(query.getStartTime() != null, SysOperLog::getOperTime, query.getStartTime())
               .le(query.getEndTime() != null, SysOperLog::getOperTime, query.getEndTime())
               .orderByDesc(SysOperLog::getOperTime);

        Page<SysOperLog> page = mapper.selectPage(
                Page.of(query.normalizedPageNum(), query.normalizedPageSize()), wrapper);
        List<OperLogVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public OperLogVO detail(Long id) {
        SysOperLog entity = mapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND, "记录不存在");
        }
        return toVO(entity);
    }

    private OperLogVO toVO(SysOperLog entity) {
        return OperLogVO.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .username(entity.getUsername())
                .module(entity.getModule())
                .operation(entity.getOperation())
                .method(entity.getMethod())
                .requestUrl(entity.getRequestUrl())
                .requestMethod(entity.getRequestMethod())
                .requestIp(entity.getRequestIp())
                .requestParams(entity.getRequestParams())
                .responseBody(entity.getResponseBody())
                .success(entity.getSuccess())
                .errorMsg(entity.getErrorMsg())
                .costMs(entity.getCostMs())
                .operTime(entity.getOperTime())
                .build();
    }
}
