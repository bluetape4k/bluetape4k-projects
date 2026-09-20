package io.bluetape4k.kafka.coroutines

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.concurrent.completableFutureOf
import io.bluetape4k.logging.KLogging
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.apache.kafka.clients.producer.Callback
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ProducerCoroutinesContractTest {

    companion object: KLogging()

    private val producer = mockk<Producer<String, String>>()

    @BeforeEach
    fun beforeEach() {
        clearMocks(producer)
    }

    @Test
    fun `성공 callback에 metadata가 없으면 명시적인 실패를 반환한다`() = runTest {
        every { producer.send(any(), any()) } answers {
            secondArg<Callback>().onCompletion(null, null)
            completableFutureOf<RecordMetadata?>(null)
        }

        assertFailsWith<IllegalStateException> {
            producer.suspendSend(ProducerRecord("topic", "key", "value"))
        }
    }
}
