# Module bluetape4k-kafka4

English | [한국어](./README.ko.md)

`bluetape4k-kafka4` is the Kafka 4.x line of the bluetape4k Kafka utilities.
It keeps the same Kotlin-first API shape as `bluetape4k-kafka`, but is compiled
against Kafka 4.2.x, Spring Kafka 4.x, Spring Boot 4, and Jackson 3.

## Compatibility

| Module | Kafka | Spring Kafka | Spring Boot | Jackson | Notes |
|---|---:|---:|---:|---:|---|
| `bluetape4k-kafka` | 3.9.x | 3.3.x | 3.x | Jackson 2 | Existing Kafka 3 line |
| `bluetape4k-kafka4` | 4.2.x | 4.0.x | 4.x | Jackson 3 | Kafka 4 line, KRaft-only embedded tests |

Do not put both modules on the same runtime classpath unless you intentionally
manage the duplicate `io.bluetape4k.kafka` API package boundary. Choose one line
per application.

## Features

- Coroutine wrappers for Kafka producer and consumer operations.
- Spring Kafka extensions for `KafkaTemplate`, producer factories, listener
  containers, and test utilities.
- Kafka Streams helpers and test coverage compiled against Kafka 4.
- Codecs for string, byte array, Jackson 3 JSON, JDK serialization, Kryo, Fory,
  and compressed payloads with LZ4, Snappy, or Zstd.
- Embedded Kafka test support through Spring Kafka 4's KRaft-only broker.

## Dependency

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

## Dependency Boundary

![Dependency Boundary 1](../../docs/images/readme-diagrams/infra-kafka4-diagram-01.png)

`infra/kafka4/build.gradle.kts` also aligns all `org.apache.kafka` artifacts to
the Kafka 4 version used by this module. This prevents the root dependency
management from pulling Kafka 3 artifacts into the Kafka 4 test/runtime
classpath.

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

Spring Kafka 4 uses non-null key/value generic boundaries more strictly than the
Kafka 3 line. Prefer non-null value types in templates and factories unless your
application has an explicit tombstone/null-value contract.

## Jackson 3 Codec

```kotlin
import io.bluetape4k.kafka.codec.JacksonKafkaCodec

class ExampleJacksonCodec : JacksonKafkaCodec() {
    override val allowedTypePackages = setOf("java.util")
}

val codec = ExampleJacksonCodec()
val bytes = codec.serialize("events", mapOf("name" to "spring-kafka-4"))
val decoded = codec.deserialize("events", bytes)
```

The Jackson codec uses `bluetape4k-jackson3` and `tools.jackson.*` APIs.

Available codecs:

| Codec | Description |
|---|---|
| `KafkaCodecs.String` | UTF-8 string serialization |
| `KafkaCodecs.ByteArray` | Raw byte array passthrough |
| `KafkaCodecs.Jackson` | Jackson 3 JSON serialization |
| `KafkaCodecs.Jdk` | **Deprecated** — JDK serialization (RCE risk, use Fory) |
| `KafkaCodecs.Kryo` | Kryo binary serialization |
| `KafkaCodecs.Fory` | Fory binary serialization |
| `KafkaCodecs.LZ4Jdk` | LZ4 compression + Java serialization |
| `KafkaCodecs.Lz4Kryo` | LZ4 compression + Kryo serialization |
| `KafkaCodecs.Lz4Fory` | LZ4 compression + Fory serialization |
| `KafkaCodecs.SnappyJdk` | Snappy compression + Java serialization |
| `KafkaCodecs.SnappyKryo` | Snappy compression + Kryo serialization |
| `KafkaCodecs.SnappyFory` | Snappy compression + Fory serialization |
| `KafkaCodecs.ZstdJdk` | Zstd compression + Java serialization |
| `KafkaCodecs.ZstdKryo` | Zstd compression + Kryo serialization |
| `KafkaCodecs.ZstdFory` | Zstd compression + Fory serialization |

### Poison-pill Policy

`AbstractKafkaCodec.deserialize` exposes a documented poison-pill policy:

