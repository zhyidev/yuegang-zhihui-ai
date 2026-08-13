package com.yuegang.zhihui.common.mybatis;

import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FlywaySafetyCustomizerTest {

    @Test
    void overridesUnsafeDefaultsWithEnterpriseMigrationGuardrails() {
        var configuration = new FluentConfiguration()
            .validateMigrationNaming(false)
            .validateOnMigrate(false)
            .cleanDisabled(false)
            .outOfOrder(true)
            .baselineOnMigrate(true);

        new YghFlywaySafetyCustomizer().customize(configuration);

        assertThat(configuration.isValidateMigrationNaming()).isTrue();
        assertThat(configuration.isValidateOnMigrate()).isTrue();
        assertThat(configuration.isCleanDisabled()).isTrue();
        assertThat(configuration.isOutOfOrder()).isFalse();
        assertThat(configuration.isBaselineOnMigrate()).isFalse();
    }
}
