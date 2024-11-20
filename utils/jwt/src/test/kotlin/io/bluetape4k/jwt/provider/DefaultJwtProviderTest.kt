package io.bluetape4k.jwt.provider

import io.bluetape4k.logging.KLogging

class DefaultJwtProviderTest: AbstractJwtProviderTest() {

    companion object: KLogging()

    override val provider: JwtProvider =
        JwtProviderFactory.default(keyChainRepository = repository)

}
