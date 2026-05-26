package com.yuan.daydayup.common.feign.decoder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuan.daydayup.common.core.enums.ErrorCode;
import com.yuan.daydayup.common.core.exception.BizException;
import com.yuan.daydayup.common.core.result.R;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Feign 错误解码器
 *
 * <p>把下游服务返回的 {@code R.fail(...)} 翻译成 {@link BizException}，
 * 让上游业务代码可以像本地一样 try/catch。</p>
 */
@Slf4j
public class BizFeignErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Exception decode(String methodKey, Response response) {
        try {
            if (response.body() != null) {
                byte[] body = response.body().asInputStream().readAllBytes();
                R<?> r = objectMapper.readValue(new String(body, StandardCharsets.UTF_8), R.class);
                if (r != null && r.getCode() != null) {
                    return new BizException(r.getCode(),
                            r.getMessage() == null ? ErrorCode.BIZ_ERROR.getMessage() : r.getMessage());
                }
            }
        } catch (IOException ex) {
            log.warn("Feign 错误解码失败：method={}, status={}, msg={}", methodKey, response.status(), ex.getMessage());
        }
        return defaultDecoder.decode(methodKey, response);
    }
}
