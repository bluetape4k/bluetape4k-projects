package io.bluetape4k.spring.observability

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.micrometer.common.KeyValue
import io.micrometer.common.KeyValues
import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationHandler
import io.micrometer.observation.ObservationRegistry
import io.micrometer.observation.tck.ObservationRegistryAssert
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Test

class SpringObservationSupportTest {

    private fun registry(handler: RecordingObservationHandler): ObservationRegistry =
        ObservationRegistry.create().apply {
            observationConfig().observationHandler(handler)
        }

    private class RecordingObservationHandler: ObservationHandler<Observation.Context> {
        var started = 0
        var stopped = 0
        var errors = 0
        var lastContext: Observation.Context? = null

        override fun onStart(context: Observation.Context) {
            started++
            lastContext = context
        }

        override fun onStop(context: Observation.Context) {
            stopped++
            lastContext = context
        }

        override fun onError(context: Observation.Context) {
            errors++
            lastContext = context
        }

        override fun supportsContext(context: Observation.Context): Boolean = true
    }

    @Test
    fun `observeSpring starts stops and applies key values`() {
        val handler = RecordingObservationHandler()
        val registry = registry(handler)

        val result =
            registry.observeSpring(
                name = "spring.service.load",
                keyValues = SpringObservationKeyValues(
                    lowCardinality = KeyValues.of(KeyValue.of("component", "order-service")),
                    highCardinality = KeyValues.of(KeyValue.of("correlation.id", "safe-id")),
                ),
            ) { context ->
                context.addLowCardinalityKeyValue(KeyValue.of("outcome", "success"))
                "ok"
            }

        result shouldBeEqualTo "ok"
        handler.started shouldBeEqualTo 1
        handler.stopped shouldBeEqualTo 1
        handler.errors shouldBeEqualTo 0
        handler.lastContext?.getLowCardinalityKeyValue("component")?.value shouldBeEqualTo "order-service"
        handler.lastContext?.getLowCardinalityKeyValue("outcome")?.value shouldBeEqualTo "success"
        handler.lastContext?.getHighCardinalityKeyValue("correlation.id")?.value shouldBeEqualTo "safe-id"
    }

    @Test
    fun `observeSpring records exception and stops observation`() {
        val handler = RecordingObservationHandler()
        val registry = registry(handler)

        assertFailsWith<IllegalStateException> {
            registry.observeSpring("spring.service.error") {
                throw IllegalStateException("boom")
            }
        }

        handler.started shouldBeEqualTo 1
        handler.errors shouldBeEqualTo 1
        handler.stopped shouldBeEqualTo 1
    }

    @Test
    fun `observeSpring rethrows cancellation without recording error`() {
        val handler = RecordingObservationHandler()
        val registry = registry(handler)

        assertFailsWith<CancellationException> {
            registry.observeSpring("spring.service.cancel") {
                throw CancellationException("cancel")
            }
        }

        handler.started shouldBeEqualTo 1
        handler.errors shouldBeEqualTo 0
        handler.stopped shouldBeEqualTo 1
    }

    @Test
    fun `observeSpringSuspending binds and cleans current observation`() = runTest {
        val handler = RecordingObservationHandler()
        val registry = registry(handler)

        val result =
            registry.observeSpringSuspending("spring.handler.suspend") { context ->
                registry.currentObservation?.context?.name shouldBeEqualTo "spring.handler.suspend"
                context.name
            }

        result shouldBeEqualTo "spring.handler.suspend"
        yield()

        handler.started shouldBeEqualTo 1
        handler.stopped shouldBeEqualTo 1
        handler.errors shouldBeEqualTo 0
        ObservationRegistryAssert.assertThat(registry)
            .doesNotHaveAnyRemainingCurrentObservation()
    }

    @Test
    fun `observeSpringSuspending rethrows cancellation without recording error`() = runTest {
        val handler = RecordingObservationHandler()
        val registry = registry(handler)
        val cancellation = CancellationException("cancel")

        val thrown =
            assertFailsWith<CancellationException> {
                registry.observeSpringSuspending("spring.handler.cancel") {
                    throw cancellation
                }
            }

        thrown.message shouldBeEqualTo "cancel"
        handler.started shouldBeEqualTo 1
        handler.errors shouldBeEqualTo 0
        handler.stopped shouldBeEqualTo 1
        registry.currentObservation.shouldBeNull()
    }
}
