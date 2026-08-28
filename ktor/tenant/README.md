# bluetape4k-ktor-tenant

[English](./README.md) | [한국어](./README.ko.md)

JDK 25 adapter that binds a canonical `TenantId` to Ktor `ApplicationCall.attributes` with a
one-call/one-tenant contract. The application owns plugins, authentication, header parsing, and HTTP
status mapping.

## Dependency and snapshot repository

```kotlin
repositories { maven { url = uri("https://central.sonatype.com/repository/maven-snapshots/"); mavenContent { snapshotsOnly() } } }
configurations.configureEach { resolutionStrategy.cacheChangingModulesFor(0, "seconds") }
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-bom:2.0.0-SNAPSHOT"))
    implementation("io.github.bluetape4k:bluetape4k-ktor-tenant:2.0.0-SNAPSHOT") { isChanging = true }
}
```

```xml
<repositories><repository><id>central-snapshots</id><url>https://central.sonatype.com/repository/maven-snapshots/</url><releases><enabled>false</enabled></releases><snapshots><enabled>true</enabled><updatePolicy>always</updatePolicy></snapshots></repository></repositories>
<dependencyManagement><dependencies><dependency><groupId>io.github.bluetape4k</groupId><artifactId>bluetape4k-bom</artifactId><version>2.0.0-SNAPSHOT</version><type>pom</type><scope>import</scope></dependency></dependencies></dependencyManagement>
<dependencies><dependency><groupId>io.github.bluetape4k</groupId><artifactId>bluetape4k-ktor-tenant</artifactId><version>2.0.0-SNAPSHOT</version></dependency></dependencies>
```

## Usage and lifecycle

An application plugin or authentication pipeline validates raw headers/tokens, maps them to a canonical
domain value, and binds exactly once near the start of the request pipeline.

```kotlin
enum class ClinicTenant(val tenantId: TenantId) { CLINIC_A(TenantId("clinic-a")) }
val tenant = authenticateAndResolveClinic(call.request).tenantId
KtorTenantContext.bindTenant(call, tenant)
service.find(KtorTenantContext.requireCurrent(call))
```

The value survives dispatcher hops when code passes the same `ApplicationCall`. A second or concurrent
binding throws `TenantAlreadyBoundException("Tenant context is already bound to this call")` without
overwriting the winner. The request-local call owns the value, so exception/cancellation completion needs
no global cleanup or registry and a new call is unbound. Missing context throws the common
`MissingTenantContextException`; there is no default or fallback.

Never put raw headers, tokens, or tenant values in logs, exceptions, MDC, or metric tags. Only synthetic
fixtures may print values. An optional `tenant_context_binding_failures_total{carrier,stage}` metric uses
enum-only labels and existing correlation/trace IDs. Any occurrence in five minutes is a wiring alert
owned by the workshop maintainer and the SNAPSHOT-train release coordinator.

## Unsupported boundaries

- Application plugins, tenant authentication/authorization/existence checks, and header parsing
- HTTP response/status mapping or duplicate-binding recovery
- Nested rebind, mutable clear APIs, default tenants, or global registries
- Observability containing raw tenant values
