---
manualId: bluetape4k-testcontainers-spring
title: "Spring Testcontainers Property Bridge"
description: "Connect PropertyExportingServer properties to Spring Test's DynamicPropertyRegistry without changing core server lifecycle or JVM system properties."
kind: library
group: testing
learningOrder: 1150
---

# Spring Testcontainers Property Bridge

## Problem {#problem}

Spring integration tests often need container endpoints as dynamic application properties. The core `bluetape4k-testcontainers` module deliberately remains independent of Spring, so a test needs a small adapter that preserves the core property contract while registering values with Spring Test.

## When to use {#when-to-use}

Use `bluetape4k-testcontainers-spring` when a Spring Test context consumes properties exported by a `PropertyExportingServer`. Use the core module's `registerSystemProperties()` API instead when the test explicitly needs JVM system properties. Do not add this bridge to a non-Spring test or expect it to start, stop, or configure a container.

## Coordinates {#coordinates}

```kotlin
dependencies {
    testImplementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    testImplementation("io.github.bluetape4k:bluetape4k-testcontainers-spring")
}
```

Gradle project path: `:bluetape4k-testcontainers-spring`. Source directory: `testing/testcontainers-spring`.

## Concepts {#concepts}

`PropertyExportingServer` supplies a `propertyNamespace`, a set of `propertyKeys()`, and the current `properties()`. The bridge maps each key to `testcontainers.{namespace}.{key}` and registers a lazy supplier with Spring's `DynamicPropertyRegistry`. The bridge owns only registration; the test still owns the server lifecycle.

## Quick start {#quick-start}

Register a server from a static `@DynamicPropertySource` method:

```kotlin
import io.bluetape4k.testcontainers.spring.registerDynamicProperties
import io.bluetape4k.testcontainers.storage.RedisServer
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

companion object {
    @DynamicPropertySource
    @JvmStatic
    fun registerProperties(registry: DynamicPropertyRegistry) {
        RedisServer.Launcher.redis.registerDynamicProperties(registry)
    }
}
```

Start and stop `RedisServer.Launcher.redis` through the test's existing lifecycle. The bridge reads values only when Spring evaluates the registered supplier.

## API by task {#api-by-task}

| Task | Entry point | Contract |
| --- | --- | --- |
| Register server properties with Spring | `PropertyExportingServer.registerDynamicProperties(registry)` | Registers one lazy supplier for every key returned by `propertyKeys()`. |
| Keep core and Spring boundaries separate | `PropertyExportingServer` from `bluetape4k-testcontainers` | Reuses the core namespace and property map without adding Spring to the core module. |

## Patterns {#patterns}

Call the extension once from the test's `@DynamicPropertySource` method and keep container startup in the server launcher or test fixture. Because suppliers are lazy and uncached, a value is read from `properties()` for each Spring evaluation. Use one registration path for a key when possible; duplicate registrations are passed to Spring's registry ordering semantics rather than overwritten by this bridge.

## Integrations {#integrations}

The module depends on `bluetape4k-testcontainers` for `PropertyExportingServer` and on Spring Test for `DynamicPropertyRegistry`. It is an optional adapter, not Spring Boot auto-configuration. The release catalog supplies the compatible Spring Test version; consumers should not pin a separate version for this module.

## Configuration {#configuration}

No additional configuration file or system property is introduced. A server with namespace `redis` and key `host` is exposed as `testcontainers.redis.host`. The source namespace and key set remain the core server's responsibility.

## Failures {#failures}

If `propertyKeys()` declares a key that is absent from `properties()`, the supplier throws `IllegalStateException` when Spring evaluates the value. Exceptions raised by `properties()` are propagated with their original type and message. The bridge does not preflight duplicate keys, cache values, or translate server exceptions.

## Operations {#operations}

The bridge does not start or stop containers and does not mutate JVM system properties. Keep resource ownership, readiness, shutdown, and diagnostics in the existing `PropertyExportingServer` launcher or test fixture. This keeps Spring context setup independent from container lifecycle decisions.

## Testing {#testing}

Run the focused module tests:

```bash
./gradlew :bluetape4k-testcontainers-spring:test --no-configuration-cache
```

[`PropertyExportingServerDynamicPropertyRegistryTest`](../../../../testing/testcontainers-spring/src/test/kotlin/io/bluetape4k/testcontainers/spring/PropertyExportingServerDynamicPropertyRegistryTest.kt) verifies key mapping, lazy and repeated supplier evaluation, missing-key failures, exception propagation, duplicate registration delegation, and JVM system property preservation without Docker.

## Workshops {#workshops}

No dedicated workshop is registered for this adapter. The module README and focused contract test provide the runnable usage and lifecycle evidence.

## Limitations {#limitations}

This module supports Spring Test's dynamic property registry only. It does not provide Spring Boot auto-configuration, container startup, property caching, collision resolution, or migration of existing workshop helpers. Recheck the server's property contract when the core module changes.

## Sources {#sources}

- [Module README](../../../../testing/testcontainers-spring/README.md)
- [Module build](../../../../testing/testcontainers-spring/build.gradle.kts)
- [`PropertyExportingServerDynamicPropertyRegistry`](../../../../testing/testcontainers-spring/src/main/kotlin/io/bluetape4k/testcontainers/spring/PropertyExportingServerDynamicPropertyRegistry.kt)
- [`PropertyExportingServerDynamicPropertyRegistryTest`](../../../../testing/testcontainers-spring/src/test/kotlin/io/bluetape4k/testcontainers/spring/PropertyExportingServerDynamicPropertyRegistryTest.kt)
- [`PropertyExportingServer`](../../../../testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/PropertyExportingServer.kt)
