package io.bluetape4k.tink.keyset.redis

import com.google.crypto.tink.KeyTemplate
import com.google.crypto.tink.KeysetHandle
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.tink.keyset.VersionedKeysetHandle
import io.bluetape4k.tink.keyset.VersionedKeysetStore
import io.bluetape4k.tink.keyset.keysetHandleOf
import io.bluetape4k.tink.keyset.toJsonKeyset
import io.bluetape4k.tink.registerTink
import org.redisson.api.RedissonClient
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * `RedissonClient`를 직접 사용한 Redis 기반 versioned Tink keyset 저장소입니다.
 */
class RedissonVersionedKeysetStore(
    private val redisson: RedissonClient,
    keyringName: String,
    private val keyTemplate: KeyTemplate,
    private val clock: Clock = Clock.systemUTC(),
): VersionedKeysetStore {

    private val keyringName = keyringName.requireNotBlank("keyringName")
    private val activeVersionBucket = redisson.getBucket<String>("$keyringName:active")
    private val keysetsMap = redisson.getMap<String, String>("$keyringName:keysets")
    private val createdAtMap = redisson.getMap<String, String>("$keyringName:created-at")
    private val lock = redisson.getLock("$keyringName:lock")

    init {
        registerTink()
    }

    override fun current(): VersionedKeysetHandle {
        val activeVersion = activeVersionBucket.get()?.toLongOrNull()
        if (activeVersion != null) {
            return requireNotNull(find(activeVersion)) { "Missing keyset for activeVersion=$activeVersion" }
        }

        return withLock {
            val reloadedVersion = activeVersionBucket.get()?.toLongOrNull()
            if (reloadedVersion != null) {
                requireNotNull(find(reloadedVersion)) { "Missing keyset for activeVersion=$reloadedVersion" }
            } else {
                val initial = newVersionedKeyset(1L)
                persist(initial, activate = true)
                initial
            }
        }
    }

    override fun find(version: Long): VersionedKeysetHandle? {
        val keysetJson = keysetsMap[version.toString()] ?: return null
        val createdAtEpochMillis = createdAtMap[version.toString()]?.toLongOrNull() ?: return null
        return VersionedKeysetHandle(
            version = version,
            createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
            keysetHandle = keysetHandleOf(keysetJson),
        )
    }

    override fun rotate(): VersionedKeysetHandle =
        withLock {
            val nextVersion = (activeVersionBucket.get()?.toLongOrNull() ?: 0L) + 1L
            val rotated = newVersionedKeyset(nextVersion)
            persist(rotated, activate = true)
            rotated
        }

    override fun rotateIfDue(rotationPeriod: Duration): VersionedKeysetHandle {
        require(rotationPeriod > Duration.ZERO) { "rotationPeriod must be positive." }
        val current = current()
        val elapsed = Duration.between(current.createdAt, Instant.now(clock))
        return if (elapsed >= rotationPeriod) rotate() else current
    }

    private fun newVersionedKeyset(version: Long): VersionedKeysetHandle =
        VersionedKeysetHandle(
            version = version,
            createdAt = Instant.now(clock),
            keysetHandle = KeysetHandle.generateNew(keyTemplate),
        )

    private fun persist(keyset: VersionedKeysetHandle, activate: Boolean) {
        keysetsMap[keyset.version.toString()] = keyset.keysetHandle.toJsonKeyset()
        createdAtMap[keyset.version.toString()] = keyset.createdAt.toEpochMilli().toString()
        if (activate) {
            activeVersionBucket.set(keyset.version.toString())
        }
    }

    private fun <T> withLock(action: () -> T): T {
        require(lock.tryLock(5, 30, TimeUnit.SECONDS)) { "Failed to acquire lock for keyring=$keyringName" }
        return try {
            action()
        } finally {
            runCatching {
                if (lock.isHeldByCurrentThread) {
                    lock.unlock()
                }
            }
        }
    }
}
