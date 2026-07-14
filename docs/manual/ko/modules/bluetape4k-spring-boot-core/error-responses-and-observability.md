---
title: 오류 응답과 관측
description: API 예외의 HTTP 상태 매핑, 메시지 노출 경계와 Micrometer Observation scope를 설명합니다.
manualId: bluetape4k-spring-boot-core
chapterId: error-responses-and-observability
---

# 오류 응답과 관측

## 오류 응답의 모양을 고정한다

`ApiErrorBody`는 `errorCode`, 생성 시각 `timestamp`, `message`를 담습니다. `apiErrorResponseEntityOf`는 상태 코드와 body를 `ResponseEntity`로 만듭니다.

```json
{
  "errorCode": "USER_NOT_FOUND",
  "timestamp": "2026-07-14T01:23:45Z",
  "message": "사용자를 찾을 수 없습니다"
}
```

실제 JSON 모양은 애플리케이션의 Jackson 3 설정에 달려 있습니다. 특히 `Instant` serializer와 naming policy는 이 모듈이 구성하지 않습니다.

## 처리하는 예외 범위

`ApiExceptionHandler`는 다음 상태를 명시적으로 매핑합니다.

| 예외 | 상태 |
| --- | --- |
| `ApiBadRequestException`, `HttpMessageNotReadableException` | 400 |
| `ApiUnauthorizedException` | 401 |
| `ApiForbiddenException` | 403 |
| `ApiEntityNotFoundException` | 404 |
| `ApiTooManyRequestsException` | 429 |
| `ApiInternalServerErrorException` | 500 |
| `ApiServiceUnavailableException` | 503 |

모든 `Throwable`을 잡는 fallback handler는 없습니다. validation, authentication, framework exception을 같은 형식으로 만들려면 애플리케이션 advice에서 별도로 매핑합니다.

## 메시지를 외부 계약으로 다룬다

Handler는 stack trace를 body에 넣지 않고 서버 로그에 기록합니다. 하지만 `exception.message`는 그대로 응답 body에 들어갑니다. SQL, 내부 host, credential, 원인 예외 세부 정보를 메시지에 넣으면 외부에 노출될 수 있습니다.

외부 공개 문구와 내부 진단 정보를 분리합니다. `errorCode`는 client가 분기할 안정된 값으로 사용하고, 상세 원인은 log와 trace에 남깁니다.

## 애플리케이션 registry로 관측한다

```kotlin
val order = observationRegistry.observeSpring(
    name = "order.service.load",
    keyValues = SpringObservationKeyValues(
        lowCardinality = KeyValues.of("component", "order-service"),
    ),
) { context ->
    context.addLowCardinalityKeyValue(KeyValue.of("outcome", "success"))
    repository.load(id)
}
```

Helper는 observation을 생성하고 시작한 뒤 scope를 열고, 정상·실패 여부와 관계없이 정지합니다. 일반 예외는 observation error로 기록하고 다시 던집니다. `CancellationException`은 error로 기록하지 않고 다시 던집니다.

## coroutine scope 전파

`observeSpringSuspending`은 현재 Observation을 Reactor Context와 coroutine thread context에 연결합니다. suspend 후 thread가 바뀌어도 registry의 current observation을 읽을 수 있고 종료 후 scope가 정리됩니다.

Exporter, Prometheus endpoint, OpenTelemetry SDK는 설치하지 않습니다. Spring Boot Actuator와 Micrometer Tracing 설정은 애플리케이션이 소유합니다.

## cardinality를 통제한다

service 이름, operation, outcome처럼 값의 종류가 제한된 필드는 low-cardinality로 둡니다. user ID, request ID, 원문 URI는 metric label로 만들지 않습니다. 꼭 필요하면 정제한 값을 high-cardinality trace field로 둡니다.

## Source와 tests

- [`ApiErrorResponse.kt`](../../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/rest/exceptions/ApiErrorResponse.kt)
- [`ApiExceptions.kt`](../../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/rest/exceptions/ApiExceptions.kt)
- [`ApiExceptionHandler.kt`](../../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/rest/exceptions/ApiExceptionHandler.kt)
- [`SpringObservationSupport.kt`](../../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/observability/SpringObservationSupport.kt)
- [`ApiExceptionHandlerTest.kt`](../../../../../spring-boot/core/src/test/kotlin/io/bluetape4k/spring/rest/exceptions/ApiExceptionHandlerTest.kt)
- [`SpringObservationSupportTest.kt`](../../../../../spring-boot/core/src/test/kotlin/io/bluetape4k/spring/observability/SpringObservationSupportTest.kt)

## 다음 읽을 장

[실행 리소스와 lifecycle](./execution-resources-and-lifecycle.md)에서 HTTP loop, coroutine scope와 executor를 누가 만들고 닫는지 다룹니다.
