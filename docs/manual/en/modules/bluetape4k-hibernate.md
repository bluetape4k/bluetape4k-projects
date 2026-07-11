---
manualId: bluetape4k-hibernate
title: "Module bluetape4k-hibernate"
description: "A Kotlin extension library that eliminates boilerplate when working with Hibernate ORM and JPA."
kind: library
group: data
---

# Module bluetape4k-hibernate

## Problem {#problem}

A Kotlin extension library that eliminates boilerplate when working with Hibernate ORM and JPA. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-hibernate` when the application needs transaction boundaries, connection ownership, query behavior, and serialization. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-bom:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-hibernate")
}
```

Gradle project path: `:bluetape4k-hibernate`. Source directory: `data/hibernate`.

## Concepts {#concepts}

The first source-level concepts to inspect are `EntityManagerFactorySupport`, `EntityManagerSupport`, `HibernateConsts`, `SessionFactorySupport`, `SessionSupport`, `AbstractObjectAsJsonConverter`, `CompressedStringConverter`, and `DurationAsTimestampConverter`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`EntityManagerFactorySupport`](../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/EntityManagerFactorySupport.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`EntityManagerFactorySupport`](../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/EntityManagerFactorySupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`EntityManagerSupport`](../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/EntityManagerSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`HibernateConsts`](../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/HibernateConsts.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`SessionFactorySupport`](../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/SessionFactorySupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`SessionSupport`](../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/SessionSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`AbstractObjectAsJsonConverter`](../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/converters/AbstractObjectAsJsonConverter.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`CompressedStringConverter`](../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/converters/CompressedStringConverter.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`DurationAsTimestampConverter`](../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/converters/DurationAsTimestampConverter.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`EncryptedStringConverters`](../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/converters/EncryptedStringConverters.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`LocaleAsStringConverter`](../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/converters/LocaleAsStringConverter.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Overview**, **Key Features**, **Dependency**, **Spring Boot 4 Migration**, **TestEntityManager Shim**, **Architecture Diagrams**, **Persistence Extension Structure**, **JPA Entity Class Hierarchy**, **AttributeConverter Types**, and **Basic Usage**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
implementation(platform(libs.spring.boot.dependencies))
api(project(":bluetape4k-core"))
api(project(":bluetape4k-io"))
api(libs.jakarta.persistence.api.v32)
api(libs.jakarta.transaction.api)
api(libs.hibernate.core)
api(libs.hibernate.micrometer)
api(libs.querydsl.jpa)
api(libs.jakarta.el.api)
api(libs.jakarta.validation.api)
api(libs.hibernate.validator)
api(project(":bluetape4k-tink"))
```

Treat `compileOnly` edges as caller-provided capabilities and verify runtime availability before using their APIs.

## Configuration {#configuration}

No module-level configuration resource was found under `src/main/resources`. Configuration is supplied through constructors, builders, function arguments, or the integrating framework; confirm defaults in source.

## Failures {#failures}

Failure semantics are defined by the linked entry points and tests, not inferred from the artifact name. Keep cancellation and timeout signals intact, close owned resources, and translate backend exceptions only at a boundary that can add a stable domain contract. Use the test anchors below to verify the exact behavior before adding retries or fallbacks.

## Operations {#operations}

Track pool saturation, query latency, retries, transaction rollbacks, and schema compatibility. Keep capacity, timeout, retry, and shutdown settings next to the component that owns the resource; avoid process-wide defaults that hide which caller accepted the trade-off.

## Testing {#testing}

Run the module test task:

```bash
./gradlew :bluetape4k-hibernate:test --no-configuration-cache
```

Representative test anchors:

- [`AbstractHibernateTest`](../../../../data/hibernate/src/test/kotlin/io/bluetape4k/hibernate/AbstractHibernateTest.kt)
- [`EntityManagerFactorySupportTest`](../../../../data/hibernate/src/test/kotlin/io/bluetape4k/hibernate/EntityManagerFactorySupportTest.kt)
- [`EntityManagerSupportTest`](../../../../data/hibernate/src/test/kotlin/io/bluetape4k/hibernate/EntityManagerSupportTest.kt)
- [`HibernateApplication`](../../../../data/hibernate/src/test/kotlin/io/bluetape4k/hibernate/HibernateApplication.kt)
- [`HibernateConstsTest`](../../../../data/hibernate/src/test/kotlin/io/bluetape4k/hibernate/HibernateConstsTest.kt)
- [`SessionFactorySupportTest`](../../../../data/hibernate/src/test/kotlin/io/bluetape4k/hibernate/SessionFactorySupportTest.kt)
- [`SessionSupportTest`](../../../../data/hibernate/src/test/kotlin/io/bluetape4k/hibernate/SessionSupportTest.kt)
- [`TestEntityManager`](../../../../data/hibernate/src/test/kotlin/io/bluetape4k/hibernate/TestEntityManager.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

## Sources {#sources}

- [Module README](../../../../data/hibernate/README.md)
- [Module build](../../../../data/hibernate/build.gradle.kts)
- [`EntityManagerFactorySupport`](../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/EntityManagerFactorySupport.kt)
- [`EntityManagerSupport`](../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/EntityManagerSupport.kt)
- [`HibernateConsts`](../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/HibernateConsts.kt)
- [`SessionFactorySupport`](../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/SessionFactorySupport.kt)
- [`SessionSupport`](../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/SessionSupport.kt)
- [`AbstractObjectAsJsonConverter`](../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/converters/AbstractObjectAsJsonConverter.kt)
- [`CompressedStringConverter`](../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/converters/CompressedStringConverter.kt)
- [`DurationAsTimestampConverter`](../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/converters/DurationAsTimestampConverter.kt)
- [`EncryptedStringConverters`](../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/converters/EncryptedStringConverters.kt)
- [`LocaleAsStringConverter`](../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/converters/LocaleAsStringConverter.kt)
- [`AbstractHibernateTest`](../../../../data/hibernate/src/test/kotlin/io/bluetape4k/hibernate/AbstractHibernateTest.kt)
- [`EntityManagerFactorySupportTest`](../../../../data/hibernate/src/test/kotlin/io/bluetape4k/hibernate/EntityManagerFactorySupportTest.kt)
