package io.bluetape4k.javers

import org.javers.repository.api.QueryParams
import java.time.LocalDateTime
import kotlin.jvm.optionals.getOrNull

/**
 * 주어진 [date]가 [QueryParams]의 from~to 범위 안에 포함되는지 확인한다.
 *
 * ## 동작/계약
 * - from이 설정되어 있고 date보다 뒤이면 false
 * - to가 설정되어 있고 date보다 앞이면 false
 * - from/to가 미설정이면 해당 경계는 무시한다
 *
 * ```kotlin
 * val inRange = queryParams.isDateInRange(LocalDateTime.now())
 * // inRange == true (범위 내인 경우)
 * ```
 */
fun QueryParams.isDateInRange(date: LocalDateTime): Boolean {
    if (from().getOrNull()?.isAfter(date) == true) {
        return false
    }
    if (to().getOrNull()?.isBefore(date) == true) {
        return false
    }
    return true
}
