---
title: Core recipes
description: Compose validation, bounded history, aggregation, and deterministic shutdown.
manualId: bluetape4k-core
chapterId: recipes
---

# Core recipes

## Problem to solve

Connect boundary validation, bounded state, and cleanup as one workflow instead of listing utilities.

## Mental model

A recipe fixes the input invariant, data representation, capacity, failure surface, and shutdown order together.

## Smallest API surface

Combine only the required subset of `require*`, `RingBuffer` or `BoundedStack`, `ConcurrentReducer`, and `ShutdownQueue`.

## Complete example

Validate an event, add it to bounded recent history, aggregate it through a reducer, and close resources in reverse order through a shutdown queue.

## Selection guide

Prefer standard Kotlin and JDK APIs when they are sufficient. Add a Bluetape helper when it makes a repeated contract clearer.

## Failure, cancellation, and lifecycle contract

Start side effects only after validation and expose rejected aggregation and shutdown failures to the caller.

## Operations and diagnosis

Treat capacity, eviction, rejection, and cleanup latency as required signals for the recipe.

## Source and representative tests

Each recipe is grounded in Core source and representative tests under `bluetape4k/core/src/test/kotlin`.

## Next chapter and runnable workshop

Return to the preceding chapters for detailed contracts and verify composition with a small integration test in the consumer module.
