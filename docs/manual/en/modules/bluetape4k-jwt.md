---
manualId: bluetape4k-jwt
title: "JWT Authentication Utilities"
description: "A library for creating and parsing JSON Web Tokens (JWT). Built on jjwt 0.13.x, it provides a Kotlin-friendly API and KeyChain management."
kind: library
group: utilities
learningOrder: 1310
---

# JWT Authentication Utilities

## Problem {#problem}

A library for creating and parsing JSON Web Tokens (JWT). Built on jjwt 0.13.x, it provides a Kotlin-friendly API and KeyChain management. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-jwt` when the application needs input contracts, value semantics, algorithmic cost, and deterministic output. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-jwt")
}
```

Gradle project path: `:bluetape4k-jwt`. Source directory: `utils/jwt`.

## Concepts {#concepts}

The first source-level concepts to inspect are `JwtConsts`, `JwtCodecs`, `JwtComposer`, `JwtComposerDsl`, `KeyChain`, `KeyChainDto`, `AbstractKeyChainRepository`, and `KeyChainRepository`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`JwtConsts`](../../../../utils/jwt/src/main/kotlin/io/bluetape4k/jwt/JwtConsts.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`JwtConsts`](../../../../utils/jwt/src/main/kotlin/io/bluetape4k/jwt/JwtConsts.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`JwtCodecs`](../../../../utils/jwt/src/main/kotlin/io/bluetape4k/jwt/codec/JwtCodecs.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`JwtComposer`](../../../../utils/jwt/src/main/kotlin/io/bluetape4k/jwt/composer/JwtComposer.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`JwtComposerDsl`](../../../../utils/jwt/src/main/kotlin/io/bluetape4k/jwt/composer/JwtComposerDsl.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`KeyChain`](../../../../utils/jwt/src/main/kotlin/io/bluetape4k/jwt/keychain/KeyChain.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`KeyChainDto`](../../../../utils/jwt/src/main/kotlin/io/bluetape4k/jwt/keychain/KeyChainDto.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`AbstractKeyChainRepository`](../../../../utils/jwt/src/main/kotlin/io/bluetape4k/jwt/keychain/repository/AbstractKeyChainRepository.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`KeyChainRepository`](../../../../utils/jwt/src/main/kotlin/io/bluetape4k/jwt/keychain/repository/KeyChainRepository.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`InMemoryKeyChainRepository`](../../../../utils/jwt/src/main/kotlin/io/bluetape4k/jwt/keychain/repository/inmemory/InMemoryKeyChainRepository.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`RedisKeyChainRepository`](../../../../utils/jwt/src/main/kotlin/io/bluetape4k/jwt/keychain/repository/redis/RedisKeyChainRepository.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Architecture**, **JWT Create and Verify Flow**, **Class Diagram**, **JWT Token Structure**, **Key Features**, **Usage Examples**, **Basic JWT Creation and Parsing**, **Creating JWTs with Kotlin DSL**, **Using JwtReader**, and **KeyChain Rotation**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
api(project(":bluetape4k-io"))
api(libs.jjwt.api)
api(libs.jjwt.impl)
api(libs.jjwt.jackson)
api(project(":bluetape4k-jackson2"))
api(libs.jackson.module.kotlin)
api(libs.jackson.module.blackbird)
compileOnly(libs.fory.kotlin)
compileOnly(libs.kryo5)
compileOnly(libs.lz4.java)
compileOnly(libs.snappy.java)
compileOnly(libs.zstd.jni)
```

Treat `compileOnly` edges as caller-provided capabilities and verify runtime availability before using their APIs.

## Configuration {#configuration}

Configuration resources found in the module:

- [`gen-keypair.sh`](../../../../utils/jwt/src/main/resources/gen-keypair.sh)

Read property names and defaults from these resources and the binding source before overriding them.

## Failures {#failures}

Failure semantics are defined by the linked entry points and tests, not inferred from the artifact name. Keep cancellation and timeout signals intact, close owned resources, and translate backend exceptions only at a boundary that can add a stable domain contract. Use the test anchors below to verify the exact behavior before adding retries or fallbacks.

## Operations {#operations}

Measure hot paths, bound input sizes, and monitor failures at the application boundary that calls the utility. Keep capacity, timeout, retry, and shutdown settings next to the component that owns the resource; avoid process-wide defaults that hide which caller accepted the trade-off.

## Testing {#testing}

Run the module test task:

```bash
./gradlew :bluetape4k-jwt:test --no-configuration-cache
```

Representative test anchors:

- [`AbstractJwtTest`](../../../../utils/jwt/src/test/kotlin/io/bluetape4k/jwt/AbstractJwtTest.kt)
- [`JwtComposerDslTest`](../../../../utils/jwt/src/test/kotlin/io/bluetape4k/jwt/composer/JwtComposerDslTest.kt)
- [`JwtComposerTest`](../../../../utils/jwt/src/test/kotlin/io/bluetape4k/jwt/composer/JwtComposerTest.kt)
- [`AbstractKeyChainRepositoryTest`](../../../../utils/jwt/src/test/kotlin/io/bluetape4k/jwt/keychain/AbstractKeyChainRepositoryTest.kt)
- [`KeyChainTest`](../../../../utils/jwt/src/test/kotlin/io/bluetape4k/jwt/keychain/KeyChainTest.kt)
- [`InMemoryKeyChainRepositoryTest`](../../../../utils/jwt/src/test/kotlin/io/bluetape4k/jwt/keychain/inmemory/InMemoryKeyChainRepositoryTest.kt)
- [`RedisKeyChainRepositoryTest`](../../../../utils/jwt/src/test/kotlin/io/bluetape4k/jwt/keychain/redis/RedisKeyChainRepositoryTest.kt)
- [`AbstractJwtProviderTest`](../../../../utils/jwt/src/test/kotlin/io/bluetape4k/jwt/provider/AbstractJwtProviderTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### JWT Create and Verify Flow diagram

[![JWT Create and Verify Flow diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-jwt-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-jwt-diagram-01.svg)

_Release README: [`utils/jwt/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/utils/jwt/README.md)_

