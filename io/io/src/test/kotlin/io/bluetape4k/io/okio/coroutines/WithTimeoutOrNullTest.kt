package io.bluetape4k.io.okio.coroutines

import io.bluetape4k.io.okio.AbstractOkioTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import okio.Timeout
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class WithTimeoutOrNullTest: AbstractOkioTest() {

    @Test
    fun `Timeout NONE이면 블록 결과를 그대로 반환한다`() = runTest {
        val result = withTimeoutOrNull(Timeout.NONE) {
            "ok"
        }

        result shouldBeEqualTo "ok"
    }

    @Test
    fun `timeout이 경과하면 null을 반환한다`() = runTest {
        val timeout = Timeout().timeout(10, TimeUnit.MILLISECONDS)

        val result = withTimeoutOrNull(timeout) {
            delay(100)
            "late"
        }

        result.shouldBeNull()
    }

    @Test
    fun `deadline이 timeout보다 짧으면 deadline 기준으로 null을 반환한다`() = runTest {
        val timeout = Timeout()
            .timeout(5, TimeUnit.SECONDS)
            .deadline(10, TimeUnit.MILLISECONDS)

        val result = withTimeoutOrNull(timeout) {
            delay(100)
            "late"
        }

        result.shouldBeNull()
    }

    @Test
    fun `서브 밀리초 timeout 에서도 즉시 완료 블록은 성공한다`() = runTest {
        val timeout = Timeout().timeout(1, TimeUnit.NANOSECONDS)

        val result = withTimeoutOrNull(timeout) {
            "ok"
        }

        result shouldBeEqualTo "ok"
    }
}
