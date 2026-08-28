# bluetape4k-tenant-reactor

[English](./README.md) | [한국어](./README.ko.md)

JDK 25 adapter for immutable `TenantId` propagation in Reactor subscriber `Context`. It installs no
default tenant, global hook, or automatic context propagation.

## Dependency and snapshot repository

```kotlin
repositories { maven { url = uri("https://central.sonatype.com/repository/maven-snapshots/"); mavenContent { snapshotsOnly() } } }
configurations.configureEach { resolutionStrategy.cacheChangingModulesFor(0, "seconds") }
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-bom:2.0.0-SNAPSHOT"))
    implementation("io.github.bluetape4k:bluetape4k-tenant-reactor:2.0.0-SNAPSHOT") { isChanging = true }
}
```

```xml
<repositories><repository><id>central-snapshots</id><url>https://central.sonatype.com/repository/maven-snapshots/</url><releases><enabled>false</enabled></releases><snapshots><enabled>true</enabled><updatePolicy>always</updatePolicy></snapshots></repository></repositories>
<dependencyManagement><dependencies><dependency><groupId>io.github.bluetape4k</groupId><artifactId>bluetape4k-bom</artifactId><version>2.0.0-SNAPSHOT</version><type>pom</type><scope>import</scope></dependency></dependencies></dependencyManagement>
<dependencies><dependency><groupId>io.github.bluetape4k</groupId><artifactId>bluetape4k-tenant-reactor</artifactId><version>2.0.0-SNAPSHOT</version></dependency></dependencies>
```

## Usage and lifecycle

Bind once at the subscription boundary and read explicitly from `deferContextual`. `withTenant` returns
a derived immutable `Context` and does not mutate its input.

```kotlin
val result = Mono.deferContextual { context ->
    service.find(ReactorTenantContext.requireCurrent(context))
}.contextWrite { context ->
    ReactorTenantContext.withTenant(context, TenantId("clinic-a"))
}
```

Do not call `Context.put` per signal. This adapter installs no Reactor `Hooks`, automatic propagation,
or coroutine `ReactorContext` bridge. Cancellation ends the subscriber-local lifecycle without copying
the tenant into an outer context. Missing context throws the common `MissingTenantContextException`;
there is no fallback.

Authenticate raw headers/tokens and map them to a canonical application enum/domain value first. Never
put raw tenant data in logs, exceptions, MDC, or metric tags; only synthetic fixtures may print values.
An optional `tenant_context_binding_failures_total{carrier,stage}` metric uses enum-only labels and
existing correlation/trace IDs. Any occurrence in five minutes is a wiring alert owned by the workshop
maintainer and the SNAPSHOT-train release coordinator.

## Unsupported boundaries

- Tenant authentication/authorization, header parsing, and HTTP status mapping
- Default/fallback tenants or global Reactor hooks
- Automatic coroutine suspension/dispatcher-hop propagation
- Per-signal binding, mutable/global registries, or tenant-value observability
