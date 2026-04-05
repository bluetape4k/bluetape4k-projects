package io.bluetape4k.workflow.api

import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldThrow
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.time.Duration.Companion.milliseconds

class WorkAdapterTest : AbstractWorkflowTest() {

    @Test
    fun `Work SAM 변환`() {
        val w: Work = Work { ctx -> WorkReport.success(ctx) }
        val report = w.execute(context)
        report.isSuccess.shouldBeTrue()
        report.context shouldBeEqualTo context
    }

    @Test
    fun `SuspendWork SAM 변환`() {
        val sw: SuspendWork = SuspendWork { ctx -> WorkReport.success(ctx) }
        val report = runBlocking { sw.execute(context) }
        report.isSuccess.shouldBeTrue()
        report.context shouldBeEqualTo context
    }

    @Test
    fun `Work asSuspend 정상 변환`() {
        val work = Work { ctx -> WorkReport.success(ctx) }
        val suspendWork = work.asSuspend()

        val report = runBlocking { suspendWork.execute(context) }
        report.isSuccess.shouldBeTrue()
        report.context shouldBeEqualTo context
    }

    @Test
    fun `Work asSuspend 결과가 원본 Work와 동일`() {
        val ctx = WorkContext()
        ctx["key"] = "value"

        val work = Work { c ->
            val v: String? = c["key"]
            c["result"] = "processed-$v"
            WorkReport.success(c)
        }

        val suspendWork = work.asSuspend()
        val report = runBlocking { suspendWork.execute(ctx) }

        report.isSuccess.shouldBeTrue()
        val result: String? = report.context["result"]
        result shouldBeEqualTo "processed-value"
    }

    @Test
    fun `SuspendWork asBlocking 정상 변환`() {
        val suspendWork = SuspendWork { ctx -> WorkReport.success(ctx) }
        val blockingWork = suspendWork.asBlocking()

        val report = blockingWork.execute(context)
        report.isSuccess.shouldBeTrue()
        report.context shouldBeEqualTo context
    }

    @Test
    fun `SuspendWork asBlocking 결과가 원본 SuspendWork와 동일`() {
        val ctx = WorkContext()
        ctx["input"] = 42

        val suspendWork = SuspendWork { c ->
            val input: Int? = c["input"]
            c["output"] = (input ?: 0) * 2
            WorkReport.success(c)
        }

        val blockingWork = suspendWork.asBlocking()
        val report = blockingWork.execute(ctx)

        report.isSuccess.shouldBeTrue()
        val output: Int? = report.context["output"]
        output shouldBeEqualTo 84
    }

    @Test
    fun `NamedWork 이름 확인`() {
        val work = Work("my-named-work") { ctx -> WorkReport.success(ctx) }

        work.shouldBeInstanceOf<NamedWork>()
        work.name shouldBeEqualTo "my-named-work"
    }

    @Test
    fun `NamedWork toString 형식 확인`() {
        val work = NamedWork("test-work", Work { ctx -> WorkReport.success(ctx) })
        work.toString() shouldBeEqualTo "NamedWork(test-work)"
    }

    @Test
    fun `NamedWork execute 위임 동작`() {
        val work = Work("delegate-work") { ctx ->
            ctx["executed"] = true
            WorkReport.success(ctx)
        }

        val ctx = WorkContext()
        work.execute(ctx)

        val executed: Boolean? = ctx["executed"]
        executed shouldBeEqualTo true
    }

    @Test
    fun `NamedSuspendWork 이름 확인`() {
        val sw = SuspendWork("my-suspend-work") { ctx -> WorkReport.success(ctx) }

        sw.shouldBeInstanceOf<NamedSuspendWork>()
        sw.name shouldBeEqualTo "my-suspend-work"
    }

    @Test
    fun `NamedSuspendWork toString 형식 확인`() {
        val sw = NamedSuspendWork("test-suspend", SuspendWork { ctx -> WorkReport.success(ctx) })
        sw.toString() shouldBeEqualTo "NamedSuspendWork(test-suspend)"
    }

    @Test
    fun `NamedSuspendWork execute 위임 동작`() {
        val sw = SuspendWork("delegate-suspend") { ctx ->
            ctx["asyncExecuted"] = true
            WorkReport.success(ctx)
        }

        val ctx = WorkContext()
        runBlocking { sw.execute(ctx) }

        val executed: Boolean? = ctx["asyncExecuted"]
        executed shouldBeEqualTo true
    }

    @Test
    fun `RetryPolicy maxRetries 편의 프로퍼티`() {
        RetryPolicy(maxAttempts = 3).maxRetries shouldBeEqualTo 2
        RetryPolicy(maxAttempts = 1).maxRetries shouldBeEqualTo 0
        RetryPolicy(maxAttempts = 5).maxRetries shouldBeEqualTo 4
    }

    @Test
    fun `RetryPolicy maxAttempts 1 미만이면 예외`() {
        assertThrows<IllegalArgumentException> {
            RetryPolicy(maxAttempts = 0)
        }
        assertThrows<IllegalArgumentException> {
            RetryPolicy(maxAttempts = -1)
        }
    }

    @Test
    fun `RetryPolicy backoffMultiplier 1 미만이면 예외`() {
        assertThrows<IllegalArgumentException> {
            RetryPolicy(maxAttempts = 3, backoffMultiplier = 0.5)
        }
    }

    @Test
    fun `RetryPolicy 기본값 확인`() {
        val policy = RetryPolicy()

        policy.maxAttempts shouldBeEqualTo 1
        policy.maxRetries shouldBeEqualTo 0
        policy.delay shouldBeEqualTo kotlin.time.Duration.ZERO
        policy.backoffMultiplier shouldBeEqualTo 1.0
    }

    @Test
    fun `RetryPolicy NONE 상수 확인`() {
        RetryPolicy.NONE.maxAttempts shouldBeEqualTo 1
        RetryPolicy.NONE.maxRetries shouldBeEqualTo 0
    }

    @Test
    fun `RetryPolicy DEFAULT 상수 확인`() {
        val policy = RetryPolicy.DEFAULT

        policy.maxAttempts shouldBeEqualTo 3
        policy.maxRetries shouldBeEqualTo 2
        policy.delay shouldBeEqualTo 100.milliseconds
        policy.backoffMultiplier shouldBeEqualTo 2.0
    }
}
