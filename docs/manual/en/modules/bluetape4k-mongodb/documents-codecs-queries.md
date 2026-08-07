---
title: Documents, codecs, and query boundaries
description: Understand the exact Document builder and getAs contracts, typed-collection codecs, and ownership of the official MongoDB Kotlin query extensions.
manualId: bluetape4k-mongodb
chapterId: documents-codecs-queries
---

# Documents, codecs, and query boundaries

## What `documentOf` removes

The pair overload inserts keys and values into an empty `Document`. The builder overload applies a `Document.() -> Unit` block.

```kotlin
val first = documentOf(
    "sku" to "A-100",
    "stock" to 3,
    "tags" to listOf("sale", "summer"),
)

val second = documentOf {
    put("sku", "B-200")
    put("stock", 0)
}
```

Later puts overwrite duplicate keys. The builder does not validate schemas, required fields, or key naming. Validate external input before adding it to a document.

## `getAs<T>` is a safe cast

`Document.getAs<T>(key)` is implemented as `get(key) as? T`.

```kotlin
val stock: Int? = document.getAs<Int>("stock")
```

Both an absent key and a runtime mismatch, such as asking for `Int` when the value is `Long`, return `null`. There is no numeric conversion, nested-path support, BSON coercion, or error detail. Combine `containsKey` with BSON-aware accessors when missing and wrong-type cases must differ.

## `Document` versus application types

`Document` is useful for loose schemas and projected or aggregated results. Application types make business fields and validation visible at compile time.

Calling `getCollectionOf<Order>("orders")` does not register a codec. Configure a client or database codec registry that can process `Order`. The BSON Kotlin dependency and optional kotlinx.serialization codec are building blocks, not an application schema.

## Optional kotlinx.serialization codec

`mongo.bson.kotlinx` is `compileOnly` in the 1.12.1 build. If an application uses that API, verify that its runtime dependency is present. Successful module compilation does not prove codec availability in the deployed classpath.

A codec change can alter stored representation. Cross-test old and new codecs against existing documents, field renames, and default values.

## Query DSL ownership

`Filters`, `Sorts`, `Updates`, `Projections`, and KProperty-based Kotlin extensions come from the MongoDB driver. `bluetape4k-mongodb` does not define another string query language or mapping layer.

```kotlin
val filter = Filters.eq(Product::sku, "A-100")
val sort = Sorts.ascending(Product::sku)
```

Available overloads and BSON conversion follow the official driver. When string and KProperty fields are mixed, verify that codec mapping produces the same stored field names.

## Sensitive data and logs

Logging an entire `Document` can expose credentials, personal data, and large payloads. Record the collection, operation, a bounded business identifier, and exception type. Apply redaction before logging filter or update documents.

## Sources and tests

- [`DocumentExtensions.kt`](../../../../../data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/bson/DocumentExtensions.kt)
- [`DocumentExtensionsTest.kt`](../../../../../data/mongodb/src/test/kotlin/io/bluetape4k/mongodb/bson/DocumentExtensionsTest.kt)
- [`MongoDatabaseExtensions.kt`](../../../../../data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/MongoDatabaseExtensions.kt)
- [`build.gradle.kts`](../../../../../data/mongodb/build.gradle.kts)
