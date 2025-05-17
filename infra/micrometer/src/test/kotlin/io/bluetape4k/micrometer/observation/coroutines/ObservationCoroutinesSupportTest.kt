package io.bluetape4k.micrometer.observation.coroutines

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import io.bluetape4k.micrometer.observation.AbstractObservationTest
import io.micrometer.observation.tck.ObservationRegistryAssert
import kotlinx.coroutines.delay
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds

class ObservationCoroutinesSupportTest: AbstractObservationTest() {

    companion object: KLoggingChannel()

    @Test
    fun `withCoroutineObservation - in coroutines`() = runSuspendIO {
        withObservationContext("observation.coroutines", observationRegistry) {
            log.info { "Start withObservationContext" }

            ObservationRegistryAssert.assertThat(observationRegistry)
                .hasRemainingCurrentObservation()

            log.debug { "Observation=${observationRegistry.currentObservation}" }
            observationRegistry.currentObservation.shouldNotBeNull()

            val observation = currentObservationInContext()
            log.debug { "Current observation in coroutines=$observation" }
            currentObservationInContext().shouldNotBeNull()
        }

        ObservationRegistryAssert.assertThat(observationRegistry)
            .doesNotHaveAnyRemainingCurrentObservation()
    }

    @Test
    fun `복수의 suspend 메소드를 Observation 을 적용하여 실행한다`() = runSuspendIO {

        withObservationContext("observer.delay.1", observationRegistry) {
            ObservationRegistryAssert.assertThat(observationRegistry)
                .hasRemainingCurrentObservation()

            val observation = currentObservationInContext()
            observation?.highCardinalityKeyValue("delay.time", "100ms")
            delay(100.milliseconds)
            log.debug { "observation=$observation" }
        }

        ObservationRegistryAssert.assertThat(observationRegistry)
            .doesNotHaveAnyRemainingCurrentObservation()

        withObservationContext("observer.delay.2", observationRegistry) {
            ObservationRegistryAssert.assertThat(observationRegistry)
                .hasRemainingCurrentObservation()

            val observation = observationRegistry.currentObservation
            observation?.highCardinalityKeyValue("delay.time", "150ms")
            delay(150.milliseconds)
            log.debug { "observation=$observation" }
        }

        ObservationRegistryAssert.assertThat(observationRegistry)
            .doesNotHaveAnyRemainingCurrentObservation()
    }
}
