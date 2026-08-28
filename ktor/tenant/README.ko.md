# bluetape4k-ktor-tenant

[English](./README.md) | [한국어](./README.ko.md)

Ktor `ApplicationCall.attributes`에 canonical `TenantId`를 one-call/one-tenant로 binding하는
JDK 25 adapter입니다. plugin, 인증, header parsing, HTTP status mapping은 application이 소유합니다.

## 의존성과 SNAPSHOT repository

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

## 사용법과 수명주기

application plugin 또는 인증 pipeline이 raw header/token을 검증하고 canonical domain 값으로
매핑한 뒤 request pipeline 초기에 정확히 한 번 binding합니다.

```kotlin
enum class ClinicTenant(val tenantId: TenantId) { CLINIC_A(TenantId("clinic-a")) }

val tenant = authenticateAndResolveClinic(call.request).tenantId
KtorTenantContext.bindTenant(call, tenant)
service.find(KtorTenantContext.requireCurrent(call))
```

dispatcher hop에서도 같은 `ApplicationCall`을 전달하면 값이 유지됩니다. 두 번째 또는 동시
binding은 `TenantAlreadyBoundException("Tenant context is already bound to this call")`으로
실패하며 winner를 덮어쓰지 않습니다. call이 request-local owner이므로 exception/cancellation
종료 후 global cleanup이나 registry가 필요하지 않고 새 call은 unbound입니다. missing context는
공통 `MissingTenantContextException`으로 실패하며 default/fallback은 없습니다.

raw header, token, tenant 값은 log, exception, MDC, metric tag에 기록하지 않습니다. synthetic
fixture만 값을 출력할 수 있습니다. optional
`tenant_context_binding_failures_total{carrier,stage}`는 enum label과 기존 correlation/trace ID만
사용합니다. 5분 내 한 건도 wiring alert이며 workshop maintainer와 SNAPSHOT train release
coordinator가 확인합니다.

## 비지원 경계

- application plugin, tenant 인증·인가·존재 확인, header parsing
- HTTP response/status mapping과 duplicate binding 복구
- nested rebind, mutable clear API, default tenant, global registry
- raw tenant 값 observability
