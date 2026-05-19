# Module bluetape4k-kafka

English | [한국어](./README.ko.md)

A utility library for using Apache Kafka efficiently in a Kotlin environment. Provides extension functions and wrapper classes for Kafka clients, Spring Kafka, and Kafka Streams with Kotlin Coroutines support.

## Features

- **Kotlin Coroutines support**: Kafka Producer/Consumer operations as suspend functions
- **Multiple serialization formats**: Codecs for Jackson, Kryo, Fory, and compression with LZ4/Snappy/Zstd
- **Spring Kafka integration**: Kotlin extension functions for KafkaTemplate, listeners, and more
- **Kafka Streams support**: Convenience functions for KStream and KTable operations
- **Test utilities**: Testing support using Embedded Kafka

## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-kafka:$version")
}
```

### Gradle (Groovy DSL)

```groovy
dependencies {
    implementation 'io.github.bluetape4k:bluetape4k-kafka:$version'
}
```

### Maven

```xml

<dependency>
    <groupId>io.github.bluetape4k</groupId>
    <artifactId>bluetape4k-kafka</artifactId>
    <version>${version}</version>
</dependency>
```

## Dependencies

This module depends on the following libraries:

- `org.apache.kafka:kafka-clients` - Kafka client
- `org.springframework.kafka:spring-kafka` - Spring Kafka support
- `io.github.bluetape4k:bluetape4k-io` - Serialization utilities
- `io.github.bluetape4k:bluetape4k-jackson2` - JSON serialization support
- `io.projectreactor.kafka:reactor-kafka` - Reactive Kafka support

## Usage Examples

### 1. Creating a Kafka Producer

```kotlin
import io.bluetape4k.kafka.producerOf
import org.apache.kafka.common.serialization.StringSerializer

val producer = producerOf(
    mapOf(
        "bootstrap.servers" to "localhost:9092",
        "acks" to "all",
        "retries" to 3,
        "key.serializer" to StringSerializer::class.java,
        "value.serializer" to StringSerializer::class.java,
    )
)

// Publish a message
producer.send(ProducerRecord("test-topic", "key", "value"))
producer.close()
```

### 2. Using a Kafka Producer in a Coroutine Context

```kotlin
import io.bluetape4k.kafka.coroutines.suspendSend
import io.bluetape4k.kafka.coroutines.sendAsFlow
import kotlinx.coroutines.flow.flow

suspend fun produceMessages() {
    val producer = producerOf<String, String>(/* config */)

    // Publish a single message
    val record = ProducerRecord("test-topic", "key", "value")
    val metadata = producer.suspendSend(record)
    println("Sent to partition ${metadata.partition()}, offset ${metadata.offset()}")

    // Publish multiple messages via Flow
    val records = flow {
        repeat(100) { i ->
            emit(ProducerRecord("test-topic", "key-$i", "value-$i"))
        }
    }
    producer.sendAsFlow(records).collect { metadata ->
        println("Sent: ${metadata.offset()}")
    }

    producer.close()
}
```

### 3. Creating a Kafka Consumer

```kotlin
import io.bluetape4k.kafka.consumerOf
import org.apache.kafka.common.serialization.StringDeserializer

val consumer = consumerOf<String, String>(
    mapOf(
        "bootstrap.servers" to "localhost:9092",
        "group.id" to "test-group",
        "auto.offset.reset" to "earliest",
        "key.deserializer" to StringDeserializer::class.java,
        "value.deserializer" to StringDeserializer::class.java,
    )
)

consumer.subscribe(listOf("test-topic"))
while (true) {
    val records = consumer.poll(Duration.ofMillis(100))
    for (record in records) {
        println("Received: ${record.value()}")
    }
}
```

### 4. Using Kafka Codecs

```kotlin
import io.bluetape4k.kafka.codec.KafkaCodecs

// String codec
val stringCodec = KafkaCodecs.String
val bytes = stringCodec.serialize("test-topic", "Hello Kafka")
val message = stringCodec.deserialize("test-topic", bytes)

