---
manualId: bluetape4k-ktor-tenant
title: "Ktor Tenant Context 어댑터"
description: "Ktor ApplicationCall.attributes에 canonical TenantId를 one-call/one-tenant로 binding하는 JDK 25 adapter입니다. plugin, 인증, header parsing, HTTP status mapping은 application이 소유합니다."
kind: library
group: web
learningOrder: 850
---

# Ktor Tenant Context 어댑터

## 해결하는 문제 {#problem}

Ktor ApplicationCall.attributes에 canonical TenantId를 one-call/one-tenant로 binding하는 JDK 25 adapter입니다. plugin, 인증, header parsing, HTTP status mapping은 application이 소유합니다. 이 매뉴얼은 README의 기능 목록을 반복하지 않고 현재 build, source entry point, test, 설정 resource, lifecycle 근거를 연결합니다.

## 사용 시점 {#when-to-use}

Ktor `ApplicationCall`마다 인증을 마친 `TenantId` 하나를 연결하고 같은 call을 유지하는 dispatcher hop에서도 조회해야 할 때 `bluetape4k-ktor-tenant`를 사용합니다. parsing, 인증·인가, HTTP status mapping은 application plugin 또는 authentication pipeline이 계속 소유합니다.

## 의존성 좌표 {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-ktor-tenant")
}
```

Gradle project path는 `:bluetape4k-ktor-tenant`, source directory는 `ktor/tenant`입니다.

## 핵심 개념 {#concepts}

먼저 확인할 source 개념은 `KtorTenantContext`, `TenantAlreadyBoundException`입니다. 파일 이름은 탐색 anchor일 뿐이므로 public 계약으로 사용하기 전에 선언과 test를 함께 읽습니다.

## 빠른 시작 {#quick-start}

raw input을 검증한 다음 request pipeline 앞부분에서 한 번 binding합니다.

```kotlin
val tenant = authenticateAndResolveClinic(call.request).tenantId
KtorTenantContext.bindTenant(call, tenant)

service.find(KtorTenantContext.requireCurrent(call))
```

request-local binding이 필요한 downstream code에는 같은 `ApplicationCall`을 전달합니다.

## 작업별 API {#api-by-task}

| Entry point | 확인할 내용 |
| --- | --- |
| [`KtorTenantContext.bindTenant`](../../../../ktor/tenant/src/main/kotlin/io/bluetape4k/ktor/tenant/KtorTenantContext.kt) | mutable clear나 overwrite API 없이 첫 canonical tenant를 call에 binding합니다. |
| [`KtorTenantContext.currentOrNull`](../../../../ktor/tenant/src/main/kotlin/io/bluetape4k/ktor/tenant/KtorTenantContext.kt) | optional request-local binding을 읽습니다. |
| [`KtorTenantContext.requireCurrent`](../../../../ktor/tenant/src/main/kotlin/io/bluetape4k/ktor/tenant/KtorTenantContext.kt) | binding을 요구하고 누락 시 공통 missing-context exception으로 실패합니다. |
| [`TenantAlreadyBoundException`](../../../../ktor/tenant/src/main/kotlin/io/bluetape4k/ktor/tenant/TenantAlreadyBoundException.kt) | 두 번째 또는 concurrent binding을 거부하고 첫 tenant를 보존합니다. |

## 권장 패턴 {#patterns}

`bindTenant` 호출 전에 canonical tenant를 인증·확정하고 call마다 정확히 한 번 binding합니다. `ApplicationCall.attributes`가 request-local lifecycle을 소유하므로 global registry, mutable clear, nested rebind, duplicate-binding recovery를 추가하지 않습니다.

## 연동 {#integrations}

현재 build에 선언된 integration edge는 다음과 같습니다.

```kotlin
api(project(":bluetape4k-tenant"))
api(libs.ktor.server.core)
```

두 dependency 모두 public API edge이므로 consumer는 이 adapter와 함께 공통 tenant 계약과 Ktor server core type을 전달받습니다.

## 설정 {#configuration}

이 adapter에는 configuration property나 자동 설치되는 Ktor plugin이 없습니다. 인증, canonicalization, binding, error mapping 위치는 application pipeline이 결정합니다.

## 실패 동작 {#failures}

binding이 없으면 `currentOrNull`은 `null`, `requireCurrent`는 공통 `MissingTenantContextException`을 던집니다. 두 번째 또는 concurrent `bindTenant` 호출은 `TenantAlreadyBoundException("Tenant context is already bound to this call")`으로 실패하며 첫 값을 보존합니다.

## 운영 {#operations}

tenant 값은 log, exception, MDC, metric tag에 넣지 않습니다. binding failure telemetry는 bounded carrier/stage label을 사용하고 기존 trace 또는 request identifier로 연결합니다.

## 테스트 {#testing}

모듈 test task는 다음과 같습니다.

```bash
./gradlew :bluetape4k-ktor-tenant:test --no-configuration-cache
```

대표 test anchor는 다음과 같습니다.

- [`KtorTenantContextTest`](../../../../ktor/tenant/src/test/kotlin/io/bluetape4k/ktor/tenant/KtorTenantContextTest.kt)

## 워크숍 {#workshops}

manual manifest에 등록된 전용 workshop path가 없습니다. 모듈 README와 위 representative test를 실행 근거로 사용합니다.

## 제한 사항 {#limitations}

이 모듈은 application plugin 설치, tenant 인증·인가, header parsing, HTTP response mapping, duplicate binding 복구를 제공하지 않습니다. default tenant나 process-global registry도 없습니다.

## 근거 {#sources}

- [모듈 README](../../../../ktor/tenant/README.ko.md)
- [모듈 build](../../../../ktor/tenant/build.gradle.kts)
- [`KtorTenantContext`](../../../../ktor/tenant/src/main/kotlin/io/bluetape4k/ktor/tenant/KtorTenantContext.kt)
- [`TenantAlreadyBoundException`](../../../../ktor/tenant/src/main/kotlin/io/bluetape4k/ktor/tenant/TenantAlreadyBoundException.kt)
- [`KtorTenantContextTest`](../../../../ktor/tenant/src/test/kotlin/io/bluetape4k/ktor/tenant/KtorTenantContextTest.kt)
