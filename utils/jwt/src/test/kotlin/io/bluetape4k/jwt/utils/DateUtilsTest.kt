package io.bluetape4k.jwt.utils

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test
import java.util.*

class DateUtilsTest {

    companion object: KLogging()

    @Test
    fun `epochSeconds - Date 를 초 단위 epoch 으로 변환한다`() {
        val date = Date(1_700_000_000_000L)
        date.epochSeconds shouldBeEqualTo 1_700_000_000L
    }

    @Test
    fun `epochSeconds - 밀리초 미만 부분은 절삭된다`() {
        val date = Date(1_700_000_000_999L)
        date.epochSeconds shouldBeEqualTo 1_700_000_000L
    }

    @Test
    fun `epochSecondsOrNull - null Date 는 null 을 반환한다`() {
        val date: Date? = null
        date.epochSecondsOrNull.shouldBeNull()
    }

    @Test
    fun `epochSecondsOrNull - non-null Date 는 초 단위 값을 반환한다`() {
        val date: Date? = Date(1_700_000_000_000L)
        date.epochSecondsOrNull shouldBeEqualTo 1_700_000_000L
    }

    @Test
    fun `epochSecondsOrMaxValue - null Date 는 MAX_VALUE 를 반환한다`() {
        val date: Date? = null
        date.epochSecondsOrMaxValue shouldBeEqualTo Long.MAX_VALUE
    }

    @Test
    fun `epochSecondsOrMaxValue - non-null Date 는 초 단위 값을 반환한다`() {
        val date: Date? = Date(1_700_000_000_000L)
        date.epochSecondsOrMaxValue shouldBeEqualTo 1_700_000_000L
    }

    @Test
    fun `dateOfEpochSeconds - epoch 초를 Date 로 변환한다`() {
        val date = dateOfEpochSeconds(1_700_000_000L)
        date.time shouldBeEqualTo 1_700_000_000_000L
    }

    @Test
    fun `millisToSeconds - 밀리초를 초 단위로 변환한다`() {
        3_600_000L.millisToSeconds() shouldBeEqualTo 3_600L
    }

    @Test
    fun `millisToSeconds - 1000 미만의 밀리초는 0 을 반환한다`() {
        999L.millisToSeconds() shouldBeEqualTo 0L
    }

    @Test
    fun `dateOfEpochSeconds - epoch 0 은 1970-01-01 을 반환한다`() {
        val date = dateOfEpochSeconds(0L)
        date.time shouldBeEqualTo 0L
    }
}
