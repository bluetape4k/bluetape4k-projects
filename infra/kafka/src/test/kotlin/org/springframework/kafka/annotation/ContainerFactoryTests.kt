package org.springframework.kafka.annotation

import io.bluetape4k.kafka.spring.test.utils.getPropertyValue
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.mockk.mockk
import kotlinx.atomicfu.atomic
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContainSame
import org.junit.jupiter.api.Test
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory

class ContainerFactoryTests {

    companion object: KLoggingChannel()

    @Test
    fun `config container`() {
        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()
        factory.setAutoStartup(false)
        factory.setConcurrency(22)

        val cf = mockk<ConsumerFactory<String, String>>(relaxUnitFun = true)
        factory.consumerFactory = cf
        factory.setPhase(42)
        factory.containerProperties.ackCount = 123

        val customized = atomic(false)
        factory.setContainerCustomizer {
            customized.compareAndSet(false, true)
        }

        val container = factory.createContainer("foo")

        container.isAutoStartup.shouldBeFalse()
        container.phase shouldBeEqualTo 42
        container.containerProperties.ackCount shouldBeEqualTo 123
        container.getPropertyValue<Int>("concurrency") shouldBeEqualTo 22
        customized.value.shouldBeTrue()

        val container2 = factory.createContainer("foo")
        container.containerProperties.kafkaConsumerProperties shouldContainSame container2.containerProperties.kafkaConsumerProperties
    }

}
