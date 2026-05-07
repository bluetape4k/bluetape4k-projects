package io.bluetape4k.vertx

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.vertx.tests.withTestContext
import io.bluetape4k.vertx.tests.withSuspendTestContext
import io.vertx.core.Vertx
import io.vertx.junit5.VertxTestContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test

class VertxSupportTest : AbstractVertxTest() {

    companion object : KLoggingChannel()

    @Test
    fun `currentVertx 는 null이 아닌 Vertx 인스턴스를 반환한다`(vertx: Vertx, testContext: VertxTestContext) =
        withTestContext(testContext) {
            val current = currentVertx()
            current.shouldNotBeNull()
        }

    @Test
    fun `withVertxDispatcher 는 블록을 실행하고 결과를 반환한다`(vertx: Vertx, testContext: VertxTestContext) =
        runSuspendIO {
            vertx.withSuspendTestContext(testContext) {
                val result = vertx.withVertxDispatcher {
                    "dispatcher-result"
                }
                result shouldBeEqualTo "dispatcher-result"
            }
        }

    @Test
    fun `asCoroutineScope 는 CoroutineScope를 반환한다`(vertx: Vertx, testContext: VertxTestContext) =
        withTestContext(testContext) {
            val scope: CoroutineScope = vertx.asCoroutineScope()
            scope.shouldNotBeNull()
        }

    @Test
    fun `currentVertxCoroutineScope는 코루틴 스코프를 반환한다`(vertx: Vertx, testContext: VertxTestContext) =
        withTestContext(testContext) {
            // currentVertxCoroutineScope()는 Vertx.currentContext()를 사용하므로
            // vertx context 밖에서도 새 Vertx를 생성하여 반환
            val scope = currentVertxCoroutineScope()
            scope.shouldNotBeNull()
        }

    @Test
    fun `currentVertxDispatcher는 CoroutineDispatcher를 반환한다`(vertx: Vertx, testContext: VertxTestContext) =
        withTestContext(testContext) {
            val dispatcher: CoroutineDispatcher = currentVertxDispatcher()
            dispatcher.shouldNotBeNull()
        }

    @Test
    fun `asCoroutineScope의 launch 블록은 정상 실행된다`(vertx: Vertx, testContext: VertxTestContext) {
        val checkpoint = testContext.checkpoint()
        val scope: CoroutineScope = vertx.asCoroutineScope()
        scope.launch {
            testContext.verify {
                true.shouldBeTrue()
                checkpoint.flag()
            }
        }
    }
}
