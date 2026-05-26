package com.yuan.daydayup.common.web.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson 配置：统一时间格式 + 关闭时间戳输出
 *
 * <p>仅提供 {@link Jackson2ObjectMapperBuilderCustomizer}，Spring Boot 会用它构建默认 {@code ObjectMapper}，
 * 避免与 {@code JacksonAutoConfiguration} 自带的 customizer 产生注入歧义。</p>
 */
@Configuration(proxyBeanMethods = false)
public class JacksonConfig {

    private static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @Bean
    @ConditionalOnMissingBean(name = "daydayupJacksonCustomizer")
    public Jackson2ObjectMapperBuilderCustomizer daydayupJacksonCustomizer() {
        return builder -> {
            JavaTimeModule timeModule = new JavaTimeModule();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATETIME_PATTERN);
            timeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(formatter));
            timeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(formatter));
            builder.modules(timeModule);
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        };
    }
}
