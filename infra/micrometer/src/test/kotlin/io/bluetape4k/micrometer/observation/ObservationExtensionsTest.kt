package io.bluetape4k.micrometer.observation

import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationHandler
import io.micrometer.observation.ObservationRegistry
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class ObservationExtensionsTest {

    private fun registry(handler: ObservationHandler<Observation.Context>): ObservationRegistry =
        ObservationRegistry.create().apply {
            observationConfig().observationHandler(handler)
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

    @Test
    fun `withObservation should start and stop observation`() {
        val handler = RecordingObservationHandler()
        val registry = registry(handler)
        val result = withObservation("record", registry) {
            "ok"
        }

        result shouldBeEqualTo "ok"
        handler.started shouldBeEqualTo 1
        handler.stopped shouldBeEqualTo 1
    }

    @Test
    fun `withObservation should report error and still stop observation`() {
        val handler = RecordingObservationHandler()
        val registry = registry(handler)

        assertFailsWith<IllegalStateException> {
            withObservation("error", registry) {
                throw IllegalStateException("boom")
            }
        }

        handler.errors shouldBeEqualTo 1
        handler.stopped shouldBeEqualTo 1
    }
}
