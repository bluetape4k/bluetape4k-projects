package io.bluetape4k.jwt.provider

import io.bluetape4k.jwt.composer.JwtComposer
import io.bluetape4k.jwt.composer.JwtComposerDsl
import io.bluetape4k.jwt.composer.composeJwt
import io.bluetape4k.jwt.keychain.KeyChain
import kotlinx.atomicfu.locks.withLock
import java.util.concurrent.locks.ReentrantLock

abstract class AbstractJwtProvider: JwtProvider {

    protected val lock = ReentrantLock()

    override fun composer(keyChain: KeyChain?): JwtComposer {
        return lock.withLock {
            JwtComposer(keyChain ?: currentKeyChain())
        }
    }

    override fun compose(keyChain: KeyChain?, initializer: JwtComposerDsl.() -> Unit): String {
        return lock.withLock {
            composeJwt(keyChain ?: currentKeyChain(), initializer)
        }
    }
}
