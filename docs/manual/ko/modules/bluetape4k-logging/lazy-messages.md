---
title: Lazy messages와 실패 격리
description: Level guard, message supplier, marker와 error overload의 정확한 평가 계약을 설명합니다.
manualId: bluetape4k-logging
chapterId: lazy-messages
---

# Lazy messages와 실패 격리

문자열 보간, JSON 변환, collection 요약은 log level이 꺼져 있을 때 실행할 이유가 없습니다. lambda extension은 level guard 뒤에서 message를 계산합니다.

## 평가 순서

```kotlin
log.debug { "Loaded ${records.size} rows: ${records.take(3)}" }
```

1. `isDebugEnabled`를 확인합니다.
2. 활성화된 경우에만 supplier를 호출합니다.
3. 결과가 `null`이면 문자열 `"null"`을 사용합니다.
4. supplier가 던지면 business 흐름을 중단하지 않고 fallback text를 기록합니다.

`logMessageSafe`의 기본 fallback은 `Fail to generate log message.`이며 원래 exception 문자열을 뒤에 붙입니다. 이 정책은 logging 보조 코드의 실패가 서비스 동작을 바꾸지 않게 하지만, supplier bug를 숨길 수 있으므로 fallback 발생을 테스트와 모니터링에서 찾을 수 있어야 합니다.

## Error와 marker

각 level은 message-only, cause+message, marker+cause+message overload를 제공합니다.

```kotlin
log.warn(validationError) { "Rejected requestId=$requestId" }
log.error(securityMarker, failure) { "Authorization failed" }
```

WARN/ERROR helper는 message 앞에 `🔥` prefix를 추가합니다. backend의 severity는 SLF4J level이 결정하므로 prefix만 파싱해 alert를 만들지 않습니다.

## 피해야 할 패턴

```kotlin
// 이미 eager 계산이 끝났습니다.
log.debug(expensiveSnapshot())

// secret은 lazy여도 안전하지 않습니다.
log.info { "token=$token" }
```

Lazy는 비용 시점을 바꾸지만 redaction을 대신하지 않습니다. mutable object를 capture하면 evaluation 시점의 값이 기록되므로 필요한 immutable field를 먼저 추출합니다.

## Source와 tests

- [`Slf4jExtensions.kt`](../../../../../bluetape4k/logging/src/main/kotlin/io/bluetape4k/logging/Slf4jExtensions.kt)
- [`Slf4jExtensionsTest.kt`](../../../../../bluetape4k/logging/src/test/kotlin/io/bluetape4k/logging/Slf4jExtensionsTest.kt)
- [`Slf4jMdcExtensions.kt`](../../../../../bluetape4k/logging/src/main/kotlin/io/bluetape4k/logging/Slf4jMdcExtensions.kt)
- [`Slf4jMdcExtensionsTest.kt`](../../../../../bluetape4k/logging/src/test/kotlin/io/bluetape4k/logging/Slf4jMdcExtensionsTest.kt)

Correlation data의 범위는 [Scoped MDC](./scoped-mdc.md)에서 이어집니다.
