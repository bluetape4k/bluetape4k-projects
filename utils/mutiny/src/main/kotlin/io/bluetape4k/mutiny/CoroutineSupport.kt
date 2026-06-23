package io.bluetape4k.mutiny

import io.smallrye.mutiny.Uni
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel

/**
 * 코루틴 스코프에서 `suspend` 블록을 실행하고 결과를 [Uni]로 감쌉니다.
 *
 * ## 동작/계약
 * - 반환된 [Uni]를 구독할 때 현재 [CoroutineScope] 컨텍스트에서 코루틴 실행을 시작합니다.
 * - 구독이 취소되면 실행 중인 코루틴도 취소됩니다.
 * - 수신 스코프를 변경하지 않으며 새 [Uni] 인스턴스를 반환합니다.
 * - 블록에서 예외 또는 cancellation 이 발생하면 실패한 `Uni`로 전파됩니다.
 *
 * ```kotlin
 * val scope = CoroutineScope(Dispatchers.Default)
 * val uni = scope.asUni { 42L }
 * val result = uni.await().indefinitely()
 * // result == 42L
 * ```
 */
@OptIn(ExperimentalCoroutinesApi::class)
inline fun <T> CoroutineScope.asUni(
    crossinline block: suspend CoroutineScope.() -> T,
): Uni<T> {
    return Uni.createFrom().emitter { emitter ->
        val deferred = async {
            block(this@asUni)
        }

        deferred.invokeOnCompletion { cause ->
            if (cause == null) {
                emitter.complete(deferred.getCompleted())
            } else {
                emitter.fail(cause)
            }
        }

        emitter.onTermination {
            if (deferred.isActive) {
                deferred.cancel()
            }
        }
    }
}
