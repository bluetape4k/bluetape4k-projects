---
manualId: bluetape4k-logging
title: "Kotlin 로깅과 MDC"
description: lazy SLF4J logging, scoped MDC, coroutine context 전파, 선택적 비동기 logging의 경계와 수명주기를 설명합니다.
kind: library
group: foundation
learningOrder: 130
---

# Kotlin 로깅과 MDC

## 해결하는 문제 {#problem}

`bluetape4k-logging`은 “로그를 어떻게 호출할까”뿐 아니라 “메시지를 언제 계산하고, request context를 어디까지 유지하며, 비동기 전달을 누가 닫을까”를 하나의 계약으로 다룹니다. 애플리케이션은 여전히 SLF4J provider와 appender를 소유합니다.

![Logger 생성, lazy message, MDC, 비동기 전달의 책임 지도](../../assets/logging/logger-api-map.svg)

## 사용 시점 {#when-to-use}

일관된 logger naming과 lazy message, operation 범위의 correlation context가 필요할 때 사용합니다. 표준 SLF4J 호출만으로 충분하면 억지로 wrapper를 늘리지 않습니다.

## 작업별 API {#api-by-task}

| 요구 사항 | 기본 선택 | 주의할 경계 |
| --- | --- | --- |
| class/companion logger | `KLogging` | 첫 접근 시 logger가 초기화됩니다. |
| top-level 또는 명시적 이름 | `KotlinLogging.logger` | blank 이름은 거부됩니다. |
| 비용 있는 message | lambda 기반 `debug {}` 등 | level이 꺼지면 supplier를 평가하지 않습니다. |
| 동기 코드의 correlation context | `withLoggingContext` | 중첩 값 복원 정책을 선택합니다. |
| suspend 코드의 correlation context | `withCoroutineLoggingContext` | `MDCContext`가 dispatcher 전환을 연결합니다. |
| caller와 log emission 분리 | `KLoggingChannel` | buffer, collector, close와 유실 가능성을 소유해야 합니다. |

일반 서비스는 `KLogging`과 scoped MDC부터 시작합니다. `KLoggingChannel`은 synchronous appender가 실제 병목이고, 종료 시 대기 중 event를 어떻게 처리할지 결정한 경우에만 선택합니다.

## 의존성 좌표 {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-logging")
    runtimeOnly("ch.qos.logback:logback-classic:<version>")
}
```

SLF4J provider는 애플리케이션에서 하나만 선택합니다. 이 모듈은 log level, encoder, destination, retention을 설정하지 않습니다.

## 핵심 개념 {#concepts}

Logger 생성, lazy event 작성, MDC context, optional background delivery는 서로 다른 책임입니다. 이 구분이 provider 설정과 channel lifecycle을 library helper에 숨기지 않게 합니다.

## 빠른 시작 {#quick-start}

```kotlin
class OrderService {
    companion object : KLogging()

