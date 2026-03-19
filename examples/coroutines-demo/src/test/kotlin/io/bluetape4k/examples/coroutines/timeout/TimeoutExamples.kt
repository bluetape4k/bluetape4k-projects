package io.bluetape4k.examples.coroutines.timeout

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

/**
 * 코루틴 타임아웃 처리 예제입니다.
 *
 * - [withTimeout]: 지정 시간 초과 시 [TimeoutCancellationException] 발생
 * - [withTimeoutOrNull]: 지정 시간 초과 시 null 반환 (예외 없음)
 */
class TimeoutExamples {
    companion object: KLoggingChannel()

    /**
     * 시간 이내에 완료되면 결과를 반환합니다.
     */
    @Test
    fun `withTimeout - 시간 이내 완료`() =
        runTest {
            val result =
                withTimeout(1000) {
                    delay(500)
                    "완료"
                }
            result shouldBeEqualTo "완료"
        }

    /**
     * 시간 초과 시 [TimeoutCancellationException]이 발생합니다.
     */
    @Test
    fun `withTimeout - 시간 초과 시 예외 발생`() =
        runTest {
            assertFailsWith<TimeoutCancellationException> {
                withTimeout(100) {
                    delay(1000)
                    "이 결과는 반환되지 않음"
                }
            }
            log.debug { "타임아웃 예외가 발생했습니다" }
        }

    /**
     * [withTimeoutOrNull]은 시간 초과 시 예외 대신 null을 반환합니다.
     * 예외 처리 없이 안전하게 타임아웃을 처리할 때 유용합니다.
     */
    @Test
    fun `withTimeoutOrNull - 시간 초과 시 null 반환`() =
        runTest {
            val result =
                withTimeoutOrNull(100) {
                    delay(1000)
                    "이 결과는 반환되지 않음"
                }
            result.shouldBeNull()
            log.debug { "타임아웃으로 null이 반환되었습니다" }
        }

    /**
     * 시간 이내에 완료되면 정상 결과를 반환합니다.
     */
    @Test
    fun `withTimeoutOrNull - 시간 이내 완료`() =
        runTest {
            val result =
                withTimeoutOrNull(1000) {
                    delay(100)
                    "성공"
                }
            result.shouldNotBeNull()
            result shouldBeEqualTo "성공"
        }

    /**
     * 재시도 패턴: 타임아웃 시 재시도하는 실용적인 예제입니다.
     */
    @Test
    fun `withTimeoutOrNull을 이용한 재시도 패턴`() =
        runTest {
            var attempt = 0

            val result =
                retryWithTimeout(maxRetries = 3, timeoutMillis = 200) {
                    attempt++
                    if (attempt < 3) {
                        delay(500) // 처음 2번은 타임아웃
                        "실패"
                    } else {
                        delay(50) // 3번째는 성공
                        "성공"
                    }
                }

            result.shouldNotBeNull()
            result shouldBeEqualTo "성공"
            attempt shouldBeEqualTo 3
            log.debug { "$attempt 번째 시도에서 성공" }
        }

    /**
     * 타임아웃과 함께 재시도하는 유틸리티 함수입니다.
     */
    private suspend fun <T> retryWithTimeout(
        maxRetries: Int,
        timeoutMillis: Long,
        block: suspend () -> T,
    ): T? {
        repeat(maxRetries) { attempt ->
            val result = withTimeoutOrNull(timeoutMillis) { block() }
            if (result != null) return result
            log.debug { "시도 ${attempt + 1}/$maxRetries 타임아웃" }
        }
        return null
    }
}