// Jackson JSON codec
val jacksonCodec = KafkaCodecs.Jackson
val data = mapOf("name" to "John", "age" to 30)
val jsonBytes = jacksonCodec.serialize("test-topic", data)
val decoded = jacksonCodec.deserialize("test-topic", jsonBytes)

// LZ4 compression + Fory serialization
val lz4ForyCodec = KafkaCodecs.Lz4Fory
val largeObject = LargeDataObject(/* ... */)
val compressed = lz4ForyCodec.serialize("test-topic", largeObject)
```

Available codecs:

| Codec                   | Description                             |
|-------------------------|-----------------------------------------|
| `KafkaCodecs.String`    | UTF-8 string serialization              |
| `KafkaCodecs.ByteArray` | Raw byte array passthrough              |
| `KafkaCodecs.Jackson`   | JSON serialization                      |
| `KafkaCodecs.Jdk`       | **Deprecated** — JDK serialization (RCE risk, use Fory) |
| `KafkaCodecs.Kryo`      | Kryo binary serialization               |
| `KafkaCodecs.Fory`      | Fory binary serialization               |
| `KafkaCodecs.LZ4Jdk`    | LZ4 compression + Java serialization    |
| `KafkaCodecs.Lz4Kryo`   | LZ4 compression + Kryo serialization    |
| `KafkaCodecs.Lz4Fory`   | LZ4 compression + Fory serialization    |
| `KafkaCodecs.SnappyJdk` | Snappy compression + Java serialization |
| `KafkaCodecs.SnappyKryo` | Snappy compression + Kryo serialization |
| `KafkaCodecs.SnappyFory` | Snappy compression + Fory serialization |
| `KafkaCodecs.ZstdJdk`   | Zstd compression + Java serialization   |
| `KafkaCodecs.ZstdKryo`  | Zstd compression + Kryo serialization   |
| `KafkaCodecs.ZstdFory`  | Zstd compression + Fory serialization   |

#### Performance: Opt-out of Value-Type Header

By default, `AbstractKafkaCodec` writes the Java FQN of the value type to every
record header (`bluetape4k.kafka.codec.value.type`). Disable it when the consumer
already knows the type statically:

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

#### Security: Class Loading Allowlist

`AbstractKafkaCodec` loads the deserialization target class from the Kafka
header `bluetape4k.kafka.codec.value.type`. If that header can be set by an
attacker, arbitrary class loading (RCE) is possible.

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

### 5. Spring KafkaTemplate with Coroutines

```kotlin
import io.bluetape4k.kafka.spring.suspendSend
import org.springframework.kafka.core.KafkaTemplate

@Service
class MessageService(
    private val kafkaTemplate: KafkaTemplate<String, String>
) {
    suspend fun sendMessage(topic: String, key: String, value: String) {
        val result = kafkaTemplate.suspendSend(topic, key, value)
        println("Message sent to partition ${result.recordMetadata.partition()}")
    }

    suspend fun sendMessage(record: ProducerRecord<String, String>) {
        val result = kafkaTemplate.suspendSend(record)
        println("Message sent with offset ${result.recordMetadata.offset()}")
    }
}
```

### 6. Using SuspendKafkaProducerTemplate

```kotlin
import io.bluetape4k.kafka.spring.core.SuspendKafkaProducerTemplate
import reactor.kafka.sender.SenderOptions

val senderOptions = SenderOptions.create<String, String>(
    mapOf(
        "bootstrap.servers" to "localhost:9092",
        "key.serializer" to StringSerializer::class.java,
        "value.serializer" to StringSerializer::class.java,
        "acks" to "all",
    )
)

val producerTemplate = SuspendKafkaProducerTemplate(senderOptions)

suspend fun sendWithTemplate() {
    // Simple send
    producerTemplate.send("test-topic", "value")

    // Send with key
    producerTemplate.send("test-topic", "key", "value")

    // Send with ProducerRecord
    val result = producerTemplate.send(ProducerRecord("test-topic", "key", "value"))
    println("Sent to partition ${result.recordMetadata().partition()}")
}
```

### 7. Using SuspendKafkaConsumerTemplate

```kotlin
import io.bluetape4k.kafka.spring.core.SuspendKafkaConsumerTemplate
import reactor.kafka.receiver.ReceiverOptions

