package io.bluetape4k.jwt.provider.cache

import io.bluetape4k.LibraryName
import io.bluetape4k.jwt.composer.JwtComposer
import io.bluetape4k.jwt.composer.JwtComposerDsl
import io.bluetape4k.jwt.keychain.KeyChain
import io.bluetape4k.jwt.provider.AbstractJwtProvider
import io.bluetape4k.jwt.provider.JwtProvider
import io.bluetape4k.jwt.reader.JwtReader
import io.bluetape4k.jwt.reader.JwtReaderDto
import io.bluetape4k.jwt.reader.toDto
import io.bluetape4k.jwt.reader.toJwtReader
import io.bluetape4k.logging.KLogging
import org.redisson.api.RMapCache
import org.redisson.api.RedissonClient
import org.redisson.client.codec.StringCodec
import org.redisson.codec.CompositeCodec
import org.redisson.codec.FuryCodec
import org.redisson.codec.LZ4CodecV2
import java.util.concurrent.TimeUnit

/**
 * JWT 파싱된 정보[JwtReader]를 Redis에 캐싱하여 반복적인 Parsing 시간을 절약합니다.
 * Redisson의 경우 캐시 엔트리의 유효기간을 따로 설정할 수 있습니다.
 *
 * @property cache Redisson [RMapCache] 인스턴스
 * @property ttl   캐시 엔트리의 유효기간 (기본값: 3일)
 * @property delegate [JwtProvider] 인스턴스
 * @constructor Create empty Redisson jwt provider
 */
class RedissonJwtProvider private constructor(
    private val delegate: JwtProvider,
    private val cache: RMapCache<String, JwtReaderDto>,
    private val ttl: Long = DEFAULT_TTL,
): AbstractJwtProvider(), JwtProvider by delegate {

    companion object: KLogging() {
        // 3일
        const val DEFAULT_TTL: Long = 3 * 24 * 60_000L
        const val KEYCHANIN_PREFIX = "$LibraryName:jwt:keychain"

        @JvmStatic
        operator fun invoke(
            delegate: JwtProvider,
            cache: RMapCache<String, JwtReaderDto>,
            ttl: Long = DEFAULT_TTL,
        ): RedissonJwtProvider {
            return RedissonJwtProvider(delegate, cache, ttl)
        }

        @JvmStatic
        operator fun invoke(
            delegate: JwtProvider,
            redisson: RedissonClient,
            keychinName: String = KEYCHANIN_PREFIX,
            ttl: Long = DEFAULT_TTL,
        ): RedissonJwtProvider {
            // Key 는 String Codec, Value 는 Fury + LZ4 Codec
            val codec = CompositeCodec(StringCodec(), LZ4CodecV2(FuryCodec()))
            val cache = redisson.getMapCache<String, JwtReaderDto>(keychinName, codec)
            return invoke(delegate, cache, ttl)
        }
    }

    override fun tryParse(jwtString: String): JwtReader? {
        var reader = cache[jwtString]?.toJwtReader()

        return if (reader != null) {
            if (reader.isExpired) {
                cache.fastRemove(jwtString)
            }
            reader
        } else {
            // Cache에 없으므로 Parsing을 수행한다
            reader = delegate.tryParse(jwtString)

            // 만료되지 않았다면 Cache에 저장한다 (JwtReader를 parsing한 호출자가 expired를 검사해서, 버리도록 한다)
            if (reader != null && !reader.isExpired) {
                // JWT 토큰의 유효기간 또는 기본 TTL 값 중 작은 값을 TTL 로 삼는다
                // 기본 TTL은 3일이지만 JWT 토큰의 유효기간이 1일인 경우 TTL은 1일로 설정된다
                val readerTtl =
                    if (reader.expiration != null) reader.expiration.time - System.currentTimeMillis()
                    else Long.MAX_VALUE

                cache.fastPut(jwtString, reader.toDto(), minOf(ttl, readerTtl), TimeUnit.MILLISECONDS)
            }
            reader
        }
    }

    override fun composer(keyChain: KeyChain?): JwtComposer {
        return delegate.composer(keyChain)
    }

    override fun compose(keyChain: KeyChain?, initializer: JwtComposerDsl.() -> Unit): String {
        return delegate.compose(keyChain, initializer)
    }
}
