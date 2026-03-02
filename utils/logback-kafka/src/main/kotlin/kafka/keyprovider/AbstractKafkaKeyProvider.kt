package io.bluetape4k.logback.kafka.keyprovider

import ch.qos.logback.core.spi.ContextAwareBase
import ch.qos.logback.core.spi.LifeCycle

/**
 * Kafka 키 제공자 공통 베이스 클래스입니다.
 *
 * ## 동작/계약
 * - Logback [ContextAwareBase]와 [LifeCycle]을 구현합니다.
 * - [start]/[stop] 시 [errorWasShown] 플래그를 초기화합니다.
 */
abstract class AbstractKafkaKeyProvider<E: Any>: ContextAwareBase(), KafkaKeyProvider<E>, LifeCycle {

    protected var errorWasShown: Boolean = false

    override fun start() {
        errorWasShown = false
    }

    override fun stop() {
        errorWasShown = false
    }

    override fun isStarted(): Boolean = true
}
