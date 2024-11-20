package io.bluetape4k.jwt.provider.cache

import com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider
import io.bluetape4k.cache.jcache.getOrCreate
import io.bluetape4k.cache.jcache.jcacheManager
import io.bluetape4k.jwt.provider.AbstractJwtProviderTest
import io.bluetape4k.jwt.provider.JwtProvider
import io.bluetape4k.jwt.provider.JwtProviderFactory
import io.bluetape4k.jwt.reader.JwtReaderDto
import io.bluetape4k.logging.KLogging

class JCacheJwtProviderTest: AbstractJwtProviderTest() {

    companion object: KLogging()

    private val jcache =
        jcacheManager<CaffeineCachingProvider>().getOrCreate<String, JwtReaderDto>("jwt")

    private val delegate = JwtProviderFactory.default(keyChainRepository = repository)

    override val provider: JwtProvider = JwtProviderFactory.jcached(delegate, jcache)

}
