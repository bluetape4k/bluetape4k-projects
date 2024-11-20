package io.bluetape4k.jwt.provider.cache

import io.bluetape4k.jwt.composer.JwtComposer
import io.bluetape4k.jwt.composer.JwtComposerDsl
import io.bluetape4k.jwt.keychain.KeyChain
import io.bluetape4k.jwt.provider.AbstractJwtProvider
import io.bluetape4k.jwt.provider.JwtProvider
import io.bluetape4k.jwt.reader.JwtReader
import io.bluetape4k.jwt.reader.JwtReaderDto
import io.bluetape4k.jwt.reader.toDto
import io.bluetape4k.jwt.reader.toJwtReader
import javax.cache.Cache

/**
 * JWT 파싱된 정보 [JwtReader]를 Java Cache에 캐싱하여 반복적인 Parsing 시간을 절약합니다.
 *
 * @property cache    JwtReaderDto를 저장할 Javax Cache
 * @property delegate JWT 를 파싱하는 [JwtProvider] 인스턴스
 */
class JCacheJwtProvider(
    private val delegate: JwtProvider,
    private val cache: Cache<String, JwtReaderDto>,
): AbstractJwtProvider(), JwtProvider by delegate {

    override fun tryParse(jwtString: String): JwtReader? {
        return cache.get(jwtString)?.toJwtReader()
            ?.apply {
                if (isExpired) {
                    cache.remove(jwtString)
                }
            }
            ?: delegate.tryParse(jwtString)?.apply {
                if (!isExpired) {
                    cache.put(jwtString, toDto())
                }
            }
    }

    override fun composer(keyChain: KeyChain?): JwtComposer {
        return delegate.composer(keyChain)
    }

    override fun compose(keyChain: KeyChain?, initializer: JwtComposerDsl.() -> Unit): String {
        return delegate.compose(keyChain, initializer)
    }
}
