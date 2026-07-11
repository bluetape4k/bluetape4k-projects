---
manualId: bluetape4k-spring-boot-core
title: "Module bluetape4k-spring-boot-core"
description: "A unified module providing common features for Spring Boot 4.x applications."
kind: library
group: spring
---

# Module bluetape4k-spring-boot-core

## Problem {#problem}

A unified module providing common features for Spring Boot 4.x applications. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-spring-boot-core` when the application needs auto-configuration conditions, bean ownership, property binding, and application lifecycle. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-bom:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-spring-boot-core")
}
```

Gradle project path: `:bluetape4k-spring-boot-core`. Source directory: `spring-boot/core`.

## Concepts {#concepts}

The first source-level concepts to inspect are `AnnotationExtensions`, `BeanFactoryExtensions`, `BeanUtilsSupport`, `PropertyAccessorUtilsSupport`, `ProfileSupport`, `PropertyResolverExtensions`, `ToStringCreatorSupport`, and `DataBufferSupport`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`AnnotationExtensions`](../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/beans/AnnotationExtensions.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`AnnotationExtensions`](../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/beans/AnnotationExtensions.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`BeanFactoryExtensions`](../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/beans/BeanFactoryExtensions.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`BeanUtilsSupport`](../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/beans/BeanUtilsSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`PropertyAccessorUtilsSupport`](../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/beans/PropertyAccessorUtilsSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`ProfileSupport`](../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/config/ProfileSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`PropertyResolverExtensions`](../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/core/PropertyResolverExtensions.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`ToStringCreatorSupport`](../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/core/ToStringCreatorSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`DataBufferSupport`](../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/core/io/buffer/DataBufferSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`ExampleMatcherSupport`](../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/data/ExampleMatcherSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`RestClientBuilderDsl`](../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/http/RestClientBuilderDsl.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Features**, **Spring Core Utilities**, **Spring WebFlux + Coroutines**, **RestClient Coroutines DSL**, **Spring Boot Observability Helpers**, **Test Utilities**, **Diagrams**, **Spring Boot Core Capability Map**, **Spring WebFlux + Coroutines Request Flow**, and **RestClient Coroutines DSL Structure**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
implementation(platform(libs.spring.boot.dependencies))
compileOnly("org.springframework.boot:spring-boot-starter-webflux")
compileOnly("org.springframework.boot:spring-boot-starter-web")
compileOnly("org.springframework.boot:spring-boot-starter-test")
compileOnly(project(":bluetape4k-io"))
compileOnly(project(":bluetape4k-jackson3"))
compileOnly("org.springframework:spring-context-support")
compileOnly("org.springframework:spring-messaging")
compileOnly("org.springframework:spring-web")
compileOnly("org.springframework.data:spring-data-commons")
compileOnly("org.springframework.boot:spring-boot-autoconfigure")
compileOnly("org.springframework.boot:spring-boot-configuration-processor")
```

Treat `compileOnly` edges as caller-provided capabilities and verify runtime availability before using their APIs.

## Configuration {#configuration}

No module-level configuration resource was found under `src/main/resources`. Configuration is supplied through constructors, builders, function arguments, or the integrating framework; confirm defaults in source.

## Failures {#failures}

Failure semantics are defined by the linked entry points and tests, not inferred from the artifact name. Keep cancellation and timeout signals intact, close owned resources, and translate backend exceptions only at a boundary that can add a stable domain contract. Use the test anchors below to verify the exact behavior before adding retries or fallbacks.

## Operations {#operations}

Track condition reports, startup failures, pool/client health, request latency, and graceful shutdown. Keep capacity, timeout, retry, and shutdown settings next to the component that owns the resource; avoid process-wide defaults that hide which caller accepted the trade-off.

## Testing {#testing}

Run the module test task:

```bash
./gradlew :bluetape4k-spring-boot-core:test --no-configuration-cache
```

Representative test anchors:

- [`AbstractSpringTest`](../../../../spring-boot/core/src/test/kotlin/io/bluetape4k/spring/AbstractSpringTest.kt)
- [`AnnotationExtensionsTest`](../../../../spring-boot/core/src/test/kotlin/io/bluetape4k/spring/beans/AnnotationExtensionsTest.kt)
- [`BeanFactoryExtensionsTest`](../../../../spring-boot/core/src/test/kotlin/io/bluetape4k/spring/beans/BeanFactoryExtensionsTest.kt)
- [`BeanUtilsSupportTest`](../../../../spring-boot/core/src/test/kotlin/io/bluetape4k/spring/beans/BeanUtilsSupportTest.kt)
- [`PropertyResolverExtensionsTest`](../../../../spring-boot/core/src/test/kotlin/io/bluetape4k/spring/core/PropertyResolverExtensionsTest.kt)
- [`ToStringCreatorExtensionsTest`](../../../../spring-boot/core/src/test/kotlin/io/bluetape4k/spring/core/ToStringCreatorExtensionsTest.kt)
- [`DataBufferSupportTest`](../../../../spring-boot/core/src/test/kotlin/io/bluetape4k/spring/core/io/buffer/DataBufferSupportTest.kt)
- [`ExampleMatcherSupportTest`](../../../../spring-boot/core/src/test/kotlin/io/bluetape4k/spring/data/ExampleMatcherSupportTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

## Sources {#sources}

- [Module README](../../../../spring-boot/core/README.md)
- [Module build](../../../../spring-boot/core/build.gradle.kts)
- [`AnnotationExtensions`](../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/beans/AnnotationExtensions.kt)
- [`BeanFactoryExtensions`](../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/beans/BeanFactoryExtensions.kt)
- [`BeanUtilsSupport`](../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/beans/BeanUtilsSupport.kt)
- [`PropertyAccessorUtilsSupport`](../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/beans/PropertyAccessorUtilsSupport.kt)
- [`ProfileSupport`](../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/config/ProfileSupport.kt)
- [`PropertyResolverExtensions`](../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/core/PropertyResolverExtensions.kt)
- [`ToStringCreatorSupport`](../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/core/ToStringCreatorSupport.kt)
- [`DataBufferSupport`](../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/core/io/buffer/DataBufferSupport.kt)
- [`ExampleMatcherSupport`](../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/data/ExampleMatcherSupport.kt)
- [`RestClientBuilderDsl`](../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/http/RestClientBuilderDsl.kt)
- [`AbstractSpringTest`](../../../../spring-boot/core/src/test/kotlin/io/bluetape4k/spring/AbstractSpringTest.kt)
- [`AnnotationExtensionsTest`](../../../../spring-boot/core/src/test/kotlin/io/bluetape4k/spring/beans/AnnotationExtensionsTest.kt)
