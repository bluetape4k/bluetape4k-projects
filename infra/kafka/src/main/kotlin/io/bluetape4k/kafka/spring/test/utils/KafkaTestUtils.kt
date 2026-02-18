package io.bluetape4k.kafka.spring.test.utils

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.TopicPartition
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.utils.KafkaTestUtils
import java.time.Duration

/**
 * EmbeddedKafkaBroker에서 Producer 설정을 생성합니다.
 *
 * 테스트용 Kafka Producer를 생성하기 위한 설정 맵을 반환합니다.
 *
 * 사용 예시:
 * ```kotlin
 * @Autowired
 * lateinit var embeddedKafka: EmbeddedKafkaBroker
 *
 * val producer = KafkaProducer<String, String>(
 *     embeddedKafka.producerProps(),
 *     StringSerializer(),
 *     StringSerializer()
 * )
 * ```
 *
 * @return Producer 설정 맵
 */
fun EmbeddedKafkaBroker.producerProps(): MutableMap<String, Any?> = KafkaTestUtils.producerProps(this)

/**
 * EmbeddedKafkaBroker에서 Consumer 설정을 생성합니다.
 *
 * 테스트용 Kafka Consumer를 생성하기 위한 설정 맵을 반환합니다.
 *
 * 사용 예시:
 * ```kotlin
 * @Autowired
 * lateinit var embeddedKafka: EmbeddedKafkaBroker
 *
 * val consumer = KafkaConsumer<String, String>(
 *     embeddedKafka.consumerProps("test-group", autoCommit = false),
 *     StringDeserializer(),
 *     StringDeserializer()
 * )
 * ```
 *
 * @param group Consumer 그룹 ID
 * @param autoCommit 자동 커밋 여부
 * @return Consumer 설정 맵
 */
fun EmbeddedKafkaBroker.consumerProps(
    group: String,
    autoCommit: Boolean = false,
): MutableMap<String, Any?> = KafkaTestUtils.consumerProps(this.brokersAsString, group, autoCommit.toString())

/**
 * 지정된 토픽과 파티션의 마지막 오프셋을 조회합니다.
 *
 * 테스트에서 특정 파티션의 끝 오프셋을 확인할 때 유용합니다.
 *
 * 사용 예시:
 * ```kotlin
 * val endOffsets = consumer.getEndOffsets("test-topic", 0, 1, 2)
 * endOffsets.forEach { (topicPartition, offset) ->
 *     println("Partition ${topicPartition.partition()}: offset $offset")
 * }
 * ```
 *
 * @param K 키 타입
 * @param V 값 타입
 * @param topic 토픽 이름
 * @param partitions 파티션 번호들
 * @return 파티션별 마지막 오프셋 맵
 */
fun <K, V> Consumer<K, V>.getEndOffsets(
    topic: String,
    vararg partitions: Int,
): Map<TopicPartition, Long> = KafkaTestUtils.getEndOffsets(this, topic, *partitions.toTypedArray())

/**
 * 지정된 토픽에서 단일 레코드를 가져옵니다.
 *
 * 테스트에서 특정 토픽의 메시지를 하나만 수신할 때 사용합니다.
 *
 * 사용 예시:
 * ```kotlin
 * val record = consumer.getSingleRecord("test-topic", Duration.ofSeconds(5))
 * println("Received: ${record.value()}")
 * ```
 *
 * @param K 키 타입
 * @param V 값 타입
 * @param topic 토픽 이름
 * @param timeout 대기 시간
 * @return 수신된 ConsumerRecord
 */
fun <K, V> Consumer<K, V>.getSingleRecord(
    topic: String,
    timeout: Duration = Duration.ofSeconds(10),
): ConsumerRecord<K, V> = KafkaTestUtils.getSingleRecord(this, topic, timeout)

/**
 * Consumer에서 레코드들을 가져옵니다.
 *
 * 테스트에서 여러 메시지를 수신할 때 사용합니다.
 *
 * 사용 예시:
 * ```kotlin
 * val records = consumer.getRecords(Duration.ofSeconds(5), minRecords = 3)
 * records.forEach { record ->
 *     println("Received: ${record.value()}")
 * }
 * ```
 *
 * @param K 키 타입
 * @param V 값 타입
 * @param timeout 대기 시간
 * @param minRecords 최소 수신할 레코드 수 (-1이면 시간 만료까지 대기)
 * @return 수신된 ConsumerRecords
 */
fun <K, V> Consumer<K, V>.getRecords(
    timeout: Duration = Duration.ofSeconds(10),
    minRecords: Int = -1,
): ConsumerRecords<K, V> = KafkaTestUtils.getRecords(this, timeout, minRecords)

/**
 * 객체의 속성 값을 가져옵니다.
 *
 * 중첩된 속성에 접근할 때 유용합니다 (예: "innerBean.property").
 *
 * 사용 예시:
 * ```kotlin
 * val propertyValue: String = someBean.getPropertyValue("config.name")
 * ```
 *
 * @param T 속성 값 타입
 * @param path 속성 경로 (점 표기법 지원)
 * @return 속성 값
 */
inline fun <reified T> Any.getPropertyValue(path: String): T =
    KafkaTestUtils.getPropertyValue(this, path, T::class.java)
