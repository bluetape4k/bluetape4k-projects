package io.bluetape4k.workflow.coroutines

import io.bluetape4k.workflow.api.AbstractWorkflowTest
import io.bluetape4k.workflow.api.ErrorStrategy
import io.bluetape4k.workflow.api.WorkContext
import io.bluetape4k.workflow.api.WorkReport
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

class SuspendWorkflowDslTest: AbstractWorkflowTest() {

    // ──────────────────────────────────────────────────
    // suspendSequentialFlow DSL
    // ──────────────────────────────────────────────────

    @Test
    fun `suspendSequentialFlow DSL - 전체 성공`() = runTest {
        val counter = AtomicInteger(0)
        val flow = suspendSequentialFlow("test-seq") {
            execute("work-1") { ctx -> counter.incrementAndGet(); WorkReport.success(ctx) }
            execute("work-2") { ctx -> counter.incrementAndGet(); WorkReport.success(ctx) }
            execute("work-3") { ctx -> counter.incrementAndGet(); WorkReport.success(ctx) }
        }

        val report = flow.execute(context)

        report.isSuccess.shouldBeTrue()
        counter.get() shouldBeEqualTo 3
    }

    @Test
    fun `suspendSequentialFlow DSL - CONTINUE 전략으로 PartialSuccess 반환`() = runTest {
        val flow = suspendSequentialFlow("test-seq") {
            execute("work-1") { ctx -> WorkReport.success(ctx) }
            execute("work-fail") { ctx -> WorkReport.failure(ctx, RuntimeException("실패")) }
            execute("work-3") { ctx -> WorkReport.success(ctx) }
            errorStrategy(ErrorStrategy.CONTINUE)
        }

        val report = flow.execute(context)

        report shouldBeInstanceOf WorkReport.PartialSuccess::class
        val partial = report as WorkReport.PartialSuccess
        partial.failedReports.size shouldBeEqualTo 1
    }

    @Test
    fun `suspendSequentialFlow DSL - 기존 SuspendWork 추가`() = runTest {
        val flow = suspendSequentialFlow("test-seq") {
            execute(successSuspendWork("work-1"))
            execute(successSuspendWork("work-2"))
        }

        val report = flow.execute(context)

        report.isSuccess.shouldBeTrue()
    }

    // ──────────────────────────────────────────────────
    // suspendParallelFlow DSL
    // ──────────────────────────────────────────────────

    @Test
    fun `suspendParallelFlow DSL - 전체 성공`() = runTest {
        val counter = AtomicInteger(0)
        val flow = suspendParallelFlow("test-parallel") {
            execute("work-1") { ctx -> counter.incrementAndGet(); WorkReport.success(ctx) }
            execute("work-2") { ctx -> counter.incrementAndGet(); WorkReport.success(ctx) }
        }

        val report = flow.execute(context)

        report.isSuccess.shouldBeTrue()
        counter.get() shouldBeEqualTo 2
    }

    @Test
    fun `suspendParallelFlow DSL - 일부 실패`() = runTest {
        val flow = suspendParallelFlow("test-parallel") {
            execute("work-success") { ctx -> WorkReport.success(ctx) }
            execute("work-fail") { ctx -> WorkReport.failure(ctx, RuntimeException("병렬 실패")) }
        }

        val report = flow.execute(context)

        report shouldBeInstanceOf WorkReport.Failure::class
    }

    // ──────────────────────────────────────────────────
    // suspendConditionalFlow DSL
    // ──────────────────────────────────────────────────

    @Test
    fun `suspendConditionalFlow DSL - predicate true`() = runTest {
        val flow = suspendConditionalFlow("test-cond") {
            condition { _ -> true }
            then("then-work") { ctx -> WorkReport.success(ctx) }
            otherwise("otherwise-work") { ctx -> WorkReport.failure(ctx) }
        }

        val report = flow.execute(context)

        report.isSuccess.shouldBeTrue()
    }

