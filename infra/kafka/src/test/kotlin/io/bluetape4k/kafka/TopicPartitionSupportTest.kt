package io.bluetape4k.kafka

import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldThrow
import org.amshove.kluent.withMessage
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

/**
 * [TopicPartition] 관련 유틸리티 함수에 대한 테스트 클래스입니다.
 */
class TopicPartitionSupportTest: AbstractKafkaTest() {
    companion object: KLoggingChannel()

    @ParameterizedTest
    @CsvSource(
        "test-topic-0, test-topic, 0",
        "my.topic.name-5, my.topic.name, 5",
        "a-1, a, 1",
        "topic-with-many-dashes-123, topic-with-many-dashes, 123",
        "user.events-42, user.events, 42",
    )
    fun `유효한 topic-partition 문자열 파싱`(
        input: String,
        expectedTopic: String,
        expectedPartition: Int,
    ) {
        val tp = input.toTopicPartition()

        tp.topic() shouldBeEqualTo expectedTopic
        tp.partition() shouldBeEqualTo expectedPartition
    }

    @ParameterizedTest
    @CsvSource(
        "test-topic-0, test-topic, 0",
        "my.topic.name-5, my.topic.name, 5",
        "a-1, a, 1",
        "topic-with-many-dashes-123, topic-with-many-dashes, 123",
    )
    fun `topicPartitionOf 함수로 파싱`(
        input: String,
        expectedTopic: String,
        expectedPartition: Int,
    ) {
        val tp = topicPartitionOf(input)

        tp.topic() shouldBeEqualTo expectedTopic
        tp.partition() shouldBeEqualTo expectedPartition
    }

    @Test
    fun `TopicPartition 객체 생성 검증`() {
        val tp = TopicPartition("test-topic", 0)
        tp.topic() shouldBeEqualTo "test-topic"
        tp.partition() shouldBeEqualTo 0
    }

    @Test
    fun `여러 파티션 번호 파싱`() {
        val partitions = listOf(0, 1, 2, 10, 100, 999)
        val topic = "multi-partition-topic"

        partitions.forEach { partition ->
            val tp = "$topic-$partition".toTopicPartition()
            tp.topic() shouldBeEqualTo topic
            tp.partition() shouldBeEqualTo partition
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "   "])
    fun `빈 문자열은 예외 발생`(input: String) {
        val exception = { input.toTopicPartition() }
        exception shouldThrow AssertionError::class
    }

    @Test
    fun `구분자가 없는 문자열은 예외 발생`() {
        val exception = { "invalidtopicname".toTopicPartition() }
        exception shouldThrow IllegalArgumentException::class withMessage "Not found kafka topic-position delimiter (-)"
    }

    @Test
    fun `숫자가 아닌 파티션 번호는 예외 발생`() {
        val exception = { "topic-abc".toTopicPartition() }
        exception shouldThrow NumberFormatException::class
    }

    @Test
    fun `음수 파티션 번호 파싱`() {
        // "test-topic--1"에서 마지막 -를 기준으로 split하면 ["test-topic-", "1"]
        val tp = "test-topic--1".toTopicPartition()
        tp.topic() shouldBeEqualTo "test-topic-"
        tp.partition() shouldBeEqualTo 1
    }

    @Test
    fun `마지막 대시를 기준으로 파싱`() {
        val tp = "topic-with-dashes-123".toTopicPartition()
        tp.topic() shouldBeEqualTo "topic-with-dashes"
        tp.partition() shouldBeEqualTo 123
    }

    @Test
    fun `복합 토픽 이름 파싱`() {
        val tp = "com.company.service.events-7".toTopicPartition()
        tp.topic() shouldBeEqualTo "com.company.service.events"
        tp.partition() shouldBeEqualTo 7
    }
}
