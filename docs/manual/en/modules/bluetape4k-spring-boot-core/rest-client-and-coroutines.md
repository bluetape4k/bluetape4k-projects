---
title: RestClient and coroutine boundaries
description: Explains thread, cancellation, body, and converter contracts when a blocking RestClient is called from suspend functions.
manualId: bluetape4k-spring-boot-core
chapterId: rest-client-and-coroutines
---

# RestClient and coroutine boundaries

## RestClient remains blocking

The coroutine DSL runs Spring's blocking HTTP client in `runInterruptible(Dispatchers.IO)`. It provides a suspend call site but does not change the I/O implementation into a reactive client.

```kotlin
val client = restClientOf("https://api.example.com") {
    defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer $token")
}

suspend fun loadUser(id: Long): User =
    client.suspendGet("/users/$id", MediaType.APPLICATION_JSON)
```

This avoids executing `RestClient.retrieve().body()` directly on a WebFlux event-loop thread. At high concurrency, the blocking connection pool and `Dispatchers.IO` capacity still matter.

## Keep the two API layers distinct

`httpGet`, `httpPost`, `httpPut`, `httpPatch`, `httpDelete`, `httpHead`, and `httpOptions` return `RestClient.ResponseSpec`. The caller can add status handling and choose how to decode the body.

```kotlin
val response = client.httpGet("/users/1", MediaType.APPLICATION_JSON)
val user = response.body<User>()
```

The `suspendGet`, `suspendPost`, `suspendPut`, `suspendPatch`, and `suspendDelete` family performs the request and body decoding in one function. Use the `ResponseSpec` layer or a builder-level status handler when status mapping needs more control.

## Choose the empty-body contract

Non-null functions call `body(T::class.java)!!`; even a successful response fails if it has no body.

```kotlin
val required: User = client.suspendGet("/users/1")
val optional: User? = client.suspendGetOrNull("/users/1")
```

`OrNull` does not map 404 to `null`. It permits an absent body on a successful response; HTTP status exceptions from `retrieve()` still propagate.

## Cancellation sends an interrupt

`runInterruptible` interrupts the executing thread when the coroutine is cancelled. The 1.12.1 test confirms the signal with a blocking request factory that handles `InterruptedException`.

Immediate request abortion is not guaranteed if the request factory ignores interrupts or its native I/O has different cancellation rules. Configure connect and read timeouts and connection-pool limits on the client as well.

## The application owns converters and serialization

`suspendPost` sets optional content type, accept header, and body, then delegates serialization to Spring `RestClient` converters. This module does not install an `ObjectMapper`, converter, authentication, or retry policy.

With Spring Boot 4 and Jackson 3, the application starter and JSON configuration must be present at runtime. Keep external DTOs separate from domain models, and do not turn decoding failures into empty results.

## Source and tests

- [`RestClientBuilderDsl.kt`](../../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/http/RestClientBuilderDsl.kt)
- [`RestClientExtensions.kt`](../../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/http/RestClientExtensions.kt)
- [`RestClientCoroutinesDsl.kt`](../../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/http/RestClientCoroutinesDsl.kt)
- [`RestClientExtensionsTest.kt`](../../../../../spring-boot/core/src/test/kotlin/io/bluetape4k/spring/http/RestClientExtensionsTest.kt)
- [`RestClientCoroutinesDslTest.kt`](../../../../../spring-boot/core/src/test/kotlin/io/bluetape4k/spring/http/RestClientCoroutinesDslTest.kt)

## Next chapter

[WebFlux request context and DataBuffer](./webflux-request-and-buffers.md) covers request propagation and buffer ownership in a reactive server.
