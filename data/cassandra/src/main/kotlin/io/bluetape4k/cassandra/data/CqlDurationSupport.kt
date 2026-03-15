package io.bluetape4k.cassandra.data

import com.datastax.oss.driver.api.core.data.CqlDuration
import io.bluetape4k.javatimes.nanosLong
import java.time.Duration

/**
 * Java [Duration]을 Cassandra [CqlDuration]으로 변환합니다.
 *
 * ## 동작/계약
 * - 월(month)은 `0`으로 두고, 일과 일 이하 나노초를 분리하여 변환합니다.
 * - 음수 duration도 드라이버가 허용하는 범위 내에서 그대로 전달됩니다.
 * - 변환 결과는 새 [CqlDuration] 인스턴스입니다.
 *
 * ```kotlin
 * val cql = Duration.ofDays(2).toCqlDuration()
 * // cql.days == 2
 * ```
 */
fun Duration.toCqlDuration(): CqlDuration {
    val days = toDays()
    val remainingNanos = nanosLong
    return cqlDurationOf(0, days.toInt(), remainingNanos)
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
fun cqlDurationOf(
    month: Int,
    days: Int,
    nanos: Long = 0L,
): CqlDuration = CqlDuration.newInstance(month, days, nanos)

/**
 * Kotlin [kotlin.time.Duration]을 Cassandra [CqlDuration]으로 변환합니다.
 *
 * ## 동작/계약
 * - 월(month)은 `0`으로 두고, 일과 일 이하 나노초를 분리하여 변환합니다.
 * - 일 수는 `inWholeDays`로, 나머지 나노초는 전체 나노초에서 일 나노초를 뺀 값입니다.
 * - 새 [CqlDuration]을 생성해 반환합니다.
 *
 * ```kotlin
 * val cql = 3.toDuration(kotlin.time.DurationUnit.DAYS).toCqlDuration()
 * // cql.days == 3
 * ```
 */
fun kotlin.time.Duration.toCqlDuration(): CqlDuration {
    val days = inWholeDays
    val nanosPerDay = 24L * 60 * 60 * 1_000_000_000
    val remainingNanos = inWholeNanoseconds - days * nanosPerDay
    return cqlDurationOf(0, days.toInt(), remainingNanos)
}
