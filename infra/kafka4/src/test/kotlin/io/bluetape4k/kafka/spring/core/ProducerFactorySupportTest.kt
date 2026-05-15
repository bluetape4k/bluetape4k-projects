package io.bluetape4k.kafka.spring.core

import io.bluetape4k.logging.KLogging
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.kafka.core.KafkaResourceHolder
import org.springframework.kafka.core.ProducerFactoryUtils

/**
 * [KafkaResourceHolder.release] 확장 함수에 대한 단위 테스트입니다.
 *
 * [ProducerFactoryUtils.releaseResources]는 static 메서드이므로 [mockkStatic]을 사용합니다.
 */
class ProducerFactorySupportTest {

    companion object : KLogging()

    @BeforeEach
    fun setup() {
        mockkStatic(ProducerFactoryUtils::class)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(ProducerFactoryUtils::class)
    }

    @Test
    fun `release - KafkaResourceHolder release 호출 시 ProducerFactoryUtils releaseResources 위임`() {
        // Arrange
        val holder = mockk<KafkaResourceHolder<String, String>>(relaxed = true)
        every { ProducerFactoryUtils.releaseResources(any<KafkaResourceHolder<*, *>>()) } returns Unit

        // Act
        holder.release()

        // Assert
        verify(exactly = 1) { ProducerFactoryUtils.releaseResources(holder) }
        confirmVerified(holder)
    }
}
