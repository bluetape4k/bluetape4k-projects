---
title: 운영, 설정과 실전 레시피
description: Logback 출력, redaction, 테스트, channel 진단과 incident 대응을 하나의 운영 계약으로 묶습니다.
manualId: bluetape4k-logging
chapterId: operations-recipes
---

# 운영, 설정과 실전 레시피

좋은 API 호출도 backend 설정과 lifecycle이 맞지 않으면 쓸 수 있는 로그가 되지 않습니다. 운영에서는 “호출했는가”보다 “필요한 field가 안전하게 출력되고 종료 전에 전달됐는가”를 확인합니다.

## MDC 출력 설정

```xml
<pattern>%date %-5level [%thread] traceId=%X{traceId} requestId=%X{requestId} %logger - %msg%n</pattern>
```

structured JSON encoder를 사용하면 같은 key를 structured field로 매핑합니다. 설정은 application repository가 소유하고 library가 root logger를 변경하지 않습니다.

## Request boundary recipe

```kotlin
suspend fun handle(call: ApplicationCall) {
    val requestId = sanitize(call.request.headers["X-Request-Id"])
        ?: UUID.randomUUID().toString()

    withCoroutineLoggingContext("requestId" to requestId) {
        log.info { "Request accepted" }
        service.execute()
    }
}
```

외부 값을 길이와 문자 집합으로 제한한 뒤 MDC에 넣습니다. token, cookie, authorization header, raw body는 message와 MDC 모두에서 제외합니다.

## 테스트 계약

MDC test는 다음을 직접 assertion합니다.

- block 안의 값
- nested scope 뒤 outer 값 복원
- exception 뒤 복원
- `restorePrevious=false` 뒤 제거
- null entry 미설치
- dispatcher 전환 뒤 coroutine context 전파

Channel test는 capturing appender로 level/order/prefix/error를 확인하고 `closeAndJoin()` 뒤 collector inactive, close idempotency, post-close drop을 검증합니다.

```bash
./gradlew :bluetape4k-logging:test --no-configuration-cache
```

## Incident 진단표

| 증상 | 확인할 경계 | 조치 |
| --- | --- | --- |
| requestId가 간헐적으로 없음 | plain MDC가 coroutine 경계를 넘었는가 | coroutine helper로 scope를 감쌉니다. |
| DEBUG가 꺼져도 CPU 사용 증가 | eager message/serialization | lambda extension으로 이동합니다. |
| 종료 직전 log 누락 | async channel close가 drain으로 오인됨 | 중요한 event는 durable path로 이동합니다. |
| duplicate log line | 여러 provider/appender 또는 additive logger | runtime classpath와 Logback hierarchy를 확인합니다. |
| fallback message 반복 | supplier exception | message 생성 코드를 별도 테스트하고 단순화합니다. |

## Source와 이어 읽기

- [전체 logging 테스트](../../../../../bluetape4k/logging/src/test/kotlin/io/bluetape4k/logging)
- [모듈 README](../../../../../bluetape4k/logging/README.ko.md)
- [Coroutine MDC](./coroutine-mdc.md)
- [Async channel](./async-channel.md)

현재 등록된 logging 전용 workshop은 없습니다. Ktor/Spring request boundary에서 위 recipe를 적용하고 nested MDC와 shutdown test를 추가하는 것이 가장 작은 runnable exercise입니다.
