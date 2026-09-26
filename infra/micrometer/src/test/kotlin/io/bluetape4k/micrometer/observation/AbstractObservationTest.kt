package io.bluetape4k.micrometer.observation

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.micrometer.AbstractMicrometerTest
import io.micrometer.observation.ObservationRegistry

abstract class AbstractObservationTest: AbstractMicrometerTest() {

    companion object: KLogging()

    protected val observationRegistry: ObservationRegistry = simpleObservationRegistryOf { ctx ->
        log.debug { "Current observation context: $ctx" }
    }
}
