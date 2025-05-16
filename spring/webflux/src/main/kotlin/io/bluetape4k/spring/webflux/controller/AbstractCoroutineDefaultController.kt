package io.bluetape4k.spring.webflux.controller

import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * [Dispatchers.Default] 를 사용하는 CoroutineScope 를 제공하는 Controller의 추상 클래스입니다.
 */
abstract class AbstractCoroutineDefaultController:
    CoroutineScope by CoroutineScope(Dispatchers.Default + SupervisorJob()) {

    companion object: KLoggingChannel()
}
