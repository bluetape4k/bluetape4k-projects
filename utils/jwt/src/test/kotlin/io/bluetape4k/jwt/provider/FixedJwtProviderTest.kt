package io.bluetape4k.jwt.provider

import io.bluetape4k.LibraryName
import io.bluetape4k.codec.encodeBase62
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.jwt.AbstractJwtTest
import io.bluetape4k.jwt.codec.Lz4Codec
import io.bluetape4k.jwt.utils.dateOfEpochSeconds
import io.bluetape4k.jwt.utils.epochSeconds
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import io.jsonwebtoken.UnsupportedJwtException
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.assertFailsWith

class FixedJwtProviderTest: AbstractJwtTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
        private val compressCodec = Lz4Codec()
    }

    private val provider = JwtProviderFactory.fixed(UUID.randomUUID().encodeBase62())

    @Test
    fun `fixed jwt provider rotate를 수행할 수 없습니다`() {
        assertFailsWith<UnsupportedJwtException> {
            provider.rotate()
        }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `compose jwt token`() {
        val now = Date()
        val jwt = provider.compose {
            claim("author", "debop")
            issuer = LibraryName
            issuedAt = now
        }

        val reader = provider.parse(jwt)
        reader.issuer shouldBeEqualTo LibraryName
        reader.issuedAt shouldBeEqualTo dateOfEpochSeconds(now.epochSeconds)
        reader.claim<String>("author") shouldBeEqualTo "debop"
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `compose jwt in concurrency`() {
        val customData = randomString(1024)
        val now = Date()
        val jwts = CopyOnWriteArrayList<String>()

        MultithreadingTester()
            .numThreads(16)
            .roundsPerThread(32)
            .add {
                val jwt = provider.compose {
                    claim("author", "debop")
                    claim("service", LibraryName)
                    issuer = LibraryName
                    issuedAt = now
                    claim("custom-data", customData)
                    compressionCodec = compressCodec
                }

                jwts.add(jwt)
            }
            .run()

        Thread.sleep(10L)

        jwts.size shouldBeEqualTo 16 * 32
        val uniqueJwts = jwts.distinct()
        uniqueJwts.forEach { jwt ->
            log.trace { "jwt=$jwt" }
        }
        uniqueJwts.size shouldBeEqualTo 1
    }
}
