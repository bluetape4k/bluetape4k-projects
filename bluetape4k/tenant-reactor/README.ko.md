# bluetape4k-tenant-reactor

[English](./README.md) | [한국어](./README.ko.md)

Reactor subscriber `Context`에 `TenantId`를 immutable하게 전달하는 JDK 25 adapter입니다.
default tenant, global hook, automatic context propagation을 설치하지 않습니다.

## 의존성과 SNAPSHOT repository

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

## 사용법과 수명주기

subscription boundary에서 `withTenant`를 한 번 호출하고 downstream은
`deferContextual`에서 명시적으로 읽습니다. 입력 `Context`는 변경되지 않고 새 derived
`Context`가 반환됩니다.

```kotlin
val result = Mono.deferContextual { context ->
    service.find(ReactorTenantContext.requireCurrent(context))
}.contextWrite { context ->
    ReactorTenantContext.withTenant(context, TenantId("clinic-a"))
}
```

signal마다 `Context.put`을 호출하지 않습니다. `Hooks`, automatic propagation, coroutine
`ReactorContext` bridge는 설치하지 않습니다. cancellation 뒤 값은 subscriber lifecycle과 함께
사라지며 외부 `Context`로 복사되지 않습니다. missing context는 공통
`MissingTenantContextException`으로 실패하고 fallback은 없습니다.

raw header/token은 application 인증·권한 확인 뒤 canonical enum/domain 값으로 매핑합니다.
raw tenant 값은 log, exception, MDC, metric tag에 기록하지 않습니다. synthetic fixture만 값을
출력할 수 있습니다. optional `tenant_context_binding_failures_total{carrier,stage}`는 enum label과
기존 correlation/trace ID만 사용하며 5분 내 한 건도 wiring alert입니다. workshop maintainer와
SNAPSHOT train release coordinator가 확인 owner입니다.

## 비지원 경계

- tenant 인증·인가, header parsing, HTTP status mapping
- default/fallback tenant 또는 global Reactor hook
- coroutine suspension/dispatcher hop 자동 전파
- signal별 binding, mutable/global registry, tenant 값 observability
