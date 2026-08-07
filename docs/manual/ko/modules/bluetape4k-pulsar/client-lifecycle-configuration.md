---
title: Client 구성과 수명주기
description: PulsarClient builder, 직접 소유와 block scope, 설정 위치와 1.12.1 close 경계를 설명합니다.
manualId: bluetape4k-pulsar
chapterId: client-lifecycle-configuration
---

# Client 구성과 수명주기

## 두 가지 생성 방식

`pulsarClient(serviceUrl, setup)`은 새 `PulsarClient`를 반환합니다. URL이 비어 있으면 setup에서 `serviceUrl()`을 호출해야 합니다.

```kotlin
val client = pulsarClient("pulsar://localhost:6650") {
    connectionTimeout(5, TimeUnit.SECONDS)
}

val tlsClient = pulsarClient {
    serviceUrl("pulsar+ssl://broker:6651")
    tlsTrustCertsFilePath("/run/secrets/pulsar-ca.pem")
}
```

두 경우 모두 client는 새 resource입니다. 직접 생성했다면 호출자가 닫아야 합니다.

## block scope helper

`withPulsarClient`는 block이 정상 반환하거나 예외를 던진 뒤 `finally`에서 `closeAsync()`를 기다립니다.

```kotlin
val result = withPulsarClient(url) {
    withProducer(Schema.STRING, { topic(topic) }) {
        sendSuspend("event")
    }
}
```

producer, consumer, reader의 `with*`도 같은 구조입니다. 내부 block이 소유한 resource를 밖으로 반환하면 이미 close된 객체가 될 수 있으므로 scope 밖으로 유출하지 않습니다.

## 1.12.1 close 계약

1.12.1은 `runCatching { closeAsync().awaitSuspending() }`으로 close를 시도하고 실패를 warning log로 남깁니다. close 실패는 block의 반환값이나 원래 예외를 바꾸지 않습니다.

문제는 coroutine이 이미 취소된 경우입니다. 이 버전은 close await를 `NonCancellable` context에서 실행하지 않습니다. close 호출을 시도해도 취소 중에 close가 끝날 때까지 보장되지는 않습니다. 이후 branch의 `PulsarCloseSupport`를 1.12.1 기능으로 간주하면 안 됩니다.

## 애플리케이션 수명 client

웹 서비스에서는 client를 요청마다 만들지 말고 애플리케이션 컴포넌트가 하나를 소유합니다. client가 producer·consumer·reader보다 오래 살아야 합니다. 종료 순서는 새 작업 차단, in-flight 대기, child resource close, client close가 기본입니다.

`withPulsarClient`는 CLI, migration 도구, bounded batch처럼 작업 범위와 client 범위가 같은 경우에 사용합니다. long-running consumer에는 명시적 owner와 shutdown timeout이 더 잘 맞습니다.

## 설정 책임

authentication, TLS, connection timeout, listener와 memory limit는 native `ClientBuilder` 설정입니다. 이 모듈은 property binding이나 secret loading을 제공하지 않습니다. credential은 애플리케이션 설정 계층에서 주입하고 URL·token을 log에 남기지 않습니다.

## Source와 tests

- [`PulsarClientSupport.kt`](../../../../../infra/pulsar/src/main/kotlin/io/bluetape4k/pulsar/PulsarClientSupport.kt)
- [`ProducerSupport.kt`](../../../../../infra/pulsar/src/main/kotlin/io/bluetape4k/pulsar/producer/ProducerSupport.kt)
- [`ConsumerSupport.kt`](../../../../../infra/pulsar/src/main/kotlin/io/bluetape4k/pulsar/consumer/ConsumerSupport.kt)
- [`ReaderSupport.kt`](../../../../../infra/pulsar/src/main/kotlin/io/bluetape4k/pulsar/reader/ReaderSupport.kt)
- [`PulsarClientSupportTest.kt`](../../../../../infra/pulsar/src/test/kotlin/io/bluetape4k/pulsar/PulsarClientSupportTest.kt)
