package io.bluetape4k.kafka.spring.listener.adapter

import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test

/**
 * [consumerRecordMetadataFromArray], [consumerRecordMetadataOf] 확장 함수에 대한 단위 테스트입니다.
 */
class AdapterUtilsTest {

    companion object : KLogging()

    @Test
    fun `consumerRecordMetadataFromArray - 빈 배열 전달 시 null 반환`() {
        // Act
        val result = consumerRecordMetadataFromArray()

        // Assert
        result.shouldBeNull()
    }

    @Test
    fun `consumerRecordMetadataFromArray - ConsumerRecordMetadata가 아닌 문자열 전달 시 null 반환`() {
        // Act
        val result = consumerRecordMetadataFromArray("non-metadata-string")

        // Assert
        result.shouldBeNull()
    }

    @Test
    fun `consumerRecordMetadataOf - ConsumerRecordMetadata가 아닌 문자열 전달 시 null 반환`() {
        // Act
        val result = consumerRecordMetadataOf("non-metadata-string")

        // Assert
        result.shouldBeNull()
    }
}
