package io.bluetape4k.vertx.tests

import io.bluetape4k.vertx.withVertxDispatcher
import io.vertx.core.Vertx
import io.vertx.junit5.VertxTestContext
import kotlinx.coroutines.CoroutineScope

/**
 * Vertx Framework 테스트 시 [VertxTestContext]를 사용하여 테스트를 수행합니다.
 *
 * ```
 * @Test
 * fun `test something`(testContext: VertxTestContext) = withTestContext(testContext) {
 *
 *    // 테스트 코드
 *
 * }
 * ```
 *
 * @param testContext [VertxTestContext] 인스턴스
 * @param block 실행할 테스트 코드 블럭
 */
inline fun withTestContext(
    testContext: VertxTestContext,
    block: () -> Unit,
) {
    try {
        block()
        testContext.completeNow()
    } catch (e: Throwable) {
        testContext.failNow(e)
    }
}

/**
 * Vertx Framework 테스트 시 [VertxTestContext]를 사용하여 Coroutines 환경에서 테스트를 수행합니다.
 *
 * ```
 * @Test
 * fun `request to server by coroutines`(testContext: VertxTestContext) = runSuspendTest {
 *     vertx.withTestContextSuspending(testContext) {
 *         val webClient = WebClient.create(vertx)
 *         vertx.deployVerticle(SampleVerticle()).coAwait()
 *         val response = webClient.get(11981, "localhost", "/yo")
 *             .`as`(BodyCodec.string())
 *             .send()
 *             .coAwait()
 *         testContext.verify {
 *             response.statusCode() shouldBeEqualTo 200
 *             response.body() shouldContain "Yo!"
 *         }
 *     }
 * }
 * ```
 *
 * @param vertx       [Vertx] 인스턴스
 * @param testContext [VertxTestContext] 인스턴스
 * @param block 실행할 Coroutines 테스트 코드 블럭
 */
suspend inline fun <T: Any> Vertx.withSuspendTestContext(
    testContext: VertxTestContext,
    crossinline block: suspend CoroutineScope.() -> T,
): T? = withVertxDispatcher {
    try {
        val result = block()
        testContext.completeNow()
        result
    } catch (e: Throwable) {
        testContext.failNow(e)
        null
    }
}
