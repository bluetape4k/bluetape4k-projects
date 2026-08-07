---
title: Auto-configuration and ownership boundaries
description: Distinguish the ReactiveMongoTemplate fallback from responsibilities owned by Spring Boot, the application, and the MongoDB driver.
manualId: bluetape4k-spring-boot-mongodb
chapterId: auto-configuration-boundaries
---

# Auto-configuration and ownership boundaries

## What the auto-configuration does

The module's `AutoConfiguration.imports` registers only `ReactiveMongoAutoConfiguration`. It creates `ReactiveMongoTemplate` when `ReactiveMongoOperations` is on the classpath and no bean of that type exists.

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

It does not create the `ReactiveMongoDatabaseFactory` or `MongoConverter`. The fallback cannot be completed unless both beans are already available.

## A typical Spring Boot application

Spring Boot's reactive MongoDB auto-configuration normally supplies `ReactiveMongoOperations`. The bluetape4k fallback then backs off because of `@ConditionalOnMissingBean`. It also backs off when the application registers its own `ReactiveMongoTemplate`.

Adding this artifact therefore does not create a second client or pool. Use the condition evaluation report to inspect the actual bean graph.

## Responsibility map

| Layer | Owned responsibilities |
| --- | --- |
| bluetape4k module | Coroutine extensions, query DSL, template fallback |
| Spring Boot and Spring Data | Property binding, factory, mapping converter, template, lifecycle integration |
| Application | Custom conversions, auditing, index policy, transactions, domain repositories |
| MongoDB driver | Connection pool, server selection, protocol, sessions, concerns, timeouts |

The presence of MongoDB Kotlin driver dependencies in the build does not move driver configuration into bluetape4k auto-configuration.

## Custom conversion and auditing

The 1.12.1 source has no `MongoCustomConversions` bean and does not enable MongoDB auditing. Applications that need them should use a separate configuration.

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

A converter changes the stored wire format. Verify that old and new application versions can read the same documents before a rolling deployment.

## Source and tests

- [`ReactiveMongoAutoConfiguration.kt`](../../../../../spring-boot/mongodb/src/main/kotlin/io/bluetape4k/spring/mongodb/config/ReactiveMongoAutoConfiguration.kt)
- [`AutoConfiguration.imports`](../../../../../spring-boot/mongodb/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports)
- [`MongoTestApplication.kt`](../../../../../spring-boot/mongodb/src/test/kotlin/io/bluetape4k/spring/mongodb/MongoTestApplication.kt)
- [Module build](../../../../../spring-boot/mongodb/build.gradle.kts)

## Next chapter

Use the configured template in [Coroutine reads and cardinality](./coroutine-reads-cardinality.md).
