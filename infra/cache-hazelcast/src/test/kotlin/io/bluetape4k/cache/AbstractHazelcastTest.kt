package io.bluetape4k.cache

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging

abstract class AbstractHazelcastTest {

    companion object: KLogging() {

        val faker = Fakers.faker

        @JvmStatic
        protected val hazelcastClient by lazy { HazelcastServers.hazelcastClient }

    }

}
