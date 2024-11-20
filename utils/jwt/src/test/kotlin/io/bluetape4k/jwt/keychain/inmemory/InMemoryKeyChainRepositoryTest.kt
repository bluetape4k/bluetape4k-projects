package io.bluetape4k.jwt.keychain.inmemory

import io.bluetape4k.jwt.keychain.AbstractKeyChainRepositoryTest
import io.bluetape4k.jwt.keychain.repository.KeyChainRepository
import io.bluetape4k.jwt.keychain.repository.inmemory.InMemoryKeyChainRepository

class InMemoryKeyChainRepositoryTest: AbstractKeyChainRepositoryTest() {

    override val repository: KeyChainRepository = InMemoryKeyChainRepository()

}
