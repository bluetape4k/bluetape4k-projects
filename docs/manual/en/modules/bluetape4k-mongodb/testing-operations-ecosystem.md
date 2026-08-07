---
title: Testing, operations, and ecosystem paths
description: Separate server-free helper tests from Testcontainers integration evidence and continue from the MongoDB driver boundary to Spring Data MongoDB.
manualId: bluetape4k-mongodb
chapterId: testing-operations-ecosystem
---

# Testing, operations, and ecosystem paths

## Contracts without a server

`DocumentExtensionsTest` covers pair and builder construction, null values, reified safe casts, and type mismatches. `AggregationSupportTest` inspects stage BSON structure.

Both run quickly without Docker. They are the first unit contracts to inspect when reviewing helper behavior or manual examples.

## Tests that require MongoDB

Client, database, collection, and example tests extend `AbstractMongoTest`. The fixture lazily starts a `MongoDBServer.Launcher.mongoDB` Testcontainer, creates a coroutine client, and closes it after the test class.

```bash
./gradlew :bluetape4k-mongodb:test --no-configuration-cache
```

Run this suite sequentially with other Testcontainers modules. Parallel image setup, ports, and shared machine resources can obscure failure causes.

## The fixture is not a published API

The README test-support example mentions `AbstractMongoTest`, but the class lives under `data/mongodb/src/test`. Consumers of the main artifact cannot import it as a production API.

Applications should own a test fixture that starts MongoDB and connects a coroutine `MongoClient`. Do not confuse a Testcontainers helper's sync client with the coroutine client type.

## Boundaries of 1.12.1 test coverage

Release tests verify these behaviors against MongoDB:

- direct client creation and provider identity for equal keys
- database and collection name collection
- `findFirst`, `exists`, `upsert`, filter, sort, skip, and limit
- native suspend CRUD combined with helpers
- representative aggregation results

They do not directly cover transaction commit-abort-cancellation, long-term provider cache growth, custom codecs, authentication and TLS, replica-set failover, or rolling schema compatibility.

## Add these application tests

1. Create clients with production-like credentials, TLS, and codec registries.
2. Decode application types and existing documents in both directions.
3. Verify pool and session cleanup after cancellation and timeout.
4. Test commit, abort, and transient errors on a replica-set fixture when using transactions.
5. Verify stable pagination under equal sort values and concurrent inserts.
6. Cross-read documents between old and new application versions.

## Operational metrics and failure classes

Separate MongoDB command failures, codec failures, pool-wait timeouts, server-selection timeouts, and application cancellation. Keep operation, database, and collection labels bounded; never use document IDs or filter values as metric labels.

If decode failures rise after a deployment, inspect codecs, field renames, and stored BSON types instead of classifying everything as a connection incident. Include suppressed transaction-abort failures in error reports.

## Ecosystem learning path

- Coroutine-driver client, collection, and BSON helpers: this manual
- Spring Boot auto-configuration, reactive operations, and Criteria DSL: [`bluetape4k-spring-boot-mongodb`](../bluetape4k-spring-boot-mongodb.md)
- Coroutine cancellation and Flow design: [`bluetape4k-coroutines`](../bluetape4k-coroutines.md)
- Shared Testcontainers foundation: [`bluetape4k-testcontainers`](../bluetape4k-testcontainers.md)

Spring Data MongoDB adds mapping, repositories, and exception translation, but low-level driver responsibilities remain. Understand client ownership and codec and query costs before adding the framework layer.

## Sources and tests

- [`AbstractMongoTest.kt`](../../../../../data/mongodb/src/test/kotlin/io/bluetape4k/mongodb/AbstractMongoTest.kt)
- [`MongoClientSupportTest.kt`](../../../../../data/mongodb/src/test/kotlin/io/bluetape4k/mongodb/MongoClientSupportTest.kt)
- [`MongoCollectionExtensionsTest.kt`](../../../../../data/mongodb/src/test/kotlin/io/bluetape4k/mongodb/MongoCollectionExtensionsTest.kt)
- [`DocumentExtensionsTest.kt`](../../../../../data/mongodb/src/test/kotlin/io/bluetape4k/mongodb/bson/DocumentExtensionsTest.kt)
- [`AggregationExamples.kt`](../../../../../data/mongodb/src/test/kotlin/io/bluetape4k/mongodb/examples/AggregationExamples.kt)
- [`BasicCrudExamples.kt`](../../../../../data/mongodb/src/test/kotlin/io/bluetape4k/mongodb/examples/BasicCrudExamples.kt)
