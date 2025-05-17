package io.bluetape4k.examples.coroutines.builders

import io.bluetape4k.coroutines.context.PropertyCoroutineContext
import io.bluetape4k.coroutines.support.coLogging
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class CoroutineContextBuilderExamples {

    companion object: KLoggingChannel()

    @Test
    fun `부모-자식 간에 CoroutineContext 통해 정보 전달을 한다`() = runTest(CoroutineName("parent")) {
        coLogging("Started")
        val v1 = async {
            delay(500)
            coLogging("Running coroutines")
            42
        }

        launch {
            delay(1000)
            coLogging("Running launch")
        }
        coLogging { "The answer is ${v1.await()}" }
    }

    @Test
    fun `자식은 부모의 Context를 재정의 합니다`() = runTest(CoroutineName("parent")) {
        coLogging("Started")
        val v1 = async(CoroutineName("c1")) {
            delay(500)
            coLogging("Running coroutines")
            42
        }

        launch(CoroutineName("c2")) {
            delay(1000)
            coLogging("Running launch")
        }
        coLogging { "The answer is ${v1.await()}" }
    }

    @Test
    fun `자식 Context는 부모 Context를 재정의합니다 2`() =
        runTest(CoroutineName("parent") + PropertyCoroutineContext(mapOf("key1" to "value1"))) {
            coLogging("Started")
            val v1 = async(CoroutineName("child") + PropertyCoroutineContext(mapOf("key2" to "value2"))) {
                delay(500)
                coLogging("Running coroutines")
                42
            }

            launch(PropertyCoroutineContext(mapOf("key3" to "value3"))) {
                delay(1000)
                coLogging("Running launch")
            }
            coLogging { "The answer is ${v1.await()}" }
        }
}
