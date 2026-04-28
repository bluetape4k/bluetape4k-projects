package io.bluetape4k.spring.webflux.controller

import io.bluetape4k.logging.coroutines.KLoggingChannel
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob

/**
 * [Dispatchers.IO] 기반 [CoroutineScope]를 위임해 제공하는 WebFlux 컨트롤러 추상 클래스입니다.
 *
 * ## 동작/계약
 * - 스코프는 `Dispatchers.IO + SupervisorJob()` 조합으로 생성됩니다.
 * - 형제 코루틴 중 하나가 실패해도 `SupervisorJob` 특성상 다른 자식 코루틴은 즉시 취소되지 않습니다.
 * - Spring Security `ReactiveSecurityContextHolder` 컨텍스트는 이 스코프에 자동 전파되지 않습니다.
 *   보안 컨텍스트가 필요한 경우 `coroutineContext[ReactorContext]`를 통해 수동으로 전파해야 합니다.
 * - 빈 소멸 시 [cancelCoroutineScope]가 호출되어 진행 중인 코루틴이 취소됩니다.
 *
 * ```kotlin
 * class FileController: AbstractCoroutineIOController()
 * ```
 */
abstract class AbstractCoroutineIOController
    : CoroutineScope by CoroutineScope(Dispatchers.IO + SupervisorJob()) {

    companion object: KLoggingChannel()

    @PreDestroy
    fun cancelCoroutineScope() {
        coroutineContext[Job]?.cancel()
    }
}
