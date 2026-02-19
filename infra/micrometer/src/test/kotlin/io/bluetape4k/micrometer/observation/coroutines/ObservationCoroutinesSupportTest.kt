package io.bluetape4k.micrometer.observation.coroutines

import io.bluetape4k.codec.Base58
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import io.bluetape4k.micrometer.observation.AbstractObservationTest
import io.bluetape4k.micrometer.observation.start
import io.micrometer.observation.tck.ObservationRegistryAssert
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds

class ObservationCoroutinesSupportTest: AbstractObservationTest() {

    companion object: KLoggingChannel()

    @Test
    fun `observeSuspending - observe in coroutines`() = runSuspendIO {
        val name = Base58.randomString(8)
        val observation = observationRegistry.start(name)

        val result = observation.observeSuspending { ctx ->
            log.info { "observation ctx: ${ctx.name}" }
            yield()
            ctx.name shouldBeEqualTo name
            ctx.name
        }
        result shouldBeEqualTo name
    }

    @Test
    fun `tryObserveSuspending - observe in coroutines`() = runSuspendIO {
        val name = Base58.randomString(8)
        val observation = observationRegistry.start(name)

        val result = observation.tryObserveSuspending { ctx ->
            log.info { "observation ctx: ${ctx.name}" }
            yield()
            ctx.name shouldBeEqualTo name
            ctx.name
        }
        result.getOrThrow() shouldBeEqualTo name
    }

    @Test
    fun `withCoroutineObservationSuspending - in coroutines`() = runSuspendIO {
        val name = Base58.randomString(8)
        withObservationContextSuspending(name, observationRegistry) {
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
        val name1 = "observer.delay." + Base58.randomString(8)
        withObservationContextSuspending(name1, observationRegistry) {
            ObservationRegistryAssert.assertThat(observationRegistry)
                .hasRemainingCurrentObservation()

            val observation = currentObservationInContext()
            observation?.highCardinalityKeyValue("delay.time", "100ms")
            delay(100.milliseconds)
            log.debug { "observation=$observation" }
        }
        yield()

        ObservationRegistryAssert.assertThat(observationRegistry)
            .doesNotHaveAnyRemainingCurrentObservation()

        val name2 = "observer.delay." + Base58.randomString(8)
        withObservationContextSuspending(name2, observationRegistry) {
            ObservationRegistryAssert.assertThat(observationRegistry)
                .hasRemainingCurrentObservation()

            val observation = currentObservationInContext() // observationRegistry.currentObservation
            observation?.highCardinalityKeyValue("delay.time", "150ms")
            delay(150.milliseconds)
            log.debug { "observation=$observation" }
        }
        yield()

        ObservationRegistryAssert.assertThat(observationRegistry)
            .doesNotHaveAnyRemainingCurrentObservation()
    }

    @Test
    fun `복수의 suspend 메소드를 Observation Context 를 적용하여 실행한다`() = runSuspendIO {
        val name1 = "observer.delay." + Base58.randomString(8)
        val observation1 = observationRegistry.start(name1)

        observation1.withObservationContextSuspending {
            ObservationRegistryAssert.assertThat(observationRegistry)
                .hasRemainingCurrentObservation()

            observation1.highCardinalityKeyValue("delay.time", "100ms")
            delay(100.milliseconds)
            log.debug { "observation1=$observation1" }
        }
        observation1.stop()
        yield()

        ObservationRegistryAssert.assertThat(observationRegistry)
            .doesNotHaveAnyRemainingCurrentObservation()

        val name2 = "observer.delay." + Base58.randomString(8)
        val observation2 = observationRegistry.start(name2)

        observation2.withObservationContextSuspending {
            ObservationRegistryAssert.assertThat(observationRegistry)
                .hasRemainingCurrentObservation()

            observation2.highCardinalityKeyValue("delay.time", "150ms")
            delay(150.milliseconds)
            log.debug { "observation2=$observation2" }
        }
        observation2.stop()
        yield()

        ObservationRegistryAssert.assertThat(observationRegistry)
            .doesNotHaveAnyRemainingCurrentObservation()
    }
}
