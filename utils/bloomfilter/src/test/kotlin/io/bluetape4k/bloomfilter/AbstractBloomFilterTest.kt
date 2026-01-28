package io.bluetape4k.bloomfilter

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging

abstract class AbstractBloomFilterTest {

    companion object: KLogging() {
        @JvmStatic
        protected val faker = Fakers.faker
    }

}
