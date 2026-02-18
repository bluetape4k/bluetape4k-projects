package io.bluetape4k.kafka.spring.support

import io.bluetape4k.kafka.AbstractKafkaTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotBeEmpty
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.Test

/**
 * [KafkaUtils]의 포맷팅 유틸리티 함수에 대한 테스트 클래스입니다.
 */
class KafkaUtilsTest: AbstractKafkaTest() {
    companion object: KLoggingChannel()

    @Test
    fun `ProducerRecord를 포맷된 문자열로 변환`() {
        val record =
            ProducerRecord(
                "test-topic",
                0,
                System.currentTimeMillis(),
                "test-key",
                "test-value",
            )

        val formatted = record.prettyString()

        formatted.shouldNotBeEmpty()
        formatted shouldContain "test-topic"
        formatted shouldContain "test-key"
        formatted shouldContain "test-value"
    }

    @Test
    fun `키가 없는 ProducerRecord 포맷팅`() {
        val record =
            ProducerRecord<String, String>(
                "test-topic",
                null,
                "test-value",
            )

        val formatted = record.prettyString()

        formatted.shouldNotBeEmpty()
        formatted shouldContain "test-topic"
        formatted shouldContain "test-value"
    }

    @Test
    fun `ConsumerRecord를 포맷된 문자열로 변환`() {
        val record =
            ConsumerRecord<String, String>(
                "test-topic",
                0,
                42L,
                "test-key",
                "test-value",
            )

        val formatted = record.prettyString()

        formatted.shouldNotBeEmpty()
        formatted shouldContain "test-topic"
        // Spring Kafka의 format 결과는 기본적으로 toString()과 유사하며
        // key와 value가 항상 포함되지는 않을 수 있음
    }

    @Test
    fun `빈 값을 가진 ProducerRecord 포맷팅`() {
        val record =
            ProducerRecord<String, String>(
                "empty-topic",
                "",
                "",
            )

        val formatted = record.prettyString()

        formatted.shouldNotBeEmpty()
        formatted shouldContain "empty-topic"
    }

    @Test
    fun `특수 문자가 포함된 레코드 포맷팅`() {
        val record =
            ProducerRecord(
                "special-topic",
                "key-with-special-chars-!@#",
                "value-with-newline\nand-tab\there",
            )

        val formatted = record.prettyString()

        formatted.shouldNotBeEmpty()
        formatted shouldContain "special-topic"
    }

    @Test
    fun `긴 값을 가진 레코드 포맷팅`() {
        val longValue = "a".repeat(1000)
        val record =
            ProducerRecord<String, String>(
                "long-topic",
                "long-key",
                longValue,
            )

        val formatted = record.prettyString()

        formatted.shouldNotBeEmpty()
        formatted shouldContain "long-topic"
    }
}
