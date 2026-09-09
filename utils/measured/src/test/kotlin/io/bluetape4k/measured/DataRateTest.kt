package io.bluetape4k.measured

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNear
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test

@RandomizedTest
class DataRateTest {

    companion object: KLogging()

    @Test
    fun `byte 와 bit 전송률의 SI 변환이 동작한다`() {
        (1.megabytesPerSecond() `in` DataRate.bytesPerSecond).shouldBeNear(1_000_000.0, 1e-5)
        (1.megabitsPerSecond() `in` DataRate.bitsPerSecond).shouldBeNear(1_000_000.0, 1e-5)
        (1.megabytesPerSecond() `in` DataRate.megaBitsPerSecond).shouldBeNear(8.0, 1e-10)
        (1.megabitsPerSecond() `in` DataRate.kiloBytesPerSecond).shouldBeNear(125.0, 1e-10)
    }

    @Test
    fun `IEC byte 전송률은 1024 배율을 유지한다`() {
        (1.kibibytesPerSecond() `in` DataRate.bytesPerSecond).shouldBeNear(1_024.0, 1e-10)
        (1.mebibytesPerSecond() `in` DataRate.kibiBytesPerSecond).shouldBeNear(1_024.0, 1e-10)
        (1.gibibytesPerSecond() `in` DataRate.mebiBytesPerSecond).shouldBeNear(1_024.0, 1e-10)
        (1.mebibytesPerSecond() `in` DataRate.bytesPerSecond).shouldBeNear(1_048_576.0, 1e-4)
    }

    @Test
    fun `전송률과 시간으로 데이터 크기를 계산하고 역연산한다`() {
        val transferred = 10.megabytesPerSecond() * 2.seconds()

        (transferred `in` BinarySize.megaBytes).shouldBeNear(20.0, 1e-10)
        val rate = transferred.toDataRate(2.seconds())
        (rate `in` DataRate.megaBytesPerSecond).shouldBeNear(10.0, 1e-10)
    }

    @Test
    fun `바이트 크기와 시간으로 전송률을 계산한다`() {
        val rate = 1.megabytes10().toDataRate(500.milliseconds())

        (rate `in` DataRate.megaBytesPerSecond).shouldBeNear(2.0, 1e-10)
        (rate `in` DataRate.bytesPerSecond).shouldBeNear(2_000_000.0, 1e-5)
    }

    @Test
    fun `기본 toHuman 은 decimal byte 정책을 사용한다`() {
        1_500_000.bytesPerSecond().toHuman() shouldBeEqualTo "1.5 MB/s"
        1_500_000.bytesPerSecond().toHuman(DataRateFormat.DECIMAL_BYTES) shouldBeEqualTo "1.5 MB/s"
        1_048_576.bytesPerSecond().toHuman() shouldBeEqualTo "1.048576 MB/s"
    }

    @Test
    fun `명시적 표시 정책으로 bit 와 IEC byte를 선택한다`() {
        val rate = 1_048_576.bytesPerSecond()

        rate.toHuman(DataRateFormat.DECIMAL_BITS) shouldBeEqualTo "8.388608 Mbit/s"
        rate.toHuman(DataRateFormat.BINARY_BYTES) shouldBeEqualTo "1.0 MiB/s"
        1_500_000.bytesPerSecond().toHuman(DataRateFormat.BINARY_BYTES) shouldBeEqualTo "1.430511475 MiB/s"
    }

    @Test
    fun `전송률의 1000 과 1024 경계를 구분한다`() {
        1000.bytesPerSecond().toHuman(DataRateFormat.DECIMAL_BYTES) shouldBeEqualTo "1.0 kB/s"
        1024.bytesPerSecond().toHuman(DataRateFormat.BINARY_BYTES) shouldBeEqualTo "1.0 KiB/s"
        1024.bytesPerSecond().toHuman(DataRateFormat.DECIMAL_BYTES) shouldBeEqualTo "1.024 kB/s"
    }

    @Test
    fun `전송률의 음수와 0 표시를 기존 Measure 계약과 같이 처리한다`() {
        0.bytesPerSecond().toHuman() shouldBeEqualTo "0.0 B/s"
        (-1).kilobytesPerSecond().toHuman() shouldBeEqualTo "-1.0 kB/s"
        (Double.NaN * DataRate.bytesPerSecond).toHuman() shouldBeEqualTo "NaN B/s"
        (Double.POSITIVE_INFINITY * DataRate.bytesPerSecond).toHuman() shouldBeEqualTo "Infinity GB/s"
    }
}
