---
title: Jackson 2 and 3 schemas and wire compatibility
description: JSON schema metadata, mapper-specific payloads, compileOnly dependencies, and compatibility verification.
manualId: bluetape4k-pulsar
chapterId: jackson-schemas-wire-compatibility
---

# Jackson 2 and 3 schemas and wire compatibility

## Two mapper generations

`jacksonSchema<T>()` uses Jackson 2 `com.fasterxml.jackson.databind.ObjectMapper`. `jackson3Schema<T>()` uses Jackson 3 `tools.jackson.databind.ObjectMapper`.

```kotlin
val schema2: Schema<Order> = jacksonSchema()
val schema3: Schema<Order> = jackson3Schema()
```

The APIs look alike, but mapper types and modules differ. Make one generation the explicit owner of an application's payload contract.

## SchemaInfo and payload bytes

Both implementations derive schema bytes and properties from `Schema.JSON(type).schemaInfo` and declare `SchemaType.JSON`. The schema name is `type.simpleName`, with the full class name as a fallback.

The supplied mapper performs the actual encode and decode. Naming strategies, Kotlin modules, visibility, dates, and number settings can change payload bytes even when metadata looks similar. Do not infer Jackson 2 producer and Jackson 3 consumer compatibility merely because both produce JSON.

## Runtime dependencies

Both Jackson integrations are `compileOnly` in the module build. Add the generation in use to the application or schema calls can fail with `NoClassDefFoundError`.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-pulsar")
    implementation("io.github.bluetape4k:bluetape4k-jackson2")
}
```

## Supplying an application mapper

The default comes from the matching bluetape4k Jackson module. When the application uses custom serializers, property naming, or time modules, pass the same configured mapper to schema creation.

```kotlin
val schema = jacksonSchema<Order>(applicationMapper)
```

For separate producer and consumer services, lock the contract with golden JSON and bidirectional decode tests instead of relying on a convention.

## Clone and evolution

`clone()` creates an independent schema object that reuses the mapper and computed `SchemaInfo`. Release tests cover metadata, encode/decode round trips, clone behavior, and broker round trips.

Those tests do not replace Pulsar schema compatibility policy for field evolution. Test nullable and default fields, old payload fixtures, and the namespace or topic policy separately.

## Sources and tests

- [`JacksonSchema.kt`](../../../../../infra/pulsar/src/main/kotlin/io/bluetape4k/pulsar/codec/JacksonSchema.kt)
- [`Jackson3Schema.kt`](../../../../../infra/pulsar/src/main/kotlin/io/bluetape4k/pulsar/codec/Jackson3Schema.kt)
- [`JacksonSchemaTest.kt`](../../../../../infra/pulsar/src/test/kotlin/io/bluetape4k/pulsar/codec/JacksonSchemaTest.kt)
- [`Jackson3SchemaTest.kt`](../../../../../infra/pulsar/src/test/kotlin/io/bluetape4k/pulsar/codec/Jackson3SchemaTest.kt)
