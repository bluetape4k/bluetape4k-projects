package io.bluetape4k.jwt.composer

import io.bluetape4k.LibraryName
import io.bluetape4k.codec.encodeBase62
import io.bluetape4k.jwt.AbstractJwtTest
import io.bluetape4k.jwt.codec.JwtCodecs
import io.bluetape4k.jwt.keychain.KeyChain
import io.bluetape4k.jwt.provider.JwtProviderFactory
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.replicate
import io.jsonwebtoken.CompressionCodec
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.*

class JwtComposerDslTest: AbstractJwtTest() {

    companion object: KLogging()

    private fun getCodecs() = listOf(
        JwtCodecs.Deflate,
        JwtCodecs.Gzip,
        JwtCodecs.Lz4,
        JwtCodecs.Snappy,
        JwtCodecs.Zstd,
    )

    private val keyChain = KeyChain()
    private val composer = JwtComposer(keyChain)

    @Test
    fun `jwt composed by dsl`() {
        val jwt = composeJwt(keyChain) {
            header("x-author", "debop")
            claim("server", LibraryName)
            claim("library", "$LibraryName-utils-jwt")

            expirationAfterMinutes = 60L
        }

        log.debug { "jwt=$jwt" }
        jwt.shouldNotBeEmpty()
    }


    @Test
    fun `jwt from JwtKeyManager`() {
        val keyManager = JwtProviderFactory.default()

        val jwt = keyManager.compose {
            id = UUID.randomUUID().encodeBase62()
            issuer = "debop"
            subject = "jwt testing"

            header("x-author", "debop")
            claim("service", LibraryName)
            claim("library", "$LibraryName-utils-jwt")

            notBeforeInSeconds = 60L
            expirationAfterMinutes = 60L
        }

        log.debug { "jwt=$jwt" }
        jwt.shouldNotBeEmpty()
    }

    @ParameterizedTest
    @MethodSource("getCodecs")
    fun `claim에 저장할 데이터가 크다면 Compression을 이용하여 압축합니다`(codec: CompressionCodec) {
        val provider = JwtProviderFactory.fixed(kid = "test")
        val compressedJwt = provider.compose {
            header("x-author", "debop")
            issuer = LibraryName
            claim("small-data", LibraryName)

            // 아주 긴 문장
            claim("long-claim", randomString(4096).replicate(4))
            claim("long-data", randomString(4096).replicate(4))
            expirationAfterMinutes = 60L
            this.compressionCodec = codec
        }

        val reader = provider.parse(compressedJwt)

        reader.header<String>("x-author") shouldBeEqualTo "debop"
        reader.issuer shouldBeEqualTo LibraryName
        reader.claim<String>("small-data") shouldBeEqualTo LibraryName
        reader.expiration.time shouldBeGreaterThan System.currentTimeMillis()
    }
}