val receiverOptions = ReceiverOptions.create<String, String>(
    mapOf(
        "bootstrap.servers" to "localhost:9092",
        "group.id" to "test-group",
        "key.deserializer" to StringDeserializer::class.java,
        "value.deserializer" to StringDeserializer::class.java,
        "auto.offset.reset" to "earliest",
    )
).subscription(listOf("test-topic"))

val consumerTemplate = SuspendKafkaConsumerTemplate(receiverOptions)

suspend fun consumeWithTemplate() {
    consumerTemplate.subscribe("test-topic")
    val assignments = consumerTemplate.assignment()

    consumerTemplate.receive().collect { record ->
        println("Received: ${record.value()}")
        // Manual commit
        record.receiverOffset().commit().await()
    }

    // Consumer management operations
    consumerTemplate.commitCurrentOffsets(*assignments.toTypedArray())
    consumerTemplate.seekToTimestamp(assignments.first(), System.currentTimeMillis() - 60_000)
    consumerTemplate.unsubscribe()
}
```

### 8. Kafka Streams

```kotlin
import io.bluetape4k.kafka.streams.kstream.*
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Grouped
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.kstream.Materialized

fun buildTopology(builder: StreamsBuilder) {
    // Consume from topic
    val consumed = consumedOf(
        keySerde = Serdes.String(),
        valueSerde = Serdes.String(),
        resetPolicy = Topology.AutoOffsetReset.EARLIEST
    )

    // Group by key
    val grouped = groupedOf(
        keySerde = Serdes.String(),
        valueSerde = Serdes.Long().asSerde(),
        name = "group-by-key"
    )

    // Produce to topic
    val produced = producedOf(
        keySerde = Serdes.String(),
        valueSerde = Serdes.Long().asSerde()
    )

    // State store
    val materialized = materializedOf<String, Long, KeyValueStore<Bytes, ByteArray>>(
        "count-store"
    )

    // Build topology
    builder.stream("input-topic", consumed)
        .groupByKey(grouped)
        .count(materialized)
        .toStream()
        .to("output-topic", produced)
}
```

### 9. Test Utilities

```kotlin
import io.bluetape4k.kafka.spring.test.utils.*
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka

@EmbeddedKafka(
    partitions = 1,
    topics = ["test-topic"],
    brokerProperties = ["listeners=PLAINTEXT://localhost:9092"]
)
class KafkaIntegrationTest {

    @Autowired
    lateinit var embeddedKafka: EmbeddedKafkaBroker