| Throwable type        | Behavior                                         |
|-----------------------|--------------------------------------------------|
| Generic `Exception`   | WARN log + return `null` (consumer loop continues) |
| `CancellationException` | **Rethrown** — coroutine cancellation is preserved |
| `Error` (OOM, StackOverflow, …) | **Propagated** — JVM corruption is not hidden |

For permanent loss prevention, combine with Spring-Kafka's
`ErrorHandlingDeserializer` and `DeadLetterPublishingRecoverer` to route
poisoned records to a DLQ topic.

### Performance: Opt-out of Value-Type Header

By default, `AbstractKafkaCodec` writes the Java FQN of the value type to every
record header (`bluetape4k.kafka.codec.value.type`). This enables polymorphic
deserialization but adds bandwidth and storage overhead for high-throughput topics.
It also widens the class-loading attack surface described below.

If your consumer already knows the value type statically, disable the header:

```kotlin
// Fory/Kryo codecs work safely without the header
class NoHeaderForyCodec : ForyKafkaCodec() {
    override val writeValueTypeHeader = false
}
```

> **Note for `JacksonKafkaCodec`:** Jackson uses the header to determine the target type at
> deserialization time. Setting `writeValueTypeHeader = false` without also overriding
> `doDeserialize` causes it to fall back to `LinkedHashMap` (silent type corruption).
> Use `ForyKafkaCodec` or `KryoKafkaCodec` when you want to disable the header safely.

| `writeValueTypeHeader` | Effect |
|------------------------|--------|
| `true` (default) | FQN written to every record header — polymorphic deserialization works |
| `false` | Header omitted — no bandwidth overhead, smaller attack surface. Consumer must know the type statically. |

### Security: Class Loading Allowlist

Trust profile: `AllowListedTypes` by default. Use
`UnsafeLegacyCompatibility` only through `AbstractKafkaCodec.ALLOW_ALL_TYPES_UNSAFE`.

`AbstractKafkaCodec` loads the deserialization target class from the Kafka
header `bluetape4k.kafka.codec.value.type`. If that header can be set by an
attacker (untrusted broker or external network), arbitrary class loading (RCE)
is possible.

Override `allowedTypePackages` to restrict which packages may be loaded:

```kotlin
class SecureJacksonCodec : JacksonKafkaCodec() {
    override val allowedTypePackages = setOf(
        "com.example.dto",
        "io.bluetape4k.domain",
    )
}
```

| `allowedTypePackages` value | Effect |
|-----------------------------|--------|
| `emptySet()` (default) | **Deny all** — no class is loaded from the type header. Safe default for untrusted or shared topics. |
| Non-empty set | Only classes whose FQN equals or starts with a listed prefix are allowed; others → poison-pill `null`. |
| `AbstractKafkaCodec.ALLOW_ALL_TYPES_UNSAFE` | Bypass all checks — restores pre-1.8.0 allow-all behavior. Use only in fully trusted, internally controlled deployments. |

Legacy migration example:

```kotlin
class LegacyTrustedJacksonCodec : JacksonKafkaCodec() {
    override val allowedTypePackages = AbstractKafkaCodec.ALLOW_ALL_TYPES_UNSAFE
}
```

### Security: JdkKafkaCodec Deprecated

`JdkKafkaCodec` is deprecated due to JDK deserialization RCE risks. Use
`ForyKafkaCodec` instead — it is both faster and safer.

## Embedded Kafka Tests

Spring Kafka 4 embedded brokers are KRaft-only. Do not use `kraft = true`; that
flag belonged to the Spring Kafka 3 transition period.

```kotlin
import org.springframework.kafka.test.context.EmbeddedKafka

@EmbeddedKafka(
    partitions = 1,
    topics = ["events"],
    bootstrapServersProperty = "spring.kafka.bootstrap-servers",
)
class EventKafkaTest
```

## Verification

```bash
./gradlew :bluetape4k-kafka4:compileKotlin
./gradlew :bluetape4k-kafka4:compileTestKotlin
./gradlew :bluetape4k-kafka4:test
```
