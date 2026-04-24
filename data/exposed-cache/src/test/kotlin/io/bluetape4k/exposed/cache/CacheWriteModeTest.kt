package io.bluetape4k.exposed.cache

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test

/**
 * [CacheWriteMode] 열거형 단위 테스트.
 *
 * 쓰기 전략 열거형의 값 수, 이름, valueOf 조회가 명세대로인지 검증합니다.
 * 새로운 CacheWriteMode 추가 시 테스트가 실패하므로 의도치 않은 변경을 방지합니다.
 */
class CacheWriteModeTest {

    companion object : KLogging()

    @Test
    fun `CacheWriteMode 열거형은 정확히 3개의 값을 가진다`() {
        CacheWriteMode.entries shouldHaveSize 3
    }

    @Test
    fun `READ_ONLY는 캐시만 갱신하고 DB에는 반영하지 않는 모드이다`() {
        CacheWriteMode.READ_ONLY.name shouldBeEqualTo "READ_ONLY"
    }

    @Test
    fun `WRITE_THROUGH는 캐시와 DB를 동시에 반영하는 모드이다`() {
        CacheWriteMode.WRITE_THROUGH.name shouldBeEqualTo "WRITE_THROUGH"
    }

    @Test
    fun `WRITE_BEHIND는 캐시에 먼저 저장하고 DB는 비동기로 반영하는 모드이다`() {
        CacheWriteMode.WRITE_BEHIND.name shouldBeEqualTo "WRITE_BEHIND"
    }

    @Test
    fun `valueOf로 각 쓰기 전략을 조회할 수 있다`() {
        CacheWriteMode.valueOf("READ_ONLY") shouldBeEqualTo CacheWriteMode.READ_ONLY
        CacheWriteMode.valueOf("WRITE_THROUGH") shouldBeEqualTo CacheWriteMode.WRITE_THROUGH
        CacheWriteMode.valueOf("WRITE_BEHIND") shouldBeEqualTo CacheWriteMode.WRITE_BEHIND
    }
}
