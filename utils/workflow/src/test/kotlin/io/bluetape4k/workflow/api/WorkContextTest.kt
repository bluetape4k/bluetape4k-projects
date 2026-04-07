package io.bluetape4k.workflow.api

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class WorkContextTest: AbstractWorkflowTest() {

    private lateinit var ctx: WorkContext

    @BeforeEach
    fun setUp() {
        ctx = WorkContext()
    }

    @Test
    fun `기본 get set 동작`() {
        ctx["key"] = "value"
        val result: String? = ctx["key"]
        result shouldBeEqualTo "value"
    }

    @Test
    fun `타입 파라미터 get`() {
        ctx["count"] = 42
        ctx["name"] = "alice"
        ctx["flag"] = true

        val count: Int? = ctx["count"]
        val name: String? = ctx["name"]
        val flag: Boolean? = ctx["flag"]

        count shouldBeEqualTo 42
        name shouldBeEqualTo "alice"
        flag shouldBeEqualTo true
    }

    @Test
    fun `존재하지 않는 키는 null 반환`() {
        val result: String? = ctx["nonexistent"]
        result.shouldBeNull()
    }

    @Test
    fun `remove 및 contains`() {
        ctx["key"] = "value"
        ctx.contains("key").shouldBeTrue()

        ctx.remove("key")
        ctx.contains("key").shouldBeFalse()
    }

    @Test
    fun `remove 반환값 확인`() {
        ctx["key"] = "value"
        val removed = ctx.remove("key")
        removed shouldBeEqualTo "value"

        val removedAgain = ctx.remove("key")
        removedAgain.shouldBeNull()
    }

    @Test
    fun `compute 원자적 갱신`() {
        ctx["counter"] = 0
        ctx.compute("counter") { _, old -> ((old as? Int) ?: 0) + 1 }
        val result: Int? = ctx["counter"]
        result shouldBeEqualTo 1
    }

    @Test
    fun `compute 키 없을 때 초기값 생성`() {
        ctx.compute("counter") { _, old -> ((old as? Int) ?: 0) + 1 }
        val result: Int? = ctx["counter"]
        result shouldBeEqualTo 1
    }

    @Test
    fun `compute null 반환 시 키 제거`() {
        ctx["key"] = "value"
        ctx.compute("key") { _, _ -> null }
        ctx.contains("key").shouldBeFalse()
    }

    @Test
    fun `snapshot 불변성`() {
        ctx["a"] = 1
        ctx["b"] = 2

        val snap = ctx.snapshot()
        snap.containsKey("a").shouldBeTrue()
        snap.containsKey("b").shouldBeTrue()

        // 원본 변경 후 snapshot에 영향 없음
        ctx["c"] = 3
        ctx.remove("a")

        snap.containsKey("a").shouldBeTrue()
        snap.containsKey("c").shouldBeFalse()
    }

    @Test
    fun `merge 동작`() {
        val base = WorkContext()
        base["x"] = 10
        base["y"] = 20

        val other = WorkContext()
        other["y"] = 99
        other["z"] = 30

        base.merge(other)

        val x: Int? = base["x"]
        val y: Int? = base["y"]
        val z: Int? = base["z"]

        x shouldBeEqualTo 10
        y shouldBeEqualTo 99   // other의 값이 우선
        z shouldBeEqualTo 30
    }

    @Test
    fun `병렬 compute 스레드 안전성`() {
        val threadCount = 10
        val incrementsPerThread = 100
        val latch = CountDownLatch(threadCount)
        val executor = Executors.newFixedThreadPool(threadCount)

        ctx["counter"] = 0

        repeat(threadCount) {
            executor.submit {
                repeat(incrementsPerThread) {
                    ctx.compute("counter") { _, old -> ((old as? Int) ?: 0) + 1 }
                }
                latch.countDown()
            }
        }

        latch.await(5, TimeUnit.SECONDS)
        executor.shutdown()

        val finalCount: Int? = ctx["counter"]
        finalCount shouldBeEqualTo threadCount * incrementsPerThread
    }
}
