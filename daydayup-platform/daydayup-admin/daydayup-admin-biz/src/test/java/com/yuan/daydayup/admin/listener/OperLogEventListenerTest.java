package com.yuan.daydayup.admin.listener;

import com.yuan.daydayup.admin.mapper.SysOperLogMapper;
import com.yuan.daydayup.common.log.model.OperLogRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@SpringJUnitConfig(classes = {OperLogEventListenerTest.TestConfig.class, OperLogEventListener.class})
class OperLogEventListenerTest {

    @Autowired
    private OperLogEventListener listener;

    @MockBean
    private SysOperLogMapper operLogMapper;

    @Test
    void shouldSpecifyCustomExecutorForAsyncAnnotation() throws NoSuchMethodException {
        // 获取 onOperLog 方法
        Method method = OperLogEventListener.class.getMethod("onOperLog", OperLogRecord.class);
        Async asyncAnnotation = method.getAnnotation(Async.class);

        // 断言它必须指定了名为 logExecutor 的线程池
        assertThat(asyncAnnotation).isNotNull();
        assertThat(asyncAnnotation.value()).isEqualTo("logExecutor");
    }

    @Configuration
    @EnableAsync
    static class TestConfig {
        @Bean
        public SysOperLogMapper sysOperLogMapper() {
            return mock(SysOperLogMapper.class);
        }
    }
}
