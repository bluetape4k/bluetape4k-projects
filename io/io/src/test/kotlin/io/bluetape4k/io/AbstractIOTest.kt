package io.bluetape4k.io

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging

abstract class AbstractIOTest {

    companion object: KLogging() {
        protected const val REPEAT_SIZE = 3

        @JvmStatic
        protected val faker = Fakers.faker

        @JvmStatic
        protected fun randomString(length: Int = 256): String = Fakers.fixedString(length)

        @JvmStatic
        protected fun randomStrings(size: Int = 20): List<String> = List(size) { randomString() }

        @JvmStatic
        val randomBytes: ByteArray by lazy { faker.random().nextRandomBytes(1024 * 16) }
    }
}
