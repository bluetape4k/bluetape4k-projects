package io.bluetape4k.jwt.provider

import io.bluetape4k.jwt.JwtConsts.DefaultKeyChainRepository
import io.bluetape4k.jwt.JwtConsts.DefaultSignatureAlgorithm
import io.bluetape4k.jwt.keychain.repository.KeyChainRepository
import io.bluetape4k.jwt.provider.JwtProviderFactory.default
import io.bluetape4k.jwt.provider.JwtProviderFactory.fixed
import io.bluetape4k.jwt.provider.JwtProviderFactory.jcached
import io.bluetape4k.jwt.provider.JwtProviderFactory.redissonCached
import io.bluetape4k.jwt.provider.cache.JCacheJwtProvider
import io.bluetape4k.jwt.provider.cache.RedissonJwtProvider
import io.bluetape4k.jwt.provider.cache.RedissonJwtProvider.Companion.DEFAULT_TTL
import io.bluetape4k.jwt.reader.JwtReaderDto
import io.bluetape4k.logging.KLogging
import io.jsonwebtoken.security.SignatureAlgorithm
import org.redisson.api.RMapCache
import java.security.KeyPair
import javax.cache.Cache

/**
 * [JwtProvider] 구현체를 생성하는 팩토리입니다.
 *
 * ## 동작/계약
 * - [default]: 키체인 로테이션을 지원하는 [DefaultJwtProvider]를 생성합니다.
 * - [fixed]: 고정 키를 사용하는 [FixedJwtProvider]를 생성합니다.
 * - [jcached]: JCache 기반 파싱 결과 캐싱 [JCacheJwtProvider]를 생성합니다.
 * - [redissonCached]: Redisson 기반 파싱 결과 캐싱 [RedissonJwtProvider]를 생성합니다.
 */
object JwtProviderFactory: KLogging() {

    /**
     * [DefaultJwtProvider]를 생성합니다.
     *
     * @param signatureAlgorithm  RSA 공개키 기반의 암호화 알고리즘 (기본은 RSA 256)
     * @param keyChainRepository  KeyChain Repository
     * @return [DefaultJwtProvider] instance
     */
    fun default(
        signatureAlgorithm: SignatureAlgorithm = DefaultSignatureAlgorithm,
        keyChainRepository: KeyChainRepository = DefaultKeyChainRepository,
    ): JwtProvider {
        return DefaultJwtProvider(signatureAlgorithm, keyChainRepository)
    }

    /**
     * [FixedJwtProvider]를 생성합니다.
     *
     * @param kid KeyChain 의 Id (jwt header에 kid 로 제공됩니다)
     * @param signatureAlgorithm   RSA 공개키 기반의 암호화 알고리즘 (기본은 RSA 256)
     * @param keyPair 암호화에 사용할 public, private key
     *
     * @return [FixedJwtProvider] instance
     */
    fun fixed(
        kid: String,
        signatureAlgorithm: SignatureAlgorithm = DefaultSignatureAlgorithm,
        keyPair: KeyPair = signatureAlgorithm.keyPair().build(),
    ): JwtProvider {
        return FixedJwtProvider(signatureAlgorithm, keyPair, kid)
    }


    /**
     * JWT 의 파싱된 정보인 [io.bluetape4k.jwt.reader.JwtReader]를 캐싱하는 [JCacheJwtProvider] 인스턴스를 생성합니다.
     *
     * @param delegate [JwtProvider] 인스턴스
     * @param cache [JwtReaderDto]를 저장하는 JCache
     */
    fun jcached(
        delegate: JwtProvider,
        cache: Cache<String, JwtReaderDto>,
    ): JwtProvider {
        return JCacheJwtProvider(delegate, cache)
    }

    /**
     * JWT 의 파싱된 정보를 Redisson RMapCache에 캐싱하는 [RedissonJwtProvider] 인스턴스를 생성합니다.
     *
     * @param delegate [JwtProvider] 인스턴스
     * @param cache Redisson [RMapCache] 인스턴스
     * @param ttl 캐시 엔트리의 유효기간 (밀리초, 기본: 3일)
     */
    fun redissonCached(
        delegate: JwtProvider,
        cache: RMapCache<String, JwtReaderDto>,
        ttl: Long = DEFAULT_TTL,
    ): JwtProvider {
        return RedissonJwtProvider(delegate, cache, ttl)
    }
}
