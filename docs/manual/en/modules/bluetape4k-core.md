---
manualId: bluetape4k-core
title: Core Kotlin utilities
description: Validation, codecs, collections, ranges, time DSLs, reflection helpers, and small concurrency primitives used across bluetape4k.
kind: library
group: foundation
---

# Core Kotlin utilities

## Problem {#problem}

Backend modules repeatedly need the same low-level contracts: parameter validation with consistent exception types, byte-safe encoders, bounded collections, date/time helpers, and Kotlin-friendly adapters around Java or Apache Commons APIs. `bluetape4k-core` centralizes those primitives so higher modules do not invent slightly different versions.

## When to use {#when-to-use}

Add core when application code or another library needs several of its foundational types. Prefer a focused JDK/Kotlin expression when it is already clear and complete; core is most valuable when it establishes a shared contract used across modules. Do not pull in core merely for one trivial alias without checking the dependency cost.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-bom:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-core")
}
```

The repository compiles with Java 21 and Kotlin 2.3. Core is an API dependency of several higher bluetape4k modules.

## Concepts {#concepts}

The module is a toolbox rather than one runtime subsystem. Its main families are `support` validation/extensions, `codec` encoders, `collections` bounded and paginated containers, `range` value types, `concurrent` helpers, `functional` adapters, `time` DSLs, and reflection/Apache Commons bridges.

Validation names encode failure semantics: new `require*` helpers reject caller input with `IllegalArgumentException`. Collection capacity is part of the type contract; for example, `BoundedStack` and `RingBuffer` retain only a bounded working set.

## Quick start {#quick-start}

```kotlin
import io.bluetape4k.codec.encodeBase64String
import io.bluetape4k.support.requireNotBlank

fun tokenFor(userId: String?): String {
    val id = userId.requireNotBlank("userId")
    return id.encodeBase64String()
}
```

`requireNotBlank` returns the validated value, so validation can stay inside an expression without a second null assertion.

## API by task {#api-by-task}

| Task | Start with |
| --- | --- |
| Validate nullable strings and caller arguments | `io.bluetape4k.support.RequireSupport` extensions |
| Encode Base64, Base58, Base62, Hex, or URL62 | `io.bluetape4k.codec` |
| Keep a bounded LIFO or circular window | `BoundedStack`, `RingBuffer` |
| Represent pages or lazy permutations | `PaginatedList`, permutation extensions |
| Express open/closed range endpoints | `io.bluetape4k.range` types |
| Compose duration, period, temporal, or quarter operations | `io.bluetape4k.time` |
| Reduce work concurrently with explicit close semantics | `ConcurrentReducer` |

## Patterns {#patterns}

Validate at the public boundary and pass non-null values inward. Keep codecs at transport/storage boundaries instead of scattering encoding through domain logic. Choose bounded collections when unbounded growth would turn backpressure into an out-of-memory failure. Close lifecycle-owning concurrency helpers in `use`/`try-finally` blocks.

## Integrations {#integrations}

Core wraps or complements Kotlin stdlib, Java time/reflection/concurrency, Apache Commons, Eclipse Collections, and hashing utilities selected by the module build. Higher bluetape4k modules expose core types in their public APIs, so applications may receive core transitively; declare it directly when source code imports its API.

## Configuration {#configuration}

There is no global configuration file. Behavior is selected by constructor arguments and function parameters such as collection capacity, charset, range boundary, or timeout. Keep those values near the owning component rather than hiding them in unrelated global state.

## Failures {#failures}

Validation helpers throw `IllegalArgumentException` for invalid caller input. Codec decoders propagate malformed-input errors according to the underlying codec. Bounded collections reject invalid capacities during construction. `ConcurrentReducer.close()` cancels queued work and rejects submissions after closure; callers must decide whether cancellation is an expected shutdown path or an error.

## Operations {#operations}

Most helpers are allocation-only and own no background service. Pay attention to utilities that wrap executors, queues, or large buffers. Bound capacities from workload evidence, expose close/shutdown in the owning service lifecycle, and avoid using reflection helpers on hot paths without measurement.

## Testing {#testing}

Tests are organized by package and contract. Useful anchors include `BoundedStackTest`, `PaginatedListTest`, codec tests, range tests, time tests, and `ConcurrentReducer` tests. Run the module suite with:

```bash
./gradlew :bluetape4k-core:test --no-configuration-cache
```

When adopting one helper, copy the smallest matching test pattern rather than treating the whole module as one integration surface.

## Workshops {#workshops}

No single workshop covers the entire toolbox. Higher-level repository examples exercise core transitively. For focused learning, start from the matching unit test and turn one assertion into a small runnable experiment.

## Limitations {#limitations}

The breadth of core means its APIs do not share one lifecycle or performance profile. Read the source and tests for the selected family. Encoding is not encryption, reflection helpers do not make inaccessible APIs stable, and bounded containers do not provide distributed backpressure.

## Sources {#sources}

- [Module README and API catalog](../../../../bluetape4k/core/README.md)
- [Main source packages](../../../../bluetape4k/core/src/main/kotlin/io/bluetape4k)
- [Module tests](../../../../bluetape4k/core/src/test/kotlin/io/bluetape4k)
- [Module build and dependencies](../../../../bluetape4k/core/build.gradle.kts)
