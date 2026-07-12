---
title: Async channel과 수명주기
description: SharedFlow buffer, collector, scope ownership, close와 post-close event 계약을 설명합니다.
manualId: bluetape4k-logging
chapterId: async-channel
---

# Async channel과 수명주기

`KLoggingChannel`은 log event를 caller coroutine에서 바로 backend로 보내지 않고 `MutableSharedFlow`를 거쳐 background collector가 SLF4J에 전달합니다. caller latency와 emission lifecycle을 분리하는 대신 buffer와 shutdown 계약이 추가됩니다.

![KLoggingChannel send, collect, close, post-close 흐름](../../../assets/logging/async-channel-sequence.svg)

## Runtime model

- replay는 0입니다.
- extra buffer capacity는 64입니다.
- overflow policy는 `SUSPEND`입니다.
- 기본 instance들은 `Dispatchers.IO + SupervisorJob + CoroutineName("logchannel")` shared scope를 사용합니다.
- JVM shutdown hook 하나가 shared job을 취소합니다.

`SUSPEND`는 “무제한 비동기”가 아닙니다. collector가 느리고 buffer가 가득 차면 `send`의 `emit`이 suspend합니다.

## 사용과 종료

```kotlin
class ImportWorker : AutoCloseable {
    private val logger = object : KLoggingChannel() {}

    suspend fun run(id: String) {
        logger.info { "Import started id=$id" }
    }

    override fun close() = logger.close()
}
```

`close()`는 idempotent하고 이 instance의 collector job만 cancel합니다. injected `CoroutineScope`는 caller 소유이므로 취소하지 않습니다. deterministic test나 suspend lifecycle callback은 `closeAndJoin()`으로 collector 종료까지 기다립니다.

## 전달 보장 경계

현재 구현의 close는 drain이 아니라 cancel입니다. close 직전 buffer에 있던 event가 모두 backend에 기록된다고 보장하지 않습니다. close 뒤 `send`는 block하지 않고 event를 버립니다. 따라서 audit 또는 반드시 기록해야 하는 event transport로 사용하면 안 됩니다.

collector는 개별 log emission exception을 catch하고 error log를 시도합니다. `CancellationException`은 다시 던져 정상적인 취소를 유지합니다.

## 언제 선택할까

| 조건 | 선택 |
| --- | --- |
| 일반 request/service log | `KLogging` |
| appender latency가 측정된 병목이며 suspend backpressure 허용 | `KLoggingChannel` 검토 |
| shutdown 때 모든 event 보장 필요 | durable queue/explicit drain 설계 |
| audit/security event | logging channel이 아닌 보존 가능한 event pipeline |

## Source와 tests

- [`KLoggingChannel.kt`](../../../../../bluetape4k/logging/src/main/kotlin/io/bluetape4k/logging/coroutines/KLoggingChannel.kt)
- [`KLoggingChannelTest.kt`](../../../../../bluetape4k/logging/src/test/kotlin/io/bluetape4k/logging/coroutines/KLoggingChannelTest.kt)

설정과 장애 진단은 [Operations & recipes](./operations-recipes.md)에서 마무리합니다.
