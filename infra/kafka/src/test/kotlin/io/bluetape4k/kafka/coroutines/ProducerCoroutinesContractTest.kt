package io.bluetape4k.kafka.coroutines

import io.bluetape4k.assertions.assertFailsWith
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.apache.kafka.clients.producer.Callback
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture

class ProducerCoroutinesContractTest {

    @Test
    fun `성공 callback에 metadata가 없으면 명시적인 실패를 반환한다`() = runTest {
        val producer = mockk<Producer<String, String>>()
        every { producer.send(any(), any()) } answers {
            secondArg<Callback>().onCompletion(null, null)
            CompletableFuture<RecordMetadata>().apply { complete(null) }
        }

        assertFailsWith<IllegalStateException> {
            producer.suspendSend(ProducerRecord("topic", "key", "value"))
        }
    }
}
