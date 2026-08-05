package io.bluetape4k.retrofit2.client

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

/**
 * Shared OkHttp [Call.Factory] conformance tests for Retrofit transport adapters.
 */
abstract class CallFactoryConformanceTest: AbstractClientTest() {

    private lateinit var conformanceServer: MockWebServer

    protected abstract fun callFactory(callTimeout: Duration): Call.Factory

    @BeforeEach
    fun startConformanceServer() {
        conformanceServer = MockWebServer().apply { start() }
    }

    @AfterEach
    fun stopConformanceServer() {
        runCatching { conformanceServer.shutdown() }
    }

    @Test
    fun `cancel sets isCanceled to true immediately before execute`() {
        conformanceServer.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))

        val call = callFactory.newCall(request())

        call.isCanceled().shouldBeFalse()
        call.cancel()
        call.isCanceled().shouldBeTrue()
    }

    @Test
    fun `cancel before enqueue still fires onFailure callback`() {
        conformanceServer.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))

        val call = callFactory.newCall(request())
        call.cancel()

        val latch = CountDownLatch(1)
        call.enqueue(countingCallback(latch))

        latch.await(5, TimeUnit.SECONDS).shouldBeTrue()
        call.isCanceled().shouldBeTrue()
    }

    @Test
    fun `cancel during enqueue propagates to underlying request and fires callback promptly`() {
        conformanceServer.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))

        val call = callFactory.newCall(request())
        val latch = CountDownLatch(1)

        call.enqueue(countingCallback(latch))
        call.cancel()

        latch.await(5, TimeUnit.SECONDS).shouldBeTrue()
        call.isCanceled().shouldBeTrue()
    }

    @Test
    fun `delayed response completes and response body can be closed`() {
        conformanceServer.enqueue(
            MockResponse()
                .setBody("delayed")
                .setBodyDelay(100, TimeUnit.MILLISECONDS)
        )

        callFactory.newCall(request()).execute().use { response ->
            response.isSuccessful.shouldBeTrue()
            response.body.string() shouldBeEqualTo "delayed"
        }
    }

    @Test
    fun `call timeout advertises the adapter deadline`() {
        val call = callFactory.newCall(request())

        call.timeout().timeoutNanos() shouldBeEqualTo TimeUnit.SECONDS.toNanos(30)
    }

    @Test
    fun `execute timeout aborts underlying request`() {
        conformanceServer.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))

        val call = callFactory(Duration.ofMillis(100)).newCall(request())

        val error = assertFailsWith<IOException> {
            call.execute()
        }

        val errorMessage = error.message.shouldNotBeNull()
        (errorMessage.contains("timeout", ignoreCase = true) ||
                errorMessage.contains("timed out", ignoreCase = true)).shouldBeTrue()
        call.isCanceled().shouldBeTrue()
    }

    @Test
    fun `execute interruption aborts underlying request and restores interrupt status`() {
        conformanceServer.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))

        val call = callFactory(Duration.ofSeconds(5)).newCall(request())
        val started = CountDownLatch(1)
        val completed = CountDownLatch(1)
        val interruptedStatusRestored = AtomicBoolean(false)

        val worker = thread(start = true, isDaemon = true, name = "retrofit-call-interrupt-test") {
            try {
                started.countDown()
                call.execute()
            } catch (e: IOException) {
                interruptedStatusRestored.set(Thread.currentThread().isInterrupted)
            } finally {
                completed.countDown()
            }
        }

        started.await(1, TimeUnit.SECONDS).shouldBeTrue()
        Thread.sleep(100)
        worker.interrupt()

        completed.await(5, TimeUnit.SECONDS).shouldBeTrue()
        interruptedStatusRestored.get().shouldBeTrue()
        call.isCanceled().shouldBeTrue()
    }

    @Test
    fun `tag computeIfAbsent caches and returns same instance on repeated calls`() {
        val call = callFactory.newCall(request())

        data class MyTag(val value: String)

        val first = call.tag(MyTag::class) { MyTag("hello") }
        first.shouldNotBeNull()

        val second = call.tag(MyTag::class) { MyTag("world") }
        second shouldBeSameInstanceAs first
    }

    @Test
    fun `tag read returns null when tag has not been set`() {
        val call = callFactory.newCall(request())

        data class UnsetTag(val x: Int)

        call.tag(UnsetTag::class).shouldBeNull()
    }

    @Test
    fun `tag seeded from request is visible on call`() {
        data class RequestMeta(val id: Int)

        val meta = RequestMeta(42)
        val call = callFactory.newCall(request { tag(RequestMeta::class.java, meta) })

        call.tag(RequestMeta::class) shouldBeSameInstanceAs meta
    }

    @Test
    fun `등록한 event listener는 성공 호출의 시작과 종료를 받는다`() {
        conformanceServer.enqueue(MockResponse().setBody("ok"))

        val events = CopyOnWriteArrayList<String>()
        val call = callFactory.newCall(request())
        call.addEventListener(object: EventListener() {
            override fun callStart(call: Call) {
                events += "start"
            }

            override fun callEnd(call: Call) {
                events += "end"
            }
        })

        call.execute().close()

        events shouldBeEqualTo listOf("start", "end")
    }

    @Test
    fun `clone은 원본에 등록한 event listener를 상속하지 않는다`() {
        conformanceServer.enqueue(MockResponse().setBody("ok"))

        val eventCount = AtomicInteger()
        val original = callFactory.newCall(request())
        original.addEventListener(object: EventListener() {
            override fun callStart(call: Call) {
                eventCount.incrementAndGet()
            }
        })

        original.clone().execute().close()

        eventCount.get() shouldBeEqualTo 0
    }

    @Test
    fun `cancel event는 여러 번 취소해도 한 번만 전달된다`() {
        val cancelCount = AtomicInteger()
        val call = callFactory.newCall(request())
        call.addEventListener(object: EventListener() {
            override fun canceled(call: Call) {
                cancelCount.incrementAndGet()
            }
        })

        call.cancel()
        call.cancel()

        cancelCount.get() shouldBeEqualTo 1
    }

    private fun request(configure: Request.Builder.() -> Unit = {}): Request =
        Request.Builder()
            .url(conformanceServer.url("/"))
            .apply(configure)
            .build()

    private fun countingCallback(latch: CountDownLatch): okhttp3.Callback =
        object: okhttp3.Callback {
            override fun onFailure(call: Call, e: IOException) {
                latch.countDown()
            }

            override fun onResponse(call: Call, response: okhttp3.Response) {
                response.close()
                latch.countDown()
            }
        }
}
