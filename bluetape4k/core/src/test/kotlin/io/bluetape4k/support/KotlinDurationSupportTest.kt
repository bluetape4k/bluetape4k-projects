package io.bluetape4k.support

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldFailCheck
import io.bluetape4k.assertions.shouldFailRequire
import org.junit.jupiter.api.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

class KotlinDurationSupportTest {

    @Test
    fun `nanosOfMillis는 milliseconds를 제외한 나노초를 반환한다`() {
        (1.seconds + 234_567.nanoseconds).nanosOfMillis shouldBeEqualTo 234_567
    }

    @Test
    fun `sleep은 zero duration에서 즉시 반환한다`() {
        0.milliseconds.sleep()
    }

    @Test
    fun `sleep은 negative duration에서 예외를 던진다`() {
        assertFailsWith<IllegalArgumentException> {
            (-1).milliseconds.sleep()
        }
    }

    @Test
    fun `duration sign require and check helpers are complementary`() {
        val positive = 1.seconds
        val negative = (-1).seconds

        positive.requirePositiveDuration("duration") shouldBeEqualTo positive
        negative.requireZeroOrNegativeDuration("duration") shouldBeEqualTo negative
        Duration.ZERO.requireZeroOrPositiveDuration("duration") shouldBeEqualTo Duration.ZERO
        Duration.ZERO.requireZeroOrNegativeDuration("duration") shouldBeEqualTo Duration.ZERO
        Duration.INFINITE.requirePositiveDuration("duration") shouldBeEqualTo Duration.INFINITE
        (-Duration.INFINITE).requireNegativeDuration("duration") shouldBeEqualTo -Duration.INFINITE

        shouldFailRequire { Duration.ZERO.requirePositiveDuration("duration") }
        shouldFailRequire { positive.requireZeroOrNegativeDuration("duration") }
        shouldFailRequire { negative.requireZeroOrPositiveDuration("duration") }
        shouldFailRequire { negative.requirePositiveDuration("duration") }

        positive.checkPositiveDuration("duration") shouldBeEqualTo positive
        negative.checkZeroOrNegativeDuration("duration") shouldBeEqualTo negative
        Duration.ZERO.checkZeroOrPositiveDuration("duration") shouldBeEqualTo Duration.ZERO
        Duration.ZERO.checkZeroOrNegativeDuration("duration") shouldBeEqualTo Duration.ZERO
        Duration.INFINITE.checkPositiveDuration("duration") shouldBeEqualTo Duration.INFINITE
        (-Duration.INFINITE).checkNegativeDuration("duration") shouldBeEqualTo -Duration.INFINITE

        shouldFailCheck { Duration.ZERO.checkPositiveDuration("duration") }
        shouldFailCheck { positive.checkZeroOrNegativeDuration("duration") }
        shouldFailCheck { negative.checkZeroOrPositiveDuration("duration") }
        shouldFailCheck { negative.checkPositiveDuration("duration") }
    }
}
