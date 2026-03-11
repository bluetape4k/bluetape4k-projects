package io.bluetape4k.redis.lettuce

import io.bluetape4k.LibraryName
import io.bluetape4k.codec.Base58
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.lettuce.core.ExperimentalLettuceCoroutinesApi

@OptIn(ExperimentalLettuceCoroutinesApi::class)
abstract class AbstractLettuceTest {

    companion object: KLogging() {

        @JvmStatic
        val faker = Fakers.faker

        @JvmStatic
        protected fun randomName(): String =
            "${LibraryName}:${Base58.randomString(8)}"

        @JvmStatic
        protected fun randomString(size: Int = 2048): String =
            Fakers.fixedString(size)

    }

    protected val client = LettuceTestUtils.client

}
