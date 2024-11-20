package io.bluetape4k.jwt.reader

import io.bluetape4k.LibraryName
import io.bluetape4k.cache.jcache.JCaching
import io.bluetape4k.cache.nearcache.NearCache
import io.bluetape4k.cache.nearcache.NearCacheConfig
import io.bluetape4k.jwt.AbstractJwtTest
import io.bluetape4k.jwt.codec.Lz4Codec
import io.bluetape4k.jwt.provider.JwtProviderFactory
import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.storage.RedisServer
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEqualTo
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.jupiter.api.RepeatedTest
import org.redisson.codec.LZ4Codec
import java.time.Duration
import java.util.*
import javax.cache.Cache

class JwtReaderCachingTest: AbstractJwtTest() {

    companion object: KLogging() {

        private const val REPEAT_SIZE = 3

        private val frontCache1: Cache<String, JwtReaderDto> by lazy {
            JCaching.Caffeine.getOrCreate("caffeine-reader-1")
        }

        private val frontCache2: Cache<String, JwtReaderDto> by lazy {
            JCaching.EhCache.getOrCreate("ehcache-reader-2")
        }

        private val redissonConfig by lazy {
            RedisServer.Launcher.RedissonLib.getRedissonConfig().apply {
                // 압축을 하면 데이터 사이즈가 감소한다
                codec = LZ4Codec()
            }
        }

        private val backCache: Cache<String, JwtReaderDto> by lazy {
            JCaching.Redisson.getOrCreate("jwt-back-cache-reader", redissonConfig)
        }
    }

    private val jwtProvider = JwtProviderFactory.default()
    private val createdAt = System.currentTimeMillis()

    private val jwt: String = jwtProvider.compose {
        header("x-publisher", LibraryName)
        issuedAt = Date(createdAt)
        issuer = LibraryName
        claim("service", LibraryName)

        compressionCodec = Lz4Codec()
    }

    private val jwt2: String = jwtProvider.compose {
        header("x-publisher", LibraryName)
        issuedAt = Date(createdAt)
        issuer = LibraryName
        claim("service", LibraryName + "-2")

        compressionCodec = Lz4Codec()
    }

    private val reader = jwtProvider.parse(jwt)
    private val reader2 = jwtProvider.parse(jwt2)

    @RepeatedTest(REPEAT_SIZE)
    fun `caching reader at near cache`() {
        frontCache1.put(jwt, reader.toDto())
        val actual = frontCache1.get(jwt)!!.toJwtReader()

        assertSameReader(reader, actual)
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `caching reader with hash key at near cache`() {
        val hashKey1 = jwt.hashCode().toString()
        val hashKey2 = jwt2.hashCode().toString()

        hashKey1 shouldNotBeEqualTo hashKey2

        frontCache1.put(hashKey1, reader.toDto())
        frontCache1.put(hashKey2, reader2.toDto())

        val actual = frontCache1.get(hashKey1)!!.toJwtReader()
        val actual2 = frontCache1.get(hashKey2)!!.toJwtReader()

        assertSameReader(reader, actual)
        assertSameReader(reader2, actual2)
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `caching reader with remote cache`() {
        backCache.put(jwt, reader.toDto())
        val actual = backCache.get(jwt)!!.toJwtReader()
        assertSameReader(reader, actual)

        frontCache1.put(jwt, actual.toDto())
        val actual2 = frontCache1.get(jwt)!!.toJwtReader()
        assertSameReader(actual, actual2)
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `caching reader with hash key at remote cache`() {
        val hashKey1 = jwt.hashCode().toString()
        val hashKey2 = jwt2.hashCode().toString()

        hashKey1 shouldNotBeEqualTo hashKey2

        backCache.put(hashKey1, reader.toDto())
        backCache.put(hashKey2, reader2.toDto())

        val actual = backCache.get(hashKey1)!!.toJwtReader()
        val actual2 = backCache.get(hashKey2)!!.toJwtReader()

        assertSameReader(reader, actual)
        assertSameReader(reader2, actual2)
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `caching reader with two near cache`() {
        val nearCacheConfig1 = NearCacheConfig<String, JwtReaderDto>(isSynchronous = true)
        val nearCacheConfig2 = NearCacheConfig<String, JwtReaderDto>(isSynchronous = true)
        val nearCache1 = NearCache(nearCacheConfig1, backCache)
        val nearCache2 = NearCache(nearCacheConfig2, backCache)

        // Cache 1 에서 저장
        nearCache1.put(jwt, reader.toDto())
        nearCache1.get(jwt)!!.toJwtReader() shouldBeEqualTo reader

        await atMost Duration.ofMillis(100) until { nearCache2.containsKey(jwt) }

        // Cache 2 에서 조회
        val actual = nearCache2.get(jwt)!!.toJwtReader()

        assertSameReader(reader, actual)
    }

    private fun assertSameReader(expected: JwtReader, actual: JwtReader) {
        actual.header<String>("x-publisher") shouldBeEqualTo expected.header("x-publisher")
        actual.claim<String>("service") shouldBeEqualTo expected.claim<String>("service")
        actual.issuedAt shouldBeEqualTo expected.issuedAt
        actual.issuer shouldBeEqualTo expected.issuer
    }
}
