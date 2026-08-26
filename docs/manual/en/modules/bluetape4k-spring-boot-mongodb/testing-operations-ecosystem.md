---
title: Testing, operations, and ecosystem
description: Plan MongoDB Testcontainers integration tests, operational observation, and the choice between Spring Data and native driver APIs.
manualId: bluetape4k-spring-boot-mongodb
chapterId: testing-operations-ecosystem
---

# Testing, operations, and ecosystem

## Current test structure (Spring Boot 4.1+)

`AbstractReactiveMongoTest` lazily starts a `MongoDBServer` Testcontainer and registers `spring.mongodb.uri` dynamically. `spring.data.mongodb.uri` is the legacy namespace on Spring Boot 4.1+ and fails fast when it is the only URI key. `AbstractReactiveMongoCoroutineTest` adds a test scope with `Dispatchers.IO` and a `CoroutineName`.

`ReactiveMongoOperationsCoroutinesTest` uses a real Spring Boot context and MongoDB to verify:

- insert and save;
- Flow, optional, and required reads;
- count and exists;
- update-first, update-multi, and upsert;
- remove, find-and-modify, and find-and-remove;
- distinct and aggregation;
- collection existence, creation, and removal.

The three query-DSL test classes compare BSON structures without a server. Run the fast DSL checks first, then serialize the Docker-backed suite with other Testcontainers work.

## Isolate test data

The integration suite uses the dedicated `test_users` collection. Give each test a unique business key such as email and remove only documents created by that test. Assertions against a shared collection's absolute count are sensitive to execution order and parallelism.

```bash
./gradlew :bluetape4k-spring-boot-mongodb:test \
    --no-build-cache --no-configuration-cache
```

## Operational signals

The module adds no metrics, so use Spring Boot and MongoDB driver observation.

| Signal | Possible problem |
| --- | --- |
| Server-selection time | Topology change, DNS/network issues, no primary |
| Pool wait and in-use connections | Pool exhaustion, long query, slow Flow consumer |
| Command latency and error code | Missing index, timeout, duplicate key, write concern |
| Cancellation and request latency | Client disconnect, deadline, excessive result size |
| Active subscriptions during shutdown | Tailable cursor or coroutine scope not closed |

## Choose the MongoDB layer

| Need | Starting point |
| --- | --- |
| Spring Data mapping, `ReactiveMongoOperations`, and coroutines | This module |
| Spring Data repository interface | Spring Data reactive/coroutine repository |
| Direct MongoDB Kotlin driver, codec, and collection control | [`bluetape4k-mongodb`](../bluetape4k-mongodb.md) |
| Spring Boot client, properties, and health configuration | Spring Boot MongoDB auto-configuration |

Both bluetape4k modules can be present, but avoid casually mixing Spring Data mapping and native codec mapping in one repository. Assign document wire format and transaction/session ownership to one clear layer.

## Source and tests

- [`AbstractReactiveMongoTest.kt`](../../../../../spring-boot/mongodb/src/test/kotlin/io/bluetape4k/spring/mongodb/AbstractReactiveMongoTest.kt)
- [`AbstractReactiveMongoCoroutineTest.kt`](../../../../../spring-boot/mongodb/src/test/kotlin/io/bluetape4k/spring/mongodb/AbstractReactiveMongoCoroutineTest.kt)
- [`MongoTestApplication.kt`](../../../../../spring-boot/mongodb/src/test/kotlin/io/bluetape4k/spring/mongodb/MongoTestApplication.kt)
- [`ReactiveMongoOperationsCoroutinesTest.kt`](../../../../../spring-boot/mongodb/src/test/kotlin/io/bluetape4k/spring/mongodb/coroutines/ReactiveMongoOperationsCoroutinesTest.kt)
- [`User.kt`](../../../../../spring-boot/mongodb/src/test/kotlin/io/bluetape4k/spring/mongodb/model/User.kt)

## Next step

After choosing the layer, return to [Auto-configuration and ownership boundaries](./auto-configuration-boundaries.md) and pin the beans and settings owned by the application.
