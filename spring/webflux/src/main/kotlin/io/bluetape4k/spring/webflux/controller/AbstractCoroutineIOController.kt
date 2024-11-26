package io.bluetape4k.spring.webflux.controller

import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * [Dispatchers.IO] 를 사용하는 CoroutineScope 를 제공하는 Controller의 추상 클래스입니다.
 */
abstract class AbstractCoroutineIOController
    : CoroutineScope by CoroutineScope(Dispatchers.IO + SupervisorJob()) {

    companion object: KLogging()
}
