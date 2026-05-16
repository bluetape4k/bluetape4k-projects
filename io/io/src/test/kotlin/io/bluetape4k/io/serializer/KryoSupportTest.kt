package io.bluetape4k.io.serializer

import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.CancellationException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

class KryoSupportTest {

    companion object : KLogging()

    // ── withKryo ──────────────────────────────────────────────────────────────

    @Test
    fun `withKryo - 정상 실행 후 Kryo 인스턴스가 풀에 반환된다`() {
        val result = withKryo {
            val output = Output(256)
            writeClassAndObject(output, "hello-kryo")
            output.toBytes()
        }
        result.shouldNotBeNull()

        // Verify: subsequent call still works (pool was properly returned)
        val text = withKryo {
            val input = Input(result)
            readClassAndObject(input) as String
        }
        text shouldBeEqualTo "hello-kryo"
    }

    @Test
    fun `withKryo - 예외 발생 시에도 Kryo 인스턴스가 풀에 반환된다`() {
        repeat(10) {
            runCatching {
                withKryo<Unit> { throw RuntimeException("test error $it") }
            }
        }

        // Pool not exhausted — normal call still works
        val result = withKryo {
            val output = Output(256)
            writeClassAndObject(output, 42L)
            output.toBytes()
        }
        result.shouldNotBeNull()
    }

    // ── withKryoOutput ────────────────────────────────────────────────────────

    @Test
    fun `withKryoOutput - 정상 실행 후 Output 인스턴스가 풀에 반환된다`() {
        val bytes = withKryoOutput { output ->
            output.reset()
            withKryo { writeObject(output, "output-test") }
            output.toBytes()
        }
        bytes.shouldNotBeNull()

        // Verify: second call still works
        val bytes2 = withKryoOutput { output ->
            output.reset()
            withKryo { writeObject(output, "output-test-2") }
            output.toBytes()
        }
        bytes2.shouldNotBeNull()
    }

    @Test
    fun `withKryoOutput - 예외 발생 시에도 Output 인스턴스가 풀에 반환된다`() {
        repeat(10) {
            runCatching {
                withKryoOutput<Unit> { throw RuntimeException("output error $it") }
            }
        }

        // Pool not exhausted
        val bytes = withKryoOutput { output ->
            output.reset()
            withKryo { writeObject(output, "after-error") }
            output.toBytes()
        }
        bytes.shouldNotBeNull()
    }

    // ── withKryoInput ─────────────────────────────────────────────────────────

    @Test
    fun `withKryoInput - 정상 실행 후 Input 인스턴스가 풀에 반환된다`() {
        val bytes = withKryoOutput { output ->
            output.reset()
            withKryo { writeObject(output, "input-test") }
            output.toBytes()
        }

        val text = withKryoInput { input ->
            input.setBuffer(bytes)
            withKryo { readObject(input, String::class.java) }
        }
        text shouldBeEqualTo "input-test"
    }

    @Test
    fun `withKryoInput - 예외 발생 시에도 Input 인스턴스가 풀에 반환된다`() {
        repeat(10) {
            runCatching {
                withKryoInput<Unit> { throw RuntimeException("input error $it") }
            }
        }

        // Pool not exhausted
        val bytes = withKryoOutput { output ->
            output.reset()
            withKryo { writeObject(output, "after-input-error") }
            output.toBytes()
        }
        val text = withKryoInput { input ->
            input.setBuffer(bytes)
            withKryo { readObject(input, String::class.java) }
        }
        text shouldBeEqualTo "after-input-error"
    }

    // ── withKryoAsync ─────────────────────────────────────────────────────────

    @Test
    fun `withKryoAsync - 정상 실행 시 올바른 결과를 반환한다`() {
        val future = withKryoAsync {
            val output = Output(256)
            writeClassAndObject(output, "async-kryo")
            output.toBytes()
        }

        val bytes = checkNotNull(future.get(5, TimeUnit.SECONDS)) { "withKryoAsync returned null" }

        val text = withKryo {
            val input = Input(bytes)
            readClassAndObject(input) as String
        }
        text shouldBeEqualTo "async-kryo"
    }

    @Test
    fun `withKryoAsync - null 반환도 허용된다`() {
        val future = withKryoAsync<String> { null }
        val result = future.get(5, TimeUnit.SECONDS)
        result.shouldBeNull()
    }

    @Test
    fun `withKryoAsync - func 예외 발생 시 Kryo가 풀에 반환되고 이후 호출이 정상 동작한다`() {
        // Trigger exception path many times
        repeat(20) {
            val future = withKryoAsync<String> { throw RuntimeException("async error $it") }
            assertThrows<ExecutionException> { future.get(5, TimeUnit.SECONDS) }
        }

        // Pool must not be exhausted — new call still works
        val future = withKryoAsync {
            val output = Output(256)
            writeClassAndObject(output, "after-async-error")
            output.toBytes()
        }
        val bytes = future.get(5, TimeUnit.SECONDS)
        bytes.shouldNotBeNull()
    }

    @Test
    fun `withKryoAsync - Future 취소 시 Kryo가 풀에 반환되고 이후 호출이 정상 동작한다`() {
        val taskStarted = CountDownLatch(1)
        val releaseTask = CountDownLatch(1)
        val funcCompleted = CountDownLatch(1)  // signals user-func finally ran

        val future = withKryoAsync {
            taskStarted.countDown()
            try {
                releaseTask.await(10, TimeUnit.SECONDS)
                "should-not-complete"
            } finally {
                // Framework's releaseKryo() runs immediately after this
                funcCompleted.countDown()
            }
        }

        // Wait until supplier has started and obtained Kryo
        taskStarted.await(5, TimeUnit.SECONDS)

        // Cancel with interrupt — the finally block inside supplyAsync still runs
        future.cancel(true)

        // Unblock the supplier so it can reach the finally block
        releaseTask.countDown()

        // Wait for user-func's finally to complete (framework's releaseKryo follows immediately after)
        funcCompleted.await(5, TimeUnit.SECONDS)

        assertThrows<CancellationException> { future.get() }

        // Kryo must have been released — subsequent calls still work
        repeat(5) { i ->
            val next = withKryoAsync {
                val output = Output(256)
                writeClassAndObject(output, "post-cancel-$i")
                output.toBytes()
            }
            val bytes = next.get(5, TimeUnit.SECONDS)
            bytes.shouldNotBeNull()
        }
    }

    // ── withKryoSuspending ────────────────────────────────────────────────────

    @Test
    fun `withKryoSuspending - 정상 실행 시 올바른 결과를 반환한다`() = runTest {
        val bytes = checkNotNull(withKryoSuspending {
            val output = Output(256)
            writeClassAndObject(output, "suspend-kryo")
            output.toBytes()
        }) { "withKryoSuspending returned null" }

        val text = withKryoSuspending {
            val input = Input(bytes)
            readClassAndObject(input) as String
        }
        text shouldBeEqualTo "suspend-kryo"
    }

    @Test
    fun `withKryoSuspending - 예외 발생 시에도 Kryo가 풀에 반환된다`() = runTest {
        repeat(10) {
            runCatching {
                withKryoSuspending<Unit> { throw RuntimeException("suspend error $it") }
            }
        }

        // Pool not exhausted
        val result = withKryoSuspending {
            val output = Output(256)
            writeClassAndObject(output, "after-suspend-error")
            output.toBytes()
        }
        result.shouldNotBeNull()
    }
}
