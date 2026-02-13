package io.bluetape4k.support

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith
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
}

