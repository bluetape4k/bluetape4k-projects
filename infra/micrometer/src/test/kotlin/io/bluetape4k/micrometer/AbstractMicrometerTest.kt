package io.bluetape4k.micrometer

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging

@Suppress("UtilityClassWithPublicConstructor")
abstract class AbstractMicrometerTest {

    companion object: KLogging() {
        @JvmStatic
        protected val faker = Fakers.faker
    }
}
