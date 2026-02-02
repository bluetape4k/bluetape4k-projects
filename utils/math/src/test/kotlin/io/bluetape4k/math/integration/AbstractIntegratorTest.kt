package io.bluetape4k.math.integration

import io.bluetape4k.collections.doubleSequenceOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import io.bluetape4k.math.commons.approximateEqual
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

abstract class AbstractIntegratorTest {

    companion object: KLogging() {
        private const val EPSILON = 1e-5
    }

    protected abstract val integrator: Integrator

    @Test
    fun `integration with zero values`() {
        val zeroFunc = { _: Double -> 0.0 }
        val fx = integrator.integrate(0.0, 1.0, zeroFunc)
        log.trace { "result: $fx" }
        fx.approximateEqual(0.0, integrator.relativeAccuracy).shouldBeTrue()
    }

    @Test
    fun `integration with uniform value`() {
        val uniform = { _: Double -> 1.0 }
        val fx = integrator.integrate(0.0, 1.0, uniform)
        log.trace { "result: $fx" }
        fx.approximateEqual(1.0, integrator.relativeAccuracy).shouldBeTrue()
    }

    @Test
    fun `integration with identity function`() {
        val identityFunc = { x: Double -> x }
        val fx = integrator.integrate(0.0, 1.0, identityFunc)
        log.trace { "result: $fx" }
        fx.approximateEqual(0.5, integrator.relativeAccuracy).shouldBeTrue()
    }

    @Test
    fun `integration with reverse identity function`() {
        val identityFunc = { x: Double -> 1.0 - x }
        val fx = integrator.integrate(0.0, 1.0, identityFunc)
        log.trace { "result: $fx" }
        fx.approximateEqual(0.5, integrator.relativeAccuracy).shouldBeTrue()
    }

    @Test
    fun `integration with flip values`() {
        val identityFunc = { x: Double -> 1.0 - x }
        val fx = integrator.integrate(0.0, 2.0, identityFunc)
        log.trace { "result: $fx" }
        fx.approximateEqual(0.0, integrator.relativeAccuracy).shouldBeTrue()
    }

    @Test
    fun `integeration with double array`() {
        val identities = doubleSequenceOf(0.0, 1.0, 1e-4)
            .map { it to 1.0 }
            .asIterable()

        val fx = integrator.integrate(identities)
        log.trace { "result: $fx" }
        fx.approximateEqual(1.0, integrator.relativeAccuracy).shouldBeTrue()
    }

    @Test
    fun `integeration with double array with flip values`() {
        val identities = doubleSequenceOf(0.0, 1.0, 1e-4)
            .map { it to (0.5 - it) }
            .asIterable()

        val fx = integrator.integrate(identities)
        log.trace { "result: $fx" }
        fx.approximateEqual(0.0, integrator.relativeAccuracy).shouldBeTrue()
    }

    @Test
    fun `integeration with double array with interpolation`() {
        val identities = doubleSequenceOf(0.0, 1.0, 1e-4)
            .map { it to it }
            .asIterable()

        val fx = integrator.integrate(identities)
        log.trace { "result: $fx" }
        fx.approximateEqual(0.5, integrator.relativeAccuracy).shouldBeTrue()
    }

    @Test
    fun `integration with empty collection`() {
        val vars = emptyList<Pair<Double, Double>>()

        assertFailsWith<AssertionError> {
            integrator.integrate(vars)
        }
    }

    @Test
    fun `integration with different size`() {
        val xs = DoubleArray(10) { it.toDouble() }
        val ys = DoubleArray(42) { it.toDouble() }

        assertFailsWith<AssertionError> {
            integrator.integrate(xs, ys)
        }
    }
}
