package io.bluetape4k.jwt.utils

import java.util.*

/**
 * [Date]를 Unix epoch 초(seconds) 단위로 반환합니다.
 *
 * ```kotlin
 * val date = Date(1_700_000_000_000L)
 * val seconds = date.epochSeconds
 * // seconds == 1_700_000_000L
 * ```
 */
val Date.epochSeconds: Long
    get() = this.time / 1000L

/**
 * nullable [Date]를 epoch 초 단위로 반환합니다. `null`이면 `null`을 반환합니다.
 *
 * ```kotlin
 * val date: Date? = Date(1_700_000_000_000L)
 * val seconds = date.epochSecondsOrNull
 * // seconds == 1_700_000_000L
 *
 * val nullDate: Date? = null
 * val nullSeconds = nullDate.epochSecondsOrNull
 * // nullSeconds == null
 * ```
 */
val Date?.epochSecondsOrNull: Long?
    get() = this?.let { epochSeconds }

/**
 * nullable [Date]를 epoch 초 단위로 반환합니다. `null`이면 [Long.MAX_VALUE]를 반환합니다.
 *
 * ```kotlin
 * val date: Date? = Date(1_700_000_000_000L)
 * val seconds = date.epochSecondsOrMaxValue
 * // seconds == 1_700_000_000L
 *
 * val nullDate: Date? = null
 * val maxValue = nullDate.epochSecondsOrMaxValue
 * // maxValue == Long.MAX_VALUE
 * ```
 */
val Date?.epochSecondsOrMaxValue: Long
    get() = this?.let { epochSeconds } ?: Long.MAX_VALUE

/**
 * Unix epoch 초 단위 값을 [Date]로 변환합니다.
 *
 * ```kotlin
 * val date = dateOfEpochSeconds(1_700_000_000L)
 * // date.time == 1_700_000_000_000L
 * ```
 *
 * @param epochSeconds epoch 초 단위 시각
 * @return 해당 시각의 [Date] 인스턴스
 */
fun dateOfEpochSeconds(epochSeconds: Long): Date =
    Date(epochSeconds * 1000L)

/**
 * 밀리초 단위 시간을 초 단위로 변환합니다.
 *
 * ```kotlin
 * val millis = 3_600_000L
 * val seconds = millis.millisToSeconds()
 * // seconds == 3_600L
 * ```
 *
 * @return 초 단위 시간
 */
fun Long.millisToSeconds(): Long = this / 1000L
