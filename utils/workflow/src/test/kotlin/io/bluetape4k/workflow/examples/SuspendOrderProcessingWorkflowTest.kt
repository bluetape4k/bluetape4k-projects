package io.bluetape4k.workflow.examples

import io.bluetape4k.logging.KLogging
import io.bluetape4k.workflow.api.WorkReport
import io.bluetape4k.workflow.api.workContext
import io.bluetape4k.workflow.coroutines.suspendSequentialFlow
import kotlinx.coroutines.test.runTest
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * 주문 처리 워크플로 실무 예제 (Coroutines / suspend 방식).
 */
class SuspendOrderProcessingWorkflowTest {

    companion object: KLogging()

    private fun orderCtx(id: String, userId: String, amount: Long) = workContext(
        "order.id" to id,
        "order.userId" to userId,
        "order.amount" to amount,
    )

    private fun buildOrderFlow(
        inventoryAvailable: Boolean = true,
        fraudPassed: Boolean = true,
        paymentSuccessOnAttempt: Int = 1,
        pgApprovedOnPoll: Int = 2,
    ) = suspendSequentialFlow("suspend-order-processing") {

        execute(fixSuspendValidateOrder())

        parallel("pre-checks") {
            execute(fixSuspendCheckInventory(available = inventoryAvailable))
            execute(fixSuspendCheckFraud(passed = fraudPassed))
            execute(fixSuspendValidateCoupon())
        }

        retry("pg-payment") {
            execute(fixSuspendRequestPayment(successOnAttempt = paymentSuccessOnAttempt))
            policy {
                maxAttempts = 3
                delay = 50.milliseconds
            }
        }

        repeat("pg-approval-wait") {
            execute(fixSuspendPollPgApproval(approvedOnPoll = pgApprovedOnPoll))
            until { report -> report.context.get<Boolean>("pg.approved") == true }
            maxIterations(5)
            repeatDelay(5.milliseconds)
        }

        conditional("post-payment") {
            condition { ctx -> ctx.get<Boolean>("pg.approved") == true }
            then(fixSuspendConfirmOrder())
            otherwise(fixSuspendCancelOrder())
        }
    }

    @Test
    fun `정상 주문 처리 - 전체 플로우 성공`() = runTest(timeout = 30.seconds) {
        val ctx = orderCtx("ORD-001", "user-42", 15_000L)

        val report = buildOrderFlow(
            paymentSuccessOnAttempt = 1,
            pgApprovedOnPoll = 2,
        ).execute(ctx)

        report.isSuccess.shouldBeTrue()
        ctx.get<String>("order.status") shouldBeEqualTo "CONFIRMED"
        ctx.get<String>("pg.txId").shouldNotBeNull()
        (ctx.get<Boolean>("pg.approved") == true).shouldBeTrue()
        ctx.get<Int>("pg.pollCount") shouldBeEqualTo 2
        ctx.get<Long>("coupon.discount") shouldBeEqualTo 2_000L
        (ctx.get<Boolean>("inventory.ok") == true).shouldBeTrue()
        (ctx.get<Boolean>("fraud.ok") == true).shouldBeTrue()
    }

    @Test
    fun `결제 재시도 성공 - 3번째 시도에 결제 완료`() = runTest(timeout = 30.seconds) {
        val ctx = orderCtx("ORD-002", "user-99", 30_000L)

        val report = buildOrderFlow(
            paymentSuccessOnAttempt = 3,
            pgApprovedOnPoll = 1,
        ).execute(ctx)

        report.isSuccess.shouldBeTrue()
        ctx.get<String>("order.status") shouldBeEqualTo "CONFIRMED"
        ctx.get<String>("pg.txId").shouldNotBeNull()
    }

    @Test
    fun `PG 승인 지연 - 4회 폴링 후 승인 완료`() = runTest(timeout = 30.seconds) {
        val ctx = orderCtx("ORD-003", "user-77", 8_000L)

        val report = buildOrderFlow(
            paymentSuccessOnAttempt = 1,
            pgApprovedOnPoll = 4,
        ).execute(ctx)

        report.isSuccess.shouldBeTrue()
        ctx.get<String>("order.status") shouldBeEqualTo "CONFIRMED"
        ctx.get<Int>("pg.pollCount") shouldBeEqualTo 4
    }

    @Test
    fun `재고 부족 - ABORTED 즉시 전파, 결제 단계 미실행`() = runTest(timeout = 30.seconds) {
        val ctx = orderCtx("ORD-004", "user-7", 5_000L)

        val report = buildOrderFlow(inventoryAvailable = false).execute(ctx)

        report shouldBeInstanceOf WorkReport.Aborted::class
        (ctx.get<String>("pg.txId") == null).shouldBeTrue()
        (ctx.get<String>("order.status") == null).shouldBeTrue()
    }

    @Test
    fun `사기 탐지 실패 - ABORTED 반환, 결제 미실행`() = runTest(timeout = 30.seconds) {
        val ctx = orderCtx("ORD-005", "suspicious-user", 999_000L)

        val report = buildOrderFlow(fraudPassed = false).execute(ctx)

        report shouldBeInstanceOf WorkReport.Aborted::class
        (ctx.get<String>("pg.txId") == null).shouldBeTrue()
    }

    @Test
    fun `PG 승인 타임아웃 - 5회 폴링 후 미승인, 주문 취소`() = runTest(timeout = 30.seconds) {
        val ctx = orderCtx("ORD-006", "user-slow", 12_000L)

        val report = buildOrderFlow(
            paymentSuccessOnAttempt = 1,
            pgApprovedOnPoll = 99,
        ).execute(ctx)

        report.isSuccess.shouldBeTrue()
        ctx.get<String>("order.status") shouldBeEqualTo "CANCELLED"
        ctx.get<Int>("pg.pollCount") shouldBeEqualTo 5
        (ctx.get<Boolean>("pg.approved") == false).shouldBeTrue()
    }

    @Test
    fun `유효하지 않은 주문 - userId 없음 ABORTED`() = runTest(timeout = 30.seconds) {
        val ctx = workContext("order.id" to "ORD-007", "order.amount" to 10_000L)

        val report = suspendSequentialFlow("order-processing") {
            execute(fixSuspendValidateOrder())
        }.execute(ctx)

        report shouldBeInstanceOf WorkReport.Aborted::class
    }
}
