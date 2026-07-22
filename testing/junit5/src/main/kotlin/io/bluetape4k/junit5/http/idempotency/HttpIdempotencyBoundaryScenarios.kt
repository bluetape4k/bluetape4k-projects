@file:JvmSynthetic

package io.bluetape4k.junit5.http.idempotency

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotContain
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import java.io.Serializable

internal fun boundaryScenarios(): List<ConformanceScenario> = listOf(
    ConformanceScenario("retention-expiry") { adapter, config ->
        assertExpiryBeforeAtAndAfterBoundary(adapter, config)
        assertConcurrentRetryElectsOneOwnerAfterExpiry(adapter, config)
    },
    ConformanceScenario("ingress-bounds") { adapter, config ->
        assertOversizedKeyAndBodyRejectedThroughExchange(adapter, config)
        assertMalformedKeysRejectedBeforeOwnership(adapter, config)
        assertAmbiguousTupleRemainsDistinct(adapter, config)
        assertCanonicalPayloadBehavior(adapter, config)
    },
    ConformanceScenario("replay-snapshot-bounds") { adapter, config ->
        assertReplayHeaderDenylistAndAggregateBounds(adapter, config)
    },
    ConformanceScenario("repeated-fan-in") { adapter, config ->
        assertRepeatedFanInAcrossKeys(adapter, config)
    },
)

private suspend fun assertExpiryBeforeAtAndAfterBoundary(
    adapter: BoundedWaitHttpIdempotencyAdapter,
    config: BoundedWaitHttpIdempotencyConformanceConfig,
) {
    val before = request(idempotencyKeys = listOf("retention-before-key"))
    completeNewOwner(adapter, config, before)
    adapter.advanceTimeBy(config.retention.minusNanos(1))
    exchangeChecked(adapter, config, before) shouldBeEqualTo createdResponse().withReplayFlag(true)
    adapter.sideEffectCount(before) shouldBeEqualTo 1

    val at = request(idempotencyKeys = listOf("retention-at-key"))
    completeNewOwner(adapter, config, at)
    adapter.advanceTimeBy(config.retention)
    completeExpiredRetry(adapter, config, at)
    adapter.sideEffectCount(at) shouldBeEqualTo 2

    val after = request(idempotencyKeys = listOf("retention-after-key"))
    completeNewOwner(adapter, config, after)
    adapter.advanceTimeBy(config.retention.plusNanos(1))
    completeExpiredRetry(adapter, config, after)
    adapter.sideEffectCount(after) shouldBeEqualTo 2
}

private suspend fun assertConcurrentRetryElectsOneOwnerAfterExpiry(
    adapter: BoundedWaitHttpIdempotencyAdapter,
    config: BoundedWaitHttpIdempotencyConformanceConfig,
) = coroutineScope {
    val command = request(idempotencyKeys = listOf("retention-concurrent-key"))
    completeNewOwner(adapter, config, command)
    adapter.advanceTimeBy(config.retention)

    val retries = List(2) {
        async(start = CoroutineStart.UNDISPATCHED) { exchangeChecked(adapter, config, command) }
    }
    adapter.awaitOwnerStarted(command)
    adapter.awaitWaiterCount(command, 1)
    adapter.completeOwner(command, createdResponse())

    retries.map { retry ->
        checkNotNull(retry.await().headers["idempotency-replayed"]?.single())
    }.sorted() shouldBeEqualTo listOf("false", "true")
    adapter.sideEffectCount(command) shouldBeEqualTo 2
}

private suspend fun completeNewOwner(
    adapter: BoundedWaitHttpIdempotencyAdapter,
    config: BoundedWaitHttpIdempotencyConformanceConfig,
    command: HttpIdempotencyRequest,
) = coroutineScope {
    val owner = async(start = CoroutineStart.UNDISPATCHED) { exchangeChecked(adapter, config, command) }
    adapter.awaitOwnerStarted(command)
    adapter.completeOwner(command, createdResponse())
    owner.await() shouldBeEqualTo createdResponse().withReplayFlag(false)
}

private suspend fun completeExpiredRetry(
    adapter: BoundedWaitHttpIdempotencyAdapter,
    config: BoundedWaitHttpIdempotencyConformanceConfig,
    command: HttpIdempotencyRequest,
) = coroutineScope {
    val owner = async(start = CoroutineStart.UNDISPATCHED) { exchangeChecked(adapter, config, command) }
    adapter.awaitOwnerStarted(command)
    adapter.completeOwner(command, createdResponse())
    owner.await() shouldBeEqualTo createdResponse().withReplayFlag(false)
}

