# bluetape4k-tenant

[English](./README.md) | [한국어](./README.ko.md)

Common APIs and `ThreadLocal`/`ScopedValue` carriers for explicit tenant binding in JDK 25
applications. There is no default tenant or fallback.

## Dependency

Resolve the current `2.0.0-SNAPSHOT` from Central snapshots. Mark the artifact as changing and
disable the Gradle changing-module cache when validating a new snapshot build.

```kotlin
repositories {
    maven {
        url = uri("https://central.sonatype.com/repository/maven-snapshots/")
        mavenContent { snapshotsOnly() }
    }
}
configurations.configureEach {
    resolutionStrategy.cacheChangingModulesFor(0, "seconds")
}
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-bom:2.0.0-SNAPSHOT"))
    implementation("io.github.bluetape4k:bluetape4k-tenant:2.0.0-SNAPSHOT") {
        isChanging = true
    }
}
```

```xml
<repositories>
  <repository>
    <id>central-snapshots</id>
    <url>https://central.sonatype.com/repository/maven-snapshots/</url>
    <releases><enabled>false</enabled></releases>
    <snapshots><enabled>true</enabled><updatePolicy>always</updatePolicy></snapshots>
  </repository>
</repositories>
<dependencyManagement><dependencies><dependency>
  <groupId>io.github.bluetape4k</groupId><artifactId>bluetape4k-bom</artifactId>
  <version>2.0.0-SNAPSHOT</version><type>pom</type><scope>import</scope>
</dependency></dependencies></dependencyManagement>
<dependencies><dependency>
  <groupId>io.github.bluetape4k</groupId><artifactId>bluetape4k-tenant</artifactId>
  <version>2.0.0-SNAPSHOT</version>
</dependency></dependencies>
```

## Usage and lifecycle

For an MVC/Servlet request that keeps synchronous work on the same execution thread, inject one
application-singleton `ThreadLocalTenantContext` into the filter and downstream components. Use only
the lexical `withTenant` API; no raw `set`/`clear` surface exists.

```kotlin
val tenantContext: TenantContext = ThreadLocalTenantContext()
tenantContext.withTenant(TenantId("clinic-a")) {
    repository.findAppointments(tenantContext.requireCurrent())
}
```

Use one application-singleton `ScopedValueTenantContext` for virtual-thread and structured-concurrency
code. `StructuredTaskScope.fork` inherits the lexical binding. An independently started virtual thread,
coroutine suspension, or dispatcher hop does not receive an automatic propagation guarantee.

`currentOrNull()` returns `null` when unbound; `requireCurrent()` throws
`MissingTenantContextException("Tenant context is not bound")`. Do not create a carrier or
`ScopedValue` key per request.

## Application boundary and operational safety

Authenticate and map raw headers/tokens to a canonical application domain value before creating
`TenantId`; never treat the raw value as an authorized tenant.

```kotlin
enum class ClinicTenant(val tenantId: TenantId) { CLINIC_A(TenantId("clinic-a")) }
val tenant = authenticateAndResolveClinic(rawHeader).tenantId
tenantContext.withTenant(tenant) { handleRequest() }
```

Never put raw headers, tokens, or tenant values in logs, exceptions, MDC, or metric tags. Only synthetic
fixtures may print expected/observed tenant values. The library installs no logging or metric backend.
An optional consumer metric such as `tenant_context_binding_failures_total{carrier,stage}` must use
documented enum-only labels and existing correlation/trace IDs. Any missing or duplicate binding after
the boundary in a five-minute window is a wiring alert owned by the workshop maintainer and, during the
SNAPSHOT train, the release coordinator.

## Unsupported boundaries

- Tenant authentication, existence/authorization checks, and HTTP status mapping
- Default tenants, a library/static process-global carrier singleton, or public `set`/`clear`
  (an application DI-scoped singleton is supported)
- Implicit propagation across coroutine suspension/dispatcher hops
- Logging, MDC, metrics, or exceptions containing raw tenant values
