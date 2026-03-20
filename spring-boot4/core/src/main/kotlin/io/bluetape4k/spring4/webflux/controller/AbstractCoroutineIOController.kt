package io.bluetape4k.spring4.webflux.controller

import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * [Dispatchers.IO] 기반 [CoroutineScope]를 위임해 제공하는 WebFlux 컨트롤러 추상 클래스입니다.
 *
 * ## 동작/계약
 * - 스코프는 `Dispatchers.IO + SupervisorJob()` 조합으로 생성됩니다.
 * - 형제 코루틴 중 하나가 실패해도 `SupervisorJob` 특성상 다른 자식 코루틴은 즉시 취소되지 않습니다.
 * - 별도 취소 처리를 하지 않으면 스코프 생명주기는 인스턴스 생명주기를 따릅니다.
 *
 * ```kotlin
 * class FileController: AbstractCoroutineIOController()
 * ```
 */
abstract class AbstractCoroutineIOController: CoroutineScope by CoroutineScope(Dispatchers.IO + SupervisorJob()) {
    companion object: KLoggingChannel()
}