private suspend fun assertOversizedKeyAndBodyRejectedThroughExchange(
    adapter: BoundedWaitHttpIdempotencyAdapter,
    config: BoundedWaitHttpIdempotencyConformanceConfig,
) {
    val oversizedBody = request(
        idempotencyKeys = listOf("oversized-body-key"),
        requestBody = "x".repeat(config.maxRequestBodyBytes + 1),
    )
    exchangeChecked(adapter, config, oversizedBody, allowConfiguredRequestOverflow = true) shouldBeEqualTo
            oversizedRequestResponse()
    adapter.sideEffectCount(oversizedBody) shouldBeEqualTo 0

    val oversizedKey = request(idempotencyKeys = listOf("k".repeat(config.maxIdempotencyKeyBytes + 1)))
    exchangeChecked(adapter, config, oversizedKey, allowConfiguredRequestOverflow = true) shouldBeEqualTo
            invalidIdempotencyRequestResponse()
    adapter.sideEffectCount(oversizedKey) shouldBeEqualTo 0
}

private suspend fun assertMalformedKeysRejectedBeforeOwnership(
    adapter: BoundedWaitHttpIdempotencyAdapter,
    config: BoundedWaitHttpIdempotencyConformanceConfig,
) {
    listOf(
        listOf("duplicate-a", "duplicate-b"),
        listOf(" "),
        listOf("\t"),
        listOf("é"),
    ).forEachIndexed { index, keys ->
        val malformed = request(resourceIdentity = "malformed-$index", idempotencyKeys = keys)
        exchangeChecked(adapter, config, malformed) shouldBeEqualTo invalidIdempotencyRequestResponse()
        adapter.sideEffectCount(malformed) shouldBeEqualTo 0
    }
}

private suspend fun assertAmbiguousTupleRemainsDistinct(
    adapter: BoundedWaitHttpIdempotencyAdapter,
    config: BoundedWaitHttpIdempotencyConformanceConfig,
) {
    val first = request(resourceIdentity = "ab", idempotencyKeys = listOf("c"))
    val second = request(resourceIdentity = "a", idempotencyKeys = listOf("bc"))
    completeNewOwner(adapter, config, first)
    completeNewOwner(adapter, config, second)
    adapter.sideEffectCount(first) shouldBeEqualTo 1
    adapter.sideEffectCount(second) shouldBeEqualTo 1
}

private suspend fun assertCanonicalPayloadBehavior(
    adapter: BoundedWaitHttpIdempotencyAdapter,
    config: BoundedWaitHttpIdempotencyConformanceConfig,
) {
    val first = request(
        idempotencyKeys = listOf("canonical-json-key"),
        requestBody = "{\"b\":2,\"a\":1}",
    )
    completeNewOwner(adapter, config, first)
    val reordered = first.copy(requestBody = "{\"a\":1,\"b\":2}")
    exchangeChecked(adapter, config, reordered) shouldBeEqualTo createdResponse().withReplayFlag(true)
    adapter.sideEffectCount(first) shouldBeEqualTo 1

    val nested = request(
        idempotencyKeys = listOf("nested-canonical-json-key"),
        requestBody = """{"o":{"b":2,"a":1},"x":[{"z":0,"a":"a,b}"}],"l":"\u0061"}""",
    )
    completeNewOwner(adapter, config, nested)
    val nestedReordered = nested.copy(
        requestBody = """{"l":"a","x":[{"a":"a,b}","z":0.0}],"o":{"a":1.0,"b":2e0}}""",
    )
    exchangeChecked(adapter, config, nestedReordered) shouldBeEqualTo createdResponse().withReplayFlag(true)

    val escapedUnicode = request(
        idempotencyKeys = listOf("escaped-unicode-key"),
        requestBody = """{"emoji":"\uD83D\uDE00"}""",
    )
    completeNewOwner(adapter, config, escapedUnicode)
    exchangeChecked(adapter, config, escapedUnicode.copy(requestBody = """{"emoji":"😀"}""")) shouldBeEqualTo
            createdResponse().withReplayFlag(true)

    val orderedArray = request(
        idempotencyKeys = listOf("ordered-array-key"),
        requestBody = """{"items":[1,2]}""",
    )
    completeNewOwner(adapter, config, orderedArray)
    exchangeChecked(adapter, config, orderedArray.copy(requestBody = """{"items":[2,1]}""")) shouldBeEqualTo
            idempotencyConflictResponse()

    listOf(
        "{\"name\":\"first\",\"name\":\"second\"}",
        "{\"outer\":{\"name\":1,\"name\":2}}",
        "{\"missing\":true",
        "{\"trailing\":true,}",
        "{\"badEscape\":\"\\x\"}",
        """{"unpaired":"\uD800"}""",
    ).forEachIndexed { index, body ->
        val invalid = request(
            idempotencyKeys = listOf("invalid-json-$index"),
            requestBody = body,
        )
        exchangeChecked(adapter, config, invalid) shouldBeEqualTo invalidIdempotencyRequestResponse()
        adapter.sideEffectCount(invalid) shouldBeEqualTo 0
    }
}