    @Test
    fun `suspendConditionalFlow DSL - suspend predicate false`() = runTest {
        val ctx = WorkContext()
        ctx["flag"] = false

        val flow = suspendConditionalFlow("test-cond") {
            condition { c -> c.get<Boolean>("flag") == true }
            then("then-work") { c -> WorkReport.success(c) }
            otherwise("otherwise-work") { c -> WorkReport.failure(c) }
        }

        val report = flow.execute(ctx)

        report shouldBeInstanceOf WorkReport.Failure::class
    }

    @Test
    fun `suspendConditionalFlow DSL - otherwise 없고 predicate false - Success 반환`() = runTest {
        val flow = suspendConditionalFlow("test-cond") {
            condition { _ -> false }
            then("then-work") { ctx -> WorkReport.failure(ctx) }
        }

        val report = flow.execute(context)

        report.isSuccess.shouldBeTrue()
    }

    // ──────────────────────────────────────────────────
    // suspendRepeatFlow DSL
    // ──────────────────────────────────────────────────

    @Test
    fun `suspendRepeatFlow DSL - maxIterations 적용`() = runTest {
        val counter = AtomicInteger(0)
        val flow = suspendRepeatFlow("test-repeat") {
            execute("count-work") { ctx -> counter.incrementAndGet(); WorkReport.success(ctx) }
            repeatWhile { it.isSuccess }
            maxIterations(4)
            repeatDelay(0.milliseconds)
        }

        val report = flow.execute(context)

        report.isSuccess.shouldBeTrue()
        counter.get() shouldBeEqualTo 4
    }

    @Test
    fun `suspendRepeatFlow DSL - until 조건`() = runTest {
        val ctx = WorkContext()
        ctx["count"] = 0

        val flow = suspendRepeatFlow("test-repeat-until") {
            execute("increment") { c ->
                val current = c.get<Int>("count") ?: 0
                c["count"] = current + 1
                WorkReport.success(c)
            }
            until { report -> (report.context.get<Int>("count") ?: 0) >= 3 }
            maxIterations(100)
            repeatDelay(0.milliseconds)
        }

        val report = flow.execute(ctx)

        report.isSuccess.shouldBeTrue()
        ctx.get<Int>("count") shouldBeEqualTo 3
    }

    // ──────────────────────────────────────────────────
    // suspendRetryFlow DSL
    // ──────────────────────────────────────────────────

    @Test
    fun `suspendRetryFlow DSL - 성공`() = runTest {
        val counter = AtomicInteger(0)
        val flow = suspendRetryFlow("test-retry") {
            execute("work") { ctx -> counter.incrementAndGet(); WorkReport.success(ctx) }
            policy {
                maxAttempts = 3
                delay = 0.milliseconds
            }
        }

        val report = flow.execute(context)

        report.isSuccess.shouldBeTrue()
        counter.get() shouldBeEqualTo 1
    }

    @Test
    fun `suspendRetryFlow DSL - N번 실패 후 성공`() = runTest {
        val counter = AtomicInteger(0)
        val flow = suspendRetryFlow("test-retry-flaky") {
            execute("flaky-work") { ctx ->
                val cnt = counter.incrementAndGet()
                if (cnt < 3) WorkReport.failure(ctx, RuntimeException("시도 #$cnt 실패"))
                else WorkReport.success(ctx)
            }
            policy {
                maxAttempts = 5
                delay = 0.milliseconds
            }
        }

        val report = flow.execute(context)

        report.isSuccess.shouldBeTrue()
        counter.get() shouldBeEqualTo 3
    }

    @Test
    fun `suspendRetryFlow DSL - maxAttempts 소진`() = runTest {
        val counter = AtomicInteger(0)
        val flow = suspendRetryFlow("test-retry-exhaust") {
            execute("fail-work") { ctx ->
                counter.incrementAndGet()
                WorkReport.failure(ctx, RuntimeException("항상 실패"))
            }
            policy {
                maxAttempts = 3
                delay = 0.milliseconds
            }
        }

        val report = flow.execute(context)

        report shouldBeInstanceOf WorkReport.Failure::class
        counter.get() shouldBeEqualTo 3
    }

    // ──────────────────────────────────────────────────
    // suspendWorkflow DSL (중첩 구성)
    // ──────────────────────────────────────────────────

