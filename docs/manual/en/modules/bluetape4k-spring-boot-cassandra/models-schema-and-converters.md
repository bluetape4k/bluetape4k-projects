---
title: "Models, conversion, and schema"
description: "Define Persistable and auditing new-state rules, converter ownership, and the limits of SchemaGenerator creation and truncation."
---

# Models, conversion, and schema

## Persistable IDs and equality

`AbstractCassandraPersistable<PK>` implements Spring Data `Persistable` and `Serializable`. A subclass supplies `getId()` and `setId()`.

- `isNew()` is true while the ID is null.
- Two objects are equal only when their effective user classes and non-null IDs match.
- With an ID, the hash is the ID hash; without one, it uses identity hash.
- Assigning an ID can therefore change the hash code.

Do not put a transient entity into a `HashSet` or use it as a `HashMap` key and then assign its ID. Review mutable-entity equality as part of the domain model.

## Auditing changes new-state detection

`AbstractCassandraAuditable<U, PK>` maps created-by, created-at, last-modified-by, and last-modified-at fields. It overrides the parent ID-based rule and returns `_createdAt == null` from `isNew()`.

```kotlin
@EnableCassandraAuditing
@Table("users")
class UserEntity : AbstractCassandraAuditable<String, UUID>() {
    @PrimaryKey
    private var pk: UUID? = null

    override fun getId(): UUID? = pk
    override fun setId(id: UUID) { pk = id }
}
```

Without active auditing and an `AuditorAware`, `createdAt` is not populated and an entity with an ID may still appear new. The source maps the last-modified user with the column spelling `lastModified_by`; verify it against an existing schema convention.

## Spring Data owns converters

The module does not auto-register application converters. Register custom types in Spring Data Cassandra configuration. The test `CurrencyConverter` and type-mapping fixtures demonstrate this boundary.

For UDTs, collections, temporal values, and enums, decide whether the driver codec or Spring Data converter owns conversion. Defining conflicting conversion at both levels can make repository and raw-session results disagree.

## What SchemaGenerator does

`SchemaGenerator.createTableAndTypes` reads entity metadata from the Spring Data mapping context and then:

1. discovers UDTs required by the entity or its properties;
2. executes UDT create specifications with `IF NOT EXISTS`;
3. creates the table only when current keyspace metadata has no such table.

`potentiallyCreateTableFor` checks table existence only. It does not compare columns, primary keys, clustering order, or table options.

## Truncation is destructive

`SchemaGenerator.truncate<T>` runs Spring Data truncate when the table exists in current keyspace metadata. It is a no-op for an absent table, but removes every row from a present table.

This is useful for test-fixture setup, not an application startup or request path. Use reviewable CQL with history, ordering, and rollback strategy for production migration.

## Criteria helper

`Criteria.eq` is an infix alias for the backtick-heavy `Criteria.is(value)` call.

```kotlin
val query = Query.query(Criteria.where("tenant_id") eq tenantId)
```

It does not bypass Cassandra query-model rules. Partition, clustering, and index design remain the caller's responsibility.

## Sources

- [`AbstractCassandraPersistable.kt`](../../../../../spring-boot/cassandra/src/main/kotlin/io/bluetape4k/spring/cassandra/model/AbstractCassandraPersistable.kt)
- [`AbstractCassandraAuditable.kt`](../../../../../spring-boot/cassandra/src/main/kotlin/io/bluetape4k/spring/cassandra/model/AbstractCassandraAuditable.kt)
- [`SchemaGenerator.kt`](../../../../../spring-boot/cassandra/src/main/kotlin/io/bluetape4k/spring/cassandra/schema/SchemaGenerator.kt)
- [`AbstractCassandraModelTest.kt`](../../../../../spring-boot/cassandra/src/test/kotlin/io/bluetape4k/spring/cassandra/model/AbstractCassandraModelTest.kt)
- [`SchemaGeneratorTest.kt`](../../../../../spring-boot/cassandra/src/test/kotlin/io/bluetape4k/spring/cassandra/schema/SchemaGeneratorTest.kt)
