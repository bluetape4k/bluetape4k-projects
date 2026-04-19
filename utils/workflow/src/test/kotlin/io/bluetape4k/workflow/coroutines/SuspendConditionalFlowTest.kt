package io.bluetape4k.workflow.coroutines

import io.bluetape4k.workflow.api.AbstractWorkflowTest
import io.bluetape4k.workflow.api.WorkContext
import io.bluetape4k.workflow.api.WorkReport
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class SuspendConditionalFlowTest: AbstractWorkflowTest() {

    @Test
    fun `predicate true - thenWork 실행`() = runTest {
        val flow = SuspendConditionalFlow(
            predicate = { _ -> true },
            thenWork = successSuspendWork("then-work"),
            otherwiseWork = failSuspendWork("otherwise-work"),
        )

        val report = flow.execute(context)

        report.isSuccess.shouldBeTrue()
        report shouldBeInstanceOf WorkReport.Success::class
    }

    @Test
    fun `predicate false - otherwiseWork 실행`() = runTest {
        val flow = SuspendConditionalFlow(
            predicate = { _ -> false },
            thenWork = successSuspendWork("then-work"),
            otherwiseWork = failSuspendWork("otherwise-work"),
        )

        flow.execute(context) shouldBeInstanceOf WorkReport.Failure::class
    }

    @Test
    fun `predicate false otherwiseWork null - Success 반환`() = runTest {
        val flow = SuspendConditionalFlow(
            predicate = { _ -> false },
            thenWork = failSuspendWork("then-work"),
            otherwiseWork = null,
        )

        val report = flow.execute(context)

        report.isSuccess.shouldBeTrue()
        report shouldBeInstanceOf WorkReport.Success::class
    }

    @Test
    fun `suspend predicate - 컨텍스트 값 기반 분기`() = runTest {
        val ctx = WorkContext()
        ctx["enabled"] = true

        val flow = SuspendConditionalFlow(
            predicate = { c -> c.get<Boolean>("enabled") == true },
            thenWork = successSuspendWork("then-work"),
            otherwiseWork = failSuspendWork("otherwise-work"),
        )

        flow.execute(ctx).isSuccess.shouldBeTrue()
    }

    @Test
    fun `suspend predicate false - otherwiseWork 실행`() = runTest {
        val ctx = WorkContext()
        ctx["enabled"] = false

        val flow = SuspendConditionalFlow(
            predicate = { c -> c.get<Boolean>("enabled") == true },
            thenWork = successSuspendWork("then-work"),
            otherwiseWork = failSuspendWork("otherwise-work"),
        )

        flow.execute(ctx) shouldBeInstanceOf WorkReport.Failure::class
    }

    @Test
    fun `predicate 예외 발생 - Failure 반환`() = runTest {
        val flow = SuspendConditionalFlow(
            predicate = { _ -> throw RuntimeException("predicate 예외") },
            thenWork = successSuspendWork(),
        )

        flow.execute(context) shouldBeInstanceOf WorkReport.Failure::class
    }

    @Test
    fun `thenWork aborted - Aborted 반환`() = runTest {
        val flow = SuspendConditionalFlow(
            predicate = { _ -> true },
            thenWork = abortSuspendWork("then-abort"),
        )

        flow.execute(context) shouldBeInstanceOf WorkReport.Aborted::class
    }

    @Test
    fun `otherwiseWork aborted - Aborted 반환`() = runTest {
        val flow = SuspendConditionalFlow(
            predicate = { _ -> false },
            thenWork = successSuspendWork(),
            otherwiseWork = abortSuspendWork("otherwise-abort"),
        )

        flow.execute(context) shouldBeInstanceOf WorkReport.Aborted::class
    }
}