    @Test
    fun `suspendWorkflow DSL - sequential 루트`() = runTest {
        val counter = AtomicInteger(0)
        val root = suspendWorkflow("test-workflow") {
            sequential("main") {
                execute("work-1") { ctx -> counter.incrementAndGet(); WorkReport.success(ctx) }
                execute("work-2") { ctx -> counter.incrementAndGet(); WorkReport.success(ctx) }
            }
        }

        val report = root.execute(context)

        report.isSuccess.shouldBeTrue()
        counter.get() shouldBeEqualTo 2
    }

    @Test
    fun `suspendWorkflow DSL - parallel 루트`() = runTest {
        val counter = AtomicInteger(0)
        val root = suspendWorkflow("test-workflow-parallel") {
            parallel("main") {
                execute("work-1") { ctx -> counter.incrementAndGet(); WorkReport.success(ctx) }
                execute("work-2") { ctx -> counter.incrementAndGet(); WorkReport.success(ctx) }
            }
        }

        val report = root.execute(context)

        report.isSuccess.shouldBeTrue()
        counter.get() shouldBeEqualTo 2
    }

    @Test
    fun `suspendWorkflow DSL - conditional 루트`() = runTest {
        val root = suspendWorkflow("test-workflow-cond") {
            conditional("main") {
                condition { _ -> true }
                then("then") { ctx -> WorkReport.success(ctx) }
            }
        }

        val report = root.execute(context)

        report.isSuccess.shouldBeTrue()
    }

    @Test
    fun `suspendWorkflow DSL - repeat 루트`() = runTest {
        val counter = AtomicInteger(0)
        val root = suspendWorkflow("test-workflow-repeat") {
            repeat("main") {
                execute("work") { ctx -> counter.incrementAndGet(); WorkReport.success(ctx) }
                maxIterations(3)
                repeatDelay(0.milliseconds)
            }
        }

        val report = root.execute(context)

        report.isSuccess.shouldBeTrue()
        counter.get() shouldBeEqualTo 3
    }

    @Test
    fun `suspendWorkflow DSL - sequential 안에 parallel 중첩`() = runTest {
        val seqCounter = AtomicInteger(0)
        val parallelCounter = AtomicInteger(0)

        val root = suspendWorkflow("nested-workflow") {
            sequential("main") {
                execute("seq-1") { ctx -> seqCounter.incrementAndGet(); WorkReport.success(ctx) }
                parallel("inner-parallel") {
                    execute("par-1") { ctx -> parallelCounter.incrementAndGet(); WorkReport.success(ctx) }
                    execute("par-2") { ctx -> parallelCounter.incrementAndGet(); WorkReport.success(ctx) }
                }
                execute("seq-2") { ctx -> seqCounter.incrementAndGet(); WorkReport.success(ctx) }
            }
        }

        val report = root.execute(context)

        report.isSuccess.shouldBeTrue()
        seqCounter.get() shouldBeEqualTo 2
        parallelCounter.get() shouldBeEqualTo 2
    }

    // ──────────────────────────────────────────────────
    // 루트 중복 선언 - IllegalArgumentException
    // ──────────────────────────────────────────────────

    @Test
    fun `suspendWorkflow DSL - 루트 중복 선언 시 IllegalArgumentException`() {
        assertThrows<IllegalArgumentException> {
            suspendWorkflow("test-dup") {
                sequential("seq1") {
                    execute("work") { ctx -> WorkReport.success(ctx) }
                }
                sequential("seq2") {
                    execute("work") { ctx -> WorkReport.success(ctx) }
                }
            }
        }
    }

    @Test
    fun `suspendWorkflow DSL - parallel 루트 중복 선언 시 IllegalArgumentException`() {
        assertThrows<IllegalArgumentException> {
            suspendWorkflow("test-dup-parallel") {
                parallel("par1") {
                    execute("work") { ctx -> WorkReport.success(ctx) }
                }
                parallel("par2") {
                    execute("work") { ctx -> WorkReport.success(ctx) }
                }
            }
        }
    }

