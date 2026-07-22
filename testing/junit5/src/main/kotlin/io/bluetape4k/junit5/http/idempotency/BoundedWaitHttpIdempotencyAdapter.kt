package io.bluetape4k.junit5.http.idempotency

import java.time.Duration

/**
 * Connects a caller-owned HTTP test application to the bounded-wait conformance runner.
 *
 * The runner invokes this adapter in the caller's structured scope. The caller closes the HTTP
 * application and any blocking dispatcher after the runner returns. Implementations must preserve
 * cancellation and must never log raw keys, request bodies, responses, or headers.
 */
interface BoundedWaitHttpIdempotencyAdapter {

    /** Sends one synthetic request through the real framework HTTP boundary. */
    suspend fun exchange(request: HttpIdempotencyRequest): HttpIdempotencyResponse

    /** Suspends until [request] owns one business execution. */
    suspend fun awaitOwnerStarted(request: HttpIdempotencyRequest)

    /** Suspends until exactly [expected] same-fingerprint waiters are registered for [request]. */
    suspend fun awaitWaiterCount(request: HttpIdempotencyRequest, expected: Int)

    /** Releases the owner of [request] with a terminal replayable [outcome]. */
    suspend fun completeOwner(request: HttpIdempotencyRequest, outcome: HttpIdempotencyResponse)

    /**
     * Arms a response-delivery hold before [request] starts.
     *
     * The owner remains suspended after its terminal outcome commits until the hold is released or
     * the owner is cancelled. Cancellation and [resetScenario] must reclaim the hold exactly once.
     */
    suspend fun holdOwnerResponseDelivery(request: HttpIdempotencyRequest)

    /** Releases a response delivery previously held by [holdOwnerResponseDelivery]. */
    suspend fun releaseOwnerResponseDelivery(request: HttpIdempotencyRequest)

    /** Releases the owner of [request] with a transient non-replayable [outcome]. */
    suspend fun abandonOwner(request: HttpIdempotencyRequest, outcome: HttpIdempotencyResponse)

    /** Advances only the adapter-owned behavioral virtual clock by [duration]. */
    suspend fun advanceTimeBy(duration: Duration)

    /** Clears scenario-owned records, waiters, gates, and child work cooperatively. */
    suspend fun resetScenario()

    /** Returns committed business executions for [request]'s server-resolved scope. */
    fun sideEffectCount(request: HttpIdempotencyRequest): Int

    /** Reports adapter-owned resources that must be zero after scenario cleanup. */
    fun quiescence(): HttpIdempotencyQuiescence
}
