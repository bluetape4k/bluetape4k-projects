package io.bluetape4k.cassandra.data

import com.datastax.oss.driver.api.core.data.CqlDuration
import java.time.Duration

/**
 * Java [Duration]을 Cassandra [CqlDuration]으로 변환합니다.
 *
 * ## 동작/계약
 * - 월(month)은 `0`으로 두고, 일/나노초를 각각 `toDays()`/`toNanos()`로 변환합니다.
 * - 음수 duration도 드라이버가 허용하는 범위 내에서 그대로 전달됩니다.
 * - 변환 결과는 새 [CqlDuration] 인스턴스입니다.
 *
 * ```kotlin
 * val cql = Duration.ofDays(2).toCqlDuration()
 * // cql.days == 2
 * ```
 */
fun Duration.toCqlDuration(): CqlDuration {
    return cqlDurationOf(0, toDays().toInt(), toNanos())
}

/**
 * month/day/nanos로 [CqlDuration]을 직접 생성합니다.
 *
 * ## 동작/계약
 * - `CqlDuration.newInstance(month, days, nanos)`를 그대로 호출합니다.
 * - 값 범위가 드라이버 제약을 벗어나면 드라이버 예외가 발생할 수 있습니다.
 * - 입력값 검증/정규화는 별도로 수행하지 않습니다.
 *
 * ```kotlin
 * val cql = cqlDurationOf(month = 1, days = 2, nanos = 3)
 * // cql.months == 1
 * ```
 */
fun cqlDurationOf(month: Int, days: Int, nanos: Long = 0L): CqlDuration {
    return CqlDuration.newInstance(month, days, nanos)
}

/**
 * Kotlin [kotlin.time.Duration]을 Cassandra [CqlDuration]으로 변환합니다.
 *
 * ## 동작/계약
 * - 월(month)은 `0`, 일/나노초는 `inWholeDays`/`inWholeNanoseconds`를 사용합니다.
 * - 단위 변환에서 소수 부분은 각 `inWhole*` 규칙대로 버려집니다.
 * - 새 [CqlDuration]을 생성해 반환합니다.
 *
 * ```kotlin
 * val cql = 3.toDuration(kotlin.time.DurationUnit.DAYS).toCqlDuration()
 * // cql.days == 3
 * ```
 */
fun kotlin.time.Duration.toCqlDuration(): CqlDuration =
    cqlDurationOf(0, inWholeDays.toInt(), inWholeNanoseconds)
