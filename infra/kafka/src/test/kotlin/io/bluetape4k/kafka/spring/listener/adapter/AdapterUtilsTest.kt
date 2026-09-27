package io.bluetape4k.kafka.spring.listener.adapter

import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.junit.jupiter.api.Test

class AdapterUtilsTest {

    companion object: KLoggingChannel()

    @Test
    fun `consumerRecordMetadataFromArray - 빈 배열로 호출하면 null을 반환한다`() {
        val result = consumerRecordMetadataFromArray()
        result.shouldBeNull()
    }

    @Test
    fun `consumerRecordMetadataOf - 일반 객체는 null을 반환한다`() {
        val result = consumerRecordMetadataOf("plain-string")
        result.shouldBeNull()
    }
}
