---
title: Connection과 option 소유권
description: natsOptions DSL, Connection 생성·공유·종료, reconnect와 listener의 책임을 설명합니다.
manualId: bluetape4k-nats
chapterId: connection-options-ownership
---

# Connection과 option 소유권

## DSL이 하는 일

`natsOptions { ... }`는 매번 새 `Options.Builder`를 만들고 block을 적용한 뒤 `build()`합니다. `natsOptions(properties)`는 `Properties`를 초기값으로 쓰며, block에서 값을 덮어쓸 수 있습니다. `natsOptionsOf`는 URL, 최대 reconnect 횟수, buffer size 세 값만 빠르게 지정하는 helper입니다.

```kotlin
val options = natsOptions {
    servers(arrayOf("nats://nats-a:4222", "nats://nats-b:4222"))
    maxReconnects(20)
    bufferSize(8 * 1024 * 1024)
}

val connection = Nats.connect(options)
```

DSL은 builder를 Kotlin 문법으로 감쌀 뿐입니다. authentication, TLS, reconnect wait, ping와 executor의 의미 및 기본값은 jNATS가 결정합니다. 유효하지 않은 값도 별도 변환 없이 jNATS builder에서 실패합니다.

## 누가 connection을 소유하는가

이 모듈에는 connection provider나 global cache가 없습니다. `Nats.connect(options)`를 호출한 컴포넌트가 반환된 `Connection`의 소유자입니다. 소유자는 다음 책임을 함께 가져야 합니다.

- endpoint와 credential을 구성합니다.
- connection event와 async error listener를 등록합니다.
- reconnect가 끝내 실패했을 때 application readiness를 어떻게 바꿀지 정합니다.
- 종료 시 새 작업을 막고 subscription·consumer를 drain한 뒤 connection을 닫습니다.

짧은 command나 test에서는 `use`로 범위를 드러낼 수 있습니다.

```kotlin
Nats.connect(options).use { connection ->
    connection.publish("jobs.triggered", "nightly")
    connection.flush(2.seconds)
}
```

장기 실행 서버에서는 request마다 connection을 열지 않습니다. application 시작 시 하나를 만들고 필요한 publisher와 consumer에 주입한 뒤 shutdown hook에서 정리합니다.

## `drain`과 `close`

`Connection.drainSuspending(timeout)`은 jNATS의 asynchronous drain future를 기다립니다. timeout은 0 이상이어야 하며 음수는 `IllegalArgumentException`입니다.

```kotlin
try {
    serve(connection)
} finally {
    val drained = connection.drainSuspending(10.seconds)
    if (!drained) logger.warn { "NATS connection drain timed out" }
    connection.close()
}
```

drain 결과가 `true`라고 해서 애플리케이션의 모든 외부 side effect가 완료된 것은 아닙니다. callback이 database나 HTTP 작업을 시작했다면 그 작업의 lifecycle도 별도로 기다려야 합니다.

## reconnect 정책

reconnect는 잠깐 끊긴 network를 견디게 하지만 메시지 처리 자체를 재시도하지는 않습니다. core NATS publish가 disconnect 경계에서 어떻게 처리되는지, pending buffer가 가득 차면 무엇이 실패하는지 jNATS 설정과 application test로 확인합니다.

connection listener는 상태 전환 관찰에 쓰고 credential이나 token이 포함된 server URL을 그대로 log에 남기지 않습니다. readiness는 `CONNECTED` 하나만 보고 판단하기보다 서비스가 실제로 publish/request할 수 있는지와 JetStream 의존성을 함께 반영합니다.

## Spring에서의 소유권

`nats-spring`은 `compileOnly`입니다. 이 artifact에는 Spring bean이나 auto-configuration이 없습니다. Spring Boot 애플리케이션이 `Connection` bean을 만들었다면 같은 connection을 다른 singleton이 별도로 생성하지 않도록 합니다. bean destroy method나 lifecycle component에서 drain·close를 한 번만 수행합니다.

## Source와 tests

- [`Options.kt`](../../../../../infra/nats/src/main/kotlin/io/bluetape4k/nats/client/Options.kt)
- [`ConnectionExtensions.kt`](../../../../../infra/nats/src/main/kotlin/io/bluetape4k/nats/client/ConnectionExtensions.kt)
- [`Consumer.kt`](../../../../../infra/nats/src/main/kotlin/io/bluetape4k/nats/client/Consumer.kt)
- [`OptionsTest.kt`](../../../../../infra/nats/src/test/kotlin/io/bluetape4k/nats/client/OptionsTest.kt)
- [`ConsumerExtensionsTest.kt`](../../../../../infra/nats/src/test/kotlin/io/bluetape4k/nats/client/ConsumerExtensionsTest.kt)
- [`AbstractNatsTest.kt`](../../../../../infra/nats/src/test/kotlin/io/bluetape4k/nats/AbstractNatsTest.kt)

1.12.1 test fixture는 `Connection.use`와 Testcontainer를 사용합니다. production lifecycle과 reconnect 종료 정책은 애플리케이션 통합 테스트가 보완해야 합니다.
