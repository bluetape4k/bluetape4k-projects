package io.bluetape4k.workflow.examples

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.workflow.api.SuspendWork
import io.bluetape4k.workflow.api.Work
import io.bluetape4k.workflow.api.WorkContext
import io.bluetape4k.workflow.api.WorkReport
import io.bluetape4k.workflow.api.workContext
import io.bluetape4k.workflow.coroutines.suspendSequentialFlow
import io.bluetape4k.workflow.core.sequentialFlow
import kotlinx.coroutines.delay
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/**
 * 동기(Virtual Threads)와 코루틴 워크플로 실행 시간을 같은 시나리오로 비교하는 benchmark 성격의 테스트입니다.
 *
 * 절대 성능을 보장하는 테스트가 아니라, 두 실행 모델이 동일한 결과를 내는지 확인하면서
 * 현재 환경에서의 상대적인 실행 시간을 관찰하기 위한 용도입니다.
 */
class OrderProcessingExecutionModelBenchmarkTest {

    companion object: KLogging()

    private data class BenchmarkResult(
        val report: WorkReport,
        val elapsed: Duration,
        val context: WorkContext,
    )

    private fun syncValidateOrder(): Work = Work("order-validate") { ctx ->
        val amount = ctx.get<Long>("order.amount") ?: 0L
        val userId = ctx.get<String>("order.userId")
        if (amount <= 0L || userId.isNullOrBlank()) {
            return@Work WorkReport.aborted(ctx, "주문 정보가 유효하지 않습니다 (userId=$userId, amount=$amount)")
        }
        WorkReport.success(ctx)
    }

    private fun suspendValidateOrder(): SuspendWork = SuspendWork("order-validate") { ctx ->
        val amount = ctx.get<Long>("order.amount") ?: 0L
        val userId = ctx.get<String>("order.userId")
        if (amount <= 0L || userId.isNullOrBlank()) {
            return@SuspendWork WorkReport.aborted(ctx, "주문 정보가 유효하지 않습니다 (userId=$userId, amount=$amount)")
        }
        WorkReport.success(ctx)
    }

    private fun syncCheckInventory(available: Boolean = true): Work = Work("inventory-check") { ctx ->
        Thread.sleep(10)
        ctx["inventory.ok"] = available
        if (!available) WorkReport.aborted(ctx, "재고 부족") else WorkReport.success(ctx)
    }

    private fun suspendCheckInventory(available: Boolean = true): SuspendWork = SuspendWork("inventory-check") { ctx ->
        delay(10.milliseconds)
        ctx["inventory.ok"] = available
        if (!available) WorkReport.aborted(ctx, "재고 부족") else WorkReport.success(ctx)
    }

    private fun syncCheckFraud(passed: Boolean = true): Work = Work("fraud-check") { ctx ->
        Thread.sleep(15)
        ctx["fraud.ok"] = passed
        if (!passed) WorkReport.aborted(ctx, "사기 의심 거래") else WorkReport.success(ctx)
    }

    private fun suspendCheckFraud(passed: Boolean = true): SuspendWork = SuspendWork("fraud-check") { ctx ->
        delay(15.milliseconds)
        ctx["fraud.ok"] = passed
        if (!passed) WorkReport.aborted(ctx, "사기 의심 거래") else WorkReport.success(ctx)
    }

    private fun syncValidateCoupon(): Work = Work("coupon-validate") { ctx ->
        Thread.sleep(5)
        ctx["coupon.discount"] = 2_000L
        WorkReport.success(ctx)
    }

    private fun suspendValidateCoupon(): SuspendWork = SuspendWork("coupon-validate") { ctx ->
        delay(5.milliseconds)
        ctx["coupon.discount"] = 2_000L
        WorkReport.success(ctx)
    }

    private fun syncRequestPayment(successOnAttempt: Int): Work {
        var attempt = 0
        return Work("pg-payment-request") { ctx ->
            attempt++
            if (attempt < successOnAttempt) {
                return@Work WorkReport.failure(ctx, RuntimeException("PG 서버 일시 오류 (시도 #$attempt)"))
            }
            ctx["pg.txId"] = "SYNC-TX-$attempt"
            ctx["pg.approved"] = false
            WorkReport.success(ctx)
        }
    }

