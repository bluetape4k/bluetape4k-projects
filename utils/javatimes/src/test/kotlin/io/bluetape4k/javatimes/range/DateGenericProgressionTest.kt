package io.bluetape4k.javatimes.range

import io.bluetape4k.javatimes.dateOf
import io.bluetape4k.javatimes.days
import io.bluetape4k.javatimes.hours
import io.bluetape4k.javatimes.minus
import io.bluetape4k.javatimes.plus
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertFailsWith

class DateGenericProgressionTest {

    companion object: KLogging()

    private val today = dateOf()
    private val tomorrow = today + 1.days()
    private val yesterday = today - 1.days()

    @Test
    fun `create DateProgression instance`() {
        val start = dateOf()
        val endInclusive = start + 1.days()

        val progression = dateProgressionOf(start, endInclusive, 1.hours())

        progression.first shouldBeEqualTo start
        progression.last shouldBeEqualTo endInclusive
        progression.step shouldBeEqualTo 1.hours()

        progression.count() shouldBeEqualTo 25
    }

    @Test
    fun `create DateProgression with negated step`() {
        val start = dateOf()
        val endInclusive = start - 1.days()

        val progression = dateProgressionOf(start, endInclusive, 1.hours().negated())

        progression.first shouldBeEqualTo start
        progression.last shouldBeEqualTo endInclusive
        progression.step shouldBeEqualTo 1.hours().negated()

        progression.count() shouldBeEqualTo 25
    }

    @Test
    fun `STEP은 0이면 안됩니다`() {
        assertFailsWith<AssertionError> {
            dateProgressionOf(today, tomorrow, Duration.ZERO)
        }
    }

    @Test
    fun `start less than end 일 경우 step은 positive 여야 합니다`() {
        assertFailsWith<AssertionError> {
            dateProgressionOf(today, tomorrow, 1.hours().negated())
        }
    }

    @Test
    fun `start greater than end 일 경우 step은 negative 여야 합니다`() {
        assertFailsWith<AssertionError> {
            dateProgressionOf(today, yesterday, 1.hours())
        }
    }

    @Test
    fun `step이 start ~ endInclusive 보다 큰 경우에는 start만 가진다`() {

        val progression = dateProgressionOf(today, tomorrow, 7.days())

        progression.first shouldBeEqualTo today
        progression.last shouldBeEqualTo today
        progression.step shouldBeEqualTo 7.days()

        progression.forEachIndexed { index, date ->
            log.debug { "progression[$index]=$date" }
        }

        progression.count() shouldBeEqualTo 1
    }

    @Test
    fun `progression isEmpty`() {
        dateProgressionOf(today, yesterday, 1.hours().negated()).isEmpty().shouldBeFalse()
        dateProgressionOf(today, tomorrow, 1.hours()).isEmpty().shouldBeFalse()

        // 시작과 끝이 같다면 하나의 값을 가진다
        dateProgressionOf(today, today).isEmpty().shouldBeFalse()
    }
}
