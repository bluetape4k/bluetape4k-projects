---
title: Auto-configuration and ownership boundaries
description: Apply the Spring Boot 4.1 MongoDB namespace, legacy-key guard, fallback ordering, and ownership boundaries used by the 2.0 line.
manualId: bluetape4k-spring-boot-mongodb
chapterId: auto-configuration-boundaries
---

# Auto-configuration and ownership boundaries

> Contract scope: **2.0.0 current contract** on `develop`. The stable rollback
> reference remains [1.12.1](https://github.com/bluetape4k/bluetape4k-projects/releases/tag/1.12.1).

## What the auto-configuration does

The module's `AutoConfiguration.imports` registers only
`ReactiveMongoAutoConfiguration`. It runs after Spring Boot's
`DataMongoReactiveAutoConfiguration`, requires `ReactiveMongoOperations` on the
classpath, and backs off when an operations bean already exists.

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

It does not create `ReactiveMongoDatabaseFactory` or `MongoConverter`. The
fallback can be completed only when both beans are already available.

## Property migration and precedence

Spring Boot 4.1 binds MongoDB connection settings under `spring.mongodb.*`.
Use the current URI key:

```yaml
spring:
  mongodb:
    uri: mongodb://127.0.0.1:27018/synthetic
```

| Properties present | Result when the library fallback participates |
| --- | --- |
| `spring.mongodb.uri` only | Use the current Spring Boot 4.1 namespace |
| `spring.data.mongodb.uri` only | Fail at startup instead of silently using localhost |
| Both keys | `spring.mongodb.uri` takes precedence |

The legacy-only failure is:

```text
IllegalStateException: Unsupported legacy MongoDB property 'spring.data.mongodb.uri'; use 'spring.mongodb.uri' on Spring Boot 4.1+
```

Use synthetic URIs in tests and keep credentials out of logs and diagnostic
artifacts.

## Back-off and ownership

Spring Boot's reactive MongoDB auto-configuration normally supplies
`ReactiveMongoOperations`. The class-level `@ConditionalOnMissingBean` then
backs off the entire bluetape4k configuration, including the legacy-property
guard. The same rule applies when the application provides its own operations
bean. Adding this artifact therefore does not create a second client or pool.
Use the condition evaluation report to inspect the actual bean graph.

## Responsibility map

| Layer | Owned responsibilities |
| --- | --- |
| bluetape4k module | Coroutine extensions, query DSL, template fallback |
| Spring Boot and Spring Data | Property binding, factory, mapping converter, template, lifecycle integration |
| Application | Custom conversions, auditing, index policy, transactions, domain repositories |
| MongoDB driver | Connection pool, server selection, protocol, sessions, concerns, timeouts |

The presence of MongoDB Kotlin driver dependencies in the build does not move driver configuration into bluetape4k auto-configuration.

## Custom conversion and auditing

The current module does not create a `MongoCustomConversions` bean or enable
MongoDB auditing. Applications that need either feature should use a separate
configuration.

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
- [`ReactiveMongoAutoConfigurationTest.kt` on `develop`](https://github.com/bluetape4k/bluetape4k-projects/blob/develop/spring-boot/mongodb/src/test/kotlin/io/bluetape4k/spring/mongodb/ReactiveMongoAutoConfigurationTest.kt)
- [Module build](../../../../../spring-boot/mongodb/build.gradle.kts)

## Next chapter

Use the configured template in [Coroutine reads and cardinality](./coroutine-reads-cardinality.md).
