package io.bluetape4k.assertions

import org.junit.jupiter.api.Test
import org.opentest4j.AssertionFailedError
import kotlin.time.Duration.Companion.seconds

class KotlinDurataionsTest {

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