    // ──────────────────────────────────────────────────
    // suspendParallelAllFlow / suspendParallelAnyFlow DSL 테스트
    // ──────────────────────────────────────────────────

    @Test
    fun `suspendParallelAllFlow DSL - policy=ALL 생성 확인`() = runTest {
        val counter = AtomicInteger(0)

        val flow = suspendParallelAllFlow("test-suspend-parallel-all") {
            execute("p-1") { ctx -> counter.incrementAndGet(); WorkReport.success(ctx) }
            execute("p-2") { ctx -> counter.incrementAndGet(); WorkReport.success(ctx) }
            execute("p-3") { ctx -> counter.incrementAndGet(); WorkReport.success(ctx) }
        }

        val report = flow.execute(context)

        report.isSuccess.shouldBeTrue()
        report shouldBeInstanceOf WorkReport.Success::class
        counter.get() shouldBeEqualTo 3
    }

    @Test
    fun `suspendParallelAnyFlow DSL - policy=ANY 생성 확인`() = runTest {
        val counter = AtomicInteger(0)

        val flow = suspendParallelAnyFlow("test-suspend-parallel-any") {
            execute("p-1") { ctx -> counter.incrementAndGet(); WorkReport.success(ctx) }
            execute("p-2") { ctx -> counter.incrementAndGet(); WorkReport.success(ctx) }
        }

        val report = flow.execute(context)

        report.isSuccess.shouldBeTrue()
        report shouldBeInstanceOf WorkReport.Success::class
    }

    @Test
    fun `suspendParallelAnyFlow DSL - 모두 실패 시 Failure 반환`() = runTest {
        val flow = suspendParallelAnyFlow("test-suspend-parallel-any-fail") {
            execute("fail-1") { ctx -> WorkReport.failure(ctx, RuntimeException("실패1")) }
            execute("fail-2") { ctx -> WorkReport.failure(ctx, RuntimeException("실패2")) }
        }

        val report = flow.execute(context)

        report shouldBeInstanceOf WorkReport.Failure::class
    }

    @Test
    fun `suspendWorkflow DSL - parallelAll 루트`() = runTest {
        val counter = AtomicInteger(0)

        val root = suspendWorkflow("workflow-suspend-parallel-all") {
            parallelAll("inner-all") {
                execute("w-1") { ctx -> counter.incrementAndGet(); WorkReport.success(ctx) }
                execute("w-2") { ctx -> counter.incrementAndGet(); WorkReport.success(ctx) }
            }
        }

        val report = root.execute(context)

        report.isSuccess.shouldBeTrue()
        counter.get() shouldBeEqualTo 2
    }

    @Test
    fun `suspendWorkflow DSL - parallelAny 루트`() = runTest {
        val root = suspendWorkflow("workflow-suspend-parallel-any") {
            parallelAny("inner-any") {
                execute("w-1") { ctx -> WorkReport.success(ctx) }
                execute("w-2") { ctx -> WorkReport.success(ctx) }
            }
        }

        val report = root.execute(context)

        report.isSuccess.shouldBeTrue()
        report shouldBeInstanceOf WorkReport.Success::class
    }

    @Test
    fun `suspendSequentialFlow DSL - parallelAll과 parallelAny 혼재`() = runTest {
        val allCounter = AtomicInteger(0)

        val flow = suspendSequentialFlow("seq-mixed-parallel") {
            execute("before") { ctx -> WorkReport.success(ctx) }
            parallelAll("fan-out-all") {
                execute("all-1") { ctx -> allCounter.incrementAndGet(); WorkReport.success(ctx) }
                execute("all-2") { ctx -> allCounter.incrementAndGet(); WorkReport.success(ctx) }
            }
            parallelAny("fan-out-any") {
                execute("any-1") { ctx -> WorkReport.success(ctx) }
                execute("any-2") { ctx -> WorkReport.success(ctx) }
            }
            execute("after") { ctx -> WorkReport.success(ctx) }
        }

        val report = flow.execute(context)

        report.isSuccess.shouldBeTrue()
        allCounter.get() shouldBeEqualTo 2
    }
}
