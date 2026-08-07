---
title: Map rows and Cassandra values into Kotlin types
description: Choose among row maps, typed getters and setters, UDT and tuple codecs, and mapper helpers.
manualId: bluetape4k-cassandra
chapterId: rows-data-mapping
---

# Map rows and Cassandra values into Kotlin types

## Map into a domain type first

When query columns are stable, convert each `Row` at the read boundary instead of carrying a `Map<String, Any?>` through the application. One mapper then owns column names, null decisions, and collection-copying policy, and schema mismatches fail close to the query.

```kotlin
import com.datastax.oss.driver.api.core.cql.Row
import io.bluetape4k.cassandra.data.getList

data class User(
    val id: Long,
    val name: String,
    val tags: List<String>,
)

fun Row.toUser(): User = User(
    id = getLong("id"),
    name = getString("name") ?: error("users.name must not be null"),
    tags = getList<String>("tags")?.toList().orEmpty(),
)
```

The mapper records that `name` is required. It treats null `tags` as an empty collection and copies the mutable list returned by the driver with `toList()`. Keep the property nullable instead of using `orEmpty()` when null and an empty collection have different domain meanings.

## Inspect a row dynamically

`RowSupport` can expose columns as maps keyed by index, rendered name, or `CqlIdentifier`.

| API | Key | Suitable use |
| --- | --- | --- |
| `toMap()` | Column index | Diagnostics that need result order |
| `toNamedMap()` | Rendered CQL name | Dynamic export, diagnostic summaries, schema exploration |
| `toCqlIdentifierMap()` | `CqlIdentifier` | Dynamic processing that must preserve quoting and case rules |
| `map`, `mapWithName`, `mapWithCqlIdentifier` | Same as above | Apply one transformation at a dynamic boundary |
| `columnCodecs()` | `CqlIdentifier` | Inspect the `TypeCodec` selected by the driver |

These functions decode each column with the driver codec selected for its type. `toNamedMap()` sorts entries by rendered CQL name, so it does not preserve select-list order. The non-transforming `toMap()`, `toNamedMap()`, and `toCqlIdentifierMap()` functions use `Any?` values, which means a misspelled field or wrong expected type is not caught at compile time. The `map*` functions instead return the transform result type `T`.

Use map conversion for diagnostics or boundaries whose columns are only known at runtime. Prefer an explicit mapper such as `toUser()` for a stable domain model. Do not turn a complete row into a map and log it when the result can contain sensitive columns.

## Decide whether null and empty string are equivalent

`getStringOrEmpty(index|name|id)` applies `orEmpty()` to the nullable value returned by the driver. It therefore maps both Cassandra null and an empty string to `""`.

```kotlin
import io.bluetape4k.cassandra.cql.getStringOrEmpty

val displayName = row.getStringOrEmpty("display_name")

// Preserve the nullable getter when null and empty have different meanings.
val middleName: String? = row.getString("middle_name")
```

Use `getStringOrEmpty` only when the consumer intentionally treats both states alike, such as a display fallback. Keep the nullable getter when absence affects validation, updates, or audit behavior.

## Get by name, index, or CqlIdentifier

`GettableSupport` adds Kotlin reified helpers to the Java Driver's `GettableByName`, `GettableByIndex`, and `GettableById` interfaces. Each form provides `getValue`, `getList`, `getSet`, and `getMap`; the session codec registry still performs the actual decoding.

```kotlin
import com.datastax.oss.driver.api.core.CqlIdentifier
import io.bluetape4k.cassandra.data.getList
import io.bluetape4k.cassandra.data.getValue

val name: String? = row.getValue("name")
val tags: MutableList<String>? = row.getList("tags")

val nameId = CqlIdentifier.fromInternal("name")
val sameName: String? = row.getValue(nameId)
```

These helpers do not let the compiler prove that a requested Kotlin type matches the Cassandra column. Reading fails through the driver when no suitable codec exists or the requested type is incompatible. Prefer names in ordinary application mapping, `CqlIdentifier` when metadata or quoted identifiers matter, and indexes only inside narrow functions that deliberately depend on select-list order.

## Preserve the type boundary while writing

`SettableSupport` adds `setValue`, `setList`, `setSet`, and `setMap` to the settable interfaces implemented by values such as `BoundStatement`, `UdtValue`, and `TupleValue`. Name-based `setValue` accepts a nullable value, while the collection helpers pass reified element types to the driver.

```kotlin
import io.bluetape4k.cassandra.data.setList
import io.bluetape4k.cassandra.data.setValue

val bound = prepared.boundStatementBuilder()
    .setValue("name", user.name)
    .setList("tags", user.tags)
    .build()
```

The helpers do not interpolate values into CQL. Define bind markers in a prepared statement, then set values with names and Cassandra types that match that statement. Whether null should be bound or omitted remains a mapper or call-site decision.

## UDT and Tuple values

For a UDT, find its `UserDefinedType` in keyspace metadata and create a value with `newValue`. Define a tuple with `DataTypes.tupleOf(...)` and create values from that type. Read them with the Java Driver's `getUdtValue` and `getTupleValue` methods.

