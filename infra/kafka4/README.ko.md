# Module bluetape4k-kafka4

[English](./README.md) | 한국어

`bluetape4k-kafka4`는 bluetape4k Kafka 유틸리티의 Kafka 4.x 라인입니다.
기존 `bluetape4k-kafka`와 같은 Kotlin 우선 API 형태를 유지하되 Kafka 4.2.x,
Spring Kafka 4.x, Spring Boot 4, Jackson 3 기준으로 컴파일합니다.

## 호환성

| 모듈 | Kafka | Spring Kafka | Spring Boot | Jackson | 비고 |
|---|---:|---:|---:|---:|---|
| `bluetape4k-kafka` | 3.9.x | 3.3.x | 3.x | Jackson 2 | 기존 Kafka 3 라인 |
| `bluetape4k-kafka4` | 4.2.x | 4.0.x | 4.x | Jackson 3 | Kafka 4 라인, Embedded Kafka는 KRaft 전용 |

두 모듈은 같은 `io.bluetape4k.kafka` API 패키지를 제공합니다. 중복 패키지 경계를
의도적으로 관리하는 경우가 아니라면 한 애플리케이션 런타임 classpath에는 둘 중 하나만
선택하세요.

## 특징

- Kafka producer/consumer 작업을 위한 coroutine 래퍼.
- `KafkaTemplate`, producer factory, listener container, test utility를 위한
  Spring Kafka 확장 함수.
- Kafka 4 기준으로 컴파일되는 Kafka Streams helper와 테스트.
- 문자열, 바이트 배열, Jackson 3 JSON, JDK 직렬화, Kryo, Fory, LZ4/Snappy/Zstd
  압축 payload codec.
- Spring Kafka 4의 KRaft 전용 embedded broker 테스트 지원.

## 의존성

### Gradle Kotlin DSL

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-kafka4:$version")
}
```

### Maven

```xml
<dependency>
    <groupId>io.github.bluetape4k</groupId>
    <artifactId>bluetape4k-kafka4</artifactId>
    <version>${version}</version>
</dependency>
```

## 의존성 경계

```mermaid
flowchart LR
    app[Application] --> kafka4[bluetape4k-kafka4]
    kafka4 --> clients[Kafka clients 4.2.x]
    kafka4 --> spring[Spring Kafka 4.0.x]
    kafka4 --> boot[Spring Boot 4 BOM]
    kafka4 --> jackson[bluetape4k-jackson3]
    kafka4 --> reactor[reactor-kafka]
```

`infra/kafka4/build.gradle.kts`는 모든 `org.apache.kafka` artifact를 이 모듈의
Kafka 4 버전으로 정렬합니다. root dependency management가 Kafka 3 artifact를
Kafka 4 test/runtime classpath에 섞는 것을 막기 위한 조치입니다.

## Producer

```kotlin
import io.bluetape4k.kafka.producerOf
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer

val producer = producerOf<String, String>(
    mapOf(
        "bootstrap.servers" to "localhost:9092",
        "acks" to "all",
        "key.serializer" to StringSerializer::class.java,
        "value.serializer" to StringSerializer::class.java,
    )
)

producer.send(ProducerRecord("events", "key", "value"))
producer.close()
```

## Coroutine Producer

```kotlin
import io.bluetape4k.kafka.coroutines.suspendSend
import org.apache.kafka.clients.producer.ProducerRecord

suspend fun sendEvent() {
    val metadata = producer.suspendSend(ProducerRecord("events", "key", "value"))
    println("sent partition=${metadata.partition()} offset=${metadata.offset()}")
}
```

## Spring Kafka

```kotlin
import io.bluetape4k.kafka.spring.suspendSend
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class EventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, String>,
) {
    suspend fun publish(value: String) {
        kafkaTemplate.suspendSend("events", "key", value)
    }
}
```

Spring Kafka 4는 Kafka 3 라인보다 key/value generic의 non-null 경계를 엄격하게
적용합니다. tombstone/null-value 계약이 명확한 경우가 아니라면 template과 factory에는
non-null value type을 우선 사용하세요.

## Jackson 3 Codec

```kotlin
import io.bluetape4k.kafka.codec.KafkaCodecs

val codec = KafkaCodecs.Jackson
val bytes = codec.serialize("events", mapOf("name" to "spring-kafka-4"))
val decoded = codec.deserialize("events", bytes)
```

Jackson codec은 `bluetape4k-jackson3`와 `tools.jackson.*` API를 사용합니다.

### Poison-pill 정책

`AbstractKafkaCodec.deserialize` 는 명시적인 poison-pill 정책을 가진다.

| Throwable 종류        | 동작                                                |
|-----------------------|-----------------------------------------------------|
| 일반 `Exception`      | WARN 로그 + `null` 반환 (consumer 루프 진행 보장)   |
| `CancellationException` | **재던짐** — 코루틴 취소 신호 보존                   |
| `Error` (OOM, StackOverflow 등) | **전파** — JVM 손상 상태를 은폐하지 않음        |

영구 손실을 막으려면 Spring-Kafka 의 `ErrorHandlingDeserializer` +
`DeadLetterPublishingRecoverer` 와 함께 사용하여 poison record 를 DLQ 토픽으로
라우팅하라.

### 보안: 클래스 로딩 허용 목록

`AbstractKafkaCodec` 은 Kafka 헤더 `bluetape4k.kafka.codec.value.type` 에서
역직렬화 대상 클래스를 로드합니다. 이 헤더를 공격자가 조작할 수 있는 환경(신뢰할 수
없는 브로커, 외부 네트워크)에서는 임의 클래스 로딩(RCE)이 가능합니다.

`allowedTypePackages` 를 오버라이드하여 로드 가능한 패키지를 제한하세요:

```kotlin
class SecureJacksonCodec : JacksonKafkaCodec() {
    override val allowedTypePackages = setOf(
        "com.example.dto",
        "io.bluetape4k.domain",
    )
}
```

| `allowedTypePackages` 값 | 동작 |
|--------------------------|------|
| `emptySet()` (기본값) | 모든 클래스 허용 — 하위 호환, 신뢰 환경 전용 |
| 비어 있지 않은 집합 | 나열된 패키지 하위 클래스만 허용; 그 외 `IllegalArgumentException` → poison-pill (`null`) |

> **경고:** 기본값 `emptySet()` 은 하위 호환성을 위해 모든 클래스 로딩을 허용합니다.
> Kafka 브로커를 완전히 신뢰할 수 없는 프로덕션 환경에서는 반드시 명시적 패키지를 지정하십시오.

### 보안: JdkKafkaCodec 지원 중단

`JdkKafkaCodec` 은 JDK 역직렬화 RCE 위험으로 인해 `@Deprecated` 처리되었습니다.
`KryoKafkaCodec` 또는 `JacksonKafkaCodec` 을 사용하세요.

## Embedded Kafka 테스트

Spring Kafka 4의 embedded broker는 KRaft 전용입니다. Spring Kafka 3 전환기에 쓰던
`kraft = true` 속성은 사용하지 않습니다.

```kotlin
import org.springframework.kafka.test.context.EmbeddedKafka

@EmbeddedKafka(
    partitions = 1,
    topics = ["events"],
    bootstrapServersProperty = "spring.kafka.bootstrap-servers",
)
class EventKafkaTest
```

## 검증

```bash
./gradlew :bluetape4k-kafka4:compileKotlin
./gradlew :bluetape4k-kafka4:compileTestKotlin
./gradlew :bluetape4k-kafka4:test
```
