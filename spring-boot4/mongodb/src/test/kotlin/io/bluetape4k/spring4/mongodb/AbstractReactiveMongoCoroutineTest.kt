package io.bluetape4k.spring4.mongodb

import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * Kotlin Coroutines 기반 Spring Data MongoDB 통합 테스트의 기반 클래스입니다.
 *
 * [AbstractReactiveMongoTest]를 상속하며 [CoroutineScope]를 구현합니다.
 * `Dispatchers.IO`와 지정한 [coroutineName]으로 스코프가 초기화됩니다.
 *
 * **사용 방법**: 구체 테스트 클래스에 `@DataMongoTest` 어노테이션을 붙이고 이 클래스를 상속합니다.
 *
 * ```kotlin
 * @DataMongoTest
 * class MyCoroutineMongoTest: AbstractReactiveMongoCoroutineTest() {
 *     @Test
 *     fun `coroutine test`() = runTest {
 *         val user = mongoOperations.insertSuspending(User(name = "Bob"))
 *         user.id.shouldNotBeNull()
 *     }
 * }
 * ```
 *
 * @param coroutineName 코루틴 스코프의 이름 (기본값: `"spring-mongodb"`)
 */
abstract class AbstractReactiveMongoCoroutineTest(
    private val coroutineName: String = "spring-mongodb",
) : AbstractReactiveMongoTest(),
    CoroutineScope by CoroutineScope(Dispatchers.IO + CoroutineName(coroutineName)) {
    companion object : KLoggingChannel()
}
