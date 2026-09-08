package io.bluetape4k.measured

/** 전송률을 사람이 읽을 수 있는 단위로 표시하는 정책입니다. */
enum class DataRateFormat {
    /** 10진 바이트 단위(B/s, kB/s, MB/s, GB/s)를 사용합니다. */
    DECIMAL_BYTES,

    /** 10진 비트 단위(bit/s, kbit/s, Mbit/s, Gbit/s)를 사용합니다. */
    DECIMAL_BITS,

    /** 2진 바이트 단위(B/s, KiB/s, MiB/s, GiB/s)를 사용합니다. */
    BINARY_BYTES,
}

/**
 * 데이터 전송률 단위를 나타냅니다.
 *
 * 기준 단위는 초당 바이트([bytesPerSecond])입니다. 비트 단위는 1 byte = 8 bit
 * 관계를 사용하고, 바이트 접두어는 SI와 IEC 배율을 구분합니다.
 */
open class DataRate(
    suffix: String,
    ratio: Double = 1.0,
): Units(suffix, ratio) {
    companion object {
        @JvmField
        val bytesPerSecond: DataRate = DataRate("B/s")

        @JvmField
        val kiloBytesPerSecond: DataRate = DataRate("kB/s", 1_000.0)

        @JvmField
        val megaBytesPerSecond: DataRate = DataRate("MB/s", 1_000_000.0)

        @JvmField
        val gigaBytesPerSecond: DataRate = DataRate("GB/s", 1_000_000_000.0)

        @JvmField
        val bitsPerSecond: DataRate = DataRate("bit/s", 1.0 / 8.0)

        @JvmField
        val kiloBitsPerSecond: DataRate = DataRate("kbit/s", 1_000.0 / 8.0)

        @JvmField
        val megaBitsPerSecond: DataRate = DataRate("Mbit/s", 1_000_000.0 / 8.0)

        @JvmField
        val gigaBitsPerSecond: DataRate = DataRate("Gbit/s", 1_000_000_000.0 / 8.0)

        @JvmField
        val kibiBytesPerSecond: DataRate = DataRate("KiB/s", 1_024.0)

        @JvmField
        val mebiBytesPerSecond: DataRate = DataRate("MiB/s", 1_048_576.0)

        @JvmField
        val gibiBytesPerSecond: DataRate = DataRate("GiB/s", 1_073_741_824.0)
    }
}

/** 숫자를 초당 바이트 단위 전송률로 변환합니다. */
fun Number.bytesPerSecond(): Measure<DataRate> = this * DataRate.bytesPerSecond

/** 숫자를 초당 킬로바이트 단위 전송률로 변환합니다. */
fun Number.kilobytesPerSecond(): Measure<DataRate> = this * DataRate.kiloBytesPerSecond

/** 숫자를 초당 메가바이트 단위 전송률로 변환합니다. */
fun Number.megabytesPerSecond(): Measure<DataRate> = this * DataRate.megaBytesPerSecond

/** 숫자를 초당 기가바이트 단위 전송률로 변환합니다. */
fun Number.gigabytesPerSecond(): Measure<DataRate> = this * DataRate.gigaBytesPerSecond

/** 숫자를 초당 비트 단위 전송률로 변환합니다. */
fun Number.bitsPerSecond(): Measure<DataRate> = this * DataRate.bitsPerSecond

/** 숫자를 초당 킬로비트 단위 전송률로 변환합니다. */
fun Number.kilobitsPerSecond(): Measure<DataRate> = this * DataRate.kiloBitsPerSecond

/** 숫자를 초당 메가비트 단위 전송률로 변환합니다. */
fun Number.megabitsPerSecond(): Measure<DataRate> = this * DataRate.megaBitsPerSecond

/** 숫자를 초당 기가비트 단위 전송률로 변환합니다. */
fun Number.gigabitsPerSecond(): Measure<DataRate> = this * DataRate.gigaBitsPerSecond

/** 숫자를 초당 키비바이트 단위 전송률로 변환합니다. */
fun Number.kibibytesPerSecond(): Measure<DataRate> = this * DataRate.kibiBytesPerSecond

/** 숫자를 초당 메비바이트 단위 전송률로 변환합니다. */
fun Number.mebibytesPerSecond(): Measure<DataRate> = this * DataRate.mebiBytesPerSecond

/** 숫자를 초당 기비바이트 단위 전송률로 변환합니다. */
fun Number.gibibytesPerSecond(): Measure<DataRate> = this * DataRate.gibiBytesPerSecond
