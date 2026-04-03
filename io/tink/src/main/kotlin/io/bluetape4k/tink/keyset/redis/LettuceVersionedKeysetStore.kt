package io.bluetape4k.tink.keyset.redis

import com.google.crypto.tink.KeyTemplate
import com.google.crypto.tink.KeysetHandle
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.tink.keyset.VersionedKeysetHandle
import io.bluetape4k.tink.keyset.VersionedKeysetStore
import io.bluetape4k.tink.keyset.keysetHandleOf
import io.bluetape4k.tink.keyset.toJsonKeyset
import io.bluetape4k.tink.registerTink
import io.lettuce.core.SetArgs
import io.lettuce.core.api.StatefulRedisConnection
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.*

/**
 * `lettuce-core`만 직접 사용해 구현한 Redis 기반 versioned Tink keyset 저장소입니다.
 *
 * active version은 String key로, version별 keyset JSON과 생성 시각은 Redis Hash로 저장합니다.
 * rotation은 Redis `SET NX PX` 기반 분산 락으로 직렬화합니다.
 */
class LettuceVersionedKeysetStore(
    private val connection: StatefulRedisConnection<String, String>,
    keyringName: String,
    private val keyTemplate: KeyTemplate,
    private val clock: Clock = Clock.systemUTC(),
) : VersionedKeysetStore {

    private val keyringName = keyringName.requireNotBlank("keyringName")
    private val commands get() = connection.sync()

    private val activeVersionKey = "$keyringName:active"
    private val keysetsKey = "$keyringName:keysets"
    private val createdAtKey = "$keyringName:created-at"
    private val lockKey = "$keyringName:lock"

    init {
        registerTink()
    }

    override fun current(): VersionedKeysetHandle {
        val activeVersion = commands.get(activeVersionKey)?.toLongOrNull()
        if (activeVersion != null) {
            return requireNotNull(find(activeVersion)) { "Missing keyset for activeVersion=$activeVersion" }
        }

        return withLock {
            val reloadedVersion = commands.get(activeVersionKey)?.toLongOrNull()
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
        val keysetJson = commands.hget(keysetsKey, version.toString()) ?: return null
        val createdAtEpochMillis = commands.hget(createdAtKey, version.toString())?.toLongOrNull() ?: return null
        return VersionedKeysetHandle(
            version = version,
            createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
            keysetHandle = keysetHandleOf(keysetJson),
        )
    }

    override fun rotate(): VersionedKeysetHandle =
        withLock {
            val nextVersion = (commands.get(activeVersionKey)?.toLongOrNull() ?: 0L) + 1L
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
        commands.hset(keysetsKey, keyset.version.toString(), keyset.keysetHandle.toJsonKeyset())
        commands.hset(createdAtKey, keyset.version.toString(), keyset.createdAt.toEpochMilli().toString())
        if (activate) {
            commands.set(activeVersionKey, keyset.version.toString())
        }
    }

    private fun <T> withLock(action: () -> T): T {
        val token = UUID.randomUUID().toString()
        val acquired = commands.set(lockKey, token, SetArgs().nx().px(30_000)) != null
        require(acquired) { "Failed to acquire lock for keyring=$keyringName" }
        return try {
            action()
        } finally {
            runCatching {
                if (commands.get(lockKey) == token) {
                    commands.del(lockKey)
                }
            }
        }
    }
}