    private fun suspendRequestPayment(successOnAttempt: Int): SuspendWork {
        var attempt = 0
        return SuspendWork("pg-payment-request") { ctx ->
            attempt++
            if (attempt < successOnAttempt) {
                return@SuspendWork WorkReport.failure(ctx, RuntimeException("PG 서버 일시 오류 (시도 #$attempt)"))
            }
            ctx["pg.txId"] = "SUSPEND-TX-$attempt"
            ctx["pg.approved"] = false
            WorkReport.success(ctx)
        }
    }

    private fun syncPollPgApproval(approvedOnPoll: Int): Work {
        var poll = 0
        return Work("pg-approval-poll") { ctx ->
            poll++
            ctx["pg.pollCount"] = poll
            Thread.sleep(10)
            if (poll >= approvedOnPoll) {
                ctx["pg.approved"] = true
            }
            WorkReport.success(ctx)
        }
    }

    private fun suspendPollPgApproval(approvedOnPoll: Int): SuspendWork {
        var poll = 0
        return SuspendWork("pg-approval-poll") { ctx ->
            poll++
            ctx["pg.pollCount"] = poll
            delay(10.milliseconds)
            if (poll >= approvedOnPoll) {
                ctx["pg.approved"] = true
            }
            WorkReport.success(ctx)
        }
    }

    private fun syncConfirmOrder(): Work = Work("order-confirm") { ctx ->
        ctx["order.status"] = "CONFIRMED"
        WorkReport.success(ctx)
    }

    private fun suspendConfirmOrder(): SuspendWork = SuspendWork("order-confirm") { ctx ->
        ctx["order.status"] = "CONFIRMED"
        WorkReport.success(ctx)
    }

    private fun syncCancelOrder(): Work = Work("order-cancel") { ctx ->
        ctx["order.status"] = "CANCELLED"
        WorkReport.success(ctx)
    }

    private fun suspendCancelOrder(): SuspendWork = SuspendWork("order-cancel") { ctx ->
        ctx["order.status"] = "CANCELLED"
        WorkReport.success(ctx)
    }

    private fun buildSyncOrderFlow(
        paymentSuccessOnAttempt: Int,
        pgApprovedOnPoll: Int,
    ) = sequentialFlow("order-processing-sync-benchmark") {
        execute(syncValidateOrder())
        parallel("pre-checks") {
            execute(syncCheckInventory())
            execute(syncCheckFraud())
            execute(syncValidateCoupon())
        }
        retry("pg-payment") {
            execute(syncRequestPayment(paymentSuccessOnAttempt))
            policy {
                maxAttempts = 3
                delay = 50.milliseconds
            }
        }
        repeat("pg-approval-wait") {
            execute(syncPollPgApproval(pgApprovedOnPoll))
            until { report -> report.context.get<Boolean>("pg.approved") == true }
            maxIterations(5)
        }
        conditional("post-payment") {
            condition { ctx -> ctx.get<Boolean>("pg.approved") == true }
            then(syncConfirmOrder())
            otherwise(syncCancelOrder())
        }
    }

    private fun buildSuspendOrderFlow(
        paymentSuccessOnAttempt: Int,
        pgApprovedOnPoll: Int,
    ) = suspendSequentialFlow("order-processing-suspend-benchmark") {
        execute(suspendValidateOrder())
        parallel("pre-checks") {
            execute(suspendCheckInventory())
            execute(suspendCheckFraud())
            execute(suspendValidateCoupon())
        }
        retry("pg-payment") {
            execute(suspendRequestPayment(paymentSuccessOnAttempt))
            policy {
                maxAttempts = 3
                delay = 50.milliseconds
            }
        }
        repeat("pg-approval-wait") {
            execute(suspendPollPgApproval(pgApprovedOnPoll))
            until { report -> report.context.get<Boolean>("pg.approved") == true }
            maxIterations(5)
            repeatDelay(5.milliseconds)
        }
        conditional("post-payment") {
            condition { ctx -> ctx.get<Boolean>("pg.approved") == true }
            then(suspendConfirmOrder())
            otherwise(suspendCancelOrder())
        }
    }

