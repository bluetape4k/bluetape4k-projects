package io.bluetape4k

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging

abstract class AbstractCoreTest {

    companion object: KLogging() {

        @JvmStatic
        val faker = Fakers.faker

    }
}
