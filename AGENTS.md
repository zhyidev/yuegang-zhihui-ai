# 粤港甄选 (yuegang-zhihui-ai) — AI 代理协作指南

多模块 Maven 项目，Java 25 + Spring Boot 4.0.7 + Spring Cloud Gateway，面向 C 端电商业务。

## 构建环境

- **JDK 25**：`/usr/java/default`（Oracle JDK 25.0.3）
- **Maven 3.9.16**：`/opt/apache-maven-3.9.16/bin/mvn`（enforcer 强制 [3.9.16,4.0.0)，系统自带的 3.9.9 不满足）
- **编译命令**：
  ```bash
  export JAVA_HOME=/usr/java/default
  export PATH=$JAVA_HOME/bin:$PATH
  cd /home/zzy/IdeaProjects/yuegang-zhihui-ai
  /opt/apache-maven-3.9.16/bin/mvn -pl <模块路径> -am compile -q
  ```
- **enforcer 规则**：Java [25,26)、Maven [3.9.16,4.0.0)、禁止重复依赖声明、Release 构建时不允许 SNAPSHOT 依赖
- **JaCoCo 门槛**：行覆盖 ≥70%、分支覆盖 ≥60%（gateway 模块分支 ≥90%）

## 模块结构

```
yuegang-zhihui-ai/
├── ygh-dependencies/          # BOM — 统一声明所有第三方版本号
├── ygh-common/                # 公共基础层
│   ├── ygh-common-core/       #   ApiResponse / ErrorCode / PageRequest / PageResponse
│   ├── ygh-common-security/   #   SessionRevocationStore / 权限注解 / 内部服务签名
│   ├── ygh-common-redis/      #   RedisKeyBuilder / 分布式锁 / 会话状态存储（自动配置）
│   └── ygh-common-mybatis/    #   Flyway 迁移策略 / MyBatis-Plus 分页适配
├── ygh-platform/              # 平台服务层
│   ├── ygh-gateway/           #   Spring Cloud Gateway（WebFlux 响应式）
│   └── ygh-auth-service/      #   认证服务（开发中）
├── ygh-applications/          # 应用启动入口（待开发）
├── ygh-tests/                 # 集成测试（待开发）
└── ygh-deploy/                # 部署配置
```

## 技术栈版本

| 组件 | 版本 |
|------|------|
| Spring Boot | 4.0.7 |
| Spring Cloud | 2025.1.2 |
| Spring Cloud Alibaba | 2025.1.0.0 |
| MyBatis-Plus | 3.5.16（`mybatis-plus-spring-boot4-starter`） |
| Springdoc OpenAPI | 3.0.3 |

## Spring Boot 4 陷阱（重要）

### Jackson 3 包名迁移

Spring Boot 4 升级到 Jackson 3.x，包名从 `com.fasterxml.jackson` 改为 `tools.jackson`：

```java
// ❌ 旧（Spring Boot 3.x / Jackson 2.x）
import com.fasterxml.jackson.databind.ObjectMapper;

// ✅ 新（Spring Boot 4.x / Jackson 3.x）
import tools.jackson.databind.ObjectMapper;
```

Spring Boot 4 自动注册的 `ObjectMapper` Bean 是 `tools.jackson.databind.ObjectMapper`。如果 import 旧包名，会报 "No beans of 'ObjectMapper' type found"。

`jackson-annotations` 仍然是 `com.fasterxml.jackson.core`（没变），只有 `jackson-databind` 和 `jackson-core` 变成了 `tools.jackson.core`。

### 自动配置注册

Spring Boot 4 用 `AutoConfiguration.imports` 文件注册自动配置类：