### JWT Class Structure diagram

[![JWT Class Structure diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-jwt-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-jwt-diagram-02.svg)

_Release README: [`utils/jwt/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/utils/jwt/README.md)_

<!-- release-readme-diagrams:end -->

## Sources {#sources}

- [Module README](../../../../utils/jwt/README.md)
- [Module build](../../../../utils/jwt/build.gradle.kts)
- [`JwtConsts`](../../../../utils/jwt/src/main/kotlin/io/bluetape4k/jwt/JwtConsts.kt)
- [`JwtCodecs`](../../../../utils/jwt/src/main/kotlin/io/bluetape4k/jwt/codec/JwtCodecs.kt)
- [`JwtComposer`](../../../../utils/jwt/src/main/kotlin/io/bluetape4k/jwt/composer/JwtComposer.kt)
- [`JwtComposerDsl`](../../../../utils/jwt/src/main/kotlin/io/bluetape4k/jwt/composer/JwtComposerDsl.kt)
- [`KeyChain`](../../../../utils/jwt/src/main/kotlin/io/bluetape4k/jwt/keychain/KeyChain.kt)
- [`KeyChainDto`](../../../../utils/jwt/src/main/kotlin/io/bluetape4k/jwt/keychain/KeyChainDto.kt)
- [`AbstractKeyChainRepository`](../../../../utils/jwt/src/main/kotlin/io/bluetape4k/jwt/keychain/repository/AbstractKeyChainRepository.kt)
- [`KeyChainRepository`](../../../../utils/jwt/src/main/kotlin/io/bluetape4k/jwt/keychain/repository/KeyChainRepository.kt)
- [`InMemoryKeyChainRepository`](../../../../utils/jwt/src/main/kotlin/io/bluetape4k/jwt/keychain/repository/inmemory/InMemoryKeyChainRepository.kt)
- [`RedisKeyChainRepository`](../../../../utils/jwt/src/main/kotlin/io/bluetape4k/jwt/keychain/repository/redis/RedisKeyChainRepository.kt)
- [`AbstractJwtTest`](../../../../utils/jwt/src/test/kotlin/io/bluetape4k/jwt/AbstractJwtTest.kt)
- [`JwtComposerDslTest`](../../../../utils/jwt/src/test/kotlin/io/bluetape4k/jwt/composer/JwtComposerDslTest.kt)
