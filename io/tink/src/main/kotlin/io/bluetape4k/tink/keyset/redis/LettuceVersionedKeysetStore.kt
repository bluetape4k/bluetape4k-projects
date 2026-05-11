package io.bluetape4k.tink.keyset.redis

import com.google.crypto.tink.KeyTemplate
import com.google.crypto.tink.KeysetHandle
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.tink.keyset.VersionedKeysetHandle
import io.bluetape4k.tink.keyset.VersionedKeysetStore
import io.bluetape4k.tink.keyset.keysetHandleOf
import io.bluetape4k.tink.keyset.toJsonKeyset
import io.bluetape4k.tink.registerTink
import io.lettuce.core.ScriptOutputType
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
 *
 * 저장되는 keyset JSON은 Tink key material을 포함합니다. 운영 환경에서는 Redis 접근 제어, 전송/저장 구간 암호화,
 * 백업 보호, KMS/HSM 기반 envelope 보호 같은 별도 key-management 통제를 적용하세요.
 */
class LettuceVersionedKeysetStore(
    private val connection: StatefulRedisConnection<String, String>,
    keyringName: String,
    private val keyTemplate: KeyTemplate,
    private val clock: Clock = Clock.systemUTC(),
): VersionedKeysetStore {

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
        return withLock {
            // Due 판단은 lock 안에서 다시 읽는다. 여러 caller가 같은 stale active keyset을 보고
            // 순차적으로 rotate하는 것을 막아 "한 due window당 한 번"만 회전하게 한다.
            val activeVersion = commands.get(activeVersionKey)?.toLongOrNull()
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
        commands.hset(keysetsKey, keyset.version.toString(), keyset.keysetHandle.toJsonKeyset())
        commands.hset(createdAtKey, keyset.version.toString(), keyset.createdAt.toEpochMilli().toString())
        if (activate) {
            commands.set(activeVersionKey, keyset.version.toString())
        }
    }

    private fun <T> withLock(action: () -> T): T {
        val token = UUID.randomUUID().toString()
        val acquired = acquireLock(token)
        check(acquired) { "Failed to acquire lock for keyring=$keyringName" }
        return try {
            action()
        } finally {
            runCatching {
                releaseLockIfOwned(commands, lockKey, token)
            }
        }
    }

    private fun acquireLock(token: String): Boolean {
        val deadline = System.nanoTime() + LOCK_WAIT_TIMEOUT_NANOS
        while (System.nanoTime() < deadline) {
            if (commands.set(lockKey, token, SetArgs().nx().px(LOCK_TTL_MILLIS)) != null) {
                return true
            }
            // Lettuce sync API를 쓰는 저장소라 짧게 대기 후 재시도한다.
            // 즉시 실패시키면 정상적인 동시 rotate/current 호출도 테스트 flakes가 된다.
            Thread.sleep(LOCK_RETRY_DELAY_MILLIS)
        }
        return false
    }

    companion object {
        private const val LOCK_TTL_MILLIS = 30_000L
        private const val LOCK_WAIT_TIMEOUT_MILLIS = 5_000L
        private const val LOCK_RETRY_DELAY_MILLIS = 20L
        private val LOCK_WAIT_TIMEOUT_NANOS = Duration.ofMillis(LOCK_WAIT_TIMEOUT_MILLIS).toNanos()

        private const val RELEASE_LOCK_SCRIPT: String =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                "return redis.call('del', KEYS[1]) " +
                "else return 0 end"

        internal fun releaseLockIfOwned(
            commands: io.lettuce.core.api.sync.RedisCommands<String, String>,
            lockKey: String,
            token: String,
        ): Boolean {
            // Redis lock 해제는 GET 후 DEL로 나누면 TTL 만료 경계에서 새 소유자의 lock을 지울 수 있다.
            // Lua script 한 번으로 token 비교와 삭제를 원자적으로 처리한다.
            val deleted = commands.eval<Long>(
                RELEASE_LOCK_SCRIPT,
                ScriptOutputType.INTEGER,
                arrayOf(lockKey),
                token
            )
            return deleted == 1L
        }
    }
}
