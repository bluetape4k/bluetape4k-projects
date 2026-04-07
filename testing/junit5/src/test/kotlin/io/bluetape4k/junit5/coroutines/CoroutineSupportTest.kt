package io.bluetape4k.junit5.coroutines

import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContain
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

/**
 * [runSuspendTest], [runSuspendIO], [runSuspendDefault], [runSuspendVT] 함수 검증 테스트
 */
class CoroutineSupportTest {
    companion object: KLogging()

    @Test
    fun `runSuspendTest - suspend 블록을 동기 실행한다`() {
        var executed = false
        runSuspendTest { executed = true }
        executed.shouldBeTrue()
    }

    @Test
    fun `runSuspendTest - 블록 예외는 호출자에게 전파된다`() {
        assertFailsWith<IllegalStateException> {
            runSuspendTest { throw IllegalStateException("boom") }
        }
    }

    @Test
    fun `runSuspendTest - context 파라미터가 적용된다`() {
        val threadNames = mutableListOf<String>()
        runSuspendTest(Dispatchers.IO) {
            threadNames += Thread.currentThread().name
        }
        threadNames.isNotEmpty().shouldBeTrue()
    }

    @Test
    fun `runSuspendIO - IO Dispatcher 에서 실행된다`() {
        val threadName = StringBuilder()
        runSuspendIO {
            threadName.append(Thread.currentThread().name)
        }
        threadName.toString().shouldContain("DefaultDispatcher-worker")
    }

    @Test
    fun `runSuspendDefault - Default Dispatcher 에서 실행된다`() {
        val threadName = StringBuilder()
        runSuspendDefault {
            threadName.append(Thread.currentThread().name)
        }
        threadName.toString().shouldContain("DefaultDispatcher-worker")
    }

    @Test
    fun `runSuspendVT - Virtual Thread 에서 실행된다`() {
        var isVirtual = false
        runSuspendVT {
            isVirtual = Thread.currentThread().isVirtual
        }
        isVirtual.shouldBeTrue()
    }

    @Test
    fun `runSuspendTest - delay 를 포함한 suspend 블록을 실행한다`() {
        var count = 0
        runSuspendTest {
            delay(10)
            count++
            delay(10)
            count++
        }
        count shouldBeEqualTo 2
    }

    @Test
    fun `runSuspendIO - 반환값이 올바르게 캡처된다`() {
        val results = mutableListOf<Int>()
        runSuspendIO {
            val v = withContext(Dispatchers.IO) { 42 }
            results += v
        }
        results shouldBeEqualTo listOf(42)
    }
}
