package io.bluetape4k.coroutines.support

import kotlinx.coroutines.CoroutineScope

/**
 * 현재 `CoroutineScope`의 `coroutineContext.hashCode()`를 `Long`으로 노출합니다.
 *
 * ## 동작/계약
 * - 값은 `coroutineContext` 해시 기반이므로 전역 고유 식별자가 아닙니다.
 * - 동일 컨텍스트에서는 같은 값을 반환할 수 있지만, 실행/환경에 따라 달라질 수 있습니다.
 * - 계산은 `hashCode()` 호출 1회로 완료되며 별도 상태를 변경하지 않습니다.
 *
 * ```kotlin
 * val id = coroutineScope.currentCoroutineId
 * // id == coroutineScope.coroutineContext.hashCode().toLong()
 * ```
 */
@Deprecated(message = "제대로 된 Id 값이라 볼 수 없다. SnowflakeId 같은 것을 사용하세요")
val CoroutineScope.currentCoroutineId: Long
    get() = this.coroutineContext.hashCode().toLong()
