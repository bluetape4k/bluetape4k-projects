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
 *
 * 저장되는 keyset JSON은 Tink key material을 포함합니다. 운영 환경에서는 Redis 접근 제어, 전송/저장 구간 암호화,
 * 백업 보호, KMS/HSM 기반 envelope 보호 같은 별도 key-management 통제를 적용하세요.
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
        return withLock {
            // Due 판단은 lock 안에서 다시 읽는다. 여러 caller가 같은 stale active keyset을 보고
            // 순차적으로 rotate하는 것을 막아 "한 due window당 한 번"만 회전하게 한다.
            val activeVersion = activeVersionBucket.get()?.toLongOrNull()
            val current = if (activeVersion != null) {
                requireNotNull(find(activeVersion)) { "Missing keyset for activeVersion=$activeVersion" }
            } else {
                val initial = newVersionedKeyset(1L)
                persist(initial, activate = true)
                initial
            }
            val elapsed = Duration.between(current.createdAt, Instant.now(clock))
            if (elapsed >= rotationPeriod) {
                val rotated = newVersionedKeyset(current.version + 1L)
                persist(rotated, activate = true)
                rotated
            } else {
                current
            }
        }
    }

    private fun newVersionedKeyset(version: Long): VersionedKeysetHandle =
        VersionedKeysetHandle(
            version = version,
            createdAt = Instant.now(clock),
            keysetHandle = KeysetHandle.generateNew(keyTemplate),
        )

    private fun persist(keyset: VersionedKeysetHandle, activate: Boolean) {
        // active version은 마지막에 갱신한다. reader가 active=N을 봤다면 keyset/createdAt도 이미 기록된 상태여야 한다.
        if (activate) {
            check(lock.isHeldByCurrentThread) { "Lost lock ownership for keyring=$keyringName" }
        }
        keysetsMap[keyset.version.toString()] = keyset.keysetHandle.toJsonKeyset()
        createdAtMap[keyset.version.toString()] = keyset.createdAt.toEpochMilli().toString()
        if (activate) {
            activeVersionBucket.set(keyset.version.toString())
        }
    }

    private fun <T> withLock(action: () -> T): T {
        check(lock.tryLock(5, TimeUnit.SECONDS)) { "Failed to acquire lock for keyring=$keyringName" }
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
