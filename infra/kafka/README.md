# bluetape4k-kafka

Apache Kafka를 Kotlin 환경에서 효율적으로 사용하기 위한 유틸리티 라이브러리입니다. Kafka 클라이언트, Spring Kafka, Kafka Streams를 Kotlin 코루틴과 함께 사용할 수 있도록 다양한 확장 함수와 래퍼 클래스를 제공합니다.

## 특징

- **Kotlin Coroutines 지원**: Kafka Producer/Consumer 작업을 suspend 함수로 수행
- **다양한 직렬화 지원**: Jackson, Kryo, FST, LZ4/Snappy/Zstd 압축을 포함한 다양한 Codec 제공
- **Spring Kafka 통합**: Spring Kafka의 KafkaTemplate, 리스너 등을 위한 Kotlin 확장 함수
- **Kafka Streams 지원**: KStream, KTable 작업을 위한 편의 함수들
- **테스트 유틸리티**: Embedded Kafka를 활용한 테스트 지원

## 설치

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-kafka:$version")
}
```

### Gradle (Groovy DSL)

```groovy
dependencies {
    implementation 'io.bluetape4k:bluetape4k-kafka:$version'
}
```

### Maven

```xml

<dependency>
    <groupId>io.bluetape4k</groupId>
    <artifactId>bluetape4k-kafka</artifactId>
    <version>${version}</version>
</dependency>
```

## 의존성

이 모듈은 다음 라이브러리에 의존합니다:

- `org.apache.kafka:kafka-clients` - Kafka 클라이언트
- `org.springframework.kafka:spring-kafka` - Spring Kafka 지원
- `io.bluetape4k:bluetape4k-io` - 직렬화 관련 유틸리티
- `io.bluetape4k:bluetape4k-jackson` - JSON 직렬화 지원
- `io.projectreactor.kafka:reactor-kafka` - Reactive Kafka 지원

## 사용 예시

### 1. Kafka Producer 생성

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

// 메시지 발행
producer.send(ProducerRecord("test-topic", "key", "value"))
producer.close()
```

### 2. Coroutine 환경에서 Kafka Producer 사용

```kotlin
import io.bluetape4k.kafka.coroutines.suspendSend
import io.bluetape4k.kafka.coroutines.sendAsFlow
import kotlinx.coroutines.flow.flow

suspend fun produceMessages() {
    val producer = producerOf<String, String>(/* config */)

    // 단일 메시지 발행
    val record = ProducerRecord("test-topic", "key", "value")
    val metadata = producer.suspendSend(record)
    println("Sent to partition ${metadata.partition()}, offset ${metadata.offset()}")

    // Flow를 통한 다수 메시지 발행
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

### 3. Kafka Consumer 생성

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

### 4. Kafka Codecs 사용

```kotlin
import io.bluetape4k.kafka.codec.KafkaCodecs

// 문자열 Codec
val stringCodec = KafkaCodecs.String
val bytes = stringCodec.serialize("test-topic", "Hello Kafka")
val message = stringCodec.deserialize("test-topic", bytes)

// Jackson JSON Codec
val jacksonCodec = KafkaCodecs.Jackson
val data = mapOf("name" to "John", "age" to 30)
val jsonBytes = jacksonCodec.serialize("test-topic", data)
val decoded = jacksonCodec.deserialize("test-topic", jsonBytes)

// LZ4 압축 + Kryo 직렬화
val lz4KryoCodec = KafkaCodecs.Lz4Kryo
val largeObject = LargeDataObject(/* ... */)
val compressed = lz4KryoCodec.serialize("test-topic", largeObject)
```

사용 가능한 Codecs:

| Codec                   | 설명                   |
|-------------------------|----------------------|
| `KafkaCodecs.String`    | UTF-8 문자열 직렬화        |
| `KafkaCodecs.ByteArray` | 바이트 배열 직접 전달         |
| `KafkaCodecs.Jackson`   | JSON 직렬화             |
| `KafkaCodecs.Jdk`       | Java 직렬화             |
| `KafkaCodecs.Kryo`      | Kryo 바이너리 직렬화        |
| `KafkaCodecs.Fory`      | FST 바이너리 직렬화         |
| `KafkaCodecs.LZ4Jdk`    | LZ4 압축 + Java 직렬화    |
| `KafkaCodecs.Lz4Kryo`   | LZ4 압축 + Kryo 직렬화    |
| `KafkaCodecs.SnappyJdk` | Snappy 압축 + Java 직렬화 |
| `KafkaCodecs.ZstdKryo`  | Zstd 압축 + Kryo 직렬화   |

### 5. Spring KafkaTemplate과 Coroutines

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

### 6. SuspendKafkaProducerTemplate 사용

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
    // 단순 발송
    producerTemplate.send("test-topic", "value")

    // 키와 함께 발송
    producerTemplate.send("test-topic", "key", "value")

    // ProducerRecord와 함께 발송
    val result = producerTemplate.send(ProducerRecord("test-topic", "key", "value"))
    println("Sent to partition ${result.recordMetadata().partition()}")
}
```

