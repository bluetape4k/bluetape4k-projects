package io.bluetape4k.jwt.keychain.redis

import io.bluetape4k.jwt.keychain.AbstractKeyChainRepositoryTest
import io.bluetape4k.jwt.keychain.repository.KeyChainRepository
import io.bluetape4k.jwt.keychain.repository.redis.RedisKeyChainRepository
import io.bluetape4k.testcontainers.storage.RedisServer

class RedisKeyChainRepositoryTest: AbstractKeyChainRepositoryTest() {

    private val redisson by lazy {
        RedisServer.Launcher.RedissonLib.getRedisson()
    }

    override val repository: KeyChainRepository by lazy {
        RedisKeyChainRepository(redisson)
    }

}
