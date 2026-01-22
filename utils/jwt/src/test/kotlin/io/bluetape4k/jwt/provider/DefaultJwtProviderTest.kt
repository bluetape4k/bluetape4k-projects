package io.bluetape4k.jwt.provider

import io.bluetape4k.jwt.keychain.repository.KeyChainRepository
import io.bluetape4k.jwt.keychain.repository.inmemory.InMemoryKeyChainRepository
import io.bluetape4k.logging.KLogging

class DefaultJwtProviderTest: AbstractJwtProviderTest() {

    companion object: KLogging()

    override val repository: KeyChainRepository = InMemoryKeyChainRepository()

    override val provider: JwtProvider =
        JwtProviderFactory.default(keyChainRepository = repository)

}