```kotlin
val coordinatesType = session.metadata
    .getKeyspace("examples")
    .flatMap { keyspace -> keyspace.getUserDefinedType("coordinates") }
    .orElseThrow(::IllegalArgumentException)

val coordinates = coordinatesType.newValue(12, 34)
val bound = prepared.bind().setUdtValue("coordinates", coordinates)

val stored = row.getUdtValue("coordinates")
val x = stored?.getInt("x")
```

Tuple values use the same pattern with `TupleValue` and `setTupleValue`/`getTupleValue`. The bluetape4k helpers do not replace this driver model with a different serialization format. Schema and call-site code still own UDT field names and order, tuple element order, and nullability.

## Connect application types with codecs

Register types not covered by built-in codecs with `CqlSessionBuilder.addTypeCodecs(...)`. The 1.12.1 examples register `MappingCodec`, enum, `Optional`, and array codecs before using `getValue<T>` and `setValue`.

```kotlin
sessionBuilder.addTypeCodecs(CqlIntToStringCodec())

val bound = prepared.bind().setValue("pk", "1")
val pk: String? = row.getValue("pk")
```

Calling `getValue<String>` does not by itself turn a Cassandra `int` into a string. A `TypeCodec` connecting those CQL and Kotlin types must be registered in that session. Treat codec selection rules as session configuration when more than one codec can handle the same pair.

## When EntityHelper is available

If the DataStax Mapper generated an `EntityHelper<T>`, bluetape4k mapper helpers can remove repetitive preparation and binding code.

```kotlin
import io.bluetape4k.cassandra.mapper.bind
import io.bluetape4k.cassandra.mapper.prepareInsert

val prepared = userEntityHelper.prepareInsert(session)
val statement = userEntityHelper.bind(prepared, user)
val result = session.execute(statement)
```

`prepareInsert` and `prepareInsertIfNotExists` pass CQL produced by the `EntityHelper` to `CqlSession.prepare`. `bind` fills a prepared-statement builder from the entity and returns a `BoundStatement`. None of these functions executes the statement; execution remains the caller's responsibility.

The `bind` defaults are `NullSavingStrategy.DO_NOT_SET` and `lenient = true`. `DO_NOT_SET` does not call a setter for a null property, leaving its bind marker unset; in an UPDATE, an existing column value is therefore not overwritten. `SET_TO_NULL` binds null properties as CQL `NULL`.

With `lenient = true`, entity properties without matching target columns are skipped, so the statement may be only partially populated. With `lenient = false`, every non-computed entity property must have a matching target column or binding throws `IllegalArgumentException`. Choose both arguments after checking the intended write and the prepared statement's bind markers.

`bluetape4k-cassandra` 1.12.1 exposes the DataStax mapper runtime as an API dependency. An annotation processor must still generate the application's `EntityHelper<T>` code. A runtime dependency on its own does not create helpers. Without processor configuration, an explicit typed row mapper is often the simpler boundary.

## Selection guide

| Situation | Choose |
| --- | --- |
| Stable query columns | An explicit `Row`-to-domain mapping function |
| Columns selected at runtime | `toNamedMap()` or `toCqlIdentifierMap()` |
| Read or write collections by name, index, or identifier | Reified getter/setter helpers |
| Work directly with Cassandra UDT or Tuple values | Driver `UdtValue` and `TupleValue` APIs |
| Connect a separate Kotlin type to a CQL type | A session-registered `TypeCodec` |
| DataStax Mapper generated an entity helper | `EntityHelper` prepare/bind helpers |

Avoid making `Any?` maps the default for a fixed schema, collapsing null into empty values without a domain decision, or requesting arbitrary types without a registered codec. A short, explicit conversion boundary exposes column and type errors close to the query.

## Sources and representative tests

- [`RowSupport.kt`](../../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/cql/RowSupport.kt): string defaults, dynamic maps, and column codecs
- [`GettableSupport.kt`](../../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/data/GettableSupport.kt): name, index, and `CqlIdentifier` getters
- [`SettableSupport.kt`](../../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/data/SettableSupport.kt): name, index, and `CqlIdentifier` setters
- [`EntitySupport.kt`](../../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/mapper/EntitySupport.kt): `EntityHelper` preparation and binding
- [`CustomCodecExamples.kt`](../../../../../data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/examples/datatypes/CustomCodecExamples.kt): custom-codec registration and access
- [`UserDefinedTypesSimpleExamples.kt`](../../../../../data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/examples/datatypes/UserDefinedTypesSimpleExamples.kt): UDT metadata and values
- [`TuplesSimpleExamples.kt`](../../../../../data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/examples/datatypes/TuplesSimpleExamples.kt): tuple creation and reading
- [`EntitySupportTest.kt`](../../../../../data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/mapper/EntitySupportTest.kt): preparation and binding delegation
- [`build.gradle.kts`](../../../../../data/cassandra/build.gradle.kts): mapper runtime and annotation processor dependencies

## Previous and next

- Previous: [Coroutine queries and multi-page reads](./coroutine-queries.md)
- Next: [Choosing statements and QueryBuilder](./statements-query-builder.md)
