package com.yuan.daydayup.common.mybatis.service;

public interface DeletableCrudService<ID> {
    void delete(ID id);
}
