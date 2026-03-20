package io.bluetape4k.spring.webflux.controller

import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * [Dispatchers.Default] 기반 [CoroutineScope]를 위임해 제공하는 WebFlux 컨트롤러 추상 클래스입니다.
 *
 * ## 동작/계약
 * - 스코프는 `Dispatchers.Default + SupervisorJob()` 조합으로 생성됩니다.
 * - CPU 중심 작업을 기본 Dispatcher에서 실행하도록 설계된 베이스 타입입니다.
 * - `SupervisorJob`을 사용하므로 한 자식 코루틴 실패가 다른 자식 코루틴을 즉시 취소하지 않습니다.
 *
 * ```kotlin
 * class ComputeController: AbstractCoroutineDefaultController()
 * ```
 */
abstract class AbstractCoroutineDefaultController:
    CoroutineScope by CoroutineScope(Dispatchers.Default + SupervisorJob()) {
    companion object: KLoggingChannel()
}
