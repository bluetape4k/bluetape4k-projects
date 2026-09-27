package io.bluetape4k.redis.redisson

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging

abstract class AbstractRedissonTest {

    companion object: KLogging() {

        @JvmStatic
        val faker = Fakers.faker
    }
}
