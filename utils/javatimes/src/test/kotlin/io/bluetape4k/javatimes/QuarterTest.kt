package io.bluetape4k.javatimes

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertFailsWith

class QuarterTest {

    companion object: KLogging()

    @Test
    fun `months in quarter`() {
        (1..4).forEach { q ->
            val startMonth = 1 + (q - 1) * MonthsPerQuarter
            Quarter.of(q).months shouldContainSame intArrayOf(startMonth, startMonth + 1, startMonth + 2)
        }
    }

    @Test
    fun `특정 월이 속한 Quarter 구하기`() {

        (1..12).forEach { monthOfYear ->
            // log.trace { "monthOfYear = $monthOfYear" }
            Quarter.ofMonth(monthOfYear) shouldBeEqualTo Quarter.of((monthOfYear - 1) / 3 + 1)
        }

        assertFailsWith<AssertionError> {
            Quarter.ofMonth(0)
        }

        assertFailsWith<AssertionError> {
            Quarter.ofMonth(-1)
        }

        assertFailsWith<AssertionError> {
            Quarter.ofMonth(13)
        }
    }

    @Test
    fun `특정 Quarter의 시작 월과 마지막 월`() {
        Quarter.VALS.forEach { q ->
            q.startMonth shouldBeEqualTo (q.number - 1) * 3 + 1
            q.endMonth shouldBeEqualTo q.startMonth + 2
        }
    }

    @Test
    fun `Quarter 더하기`() {
        val quarters1 = Quarter.VALS
        val quarters2 = Quarter.VALS

        quarters1.forEach { q1 ->
            quarters2.forEach { q2 ->
                val q3 = q1 + q2
                log.trace { "$q1 + $q2 = $q3" }
                q3.ordinal shouldBeEqualTo (q1.number + q2.number - 1) % QuartersPerYear
            }
        }
    }

    @ParameterizedTest(name = "increment quarter {0}")
    @ValueSource(ints = [0, 1, 2, 3, 4, 100, -1, -5, -11])
    fun `increment quarter`(quarterCount: Int) {
        Quarter.VALS.forEach { q ->
            val newQ = q.increment(quarterCount)
            log.trace { "$q increment by $quarterCount. newQ=$newQ" }
            if (quarterCount == 0) {
                newQ shouldBeEqualTo q
            }
        }
    }

    @Test
    fun `Quarter 빼기`() {
        val quarters1 = Quarter.VALS
        val quarters2 = Quarter.VALS

        quarters1.forEach { q1 ->
            quarters2.forEach { q2 ->
                val q3 = q1 - q2
                log.trace { "$q1 - $q2 = $q3" }
            }
        }
    }

    @ParameterizedTest(name = "decrement quarter {0}")
    @ValueSource(ints = [0, 1, 2, 3, 4, 100, -1, -5, -11])
    fun `decrement quarter`(quarterCount: Int) {
        Quarter.VALS.forEach { q ->
            val newQ = q.decrement(quarterCount)
            log.trace { "$q decrement by $quarterCount. newQ=$newQ" }
            if (quarterCount == 0) {
                newQ shouldBeEqualTo q
            }
        }
    }
}
