package io.bluetape4k.jackson.text

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging

abstract class AbstractJacksonTextTest {

    companion object: KLogging() {
        @JvmStatic
        val faker = Fakers.faker
    }

}
