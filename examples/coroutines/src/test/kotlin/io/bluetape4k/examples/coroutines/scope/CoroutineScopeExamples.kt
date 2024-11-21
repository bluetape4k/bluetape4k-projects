package io.bluetape4k.examples.coroutines.scope

import io.bluetape4k.coroutines.support.log
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import io.bluetape4k.logging.warn
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldContainSame
import org.junit.jupiter.api.Test

class CoroutineScopeExamples {

    companion object: KLogging()

    private data class Details(val name: String, val followers: Int)
    private data class Tweet(val text: String)

    private fun getFollowersNumber(): Int = throw RuntimeException("Service exception")

    private suspend fun getUserName(): String {
        delay(500)
        log.debug { "get user name ..." }
        return "debop"
    }

    private suspend fun getTweets(): List<Tweet> {
        delay(10)
        log.debug { "Get tweets ..." }
        return listOf(Tweet("Hello, world"))
    }

    private suspend fun getUserDetails(): Details {
        // 하나의 CoroutineScope 에서 두 개의 Coroutine 작업이 실행될 때,
        // 하나가 예외를 일으키면, 다른 하나는 Cancel 되고, 예외가 전파된다.
        return coroutineScope {
            val userName = async { getUserName() }
            val follwerNumbers = async { getFollowersNumber() }

            Details(userName.await(), follwerNumbers.await())
        }
    }

    @Test
    fun `coroutine scope를 사용하여 복수의 작업 실행하기`() = runTest {
        val details: Details? = try {
            getUserDetails()
        } catch (e: RuntimeException) {
            log.warn { "getFollowersNumber 에서 예외 발생. ${e.message}" }
            null
        }
        val tweets = async { getTweets() }

        log.info { "Details: $details" }  // null
        details.shouldBeNull()

        log.info { "Tweets: ${tweets.await()}" } // [Tweet(text=Hello, world)]
        tweets.await() shouldContainSame listOf(Tweet("Hello, world"))
    }

    /**
     * 여러 작업을 병렬로 수행하기 위해서는 `coroutineScope` 또는 `supervisorScope` 를 사용하세요
     * `coroutineScope`는 부모 Coroutine Context 를 상속받아서 사용합니다.
     *
     * 그리고 `coroutineScope` 내의 비동기 작업은 모두 종료될 때까지 기다립니다.
     */
    private suspend fun longTask(taskStatus: MutableMap<String, Boolean>) = coroutineScope {
        launch {
            delay(100)
            val name = coroutineContext[CoroutineName]?.name
            log.info { "[$name] finish task 1" }
            taskStatus["Task 1"] = true
        }
        launch {
            delay(200)
            val name = coroutineContext[CoroutineName]?.name
            log.info { "[$name] finish task 2" }
            taskStatus["Task 2"] = true
        }
    }

    @Test
    fun `coroutineScope를 사용하여 비동기 작업하기`() = runTest {
        val taskStatus = mutableMapOf("Task 1" to false, "Task 2" to false)

        val job = launch(CoroutineName("Parent")) {
            longTask(taskStatus)
        }.log("Parent")

        // Task 1만 실행되고, Task 2는 Cancel 됩니다.
        delay(150)
        job.cancel()
        taskStatus shouldContainSame mapOf("Task 1" to true, "Task 2" to false)
    }
}
