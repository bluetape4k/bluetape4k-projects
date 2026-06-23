package io.bluetape4k.jwt.reader

import io.bluetape4k.jwt.AbstractJwtTest
import io.bluetape4k.jwt.provider.JwtProviderFactory
import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeLessThan
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import io.bluetape4k.assertions.assertFailsWith
import java.util.*

class JwtReaderExpirationTest: AbstractJwtTest() {

    companion object: KLogging()

    private val provider = JwtProviderFactory.fixed(kid = "expiration-test")

    @Test
    fun `isExpired - 만료 시각이 미래인 토큰은 만료되지 않았다`() {
        val jwt = provider.compose {
            subject = "alice"
            expirationAfterMinutes = 60
        }

        val reader = provider.parse(jwt)
        reader.isExpired.shouldBeFalse()
        reader.expiredTtl shouldBeGreaterThan 0L
        reader.expiredTtl shouldBeLessThan 3_600_001L
    }

    @Test
    fun `isExpired - 만료 시각이 과거인 토큰은 만료된 상태이다`() {
        val pastExpiration = Date(System.currentTimeMillis() - 10_000L)

        val jwt = provider.composer().apply {
            subject("bob")
            expiration(pastExpiration)
        }.compose()

        // jjwt 는 기본적으로 만료된 토큰 파싱 시 예외를 던진다
        assertFailsWith<Exception> {
            provider.parse(jwt)
        }
    }

    @Test
    fun `expiresAtMillis - exp 클레임이 있으면 밀리초 타임스탬프를 반환한다`() {
        val jwt = provider.compose {
            subject = "alice"
            expirationAfterSeconds = 3600
        }

        val reader = provider.parse(jwt)
        reader.expiresAtMillis.shouldNotBeNull()
        reader.expiresAtMillis!! shouldBeGreaterThan System.currentTimeMillis()
    }

    @Test
    fun `expiredTtl - exp 클레임이 있으면 남은 TTL 밀리초를 반환한다`() {
        val jwt = provider.compose {
            subject = "alice"
            expirationAfterSeconds = 3600
        }

        val reader = provider.parse(jwt)
        reader.expiredTtl shouldBeGreaterThan 0L
        reader.expiredTtl shouldBeLessThan 3_600_001L
        reader.remainingTtlMillis shouldBeGreaterThan 0L
        reader.remainingTtlMillis shouldBeLessThan 3_600_001L
    }

    @Test
    fun `kid - 헤더에서 kid 를 올바르게 읽어온다`() {
        val jwt = provider.compose {
            subject = "alice"
            expirationAfterMinutes = 60
        }

        val reader = provider.parse(jwt)
        reader.kid.shouldNotBeNull()
        reader.kid!! shouldBeGreaterThan ""
    }

    @Test
    fun `tryParse - 잘못된 JWT 문자열은 null 을 반환한다`() {
        val result = provider.tryParse("invalid.jwt.string")
        (result == null).shouldBeTrue()
    }

    @Test
    fun `parse - 잘못된 JWT 문자열은 예외를 던진다`() {
        assertFailsWith<Exception> {
            provider.parse("invalid.jwt.string")
        }
    }
}