    private fun baseContext(orderId: String) = workContext(
        "order.id" to orderId,
        "order.userId" to "benchmark-user",
        "order.amount" to 15_000L,
    )

    private fun runSyncBenchmark(
        orderId: String,
        paymentSuccessOnAttempt: Int,
        pgApprovedOnPoll: Int,
    ): BenchmarkResult {
        val context = baseContext(orderId)
        val started = TimeSource.Monotonic.markNow()
        val report = buildSyncOrderFlow(
            paymentSuccessOnAttempt = paymentSuccessOnAttempt,
            pgApprovedOnPoll = pgApprovedOnPoll,
        ).execute(context)
        return BenchmarkResult(report, started.elapsedNow(), context)
    }

    private fun runSuspendBenchmark(
        orderId: String,
        paymentSuccessOnAttempt: Int,
        pgApprovedOnPoll: Int,
    ): BenchmarkResult {
        val context = baseContext(orderId)
        val started = TimeSource.Monotonic.markNow()
        lateinit var report: WorkReport
        runSuspendIO {
            report = buildSuspendOrderFlow(
                paymentSuccessOnAttempt = paymentSuccessOnAttempt,
                pgApprovedOnPoll = pgApprovedOnPoll,
            ).execute(context)
        }
        return BenchmarkResult(report, started.elapsedNow(), context)
    }

    private fun assertConfirmed(result: BenchmarkResult, expectedPollCount: Int) {
        result.report.isSuccess.shouldBeTrue()
        result.context.get<String>("order.status") shouldBeEqualTo "CONFIRMED"
        (result.context.get<Boolean>("pg.approved") == true).shouldBeTrue()
        result.context.get<Int>("pg.pollCount") shouldBeEqualTo expectedPollCount
    }

    @Test
    fun `동기와 코루틴 주문 처리 실행 시간을 비교한다`() {
        val sync = runSyncBenchmark(
            orderId = "BENCH-SYNC-001",
            paymentSuccessOnAttempt = 1,
            pgApprovedOnPoll = 2,
        )
        val suspend = runSuspendBenchmark(
            orderId = "BENCH-SUSPEND-001",
            paymentSuccessOnAttempt = 1,
            pgApprovedOnPoll = 2,
        )

        assertConfirmed(sync, expectedPollCount = 2)
        assertConfirmed(suspend, expectedPollCount = 2)

        log.info {
            "workflow benchmark(normal): sync=${sync.elapsed}, suspend=${suspend.elapsed}, " +
                "ratio=${"%.2f".format(suspend.elapsed.inWholeMilliseconds.toDouble() / sync.elapsed.inWholeMilliseconds.coerceAtLeast(1))}"
        }
    }

    @Test
    fun `재시도와 폴링이 포함된 주문 처리 실행 시간을 비교한다`() {
        val sync = runSyncBenchmark(
            orderId = "BENCH-SYNC-RETRY-001",
            paymentSuccessOnAttempt = 3,
            pgApprovedOnPoll = 4,
        )
        val suspend = runSuspendBenchmark(
            orderId = "BENCH-SUSPEND-RETRY-001",
            paymentSuccessOnAttempt = 3,
            pgApprovedOnPoll = 4,
        )

        assertConfirmed(sync, expectedPollCount = 4)
        assertConfirmed(suspend, expectedPollCount = 4)

        log.info {
            "workflow benchmark(retry+poll): sync=${sync.elapsed}, suspend=${suspend.elapsed}, " +
                "ratio=${"%.2f".format(suspend.elapsed.inWholeMilliseconds.toDouble() / sync.elapsed.inWholeMilliseconds.coerceAtLeast(1))}"
        }
    }
}
