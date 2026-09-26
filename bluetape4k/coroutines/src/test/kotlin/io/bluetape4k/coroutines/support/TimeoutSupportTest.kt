package io.bluetape4k.coroutines.support

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds

class TimeoutSupportTest {

    companion object: KLoggingChannel()

    /**
     * 재시도 패턴: 타임아웃 시 재시도하는 실용적인 예제입니다.
     */
    @Test
    fun `withTimeoutOrNull을 이용한 재시도 패턴`() = runTest {
        var attempt = 0

        val result = retryWithTimeoutOrNull(3, 200.milliseconds) {
            attempt++
            if (attempt < 3) {
                delay(500.milliseconds) // 처음 2번은 타임아웃
                "실패"
            } else {
                delay(50.milliseconds) // 3번째는 성공
                "성공"
            }
        }

        result shouldBeEqualTo "성공"
        attempt shouldBeEqualTo 3
        log.debug { "$attempt 번째 시도에서 성공" }
    }
}