    @Test
    fun `test message publish and receive`() {
        // Create producer
        val producer = KafkaProducer<String, String>(
            embeddedKafka.producerProps(),
            StringSerializer(),
            StringSerializer()
        )

        // Publish message
        producer.send(ProducerRecord("test-topic", "key", "value"))
        producer.flush()

        // Create consumer
        val consumer = KafkaConsumer<String, String>(
            embeddedKafka.consumerProps("test-group", autoCommit = false),
            StringDeserializer(),
            StringDeserializer()
        )
        consumer.subscribe(listOf("test-topic"))

        // Verify received message
        val record = consumer.getSingleRecord("test-topic", Duration.ofSeconds(5))
        assertThat(record.value()).isEqualTo("value")

        consumer.close()
        producer.close()
    }
}
```

## Architecture Diagrams

### Kafka Class Hierarchy

![Kafka Class Hierarchy 1](../../docs/images/readme-diagrams/infra-kafka-diagram-01.svg)

### Producer/Consumer Message Flow

![Producer / Consumer Message Flow diagram](../../docs/images/readme-diagrams/infra-kafka-sequence-01.png)

### Kafka Streams Processing Flow

![Kafka Streams Processing Flow 2](../../docs/images/readme-diagrams/infra-kafka-diagram-02.svg)

## Package Structure

```
io.bluetape4k.kafka
├── codec/                    # Kafka serialization/deserialization codecs
│   ├── KafkaCodec.kt         # Base codec interface
│   ├── KafkaCodecs.kt        # Codec instances
│   ├── JacksonKafkaCodec.kt  # JSON serialization
│   ├── BinaryKafkaCodecs.kt  # Binary serialization (JDK, Kryo, Fory)
│   ├── StringKafkaCodec.kt   # String serialization
│   └── ByteArrayKafkaCodec.kt # Byte array serialization
├── coroutines/               # Coroutine support
│   └── ProducerCoroutines.kt # Suspend functions for Producer
├── spring/                   # Spring Kafka integration
│   ├── KafkaOperationsExtensions.kt  # KafkaTemplate extensions
│   ├── core/                 # Core Spring Kafka support
│   │   ├── SuspendKafkaProducerTemplate.kt  # Suspend Producer
│   │   ├── SuspendKafkaConsumerTemplate.kt  # Suspend Consumer
│   │   ├── KafkaOperationExtensions.kt      # KafkaOperations extensions
│   │   └── ProducerFactorySupport.kt        # ProducerFactory support
│   ├── listener/             # Listener utilities
│   │   ├── ListenerUtils.kt
│   │   └── adapter/
│   ├── support/              # Support utilities
│   │   └── KafkaUtils.kt
│   └── test/utils/           # Test utilities
│       └── KafkaTestUtils.kt
├── streams/                  # Kafka Streams support
│   ├── StreamConfig.kt       # Streams configuration
│   └── kstream/              # KStream DSL extensions
│       ├── Consumed.kt
│       ├── Produced.kt
│       ├── Joined.kt
│       ├── Grouped.kt
│       ├── Materialized.kt
│       ├── StreamJoined.kt
│       ├── Repartitioned.kt
│       ├── TableJoined.kt
│       ├── Branched.kt
│       └── Windowed.kt
├── ProducerSupport.kt        # Producer creation utilities
├── ConsumerSupport.kt        # Consumer creation utilities
└── TopicPartitionSupport.kt  # TopicPartition utilities
```

## Security: LZ4 Migration (CVE-2025-12183, CVE-2025-66566)

`org.lz4:lz4-java` was archived in December 2025 and has two unpatched CVEs:

- **CVE-2025-12183** (CVSS 8.8) — out-of-bounds read
- **CVE-2025-66566** (CVSS 8.2) — uninitialized buffer info leak

This module migrates to the maintained fork **`at.yawk.lz4:lz4-java:1.11.0`**, which keeps the
`net.jpountz.lz4.*` package namespace (binary-compatible — no source changes required).

Because Kafka clients (`kafka-clients`, `spring-kafka`, `reactor-kafka`, `kafka-streams`) still
declare a transitive dependency on `org.lz4:lz4-java`, this module evicts it via:

```kotlin
configurations.all {
    exclude(group = "org.lz4", module = "lz4-java")
}
```

### Downstream consumers

If your application directly depends on `kafka-clients` (or any of its siblings) **without** going
through `bluetape4k-kafka`, add the same `configurations.all { exclude(...) }` block **and** declare
the replacement explicitly:

```kotlin
configurations.all {
    exclude(group = "org.lz4", module = "lz4-java")
}

dependencies {
    // Provides net.jpountz.lz4.* at runtime — required for Kafka LZ4 compression codec
    implementation("at.yawk.lz4:lz4-java:1.11.0")
}
```

`bluetape4k-kafka` already exposes `at.yawk.lz4:lz4-java:1.11.0` as an `api` dependency,
so direct users of this module do not need to add it manually.

## References

- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Spring for Apache Kafka](https://spring.io/projects/spring-kafka)
- [Kafka Streams Documentation](https://kafka.apache.org/documentation/streams/)
- [Microservices with Spring Boot and Kafka Demo Project](https://www.github.com/piomin/sample-spring-kafka-microservices)

## License

MIT License