    suspend fun load(orderId: String): Order =
        withCoroutineLoggingContext("orderId" to orderId) {
            log.debug { "Loading order" }
            repository.load(orderId)
        }
}
```

message lambda는 DEBUG가 활성화됐을 때만 평가되고, `orderId`는 coroutine context와 함께 전달된 뒤 scope 밖에서 복원됩니다.

## 권장 패턴 {#patterns}

Correlation key는 transport boundary에서 sanitize한 뒤 가장 작은 operation scope에 둡니다. message supplier는 값 계산만 담당하고 side effect를 만들지 않습니다.

## 연동 {#integrations}

SLF4J provider, `kotlinx-coroutines-slf4j`의 `MDCContext`, Ktor/Spring request lifecycle과 연동합니다. tracing/metrics를 직접 생성하지는 않습니다.

## 설정 {#configuration}

Level, encoder, appender, destination, retention과 MDC 출력 pattern은 host application이 소유합니다. Runtime classpath에는 provider 하나만 둡니다.

## 실패 동작 {#failures}

Message supplier failure는 fallback text로 격리됩니다. MDC는 `finally`에서 복원합니다. Channel close는 drain이 아니라 cancel이고 close 뒤 event는 drop됩니다.

## 운영 {#operations}

Fallback message, duplicate provider/appender, missing MDC field, appender latency, channel shutdown 누락을 관찰합니다. Secret은 supplier나 MDC에 들어가기 전에 제거합니다.

## 테스트 {#testing}

```bash
./gradlew :bluetape4k-logging:test --no-configuration-cache
```

Naming, level guard, supplier failure, nested MDC, coroutine 전파, channel close를 대표 테스트로 확인합니다.

## 워크숍 {#workshops}

등록된 logging 전용 workshop은 없습니다. Ktor/Spring request boundary에 correlation context를 적용하고 nested restore와 shutdown을 테스트하는 것이 가장 작은 실습입니다.

## 제한 사항 {#limitations}

Logging은 tracing, audit storage, durable event delivery가 아닙니다. MDC는 bridge 없이 thread 전환을 따라가지 않으며 async channel은 종료 시 pending event를 보장하지 않습니다.

## 학습 경로

1. [Logger foundation](./bluetape4k-logging/logger-foundation.md) — logger 이름과 provider 경계
2. [Lazy messages](./bluetape4k-logging/lazy-messages.md) — level guard와 supplier 실패
3. [Scoped MDC](./bluetape4k-logging/scoped-mdc.md) — 중첩 복원과 exception cleanup
4. [Coroutine MDC](./bluetape4k-logging/coroutine-mdc.md) — suspension과 child coroutine 전파
5. [Async channel](./bluetape4k-logging/async-channel.md) — buffer, collector, close와 event 유실
6. [Operations & recipes](./bluetape4k-logging/operations-recipes.md) — 설정, redaction, 진단과 테스트

## 매뉴얼이 보장하는 범위

현재 source와 representative test로 확인되는 naming, lazy evaluation, fallback message, MDC restore/remove, coroutine bridge, channel close 동작만 기술 계약으로 다룹니다. README의 예시 성능 수치는 특정 appender와 환경을 고정한 benchmark가 아니므로 보장값으로 사용하지 않습니다.

## Source {#sources}

- [모듈 source](../../../../bluetape4k/logging/src/main/kotlin/io/bluetape4k/logging)
- [대표 테스트](../../../../bluetape4k/logging/src/test/kotlin/io/bluetape4k/logging)
- [README](../../../../bluetape4k/logging/README.ko.md)

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `1.12.1` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### KLogging, KotlinLogging, MDC 도우미, 비동기 채널 로깅의 클래스 구조

[![KLogging, KotlinLogging, MDC 도우미, 비동기 채널 로깅의 클래스 구조](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-logging-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-logging-diagram-01.svg)

_배포본 README: [`bluetape4k/logging/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/bluetape4k/logging/README.ko.md)_

### 레벨 가드, 지연 메시지 supplier, MDC 컨텍스트, SLF4J 출력으로 이어지는 로깅 처리 흐름

[![레벨 가드, 지연 메시지 supplier, MDC 컨텍스트, SLF4J 출력으로 이어지는 로깅 처리 흐름](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-logging-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-logging-diagram-02.svg)

_배포본 README: [`bluetape4k/logging/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/bluetape4k/logging/README.ko.md)_

### SharedFlow 버퍼와 백그라운드 collector로 로그 이벤트를 출력하는 KLoggingChannel 시퀀스

[![SharedFlow 버퍼와 백그라운드 collector로 로그 이벤트를 출력하는 KLoggingChannel 시퀀스](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-logging-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-logging-sequence-01.svg)

_배포본 README: [`bluetape4k/logging/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/bluetape4k/logging/README.ko.md)_

<!-- release-readme-diagrams:end -->
