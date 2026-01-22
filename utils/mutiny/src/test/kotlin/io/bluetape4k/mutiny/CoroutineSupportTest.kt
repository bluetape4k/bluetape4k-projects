package io.bluetape4k.mutiny

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.awaitSuspending
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class CoroutineSupportTest {

    companion object: KLoggingChannel()

    @Test
    fun `suspend 함수를 Uni로 변환하기`() = runTest {
        val expected1 = 42L
        val expected2 = 43L

        val defaultScope = CoroutineScope(Dispatchers.Default)
        val u1: Uni<Long> = defaultScope.asUni() {
            delay(100L)
            log.debug { "suspend method 1 실행 in Uni" }
            expected1
        }

        val ioScope = CoroutineScope(Dispatchers.IO)
        val u2: Uni<Long> = ioScope.asUni {
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
