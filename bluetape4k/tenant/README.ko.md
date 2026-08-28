# bluetape4k-tenant

[English](./README.md) | [한국어](./README.ko.md)

JDK 25 애플리케이션에서 tenant를 명시적으로 binding하는 공통 API와
`ThreadLocal`/`ScopedValue` carrier를 제공합니다. default tenant와 fallback은 없습니다.

## 의존성

현재 `2.0.0-SNAPSHOT`은 Central snapshots repository에서 받습니다. SNAPSHOT은 changing
module로 선언하고 Gradle cache를 비활성화해야 새 빌드를 즉시 확인할 수 있습니다.

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
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>io.github.bluetape4k</groupId>
      <artifactId>bluetape4k-bom</artifactId>
      <version>2.0.0-SNAPSHOT</version>
      <type>pom</type><scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
<dependencies>
  <dependency>
    <groupId>io.github.bluetape4k</groupId>
    <artifactId>bluetape4k-tenant</artifactId>
    <version>2.0.0-SNAPSHOT</version>
  </dependency>
</dependencies>
```

## 사용법

MVC/Servlet 요청처럼 같은 실행 thread에서 동기 호출을 이어갈 때는 application singleton
`ThreadLocalTenantContext`를 filter와 downstream에 같은 identity로 주입합니다. raw
`set`/`clear` 대신 lexical `withTenant`만 사용합니다.

```kotlin
val tenantContext: TenantContext = ThreadLocalTenantContext()

tenantContext.withTenant(TenantId("clinic-a")) {
    repository.findAppointments(tenantContext.requireCurrent())
}
```

virtual thread와 structured concurrency에는 application singleton `ScopedValueTenantContext`를
사용할 수 있습니다. `StructuredTaskScope.fork`는 lexical binding을 상속하지만 독립적으로
시작한 virtual thread와 coroutine suspension/dispatcher hop에는 자동 전파를 보장하지 않습니다.

```kotlin
val tenantContext: TenantContext = ScopedValueTenantContext()

tenantContext.withTenant(TenantId("clinic-a")) {
    service.handle(tenantContext.requireCurrent())
}
```

`currentOrNull()`은 미설정 시 `null`, `requireCurrent()`는
`MissingTenantContextException("Tenant context is not bound")`을 던집니다. default tenant는
없습니다. carrier instance나 `ScopedValue` key를 request마다 만들지 마세요.

## Application boundary와 운영 안전

raw header/token은 application에서 검증·권한 확인 후 canonical domain 값으로 매핑합니다.

```kotlin
enum class ClinicTenant(val tenantId: TenantId) {
    CLINIC_A(TenantId("clinic-a")),
}

val tenant = authenticateAndResolveClinic(rawHeader).tenantId
tenantContext.withTenant(tenant) { handleRequest() }
```

raw header, token, tenant 값은 log, exception, MDC, metric tag에 기록하지 않습니다. synthetic
test fixture만 expected/observed 값을 사용할 수 있습니다. library는 logging/metric backend를
설치하지 않습니다. consumer가 선택적으로
`tenant_context_binding_failures_total{carrier,stage}`를 기록한다면 `carrier`와 `stage`는
문서화한 enum만 사용하고 기존 correlation/trace ID로 연결합니다. boundary 이후 missing 또는
duplicate binding이 5분 구간에 한 건이라도 있으면 wiring alert로 취급하며 workshop maintainer와
SNAPSHOT train의 release coordinator가 확인합니다.

## 비지원 경계

- tenant 인증·존재 확인·권한 부여와 HTTP status mapping
- default tenant, 라이브러리의 `static` 프로세스 전역 carrier singleton, public `set`/`clear`
  (애플리케이션 DI scope singleton은 지원)
- coroutine suspension/dispatcher hop의 암묵적 전파
- raw tenant 값을 포함한 logging, MDC, metrics, exception
