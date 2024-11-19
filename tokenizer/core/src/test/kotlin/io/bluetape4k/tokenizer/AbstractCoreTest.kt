package io.bluetape4k.tokenizer

import com.fasterxml.jackson.databind.json.JsonMapper
import io.bluetape4k.jackson.Jackson
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging

abstract class AbstractCoreTest {

    companion object: KLogging() {
        const val REPEAT_SIZE = 5

        @JvmStatic
        protected val faker = Fakers.faker

        @JvmStatic
        protected val mapper: JsonMapper by lazy { Jackson.defaultJsonMapper }
    }
}