```
ygh-common-redis/src/main/resources/META-INF/spring/
  org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

内容为自动配置类的全限定名，每行一个。`@AutoConfiguration` 注解的类必须在此文件注册才能被 Spring Boot 扫描到。

## Redis 约定

### Key 命名规范

`RedisKeyBuilder` 强制所有 key 遵循格式：`ygh{environment}:{service}:{business}:{identifier}`

- 5 段，冒号分隔，第一段固定 `ygh`
- environment/service/business：小写字母数字开头，允许中划线，≤32 字符
- identifier：字母数字开头，允许 `._-`，≤128 字符
- 示例：`yghdev:auth:session:user123`

### 会话相关 Key

`SessionRedisKeys` 在 key 中加入 `{accountId}` Hash Tag，确保同一用户的 key 落在同一 Redis slot：

```
ygh:{env}:auth:{accountId}:session:{jwtId}
ygh:{env}:auth:{accountId}:revoked:{jwtId}
ygh:{env}:auth:{accountId}:account-state:current
```

### 自动配置

`YghRedisAutoConfiguration` 注册了分布式锁、会话状态存储等 Bean。响应式组件（`ReactiveRedisSessionValidator`）以 `@ConditionalOnBean(ReactiveStringRedisTemplate.class)` 为条件，仅在 WebFlux 环境下激活。

## Flyway 迁移策略

`FlywayMigrationPolicy` 强制代码库级别的"仅向前"迁移：

- **命名格式**：`db/migration/V<正整数>_<小写snake_case>.sql`（如 `V1__create_user_table.sql`）
- **禁止**：Undo 脚本（`U` 前缀）、Repeatable 脚本（`R__` 前缀）、Java 迁移
- **版本号**：必须单调递增，新脚本版本号必须大于已应用的最高版本号
- **路径穿越**：禁止 `../` 路径
- `YghFlywayMigrationStrategy` 拦截 Spring 自动迁移，先校验命名/版本/配置安全，再委托 Flyway 执行

## Gateway 模块

- **Web 类型**：`spring.main.web-application-type: reactive`（WebFlux，不可改）
- **安全**：OAuth2 Resource Server + JWT（RS256），JWK Set URI 从配置读取
- **Session 校验**：`JwtSessionValidationFilter` 通过 `ReactiveSessionValidator` 查 Redis 黑名单
- **错误响应**：`GatewaySecurityErrorWriter` 统一 JSON 错误格式，已标注 `@Component`
- **配置开关**：`ygh.security.session-validation.enabled`（默认 true）

## 编码约定

- record 类用于不可变 DTO/值对象（`MigrationDescriptor`、`MigrationViolation` 等）
- `@ConditionalOnProperty` 控制 Bean 的条件加载
- 构造器注入优先，不用 `@Autowired` 字段注入
- 包扫描：`@SpringBootApplication` 默认从启动类所在包开始
- 禁止 `print()` / `System.out`，用 SLF4J logger

## 常见问题排查

| 症状 | 原因 | 解决 |
|------|------|------|
| `No beans of 'ObjectMapper' type found` | import 了 `com.fasterxml.jackson` 旧包名 | 改为 `tools.jackson.databind.ObjectMapper` |
| `No beans of 'XxxWriter' type found` | 类没有 `@Component` 或没有 `@Bean` 方法 | 加 `@Component` 或在 `@Configuration` 类注册 `@Bean` |
| `No beans of 'ReactiveSessionValidator' type found` | `@ConditionalOnBean(ReactiveStringRedisTemplate.class)` 条件不满足 | 确认依赖链上存在 reactive Redis |
| enforcer 报 Maven 版本不对 | 系统 Maven 3.9.9 < 3.9.16 | 用 `/opt/apache-maven-3.9.16/bin/mvn` |
| enforcer 报 Java 版本不对 | 默认 JDK 21 < 25 | `export JAVA_HOME=/usr/java/default` |
| `AutoConfiguration` 类不被扫描 | 缺少 `AutoConfiguration.imports` 注册文件 | 在 `META-INF/spring/` 下创建 |

## 提交规范

```
type(模块): 简述

可选正文。
```

类型：`feat` / `fix` / `refactor` / `docs` / `chore` / `test`

示例：
```
feat(common-mybatis): 实现 Flyway 仅向前迁移策略
fix(gateway): 修复 GatewaySecurityErrorWriter 未注册为 Bean
```
