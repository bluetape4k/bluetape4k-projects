package io.bluetape4k.assertions

import java.time.Duration
import org.junit.jupiter.api.Test
import org.opentest4j.AssertionFailedError

class JavaDurationsTest {

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

}
