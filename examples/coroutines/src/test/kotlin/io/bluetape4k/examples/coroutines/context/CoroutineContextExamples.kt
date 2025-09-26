package io.bluetape4k.examples.coroutines.context

import io.bluetape4k.coroutines.context.PropertyCoroutineContext
import io.bluetape4k.coroutines.support.log
import io.bluetape4k.coroutines.support.suspendLogging
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldContain
import org.junit.jupiter.api.Test
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

class CoroutineContextExamples {

    companion object: KLoggingChannel()

    @Test
    fun `coroutineContext에서 element 찾기`() = runTest {
        val ctx: CoroutineContext = CoroutineName("A name")

        // CoroutineContext 인 ctx에서 [CoroutineName]이라는 CoroutineContext.Element를 찾는다.
        val coroutineName: CoroutineName? = ctx[CoroutineName]
        log.debug { "coroutine name=" + coroutineName?.name }
        coroutineName?.name shouldBeEqualTo "A name"
    }

    @Test
    fun `coroutineContext에 element 추가하기`() = runTest {
        val ctx1 = CoroutineName("name1")
        val ctx2 = Job()

        // ctx2 는 ctx1, ctx2 를 모두 가진다
        val ctx3 = ctx1 + ctx2

        ctx3[CoroutineName]?.name shouldBeEqualTo ctx1[CoroutineName]?.name shouldBeEqualTo "name1"
        ctx3[Job]?.isActive shouldBeEqualTo ctx2[Job]?.isActive shouldBeEqualTo true
        ctx1[Job].shouldBeNull()
    }

    @Test
    fun `coroutineContext에 element 제거하기`() = runTest {
        val ctx1 = CoroutineName("name1") + Job()

        val ctx2 = ctx1.minusKey(CoroutineName)

        ctx2[CoroutineName].shouldBeNull()
        ctx2[Job]?.isActive shouldBeEqualTo true
    }

    @Test
    fun `fold - 복수의 CoroutineContext의 Element들을 누적한다`() = runTest {
        val props = mapOf("key1" to 1, "key2" to "two")
        val ctx = PropertyCoroutineContext(props) + Job() + Dispatchers.IO

        // ctx의 모든 element 들을 누적한다
        val str = ctx.fold("") { acc, element -> "$acc $element" }
        suspendLogging { "ctx=$str" }
        str shouldContain "key1=1, key2=two" shouldContain "JobImpl{Active}"

        val empty = emptyList<CoroutineContext>()
        val ctxs: List<CoroutineContext> = ctx.fold(empty) { acc, element -> acc + element }
        val strs = ctxs.joinToString()
        suspendLogging { "ctxs=$strs" }
        strs shouldContain "key1=1, key2=two" shouldContain "JobImpl{Active}"
    }

    @Test
    fun `Counter를 가진 CoroutineContext 사용`() = runTest {
        withContext(CounterCoroutineContext("Outer")) {
            printNextCount()
            launch {
                coroutineContext[CounterCoroutineContext]?.number shouldBeEqualTo 1L
                printNextCount()    // Outer 1

                launch(Dispatchers.Default) {
                    coroutineContext[CounterCoroutineContext]?.number shouldBeEqualTo 2L
                    printNextCount()  // Outer 2
                }
                    .log("outer2")
                    .join()

                // 새로운 Counter 를 가진 CoroutineContext 를 만들어서 launch 한다
                launch(CounterCoroutineContext("Inner")) {
                    coroutineContext[CounterCoroutineContext]?.number shouldBeEqualTo 0L
                    printNextCount()  // Inner 0

                    coroutineContext[CounterCoroutineContext]?.number shouldBeEqualTo 1L
                    printNextCount()  // Inner 1

                    launch(Dispatchers.IO) {
                        coroutineContext[CounterCoroutineContext]?.number shouldBeEqualTo 2L
                        printNextCount()  // Inner 2
                    }
                        .log("inner2")
                }
                    .log("inner")
                    .join()
            }
                .log("outer")
                .join()

            coroutineContext[CounterCoroutineContext]?.number shouldBeEqualTo 3L
            printNextCount()  // Outer 3
        }
    }

    @Test
    fun `Timebased UUID를 제공하는 CoroutineContext 사용하기`() = runTest {

        val scope = CoroutineScope(TimebasedUuidProviderCoroutineContext() + Dispatchers.IO)

        scope.launch {
            val user = makeUser("Debop")
            log.debug { "user=$user" }
        }.join()

        // 제대로된 UUID 를 생성해주는 CoroutineContext 를 사용한다
        withContext(TimebasedUuidProviderCoroutineContext()) {
            val user = makeUser("Debop")
            log.debug { "user=$user" }
        }

        withContext(FakeUuidProviderCoroutineContext("FAKE_UUID")) {
            val user = makeUser("Debop")
            log.debug { "user=$user" }
            user shouldBeEqualTo User("FAKE_UUID", "Debop")
        }

        withContext(FakeUuidProviderCoroutineContext("임시_UUID")) {
            val user = makeUser("Debop")
            log.debug { "user=$user" }
            user shouldBeEqualTo User("임시_UUID", "Debop")
        }
    }

    /**
     * CoroutineScope 에서 [UuidProviderCoroutineContext] 가 있으면 새로운 Uuid 문자열을 제공합니다.
     */
    private suspend fun nextUuid(): String =
        coroutineContext[UuidProviderCoroutineContext]?.nextUuid()
            ?: error("UuidProviderCoroutineContext not present")

    data class User(val id: String, val name: String)

    private suspend fun makeUser(name: String) = User(id = nextUuid(), name = name)
}
