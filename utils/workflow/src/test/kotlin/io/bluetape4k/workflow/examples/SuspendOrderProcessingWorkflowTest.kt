package io.bluetape4k.workflow.examples

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.workflow.api.SuspendWork
import io.bluetape4k.workflow.api.WorkContext
import io.bluetape4k.workflow.api.WorkReport
import io.bluetape4k.workflow.api.workContext
import io.bluetape4k.workflow.coroutines.suspendSequentialFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * 주문 처리 워크플로 실무 예제 (Coroutines / suspend 방식).
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
 *
 * ### Context 키 규약
 * | 키                  | 타입    | 설명                       |
 * |---------------------|---------|---------------------------|
 * | `order.id`          | String  | 주문 ID                    |
 * | `order.userId`      | String  | 사용자 ID                  |
 * | `order.amount`      | Long    | 결제 금액                  |
 * | `inventory.ok`      | Boolean | 재고 확인 결과             |
 * | `fraud.ok`          | Boolean | 사기 탐지 결과             |
 * | `coupon.discount`   | Long    | 쿠폰 할인 금액             |
 * | `pg.txId`           | String  | PG 트랜잭션 ID             |
 * | `pg.approved`       | Boolean | PG 승인 완료 여부          |
 * | `pg.pollCount`      | Int     | PG 폴링 횟수               |
 * | `order.status`      | String  | 최종 주문 상태             |
 */
class SuspendOrderProcessingWorkflowTest {

    companion object : KLogging()

    // ──────────────────────────────────────────────────────────────────────────
    // Step ① 주문 유효성 검사
    // ──────────────────────────────────────────────────────────────────────────

