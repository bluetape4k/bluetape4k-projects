package io.bluetape4k.redis.lettuce

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class RedisCommandSupportsTest: AbstractLettuceTest() {

    @AfterEach
    fun teardown() {
        RedisCommandSupports.clearCache()
    }

    @Test
    fun `supports - 기본 명령어 GET은 항상 지원`() {
        RedisCommandSupports.supports(client, "GET").shouldBeTrue()
    }

    @Test
    fun `supports - 알 수 없는 명령어도 예외 없이 완료`() {
        val result = runCatching { RedisCommandSupports.supports(client, "NONEXISTENTCMD12345") }
        result.isSuccess.shouldBeTrue()
    }

    @Test
    fun `supports - 동일 명령어 재조회는 캐시에서 반환`() {
        RedisCommandSupports.supports(client, "SET").shouldBeTrue()
        RedisCommandSupports.supports(client, "SET").shouldBeTrue()
    }

    @Test
    fun `supports - 대소문자 무관하게 동작`() {
        val upper = RedisCommandSupports.supports(client, "HSET")
        val lower = RedisCommandSupports.supports(client, "hset")
        upper shouldBeEqualTo lower
    }

    @Test
    fun `supportsHSetEx - Redis 서버 버전에 따라 결과 반환`() {
        // 결과에 관계없이 예외 없이 완료되어야 함
        val result = runCatching { RedisCommandSupports.supportsHSetEx(client) }
        result.isSuccess.shouldBeTrue()
    }

    @Test
    fun `supportsLMPop - Redis 7+에서 지원`() {
        val result = runCatching { RedisCommandSupports.supportsLMPop(client) }
        result.isSuccess.shouldBeTrue()
    }

    @Test
    fun `supportsWaitAof - Redis 7_2+에서 지원`() {
        val result = runCatching { RedisCommandSupports.supportsWaitAof(client) }
        result.isSuccess.shouldBeTrue()
    }

    @Test
    fun `clearCache - 캐시를 초기화한다`() {
        RedisCommandSupports.supports(client, "GET")
        RedisCommandSupports.clearCache()
        RedisCommandSupports.supports(client, "GET").shouldBeTrue()
    }
}
