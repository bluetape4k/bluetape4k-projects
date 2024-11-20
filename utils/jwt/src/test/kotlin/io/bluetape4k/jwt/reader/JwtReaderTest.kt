package io.bluetape4k.jwt.reader

import io.bluetape4k.LibraryName
import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.jwt.AbstractJwtTest
import io.bluetape4k.jwt.provider.JwtProviderFactory
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.junit.jupiter.api.RepeatedTest
import java.time.Instant
import java.util.*

@RandomizedTest
class JwtReaderTest: AbstractJwtTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    private val jwtProvider = JwtProviderFactory.default()


    @RepeatedTest(REPEAT_SIZE)
    fun `read jwt`(@RandomValue claim1: String, @RandomValue claim2: String, @RandomValue claim3: Long) {

        val now = Date()
        val nowSeconds = now.time / 1000L * 1000L

        val jwt = jwtProvider.composer()
            .header("x-author", "debop")
            .claim("claim1", claim1)
            .claim("claim2", claim2)
            .claim("claim3", claim3)
            .issuer(LibraryName)
            .issuedAt(now)
            .notBefore(now)
            .expirationAfterMinutes(60L)
            .compose()

        log.trace { "jwt length=${jwt.length}" }
        log.trace { "jwt=$jwt" }

        val reader = jwtProvider.parse(jwt)
        reader.header<String>("x-author") shouldBeEqualTo "debop"
        reader.claim<String>("claim1") shouldBeEqualTo claim1
        reader.claim<String>("claim2") shouldBeEqualTo claim2
        reader.claim<Long>("claim3") shouldBeEqualTo claim3

        reader.issuer shouldBeEqualTo LibraryName
        reader.issuedAt.time shouldBeEqualTo nowSeconds
        reader.notBefore.time shouldBeEqualTo nowSeconds
        reader.expiration.time shouldBeGreaterThan now.time
    }

    /**
     * java time은 기본적으로 제공하지 않으니, Epoch 을 사용하세요.
     *
     */
    @RepeatedTest(REPEAT_SIZE)
    fun `claim value with java time`() {
        val createdAt = Instant.now().toEpochMilli()
        val jwt = jwtProvider.compose {
            claim("createdAt", createdAt)
        }

        val reader = jwtProvider.parse(jwt)
        reader.claim<Long>("createdAt") shouldBeEqualTo createdAt
    }
}
