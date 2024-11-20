package io.bluetape4k.jwt.composer

import io.bluetape4k.LibraryName
import io.bluetape4k.jwt.AbstractJwtTest
import io.bluetape4k.jwt.codec.JwtCodecs
import io.bluetape4k.jwt.keychain.KeyChain
import io.bluetape4k.jwt.provider.JwtProviderFactory
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.replicate
import io.jsonwebtoken.Claims
import io.jsonwebtoken.CompressionCodec
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeLessThan
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertFailsWith

class JwtComposerTest: AbstractJwtTest() {

    companion object: KLogging()

    private fun getCodecs() = listOf(
        JwtCodecs.Lz4,
        JwtCodecs.Snappy,
        JwtCodecs.Zstd,
        JwtCodecs.Gzip,
        JwtCodecs.Deflate
    )

    private val composer = JwtComposer(KeyChain())

    @Test
    fun `JwtComposer 생성하기`() {
        composer.header("x-author", "debop")
        composer.claim("service", LibraryName)
        composer.expirationAfterMinutes(60L)

        val jwt = composer.compose()
        log.debug { "jwt=$jwt" }
        jwt.shouldNotBeEmpty()
    }

    @Test
    fun `잘못된 헤더 정보를 설정하면 예외가 발생한다`() {
        assertFailsWith<IllegalArgumentException> {
            composer.header("", "value")
        }
    }

    @Test
    fun `예약된 헤더 정보를 설정하면 적용되지 않는다`() {
        composer.header(io.bluetape4k.jwt.JwtConsts.HEADER_KEY_ID, "header-key")
        composer.headers[io.bluetape4k.jwt.JwtConsts.HEADER_KEY_ID] shouldNotBeEqualTo "header-key"
    }

    @Test
    fun `잘못된 claim name을 사용하면 예외가 발생한다`() {
        assertFailsWith<IllegalArgumentException> {
            composer.claim("", "value")
        }

        assertFailsWith<IllegalArgumentException> {
            composer.claim(" \t ", "value")
        }

        assertFailsWith<IllegalArgumentException> {
            composer.claim(Claims.EXPIRATION, "value")
        }
        assertFailsWith<IllegalArgumentException> {
            composer.claim(Claims.ISSUED_AT, "value")
        }
        assertFailsWith<IllegalArgumentException> {
            composer.claim(Claims.NOT_BEFORE, "value")
        }
    }

    @ParameterizedTest(name = "compression codec={0}")
    @MethodSource("getCodecs")
    fun `claim 정보가 큰 경우 압축을 사용합니다`(codec: CompressionCodec) {
        repeat(3) {
            val provider = JwtProviderFactory.fixed(kid = "fixed")
            val composer = provider.composer()

            composer.header("x-author", "debop")
            composer.issuer(LibraryName)
            composer.claim("small-data", LibraryName)

            // 아주 긴 문장이라면
            composer.claim("long-claim", randomString(4096).replicate(10))
            composer.claim("long-data", randomString(4096).replicate(10))

            composer.expirationAfterMinutes(60L)

            val plainJwt = composer.compose()
            plainJwt.shouldNotBeEmpty()

            composer.setCompressionCodec(codec)
            val compressedJwt = composer.compose()
            compressedJwt.shouldNotBeEmpty()

            compressedJwt.length shouldBeLessThan plainJwt.length

            log.debug {
                "${codec.algorithmName}, compression ratio=${compressedJwt.length / plainJwt.length.toDouble()}"
            }


            val reader = provider.parse(compressedJwt)

            reader.header<String>("x-author") shouldBeEqualTo "debop"
            reader.issuer shouldBeEqualTo LibraryName
            reader.claim<String>("small-data") shouldBeEqualTo LibraryName
            reader.expiration.time shouldBeGreaterThan System.currentTimeMillis()

            reader.claim<String>("long-claim").shouldNotBeNull().shouldNotBeEmpty()
            reader.claim<String>("long-data").shouldNotBeNull().shouldNotBeEmpty()
        }
    }
}
