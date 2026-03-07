package io.bluetape4k.jwt.utils

import java.util.*

/** [Date]를 Unix epoch 초(seconds) 단위로 반환합니다. */
val Date.epochSeconds: Long
    get() = this.time / 1000L

/** nullable [Date]를 epoch 초 단위로 반환합니다. `null`이면 `null`을 반환합니다. */
val Date?.epochSecondsOrNull: Long?
    get() = this?.let { epochSeconds }

/** nullable [Date]를 epoch 초 단위로 반환합니다. `null`이면 [Long.MAX_VALUE]를 반환합니다. */
val Date?.epochSecondsOrMaxValue: Long
    get() = this?.let { epochSeconds } ?: Long.MAX_VALUE

/**
 * Unix epoch 초 단위 값을 [Date]로 변환합니다.
 *
 * @param epochSeconds epoch 초 단위 시각
 * @return 해당 시각의 [Date] 인스턴스
 */
fun dateOfEpochSeconds(epochSeconds: Long): Date =
    Date(epochSeconds * 1000L)

/**
 * 밀리초 단위 시간을 초 단위로 변환합니다.
 *
 * @return 초 단위 시간
 */
fun Long.millisToSeconds(): Long = this / 1000L
