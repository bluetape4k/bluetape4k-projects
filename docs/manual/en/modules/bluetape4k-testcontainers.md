---
manualId: bluetape4k-testcontainers
title: "Testcontainers Support"
description: "A server wrapper and utility library for building integration tests quickly on top of Testcontainers 2.0.3."
kind: library
group: testing
learningOrder: 1120
---

# Testcontainers Support

## Problem {#problem}

A server wrapper and utility library for building integration tests quickly on top of Testcontainers 2.0.3. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-testcontainers` when the application needs fixture ownership, isolation, deterministic cleanup, and failure diagnostics. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-testcontainers")
}
```

Gradle project path: `:bluetape4k-testcontainers`. Source directory: `testing/testcontainers`.

## Concepts {#concepts}

The first source-level concepts to inspect are `GenericContainerExtensions`, `GenericServer`, `PropertyExportingServer`, `AwsEmulatorServer`, `AwsEmulatorServerExtensions`, `DynamoDbLocalServer`, `ElasticMqServer`, and `FlociServer`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`GenericContainerExtensions`](../../../../testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/GenericContainerExtensions.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`GenericContainerExtensions`](../../../../testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/GenericContainerExtensions.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`GenericServer`](../../../../testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/GenericServer.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`PropertyExportingServer`](../../../../testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/PropertyExportingServer.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`AwsEmulatorServer`](../../../../testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/aws/AwsEmulatorServer.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`AwsEmulatorServerExtensions`](../../../../testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/aws/AwsEmulatorServerExtensions.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`DynamoDbLocalServer`](../../../../testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/aws/DynamoDbLocalServer.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`ElasticMqServer`](../../../../testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/aws/ElasticMqServer.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`FlociServer`](../../../../testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/aws/FlociServer.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`LocalStackServer`](../../../../testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/aws/LocalStackServer.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`MiniStackServer`](../../../../testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/aws/MiniStackServer.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Architecture**, **Container Lifecycle**, **Supported Container Class Diagram**, **Supported Container Structure**, **Key Features**, **System Property Export (PropertyExportingServer)**, **Exported Keys by Server**, **Usage Examples**, **Database**, and **PostgreSQL Extensions**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
implementation(platform(libs.spring.boot.dependencies))
api(project(":bluetape4k-core"))
api(libs.testcontainers)
api(libs.testcontainers.junit.jupiter)
api(libs.awaitility.kotlin)
api(libs.jna)
api(libs.jna.platform)
compileOnly(libs.hikaricp)
compileOnly(libs.testcontainers.mysql)
compileOnly(libs.testcontainers.mariadb)
compileOnly(libs.testcontainers.postgresql)
compileOnly(libs.testcontainers.cockroachdb)
```

Treat `compileOnly` edges as caller-provided capabilities and verify runtime availability before using their APIs.

## Configuration {#configuration}

Configuration resources found in the module:

- [`rootCA.pem`](../../../../testing/testcontainers/src/main/resources/certs/rootCA.pem)
- [`redisson-cluster.yml`](../../../../testing/testcontainers/src/main/resources/redisson-cluster.yml)

Read property names and defaults from these resources and the binding source before overriding them.

## Failures {#failures}

Failure semantics are defined by the linked entry points and tests, not inferred from the artifact name. Keep cancellation and timeout signals intact, close owned resources, and translate backend exceptions only at a boundary that can add a stable domain contract. Use the test anchors below to verify the exact behavior before adding retries or fallbacks.

## Operations {#operations}

Keep fixtures isolated, bound resource use, expose diagnostics, and close shared services deterministically. Keep capacity, timeout, retry, and shutdown settings next to the component that owns the resource; avoid process-wide defaults that hide which caller accepted the trade-off.

## Testing {#testing}

Run the module test task:

```bash
./gradlew :bluetape4k-testcontainers:test --no-configuration-cache
```

Representative test anchors:

- [`AbstractContainerTest`](../../../../testing/testcontainers/src/test/kotlin/io/bluetape4k/testcontainers/AbstractContainerTest.kt)
- [`GenericContainerExtensionsSupportTest`](../../../../testing/testcontainers/src/test/kotlin/io/bluetape4k/testcontainers/GenericContainerExtensionsSupportTest.kt)
- [`GenericServerSupportTest`](../../../../testing/testcontainers/src/test/kotlin/io/bluetape4k/testcontainers/GenericServerSupportTest.kt)
- [`GenericServerTest`](../../../../testing/testcontainers/src/test/kotlin/io/bluetape4k/testcontainers/GenericServerTest.kt)
- [`PropertyExportingServerContractTest`](../../../../testing/testcontainers/src/test/kotlin/io/bluetape4k/testcontainers/PropertyExportingServerContractTest.kt)
- [`RegisterSystemPropertiesTest`](../../../../testing/testcontainers/src/test/kotlin/io/bluetape4k/testcontainers/RegisterSystemPropertiesTest.kt)
- [`DynamoDbLocalServerTest`](../../../../testing/testcontainers/src/test/kotlin/io/bluetape4k/testcontainers/aws/DynamoDbLocalServerTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### Testcontainers Core Contract Class Diagram

[![Testcontainers Core Contract Class Diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/testing-testcontainers-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/testing-testcontainers-diagram-01.svg)

_Release README: [`testing/testcontainers/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/testing/testcontainers/README.md)_

### Testcontainers Supported Container Structure

[![Testcontainers Supported Container Structure](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/testing-testcontainers-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/testing-testcontainers-diagram-02.svg)

_Release README: [`testing/testcontainers/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/testing/testcontainers/README.md)_

### Container Lifecycle diagram

[![Container Lifecycle diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/testing-testcontainers-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/testing-testcontainers-sequence-01.svg)

_Release README: [`testing/testcontainers/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/testing/testcontainers/README.md)_

### Toxiproxy (Chaos Testing) diagram

[![Toxiproxy (Chaos Testing) diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/testing-testcontainers-sequence-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/testing-testcontainers-sequence-02.svg)

_Release README: [`testing/testcontainers/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/testing/testcontainers/README.md)_

<!-- release-readme-diagrams:end -->

## Sources {#sources}

- [Module README](../../../../testing/testcontainers/README.md)
- [Module build](../../../../testing/testcontainers/build.gradle.kts)
- [`GenericContainerExtensions`](../../../../testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/GenericContainerExtensions.kt)
- [`GenericServer`](../../../../testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/GenericServer.kt)
- [`PropertyExportingServer`](../../../../testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/PropertyExportingServer.kt)
- [`AwsEmulatorServer`](../../../../testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/aws/AwsEmulatorServer.kt)
- [`AwsEmulatorServerExtensions`](../../../../testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/aws/AwsEmulatorServerExtensions.kt)
- [`DynamoDbLocalServer`](../../../../testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/aws/DynamoDbLocalServer.kt)
- [`ElasticMqServer`](../../../../testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/aws/ElasticMqServer.kt)
- [`FlociServer`](../../../../testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/aws/FlociServer.kt)
- [`LocalStackServer`](../../../../testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/aws/LocalStackServer.kt)
- [`MiniStackServer`](../../../../testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/aws/MiniStackServer.kt)
- [`AbstractContainerTest`](../../../../testing/testcontainers/src/test/kotlin/io/bluetape4k/testcontainers/AbstractContainerTest.kt)
