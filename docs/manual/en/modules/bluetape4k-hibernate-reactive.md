---
manualId: bluetape4k-hibernate-reactive
title: "Module bluetape4k-hibernate-reactive"
description: "A Kotlin extension library that eliminates boilerplate when working with Hibernate Reactive (Mutiny/Stage)."
kind: library
group: data
---

# Module bluetape4k-hibernate-reactive

## Problem {#problem}

A Kotlin extension library that eliminates boilerplate when working with Hibernate Reactive (Mutiny/Stage). This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-hibernate-reactive` when the application needs transaction boundaries, connection ownership, query behavior, and serialization. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-bom:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-hibernate-reactive")
}
```

Gradle project path: `:bluetape4k-hibernate-reactive`. Source directory: `data/hibernate-reactive`.

## Concepts {#concepts}

The first source-level concepts to inspect are `EntityManagerFactorySupport`, `SessionFactorySupport`, `SessionSupport`, `StatelessSessionSupport`, `EntityManagerFactorySupport`, `SessionFactorySupport`, `SessionSupport`, and `StatelessSessionSupport`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`EntityManagerFactorySupport`](../../../../data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/mutiny/EntityManagerFactorySupport.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`EntityManagerFactorySupport`](../../../../data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/mutiny/EntityManagerFactorySupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`SessionFactorySupport`](../../../../data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/mutiny/SessionFactorySupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`SessionSupport`](../../../../data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/mutiny/SessionSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`StatelessSessionSupport`](../../../../data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/mutiny/StatelessSessionSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`EntityManagerFactorySupport`](../../../../data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/stage/EntityManagerFactorySupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`SessionFactorySupport`](../../../../data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/stage/SessionFactorySupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`SessionSupport`](../../../../data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/stage/SessionSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`StatelessSessionSupport`](../../../../data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/stage/StatelessSessionSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Key Features**, **Dependency**, **Feature Details**, **1. SessionFactory Conversion**, **2. Coroutine SessionFactory API**, **3. Mutiny Session / StatelessSession Extensions**, **4. Stage Session / StatelessSession Extensions**, **5. Example Tests**, **Architecture Diagrams**, and **Reactive Extension Structure**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
implementation(platform(libs.spring.boot.dependencies))
implementation("io.netty:netty-tcnative-classes") {
api(project(":bluetape4k-hibernate"))
api(project(":bluetape4k-mutiny"))
api(project(":bluetape4k-vertx"))
api(libs.hibernate.reactive.core)
api(libs.jakarta.validation.api)
implementation(libs.hibernate.validator)
api(libs.mutiny.kotlin)
api(libs.kotlinx.coroutines.core)
api(libs.kotlinx.coroutines.reactive)
compileOnly(project(":bluetape4k-tink"))
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
./gradlew :bluetape4k-hibernate-reactive:test --no-configuration-cache
```

Representative test anchors:

- [`Author_`](../../../../data/hibernate-reactive/src/test/java/io/bluetape4k/hibernate/reactive/examples/model/Author_.java)
- [`Book_`](../../../../data/hibernate-reactive/src/test/java/io/bluetape4k/hibernate/reactive/examples/model/Book_.java)
- [`AbstractHibernateReactiveTest`](../../../../data/hibernate-reactive/src/test/kotlin/io/bluetape4k/hibernate/reactive/AbstractHibernateReactiveTest.kt)
- [`MySQLLauncher`](../../../../data/hibernate-reactive/src/test/kotlin/io/bluetape4k/hibernate/reactive/MySQLLauncher.kt)
- [`Author`](../../../../data/hibernate-reactive/src/test/kotlin/io/bluetape4k/hibernate/reactive/examples/model/Author.kt)
- [`Book`](../../../../data/hibernate-reactive/src/test/kotlin/io/bluetape4k/hibernate/reactive/examples/model/Book.kt)
- [`AbstractMutinyTest`](../../../../data/hibernate-reactive/src/test/kotlin/io/bluetape4k/hibernate/reactive/examples/mutiny/AbstractMutinyTest.kt)
- [`MutinyExtrasTest`](../../../../data/hibernate-reactive/src/test/kotlin/io/bluetape4k/hibernate/reactive/examples/mutiny/MutinyExtrasTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

## Sources {#sources}

- [Module README](../../../../data/hibernate-reactive/README.md)
- [Module build](../../../../data/hibernate-reactive/build.gradle.kts)
- [`EntityManagerFactorySupport`](../../../../data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/mutiny/EntityManagerFactorySupport.kt)
- [`SessionFactorySupport`](../../../../data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/mutiny/SessionFactorySupport.kt)
- [`SessionSupport`](../../../../data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/mutiny/SessionSupport.kt)
- [`StatelessSessionSupport`](../../../../data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/mutiny/StatelessSessionSupport.kt)
- [`EntityManagerFactorySupport`](../../../../data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/stage/EntityManagerFactorySupport.kt)
- [`SessionFactorySupport`](../../../../data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/stage/SessionFactorySupport.kt)
- [`SessionSupport`](../../../../data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/stage/SessionSupport.kt)
- [`StatelessSessionSupport`](../../../../data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/stage/StatelessSessionSupport.kt)
- [`Author_`](../../../../data/hibernate-reactive/src/test/java/io/bluetape4k/hibernate/reactive/examples/model/Author_.java)
- [`Book_`](../../../../data/hibernate-reactive/src/test/java/io/bluetape4k/hibernate/reactive/examples/model/Book_.java)
- [`AbstractHibernateReactiveTest`](../../../../data/hibernate-reactive/src/test/kotlin/io/bluetape4k/hibernate/reactive/AbstractHibernateReactiveTest.kt)
- [`MySQLLauncher`](../../../../data/hibernate-reactive/src/test/kotlin/io/bluetape4k/hibernate/reactive/MySQLLauncher.kt)
