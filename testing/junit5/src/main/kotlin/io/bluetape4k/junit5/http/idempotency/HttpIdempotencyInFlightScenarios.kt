package io.bluetape4k.junit5.http.idempotency

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import java.time.Duration

internal fun inFlightScenarios(): List<ConformanceScenario> = listOf(
    ConformanceScenario("bounded-wait") { adapter, config ->
        assertWaiterReceivesOwnerTerminal(adapter, config)
    },
    deadlineOrderingScenario(),
    ConformanceScenario("wait-timeout") { adapter, config -> assertTimeoutReclaimsSlot(adapter, config) },
    ConformanceScenario("waiter-overflow") { adapter, config -> assertOverflowIsImmediate(adapter, config) },
    ConformanceScenario("waiter-cancellation") { adapter, config ->
        assertCancellationReclaimsSlot(adapter, config)
    },
    ConformanceScenario("transient-abandon") { adapter, config ->
        assertTransientAbandonElectsOneRetryOwner(adapter, config)
    },
    ConformanceScenario("owner-disconnect") { adapter, config ->
        assertOwnerDisconnectBeforeAndAfterCommit(adapter, config)
    },
)

internal fun deadlineOrderingScenario(): ConformanceScenario =
    ConformanceScenario("deadline-ordering") { adapter, config -> assertDeadlineOrdering(adapter, config) }

private suspend fun assertWaiterReceivesOwnerTerminal(
    adapter: BoundedWaitHttpIdempotencyAdapter,
    config: BoundedWaitHttpIdempotencyConformanceConfig,
) = coroutineScope {
    val command = request(idempotencyKeys = listOf("bounded-wait-key"))
    val owner = async { exchangeChecked(adapter, config, command) }
    adapter.awaitOwnerStarted(command)
    val waiter = async { exchangeChecked(adapter, config, command) }
    adapter.awaitWaiterCount(command, 1)

    adapter.completeOwner(command, createdResponse())

    owner.await() shouldBeEqualTo createdResponse().withReplayFlag(false)
    waiter.await() shouldBeEqualTo createdResponse().withReplayFlag(true)
    adapter.sideEffectCount(command) shouldBeEqualTo 1
}

private suspend fun assertDeadlineOrdering(
    adapter: BoundedWaitHttpIdempotencyAdapter,
    config: BoundedWaitHttpIdempotencyConformanceConfig,
) {
    assertDeadlineOrdering(
        adapter,
        config,
        key = "deadline-before-key",
        elapsed = config.waitTimeout.minusNanos(1),
        expectedCode = null,
    )
    assertDeadlineOrdering(
        adapter,
        config,
        key = "deadline-at-key",
        elapsed = config.waitTimeout,
        expectedCode = "idempotency_in_flight",
    )
    assertDeadlineOrdering(
        adapter,
        config,
        key = "deadline-after-key",
        elapsed = config.waitTimeout.plusNanos(1),
        expectedCode = "idempotency_in_flight",
    )
}

private suspend fun assertDeadlineOrdering(
    adapter: BoundedWaitHttpIdempotencyAdapter,
    config: BoundedWaitHttpIdempotencyConformanceConfig,
    key: String,
    elapsed: Duration,
    expectedCode: String?,
) = coroutineScope {
    val command = request(idempotencyKeys = listOf(key))
    val owner = async { exchangeChecked(adapter, config, command) }
    adapter.awaitOwnerStarted(command)
    val waiter = async { exchangeChecked(adapter, config, command) }
    adapter.awaitWaiterCount(command, 1)

    adapter.advanceTimeBy(elapsed)
    if (expectedCode == null) {
        adapter.completeOwner(command, createdResponse())
        waiter.await() shouldBeEqualTo createdResponse().withReplayFlag(true)
    } else {
        waiter.await() shouldBeEqualTo inFlightTimeoutResponse(config)
        adapter.awaitWaiterCount(command, 0)
        adapter.completeOwner(command, createdResponse())
    }
    owner.await() shouldBeEqualTo createdResponse().withReplayFlag(false)
    adapter.sideEffectCount(command) shouldBeEqualTo 1
}

private suspend fun assertTimeoutReclaimsSlot(
    adapter: BoundedWaitHttpIdempotencyAdapter,
    config: BoundedWaitHttpIdempotencyConformanceConfig,
) = coroutineScope {
    val command = request(idempotencyKeys = listOf("timeout-key"))
    val owner = async { exchangeChecked(adapter, config, command) }
    adapter.awaitOwnerStarted(command)
    val timedOut = async { exchangeChecked(adapter, config, command) }
    adapter.awaitWaiterCount(command, 1)

    adapter.advanceTimeBy(config.waitTimeout)
    timedOut.await() shouldBeEqualTo inFlightTimeoutResponse(config)
    adapter.awaitWaiterCount(command, 0)

    val replacement = async { exchangeChecked(adapter, config, command) }
    adapter.awaitWaiterCount(command, 1)
    adapter.completeOwner(command, createdResponse())
    owner.await() shouldBeEqualTo createdResponse().withReplayFlag(false)
    replacement.await() shouldBeEqualTo createdResponse().withReplayFlag(true)
    adapter.sideEffectCount(command) shouldBeEqualTo 1
}

