package io.bluetape4k.workflow.core

import io.bluetape4k.workflow.api.AbstractWorkflowTest
import io.bluetape4k.workflow.api.ErrorStrategy
import io.bluetape4k.workflow.api.WorkReport
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

class WorkflowDslTest: AbstractWorkflowTest() {

    @Test
    fun `sequentialFlow DSL로 플로우 구성`() {
        val counter = AtomicInteger(0)

        val flow = sequentialFlow("test-sequential") {
            execute("step-1") { ctx -> counter.incrementAndGet(); WorkReport.success(ctx) }
            execute("step-2") { ctx -> counter.incrementAndGet(); WorkReport.success(ctx) }
            execute("step-3") { ctx -> counter.incrementAndGet(); WorkReport.success(ctx) }
            errorStrategy(ErrorStrategy.CONTINUE)
        }

        val report = flow.execute(context)

        report.isSuccess.shouldBeTrue()
        counter.get() shouldBeEqualTo 3
    }

    @Test
    fun `parallelFlow DSL`() {
        val counter = AtomicInteger(0)

        val flow = parallelFlow("test-parallel") {
            execute("p-1") { ctx -> counter.incrementAndGet(); WorkReport.success(ctx) }
            execute("p-2") { ctx -> counter.incrementAndGet(); WorkReport.success(ctx) }
            execute("p-3") { ctx -> counter.incrementAndGet(); WorkReport.success(ctx) }
        }

        val report = flow.execute(context)

        report.isSuccess.shouldBeTrue()
        counter.get() shouldBeEqualTo 3
    }

    @Test
    fun `conditionalFlow DSL - condition, then, otherwise`() {
        context["value"] = 10

        val flow = conditionalFlow("test-conditional") {
            condition { ctx -> (ctx.get<Int>("value") ?: 0) > 5 }
            then("big-value") { ctx ->
                ctx["result"] = "big"
                WorkReport.success(ctx)
            }
            otherwise("small-value") { ctx ->
                ctx["result"] = "small"
                WorkReport.success(ctx)
            }
        }

        val report = flow.execute(context)

        report.isSuccess.shouldBeTrue()
        context.get<String>("result") shouldBeEqualTo "big"
    }

    @Test
    fun `repeatFlow DSL - repeatWhile, until, maxIterations`() {
        val counter = AtomicInteger(0)

        val flow = repeatFlow("test-repeat") {
            execute("count-step") { ctx ->
                val count = counter.incrementAndGet()
                ctx["count"] = count
                WorkReport.success(ctx)
            }
            until { report -> (report.context.get<Int>("count") ?: 0) >= 3 }
            maxIterations(10)
        }

        val report = flow.execute(context)

        report.isSuccess.shouldBeTrue()
        counter.get() shouldBeEqualTo 3
    }

    @Test
    fun `retryFlow DSL - policy 인라인`() {
        val counter = AtomicInteger(0)

        val flow = retryFlow("test-retry") {
            execute("flaky-work") { ctx ->
                val attempt = counter.incrementAndGet()
                if (attempt < 2) WorkReport.failure(ctx, RuntimeException("attempt $attempt"))
                else WorkReport.success(ctx)
            }
            policy {
                maxAttempts = 5
                delay = 0.milliseconds
                backoffMultiplier = 1.0
            }
        }

        val report = flow.execute(context)

        report.isSuccess.shouldBeTrue()
        counter.get() shouldBeEqualTo 2
    }

    @Test
    fun `workflow 최상위 DSL - sequential 중첩`() {
        val counter = AtomicInteger(0)

        val root = workflow("test-workflow") {
            sequential("main-seq") {
                execute("w-1") { ctx -> counter.incrementAndGet(); WorkReport.success(ctx) }
                parallel("inner-parallel") {
                    execute("p-1") { ctx -> counter.incrementAndGet(); WorkReport.success(ctx) }
                    execute("p-2") { ctx -> counter.incrementAndGet(); WorkReport.success(ctx) }
                }
                execute("w-2") { ctx -> counter.incrementAndGet(); WorkReport.success(ctx) }
            }
        }

        val report = root.execute(context)

        report.isSuccess.shouldBeTrue()
        counter.get() shouldBeEqualTo 4
    }

    @Test
    fun `workflow 최상위 DSL - parallel 루트`() {
        val counter = AtomicInteger(0)

        val root = workflow("parallel-workflow") {
            parallel("root-parallel") {
                execute("p-1") { ctx -> counter.incrementAndGet(); WorkReport.success(ctx) }
                execute("p-2") { ctx -> counter.incrementAndGet(); WorkReport.success(ctx) }
            }
        }

        val report = root.execute(context)

        report.isSuccess.shouldBeTrue()
        counter.get() shouldBeEqualTo 2
    }

