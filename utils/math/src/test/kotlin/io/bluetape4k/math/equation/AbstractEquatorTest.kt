package io.bluetape4k.math.equation

import io.bluetape4k.collections.doubleSequenceOf
import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import io.bluetape4k.math.commons.approximateEqual
import org.amshove.kluent.shouldBeTrue
import org.apache.commons.math3.exception.NoBracketingException
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.sin
import kotlin.test.assertFailsWith

abstract class AbstractEquatorTest {

    companion object: KLogging()

    protected abstract val equator: Equator

    @Test
    fun `root find for linear`() {
        val values = doubleSequenceOf(-1.0, 1.0, 0.03)
            .map { it to it }
            .toFastList()

        val root = equator.solve(Equator.MAXEVAL, values)
        log.trace { "root=$root" }
        root.approximateEqual(0.0, equator.absoluteAccuracy).shouldBeTrue()
    }

    @Test
    open fun `경계가 같은 + 부호를 가지는 경우`() {
        val values = doubleSequenceOf(-1.0, 1.0, 0.1)
            .map { it to 1.0 }
            .toFastList()

        assertFailsWith<NoBracketingException> {
            val root = equator.solve(Equator.MAXEVAL, values)
            log.trace { "root=$root" }
        }
    }

    @Test
    open fun `경계가 같은 - 부호를 가지는 경우`() {
        val values = doubleSequenceOf(-1.0, 1.0, 0.1)
            .map { it to -1.0 }
            .toFastList()

        assertFailsWith<NoBracketingException> {
            val root = equator.solve(Equator.MAXEVAL, values)
            log.trace { "root=$root" }
        }
    }

    @Test
    fun `sin 함수에서 근 구하기`() {
        val f = { x: Double -> sin(x) }

        val result = equator.solve(Equator.MAXEVAL, 3.0, 4.0, f)
        log.trace { "result=$result" }
        result.approximateEqual(PI, equator.absoluteAccuracy).shouldBeTrue()

        val result2 = equator.solve(Equator.MAXEVAL, 1.0, 4.0, f)
        log.trace { "result2=$result2" }
        result.approximateEqual(PI, equator.absoluteAccuracy).shouldBeTrue()
    }
}
