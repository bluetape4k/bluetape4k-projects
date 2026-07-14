---
title: WebFlux 요청 컨텍스트와 DataBuffer
description: Reactor Context 요청 전달, 내부 경로 치환, DataBuffer Flow와 pooled buffer 수명주기를 설명합니다.
manualId: bluetape4k-spring-boot-core
chapterId: webflux-request-and-buffers
---

# WebFlux 요청 컨텍스트와 DataBuffer

## 현재 요청은 Reactor Context에 둔다

`HttpRequestCapturer`는 `ServerHttpRequest` 복제본을 `ServerHttpRequest::class.java` 키로 Reactor Context에 저장합니다. `HttpRequestHolder.getHttpRequest()`는 구독 시점의 context를 읽고 값이 없으면 빈 `Mono`를 반환합니다.

```kotlin
val request = HttpRequestHolder.getHttpRequest().awaitSingleOrNull()
val requestId = request?.headers?.getFirst("X-Request-Id")
```

ThreadLocal 전역 holder가 아닙니다. filter chain 밖에서 새 publisher를 만들거나 Reactor Context를 잃으면 요청도 조회되지 않습니다. request 전체를 장기간 저장하지 말고 필요한 값만 읽어 domain 호출에 넘깁니다.

## component scan과 필터 순서

`HttpRequestCapturer`는 `@Component`이므로 application scan 범위에 들어와야 합니다. 다른 filter가 context 값을 필요로 한다면 ordering을 명시하고 실제 request pipeline에서 검증합니다.

`AbstractRedirectWebFilter`는 HTTP redirect 응답을 보내지 않습니다. path가 정확히 일치할 때 `ServerWebExchange`의 request path를 바꿔 다음 filter로 넘기는 내부 rewrite입니다. query와 나머지 exchange 정보는 유지됩니다.

## DataBuffer를 Flow로 읽는다

`InputStream`, channel, `Path`, Spring `Resource`를 `Flow<DataBuffer>`로 바꿀 수 있습니다.

```kotlin
fun download(path: Path, factory: DataBufferFactory): Flow<DataBuffer> =
    path.readAsDataBuffer(factory, bufferSize = 16 * 1024)
```

반환 Flow는 수집할 때 읽기를 시작합니다. 여러 번 수집하면 source를 여러 번 열거나 같은 stream을 재사용하려 할 수 있으므로 source 수명을 확인합니다. 대용량 응답은 `join()`으로 합치지 말고 streaming을 유지합니다.

## byte 제한과 메모리 경계

`takeUntilByteCount`는 지정한 누적 byte까지만 전달하고, `skipUntilByteCount`는 그만큼 건너뜁니다. `join(maxByteCount)`는 여러 buffer를 하나로 합칩니다.

외부 입력을 무제한 `join()`하면 heap 또는 direct memory를 소진할 수 있습니다. protocol이나 application limit을 `maxByteCount`로 명시하고, 초과 시 Spring이 발생시키는 오류를 client에게 어떻게 매핑할지 정합니다.

## pooled buffer를 해제한다

Netty pooled `DataBuffer`는 reference count를 갖습니다. `retain`, `touch`, `release` helper는 `DataBufferUtils`에 위임합니다.

```kotlin
val retained = buffer.retain()
try {
    consume(retained)
} finally {
    retained.release()
}
```

소유권을 넘겨받은 쪽만 해제해야 합니다. 이미 framework가 소비·해제하는 buffer를 다시 release하거나, 별도 비동기 작업으로 넘기면서 retain하지 않으면 use-after-release 또는 leak이 생길 수 있습니다. `write(OutputStream)`과 channel overload는 대상 stream/channel을 자동으로 닫지 않습니다.

## Source와 tests

- [`HttpRequestCapturer.kt`](../../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/webflux/filter/HttpRequestCapturer.kt)
- [`HttpRequestHolder.kt`](../../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/webflux/filter/HttpRequestHolder.kt)
- [`AbstractRedirectWebFilter.kt`](../../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/webflux/filter/AbstractRedirectWebFilter.kt)
- [`DataBufferSupport.kt`](../../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/core/io/buffer/DataBufferSupport.kt)
- [`HttpRequestFilterTest.kt`](../../../../../spring-boot/core/src/test/kotlin/io/bluetape4k/spring/webflux/filter/HttpRequestFilterTest.kt)
- [`DataBufferSupportTest.kt`](../../../../../spring-boot/core/src/test/kotlin/io/bluetape4k/spring/core/io/buffer/DataBufferSupportTest.kt)

## 다음 읽을 장

[오류 응답과 관측](./error-responses-and-observability.md)에서 Web 경계의 실패를 응답과 telemetry로 표현하는 방법을 설명합니다.
