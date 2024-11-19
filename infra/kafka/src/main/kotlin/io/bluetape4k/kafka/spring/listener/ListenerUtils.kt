package io.bluetape4k.kafka.spring.listener

import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.springframework.kafka.listener.ListenerType
import org.springframework.kafka.listener.ListenerUtils
import org.springframework.kafka.listener.MessageListenerContainer

fun listenerTypeOf(listener: Any): ListenerType =
    ListenerUtils.determineListenerType(listener)

fun MessageListenerContainer.stoppableSleep(interval: Long) {
    ListenerUtils.stoppableSleep(this, interval)
}

fun MessageListenerContainer.createOffsetAndMetadata(offset: Long): OffsetAndMetadata {
    return ListenerUtils.createOffsetAndMetadata(this, offset)
}
