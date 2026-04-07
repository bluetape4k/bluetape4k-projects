package io.bluetape4k.workflow.core

import io.bluetape4k.workflow.api.AbstractWorkflowTest
import io.bluetape4k.workflow.api.WorkReport
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class ConditionalWorkFlowTest: AbstractWorkflowTest() {

    @Test
    fun `predicate true - thenWork 실행`() {
        val flow = ConditionalWorkFlow(
            predicate = { true },
            thenWork = successWork("then-work"),
            otherwiseWork = failWork("otherwise-work"),
        )

        val report = flow.execute(context)

        report.isSuccess.shouldBeTrue()
        report shouldBeInstanceOf WorkReport.Success::class
    }

    @Test
    fun `predicate false, otherwiseWork 있음 - otherwiseWork 실행`() {
        val flow = ConditionalWorkFlow(
            predicate = { false },
            thenWork = successWork("then-work"),
            otherwiseWork = failWork("otherwise-work", "otherwise 실행됨"),
        )

        val report = flow.execute(context)

        report shouldBeInstanceOf WorkReport.Failure::class
    }

    @Test
    fun `predicate false, otherwiseWork null - Success 반환 (no-op)`() {
        val flow = ConditionalWorkFlow(
            predicate = { false },
            thenWork = failWork("then-work"),
            otherwiseWork = null,
        )

        val report = flow.execute(context)

        report.isSuccess.shouldBeTrue()
        report shouldBeInstanceOf WorkReport.Success::class
    }

    @Test
    fun `thenWork가 ABORTED - Aborted 전파`() {
        val flow = ConditionalWorkFlow(
            predicate = { true },
            thenWork = abortWork("abort-work"),
            otherwiseWork = successWork("otherwise-work"),
        )

        val report = flow.execute(context)

        report shouldBeInstanceOf WorkReport.Aborted::class
    }

    @Test
    fun `predicate가 컨텍스트 값 기반으로 분기`() {
        context["flag"] = true

        val flow = ConditionalWorkFlow(
            predicate = { ctx -> ctx.get<Boolean>("flag") == true },
            thenWork = successWork("flag-true-work"),
            otherwiseWork = failWork("flag-false-work"),
        )

        val report = flow.execute(context)

        report.isSuccess.shouldBeTrue()
    }
}
