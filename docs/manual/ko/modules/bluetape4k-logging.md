---
manualId: bluetape4k-logging
title: Kotlin logging과 MDC 지원
description: lazy SLF4J logging, scoped MDC, coroutine context 전파, 선택적인 channel 기반 비동기 logging을 제공합니다.
kind: library
group: foundation
---

# Kotlin logging과 MDC 지원

## 해결하는 문제 {#problem}

backend 코드에는 일관된 logger 선언과 lazy message 평가가 필요합니다. request context는 coroutine boundary를 넘어가야 하지만 다음 request로 새면 안 됩니다. log가 많은 경로에서는 bounded 비동기 전달도 필요할 수 있습니다. `bluetape4k-logging`은 SLF4J와 kotlin-logging 위에 이 계약을 구현합니다.

## 사용 시점 {#when-to-use}

class나 companion logger에는 `KLogging`, package-level function에는 `KotlinLogging.logger {}`를 사용합니다. trace, request, tenant, user ID를 log에 실어야 할 때 scoped MDC helper를 사용하고 suspend 코드에서는 coroutine MDC helper를 선택합니다. `KLoggingChannel`은 synchronous logging이 실제 bottleneck인지 측정하고 shutdown에서 queue를 어떻게 처리할지 정한 뒤 도입합니다.

## 의존성 좌표 {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-bom:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-logging")
    runtimeOnly("ch.qos.logback:logback-classic:<version>")
}
```

애플리케이션은 SLF4J provider 하나를 선택해야 합니다. 서로 경쟁하는 provider를 여러 개 넣으면 안 됩니다.

## 핵심 개념 {#concepts}

`KLogging`은 owning type 이름으로 lazy 초기화한 SLF4J `Logger`를 제공합니다. lambda extension은 해당 log level이 꺼져 있을 때 message 계산을 피합니다. `withLoggingContext`는 block 동안 MDC key/value를 설치하고 `finally`에서 이전 값을 복원하거나 제거합니다. `withCoroutineLoggingContext`는 coroutine suspension 뒤에도 context를 전달합니다.

`KLoggingChannel`은 queue와 runtime lifecycle을 추가합니다. 일반 logging과 달리 caller가 반환된 뒤에도 전달이 남아 있을 수 있습니다.

## 빠른 시작 {#quick-start}

```kotlin
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.withLoggingContext

class OrderService {
    companion object : KLogging()

    fun load(orderId: String) = withLoggingContext("orderId" to orderId) {
        log.info { "Loading order" }
    }
}
```

block이 exception으로 끝나도 MDC helper가 이전 값을 복원합니다.

## 작업별 API {#api-by-task}

| 작업 | API |
| --- | --- |
| class 또는 companion logger | `KLogging` |
| top-level logger | `KotlinLogging.logger {}` |
| lazy SLF4J message | `log.debug { ... }`, `log.info { ... }` extension |
| 한 개 또는 여러 scoped MDC entry | `withLoggingContext` overload |
| suspend/async boundary의 MDC | `withCoroutineLoggingContext` |
| queue 기반 비동기 logging | `KLoggingChannel` |

## 권장 패턴 {#patterns}

correlation data는 transport boundary에서 추가하고 해당 operation을 소유한 범위에 MDC를 둡니다. `traceId`, `requestId`, `tenantId`처럼 안정적이고 크기가 작은 key를 사용합니다. secret이나 크기가 제한되지 않은 payload를 MDC에 넣으면 안 됩니다. message는 lazy하게 만들고 backend encoder가 구조화할 수 있는 형태를 유지합니다.

## 연동 {#integrations}

SLF4J를 대상으로 하며 Logback이나 애플리케이션이 선택한 provider와 함께 동작합니다. coroutine MDC 지원은 SLF4J MDC와 Kotlin coroutine context를 연결합니다. observability 모듈이 trace ID를 채울 수 있지만 logging 자체가 span이나 metric을 만들지는 않습니다.

## 설정 {#configuration}

log level, encoder, appender, retention, destination은 SLF4J provider 설정에 둡니다. `withLoggingContext`의 기본값은 `restorePrevious=true`입니다. channel logging의 queue/runtime option은 `KLoggingChannel` 계약을 따르며, 처리량과 허용 가능한 유실을 측정해 정합니다.

## 실패 동작 {#failures}

logger backend failure는 선택한 provider 계약을 따릅니다. scoped MDC cleanup은 `finally`에서 실행됩니다. map cleanup callback의 exception은 business exception을 덮지 않도록 무시합니다. bounded async queue는 policy에 따라 reject, block, drop할 수 있고 drain 전에 shutdown하면 message가 기록되지 않을 수 있습니다.

## 운영 {#operations}

log volume, channel logging의 queue/drop 수, appender latency, disk/network backpressure를 관찰합니다. 값이 MDC나 message lambda에 들어가기 전에 secret을 제거합니다. application shutdown에서 비동기 logging resource를 닫고 drain 동작을 테스트합니다.

## 테스트 {#testing}

`KLoggingTest`, `KotlinLoggingTest`는 logger naming과 생성을 검증합니다. `MdcSupportTest`, `MdcSupportCoroutinesTest`는 복원과 전파를 확인합니다. `KLoggingChannelTest`는 channel lifecycle을 다룹니다.

```bash
./gradlew :bluetape4k-logging:test --no-configuration-cache
```

## 워크숍 {#workshops}

등록된 logging 전용 workshop은 없습니다. Ktor와 Spring observability example에서 request ID와 trace context를 이 모듈에 연결할 수 있습니다. 작은 실습에서는 nested MDC 복원과 coroutine `async` 전파를 검증합니다.

## 제한 사항 {#limitations}

MDC의 기반은 thread-local state이므로 bridge 없이 thread가 바뀌면 context를 잃을 수 있습니다. logging은 tracing, audit, secret 저장소가 아닙니다. 비동기 전달은 작업을 queue로 옮겨 caller latency를 줄이는 대신 buffering과 shutdown trade-off를 추가합니다.

## 근거 {#sources}

- [모듈 README와 예제](../../../../bluetape4k/logging/README.ko.md)
- [`KLogging` source](../../../../bluetape4k/logging/src/main/kotlin/io/bluetape4k/logging/KLogging.kt)
- [Scoped MDC 구현](../../../../bluetape4k/logging/src/main/kotlin/io/bluetape4k/logging/MdcSupport.kt)
- [Coroutine/channel logging source](../../../../bluetape4k/logging/src/main/kotlin/io/bluetape4k/logging/coroutines)
- [Logging 테스트](../../../../bluetape4k/logging/src/test/kotlin/io/bluetape4k/logging)