    @Test
    fun `루트 중복 선언 시 IllegalArgumentException`() {
        assertThrows<IllegalArgumentException> {
            workflow("duplicate-root") {
                sequential { execute("s-1") { ctx -> WorkReport.success(ctx) } }
                parallel { execute("p-1") { ctx -> WorkReport.success(ctx) } }
            }
        }
    }

    @Test
    fun `condition 없이 conditionalFlow - requireNotNull 예외`() {
        assertThrows<IllegalArgumentException> {
            conditionalFlow("no-condition") {
                // condition {} 없음
                then("then-work") { ctx -> WorkReport.success(ctx) }
            }
        }
    }

    @Test
    fun `then 없이 conditionalFlow - requireNotNull 예외`() {
        assertThrows<IllegalArgumentException> {
            conditionalFlow("no-then") {
                condition { true }
                // then {} 없음
            }
        }
    }

    @Test
    fun `sequentialFlow 안에 conditional 중첩`() {
        context["flag"] = false

        val counter = AtomicInteger(0)
        val flow = sequentialFlow("seq-with-conditional") {
            execute("before") { ctx -> counter.incrementAndGet(); WorkReport.success(ctx) }
            conditional("branch") {
                condition { ctx -> ctx.get<Boolean>("flag") == true }
                then("true-branch") { ctx -> counter.incrementAndGet(); WorkReport.success(ctx) }
                otherwise("false-branch") { ctx ->
                    ctx["branch"] = "false"
                    WorkReport.success(ctx)
                }
            }
            execute("after") { ctx -> counter.incrementAndGet(); WorkReport.success(ctx) }
        }

        val report = flow.execute(context)

        report.isSuccess.shouldBeTrue()
        counter.get() shouldBeEqualTo 2
        context.get<String>("branch") shouldBeEqualTo "false"
    }

    @Test
    fun `sequentialFlow 안에 retry 중첩`() {
        val counter = AtomicInteger(0)

        val flow = sequentialFlow("seq-with-retry") {
            execute("before") { ctx -> WorkReport.success(ctx) }
            retry("inner-retry") {
                execute("flaky") { ctx ->
                    val attempt = counter.incrementAndGet()
                    if (attempt < 3) WorkReport.failure(ctx, RuntimeException("fail $attempt"))
                    else WorkReport.success(ctx)
                }
                policy {
                    maxAttempts = 5
                    delay = 0.milliseconds
                }
            }
            execute("after") { ctx -> WorkReport.success(ctx) }
        }

        val report = flow.execute(context)

        report.isSuccess.shouldBeTrue()
        counter.get() shouldBeEqualTo 3
    }

    @Test
    fun `repeatFlow DSL - repeatWhile 조건 사용`() {
        val counter = AtomicInteger(0)

        val flow = repeatFlow("test-repeat-while") {
            execute("step") { ctx ->
                val count = counter.incrementAndGet()
                ctx["count"] = count
                WorkReport.success(ctx)
            }
            repeatWhile { report -> (report.context.get<Int>("count") ?: 0) < 5 }
            maxIterations(10)
        }

        val report = flow.execute(context)

        report.isSuccess.shouldBeTrue()
        counter.get() shouldBeEqualTo 5
    }

    // ──────────────────────────────────────────────────
    // parallelAllFlow / parallelAnyFlow DSL 테스트
    // ──────────────────────────────────────────────────

    @Test
    fun `parallelAllFlow DSL - policy=ALL 생성 확인`() {
        val counter = AtomicInteger(0)

        val flow = parallelAllFlow("test-parallel-all") {
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
    fun `parallelAnyFlow DSL - policy=ANY 생성 확인`() {
        val counter = AtomicInteger(0)

        val flow = parallelAnyFlow("test-parallel-any") {
            execute("p-1") { ctx -> counter.incrementAndGet(); WorkReport.success(ctx) }
            execute("p-2") { ctx -> counter.incrementAndGet(); WorkReport.success(ctx) }
        }

        val report = flow.execute(context)

        report.isSuccess.shouldBeTrue()
        report shouldBeInstanceOf WorkReport.Success::class
    }

    @Test
    fun `parallelAnyFlow DSL - 모두 실패 시 Failure 반환`() {
        val flow = parallelAnyFlow("test-parallel-any-fail") {
            execute("fail-1") { ctx -> WorkReport.failure(ctx, RuntimeException("실패1")) }
            execute("fail-2") { ctx -> WorkReport.failure(ctx, RuntimeException("실패2")) }
        }

        val report = flow.execute(context)

        report shouldBeInstanceOf WorkReport.Failure::class
    }

    @Test
    fun `workflow DSL - parallelAll 중첩`() {
        val counter = AtomicInteger(0)

        val root = workflow("workflow-parallel-all") {
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
    fun `workflow DSL - parallelAny 중첩`() {
        val root = workflow("workflow-parallel-any") {
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
    fun `sequential DSL - parallelAll과 parallelAny 혼재`() {
        val allCounter = AtomicInteger(0)

        val flow = sequentialFlow("seq-mixed-parallel") {
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
