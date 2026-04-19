package io.bluetape4k.workflow.examples

import io.bluetape4k.workflow.api.SuspendWork
import io.bluetape4k.workflow.api.Work
import io.bluetape4k.workflow.api.WorkReport
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

// ──────────────────────────────────────────────────────────────────────────
// Sync (Work) fixtures
// ──────────────────────────────────────────────────────────────────────────

internal fun fixSyncValidateOrder(): Work = Work("order-validate") { ctx ->
    val amount = ctx.get<Long>("order.amount") ?: 0L
    val userId = ctx.get<String>("order.userId")
    if (amount <= 0L || userId.isNullOrBlank()) {
        return@Work WorkReport.aborted(ctx, "주문 정보가 유효하지 않습니다 (userId=$userId, amount=$amount)")
    }
    WorkReport.success(ctx)
}

internal fun fixSyncCheckInventory(available: Boolean = true): Work = Work("inventory-check") { ctx ->
    Thread.sleep(10)
    ctx["inventory.ok"] = available
    if (!available) WorkReport.aborted(ctx, "재고 부족") else WorkReport.success(ctx)
}

internal fun fixSyncCheckFraud(passed: Boolean = true): Work = Work("fraud-check") { ctx ->
    Thread.sleep(15)
    ctx["fraud.ok"] = passed
    if (!passed) WorkReport.aborted(ctx, "사기 의심 거래") else WorkReport.success(ctx)
}

internal fun fixSyncValidateCoupon(): Work = Work("coupon-validate") { ctx ->
    Thread.sleep(5)
    ctx["coupon.discount"] = 2_000L
    WorkReport.success(ctx)
}

internal fun fixSyncRequestPayment(
    successOnAttempt: Int = 1,
    txIdPrefix: String = "TX",
): Work {
    var attempt = 0
    return Work("pg-payment-request") { ctx ->
        attempt++
        if (attempt < successOnAttempt) {
            return@Work WorkReport.failure(ctx, RuntimeException("PG 서버 일시 오류 (시도 #$attempt)"))
        }
        ctx["pg.txId"] = if (txIdPrefix == "TX") "TX-${System.currentTimeMillis()}" else "$txIdPrefix-$attempt"
        ctx["pg.approved"] = false
        WorkReport.success(ctx)
    }
}

internal fun fixSyncPollPgApproval(approvedOnPoll: Int = 2): Work {
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

internal fun fixSyncConfirmOrder(): Work = Work("order-confirm") { ctx ->
    ctx["order.status"] = "CONFIRMED"
    WorkReport.success(ctx)
}

internal fun fixSyncCancelOrder(): Work = Work("order-cancel") { ctx ->
    ctx["order.status"] = "CANCELLED"
    WorkReport.success(ctx)
}

// ──────────────────────────────────────────────────────────────────────────
// Suspend (SuspendWork) fixtures
// ──────────────────────────────────────────────────────────────────────────

internal fun fixSuspendValidateOrder(): SuspendWork = SuspendWork("order-validate") { ctx ->
    val amount = ctx.get<Long>("order.amount") ?: 0L
    val userId = ctx.get<String>("order.userId")
    if (amount <= 0L || userId.isNullOrBlank()) {
        return@SuspendWork WorkReport.aborted(ctx, "주문 정보가 유효하지 않습니다 (userId=$userId, amount=$amount)")
    }
    WorkReport.success(ctx)
}

internal fun fixSuspendCheckInventory(available: Boolean = true): SuspendWork = SuspendWork("inventory-check") { ctx ->
    delay(10.milliseconds)
    ctx["inventory.ok"] = available
    if (!available) WorkReport.aborted(ctx, "재고 부족") else WorkReport.success(ctx)
}

internal fun fixSuspendCheckFraud(passed: Boolean = true): SuspendWork = SuspendWork("fraud-check") { ctx ->
    delay(15.milliseconds)
    ctx["fraud.ok"] = passed
    if (!passed) WorkReport.aborted(ctx, "사기 의심 거래") else WorkReport.success(ctx)
}

internal fun fixSuspendValidateCoupon(): SuspendWork = SuspendWork("coupon-validate") { ctx ->
    delay(5.milliseconds)
    ctx["coupon.discount"] = 2_000L
    WorkReport.success(ctx)
}

internal fun fixSuspendRequestPayment(
    successOnAttempt: Int = 1,
    txIdPrefix: String = "TX",
): SuspendWork {
    var attempt = 0
    return SuspendWork("pg-payment-request") { ctx ->
        attempt++
        if (attempt < successOnAttempt) {
            return@SuspendWork WorkReport.failure(ctx, RuntimeException("PG 서버 일시 오류 (시도 #$attempt)"))
        }
        ctx["pg.txId"] = if (txIdPrefix == "TX") "TX-${System.currentTimeMillis()}" else "$txIdPrefix-$attempt"
        ctx["pg.approved"] = false
        WorkReport.success(ctx)
    }
}

internal fun fixSuspendPollPgApproval(approvedOnPoll: Int = 2): SuspendWork {
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

internal fun fixSuspendConfirmOrder(): SuspendWork = SuspendWork("order-confirm") { ctx ->
    ctx["order.status"] = "CONFIRMED"
    WorkReport.success(ctx)
}

internal fun fixSuspendCancelOrder(): SuspendWork = SuspendWork("order-cancel") { ctx ->
    ctx["order.status"] = "CANCELLED"
    WorkReport.success(ctx)
}
