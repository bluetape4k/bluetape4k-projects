---
title: Cancellation, failures, testing, and operations
description: Future cancellation and server boundaries, close limitations, Testcontainers evidence, and production signals.
manualId: bluetape4k-pulsar
chapterId: cancellation-failures-testing-operations
---

# Cancellation, failures, testing, and operations

## Cancellation scope of suspend and Flow

Single-operation helpers await Pulsar `CompletableFuture` values with `awaitSuspending()`. Repeated adapters `sendAsFlow`, `receiveAsFlow`, and `readAsFlow` catch `CancellationException` while awaiting, call `cancel(true)` on the current future, and rethrow cancellation.

This lets the coroutine leave its wait. It does not prove that a send accepted by the broker was rolled back or that server-side receive and read work stopped. Treat possible send duplication and receive redelivery as normal distributed-system boundaries.

## Close failure and cancellation

Version 1.12.1 `withPulsarClient`, `withProducer`, `withConsumer`, and `withReader` await `closeAsync()` in `finally` and log a warning on failure.

There are two limits:

- Close failure is not rethrown, so a successful block does not prove a clean shutdown.
- Close await is not wrapped in `NonCancellable`, so cleanup completion is not guaranteed in an already-cancelled context.

These are release-source facts. Do not project the later branch's `PulsarCloseSupport` and cancellation cleanup tests back onto 1.12.1.

## Failure-handling principles

Keep Pulsar Client exceptions intact until a domain boundary can add stable meaning. Do not collapse timeout, authentication, authorization, schema rejection, producer queue exhaustion, and ack failure into one generic messaging error.

Before retrying, determine whether the operation is idempotent. A send timeout can leave broker acceptance unknown. A failure to ack after successful handling can cause redelivery. Use business keys and processing records to absorb duplicates.

## What release tests prove

`AbstractPulsarTest` starts a Testcontainers `PulsarContainer`. Release tests cover:

- Client creation by URL and setup-only configuration
- Normal paths through producer, consumer, and reader scope helpers
- Direct, message-DSL, and Flow sends and receives
- Individual ack, Exclusive cumulative ack, and Shared cumulative-ack rejection
- Jackson 2 and 3 encode/decode, clone, and broker round trips
- Earliest Reader backlog and an empty latest-position result

## What they do not prove

The 1.12.1 tests have no fixture proving that close completes while the block is cancelled. They also omit broker restarts, network partitions, authentication rotation, schema evolution, retry and dead-letter behavior, pressure limits, and long-running soak tests.

The full module suite uses a real container and is not needed for this documentation-only change. In application CI, serialize it with other Testcontainers suites and separate fast mapper round-trip tests first.

## Operations checklist

- Client connections and reconnect count
- Producer send latency, pending queue, timeout, and failure rate
- Consumer backlog, unacknowledged messages, redelivery, handler latency, and ack latency
- Reader start and processing positions and lag
- Schema rejection and payload decode failures
- Shutdown start and completion, in-flight count, and close failures

Keep metric labels to bounded topics, subscriptions, and result codes. Do not label with message keys, payloads, exception messages, or unbounded tenant IDs.

## Sources and tests

- [`PulsarClientSupport.kt`](../../../../../infra/pulsar/src/main/kotlin/io/bluetape4k/pulsar/PulsarClientSupport.kt)
- [`ProducerExtensions.kt`](../../../../../infra/pulsar/src/main/kotlin/io/bluetape4k/pulsar/producer/ProducerExtensions.kt)
- [`ConsumerExtensions.kt`](../../../../../infra/pulsar/src/main/kotlin/io/bluetape4k/pulsar/consumer/ConsumerExtensions.kt)
- [`ReaderExtensions.kt`](../../../../../infra/pulsar/src/main/kotlin/io/bluetape4k/pulsar/reader/ReaderExtensions.kt)
- [`AbstractPulsarTest.kt`](../../../../../infra/pulsar/src/test/kotlin/io/bluetape4k/pulsar/AbstractPulsarTest.kt)
