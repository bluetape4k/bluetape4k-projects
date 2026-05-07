package io.bluetape4k.exposed.cache

import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import io.bluetape4k.assertions.assertFailsWith
import java.time.Duration

/**
 * [LocalCacheConfig] 단위 테스트.
 *
 * 기본값 생성, 정적 팩토리, 입력 검증(init 블록) 경로를 모두 커버합니다.
 */
class LocalCacheConfigTest {

    companion object : KLogging()

    // ----------------------------------------------------------------
    // 기본값 및 정상 생성
    // ----------------------------------------------------------------

    @Test
    fun `기본값으로 생성 시 모든 프로퍼티가 명세된 기본값을 가진다`() {
        val config = LocalCacheConfig()

        config.keyPrefix shouldBeEqualTo "local"
        config.maximumSize shouldBeEqualTo 10_000L
        config.expireAfterWrite shouldBeEqualTo Duration.ofMinutes(10)
        config.expireAfterAccess.shouldBeNull()
        config.writeMode shouldBeEqualTo CacheWriteMode.READ_ONLY
        config.writeBehindBatchSize shouldBeEqualTo 100
        config.writeBehindQueueCapacity shouldBeEqualTo 10_000
    }

    @Test
    fun `READ_ONLY 정적 팩토리는 READ_ONLY writeMode를 가진다`() {
        val config = LocalCacheConfig.READ_ONLY
        config.writeMode shouldBeEqualTo CacheWriteMode.READ_ONLY
    }

    @Test
    fun `WRITE_THROUGH 정적 팩토리는 WRITE_THROUGH writeMode를 가진다`() {
        val config = LocalCacheConfig.WRITE_THROUGH
        config.writeMode shouldBeEqualTo CacheWriteMode.WRITE_THROUGH
    }

    @Test
    fun `WRITE_BEHIND 정적 팩토리는 WRITE_BEHIND writeMode를 가진다`() {
        val config = LocalCacheConfig.WRITE_BEHIND
        config.writeMode shouldBeEqualTo CacheWriteMode.WRITE_BEHIND
    }

    @Test
    fun `expireAfterAccess를 설정하면 프로퍼티에 저장된다`() {
        val access = Duration.ofMinutes(5)
        val config = LocalCacheConfig(expireAfterAccess = access)
        config.expireAfterAccess.shouldNotBeNull() shouldBeEqualTo access
    }

    @Test
    fun `최소 유효 값으로 생성이 가능하다`() {
        val config = LocalCacheConfig(
            keyPrefix = "x",
            maximumSize = 1L,
            expireAfterWrite = Duration.ofMillis(1),
            writeBehindBatchSize = 1,
            writeBehindQueueCapacity = 1,
        )
        config.maximumSize shouldBeEqualTo 1L
        config.writeBehindBatchSize shouldBeEqualTo 1
        config.writeBehindQueueCapacity shouldBeEqualTo 1
    }

    // ----------------------------------------------------------------
    // 입력 검증 — keyPrefix
    // ----------------------------------------------------------------

    @Test
    fun `keyPrefix가 빈 문자열이면 IllegalArgumentException이 발생한다`() {
        // 빈 keyPrefix는 캐시 키 네임스페이스 충돌로 이어지므로 허용하지 않는다.
        assertFailsWith<IllegalArgumentException> {
            LocalCacheConfig(keyPrefix = "")
        }
    }

    @Test
    fun `keyPrefix가 공백만 있으면 IllegalArgumentException이 발생한다`() {
        assertFailsWith<IllegalArgumentException> {
            LocalCacheConfig(keyPrefix = "   ")
        }
    }

    // ----------------------------------------------------------------
    // 입력 검증 — maximumSize
    // ----------------------------------------------------------------

    @Test
    fun `maximumSize가 0이면 IllegalArgumentException이 발생한다`() {
        // maximumSize 0은 캐시를 비활성화하거나 구현체에 따라 OOM을 유발할 수 있다.
        assertFailsWith<IllegalArgumentException> {
            LocalCacheConfig(maximumSize = 0L)
        }
    }

    @Test
    fun `maximumSize가 음수이면 IllegalArgumentException이 발생한다`() {
        assertFailsWith<IllegalArgumentException> {
            LocalCacheConfig(maximumSize = -1L)
        }
    }

    // ----------------------------------------------------------------
    // 입력 검증 — expireAfterWrite
    // ----------------------------------------------------------------

