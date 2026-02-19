# Module bluetape4k-nats

[Nats.io](https://nats.io/)는 클라우드 네이티브 애플리케이션, IoT 메시징, 마이크로서비스 아키텍처를 위한 단순하고 안전하며 고성능의 오픈소스 메시징 시스템입니다.

이 모듈은 NATS를 Kotlin에서 더욱 편리하게 사용할 수 있도록 하는 확장 함수와 DSL을 제공합니다.

## 특징

- **Kotlin 확장 함수**: NATS Java 클라이언트를 코틀린스럽게 사용
- **Coroutines 지원**: `suspend` 함수를 통한 비동기 작업 처리
- **JetStream 지원**: 스트림 생성, 메시지 발행/구독, 소비자 관리
- **NATS Service**: 마이크로서비스 엔드포인트 구축 지원
- **DSL 제공**: Stream, Consumer, Key-Value, Object Store 설정을 위한 DSL

## 의존성

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-nats:${bluetape4kVersion}")
}
```

## 주요 기능

### 1. Connection 확장 함수

```kotlin
import io.bluetape4k.nats.client.*
import io.nats.client.Nats
import kotlin.time.Duration.Companion.seconds

// NATS 연결 생성
val connection = Nats.connect("nats://localhost:4222")

// 메시지 발행
connection.publish("subject", "Hello, NATS!")

// Request-Reply 패턴
val response = connection.request("subject", "request body", timeout = 5.seconds)

// 비동기 Request
val future = connection.requestAsync("subject", "body")

// Coroutines 지원
suspend fun coroutineExample() {
    val response = connection.requestSuspending("subject", "body".toUtf8Bytes())
}

// 연결 종료
connection.drainSuspending(10.seconds)
```

### 2. JetStream 지원

```kotlin
import io.bluetape4k.nats.client.*
import io.nats.client.api.StorageType

// JetStream 생성
val jetStream = connection.jetStream()

// 메시지 발행
val ack = jetStream.publish("stream.subject", "message body")

// 비동기 발행
val future = jetStream.publishAsync("stream.subject", "message body")

// Coroutines 지원
suspend fun publishAsync() {
    val ack = jetStream.coPublish("stream.subject", "message body")
}

// Stream 생성
val streamInfo = connection.createStream(
    streamName = "my-stream",
    storageType = StorageType.Memory,
    subjects = arrayOf("events.*")
)
```

### 3. JetStreamManagement

```kotlin
import io.bluetape4k.nats.client.*

val management = connection.jetStreamManagement()

// Stream 관리
management.createStream("my-stream", subjects = arrayOf("orders.*"))
management.createOrReplaceStream("my-stream", subjects = arrayOf("orders.*"))
management.createStreamOrUpdateSubjects("my-stream", subjects = arrayOf("orders.*", "payments.*"))

// Stream 조회
val exists = management.streamExists("my-stream")
val info = management.getStreamInfoOrNull("my-stream")

// Stream 삭제
management.forcedDeleteStream("my-stream")
management.forcedPurgeStream("my-stream")

// Consumer 관리
val consumerExists = management.consumerExists("my-stream", "my-consumer")
management.forcedDeleteConsumer("my-stream", "my-consumer")
```

### 4. Subscription 확장

```kotlin
import io.bluetape4k.nats.client.nextMessage
import kotlin.time.Duration.Companion.seconds

val subscription = connection.subscribe("subject")
val message = subscription.nextMessage(5.seconds)
```

### 5. NATS Service

```kotlin
import io.bluetape4k.nats.service.*
import io.nats.service.ServiceEndpoint

// 서비스 엔드포인트 생성
val endpoint = ServiceEndpoint.builder()
    .endpoint(Endpoint.builder().name("echo").subject("service.echo").build())
    .handler { msg -> msg.respond(connection, msg.data) }
    .build()

// 서비스 생성
val service = natsServiceOf(
    nc = connection,
    name = "my-service",
    version = "1.0.0",
    endpoint
)

// DSL 방식
val service = natsService {
    connection(connection)
    name("my-service")
    version("1.0.0")
    addServiceEndpoint(endpoint)
}
```

### 6. Stream Configuration DSL

```kotlin
import io.bluetape4k.nats.client.api.*
import io.nats.client.api.*

val config = streamConfiguration {
    name("my-stream")
    subjects("events.*", "logs.*")
    storageType(StorageType.File)
    retentionPolicy(RetentionPolicy.Limits)
    maxMessages(100000)
    maxBytes(1024 * 1024 * 100)  // 100MB
    maxAge(Duration.ofDays(7))
}
```

### 7. Consumer Configuration DSL

```kotlin
import io.bluetape4k.nats.client.api.*

val config = consumerConfiguration {
    name("my-consumer")
    durable("my-consumer-durable")
    deliverPolicy(DeliverPolicy.All)
    ackPolicy(AckPolicy.Explicit)
    maxDeliver(3)
    maxAckPending(1000)
}
```

### 8. Key-Value Store

```kotlin
import io.bluetape4k.nats.client.*

val kvManagement = connection.keyValueManagement()

// Bucket 생성
val config = keyValueConfiguration {
    name("my-bucket")
    maxHistoryPerKey(5)
    ttl(3600)  // 1시간
}
kvManagement.create(config)

// Key-Value 작업
val kv = connection.keyValue("my-bucket")
kv.put("key", "value")
val value = kv.get("key")
kv.delete("key")
```

### 9. Object Store

```kotlin
import io.bluetape4k.nats.client.*
import io.bluetape4k.nats.client.api.*

val objManagement = connection.objectStoreManagement()

// Bucket 생성
val config = objectStoreConfiguration {
    name("my-objects")
    maxBytes(1024 * 1024 * 1000)  // 1GB
}
objManagement.create(config)

// 객체 작업
val store = connection.objectStore("my-objects")
store.put("file.txt", inputStream)
val obj = store.get("file.txt")
store.delete("file.txt")
```

## 테스트 지원

`AbstractNatsTest`를 상속하여 테스트를 작성할 수 있습니다:

```kotlin
class MyNatsTest: AbstractNatsTest() {

    @Test
    fun `메시지 발행 및 수신`() {
        val subject = "test.subject"
        val message = "Hello, NATS!"

        // 구독
        val subscription = connection.subscribe(subject)

        // 발행
        connection.publish(subject, message)

        // 수신 확인
        val received = subscription.nextMessage(5.seconds)
        received.data.toUtf8String() shouldBeEqualTo message
    }
}
```

## 예제

더 많은 예제는 `src/test/kotlin/io/nats/examples` 및 `src/test/kotlin/io/bluetape4k/nats` 패키지에서 확인할 수 있습니다.

### 주요 예제 목록

- `PubSubExample.kt`: 기본 발행/구독 예제
- `RequestReplyExample.kt`: Request-Reply 패턴 예제
- `jetstream/`: JetStream 관련 예제
- `service/`: NATS Service 예제
- `KeyValueIntroExamples.kt`: Key-Value 스토어 예제
- `ObjectStoreExample.kt`: Object Store 예제

## 참고 자료

- [NATS 공식 문서](https://docs.nats.io/)
- [NATS Java 클라이언트](https://github.com/nats-io/nats.java)
- [JetStream 문서](https://docs.nats.io/nats-concepts/jetstream)
- [NATS Service API](https://docs.nats.io/nats-concepts/service)

## 라이선스

Apache License 2.0
