---
manualId: bluetape4k-tenant
title: "Tenant Context Core"
description: "Common APIs and ThreadLocal/ScopedValue carriers for explicit tenant binding in JDK 25 applications. There is no default tenant or fallback."
kind: library
group: concurrency
learningOrder: 250
---

# Tenant Context Core

## Problem {#problem}

Common APIs and ThreadLocal/ScopedValue carriers for explicit tenant binding in JDK 25 applications. There is no default tenant or fallback. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-tenant` when an application must carry an authenticated, canonical `TenantId` through a lexical scope without inventing a default tenant. Choose `ThreadLocalTenantContext` for synchronous work that remains on one platform thread and `ScopedValueTenantContext` for virtual-thread or structured-concurrency code. Use a carrier-specific adapter for Reactor, Ktor, or coroutine boundaries instead of assuming automatic propagation.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-tenant")
}
```

Gradle project path: `:bluetape4k-tenant`. Source directory: `bluetape4k/tenant`.

## Concepts {#concepts}

The first source-level concepts to inspect are `MissingTenantContextException`, `ScopedValueTenantContext`, `TenantContext`, `TenantId`, and `ThreadLocalTenantContext`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Create one application-scoped carrier and use only the lexical `withTenant` API:

```kotlin
val tenantContext: TenantContext = ThreadLocalTenantContext()

tenantContext.withTenant(TenantId("clinic-a")) {
    repository.findAppointments(tenantContext.requireCurrent())
}
```

Map raw headers or tokens to an authorized domain value before constructing `TenantId`.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`TenantContext`](../../../../bluetape4k/tenant/src/main/kotlin/io/bluetape4k/tenant/TenantContext.kt) | Bind with `withTenant`, query optionally with `currentOrNull`, or require a binding with `requireCurrent`. |
| [`ThreadLocalTenantContext`](../../../../bluetape4k/tenant/src/main/kotlin/io/bluetape4k/tenant/ThreadLocalTenantContext.kt) | Keep synchronous platform-thread bindings lexical; nested scopes restore the previous value in `finally`. |
| [`ScopedValueTenantContext`](../../../../bluetape4k/tenant/src/main/kotlin/io/bluetape4k/tenant/ScopedValueTenantContext.kt) | Use JDK 25 `ScopedValue` lexical inheritance with virtual threads and `StructuredTaskScope`. |
| [`TenantId`](../../../../bluetape4k/tenant/src/main/kotlin/io/bluetape4k/tenant/TenantId.kt) | Carry only a canonical application value after authentication and authorization. |
| [`MissingTenantContextException`](../../../../bluetape4k/tenant/src/main/kotlin/io/bluetape4k/tenant/MissingTenantContextException.kt) | Handle a missing required binding at the application boundary that owns status or error mapping. |

## Patterns {#patterns}

Inject one carrier instance into the boundary and downstream components that share the binding. Keep authentication, authorization, tenant existence checks, schema or connection routing, and persistence outside this module. Do not expose mutable `set` or `clear` operations or create a carrier per request.

## Integrations {#integrations}

The module build declares no direct `api`, `implementation`, `compileOnly`, or `runtimeOnly` dependency line. Inspect plugins and generated metadata in the build file.

## Configuration {#configuration}

The module has no configuration properties or resources. Carrier choice and lifecycle are explicit application decisions made when the `TenantContext` instance is constructed and injected.

## Failures {#failures}

`currentOrNull()` returns `null` when unbound, while `requireCurrent()` throws `MissingTenantContextException("Tenant context is not bound")`. `ThreadLocalTenantContext` restores a nested previous value and removes the binding in `finally`; `ScopedValueTenantContext` keeps the binding inside its lexical carrier. There is no fallback tenant.

## Operations {#operations}

Do not put raw headers, tokens, or tenant values in logs, exceptions, MDC, or metric labels. If a consumer records binding failures, use bounded carrier/stage labels and existing correlation or trace identifiers without exposing tenant identity.

## Testing {#testing}

Run the module test task:

```bash
./gradlew :bluetape4k-tenant:test --no-configuration-cache
```

Representative test anchors:

- [`ScopedValueTenantContextTest`](../../../../bluetape4k/tenant/src/test/kotlin/io/bluetape4k/tenant/ScopedValueTenantContextTest.kt)
- [`TenantContextApiTest`](../../../../bluetape4k/tenant/src/test/kotlin/io/bluetape4k/tenant/TenantContextApiTest.kt)
- [`TenantContextRetentionStressTest`](../../../../bluetape4k/tenant/src/test/kotlin/io/bluetape4k/tenant/TenantContextRetentionStressTest.kt)
- [`TenantIdTest`](../../../../bluetape4k/tenant/src/test/kotlin/io/bluetape4k/tenant/TenantIdTest.kt)
- [`ThreadLocalTenantContextTest`](../../../../bluetape4k/tenant/src/test/kotlin/io/bluetape4k/tenant/ThreadLocalTenantContextTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

The module does not authenticate tenants, authorize access, select schemas or connections, or propagate bindings across coroutine suspension and dispatcher hops. Independently started virtual threads also do not inherit a binding automatically. Applications must select the correct adapter at each asynchronous or framework boundary.

## Sources {#sources}

- [Module README](../../../../bluetape4k/tenant/README.md)
- [Module build](../../../../bluetape4k/tenant/build.gradle.kts)
- [`MissingTenantContextException`](../../../../bluetape4k/tenant/src/main/kotlin/io/bluetape4k/tenant/MissingTenantContextException.kt)
- [`ScopedValueTenantContext`](../../../../bluetape4k/tenant/src/main/kotlin/io/bluetape4k/tenant/ScopedValueTenantContext.kt)
- [`TenantContext`](../../../../bluetape4k/tenant/src/main/kotlin/io/bluetape4k/tenant/TenantContext.kt)
- [`TenantId`](../../../../bluetape4k/tenant/src/main/kotlin/io/bluetape4k/tenant/TenantId.kt)
- [`ThreadLocalTenantContext`](../../../../bluetape4k/tenant/src/main/kotlin/io/bluetape4k/tenant/ThreadLocalTenantContext.kt)
- [`ScopedValueTenantContextTest`](../../../../bluetape4k/tenant/src/test/kotlin/io/bluetape4k/tenant/ScopedValueTenantContextTest.kt)
- [`TenantContextApiTest`](../../../../bluetape4k/tenant/src/test/kotlin/io/bluetape4k/tenant/TenantContextApiTest.kt)
- [`TenantContextRetentionStressTest`](../../../../bluetape4k/tenant/src/test/kotlin/io/bluetape4k/tenant/TenantContextRetentionStressTest.kt)
- [`TenantIdTest`](../../../../bluetape4k/tenant/src/test/kotlin/io/bluetape4k/tenant/TenantIdTest.kt)
- [`ThreadLocalTenantContextTest`](../../../../bluetape4k/tenant/src/test/kotlin/io/bluetape4k/tenant/ThreadLocalTenantContextTest.kt)
