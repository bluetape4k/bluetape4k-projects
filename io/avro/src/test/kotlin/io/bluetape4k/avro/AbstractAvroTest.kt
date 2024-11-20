package io.bluetape4k.avro

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging

abstract class AbstractAvroTest {

    companion object: KLogging() {
        @JvmStatic
        val faker = Fakers.faker
    }

}