### 7. SuspendKafkaConsumerTemplate 사용

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
    consumerTemplate.receive().collect { record ->
        println("Received: ${record.value()}")
        // 수동 커밋
        record.receiverOffset().commit().await()
    }
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
    // 토픽 소비
    val consumed = consumedOf(
        keySerde = Serdes.String(),
        valueSerde = Serdes.String(),
        resetPolicy = Topology.AutoOffsetReset.EARLIEST
    )

    // 그룹화
    val grouped = groupedOf(
        keySerde = Serdes.String(),
        valueSerde = Serdes.Long().asSerde(),
        name = "group-by-key"
    )

    // 결과 생산
    val produced = producedOf(
        keySerde = Serdes.String(),
        valueSerde = Serdes.Long().asSerde()
    )

    // 상태 저장소
    val materialized = materializedOf<String, Long, KeyValueStore<Bytes, ByteArray>>(
        "count-store"
    )

    // Topology 구성
    builder.stream("input-topic", consumed)
        .groupByKey(grouped)
        .count(materialized)
        .toStream()
        .to("output-topic", produced)
}
```

### 9. 테스트 유틸리티

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
    fun `메시지 발행 및 수신 테스트`() {
        // Producer 생성
        val producer = KafkaProducer<String, String>(
            embeddedKafka.producerProps(),
            StringSerializer(),
            StringSerializer()
        )

        // 메시지 발행
        producer.send(ProducerRecord("test-topic", "key", "value"))
        producer.flush()

        // Consumer 생성
        val consumer = KafkaConsumer<String, String>(
            embeddedKafka.consumerProps("test-group", autoCommit = false),
            StringDeserializer(),
            StringDeserializer()
        )
        consumer.subscribe(listOf("test-topic"))

        // 메시지 수신 검증
        val record = consumer.getSingleRecord("test-topic", Duration.ofSeconds(5))
        assertThat(record.value()).isEqualTo("value")

        consumer.close()
        producer.close()
    }
}
```

## 패키지 구조

```
io.bluetape4k.kafka
├── codec/                    # Kafka 직렬화/역직렬화 Codec
│   ├── KafkaCodec.kt         # 기본 Codec 인터페이스
│   ├── KafkaCodecs.kt        # Codec 인스턴스 제공
│   ├── JacksonKafkaCodec.kt  # JSON 직렬화
│   ├── BinaryKafkaCodecs.kt  # 바이너리 직렬화 (JDK, Kryo, FST)
│   ├── StringKafkaCodec.kt   # 문자열 직렬화
│   └── ByteArrayKafkaCodec.kt # 바이트 배열 직렬화
├── coroutines/               # Coroutine 지원
│   └── ProducerCoroutines.kt # Producer용 suspend 함수
├── spring/                   # Spring Kafka 통합
│   ├── KafkaOperationsExtensions.kt  # KafkaTemplate 확장
│   ├── core/                 # 핵심 Spring Kafka 지원
│   │   ├── SuspendKafkaProducerTemplate.kt  # Suspend Producer
│   │   ├── SuspendKafkaConsumerTemplate.kt  # Suspend Consumer
│   │   ├── KafkaOperationExtensions.kt      # KafkaOperations 확장
│   │   └── ProducerFactorySupport.kt        # ProducerFactory 지원
│   ├── listener/             # 리스너 유틸리티
│   │   ├── ListenerUtils.kt
│   │   └── adapter/
│   ├── support/              # 지원 유틸리티
│   │   └── KafkaUtils.kt
│   └── test/utils/           # 테스트 유틸리티
│       └── KafkaTestUtils.kt
├── streams/                  # Kafka Streams 지원
│   ├── StreamConfig.kt       # Streams 설정
│   └── kstream/              # KStream DSL 확장
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
├── ProducerSupport.kt        # Producer 생성 유틸리티
├── ConsumerSupport.kt        # Consumer 생성 유틸리티
└── TopicPartitionSupport.kt  # TopicPartition 유틸리티
```

## 참고 자료

- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Spring for Apache Kafka](https://spring.io/projects/spring-kafka)
- [Kafka Streams Documentation](https://kafka.apache.org/documentation/streams/)
- [Microservices with Spring Boot and Kafka Demo Project](https://www.github.com/piomin/sample-spring-kafka-microservices)
- [Embedded Kafka를 통한 Kafka 테스트](https://velog.io/@wodyd202/Embedded-Kafka%EB%A5%BC-%ED%86%B5%ED%95%9C-Kafka-%ED%85%8C%EC%8A%A4%ED%8A%B8)

## 라이선스

Apache License 2.0
