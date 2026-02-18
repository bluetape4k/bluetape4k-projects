package io.bluetape4k.micrometer.observation

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.micrometer.observation.tck.ObservationRegistryAssert
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class ObservationRegistrySupportTest: AbstractObservationTest() {
    companion object: KLogging()

    @Test
    fun `NoopObservationRegistry - 아묟은 동작도 하지 않음`() {
        NoopObservationRegistry.shouldNotBeNull()
        ObservationRegistryAssert
            .assertThat(NoopObservationRegistry)
            .doesNotHaveAnyRemainingCurrentObservation()
    }

    @Test
    fun `observationRegistryOf - 커스텀 핸들러 등록`() {
        var handlerCalled = false

        val registry =
            observationRegistryOf { ctx ->
                handlerCalled = true
                log.debug { "Handler called with context: $ctx" }
                true
            }

        val observation = registry.start("test")
        observation.observe { }

        handlerCalled shouldBeEqualTo true
    }

    @Test
    fun `simpleObservationRegistryOf - 간단한 핸들러 등록`() {
        var handlerCalled = false

        val registry =
            simpleObservationRegistryOf { ctx ->
                handlerCalled = true
                log.debug { "Handler called: $ctx" }
            }

        val observation = registry.start("test")
        observation.observe { }

        handlerCalled shouldBeEqualTo true
    }

    @Test
    fun `start - 기본 이름으로 Observation 시작`() {
        val observation = observationRegistry.start("test.observation")

        observation.shouldNotBeNull()
        observation.context.name shouldBeEqualTo "test.observation"
    }

    @Test
    fun `start - 컨텍스트 이름 지정`() {
        val observation =
            observationRegistry.start(
                "test.name",
                "Test Contextual Name",
            )

        observation.context.name shouldBeEqualTo "test.name"
    }

    @Test
    fun `createNotStarted - 시작되지 않은 Observation 생성`() {
        val observation = observationRegistry.createNotStarted("test.not.started")

        observation.shouldNotBeNull()
        observation.context.name shouldBeEqualTo "test.not.started"

        // 아직 시작되지 않았으므로 current observation이 없어야 함
        ObservationRegistryAssert
            .assertThat(observationRegistry)
            .doesNotHaveAnyRemainingCurrentObservation()
    }

    @Test
    fun `createNotStarted - 컨텍스트 이름 지정`() {
        val observation =
            observationRegistry.createNotStarted(
                "test.name",
                "Custom Contextual Name",
            )

        observation.context.name shouldBeEqualTo "test.name"
    }
}
