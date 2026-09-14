package com.github.nurbolotalt.bank_app;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SmokeTest extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
        assertThat(postgres.isRunning()).isTrue();
    }
}