---
title: RestClient와 coroutine 경계
description: blocking RestClient를 suspend 함수에서 사용할 때의 thread, cancellation, body와 converter 계약을 설명합니다.
manualId: bluetape4k-spring-boot-core
chapterId: rest-client-and-coroutines
---

# RestClient와 coroutine 경계

## RestClient는 blocking client다

`RestClient` coroutine DSL은 Spring의 blocking HTTP client를 `runInterruptible(Dispatchers.IO)`에서 실행합니다. 호출하는 코드는 suspend 함수가 되지만 I/O 구현이 reactive로 바뀌지는 않습니다.

```kotlin
val client = restClientOf("https://api.example.com") {
    defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer $token")
}

suspend fun loadUser(id: Long): User =
    client.suspendGet("/users/$id", MediaType.APPLICATION_JSON)
```

WebFlux event-loop thread에서 직접 `RestClient.retrieve().body()`를 호출하는 것보다 안전하지만, 높은 동시성에서 blocking connection pool과 `Dispatchers.IO` capacity를 함께 봐야 합니다.

## 두 API 층을 구분한다

`httpGet`, `httpPost`, `httpPut`, `httpPatch`, `httpDelete`, `httpHead`, `httpOptions`는 `RestClient.ResponseSpec`을 반환합니다. 호출자가 status handler와 body decoding을 이어서 구성할 수 있습니다.

```kotlin
val response = client.httpGet("/users/1", MediaType.APPLICATION_JSON)
val user = response.body<User>()
```

`suspendGet`, `suspendPost`, `suspendPut`, `suspendPatch`, `suspendDelete`는 요청 실행과 body decoding까지 한 번에 수행합니다. 세밀한 status mapping이 필요하면 `ResponseSpec` 층을 사용하거나 `RestClient` builder에 공통 status handler를 등록합니다.

## 빈 body 계약을 고른다

Non-null 함수는 `body(T::class.java)!!`를 사용합니다. HTTP 2xx라도 body가 없으면 실패합니다.

```kotlin
val required: User = client.suspendGet("/users/1")
val optional: User? = client.suspendGetOrNull("/users/1")
```

404를 `null`로 바꾸는 기능은 아닙니다. `OrNull`은 성공 응답의 body가 없을 때만 `null`을 허용하며, `retrieve()`가 만든 HTTP status 예외는 그대로 전파됩니다.

## cancellation은 interrupt 신호다

`runInterruptible`은 coroutine 취소 시 실행 thread를 interrupt합니다. 1.12.1 테스트는 blocking request factory가 `InterruptedException`을 처리할 때 실제로 interrupt가 전달되는지 검증합니다.

요청 factory가 interrupt를 무시하거나 native I/O가 다른 중단 규칙을 사용하면 즉시 끊긴다고 보장할 수 없습니다. connect/read timeout과 connection pool 제한은 여전히 client configuration에 둬야 합니다.

## converter와 직렬화는 애플리케이션 소유다

`suspendPost`는 `contentType`, `accept`, 선택적 body를 설정하고 Spring `RestClient` converter로 직렬화합니다. 이 모듈은 `ObjectMapper`, converter, authentication, retry를 설치하지 않습니다.

Spring Boot 4와 Jackson 3을 쓴다면 애플리케이션 starter와 JSON 설정이 runtime에 있어야 합니다. 외부 API DTO와 domain model을 분리하고, decoding 오류를 빈 결과로 바꾸지 않습니다.

## Source와 tests

- [`RestClientBuilderDsl.kt`](../../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/http/RestClientBuilderDsl.kt)
- [`RestClientExtensions.kt`](../../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/http/RestClientExtensions.kt)
- [`RestClientCoroutinesDsl.kt`](../../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/http/RestClientCoroutinesDsl.kt)
- [`RestClientExtensionsTest.kt`](../../../../../spring-boot/core/src/test/kotlin/io/bluetape4k/spring/http/RestClientExtensionsTest.kt)
- [`RestClientCoroutinesDslTest.kt`](../../../../../spring-boot/core/src/test/kotlin/io/bluetape4k/spring/http/RestClientCoroutinesDslTest.kt)

## 다음 읽을 장

[WebFlux 요청 컨텍스트와 DataBuffer](./webflux-request-and-buffers.md)에서 reactive server의 요청과 buffer 수명주기를 다룹니다.
