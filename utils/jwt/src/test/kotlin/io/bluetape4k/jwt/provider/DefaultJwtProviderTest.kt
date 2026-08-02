package io.bluetape4k.jwt.provider

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.jwt.keychain.KeyChain
import io.bluetape4k.jwt.keychain.repository.AbstractKeyChainRepository
import io.bluetape4k.jwt.keychain.repository.KeyChainRepository
import io.bluetape4k.jwt.keychain.repository.inmemory.InMemoryKeyChainRepository
import io.bluetape4k.logging.KLogging
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertTrue

class DefaultJwtProviderTest: AbstractJwtProviderTest() {

    companion object: KLogging()

    override val repository: KeyChainRepository = InMemoryKeyChainRepository()

    override val provider: JwtProvider =
        JwtProviderFactory.default(keyChainRepository = repository)

    @Test
    fun `key chain repository exposes close and stops its timer`() {
        val lifecycleRepository = LifecycleKeyChainRepository(refreshIntervalMillis = 25)
        val timerName = lifecycleRepository.javaClass.name

        try {
            await.atMost(Duration.ofSeconds(2)).until { lifecycleRepository.refreshCount.get() > 0 }
            await.atMost(Duration.ofSeconds(2)).until { hasLiveThread(timerName) }

            assertTrue(lifecycleRepository is AutoCloseable)
            (lifecycleRepository as AutoCloseable).close()

            val refreshCountAfterClose = lifecycleRepository.refreshCount.get()
            Thread.sleep(100)
            lifecycleRepository.refreshCount.get().shouldBeEqualTo(refreshCountAfterClose)
            await.atMost(Duration.ofSeconds(2)).until { !hasLiveThread(timerName) }
        } finally {
            lifecycleRepository.close()
        }
    }

    @Test
    fun `default jwt provider exposes close and stops its timer without closing borrowed repository`() {
        val lifecycleRepository = LifecycleKeyChainRepository(refreshIntervalMillis = 25)
        val lifecycleProvider: JwtProvider = DefaultJwtProvider.forTesting(
            keyChainRepository = lifecycleRepository,
            rotationIntervalMillis = 25,
        )
        val repositoryTimerName = lifecycleRepository.javaClass.name

        try {
            await.atMost(Duration.ofSeconds(2)).until { lifecycleRepository.rotateCount.get() > 1 }
            await.atMost(Duration.ofSeconds(2)).until { hasLiveThread(repositoryTimerName) }

            assertTrue(lifecycleProvider is AutoCloseable)
            (lifecycleProvider as AutoCloseable).close()

            val rotationCountAfterClose = lifecycleRepository.rotateCount.get()
            Thread.sleep(100)
            lifecycleRepository.rotateCount.get().shouldBeEqualTo(rotationCountAfterClose)
            hasLiveThread(repositoryTimerName).shouldBeEqualTo(true)

            (lifecycleRepository as AutoCloseable).close()
            await.atMost(Duration.ofSeconds(2)).until { !hasLiveThread(repositoryTimerName) }
        } finally {
            lifecycleProvider.close()
            lifecycleRepository.close()
        }
    }

    private fun hasLiveThread(name: String): Boolean =
        Thread.getAllStackTraces().keys.any { it.isAlive && it.name == name }

    private class LifecycleKeyChainRepository(
        refreshIntervalMillis: Long,
    ): AbstractKeyChainRepository(refreshIntervalMillis) {

        private val keyChains = ConcurrentLinkedDeque<KeyChain>()
        val refreshCount = AtomicInteger()
        val rotateCount = AtomicInteger()

        override val capacity: Int = KeyChainRepository.DEFAULT_CAPACITY

        override fun doLoadCurrent(): KeyChain? {
            refreshCount.incrementAndGet()
            return keyChains.firstOrNull()
        }

        override fun doInsert(keyChain: KeyChain) {
            keyChains.addFirst(keyChain)
        }

        override fun findOrNull(kid: String): KeyChain? = keyChains.find { it.id == kid }

        override fun rotate(keyChain: KeyChain): Boolean {
            rotateCount.incrementAndGet()
            return changeCurrent(keyChain)
        }

        override fun forcedRotate(keyChain: KeyChain): Boolean {
            rotateCount.incrementAndGet()
            return changeCurrent(keyChain)
        }

        override fun deleteAll() {
            keyChains.clear()
            cachedCurrent = null
        }
    }

}