private suspend fun assertOverflowIsImmediate(
    adapter: BoundedWaitHttpIdempotencyAdapter,
    config: BoundedWaitHttpIdempotencyConformanceConfig,
) = coroutineScope {
    val command = request(idempotencyKeys = listOf("overflow-key"))
    val owner = async { exchangeChecked(adapter, config, command) }
    adapter.awaitOwnerStarted(command)
    val admitted = List(config.maxWaitersPerKey) {
        async { exchangeChecked(adapter, config, command) }
    }
    adapter.awaitWaiterCount(command, config.maxWaitersPerKey)

    exchangeChecked(adapter, config, command) shouldBeEqualTo waiterOverflowResponse(config)
    adapter.awaitWaiterCount(command, config.maxWaitersPerKey)
    adapter.sideEffectCount(command) shouldBeEqualTo 1

    adapter.completeOwner(command, createdResponse())
    owner.await() shouldBeEqualTo createdResponse().withReplayFlag(false)
    admitted.forEach { waiter ->
        waiter.await() shouldBeEqualTo createdResponse().withReplayFlag(true)
    }
}

private suspend fun assertCancellationReclaimsSlot(
    adapter: BoundedWaitHttpIdempotencyAdapter,
    config: BoundedWaitHttpIdempotencyConformanceConfig,
) = coroutineScope {
    val command = request(idempotencyKeys = listOf("waiter-cancel-key"))
    val owner = async { exchangeChecked(adapter, config, command) }
    adapter.awaitOwnerStarted(command)
    val cancelledWaiter = async { exchangeChecked(adapter, config, command) }
    adapter.awaitWaiterCount(command, 1)

    cancelledWaiter.cancelAndJoin()
    cancelledWaiter.isCancelled.shouldBeTrue()
    adapter.awaitWaiterCount(command, 0)

    val replacement = async { exchangeChecked(adapter, config, command) }
    adapter.awaitWaiterCount(command, 1)
    adapter.completeOwner(command, createdResponse())
    owner.await() shouldBeEqualTo createdResponse().withReplayFlag(false)
    replacement.await() shouldBeEqualTo createdResponse().withReplayFlag(true)
    adapter.sideEffectCount(command) shouldBeEqualTo 1
}

private suspend fun assertTransientAbandonElectsOneRetryOwner(
    adapter: BoundedWaitHttpIdempotencyAdapter,
    config: BoundedWaitHttpIdempotencyConformanceConfig,
) = coroutineScope {
    val command = request(idempotencyKeys = listOf("abandon-key"))
    val owner = async { exchangeChecked(adapter, config, command) }
    adapter.awaitOwnerStarted(command)
    val waiter = async { exchangeChecked(adapter, config, command) }
    adapter.awaitWaiterCount(command, 1)

    adapter.abandonOwner(command, transientFailureResponse())
    owner.await() shouldBeEqualTo transientFailureResponse()
    waiter.await() shouldBeEqualTo transientFailureResponse()
    adapter.sideEffectCount(command) shouldBeEqualTo 1

    val retries = List(2) { async { exchangeChecked(adapter, config, command) } }
    adapter.awaitOwnerStarted(command)
    adapter.awaitWaiterCount(command, 1)
    adapter.sideEffectCount(command) shouldBeEqualTo 2
    adapter.completeOwner(command, createdResponse())

    retries.map { retry ->
        checkNotNull(retry.await().headers["idempotency-replayed"]?.single())
    }.sorted() shouldBeEqualTo listOf("false", "true")
    adapter.sideEffectCount(command) shouldBeEqualTo 2
}

private suspend fun assertOwnerDisconnectBeforeAndAfterCommit(
    adapter: BoundedWaitHttpIdempotencyAdapter,
    config: BoundedWaitHttpIdempotencyConformanceConfig,
) = coroutineScope {
    val beforeCommit = request(idempotencyKeys = listOf("disconnect-before-key"))
    val disconnectedOwner = async { exchangeChecked(adapter, config, beforeCommit) }
    adapter.awaitOwnerStarted(beforeCommit)
    disconnectedOwner.cancelAndJoin()
    disconnectedOwner.isCancelled.shouldBeTrue()
    adapter.quiescence() shouldBeEqualTo HttpIdempotencyQuiescence(0, 0, 0)

    val replacement = async { exchangeChecked(adapter, config, beforeCommit) }
    adapter.awaitOwnerStarted(beforeCommit)
    adapter.completeOwner(beforeCommit, createdResponse())
    replacement.await() shouldBeEqualTo createdResponse().withReplayFlag(false)
    adapter.sideEffectCount(beforeCommit) shouldBeEqualTo 2

    val afterCommit = request(idempotencyKeys = listOf("disconnect-after-key"))
    adapter.holdOwnerResponseDelivery(afterCommit)
    val committedOwner = async { exchangeChecked(adapter, config, afterCommit) }
    adapter.awaitOwnerStarted(afterCommit)
    adapter.completeOwner(afterCommit, createdResponse())
    committedOwner.cancelAndJoin()
    committedOwner.isCancelled.shouldBeTrue()
    adapter.releaseOwnerResponseDelivery(afterCommit)
    exchangeChecked(adapter, config, afterCommit) shouldBeEqualTo createdResponse().withReplayFlag(true)
    adapter.sideEffectCount(afterCommit) shouldBeEqualTo 1
}
