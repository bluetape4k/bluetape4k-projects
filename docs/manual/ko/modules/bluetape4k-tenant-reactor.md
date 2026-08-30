---
manualId: bluetape4k-tenant-reactor
title: "Reactor Tenant Context 어댑터"
description: "Reactor subscriber Context에 TenantId를 immutable하게 전달하는 JDK 25 adapter입니다. default tenant, global hook, automatic context propagation을 설치하지 않습니다."
kind: library
group: concurrency
learningOrder: 260
---

# Reactor Tenant Context 어댑터

## 해결하는 문제 {#problem}

Reactor subscriber Context에 TenantId를 immutable하게 전달하는 JDK 25 adapter입니다. default tenant, global hook, automatic context propagation을 설치하지 않습니다. 이 매뉴얼은 README의 기능 목록을 반복하지 않고 현재 build, source entry point, test, 설정 resource, lifecycle 근거를 연결합니다.

## 사용 시점 {#when-to-use}

reactive pipeline이 인증을 마친 `TenantId`를 Reactor subscriber `Context`로 전달해야 할 때 `bluetape4k-tenant-reactor`를 사용합니다. subscription boundary에서 한 번 binding하고 downstream은 `ContextView`로 읽습니다. global hook이나 automatic context propagation보다 명시적 전파가 필요할 때 적합합니다.

## 의존성 좌표 {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-tenant-reactor")
}
```

Gradle project path는 `:bluetape4k-tenant-reactor`, source directory는 `bluetape4k/tenant-reactor`입니다.

## 핵심 개념 {#concepts}

먼저 확인할 source 개념은 `ReactorTenantContext`입니다. 파일 이름은 탐색 anchor일 뿐이므로 public 계약으로 사용하기 전에 선언과 test를 함께 읽습니다.

## 빠른 시작 {#quick-start}

`contextWrite`에서 binding하고 `deferContextual`에서 지연 조회합니다.

```kotlin
val result = Mono.deferContextual { context ->
    service.find(ReactorTenantContext.requireCurrent(context))
}.contextWrite { context ->
    ReactorTenantContext.withTenant(context, TenantId("clinic-a"))
}
```

`withTenant`는 입력 `Context`를 변경하지 않고 새 immutable `Context`를 반환합니다.

## 작업별 API {#api-by-task}

| Entry point | 확인할 내용 |
| --- | --- |
| [`ReactorTenantContext.currentOrNull`](../../../../bluetape4k/tenant-reactor/src/main/kotlin/io/bluetape4k/tenant/reactor/ReactorTenantContext.kt) | `ContextView`에서 optional `TenantId`를 읽습니다. |
| [`ReactorTenantContext.requireCurrent`](../../../../bluetape4k/tenant-reactor/src/main/kotlin/io/bluetape4k/tenant/reactor/ReactorTenantContext.kt) | binding을 요구하고 누락 시 공통 missing-context exception으로 실패합니다. |
| [`ReactorTenantContext.withTenant`](../../../../bluetape4k/tenant-reactor/src/main/kotlin/io/bluetape4k/tenant/reactor/ReactorTenantContext.kt) | canonical `TenantId`를 포함한 새 `Context`를 만듭니다. |

## 권장 패턴 {#patterns}

subscription boundary 가까이에서 `withTenant`를 한 번 호출하고 tenant가 필요한 downstream component에서 `deferContextual`을 사용합니다. signal마다 `Context.put`을 호출하거나 global `Hooks` bridge, mutable registry를 설치하지 않습니다.

## 연동 {#integrations}

현재 build에 선언된 integration edge는 다음과 같습니다.

```kotlin
api(project(":bluetape4k-tenant"))
api(libs.reactor.core)
```

두 dependency 모두 public API edge이므로 consumer는 이 adapter와 함께 공통 tenant 계약과 Reactor `Context` type을 전달받습니다.

## 설정 {#configuration}

이 adapter에는 configuration property, global registration, automatic-propagation switch가 없습니다. application이 pipeline 안에서 subscription boundary를 명시적으로 선택합니다.

## 실패 동작 {#failures}

binding이 없으면 `currentOrNull`은 `null`, `requireCurrent`는 공통 `MissingTenantContextException`을 던집니다. cancellation이 발생하면 subscriber lifecycle과 함께 context도 끝납니다. default, fallback, duplicate recovery 정책은 제공하지 않습니다.

## 운영 {#operations}

tenant 값은 log, exception, MDC, metric tag에 노출하지 않습니다. binding failure telemetry에는 bounded carrier/stage label과 기존 correlation 또는 trace identifier를 사용합니다.

## 테스트 {#testing}

모듈 test task는 다음과 같습니다.

```bash
./gradlew :bluetape4k-tenant-reactor:test --no-configuration-cache
```

대표 test anchor는 다음과 같습니다.

- [`ReactorTenantContextTest`](../../../../bluetape4k/tenant-reactor/src/test/kotlin/io/bluetape4k/tenant/reactor/ReactorTenantContextTest.kt)

## 워크숍 {#workshops}

manual manifest에 등록된 전용 workshop path가 없습니다. 모듈 README와 위 representative test를 실행 근거로 사용합니다.

## 제한 사항 {#limitations}

이 adapter는 tenant 인증, header parsing, HTTP error mapping, Reactor hook, coroutine `ReactorContext` 자동 bridge를 제공하지 않습니다. 각 boundary가 전파를 명시적으로 선택해야 합니다.

## 근거 {#sources}

- [모듈 README](../../../../bluetape4k/tenant-reactor/README.ko.md)
- [모듈 build](../../../../bluetape4k/tenant-reactor/build.gradle.kts)
- [`ReactorTenantContext`](../../../../bluetape4k/tenant-reactor/src/main/kotlin/io/bluetape4k/tenant/reactor/ReactorTenantContext.kt)
- [`ReactorTenantContextTest`](../../../../bluetape4k/tenant-reactor/src/test/kotlin/io/bluetape4k/tenant/reactor/ReactorTenantContextTest.kt)
