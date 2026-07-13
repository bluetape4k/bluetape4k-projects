---
title: Practical recipes
description: Apply declaration markers, narrow opt-ins, stable wrappers, and implementation-sensitive SPI boundaries in complete examples.
manualId: bluetape4k-annotations
chapterId: recipes
---

# Practical recipes

## Publish an experimental API and use it from one function

The provider marks the declaration that owns the unstable contract.

```kotlin
@BluetapeExperimentalApi
fun planNextGenerationIndex(): IndexPlan = TODO()
```

The consumer opts in at the smallest accepting scope.

```kotlin
@OptIn(BluetapeExperimentalApi::class)
fun evaluateNewIndex(): Report =
    benchmark(planNextGenerationIndex())
```

## Contain a delicate API behind an adapter

Keep APIs with lifecycle or serialization risks inside a small adapter. Bluetape's Kafka codecs use `BluetapeDelicateApi` for this boundary.

```kotlin
class TrustedPayloadDecoder {

    @OptIn(BluetapeDelicateApi::class)
    fun decode(bytes: ByteArray): Payload {
        require(bytes.isNotEmpty())
        return KafkaCodecs.Jdk.decode(bytes) as Payload
    }
}
```

The adapter is stable only if it owns input validation and allowed types. Otherwise propagate the marker.

## Use an internal API from an integration bridge

Keep `BluetapeInternalApi` out of ordinary application code. Permit it only in a small framework bridge when a public-for-technical-reasons declaration is unavoidable.

```kotlin
@OptIn(BluetapeInternalApi::class)
internal fun installFrameworkBridge(registry: Registry) {
    registry.register(internalAdapter())
}
```

Keeping the bridge itself `internal` prevents the unstable dependency from spreading.

## Restrict third-party implementation only

```kotlin
@SubclassOptInRequired(BluetapeImplementationApi::class)
interface QueryEngine {
    fun execute(query: Query): Result
}

fun run(engine: QueryEngine, query: Query): Result = engine.execute(query)

@OptIn(BluetapeImplementationApi::class)
class CustomQueryEngine : QueryEngine {
    override fun execute(query: Query): Result = TODO()
}
```

`run` needs no opt-in. Only `CustomQueryEngine` accepts the implementation contract.

## Opt an entire experimental module in

Use a compiler option only when the whole source set deliberately shares one experimental policy.

```kotlin
kotlin {
    compilerOptions {
        optIn.add("io.bluetape4k.annotations.BluetapeExperimentalApi")
    }
}
```

Avoid this in ordinary production modules because new experimental calls no longer add an `@OptIn` line to the source diff.

## Review checklist

- Could visibility close the declaration instead?
- Does the marker describe the actual unstable contract?
- Was the diagnostic level chosen for policy rather than convenience?
- Is `@OptIn` broader than necessary?
- Does a public wrapper really absorb the risk, or should it propagate the marker?
- Are Java callers and migration paths documented separately?
- Is an owner responsible for reviewing the opt-in during upgrades?

## Verification

```bash
./gradlew :bluetape4k-annotations:test --no-configuration-cache
```

The current tests verify explicit opt-in for ordinary markers, local implementation of an implementation-sensitive SPI, and the stable type names of all six markers.

## Sources

- [Marker contract tests](../../../../../bluetape4k/annotations/src/test/kotlin/io/bluetape4k/annotations/BluetapeApiMarkersTest.kt)
- [Kafka codec source](../../../../../infra/kafka/src/main/kotlin/io/bluetape4k/kafka/codec/KafkaCodecs.kt)
- [Module build](../../../../../bluetape4k/annotations/build.gradle.kts)
