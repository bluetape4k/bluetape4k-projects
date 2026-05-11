package io.bluetape4k.tink.keyset.redis

import com.google.crypto.tink.aead.AesGcmKeyManager
import com.google.crypto.tink.daead.AesSivKeyManager
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.testcontainers.storage.RedisServer
import io.bluetape4k.tink.aead.TinkAeads
import io.bluetape4k.tink.keyset.VersionedTinkDaead
import io.lettuce.core.RedisClient
import io.lettuce.core.codec.StringCodec
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class LettuceVersionedKeysetStoreTest {

    companion object {
        private val redis by lazy { RedisServer.Launcher.redis }
        private val client: RedisClient by lazy { RedisServer.Launcher.LettuceLib.getRedisClient(redis.url) }
        private fun randomName(): String = "tink:keyring:${System.nanoTime()}"
    }

    @Test
    fun `store initializes active keyset and reuses it across instances`() {
        val keyring = randomName()
        val connection = client.connect(StringCodec.UTF8)
        val store1 = LettuceVersionedKeysetStore(connection, keyring, AesGcmKeyManager.aes256GcmTemplate())
        val store2 = LettuceVersionedKeysetStore(connection, keyring, AesGcmKeyManager.aes256GcmTemplate())

        store1.current().version shouldBeEqualTo 1L
        store2.current().version shouldBeEqualTo 1L
    }

    @Test
    fun `versioned aead decrypts ciphertext encrypted before rotation`() {
        val keyring = randomName()
        val connection = client.connect(StringCodec.UTF8)
        val store = LettuceVersionedKeysetStore(connection, keyring, AesGcmKeyManager.aes256GcmTemplate())
        val aead = TinkAeads.versioned(store)

        val beforeRotation = aead.encrypt("hello")
        val rotated = aead.rotate()
        val afterRotation = aead.encrypt("world")

        rotated.version shouldBeEqualTo 2L
        aead.decrypt(beforeRotation) shouldBeEqualTo "hello"
        aead.decrypt(afterRotation) shouldBeEqualTo "world"
    }

    @Test
    fun `rotateIfDue returns current key when period not elapsed`() {
        val keyring = randomName()
        val connection = client.connect(StringCodec.UTF8)
        val store = LettuceVersionedKeysetStore(connection, keyring, AesGcmKeyManager.aes256GcmTemplate())

        val current = store.current()
        val result = store.rotateIfDue(Duration.ofDays(1))

        result.version shouldBeEqualTo current.version
    }

    @Test
    fun `versioned deterministic aead keeps old ciphertext decryptable after rotation`() {
        val keyring = randomName()
        val connection = client.connect(StringCodec.UTF8)
        val store = LettuceVersionedKeysetStore(connection, keyring, AesSivKeyManager.aes256SivTemplate())
        val daead = VersionedTinkDaead(store)

        val ct1 = daead.encryptDeterministically("hello")
        val ct2 = daead.encryptDeterministically("hello")
        ct1 shouldBeEqualTo ct2

        store.rotate()

        daead.decryptDeterministically(ct1) shouldBeEqualTo "hello"
        val ct3 = daead.encryptDeterministically("hello")
        (ct3 != ct1).shouldBeTrue()
    }

    @Test
    fun `lock release deletes only when token matches`() {
        val connection = client.connect(StringCodec.UTF8)
        val commands = connection.sync()
        val lockKey = "${randomName()}:lock"

        commands.set(lockKey, "owner-1")
        LettuceVersionedKeysetStore.releaseLockIfOwned(commands, lockKey, "owner-1").shouldBeTrue()
        commands.get(lockKey).shouldBeNull()

        commands.set(lockKey, "owner-2")
        LettuceVersionedKeysetStore.releaseLockIfOwned(commands, lockKey, "stale-owner").shouldBeFalse()
        commands.get(lockKey) shouldBeEqualTo "owner-2"
    }

    @Test
    fun `MultithreadingTester - concurrent rotateIfDue performs a single rotation`() {
        val keyring = randomName()
        val connection = client.connect(StringCodec.UTF8)
        val clock = MutableClock(Instant.parse("2026-05-11T00:00:00Z"))
        val store = LettuceVersionedKeysetStore(
            connection,
            keyring,
            AesGcmKeyManager.aes256GcmTemplate(),
            clock
        )

        store.current().version shouldBeEqualTo 1L
        clock.advanceBy(Duration.ofDays(2))

        // 모든 worker가 같은 due window에서 rotateIfDue를 호출해도 active version은 한 번만 증가해야 한다.
        MultithreadingTester()
            .workers(8)
            .rounds(2)
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
