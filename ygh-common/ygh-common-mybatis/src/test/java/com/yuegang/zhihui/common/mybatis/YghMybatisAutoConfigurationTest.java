package com.yuegang.zhihui.common.mybatis;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.io.IOException;
import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

class YghMybatisAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(YghMybatisAutoConfiguration.class))
        .withBean(AuditorProvider.class, AuditorProvider::system);

    private static MybatisPlusInterceptor interceptor(long maxLimit, boolean overflow) {
        var pagination = new PaginationInnerInterceptor();
        pagination.setMaxLimit(maxLimit);
        pagination.setOverflow(overflow);
        var interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }

    private static void assertGuardRejected(Throwable failure) {
        assertThat(failure)
            .hasMessageContaining("MybatisPlusInterceptor must include pagination limited to 100");
    }

    @Test
    void registersAuditingClockAndPaginationInfrastructure() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(AuditorProvider.class);
            assertThat(context).hasSingleBean(Clock.class);
            assertThat(context).hasSingleBean(MetaObjectHandler.class);
            assertThat(context).hasSingleBean(MybatisPlusInterceptor.class);
            assertThat(context).hasSingleBean(FlywayMigrationStrategy.class);
        });
    }

    @Test
    void preservesAServiceSpecificAuditorProvider() {
        AuditorProvider custom = () -> "user-1";

        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(YghMybatisAutoConfiguration.class))
            .withBean(AuditorProvider.class, () -> custom)
            .run(context -> assertThat(context.getBean(AuditorProvider.class)).isSameAs(custom));
    }

    @Test
    void refusesToStartWithoutAnExplicitAuditorProvider() {
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(YghMybatisAutoConfiguration.class))
            .run(context -> assertThat(context.getStartupFailure())
                .hasMessageContaining("AuditorProvider"));
    }

    @Test
    void rejectsCustomInterceptorThatRemovesBoundedPagination() {
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(YghMybatisAutoConfiguration.class))
            .withBean(AuditorProvider.class, AuditorProvider::system)
            .withBean(MybatisPlusInterceptor.class, MybatisPlusInterceptor::new)
            .run(context -> assertThat(context.getStartupFailure())
                .hasMessageContaining(
                    "MybatisPlusInterceptor must include pagination limited to 100"));
    }

    @Test
    void rejectsZeroLimitAndOverflowPagination() {
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(YghMybatisAutoConfiguration.class))
            .withBean(AuditorProvider.class, AuditorProvider::system)
            .withBean(MybatisPlusInterceptor.class, () -> interceptor(0L, false))
            .run(context -> assertGuardRejected(context.getStartupFailure()));

        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(YghMybatisAutoConfiguration.class))
            .withBean(AuditorProvider.class, AuditorProvider::system)
            .withBean(MybatisPlusInterceptor.class, () -> interceptor(100L, true))
            .run(context -> assertGuardRejected(context.getStartupFailure()));
    }

    @Test
    void rejectsMultiplePaginationInterceptors() {
        var interceptor = interceptor(100L, false);
        var duplicate = new PaginationInnerInterceptor();
        duplicate.setMaxLimit(100L);
        duplicate.setOverflow(false);
        interceptor.addInnerInterceptor(duplicate);

        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(YghMybatisAutoConfiguration.class))
            .withBean(AuditorProvider.class, AuditorProvider::system)
            .withBean(MybatisPlusInterceptor.class, () -> interceptor)
            .run(context -> assertGuardRejected(context.getStartupFailure()));
    }

    @Test
    void autoConfigurationIsPublishedThroughBootFourImports() throws IOException {
        var path = "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports";

        try (var stream = getClass().getClassLoader().getResourceAsStream(path)) {
            assertThat(stream).isNotNull();
            assertThat(new String(stream.readAllBytes()))
                .contains(YghMybatisAutoConfiguration.class.getName());
        }
    }
}
