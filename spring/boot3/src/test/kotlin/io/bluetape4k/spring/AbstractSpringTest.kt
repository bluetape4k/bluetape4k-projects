package io.bluetape4k.spring

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging

abstract class AbstractSpringTest {

    companion object: KLogging() {
        const val REPEAT_SIZE = 5

        @JvmStatic
        val faker = Fakers.faker
    }
}
