package io.bluetape4k.tink.keyset.redis

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import org.junit.jupiter.api.Test

class LockCleanupSupportTest {

    @Test
    fun `작업 성공 후 lock cleanup 실패는 호출자에게 전달된다`() {
        val cleanupFailure = IllegalStateException("cleanup failed")

        val thrown = assertFailsWith<IllegalStateException> {
            withObservedCleanup(
                action = {},
                cleanup = { throw cleanupFailure },
            )
        }

        thrown shouldBeSameInstanceAs cleanupFailure
    }

    @Test
    fun `작업 실패와 lock cleanup 실패가 함께 발생하면 작업 예외와 suppressed를 보존한다`() {
        val actionFailure = IllegalStateException("action failed")
        val cleanupFailure = IllegalArgumentException("cleanup failed")

        val thrown = assertFailsWith<IllegalStateException> {
            withObservedCleanup<String>(
                action = { throw actionFailure },
                cleanup = { throw cleanupFailure },
            )
        }

        thrown shouldBeSameInstanceAs actionFailure
        thrown.suppressed.single() shouldBeSameInstanceAs cleanupFailure
    }

    @Test
    fun `lock cleanup은 작업 결과를 유지하면서 한 번 실행된다`() {
        var cleanupCount = 0

        val result = withObservedCleanup(
            action = { "completed" },
            cleanup = { cleanupCount++ },
        )

        result shouldBeEqualTo "completed"
        cleanupCount shouldBeEqualTo 1
    }
}
