package io.bluetape4k.support

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldFailRequire
import org.junit.jupiter.api.Test
import java.time.Duration as JavaDuration
import kotlin.time.Duration.Companion.seconds

class RequireDurationSupportTest {

    @Test
    fun `Java Duration requireNegative는 음수만 허용한다`() {
        val negative = JavaDuration.ofSeconds(-1)
        val zero = JavaDuration.ZERO
        val positive = JavaDuration.ofSeconds(1)

        negative.requireNegative("duration") shouldBeEqualTo negative
        negative.requireNegative { "duration=$negative" } shouldBeEqualTo negative

        shouldFailRequire { zero.requireNegative("duration") }
        shouldFailRequire { zero.requireNegative { "duration=$zero" } }
        shouldFailRequire { positive.requireNegative("duration") }
        shouldFailRequire { positive.requireNegative { "duration=$positive" } }
    }

    @Test
    fun `Kotlin Duration requireNegative는 음수만 허용한다`() {
        val negative = (-1).seconds
        val zero = 0.seconds
        val positive = 1.seconds

        negative.requireNegative("duration") shouldBeEqualTo negative
        negative.requireNegative { "duration=$negative" } shouldBeEqualTo negative

        shouldFailRequire { zero.requireNegative("duration") }
        shouldFailRequire { zero.requireNegative { "duration=$zero" } }
        shouldFailRequire { positive.requireNegative("duration") }
        shouldFailRequire { positive.requireNegative { "duration=$positive" } }
    }
}
