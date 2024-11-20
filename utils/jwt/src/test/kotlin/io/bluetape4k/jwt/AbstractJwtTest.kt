package io.bluetape4k.jwt

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.jsonwebtoken.SignatureAlgorithm

abstract class AbstractJwtTest {

    companion object: KLogging() {
        const val PLAIN_TEXT = "Hello, World! 동해물과 백두산이 # debop@kommons.io"

        @JvmStatic
        protected val faker = Fakers.faker

        @JvmStatic
        protected fun randomString(maxSize: Int = 4096): String {
            return Fakers.randomString(maxSize / 2, maxSize)
        }
    }

    protected val rsaAlgorithm: List<SignatureAlgorithm> = SignatureAlgorithm.entries.filter { it.isRsa }

}
