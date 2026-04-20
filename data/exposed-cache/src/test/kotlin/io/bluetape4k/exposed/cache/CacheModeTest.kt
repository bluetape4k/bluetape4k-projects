package io.bluetape4k.exposed.cache

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test

/**
 * [CacheMode] 열거형 단위 테스트.
 *
 * 열거형 값의 수와 이름이 명세대로인지 검증합니다.
 * 새로운 CacheMode 추가 시 테스트가 실패하므로 의도치 않은 변경을 방지합니다.
 */
class CacheModeTest {

    companion object : KLogging()

    @Test
    fun `CacheMode 열거형은 정확히 3개의 값을 가진다`() {
        CacheMode.entries shouldHaveSize 3
    }

    @Test
    fun `LOCAL 모드는 인프로세스 캐시만 사용한다`() {
        CacheMode.LOCAL.name shouldBeEqualTo "LOCAL"
    }

    @Test
    fun `REMOTE 모드는 Redis 원격 캐시만 사용한다`() {
        CacheMode.REMOTE.name shouldBeEqualTo "REMOTE"
    }

    @Test
    fun `NEAR_CACHE 모드는 로컬과 Redis를 함께 사용한다`() {
        CacheMode.NEAR_CACHE.name shouldBeEqualTo "NEAR_CACHE"
    }

    @Test
    fun `valueOf로 각 모드를 조회할 수 있다`() {
        CacheMode.valueOf("LOCAL") shouldBeEqualTo CacheMode.LOCAL
        CacheMode.valueOf("REMOTE") shouldBeEqualTo CacheMode.REMOTE
        CacheMode.valueOf("NEAR_CACHE") shouldBeEqualTo CacheMode.NEAR_CACHE
    }
}