    @Test
    fun `expireAfterWrite가 0이면 IllegalArgumentException이 발생한다`() {
        // 0 이하이면 저장 직후 즉시 만료되어 캐시 효과가 없다.
        assertFailsWith<IllegalArgumentException> {
            LocalCacheConfig(expireAfterWrite = Duration.ZERO)
        }
    }

    @Test
    fun `expireAfterWrite가 음수이면 IllegalArgumentException이 발생한다`() {
        assertFailsWith<IllegalArgumentException> {
            LocalCacheConfig(expireAfterWrite = Duration.ofSeconds(-1))
        }
    }

    // ----------------------------------------------------------------
    // 입력 검증 — expireAfterAccess
    // ----------------------------------------------------------------

    @Test
    fun `expireAfterAccess가 0이면 IllegalArgumentException이 발생한다`() {
        // 설정된 expireAfterAccess가 0이면 접근 직후 즉시 만료되는 것과 같다.
        assertFailsWith<IllegalArgumentException> {
            LocalCacheConfig(expireAfterAccess = Duration.ZERO)
        }
    }

    @Test
    fun `expireAfterAccess가 음수이면 IllegalArgumentException이 발생한다`() {
        assertFailsWith<IllegalArgumentException> {
            LocalCacheConfig(expireAfterAccess = Duration.ofSeconds(-1))
        }
    }

    @Test
    fun `expireAfterAccess가 null이면 정상 생성된다`() {
        val config = LocalCacheConfig(expireAfterAccess = null)
        config.expireAfterAccess.shouldBeNull()
    }

    // ----------------------------------------------------------------
    // 입력 검증 — writeBehindBatchSize
    // ----------------------------------------------------------------

    @Test
    fun `writeBehindBatchSize가 0이면 IllegalArgumentException이 발생한다`() {
        // writeBehindBatchSize 0은 Write-Behind flush 배치가 영원히 실행되지 않는다.
        assertFailsWith<IllegalArgumentException> {
            LocalCacheConfig(writeBehindBatchSize = 0)
        }
    }

    @Test
    fun `writeBehindBatchSize가 음수이면 IllegalArgumentException이 발생한다`() {
        assertFailsWith<IllegalArgumentException> {
            LocalCacheConfig(writeBehindBatchSize = -1)
        }
    }

    // ----------------------------------------------------------------
    // 입력 검증 — writeBehindQueueCapacity
    // ----------------------------------------------------------------

    @Test
    fun `writeBehindQueueCapacity가 writeBehindBatchSize보다 작으면 IllegalArgumentException이 발생한다`() {
        // 큐 용량이 배치 크기보다 작으면 큐가 즉시 포화되어 Write-Behind가 동작하지 않는다.
        assertFailsWith<IllegalArgumentException> {
            LocalCacheConfig(writeBehindBatchSize = 100, writeBehindQueueCapacity = 99)
        }
    }

    @Test
    fun `writeBehindQueueCapacity가 writeBehindBatchSize와 같으면 정상 생성된다`() {
        val config = LocalCacheConfig(writeBehindBatchSize = 50, writeBehindQueueCapacity = 50)
        config.writeBehindQueueCapacity shouldBeEqualTo 50
    }

    // ----------------------------------------------------------------
    // Serializable 직렬화 라운드트립
    // ----------------------------------------------------------------

    @Test
    fun `Java 직렬화 라운드트립을 통해 원본과 동일한 설정이 복원된다`() {
        val original = LocalCacheConfig(
            keyPrefix = "actor",
            maximumSize = 5_000L,
            expireAfterWrite = Duration.ofMinutes(30),
            writeMode = CacheWriteMode.WRITE_THROUGH,
        )

        val bytes = java.io.ByteArrayOutputStream().use { baos ->
            java.io.ObjectOutputStream(baos).use { oos -> oos.writeObject(original) }
            baos.toByteArray()
        }
        val restored = java.io.ByteArrayInputStream(bytes).use { bais ->
            java.io.ObjectInputStream(bais).use { ois -> ois.readObject() as LocalCacheConfig }
        }

        restored.keyPrefix shouldBeEqualTo original.keyPrefix
        restored.maximumSize shouldBeEqualTo original.maximumSize
        restored.expireAfterWrite shouldBeEqualTo original.expireAfterWrite
        restored.writeMode shouldBeEqualTo original.writeMode
    }
}
