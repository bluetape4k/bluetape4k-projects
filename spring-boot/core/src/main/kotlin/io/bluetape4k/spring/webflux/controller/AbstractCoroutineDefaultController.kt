package io.bluetape4k.spring.webflux.controller

import io.bluetape4k.logging.coroutines.KLoggingChannel
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob

/**
 * [Dispatchers.Default] 기반 [CoroutineScope]를 위임해 제공하는 WebFlux 컨트롤러 추상 클래스입니다.
 *
 * ## 동작/계약
 * - 스코프는 `Dispatchers.Default + SupervisorJob()` 조합으로 생성됩니다.
 * - CPU 중심 작업을 기본 Dispatcher에서 실행하도록 설계된 베이스 타입입니다.
 * - `SupervisorJob`을 사용하므로 한 자식 코루틴 실패가 다른 자식 코루틴을 즉시 취소하지 않습니다.
 * - Spring Security `ReactiveSecurityContextHolder` 컨텍스트는 이 스코프에 자동 전파되지 않습니다.
 *   보안 컨텍스트가 필요한 경우 `coroutineContext[ReactorContext]`를 통해 수동으로 전파해야 합니다.
 * - 빈 소멸 시 [cancelCoroutineScope]가 호출되어 진행 중인 코루틴이 취소됩니다.
 *
 * ```kotlin
 * class ComputeController: AbstractCoroutineDefaultController()
 * ```
 */
abstract class AbstractCoroutineDefaultController:
    CoroutineScope by CoroutineScope(Dispatchers.Default + SupervisorJob()) {
    companion object: KLoggingChannel()

    @PreDestroy
    fun cancelCoroutineScope() {
        coroutineContext[Job]?.cancel()
    }
}
