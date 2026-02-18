package io.bluetape4k.micrometer.observation

import io.bluetape4k.logging.KLogging
import io.micrometer.observation.tck.ObservationContextAssert
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class ObservationExtensionsTest: AbstractObservationTest() {
    companion object: KLogging()

    @Test
    fun `observe - Observation 내에서 코드 블록 실행`() {
        val observation = observationRegistry.start("test.observation")
        val result = observation.observe<String> { "hello" }
        result shouldBeEqualTo "hello"
    }

    @Test
    fun `tryObserve - Observation 내에서 코드 블록 실행`() {
        val observation = observationRegistry.start("test.observation")

        val result = observation.tryObserve { "hello" }

        result.isSuccess.shouldBeTrue()
        result.getOrThrow() shouldBeEqualTo "hello"
    }

    @Test
    fun `withObservation - 컨텍스트 접근 가능`() {
        val result = withObservation("test.context", observationRegistry) {
            "hello"
        }
        result shouldBeEqualTo "hello"
    }

    @Test
    fun `withObservationContext - 컨텍스트 접근 가능`() {
        val observation = observationRegistry.start("test.context")

        observation.withObservationContext { context ->
            context.put("test.key", "test.value")
            ObservationContextAssert
                .assertThat(context)
                .hasNameEqualTo("test.context")
        }
    }

    @Test
    fun `withObservationContext - 예외 발생 시 전파`() {
        val observation = observationRegistry.start("test.error")

        assertFailsWith<IllegalStateException> {
            observation.withObservationContext<String> {
                error("Intentional error")
            }
        }
    }
}
