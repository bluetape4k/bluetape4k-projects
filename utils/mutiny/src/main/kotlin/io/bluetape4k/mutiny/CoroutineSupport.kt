package io.bluetape4k.mutiny

import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.asUni
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async

/**
 * 코루틴 스코프에서 `suspend` 블록을 실행하고 결과를 [Uni]로 감쌉니다.
 *
 * ## 동작/계약
 * - 내부적으로 `async { ... }.asUni()`를 사용하므로 블록은 현재 [CoroutineScope] 컨텍스트에서 비동기 실행됩니다.
 * - 수신 스코프를 변경하지 않으며 새 [Uni] 인스턴스를 반환합니다.
 * - 블록에서 예외가 발생하면 실패한 `Uni`로 전파됩니다.
 *
 * ```kotlin
 * val scope = CoroutineScope(Dispatchers.Default)
 * val uni = scope.asUni { 42L }
 * val result = uni.await().indefinitely()
 * // result == 42L
 * ```
 */
inline fun <T> CoroutineScope.asUni(
    crossinline block: suspend CoroutineScope.() -> T,
): Uni<T> {
    return async {
        block(this@asUni)
    }.asUni()
}
