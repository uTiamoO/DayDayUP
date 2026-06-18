package com.yuan.daydayup.auth.controller;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.assertj.core.api.Assertions.assertThat;

class AuthApiPathContractTest {

    @Test
    void firstPartyTokenApiUsesSingleApiPrefixInsideAuthService() {
        assertThat(requestMappingValue(AuthApiController.class)).containsExactly("/api");
    }

    @Test
    void oidcSessionApiUsesSingleApiPrefixInsideAuthService() {
        assertThat(requestMappingValue(AuthSessionController.class)).containsExactly("/api/session");
    }

    private static String[] requestMappingValue(Class<?> controllerType) {
        return controllerType.getAnnotation(RequestMapping.class).value();
    }
}
