package io.bluetape4k.logback.kafka.exporter

import io.mockk.Called
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.apache.kafka.clients.producer.Callback
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.errors.TimeoutException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException

class DefaultKafkaExporterTest {

    private val producer = mockk<Producer<String, String>>(relaxed = true)
    private val exceptionHandler = mockk<ExportExceptionHandler<String>>(relaxed = true)
    private val exporter = DefaultKafkaExporter()

    private val topicPartition = TopicPartition("topic", 0)
    private val record = ProducerRecord<String, String>(topicPartition.topic(), topicPartition.partition(), null, "msg")
    private val recordMetadata = RecordMetadata(
        topicPartition,
        0,
        0,
        System.currentTimeMillis(),
        32,
        64
    )

    @BeforeEach
    fun beforeEach() {
        clearAllMocks()
    }

    @Test
    fun `export를 실패해도 예외가 발생하지 않으면 exceptionHandler는 호출되지 않는다`() {
        val callback = mockk<Callback>("callback")
        every { callback.onCompletion(any(), any()) } answers {
            val args = invocation.args
            val metadata = args[0] as RecordMetadata
            callback.onCompletion(metadata, null)
        }

        every { producer.send(record, callback) } returns mockk(relaxed = true)

        exporter.export(producer, record, "msg", exceptionHandler)

        verify { exceptionHandler wasNot Called }
    }

    @Test
    fun `export 에서 예외가 발생하면 exceptionHandler 가 호출된다`() {
        val exception = IOException("BAM!")

        // 참고: https://notwoods.github.io/mockk-guidebook/docs/mockito-migrate/argument-captor/
        val callbackSlot = slot<Callback>()
        every { producer.send(record, capture(callbackSlot)) } returns mockk(relaxed = true)

        exporter.export(producer, record, "msg", exceptionHandler)

        callbackSlot.captured.onCompletion(recordMetadata, exception)
        verify { exceptionHandler.handle("msg", exception) }
    }

    @Test
    fun `kafka producer에서 TimeoutException 예외가 발생하면 exception handler 가 호출된다`() {
        val exception = TimeoutException("Timeout!")

        every { producer.send(record, any<Callback>()) } throws exception

        exporter.export(producer, record, "msg", exceptionHandler)

        verify { exceptionHandler.handle("msg", exception) }
    }
}
