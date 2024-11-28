package io.bluetape4k.spring.webflux.controller

import io.bluetape4k.concurrent.virtualthread.VT
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * [Dispatchers.VT] 를 사용하는 CoroutineScope 를 제공하는 Controller의 추상 클래스입니다.
 */
abstract class AbstractCoroutineVTController
    : CoroutineScope by CoroutineScope(Dispatchers.VT + SupervisorJob()) {

    companion object: KLogging()
}
