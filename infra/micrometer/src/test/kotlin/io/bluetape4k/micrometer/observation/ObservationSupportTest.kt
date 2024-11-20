package io.bluetape4k.micrometer.observation

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.micrometer.observation.Observation
import io.micrometer.observation.tck.ObservationContextAssert
import io.micrometer.observation.tck.ObservationRegistryAssert
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class ObservationSupportTest: AbstractObservationTest() {

    companion object: KLogging()

    @Test
    fun `assert observation registry`() {
        ObservationRegistryAssert.assertThat(observationRegistry)
            .doesNotHaveAnyRemainingCurrentObservation()
    }

    @Test
    fun `create Observation`() {
        val observationName = faker.name().name()
        val observation = Observation.start(observationName, observationRegistry)

        observation.observe {
            log.info { "observation: ${observation.context.name}" }
            observation.context.name shouldBeEqualTo observationName
        }
    }

    @Test
    fun `withObservation example`() {
        val observationName = "withObserver.method"

        val result = withObservation(observationName, observationRegistry) {
            val observation = observationRegistry.currentObservation!!
            log.info { "observation context: ${observation.context}" }

            ObservationContextAssert.assertThat(observation.context)
                .hasNameEqualTo(observationName)

            ObservationRegistryAssert.assertThat(observationRegistry)
                .hasRemainingCurrentObservationSameAs(observation)

            Thread.sleep(100)
            42
        }

        result shouldBeEqualTo 42

        // withObserver 종료 후에는 current observation이 없어야 한다.
        ObservationRegistryAssert.assertThat(observationRegistry)
            .doesNotHaveAnyRemainingCurrentObservation()

        println(observationRegistry)
    }
}
