package io.bluetape4k.kafka.spring.listener

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.kafka.listener.MessageListener

class ListenerUtilsTest {

    companion object: KLoggingChannel()

    @Test
    fun `listenerTypeOf - MessageListener 타입을 반환한다`() {
        val listener = MessageListener<String, String> { _ -> }

        val listenerType = listenerTypeOf(listener)

        listenerType.shouldNotBeNull()
    }
}
