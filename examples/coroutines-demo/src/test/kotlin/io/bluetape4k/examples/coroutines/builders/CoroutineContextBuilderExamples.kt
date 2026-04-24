package io.bluetape4k.examples.coroutines.builders

import io.bluetape4k.coroutines.context.PropertyCoroutineContext
import io.bluetape4k.coroutines.support.suspendLogging
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds

/**
 * 부모-자식 코루틴 간 [CoroutineContext] 전달과 재정의를 보여주는 예제입니다.
 *
 * - 자식 코루틴은 부모의 `CoroutineContext`를 상속받습니다.
 * - 자식이 새로운 `CoroutineContext.Element`를 지정하면 해당 키의 부모 값을 재정의합니다.
 * - [PropertyCoroutineContext]를 사용하여 커스텀 프로퍼티를 전달하는 방법도 보여줍니다.
 */
class CoroutineContextBuilderExamples {

    companion object: KLoggingChannel()

    @Test
    fun `부모-자식 간에 CoroutineContext 통해 정보 전달을 한다`() = runTest(CoroutineName("parent")) {
        suspendLogging("Started")
        val v1 = async {
            delay(500.milliseconds)
            suspendLogging("Running coroutines")
            42
        }

        launch {
            delay(1000.milliseconds)
            suspendLogging("Running launch")
        }
        suspendLogging { "The answer is ${v1.await()}" }
    }

    @Test
    fun `자식은 부모의 Context를 재정의 합니다`() = runTest(CoroutineName("parent")) {
        suspendLogging("Started")
        val v1 = async(CoroutineName("c1")) {
            delay(500.milliseconds)
            suspendLogging("Running coroutines")
            42
        }

        launch(CoroutineName("c2")) {
            delay(1000.milliseconds)
            suspendLogging("Running launch")
        }
        suspendLogging { "The answer is ${v1.await()}" }
    }

    @Test
    fun `자식 Context는 부모 Context를 재정의합니다 2`() =
        runTest(CoroutineName("parent") + PropertyCoroutineContext(mapOf("key1" to "value1"))) {
            suspendLogging("Started")
            val v1 = async(CoroutineName("child") + PropertyCoroutineContext(mapOf("key2" to "value2"))) {
                delay(500.milliseconds)
                suspendLogging("Running coroutines")
                42
            }

            launch(PropertyCoroutineContext(mapOf("key3" to "value3"))) {
                delay(1000.milliseconds)
                suspendLogging("Running launch")
            }
            suspendLogging { "The answer is ${v1.await()}" }
        }
}