    private fun validateOrder(): SuspendWork = SuspendWork("order-validate") { ctx ->
        val amount = ctx.get<Long>("order.amount") ?: 0L
        val userId = ctx.get<String>("order.userId")
        if (amount <= 0L || userId.isNullOrBlank()) {
            return@SuspendWork WorkReport.aborted(ctx, "주문 정보가 유효하지 않습니다 (userId=$userId, amount=$amount)")
        }
        log.debug { "① 주문 유효성 검사 통과 (userId=$userId, amount=$amount)" }
        WorkReport.success(ctx)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Step ② 병렬 사전 검증
    // ──────────────────────────────────────────────────────────────────────────

    private fun checkInventory(available: Boolean = true): SuspendWork = SuspendWork("inventory-check") { ctx ->
        delay(10.milliseconds)
        ctx["inventory.ok"] = available
        log.debug { "② 재고 확인: available=$available" }
        if (!available) WorkReport.aborted(ctx, "재고 부족") else WorkReport.success(ctx)
    }

    private fun checkFraud(passed: Boolean = true): SuspendWork = SuspendWork("fraud-check") { ctx ->
        delay(15.milliseconds)
        ctx["fraud.ok"] = passed
        log.debug { "② 사기 탐지: passed=$passed" }
        if (!passed) WorkReport.aborted(ctx, "사기 의심 거래") else WorkReport.success(ctx)
    }

    private fun validateCoupon(): SuspendWork = SuspendWork("coupon-validate") { ctx ->
        delay(5.milliseconds)
        ctx["coupon.discount"] = 2_000L
        log.debug { "② 쿠폰 검증 완료: discount=2000" }
        WorkReport.success(ctx)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Step ③ PG 결제 요청 (retry 대상)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * PG 결제 요청 — [successOnAttempt]번째 시도에 성공합니다.
     * 성공 시 `pg.txId`를 ctx에 저장하고 `pg.approved = false`(미승인)로 초기화합니다.
     */
    private fun requestPayment(successOnAttempt: Int = 1): SuspendWork {
        var attempt = 0
        return SuspendWork("pg-payment-request") { ctx ->
            attempt++
            log.debug { "③ PG 결제 요청 시도 #$attempt" }
            if (attempt < successOnAttempt) {
                return@SuspendWork WorkReport.failure(ctx, RuntimeException("PG 서버 일시 오류 (시도 #$attempt)"))
            }
            ctx["pg.txId"] = "TX-${System.currentTimeMillis()}"
            ctx["pg.approved"] = false  // 승인 대기 — repeat 폴링 대상
            log.debug { "③ PG 결제 요청 완료: txId=${ctx.get<String>("pg.txId")}" }
            WorkReport.success(ctx)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Step ④ PG 승인 대기 (repeat 대상)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * PG 승인 상태 폴링 — [approvedOnPoll]번째 폴링에서 승인 완료됩니다.
     * 폴링 횟수는 `pg.pollCount`에 누적됩니다.
     */
    private fun pollPgApproval(approvedOnPoll: Int = 2): SuspendWork {
        var poll = 0
        return SuspendWork("pg-approval-poll") { ctx ->
            poll++
            ctx["pg.pollCount"] = poll
            delay(10.milliseconds)
            if (poll >= approvedOnPoll) {
                ctx["pg.approved"] = true
                log.debug { "④ PG 승인 완료 (폴링 #$poll)" }
            } else {
                log.debug { "④ PG 승인 대기 중... (폴링 #$poll)" }
            }
            WorkReport.success(ctx)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Step ⑤ 결과 처리 (conditional)
    // ──────────────────────────────────────────────────────────────────────────

    private fun confirmOrder(): SuspendWork = SuspendWork("order-confirm") { ctx ->
        ctx["order.status"] = "CONFIRMED"
        log.debug { "⑤ 주문 확정 완료 (txId=${ctx.get<String>("pg.txId")})" }
        WorkReport.success(ctx)
    }

    private fun cancelOrder(): SuspendWork = SuspendWork("order-cancel") { ctx ->
        ctx["order.status"] = "CANCELLED"
        log.debug { "⑤ 재고 복구 및 주문 취소 완료" }
        WorkReport.success(ctx)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 전체 주문 처리 플로우 빌더
    // ──────────────────────────────────────────────────────────────────────────

    private fun buildOrderFlow(
        inventoryAvailable: Boolean = true,
        fraudPassed: Boolean = true,
        paymentSuccessOnAttempt: Int = 1,
        pgApprovedOnPoll: Int = 2,
    ) = suspendSequentialFlow("suspend-order-processing") {

        // ① 유효성 검사
        execute(validateOrder())

        // ② 병렬 사전 검증 (재고 ‖ 사기 탐지 ‖ 쿠폰) — parallel
        parallel("pre-checks") {
            execute(checkInventory(available = inventoryAvailable))
            execute(checkFraud(passed = fraudPassed))
            execute(validateCoupon())
        }

        // ③ PG 결제 요청 (최대 3회 재시도, 50ms 백오프) — retry
        retry("pg-payment") {
            execute(requestPayment(successOnAttempt = paymentSuccessOnAttempt))
            policy {
                maxAttempts = 3
                delay = 50.milliseconds
            }
        }

        // ④ PG 승인 대기 (최대 5회 폴링, 승인 완료 시 즉시 탈출) — repeat
        repeat("pg-approval-wait") {
            execute(pollPgApproval(approvedOnPoll = pgApprovedOnPoll))
            until { report -> report.context.get<Boolean>("pg.approved") == true }
            maxIterations(5)
            repeatDelay(5.milliseconds)
        }

        // ⑤ 결제 확정 여부에 따른 분기 — conditional
        conditional("post-payment") {
            condition { ctx -> ctx.get<Boolean>("pg.approved") == true }
            then(confirmOrder())
            otherwise(cancelOrder())
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 테스트 케이스
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `정상 주문 처리 - 전체 플로우 성공`() = runTest(timeout = 30.seconds) {
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
    fun `결제 재시도 성공 - 3번째 시도에 결제 완료`() = runTest(timeout = 30.seconds) {
        val ctx = workContext(
            "order.id" to "ORD-002",
            "order.userId" to "user-99",
            "order.amount" to 30_000L,
        )

        val report = buildOrderFlow(
            paymentSuccessOnAttempt = 3,  // 2회 실패 후 3번째 성공
            pgApprovedOnPoll = 1,
        ).execute(ctx)

        report.isSuccess.shouldBeTrue()
        ctx.get<String>("order.status") shouldBeEqualTo "CONFIRMED"
        ctx.get<String>("pg.txId").shouldNotBeNull()
    }

    @Test
    fun `PG 승인 지연 - 4회 폴링 후 승인 완료`() = runTest(timeout = 30.seconds) {
        val ctx = workContext(
            "order.id" to "ORD-003",
            "order.userId" to "user-77",
            "order.amount" to 8_000L,
        )

        val report = buildOrderFlow(
            paymentSuccessOnAttempt = 1,
            pgApprovedOnPoll = 4,  // 3회 대기 후 4번째에 승인
        ).execute(ctx)

        report.isSuccess.shouldBeTrue()
        ctx.get<String>("order.status") shouldBeEqualTo "CONFIRMED"
        ctx.get<Int>("pg.pollCount") shouldBeEqualTo 4
    }

    @Test
    fun `재고 부족 - ABORTED 즉시 전파, 결제 단계 미실행`() = runTest(timeout = 30.seconds) {
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
    fun `사기 탐지 실패 - ABORTED 반환, 결제 미실행`() = runTest(timeout = 30.seconds) {
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
    fun `PG 승인 타임아웃 - 5회 폴링 후 미승인, 주문 취소`() = runTest(timeout = 30.seconds) {
        // pgApprovedOnPoll=99 → maxIterations=5 내에 승인 안 됨
        val ctx = workContext(
            "order.id" to "ORD-006",
            "order.userId" to "user-slow",
            "order.amount" to 12_000L,
        )

        val report = buildOrderFlow(
            paymentSuccessOnAttempt = 1,
            pgApprovedOnPoll = 99,  // 5회 내에 승인 불가
        ).execute(ctx)

        // repeat 정상 종료(Success) → conditional에서 pg.approved=false → cancelOrder 실행
        report.isSuccess.shouldBeTrue()
        ctx.get<String>("order.status") shouldBeEqualTo "CANCELLED"
        ctx.get<Int>("pg.pollCount") shouldBeEqualTo 5
        (ctx.get<Boolean>("pg.approved") == false).shouldBeTrue()
    }

    @Test
    fun `유효하지 않은 주문 - userId 없음 ABORTED`() = runTest(timeout = 30.seconds) {
        val ctx = workContext(
            "order.id" to "ORD-007",
            "order.amount" to 10_000L,  // userId 없음
        )

        val report = suspendSequentialFlow("order-processing") {
            execute(validateOrder())
        }.execute(ctx)

        report shouldBeInstanceOf WorkReport.Aborted::class
    }
}