private suspend fun assertReplayHeaderDenylistAndAggregateBounds(
    adapter: BoundedWaitHttpIdempotencyAdapter,
    config: BoundedWaitHttpIdempotencyConformanceConfig,
) = coroutineScope {
    val safe = request(idempotencyKeys = listOf("safe-replay-key"))
    val safeOwner = async { exchangeChecked(adapter, config, safe) }
    adapter.awaitOwnerStarted(safe)
    adapter.completeOwner(
        safe,
        createdResponse().copy(
            headers = mapOf(
                "Content-Type" to listOf("application/json"),
                "ETag" to listOf("widget-v1"),
                "Authorization" to listOf("secret-authorization"),
                "Cookie" to listOf("secret-cookie"),
                "Set-Cookie" to listOf("secret-set-cookie"),
                "Proxy-Authorization" to listOf("secret-proxy-authorization"),
                "WWW-Authenticate" to listOf("secret-challenge"),
                "Connection" to listOf("X-Hop"),
                "X-Hop" to listOf("hop-by-hop"),
                "X-Api-Key" to listOf("secret-api-key"),
                "Authentication-Info" to listOf("secret-authentication-info"),
                "Proxy-Authenticate" to listOf("secret-proxy-challenge"),
                "Proxy-Authentication-Info" to listOf("secret-proxy-info"),
                "Keep-Alive" to listOf("timeout=5"),
                "TE" to listOf("trailers"),
                "Trailer" to listOf("X-Trailer"),
                "Transfer-Encoding" to listOf("chunked"),
                "Upgrade" to listOf("websocket"),
                "X-Credential" to listOf("secret-credential"),
                "X-Secret" to listOf("secret-value"),
                "Session-Token" to listOf("secret-token"),
                "Client-Api-Key" to listOf("secret-client-key"),
            ),
        ),
    )
    val first = safeOwner.await()
    val replay = exchangeChecked(adapter, config, safe)
    listOf(first, replay).forEach { response ->
        response.headers["content-type"] shouldBeEqualTo listOf("application/json")
        response.headers["etag"] shouldBeEqualTo listOf("widget-v1")
        PROHIBITED_REPLAY_HEADERS.forEach { name -> response.headers.keys.shouldNotContain(name) }
    }

    val nominatedContentType = request(idempotencyKeys = listOf("nominated-content-type-key"))
    val nominatedOwner = async { exchangeChecked(adapter, config, nominatedContentType) }
    adapter.awaitOwnerStarted(nominatedContentType)
    adapter.completeOwner(
        nominatedContentType,
        createdResponse().copy(headers = mapOf(
            "Content-Type" to listOf("application/json"),
            "Connection" to listOf("Content-Type"),
            "ETag" to listOf("widget-v2"),
        )),
    )
    nominatedOwner.await().also { response ->
        response.headers.keys.shouldNotContain("content-type")
        response.headers["etag"] shouldBeEqualTo listOf("widget-v2")
    }

    val unsafeOutcomes = listOf(
        createdResponse().copy(body = "x".repeat(config.maxReplayBodyBytes + 1)),
        createdResponse().copy(headers = mapOf(
            "content-type" to listOf("x".repeat(config.maxReplayHeaderValueBytes + 1)),
        )),
        createdResponse().copy(headers = mapOf(
            "content-type" to List(config.maxReplayValuesPerHeader + 1) { "value-$it" },
        )),
        createdResponse().copy(headers = buildMap {
            put("content-type", listOf("application/json"))
            config.replayHeaderAllowlist
                .filter { name -> name.startsWith("x-safe-") }
                .take(config.maxReplayHeaderNames)
                .forEach { name ->
                    put(name, listOf("value"))
                }
        }),
        createdResponse().copy(headers = mapOf(
            "content-type" to List(config.maxReplayValuesPerHeader) {
                "x".repeat(config.maxReplayHeaderValueBytes)
            },
        )),
    )
    unsafeOutcomes.forEachIndexed { index, outcome ->
        val command = request(idempotencyKeys = listOf("unsafe-snapshot-$index"))
        val owner = async { exchangeChecked(adapter, config, command) }
        adapter.awaitOwnerStarted(command)
        adapter.completeOwner(command, outcome)
        owner.await() shouldBeEqualTo unsafeReplaySnapshotResponse()
        adapter.sideEffectCount(command) shouldBeEqualTo 0

        completeNewOwner(adapter, config, command)
        adapter.sideEffectCount(command) shouldBeEqualTo 1
    }
}

