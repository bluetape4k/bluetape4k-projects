---
title: Opt-in scope
description: Compare function, class, file, and compiler-wide opt-in boundaries and learn when a wrapper should absorb or propagate a marker.
manualId: bluetape4k-annotations
chapterId: opt-in-scope
---

# Opt-in scope

The API provider puts a marker on a declaration. The API user puts `@OptIn` at the point that accepts the contract. Keeping those responsibilities separate makes ownership visible during review and upgrades.

## Start with the narrowest scope

```kotlin
@OptIn(BluetapeExperimentalApi::class)
fun evaluateDraft(): Result = experimentalPlan()
```

Function scope is the default when one operation uses the marked API. The call and its accepted risk stay close together.

Class scope fits when every method in one adapter shares the same contract.

```kotlin
@OptIn(BluetapeDelicateApi::class)
class LegacyCodecAdapter {
    fun encode(value: Any): ByteArray = KafkaCodecs.Jdk.encode(value)
    fun decode(bytes: ByteArray): Any = KafkaCodecs.Jdk.decode(bytes)
}
```

File scope affects every declaration in that file. Reserve it for a clearly bounded compatibility bridge or adapter file.

```kotlin
@file:OptIn(BluetapeObsoleteApi::class)
```

## Compiler options are exceptional

Gradle can opt an entire source set into a marker.

```kotlin
kotlin {
    compilerOptions {
        optIn.add("io.bluetape4k.annotations.BluetapeBetaApi")
    }
}
```

This removes acceptance sites from source. Use it only when a bounded experimental module deliberately shares one policy. Global opt-in to `Internal` or `Obsolete` can let new calls enter without an obvious review signal.

## Wrappers either absorb or propagate the contract

A public wrapper around marked code must make one of two choices. It can opt in internally and provide a genuinely stable contract by owning validation, failure behavior, and compatibility.

```kotlin
fun decodeTrustedPayload(bytes: ByteArray): Order {
    require(bytes.isNotEmpty())
    return decodeDelicate(bytes)
}
```

If the wrapper exposes the same risk, propagate the marker.

```kotlin
@BluetapeExperimentalApi
fun experimentalFacade(): Result = experimentalPlan()
```

An internal `@OptIn` does not make a wrapper stable by itself.

## Document Java callers separately

Binary retention preserves annotation metadata, but Kotlin opt-in diagnostics are not enforced for Java callers. State the restriction in KDoc, Java-facing documentation, and migration notes when Java can access the API.

## Upgrade review

Search for `@OptIn` during upgrades. Check whether the API stabilized, a replacement appeared, or the accepted scope grew too broad. Review file- and compiler-level opt-ins before function-level sites.

## Sources

- [Narrow opt-in guidance on the experimental marker](../../../../../bluetape4k/annotations/src/main/kotlin/io/bluetape4k/annotations/BluetapeExperimentalApi.kt)
- [Function-level opt-in in Kafka codec tests](../../../../../infra/kafka/src/test/kotlin/io/bluetape4k/kafka/codec/KafkaCodecTest.kt)
- [Marker compilation contract tests](../../../../../bluetape4k/annotations/src/test/kotlin/io/bluetape4k/annotations/BluetapeApiMarkersTest.kt)
