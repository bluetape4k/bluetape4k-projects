package io.bluetape4k.micrometer.observation

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace

abstract class AbstractObservationTest {

    companion object: KLogging() {
        @JvmStatic
        protected val faker = Fakers.faker
    }

    protected val observationRegistry = simpleObservationRegistryOf { ctx ->
        log.trace { "Current context: $ctx" }
    }
}
