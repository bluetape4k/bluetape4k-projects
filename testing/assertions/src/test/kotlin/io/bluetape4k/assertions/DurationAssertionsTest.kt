package io.bluetape4k.assertions

import java.time.Duration
import kotlin.time.Duration.Companion.seconds
import org.junit.jupiter.api.Test
import org.opentest4j.AssertionFailedError

class DurationAssertionsTest {

    @Test
    fun `java duration signs have symmetric conditions`() {
        Duration.ofSeconds(1).shouldBePositive()
        Duration.ofSeconds(-1).shouldNotBePositive()
        Duration.ZERO.shouldNotBePositive()
        Duration.ZERO.shouldBeZeroOrPositive()
        Duration.ofSeconds(-1).shouldBeNegative()
        Duration.ofSeconds(1).shouldBeZeroOrPositive()

        assertFailsWith<AssertionFailedError> { Duration.ZERO.shouldBePositive() }
        assertFailsWith<AssertionFailedError> { Duration.ZERO.shouldBeNegative() }
        assertFailsWith<AssertionFailedError> { Duration.ofSeconds(1).shouldNotBePositive() }
        assertFailsWith<AssertionFailedError> { Duration.ofSeconds(-1).shouldBeZeroOrPositive() }
    }

    @Test
    fun `kotlin duration signs include infinities as sign-only values`() {
        1.seconds.shouldBePositive()
        (-1).seconds.shouldNotBePositive()
        1.seconds.shouldBeZeroOrPositive()
        kotlin.time.Duration.ZERO.shouldBeZeroOrPositive()
        (-1).seconds.shouldBeNegative()
        kotlin.time.Duration.INFINITE.shouldBePositive()
        kotlin.time.Duration.INFINITE.shouldBeZeroOrPositive()
        (-kotlin.time.Duration.INFINITE).shouldBeNegative()
        (-kotlin.time.Duration.INFINITE).shouldNotBePositive()

        assertFailsWith<AssertionFailedError> { kotlin.time.Duration.ZERO.shouldBePositive() }
        assertFailsWith<AssertionFailedError> { kotlin.time.Duration.ZERO.shouldBeNegative() }
        assertFailsWith<AssertionFailedError> { kotlin.time.Duration.INFINITE.shouldBeNegative() }
        assertFailsWith<AssertionFailedError> { (-kotlin.time.Duration.INFINITE).shouldBeZeroOrPositive() }
    }
}
