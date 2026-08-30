---
title: 자동 설정과 구성 경계
description: 2.0 계열이 사용하는 Spring Boot 4.1 MongoDB namespace, legacy key 검사, fallback 순서와 책임 경계를 설명합니다.
manualId: bluetape4k-spring-boot-mongodb
chapterId: auto-configuration-boundaries
---

# 자동 설정과 구성 경계

> 계약 범위: `develop`의 **2.0.0 current contract**입니다. 안정 릴리스 rollback
> 기준은 [1.12.1](https://github.com/bluetape4k/bluetape4k-projects/releases/tag/1.12.1)입니다.

## 자동 설정이 실제로 하는 일

이 모듈의 `AutoConfiguration.imports`에는 `ReactiveMongoAutoConfiguration`
하나만 등록됩니다. 이 구성은 Spring Boot의
`DataMongoReactiveAutoConfiguration` 이후에 실행되며, classpath에
`ReactiveMongoOperations`가 있고 operations Bean이 없을 때만 활성화됩니다.

```kotlin
@AutoConfiguration(
    afterName = [
        "org.springframework.boot.data.mongodb.autoconfigure.DataMongoReactiveAutoConfiguration",
    ],
)
@ConditionalOnClass(ReactiveMongoOperations::class)
@ConditionalOnMissingBean(ReactiveMongoOperations::class)
class ReactiveMongoAutoConfiguration : EnvironmentAware {
    override fun setEnvironment(environment: Environment) {
        if (environment.containsProperty("spring.data.mongodb.uri") &&
            !environment.containsProperty("spring.mongodb.uri")
        ) {
            throw IllegalStateException(
                "Unsupported legacy MongoDB property 'spring.data.mongodb.uri'; " +
                    "use 'spring.mongodb.uri' on Spring Boot 4.1+",
            )
        }
    }

    @Bean
    fun reactiveMongoTemplate(
        databaseFactory: ReactiveMongoDatabaseFactory,
        mongoConverter: MongoConverter,
    ): ReactiveMongoTemplate =
        ReactiveMongoTemplate(databaseFactory, mongoConverter)
}
```

`ReactiveMongoDatabaseFactory`와 `MongoConverter`를 새로 만드는 구성은
아닙니다. 두 Bean이 이미 있을 때만 fallback을 완성할 수 있습니다.

## Property migration과 우선순위

Spring Boot 4.1은 MongoDB 연결 설정을 `spring.mongodb.*`에 바인딩합니다.
URI는 현재 key로 설정합니다.

```yaml
spring:
  mongodb:
    uri: mongodb://127.0.0.1:27018/synthetic
```

| 설정된 property | library fallback이 참여할 때의 결과 |
| --- | --- |
| `spring.mongodb.uri`만 있음 | Spring Boot 4.1 namespace 사용 |
| `spring.data.mongodb.uri`만 있음 | localhost 기본값을 사용하지 않고 startup 실패 |
| 두 key가 모두 있음 | `spring.mongodb.uri` 우선 |

legacy key만 남아 있으면 다음 예외가 발생합니다.

```text
IllegalStateException: Unsupported legacy MongoDB property 'spring.data.mongodb.uri'; use 'spring.mongodb.uri' on Spring Boot 4.1+
```

테스트에는 synthetic URI를 사용하고 credential을 로그와 진단 artifact에
남기지 않습니다.

## Back-off와 책임 경계

Spring Boot의 reactive MongoDB 자동 구성이 `ReactiveMongoOperations` Bean을
제공하면 class-level `@ConditionalOnMissingBean`이 legacy property 검사를 포함한
bluetape4k 구성 전체를 back-off합니다. 애플리케이션이 자체 operations Bean을
제공해도 같습니다. 따라서 이 artifact를 추가해도 client나 pool이 중복으로
생기지 않습니다. 실제 Bean graph는 condition evaluation report로 확인합니다.

## 책임 분리

| 계층 | 소유하는 것 |
| --- | --- |
| bluetape4k 모듈 | `ReactiveMongoOperations` coroutine 확장, query DSL, template fallback |
| Spring Boot·Spring Data | property binding, factory, mapping converter, template, lifecycle integration |
| 애플리케이션 | custom conversions, auditing, index 정책, transaction 경계, domain repository |
| MongoDB driver | connection pool, server selection, protocol, session, read/write concern, timeout |

모듈 build에 MongoDB Kotlin driver가 포함돼 있어도 bluetape4k 자동 설정이 driver 설정을 대신 소유하는 것은 아닙니다.

## Custom conversion과 auditing

현재 모듈은 `MongoCustomConversions` Bean을 만들거나 MongoDB auditing을
활성화하지 않습니다. 필요한 애플리케이션은 별도 configuration을 둡니다.

```kotlin
@Configuration(proxyBeanMethods = false)
@EnableMongoAuditing
class MongoDomainConfiguration {
    @Bean
    fun mongoCustomConversions(): MongoCustomConversions =
        MongoCustomConversions.create { adapter ->
            adapter.registerConverter(MoneyWriteConverter())
        }
}
```

converter는 저장 wire format을 바꿉니다. rolling deployment에서 구·신 애플리케이션이 같은 document를 읽을 수 있는지 검증한 뒤 적용합니다.

## Source와 tests

- [`ReactiveMongoAutoConfiguration.kt`](../../../../../spring-boot/mongodb/src/main/kotlin/io/bluetape4k/spring/mongodb/config/ReactiveMongoAutoConfiguration.kt)
- [`AutoConfiguration.imports`](../../../../../spring-boot/mongodb/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports)
- [`develop`의 `ReactiveMongoAutoConfigurationTest.kt`](https://github.com/bluetape4k/bluetape4k-projects/blob/develop/spring-boot/mongodb/src/test/kotlin/io/bluetape4k/spring/mongodb/ReactiveMongoAutoConfigurationTest.kt)
- [모듈 build](../../../../../spring-boot/mongodb/build.gradle.kts)

## 다음 읽을 장

[Coroutine 조회와 cardinality](./coroutine-reads-cardinality.md)에서 template을 실제 조회에 사용합니다.
