---
title: Database, Collection, and Flow
description: Use typed collections, first-result lookup, existence checks, upsert, and filter-sort-pagination Flow helpers with their real execution boundaries.
manualId: bluetape4k-mongodb
chapterId: database-collection-flow
---

# Database, Collection, and Flow

## Eagerly collecting names

`listDatabaseNamesAsList` and `listCollectionNamesList` call `toList()` on the driver's `Flow<String>`.

```kotlin
val databaseNames: List<String> = client.listDatabaseNamesAsList()
val collectionNames: List<String> = database.listCollectionNamesList()
```

This is useful for bounded administrative lists. Use native `listDatabaseNames()` or `listCollectionNames()` directly when the result may be large or processing should begin before the complete list is allocated.

## Typed collections and codecs

`getCollectionOf<T>(name)` is a reified shortcut for `getCollection(name, T::class.java)`.

```kotlin
val products = database.getCollectionOf<Product>("products")
```

The call does not create a codec for `Product`. `Document` works with the default registry, while application classes need a POJO, Kotlin, or kotlinx.serialization codec in the driver registry. A missing codec may appear only on the first encode or decode.

## First-result lookup

`findFirst(filter)` calls `find(filter).limit(1).firstOrNull()`. `findFirstOrNull` is an alias.

```kotlin
val product = products.findFirst(Filters.eq("sku", "A-100"))
```

It returns `null` when no document matches. If several documents match and there is no sort, do not treat one arbitrary result as a business contract. Use the native find builder when a deterministic sort is required.

## Cost of existence checks

`exists(filter)` is implemented as `countDocuments(filter) > 0`. It returns a boolean but is not a first-match shortcut.

```kotlin
if (products.exists(Filters.eq("sku", sku))) {
    // document exists at the time of this read
}
```

A separate existence check followed by insert has a race. Enforce uniqueness with an index and handle duplicate-key errors. If existence checks are hot, inspect the index and explain plan and compare a native query alternative.

## Atomic boundary of upsert

`upsert(filter, update)` invokes `updateOne` with `UpdateOptions().upsert(true)`.

```kotlin
val result = products.upsert(
    Filters.eq("sku", sku),
    Updates.combine(
        Updates.set("stock", stock),
        Updates.setOnInsert("sku", sku),
    ),
)
```

MongoDB owns the atomicity of that single update command. The helper does not add idempotency keys, unique indexes, or optimistic locking. The filter and indexes must represent business identity correctly.

## Filter, sort, and pagination as Flow

`findAsFlow` applies optional skip, limit, and sort to a filtered `FindFlow` and returns it as `Flow<T>`.

```kotlin
val page = products.findAsFlow(
    filter = Filters.gt("stock", 0),
    sort = Sorts.ascending("sku"),
    skip = 20,
    limit = 20,
)

page.collect { product -> consume(product) }
```

The helper does not collect results eagerly. Cancellation, buffering, driver exceptions, and command execution timing follow the returned native flow. Use a stable sort for paging and add a tie-breaker such as `_id` when sort values can repeat.

## Limits of skip pagination

Large skips can increase the server work required to walk past earlier documents. Inserts during paging can also produce duplicates or gaps. For frequently accessed high page numbers, compare skip paging with a cursor filter based on the last sort key.

## Sources and tests

- [`MongoDatabaseExtensions.kt`](../../../../../data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/MongoDatabaseExtensions.kt)
- [`MongoCollectionExtensions.kt`](../../../../../data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/MongoCollectionExtensions.kt)
- [`MongoDatabaseExtensionsTest.kt`](../../../../../data/mongodb/src/test/kotlin/io/bluetape4k/mongodb/MongoDatabaseExtensionsTest.kt)
- [`MongoCollectionExtensionsTest.kt`](../../../../../data/mongodb/src/test/kotlin/io/bluetape4k/mongodb/MongoCollectionExtensionsTest.kt)
- [`BasicCrudExamples.kt`](../../../../../data/mongodb/src/test/kotlin/io/bluetape4k/mongodb/examples/BasicCrudExamples.kt)
