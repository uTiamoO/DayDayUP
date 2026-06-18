package com.yuan.daydayup.auth.view;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class AuthBackendOnlyContractTest {

    @Test
    void authServiceDoesNotPackageBackendRenderedLoginTemplate() {
        assertThat(new ClassPathResource("templates/login.html").exists()).isFalse();
    }
}
