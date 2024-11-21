package io.bluetape4k.logback.kafka.keyprovider

import ch.qos.logback.core.spi.ContextAwareBase
import ch.qos.logback.core.spi.LifeCycle

abstract class AbstractKafkaKeyProvider<E>: ContextAwareBase(), KafkaKeyProvider<E>, LifeCycle {

    protected var errorWasShown: Boolean = false

    override fun start() {
        errorWasShown = false
    }

    override fun stop() {
        errorWasShown = false
    }

    override fun isStarted(): Boolean = true
}