private suspend fun assertRepeatedFanInAcrossKeys(
    adapter: BoundedWaitHttpIdempotencyAdapter,
    config: BoundedWaitHttpIdempotencyConformanceConfig,
) {
    repeat(5) { round ->
        coroutineScope {
            val keys = List(3) { index -> "fan-in-$round-$index" }
            val completions = Channel<CompletedAttempt>(Channel.UNLIMITED)
            val owners = keys.map { key ->
                async(start = CoroutineStart.UNDISPATCHED) {
                    exchangeChecked(adapter, config, request(idempotencyKeys = listOf(key)))
                }
            }
            keys.forEach { key -> adapter.awaitOwnerStarted(request(idempotencyKeys = listOf(key))) }

            val attempts = keys.mapIndexed { keyIndex, key ->
                List(config.maxWaitersPerKey + 2) { attemptIndex ->
                    async(start = CoroutineStart.UNDISPATCHED) {
                        exchangeChecked(adapter, config, request(idempotencyKeys = listOf(key))).also { response ->
                            completions.send(CompletedAttempt(keyIndex, attemptIndex, response))
                        }
                    }
                }
            }
            keys.forEach { key ->
                adapter.awaitWaiterCount(request(idempotencyKeys = listOf(key)), config.maxWaitersPerKey)
            }

            val overflowByIndex = List(keys.size * 2) { completions.receive() }
                .groupBy(CompletedAttempt::keyIndex)
            keys.indices.forEach { keyIndex ->
                overflowByIndex.getValue(keyIndex).also { completed ->
                    completed shouldHaveSize 2
                }.forEach { completed ->
                    attempts[keyIndex][completed.attemptIndex].await()
                    val response = completed.response
                    response.statusCode shouldBeEqualTo 429
                    response.problemCode shouldBeEqualTo "idempotency_waiters_exceeded"
                    response.headers["retry-after"] shouldBeEqualTo
                            listOf(config.overflowRetryAfter.seconds.toString())
                }
            }

            adapter.completeOwner(request(idempotencyKeys = listOf(keys.first())), createdResponse())
            attempts.first().awaitAll()
            owners.first().await().statusCode shouldBeEqualTo 201
            owners.drop(1).forEach { owner -> owner.isCompleted.shouldBeFalse() }
            attempts.drop(1).forEachIndexed { offset, deferred ->
                val keyIndex = offset + 1
                val overflowIndexes = overflowByIndex.getValue(keyIndex)
                    .mapTo(mutableSetOf(), CompletedAttempt::attemptIndex)
                deferred.forEachIndexed { attemptIndex, attempt ->
                    if (attemptIndex in overflowIndexes) {
                        attempt.isCompleted.shouldBeTrue()
                    } else {
                        attempt.isCompleted.shouldBeFalse()
                    }
                }
            }
            keys.drop(1).forEach { key ->
                adapter.completeOwner(request(idempotencyKeys = listOf(key)), createdResponse())
            }

            owners.awaitAll().forEach { response -> response.statusCode shouldBeEqualTo 201 }
            attempts.forEachIndexed { keyIndex, deferred ->
                val observed = deferred.awaitAll()
                observed.count { response -> response.statusCode == 429 } shouldBeEqualTo 2
                observed.count { response -> response.statusCode == 201 } shouldBeEqualTo config.maxWaitersPerKey
                observed.filter { response -> response.statusCode == 201 }.forEach { response ->
                    response.headers["idempotency-replayed"] shouldBeEqualTo listOf("true")
                }
                adapter.sideEffectCount(request(idempotencyKeys = listOf(keys[keyIndex]))) shouldBeEqualTo 1
            }
            completions.cancel()
        }
    }
    adapter.quiescence() shouldBeEqualTo HttpIdempotencyQuiescence(0, 0, 0)
}

private data class CompletedAttempt(
    val keyIndex: Int,
    val attemptIndex: Int,
    val response: HttpIdempotencyResponse,
): Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

private val PROHIBITED_REPLAY_HEADERS = setOf(
    "authentication-info",
    "authorization",
    "client-api-key",
    "connection",
    "cookie",
    "keep-alive",
    "proxy-authenticate",
    "proxy-authentication-info",
    "proxy-authorization",
    "session-token",
    "set-cookie",
    "te",
    "trailer",
    "transfer-encoding",
    "upgrade",
    "www-authenticate",
    "x-api-key",
    "x-credential",
    "x-hop",
    "x-secret",
)
