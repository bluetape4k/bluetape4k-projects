package io.bluetape4k.measured

import kotlin.math.abs

/**
 * 전송률을 지정한 정책으로 표시합니다.
 *
 * 기본 정책은 [Measure.toHuman]이며, bit와 IEC byte 표시는 호출자가 정책을
 * 명시해야 합니다.
 */
fun Measure<DataRate>.toHuman(format: DataRateFormat): String {
    val candidates = when (format) {
        DataRateFormat.DECIMAL_BYTES -> listOf(
            DataRate.bytesPerSecond,
            DataRate.kiloBytesPerSecond,
            DataRate.megaBytesPerSecond,
            DataRate.gigaBytesPerSecond,
        )

        DataRateFormat.DECIMAL_BITS -> listOf(
            DataRate.bitsPerSecond,
            DataRate.kiloBitsPerSecond,
            DataRate.megaBitsPerSecond,
            DataRate.gigaBitsPerSecond,
        )

        DataRateFormat.BINARY_BYTES -> listOf(
            DataRate.bytesPerSecond,
            DataRate.kibiBytesPerSecond,
            DataRate.mebiBytesPerSecond,
            DataRate.gibiBytesPerSecond,
        )
    }

    val best = candidates.lastOrNull { abs(this `in` it) >= 1.0 } ?: candidates.first()
    return formatHuman(this `in` best, best)
}

/** 전송률과 시간을 곱해 전송된 데이터 크기를 계산합니다. */
@JvmName("dataRateTimesTimeToBinarySize")
operator fun Measure<DataRate>.times(other: Measure<Time>): Measure<BinarySize> =
    ((this `in` DataRate.bytesPerSecond) * (other `in` Time.seconds)) * BinarySize.bytes

/** 시간을 전송률과 곱해 전송된 데이터 크기를 계산합니다. */
@JvmName("timeTimesDataRateToBinarySize")
operator fun Measure<Time>.times(other: Measure<DataRate>): Measure<BinarySize> = other * this

/** 데이터 크기를 시간으로 나눠 전송률을 계산합니다. */
@JvmName("binarySizeDivTimeToDataRate")
operator fun Measure<BinarySize>.div(other: Measure<Time>): Measure<DataRate> =
    ((this `in` BinarySize.bytes) / (other `in` Time.seconds)) * DataRate.bytesPerSecond
