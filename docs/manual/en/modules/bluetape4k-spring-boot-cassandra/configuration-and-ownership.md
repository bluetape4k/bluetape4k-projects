---
title: "Configuration and object ownership"
description: "Identify what is not auto-configured and assign ownership of CqlSession, templates, and repositories."
---

# Configuration and object ownership

## What the artifact does not configure

Despite `spring-boot` in its name, the 1.12.1 source has no auto-configuration class, `AutoConfiguration.imports`, `@ConfigurationProperties`, or main resources. Adding the artifact to the classpath does not create a bean or a property namespace.

The module provides extensions for Spring Data Cassandra objects that already exist. Spring Boot or application configuration must supply:

- `CqlSession` and `ReactiveSession`
- `CassandraOperations`, `ReactiveCassandraOperations`, and `AsyncCassandraOperations`
- `CqlOperations`, `ReactiveCqlOperations`, and `AsyncCqlOperations`
- the mapping context, converters, and repository infrastructure

## The application owns CqlSession

`ReactiveSession.executeSuspending` and `prepareSuspending` only call `execute` or `prepare` on their receiver. They neither create nor close a session and do not change contact points, local datacenter, keyspace, authentication, or driver profiles.

```kotlin
@Configuration(proxyBeanMethods = false)
class CassandraConfiguration : AbstractReactiveCassandraConfiguration() {
    override fun getKeyspaceName(): String = "app"
    override fun getLocalDataCenter(): String = "datacenter1"
}
```

Supply real values through environment-specific Spring Boot settings or a secret store. `CqlSession` is a thread-safe owner of connection pools and executors, so do not create one per service or request. When Spring creates the session, leave its closure to the Spring lifecycle.

## Spring Data and driver boundaries

Entity mapping, lifecycle callbacks, optimistic locking, and repository query derivation belong to Spring Data Cassandra. Driver codecs, prepared statements, consistency, and execution profiles belong to the DataStax driver. The bluetape4k adapters await those results or expose them as Flow.

`bluetape4k-cassandra` adds driver-level APIs. If the application does not need Spring Data mapping, that module plus `CqlSession` is simpler. If it needs repositories and converters, retain the Spring Data boundary in this module.

## Selection guide

| Situation | Preferred entry point |
| --- | --- |
| Repository-centered CRUD | Spring Data repository, with template coroutine adapters where needed |
| Dynamic `Query` with entity mapping | `ReactiveCassandraOperations` or `AsyncCassandraOperations` |
| Row-by-row streaming | reactive operations plus `Flow` |
| Direct control over driver statements and paging | `bluetape4k-cassandra` plus driver APIs |
| Application schema migration | a dedicated migration process, not `SchemaGenerator` as a replacement |

Mixing repositories, reactive templates, async templates, and raw sessions in one service obscures failure and transaction boundaries. Pick the required level first and descend to raw APIs only at an explicit edge.

## Ownership visible in the tests

`AbstractCassandraTestConfiguration` and its reactive peer override `getRequiredSession()` to return a companion-object shared session. Creating a session for each Spring application context quickly accumulates connections to the Testcontainers Cassandra instance. The same evidence supports managing a session as a long-lived object in production.

## Continue learning

After configuration, read [Reactive operations and coroutines](./reactive-coroutine-operations.md) for subscription and empty-result contracts. For hand-written CQL, continue with [Async and low-level CQL operations](./async-and-cql-operations.md).

## Sources

- [Module build](../../../../../spring-boot/cassandra/build.gradle.kts)
- [`ReactiveSessionCoroutines.kt`](../../../../../spring-boot/cassandra/src/main/kotlin/io/bluetape4k/spring/cassandra/ReactiveSessionCoroutines.kt)
- [`AbstractReactiveCassandraTestConfiguration.kt`](../../../../../spring-boot/cassandra/src/test/kotlin/io/bluetape4k/spring/cassandra/AbstractReactiveCassandraTestConfiguration.kt)
