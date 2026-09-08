package io.bluetape4k.tink.keyset.redis

import com.google.crypto.tink.aead.AesGcmKeyManager
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.tink.registerTink as registerTinkSupport
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.SetArgs
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.redisson.api.RBucket
import org.redisson.api.RLock
import org.redisson.api.RMap
import org.redisson.api.RedissonClient
import java.util.concurrent.TimeUnit

class RedisLockCleanupContractTest {

    companion object {
        @JvmStatic
        @BeforeAll
        fun registerTink() {
            registerTinkSupport()
        }
    }

    @Test
    fun `Lettuce lock cleanup 실패는 작업 성공 시 호출자에게 전달된다`() {
        val commands = mockk<RedisCommands<String, String>>()
        val connection = mockk<StatefulRedisConnection<String, String>>()
        val cleanupFailure = IllegalStateException("lettuce cleanup failed")
        var evalCalls = 0

        every { connection.sync() } returns commands
        every { commands.get(any<String>()) } returns null
        every { commands.set(any<String>(), any<String>(), any<SetArgs>()) } returns "OK"
        every {
            commands.eval<Long>(any<String>(), ScriptOutputType.INTEGER, any<Array<String>>(), *anyVararg<String>())
        } answers {
            evalCalls++
            if (evalCalls == 1) 1L else throw cleanupFailure
        }

        val store = LettuceVersionedKeysetStore(
            connection,
            "tink:cleanup:lettuce",
            AesGcmKeyManager.aes256GcmTemplate(),
        )

        val thrown = assertFailsWith<IllegalStateException> { store.rotate() }

        thrown shouldBeSameInstanceAs cleanupFailure
    }

    @Test
    fun `Lettuce lock ownership loss는 작업 성공 시 호출자에게 전달된다`() {
        val commands = mockk<RedisCommands<String, String>>()
        val connection = mockk<StatefulRedisConnection<String, String>>()
        var evalCalls = 0

        every { connection.sync() } returns commands
        every { commands.get(any<String>()) } returns null
        every { commands.set(any<String>(), any<String>(), any<SetArgs>()) } returns "OK"
        every {
            commands.eval<Long>(any<String>(), ScriptOutputType.INTEGER, any<Array<String>>(), *anyVararg<String>())
        } answers {
            evalCalls++
            if (evalCalls == 1) 1L else 0L
        }

        val store = LettuceVersionedKeysetStore(
            connection,
            "tink:cleanup:lettuce-lost",
            AesGcmKeyManager.aes256GcmTemplate(),
        )

        val thrown = assertFailsWith<IllegalStateException> { store.rotate() }

        thrown.message!! shouldContain "ownership"
    }

    @Test
    fun `Redisson lock cleanup 실패는 작업 성공 시 호출자에게 전달된다`() {
        val redisson = mockk<RedissonClient>()
        val activeVersionBucket = mockk<RBucket<String>>(relaxed = true)
        val keysetsMap = mockk<RMap<String, String>>(relaxed = true)
        val createdAtMap = mockk<RMap<String, String>>(relaxed = true)
        val lock = mockk<RLock>()
        val cleanupFailure = IllegalStateException("redisson cleanup failed")

        every { redisson.getBucket<String>(any<String>()) } returns activeVersionBucket
        every { redisson.getMap<String, String>(any<String>()) } returnsMany listOf(keysetsMap, createdAtMap)
        every { redisson.getLock(any<String>()) } returns lock
        every { activeVersionBucket.get() } returns null
        every { lock.tryLock(5, TimeUnit.SECONDS) } returns true
        every { lock.isHeldByCurrentThread } returns true
        every { lock.unlock() } throws cleanupFailure

        val store = RedissonVersionedKeysetStore(
            redisson,
            "tink:cleanup:redisson",
            AesGcmKeyManager.aes256GcmTemplate(),
        )

        val thrown = assertFailsWith<IllegalStateException> { store.rotate() }

        thrown shouldBeSameInstanceAs cleanupFailure
    }

    @Test
    fun `Redisson lock ownership loss는 작업 성공 시 호출자에게 전달된다`() {
        val redisson = mockk<RedissonClient>()
        val activeVersionBucket = mockk<RBucket<String>>(relaxed = true)
        val keysetsMap = mockk<RMap<String, String>>(relaxed = true)
        val createdAtMap = mockk<RMap<String, String>>(relaxed = true)
        val lock = mockk<RLock>()

        every { redisson.getBucket<String>(any<String>()) } returns activeVersionBucket
        every { redisson.getMap<String, String>(any<String>()) } returnsMany listOf(keysetsMap, createdAtMap)
        every { redisson.getLock(any<String>()) } returns lock
        every { activeVersionBucket.get() } returns null
        every { lock.tryLock(5, TimeUnit.SECONDS) } returns true
        every { lock.isHeldByCurrentThread } returns false

        val store = RedissonVersionedKeysetStore(
            redisson,
            "tink:cleanup:redisson-lost",
            AesGcmKeyManager.aes256GcmTemplate(),
        )

        val thrown = assertFailsWith<IllegalStateException> { store.rotate() }

        thrown.message!! shouldContain "ownership"
    }
}
