package com.yuegang.zhihui.user.application;

import com.yuegang.zhihui.user.domain.AddressRepository;
import com.yuegang.zhihui.user.domain.UserProfileRepository;
import com.yuegang.zhihui.user.infrastructure.AddressCipher;
import com.yuegang.zhihui.user.infrastructure.JdbcAddressRepository;
import com.yuegang.zhihui.user.infrastructure.JdbcUserProfileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.time.Clock;

/**
 * Spring配置类，配置用户和地址相关的 Bean
 */
@Configuration(proxyBeanMethods = false) // 声明为轻量级配置类
class UserProfileConfiguration { // 用户档案模块配置类
    @Bean
    UserProfileRepository userProfileRepository(DataSource dataSource, AddressCipher cipher) {
        return new JdbcUserProfileRepository(dataSource, cipher); //注册资料仓库
    }

    @Bean
    UserProfileService userProfileService(UserProfileRepository repository) {
        return new UserProfileService(repository); //注册用户档案服务
    }

    @Bean
    AddressCipher addressCipher(@Value("${ygh.user.pii-key-base64}") String key,
                                @Value("${ygh.user-pii-key-version:1}") int version) {
        return new AddressCipher(key, version);
    }

    @Bean
    AddressRepository addressRepository(DataSource dataSource, AddressCipher cipher) {
        return new JdbcAddressRepository(dataSource, cipher); // 注册地址仓储
    }

    @Bean
    UserIdGenerator userIdGenerator(@Value("${ygh.yser.id-worker:2}") long worker) {
        return new UserIdGenerator(worker, Clock.systemUTC()); // 注册 ID 生成器，默认机器码2
    }

    @Bean
    AddressService addressService(AddressRepository repository, UserIdGenerator ids) {
        return new AddressService(repository, ids); // 注册地址服务
    }

    @Bean
    OrganizationService organizationService(DataSource dataSource, UserIdGenerator ids) {
        return new OrganizationService(dataSource, ids);
    }
}
