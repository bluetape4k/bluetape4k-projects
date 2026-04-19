package io.bluetape4k.workflow.examples

import io.bluetape4k.logging.KLogging
import io.bluetape4k.workflow.api.WorkReport
import io.bluetape4k.workflow.api.workContext
import io.bluetape4k.workflow.core.sequentialFlow
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds

/**
 * 주문 처리 워크플로 실무 예제 (Virtual Threads / 동기 방식).
 *
 * ## 전체 플로우
 * ```
 * ① [주문 유효성 검사]                      ← sequential step
 *        ↓
 * ② [재고 확인 ‖ 사기 탐지 ‖ 쿠폰 검증]     ← parallel (3개 동시)
 *        ↓
 * ③ [PG 결제 요청] (최대 3회 재시도)         ← retry  (일시 오류 복구)
 *        ↓
 * ④ [PG 승인 대기] (최대 5회 폴링)           ← repeat (승인 완료까지 반복)
 *        ↓
 * ⑤ if (pg.approved == true)
 *       → [주문 확정 + 발송 준비]            ← conditional / then
 *    else
 *       → [재고 복구 + 취소 알림]             ← conditional / otherwise
 * ```
 */
class OrderProcessingWorkflowTest {

    companion object: KLogging()

    private fun buildOrderFlow(
        inventoryAvailable: Boolean = true,
        fraudPassed: Boolean = true,
        paymentSuccessOnAttempt: Int = 1,
        pgApprovedOnPoll: Int = 2,
    ) = sequentialFlow("order-processing") {

        execute(fixSyncValidateOrder())

        parallel("pre-checks") {
            execute(fixSyncCheckInventory(available = inventoryAvailable))
            execute(fixSyncCheckFraud(passed = fraudPassed))
            execute(fixSyncValidateCoupon())
        }

        retry("pg-payment") {
            execute(fixSyncRequestPayment(successOnAttempt = paymentSuccessOnAttempt))
            policy {
                maxAttempts = 3
                delay = 50.milliseconds
            }
        }

        repeat("pg-approval-wait") {
            execute(fixSyncPollPgApproval(approvedOnPoll = pgApprovedOnPoll))
            until { report -> report.context.get<Boolean>("pg.approved") == true }
            maxIterations(5)
        }

        conditional("post-payment") {
            condition { ctx -> ctx.get<Boolean>("pg.approved") == true }
            then(fixSyncConfirmOrder())
            otherwise(fixSyncCancelOrder())
        }
    }

    @Test
    fun `정상 주문 처리 - 전체 플로우 성공`() {
        val ctx = workContext(
            "order.id" to "ORD-001",
            "order.userId" to "user-42",
            "order.amount" to 15_000L,
        )

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
    fun `결제 재시도 성공 - 3번째 시도에 결제 완료`() {
        val ctx = workContext(
            "order.id" to "ORD-002",
            "order.userId" to "user-99",
            "order.amount" to 30_000L,
        )

        val report = buildOrderFlow(
            paymentSuccessOnAttempt = 3,
            pgApprovedOnPoll = 1,
        ).execute(ctx)

        report.isSuccess.shouldBeTrue()
        ctx.get<String>("order.status") shouldBeEqualTo "CONFIRMED"
        ctx.get<String>("pg.txId").shouldNotBeNull()
    }

    @Test
    fun `PG 승인 지연 - 4회 폴링 후 승인 완료`() {
        val ctx = workContext(
            "order.id" to "ORD-003",
            "order.userId" to "user-77",
            "order.amount" to 8_000L,
        )

        val report = buildOrderFlow(
            paymentSuccessOnAttempt = 1,
            pgApprovedOnPoll = 4,
        ).execute(ctx)

        report.isSuccess.shouldBeTrue()
        ctx.get<String>("order.status") shouldBeEqualTo "CONFIRMED"
        ctx.get<Int>("pg.pollCount") shouldBeEqualTo 4
    }

    @Test
    fun `재고 부족 - ABORTED 즉시 전파, 결제 단계 미실행`() {
        val ctx = workContext(
            "order.id" to "ORD-004",
            "order.userId" to "user-7",
            "order.amount" to 5_000L,
        )

        val report = buildOrderFlow(inventoryAvailable = false).execute(ctx)

        report shouldBeInstanceOf WorkReport.Aborted::class
        (ctx.get<String>("pg.txId") == null).shouldBeTrue()
        (ctx.get<String>("order.status") == null).shouldBeTrue()
    }

    @Test
    fun `사기 탐지 실패 - ABORTED 반환, 결제 미실행`() {
        val ctx = workContext(
            "order.id" to "ORD-005",
            "order.userId" to "suspicious-user",
            "order.amount" to 999_000L,
        )

        val report = buildOrderFlow(fraudPassed = false).execute(ctx)

        report shouldBeInstanceOf WorkReport.Aborted::class
        (ctx.get<String>("pg.txId") == null).shouldBeTrue()
    }

    @Test
    fun `PG 승인 타임아웃 - 5회 폴링 후 미승인, 주문 취소`() {
        val ctx = workContext(
            "order.id" to "ORD-006",
            "order.userId" to "user-slow",
            "order.amount" to 12_000L,
        )

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
    fun `유효하지 않은 주문 - 금액 0 ABORTED`() {
        val ctx = workContext(
            "order.id" to "ORD-007",
            "order.userId" to "user-1",
            "order.amount" to 0L,
        )

        val report = sequentialFlow("order-processing") {
            execute(fixSyncValidateOrder())
        }.execute(ctx)

        report shouldBeInstanceOf WorkReport.Aborted::class
    }
}
