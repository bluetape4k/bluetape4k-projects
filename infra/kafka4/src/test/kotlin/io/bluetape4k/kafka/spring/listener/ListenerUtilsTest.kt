package io.bluetape4k.kafka.spring.listener

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.logging.KLogging
import io.mockk.every
import io.mockk.mockk
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Test
import org.springframework.kafka.listener.AcknowledgingMessageListener
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.ListenerType
import org.springframework.kafka.listener.MessageListener
import org.springframework.kafka.listener.MessageListenerContainer
import org.springframework.kafka.support.Acknowledgment

/**
 * [listenerTypeOf], [stoppableSleep], [createOffsetAndMetadata] 확장 함수에 대한 단위 테스트입니다.
 */
class ListenerUtilsTest {

    companion object : KLogging()

    @Test
    fun `MessageListener SAM 구현체 전달 시 ListenerType SIMPLE 반환`() {
        // Arrange
        val listener = MessageListener<String, String> { _ -> /* no-op */ }

        // Act
        val listenerType = listenerTypeOf(listener)

        // Assert
        listenerType shouldBeEqualTo ListenerType.SIMPLE
    }

    @Test
    fun `AcknowledgingMessageListener 구현체 전달 시 ListenerType ACKNOWLEDGING 반환`() {
        // Arrange
        val listener = AcknowledgingMessageListener<String, String> { _: ConsumerRecord<String, String>, _: Acknowledgment? -> /* no-op */ }

        // Act
        val listenerType = listenerTypeOf(listener)

        // Assert
        listenerType shouldBeEqualTo ListenerType.ACKNOWLEDGING
    }

    @Test
    fun `stoppableSleep - 컨테이너가 중지 상태이면 예외 없이 완료`() {
        // Arrange
        val container = mockk<MessageListenerContainer>(relaxed = true)
        every { container.isRunning } returns false

        // Act & Assert - 예외가 발생하지 않아야 함
        // stoppableSleep 내부에서 isRunning 외 다른 mock 호출이 있을 수 있으므로 confirmVerified 미사용
        container.stoppableSleep(50L)
    }

    @Test
    fun `createOffsetAndMetadata - 지정한 오프셋으로 OffsetAndMetadata 생성`() {
        // Arrange
        // offsetAndMetadataProvider가 null인 실제 ContainerProperties를 사용해야
        // ListenerUtils가 new OffsetAndMetadata(offset) 경로를 타서 정확한 offset 값을 반환함
        val containerProps = ContainerProperties("test-topic")
        // offsetAndMetadataProvider 기본값이 null이므로 별도 설정 불필요

        val container = mockk<MessageListenerContainer>(relaxed = true)
        every { container.containerProperties } returns containerProps

        // Act
        val metadata = container.createOffsetAndMetadata(100L)

        // Assert
        metadata.shouldNotBeNull()
        metadata.offset() shouldBeEqualTo 100L
    }
}
