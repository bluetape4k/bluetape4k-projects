---
title: Operations and observability
description: Connect jobs, queues, latency, cancellation, and readiness to operational signals.
manualId: bluetape4k-coroutines
chapterId: operations
---

# Operations and observability

## Problem to solve

Observe which work is delayed, cancelled, or unable to terminate instead of counting coroutines alone.

## Mental model

Active jobs represent demand, queues and buffers represent pressure, latency represents service time, and cancellation represents normal termination or caller abandonment.

## Smallest API surface

Combine trace propagation from coroutine context, Micrometer timers and counters, component readiness, and lifecycle hooks.

## Complete example

Record suspend work below the request span and rethrow `CancellationException`. During component shutdown, stop intake before closing channels and owned scopes.

## Selection guide

Readiness means the component can accept new work; liveness means the process can recover. A full queue cannot be diagnosed from CPU utilization alone.

## Failure, cancellation, and lifecycle contract

Do not record normal cancellation as an error span. After a shutdown timeout, close remaining work and external resources explicitly.

## Operations and diagnosis

Put P50/P95/P99 latency, in-flight work, queue depth, timeouts, and cancellation reasons on the same dashboard.

## Source and representative tests

Verify owned scopes and Subjects with the current source and the `observability/micrometer-tracing-coroutines` workshop.

## Next chapter and runnable workshop

Use that observability workshop and review [Lifecycle and cancellation](./lifecycle.md) for shutdown ownership.
