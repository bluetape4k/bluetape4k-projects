---
title: Testing and failure modes
description: Separate fast context tests from Redis integration tests and verify conditions, cache cycles, and degraded observation.
manualId: bluetape4k-spring-boot-hibernate-lettuce
chapterId: testing-and-failure-modes
---

# Testing and failure modes

## Test in two layers

Combining property conversion and real cache behavior in one test obscures failures. The 1.12.1 module separates them.

| Test | External resources | Contract |
| --- | --- | --- |
| `LettuceNearCacheAutoConfigurationTest` | None | Conditions, defaults, mapping, bean registration |
| `LettuceNearCachePropertiesCustomizerTest` | None | Multiple region TTLs and omitted metrics keys |
| `LettuceNearCacheIntegrationTest` | Redis Testcontainer + H2 | Entity reads, L2 miss/put/hit, endpoint, gauges, concurrent reads |

Run the context tests during normal editing and the container test after changing provider or integration configuration.

```bash
./gradlew :bluetape4k-spring-boot-hibernate-lettuce:test \
  --tests '*LettuceNearCacheAutoConfigurationTest' \
  --no-build-cache --no-configuration-cache
```

## Lock condition contracts

At minimum, verify:

- one `HibernatePropertiesCustomizer` exists with defaults;
- top-level `enabled=false` removes it;
- disabling metrics removes binder and statistics properties;
- a `MeterRegistry` causes one binder to register;
- an `EntityManagerFactory` causes the endpoint to register.

These checks isolate classpath and property failures before Redis connectivity enters the test.

## Verify a real cache cycle

The integration test clears Hibernate statistics, saves an entity, then reads it twice outside a transaction. The first read creates an L2 miss and put; the second should hit. Two reads inside the same persistence context can be served by first-level cache and do not prove L2 behavior.

```kotlin
sessionFactory.statistics.clear()

repository.findById(id) // L2 miss -> database -> put
repository.findById(id) // L2 hit
```

Insert callbacks may also contribute puts, so the test checks at least one rather than overfitting an exact counter.

## Interpret failures

- No customizer: inspect `enabled`, classpath, and auto-configuration imports.
- Wrong RegionFactory: inspect final Hibernate properties and other customizers.
- Empty endpoint: inspect factory unwrap, actual RegionFactory type, and region creation.
- Null L2 fields: inspect `hibernate.generate_statistics`.
- Missing gauge: inspect `MeterRegistry`, metrics conditions, and binder warnings.
- Repeated database queries: inspect transaction boundaries, entity annotations, region names, and statistics.

Observation configuration degrades failures and may continue startup. Redis connectivity and serialization failures occur in the lower provider path; do not assume they are converted into empty endpoint results.

## Integration fixture caveat

On the narrow 1.12.1 test classpath, Spring Boot 4 split auto-configuration can register duplicate JPA/JDBC beans, so the integration fixture explicitly excludes several framework defaults. This is a test-fixture constraint, not configuration to copy into a normal application.

Run Testcontainers suites sequentially with other database and Redis tests to avoid shared Docker and memory contention.

## Executable evidence

- [`LettuceNearCacheAutoConfigurationTest.kt`](../../../../../spring-boot/hibernate-lettuce/src/test/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheAutoConfigurationTest.kt)
- [`LettuceNearCachePropertiesCustomizerTest.kt`](../../../../../spring-boot/hibernate-lettuce/src/test/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCachePropertiesCustomizerTest.kt)
- [`LettuceNearCacheIntegrationTest.kt`](../../../../../spring-boot/hibernate-lettuce/src/test/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheIntegrationTest.kt)
- [`ReadmeDependencyContractTest.kt`](../../../../../spring-boot/hibernate-lettuce/src/test/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/ReadmeDependencyContractTest.kt)
