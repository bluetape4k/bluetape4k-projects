package io.bluetape4k.workflow.examples

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.workflow.api.WorkContext
import io.bluetape4k.workflow.api.WorkReport
import io.bluetape4k.workflow.api.workContext
import io.bluetape4k.workflow.coroutines.suspendSequentialFlow
import io.bluetape4k.workflow.core.sequentialFlow
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/**
 * 동기(Virtual Threads)와 코루틴 워크플로 실행 시간을 같은 시나리오로 비교하는 benchmark 성격의 테스트입니다.
 */
class OrderProcessingExecutionModelBenchmarkTest {

    companion object: KLogging()

    private data class BenchmarkResult(
        val report: WorkReport,
        val elapsed: Duration,
        val context: WorkContext,
    )

    private fun buildSyncOrderFlow(
        paymentSuccessOnAttempt: Int,
        pgApprovedOnPoll: Int,
    ) = sequentialFlow("order-processing-sync-benchmark") {
        execute(fixSyncValidateOrder())
        parallel("pre-checks") {
            execute(fixSyncCheckInventory())
            execute(fixSyncCheckFraud())
            execute(fixSyncValidateCoupon())
        }
        retry("pg-payment") {
            execute(fixSyncRequestPayment(successOnAttempt = paymentSuccessOnAttempt, txIdPrefix = "SYNC-TX"))
            policy {
                maxAttempts = 3
                delay = 50.milliseconds
            }
        }
        repeat("pg-approval-wait") {
            execute(fixSyncPollPgApproval(pgApprovedOnPoll))
            until { report -> report.context.get<Boolean>("pg.approved") == true }
            maxIterations(5)
        }
        conditional("post-payment") {
            condition { ctx -> ctx.get<Boolean>("pg.approved") == true }
            then(fixSyncConfirmOrder())
            otherwise(fixSyncCancelOrder())
        }
    }

    private fun buildSuspendOrderFlow(
        paymentSuccessOnAttempt: Int,
        pgApprovedOnPoll: Int,
    ) = suspendSequentialFlow("order-processing-suspend-benchmark") {
        execute(fixSuspendValidateOrder())
        parallel("pre-checks") {
            execute(fixSuspendCheckInventory())
            execute(fixSuspendCheckFraud())
            execute(fixSuspendValidateCoupon())
        }
        retry("pg-payment") {
            execute(fixSuspendRequestPayment(successOnAttempt = paymentSuccessOnAttempt, txIdPrefix = "SUSPEND-TX"))
            policy {
                maxAttempts = 3
                delay = 50.milliseconds
            }
        }
        repeat("pg-approval-wait") {
            execute(fixSuspendPollPgApproval(pgApprovedOnPoll))
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
