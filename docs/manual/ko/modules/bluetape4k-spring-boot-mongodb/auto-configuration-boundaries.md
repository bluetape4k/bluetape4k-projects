---
title: 자동 설정과 구성 경계
description: ReactiveMongoTemplate fallback이 만들어지는 조건과 Spring Boot, 애플리케이션, MongoDB driver의 책임을 구분합니다.
manualId: bluetape4k-spring-boot-mongodb
chapterId: auto-configuration-boundaries
---

# 자동 설정과 구성 경계

## 자동 설정이 실제로 하는 일

이 모듈의 `AutoConfiguration.imports`에는 `ReactiveMongoAutoConfiguration` 하나만 등록됩니다. 이 구성은 classpath에 `ReactiveMongoOperations`가 있고 같은 타입의 bean이 없을 때 `ReactiveMongoTemplate`을 만듭니다.

```kotlin
@AutoConfiguration
@ConditionalOnClass(ReactiveMongoOperations::class)
class ReactiveMongoAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(ReactiveMongoOperations::class)
    fun reactiveMongoTemplate(
        databaseFactory: ReactiveMongoDatabaseFactory,
        mongoConverter: MongoConverter,
    ): ReactiveMongoTemplate =
        ReactiveMongoTemplate(databaseFactory, mongoConverter)
}
```

`ReactiveMongoDatabaseFactory`와 `MongoConverter`를 새로 만드는 구성은 아닙니다. 두 bean이 없다면 이 fallback도 완성될 수 없습니다.

## 일반적인 Spring Boot 애플리케이션

Spring Boot MongoDB reactive auto-configuration이 정상적으로 적용되면 이미 `ReactiveMongoOperations`가 있습니다. 그러면 `@ConditionalOnMissingBean` 때문에 bluetape4k fallback은 물러납니다. 애플리케이션이 자체 `ReactiveMongoTemplate`을 등록해도 같은 원칙이 적용됩니다.

따라서 이 artifact를 추가했다고 client나 pool이 하나 더 생기지는 않습니다. 실제 bean graph는 condition evaluation report로 확인합니다.

## 책임 분리

| 계층 | 소유하는 것 |
| --- | --- |
| bluetape4k 모듈 | `ReactiveMongoOperations` coroutine 확장, query DSL, template fallback |
| Spring Boot·Spring Data | property binding, factory, mapping converter, template, lifecycle integration |
| 애플리케이션 | custom conversions, auditing, index 정책, transaction 경계, domain repository |
| MongoDB driver | connection pool, server selection, protocol, session, read/write concern, timeout |

모듈 build에 MongoDB Kotlin driver가 포함돼 있어도 bluetape4k 자동 설정이 driver 설정을 대신 소유하는 것은 아닙니다.

## Custom conversion과 auditing

1.12.1 소스에는 `MongoCustomConversions` bean이나 `@EnableMongoAuditing`이 없습니다. 필요한 애플리케이션은 별도 configuration을 둡니다.

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
- [`MongoTestApplication.kt`](../../../../../spring-boot/mongodb/src/test/kotlin/io/bluetape4k/spring/mongodb/MongoTestApplication.kt)
- [모듈 build](../../../../../spring-boot/mongodb/build.gradle.kts)

## 다음 읽을 장

[Coroutine 조회와 cardinality](./coroutine-reads-cardinality.md)에서 template을 실제 조회에 사용합니다.
