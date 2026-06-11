package io.bluetape4k.micrometer.observation.coroutines

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.codec.Base58
import io.bluetape4k.junit5.coroutines.assertCancellationPropagates
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import io.bluetape4k.micrometer.observation.AbstractObservationTest
import io.bluetape4k.micrometer.observation.start
import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationHandler
import io.micrometer.observation.ObservationRegistry
import io.micrometer.observation.tck.ObservationRegistryAssert
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
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

        yield()

        ObservationRegistryAssert.assertThat(observationRegistry)
            .doesNotHaveAnyRemainingCurrentObservation()
    }

    @Test
    fun `withObservationContextSuspending - named observation stops on success`() = runTest {
        val handler = RecordingObservationHandler()
        val registry =
            ObservationRegistry.create().apply {
                observationConfig().observationHandler(handler)
            }

        val result =
            withObservationContextSuspending("observer.stop.${Base58.randomString(8)}", registry) {
                currentObservationInContext().shouldNotBeNull()
                "observed"
            }

        result shouldBeEqualTo "observed"
        handler.started shouldBeEqualTo 1
        handler.stopped shouldBeEqualTo 1
        handler.errors shouldBeEqualTo 0
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
        delay(10.milliseconds)

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
        delay(10.milliseconds)

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
        delay(10.milliseconds)

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
        delay(10.milliseconds)

        ObservationRegistryAssert.assertThat(observationRegistry)
            .doesNotHaveAnyRemainingCurrentObservation()
    }

    @Test
    fun `withObservationContextSuspending - 시작하지 않은 observation 도 자동으로 시작하고 정리한다`() = runSuspendIO {
        val observation =
            Observation.createNotStarted("observer.not.started.${Base58.randomString(8)}", observationRegistry)

        val result =
            observation.withObservationContextSuspending { context ->
                currentObservationInContext().shouldNotBeNull()
                context.name
            }

        result shouldBeEqualTo observation.context.name
        yield()

        ObservationRegistryAssert.assertThat(observationRegistry)
            .doesNotHaveAnyRemainingCurrentObservation()
    }

    @Test
    fun `withObservationContextSuspending - 예외가 발생해도 current observation 을 정리한다`() = runSuspendIO {
        val name = "observer.error." + Base58.randomString(8)

        assertFailsWith<IllegalStateException> {
            withObservationContextSuspending(name, observationRegistry) {
                currentObservationInContext().shouldNotBeNull()
                throw IllegalStateException("boom")
            }
        }
        yield()

        ObservationRegistryAssert.assertThat(observationRegistry)
            .doesNotHaveAnyRemainingCurrentObservation()
    }

    @Test
    fun `tryObserveSuspending - cancellation propagates to parent job`() = runTest {
        val name = Base58.randomString(8)
        val observation = observationRegistry.start(name)

        assertCancellationPropagates {
            observation.tryObserveSuspending<String> { _ ->
                delay(100.milliseconds)  // arbitrary under runTest virtual time
                "never"
            }
        }

        yield()
        ObservationRegistryAssert.assertThat(observationRegistry)
            .doesNotHaveAnyRemainingCurrentObservation()
    }

    @Test
    fun `tryWithObservationSuspending - cancellation propagates to parent job`() = runTest {
        val name = "observer.cancel." + Base58.randomString(8)

        assertCancellationPropagates {
            tryWithObservationSuspending<String>(name, observationRegistry) {
                delay(100.milliseconds)  // arbitrary under runTest virtual time
                "never"
            }
        }

        yield()
        ObservationRegistryAssert.assertThat(observationRegistry)
            .doesNotHaveAnyRemainingCurrentObservation()
    }

    private class RecordingObservationHandler: ObservationHandler<Observation.Context> {
        var started = 0
        var stopped = 0
        var errors = 0

        override fun onStart(context: Observation.Context) {
            started++
        }

        override fun onStop(context: Observation.Context) {
            stopped++
        }

        override fun onError(context: Observation.Context) {
            errors++
        }

        override fun supportsContext(context: Observation.Context): Boolean = true
    }
}
