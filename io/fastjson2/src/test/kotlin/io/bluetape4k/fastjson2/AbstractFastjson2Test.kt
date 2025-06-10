package io.bluetape4k.fastjson2

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging

abstract class AbstractFastjson2Test {

    companion object: KLogging() {
        const val REPEAT_SIZE = 5

        @JvmStatic
        val faker = Fakers.faker
    }
}
