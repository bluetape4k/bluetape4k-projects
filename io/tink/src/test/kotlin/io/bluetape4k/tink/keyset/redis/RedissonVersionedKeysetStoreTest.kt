package io.bluetape4k.tink.keyset.redis

import com.google.crypto.tink.aead.AesGcmKeyManager
import com.google.crypto.tink.daead.AesSivKeyManager
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeEqualTo
import io.bluetape4k.codec.Base58
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.storage.RedisServer
import io.bluetape4k.tink.AbstractTinkTest
import io.bluetape4k.tink.aead.TinkAeads
import io.bluetape4k.tink.keyset.VersionedTinkDaead
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.redisson.api.RedissonClient
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class RedissonVersionedKeysetStoreTest: AbstractTinkTest() {

    companion object: KLogging() {
        private val redis by lazy { RedisServer.Launcher.redis }
        private val redisson: RedissonClient by lazy { RedisServer.Launcher.RedissonLib.getRedisson(redis.url) }
        private fun randomName(): String = "tink:keyring:${Base58.randomString(8)}"
    }

    @Test
    fun `store initializes active keyset and reuses it across instances`() {
        val keyring = randomName()
        val store1 = RedissonVersionedKeysetStore(redisson, keyring, AesGcmKeyManager.aes256GcmTemplate())
        val store2 = RedissonVersionedKeysetStore(redisson, keyring, AesGcmKeyManager.aes256GcmTemplate())

        store1.current().version shouldBeEqualTo 1L
        store2.current().version shouldBeEqualTo 1L
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `versioned aead decrypts ciphertext encrypted before rotation`() {
        val keyring = randomName()
        val store = RedissonVersionedKeysetStore(redisson, keyring, AesGcmKeyManager.aes256GcmTemplate())
        val aead = TinkAeads.versioned(store)

        val beforeText = faker.lorem().paragraph()
        val afterText = faker.lorem().paragraph()

        val beforeRotation = aead.encrypt(beforeText)
        val rotated = aead.rotate()
        val afterRotation = aead.encrypt(afterText)

        rotated.version shouldBeEqualTo 2L
        aead.decrypt(beforeRotation) shouldBeEqualTo beforeText
        aead.decrypt(afterRotation) shouldBeEqualTo afterText
    }

    @Test
    fun `rotateIfDue returns current key when period not elapsed`() {
        val keyring = randomName()
        val store = RedissonVersionedKeysetStore(redisson, keyring, AesGcmKeyManager.aes256GcmTemplate())

        val current = store.current()
        val result = store.rotateIfDue(Duration.ofDays(1))

        result.version shouldBeEqualTo current.version
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `versioned deterministic aead keeps old ciphertext decryptable after rotation`() {
        val keyring = randomName()
        val store = RedissonVersionedKeysetStore(redisson, keyring, AesSivKeyManager.aes256SivTemplate())
        val daead = VersionedTinkDaead(store)

        val plaintext = faker.lorem().paragraph()
        val ct1 = daead.encryptDeterministically(plaintext)
        val ct2 = daead.encryptDeterministically(plaintext)
        ct1 shouldBeEqualTo ct2

        store.rotate()

        daead.decryptDeterministically(ct1) shouldBeEqualTo plaintext
        val ct3 = daead.encryptDeterministically(plaintext)
        ct3 shouldNotBeEqualTo ct1
    }

    @Test
    fun `MultithreadingTester - concurrent rotateIfDue performs a single rotation`() {
        val keyring = randomName()
        val clock = MutableClock(Instant.parse("2026-05-11T00:00:00Z"))
        val store = RedissonVersionedKeysetStore(
            redisson,
            keyring,
            AesGcmKeyManager.aes256GcmTemplate(),
            clock
        )

        store.current().version shouldBeEqualTo 1L
        clock.advanceBy(Duration.ofDays(2))

        // Redisson lock은 대기형이므로, 동시 due check가 몰려도 lock 안의 재확인으로 단일 회전만 허용한다.
        MultithreadingTester()
            .workers(8)
            .rounds(4)
            .add {
                store.rotateIfDue(Duration.ofDays(1))
            }
            .run()

        store.current().version shouldBeEqualTo 2L
    }

    private class MutableClock(
        @Volatile private var current: Instant,
        private val zone: ZoneId = ZoneId.of("UTC"),
    ): Clock() {

        override fun getZone(): ZoneId = zone

        override fun withZone(zone: ZoneId): Clock = MutableClock(current, zone)

        override fun instant(): Instant = current

        fun advanceBy(duration: Duration) {
            current = current.plus(duration)
        }
    }
}
