package io.bluetape4k.micrometer.instrument

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging

abstract class AbstractMicrometerTest {

    companion object: KLogging() {
        @JvmStatic
        protected val faker = Fakers.faker
    }
}
