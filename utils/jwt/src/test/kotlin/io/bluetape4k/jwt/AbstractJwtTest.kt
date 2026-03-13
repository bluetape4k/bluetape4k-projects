package io.bluetape4k.jwt

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.SignatureAlgorithm

abstract class AbstractJwtTest {

    companion object: KLogging() {

        const val REPEAT_SIZE = 3

        const val PLAIN_TEXT = "Hello, World! 동해물과 백두산이 # debop@bluetape4k.io"

        @JvmStatic
        protected val faker = Fakers.faker

        @JvmStatic
        protected fun randomString(maxSize: Int = 4096): String {
            return Fakers.randomString(maxSize / 2, maxSize)
        }
    }

    protected val rsaAlgorithm: List<SignatureAlgorithm> = listOf(
        Jwts.SIG.RS256,
        Jwts.SIG.RS384,
        Jwts.SIG.RS512,
        Jwts.SIG.PS256,
        Jwts.SIG.PS384,
        Jwts.SIG.PS512,
    )

}
