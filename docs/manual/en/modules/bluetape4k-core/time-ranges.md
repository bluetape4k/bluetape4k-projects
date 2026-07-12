---
title: Time and ranges
description: Handle inclusive boundaries, overlap, and timezone conversion explicitly.
manualId: bluetape4k-core
chapterId: time-ranges
---

# Time and ranges

## Problem to solve

Implicit end boundaries and timezones create duplicates or gaps at range edges.

## Mental model

An Instant is a point on the timeline; a local date or time does not identify that point without a timezone. A range boundary contract matters as much as its type.

## Smallest API surface

Use the current Core range and time extensions while keeping standard `java.time` types as the primary representation.

## Complete example

Store UTC `Instant` values, derive a query range in the business timezone, and lock inclusive or exclusive end behavior with a test.

## Selection guide

Use Instant for storage and transport, and a zoned conversion boundary for user schedules. Do not convert date-only business rules to Instant too early.

## Failure, cancellation, and lifecycle contract

Handle empty and reversed ranges at construction. Do not leave daylight-saving gaps and overlaps to the system default timezone.

## Operations and diagnosis

Include the timezone or UTC marker in logs and metrics so clock representations can be compared.

## Source and representative tests

Confirm the public API against [`javatimes`](../../../../../bluetape4k/core/src/main/kotlin/io/bluetape4k/javatimes), [`ranges`](../../../../../bluetape4k/core/src/main/kotlin/io/bluetape4k/ranges), and their tests.

## Next chapter and runnable workshop

See [Validation and invariants](./validation.md) for boundary checks and [Recipes](./recipes.md) for composition.
