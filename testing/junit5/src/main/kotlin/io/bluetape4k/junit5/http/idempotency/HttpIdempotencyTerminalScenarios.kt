package io.bluetape4k.junit5.http.idempotency

import io.bluetape4k.assertions.shouldBeEqualTo
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

internal fun terminalScenarios(): List<ConformanceScenario> = listOf(
    ConformanceScenario("first-request") { adapter, config -> assertFirstRequest(adapter, config) },
    ConformanceScenario("terminal-replay") { adapter, config -> assertFirstAndReplay(adapter, config) },
    ConformanceScenario("payload-conflict") { adapter, config ->
        assertDifferentPayloadConflictIsImmediate(adapter, config)
    },
    ConformanceScenario("tenant-isolation") { adapter, config -> assertCrossTenantIsolation(adapter, config) },
    ConformanceScenario("authorization-before-lookup") { adapter, config ->
        assertUnauthorizedIsRecordIndistinguishable(adapter, config)
    },
    ConformanceScenario("terminal-failure-replay") { adapter, config ->
        assertDeterministicTerminalFailureReplay(adapter, config)
    },
)

private suspend fun assertFirstRequest(
    adapter: BoundedWaitHttpIdempotencyAdapter,
    config: BoundedWaitHttpIdempotencyConformanceConfig,
) = coroutineScope {
    val command = request(idempotencyKeys = listOf("first-key"))
    val owner = async { exchangeChecked(adapter, config, command) }

    adapter.awaitOwnerStarted(command)
    adapter.sideEffectCount(command) shouldBeEqualTo 1
    adapter.completeOwner(command, createdResponse())

    owner.await() shouldBeEqualTo createdResponse().withReplayFlag(false)
    adapter.sideEffectCount(command) shouldBeEqualTo 1
}

private suspend fun assertFirstAndReplay(
    adapter: BoundedWaitHttpIdempotencyAdapter,
    config: BoundedWaitHttpIdempotencyConformanceConfig,
) = coroutineScope {
    val command = request(idempotencyKeys = listOf("replay-key"))
    val owner = async { exchangeChecked(adapter, config, command) }

    adapter.awaitOwnerStarted(command)
    adapter.completeOwner(command, createdResponse())
    owner.await() shouldBeEqualTo createdResponse().withReplayFlag(false)
    exchangeChecked(adapter, config, command) shouldBeEqualTo createdResponse().withReplayFlag(true)
    adapter.sideEffectCount(command) shouldBeEqualTo 1
}

private suspend fun assertDifferentPayloadConflictIsImmediate(
    adapter: BoundedWaitHttpIdempotencyAdapter,
    config: BoundedWaitHttpIdempotencyConformanceConfig,
) = coroutineScope {
    val ownerCommand = request(idempotencyKeys = listOf("conflict-key"), requestBody = "A")
    val conflict = ownerCommand.copy(requestBody = "B")
    val owner = async { exchangeChecked(adapter, config, ownerCommand) }

    adapter.awaitOwnerStarted(ownerCommand)
    exchangeChecked(adapter, config, conflict) shouldBeEqualTo idempotencyConflictResponse()
    adapter.sideEffectCount(ownerCommand) shouldBeEqualTo 1
    adapter.completeOwner(ownerCommand, createdResponse())
    owner.await() shouldBeEqualTo createdResponse().withReplayFlag(false)
    exchangeChecked(adapter, config, ownerCommand) shouldBeEqualTo createdResponse().withReplayFlag(true)
    adapter.sideEffectCount(ownerCommand) shouldBeEqualTo 1
}

private suspend fun assertCrossTenantIsolation(
    adapter: BoundedWaitHttpIdempotencyAdapter,
    config: BoundedWaitHttpIdempotencyConformanceConfig,
) = coroutineScope {
    val tenantA = request(
        authenticationProfile = "tenant-a-principal",
        idempotencyKeys = listOf("shared-tenant-key"),
    )
    val tenantB = tenantA.copy(authenticationProfile = "tenant-b-principal")
    val ownerA = async { exchangeChecked(adapter, config, tenantA) }
    val ownerB = async { exchangeChecked(adapter, config, tenantB) }

    adapter.awaitOwnerStarted(tenantA)
    adapter.awaitOwnerStarted(tenantB)
    adapter.sideEffectCount(tenantA) shouldBeEqualTo 1
    adapter.sideEffectCount(tenantB) shouldBeEqualTo 1
    adapter.completeOwner(tenantA, createdResponse())
    adapter.completeOwner(tenantB, createdResponse())

    ownerA.await() shouldBeEqualTo createdResponse().withReplayFlag(false)
    ownerB.await() shouldBeEqualTo createdResponse().withReplayFlag(false)
    exchangeChecked(adapter, config, tenantA) shouldBeEqualTo createdResponse().withReplayFlag(true)
    exchangeChecked(adapter, config, tenantB) shouldBeEqualTo createdResponse().withReplayFlag(true)
    adapter.sideEffectCount(tenantA) shouldBeEqualTo 1
    adapter.sideEffectCount(tenantB) shouldBeEqualTo 1
}

private suspend fun assertUnauthorizedIsRecordIndistinguishable(
    adapter: BoundedWaitHttpIdempotencyAdapter,
    config: BoundedWaitHttpIdempotencyConformanceConfig,
) = coroutineScope {
    val authorized = request(idempotencyKeys = listOf("foreign-record-key"))
    val owner = async { exchangeChecked(adapter, config, authorized) }
    adapter.awaitOwnerStarted(authorized)
    adapter.completeOwner(authorized, createdResponse())
    owner.await()

    mapOf(
        "unauthenticated" to unauthenticatedResponse(),
        "tenant-a-read-only" to unauthorizedResponse(),
    ).forEach { (profile, expected) ->
        val present = authorized.copy(authenticationProfile = profile)
        val absent = present.copy(idempotencyKeys = listOf("absent-record-key"))

        val presentResponse = exchangeChecked(adapter, config, present)
        val absentResponse = exchangeChecked(adapter, config, absent)
        presentResponse shouldBeEqualTo expected
        absentResponse shouldBeEqualTo expected
        presentResponse shouldBeEqualTo absentResponse
        adapter.sideEffectCount(present) shouldBeEqualTo 0
        adapter.sideEffectCount(absent) shouldBeEqualTo 0
        adapter.quiescence().activeWaiters shouldBeEqualTo 0
    }
    adapter.sideEffectCount(authorized) shouldBeEqualTo 1
    adapter.quiescence() shouldBeEqualTo HttpIdempotencyQuiescence(0, 0, 0)
}

private suspend fun assertDeterministicTerminalFailureReplay(
    adapter: BoundedWaitHttpIdempotencyAdapter,
    config: BoundedWaitHttpIdempotencyConformanceConfig,
) = coroutineScope {
    val command = request(idempotencyKeys = listOf("terminal-failure-key"))
    val owner = async { exchangeChecked(adapter, config, command) }

    adapter.awaitOwnerStarted(command)
    adapter.completeOwner(command, deterministicFailureResponse())
    owner.await() shouldBeEqualTo deterministicFailureResponse().withReplayFlag(false)
    exchangeChecked(adapter, config, command) shouldBeEqualTo
            deterministicFailureResponse().withReplayFlag(true)
    adapter.sideEffectCount(command) shouldBeEqualTo 1
}
