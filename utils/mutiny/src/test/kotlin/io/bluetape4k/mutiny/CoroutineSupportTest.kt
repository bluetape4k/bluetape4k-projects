package io.bluetape4k.mutiny

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.awaitSuspending
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class CoroutineSupportTest {

    companion object: KLogging()

    @Test
    fun `suspend 함수를 Uni로 변환하기`() = runTest {
        val expected1 = 42L
        val expected2 = 43L

        val u1: Uni<Long> = asUni(Dispatchers.Default) {
            delay(100L)
            log.debug { "suspend method 1 실행 in Uni" }
            expected1
        }
        val u2: Uni<Long> = asUni(Dispatchers.IO) {
            delay(100L)
            log.debug { "suspend method 2 실행 in Uni" }
            expected2
        }
        log.debug { "Await ..." }

        u1.awaitSuspending() shouldBeEqualTo expected1
        u2.awaitSuspending() shouldBeEqualTo expected2
        log.debug { "Done" }
    }
}
