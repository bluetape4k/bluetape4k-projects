---
manualId: bluetape4k-tink
title: "Google Tink Cryptography"
description: "An idiomatic Kotlin wrapper around Google Tink cryptography library."
kind: library
group: io
learningOrder: 390
---

# Google Tink Cryptography

## Problem {#problem}

An idiomatic Kotlin wrapper around Google Tink cryptography library. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-tink` when the application needs encoding boundaries, resource ownership, streaming, compatibility, and malformed input. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-tink")
}
```

Gradle project path: `:bluetape4k-tink`. Source directory: `io/tink`.

## Concepts {#concepts}

The first source-level concepts to inspect are `SecureRandomSupport`, `TinkSupport`, `TinkAead`, `TinkAeadExtensions`, `TinkAeads`, `TinkDaeads`, `TinkDeterministicAead`, and `TinkDigester`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`SecureRandomSupport`](../../../../io/tink/src/main/kotlin/io/bluetape4k/tink/SecureRandomSupport.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`SecureRandomSupport`](../../../../io/tink/src/main/kotlin/io/bluetape4k/tink/SecureRandomSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`TinkSupport`](../../../../io/tink/src/main/kotlin/io/bluetape4k/tink/TinkSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`TinkAead`](../../../../io/tink/src/main/kotlin/io/bluetape4k/tink/aead/TinkAead.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`TinkAeadExtensions`](../../../../io/tink/src/main/kotlin/io/bluetape4k/tink/aead/TinkAeadExtensions.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`TinkAeads`](../../../../io/tink/src/main/kotlin/io/bluetape4k/tink/aead/TinkAeads.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`TinkDaeads`](../../../../io/tink/src/main/kotlin/io/bluetape4k/tink/daead/TinkDaeads.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`TinkDeterministicAead`](../../../../io/tink/src/main/kotlin/io/bluetape4k/tink/daead/TinkDeterministicAead.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`TinkDigester`](../../../../io/tink/src/main/kotlin/io/bluetape4k/tink/digest/TinkDigester.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`TinkDigesterExtensions`](../../../../io/tink/src/main/kotlin/io/bluetape4k/tink/digest/TinkDigesterExtensions.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`TinkDigesters`](../../../../io/tink/src/main/kotlin/io/bluetape4k/tink/digest/TinkDigesters.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Why Tink**, **Diagrams**, **TinkEncryptor Class Hierarchy**, **AEAD encrypt/decrypt Flow**, **Recommended Usage Scenarios**, **Anti-Patterns**, **Features**, **Dependencies**, **Quick Start**, and **AEAD — Authenticated Encryption (AES-256-GCM)**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
api(project(":bluetape4k-core"))
compileOnly(libs.lettuce.core)
compileOnly(libs.redisson)
api(libs.tink)
```

Treat `compileOnly` edges as caller-provided capabilities and verify runtime availability before using their APIs.

## Configuration {#configuration}

No module-level configuration resource was found under `src/main/resources`. Configuration is supplied through constructors, builders, function arguments, or the integrating framework; confirm defaults in source.

## Failures {#failures}

Failure semantics are defined by the linked entry points and tests, not inferred from the artifact name. Keep cancellation and timeout signals intact, close owned resources, and translate backend exceptions only at a boundary that can add a stable domain contract. Use the test anchors below to verify the exact behavior before adding retries or fallbacks.

## Operations {#operations}

Track payload size, allocation, latency, malformed-input rate, resource closure, and protocol errors. Keep capacity, timeout, retry, and shutdown settings next to the component that owns the resource; avoid process-wide defaults that hide which caller accepted the trade-off.

## Testing {#testing}

Run the module test task:

```bash
./gradlew :bluetape4k-tink:test --no-configuration-cache
```

Representative test anchors:

- [`SecureRandomSupportTest`](../../../../io/tink/src/test/kotlin/io/bluetape4k/tink/SecureRandomSupportTest.kt)
- [`TinkSupportTest`](../../../../io/tink/src/test/kotlin/io/bluetape4k/tink/TinkSupportTest.kt)
- [`TinkAeadExtensionsTest`](../../../../io/tink/src/test/kotlin/io/bluetape4k/tink/aead/TinkAeadExtensionsTest.kt)
- [`TinkAeadTest`](../../../../io/tink/src/test/kotlin/io/bluetape4k/tink/aead/TinkAeadTest.kt)
- [`TinkDeterministicAeadTest`](../../../../io/tink/src/test/kotlin/io/bluetape4k/tink/daead/TinkDeterministicAeadTest.kt)
- [`TinkDigesterTest`](../../../../io/tink/src/test/kotlin/io/bluetape4k/tink/digest/TinkDigesterTest.kt)
- [`TinkEncryptorTest`](../../../../io/tink/src/test/kotlin/io/bluetape4k/tink/encrypt/TinkEncryptorTest.kt)
- [`InMemoryVersionedDaeadKeysetStore`](../../../../io/tink/src/test/kotlin/io/bluetape4k/tink/keyset/InMemoryVersionedDaeadKeysetStore.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### TinkEncryptor Class Hierarchy diagram

[![TinkEncryptor Class Hierarchy diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-tink-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-tink-diagram-01.svg)

_Release README: [`io/tink/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/tink/README.md)_

### AEAD encrypt/decrypt Flow diagram

[![AEAD encrypt/decrypt Flow diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-tink-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-tink-sequence-01.svg)

_Release README: [`io/tink/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/tink/README.md)_

<!-- release-readme-diagrams:end -->

## Sources {#sources}

- [Module README](../../../../io/tink/README.md)
- [Module build](../../../../io/tink/build.gradle.kts)
- [`SecureRandomSupport`](../../../../io/tink/src/main/kotlin/io/bluetape4k/tink/SecureRandomSupport.kt)
- [`TinkSupport`](../../../../io/tink/src/main/kotlin/io/bluetape4k/tink/TinkSupport.kt)
- [`TinkAead`](../../../../io/tink/src/main/kotlin/io/bluetape4k/tink/aead/TinkAead.kt)
- [`TinkAeadExtensions`](../../../../io/tink/src/main/kotlin/io/bluetape4k/tink/aead/TinkAeadExtensions.kt)
- [`TinkAeads`](../../../../io/tink/src/main/kotlin/io/bluetape4k/tink/aead/TinkAeads.kt)
- [`TinkDaeads`](../../../../io/tink/src/main/kotlin/io/bluetape4k/tink/daead/TinkDaeads.kt)
- [`TinkDeterministicAead`](../../../../io/tink/src/main/kotlin/io/bluetape4k/tink/daead/TinkDeterministicAead.kt)
- [`TinkDigester`](../../../../io/tink/src/main/kotlin/io/bluetape4k/tink/digest/TinkDigester.kt)
- [`TinkDigesterExtensions`](../../../../io/tink/src/main/kotlin/io/bluetape4k/tink/digest/TinkDigesterExtensions.kt)
- [`TinkDigesters`](../../../../io/tink/src/main/kotlin/io/bluetape4k/tink/digest/TinkDigesters.kt)
- [`SecureRandomSupportTest`](../../../../io/tink/src/test/kotlin/io/bluetape4k/tink/SecureRandomSupportTest.kt)
- [`TinkSupportTest`](../../../../io/tink/src/test/kotlin/io/bluetape4k/tink/TinkSupportTest.kt)
