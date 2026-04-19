package io.bluetape4k.tink.keyset

import io.bluetape4k.logging.KLogging
import io.bluetape4k.tink.aeadKeysetHandle
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 테스트 전용 인메모리 [VersionedKeysetStore] 구현체.
 */
internal class InMemoryVersionedKeysetStore(
    private val clock: Clock = Clock.systemUTC(),
) : VersionedKeysetStore {

    companion object : KLogging()

    private val store = ConcurrentHashMap<Long, VersionedKeysetHandle>()
    private val currentVersion = AtomicLong(0)

    override fun current(): VersionedKeysetHandle {
        return store.getOrPut(1L) {
            val handle = VersionedKeysetHandle(
                version = 1L,
                createdAt = Instant.now(clock),
                keysetHandle = aeadKeysetHandle(),
            )
            currentVersion.set(1L)
            handle
        }
    }

    override fun find(version: Long): VersionedKeysetHandle? = store[version]

    override fun rotate(): VersionedKeysetHandle {
        val next = (store.keys.maxOrNull() ?: 0L) + 1L
        val handle = VersionedKeysetHandle(
            version = next,
            createdAt = Instant.now(clock),
            keysetHandle = aeadKeysetHandle(),
        )
        store[next] = handle
        currentVersion.set(next)
        return handle
    }

    override fun rotateIfDue(rotationPeriod: Duration): VersionedKeysetHandle {
        val current = current()
        val elapsed = Duration.between(current.createdAt, Instant.now(clock))
        return if (elapsed >= rotationPeriod) rotate() else current
    }
}
