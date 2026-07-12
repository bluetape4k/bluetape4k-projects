---
title: Subjects and event contracts
description: Distinguish events, latest state, replay, fan-out, and work-sharing contracts.
manualId: bluetape4k-coroutines
chapterId: subjects
---

# Subjects and event contracts

## Problem to solve

When adapting callbacks or external events to Flow, define what subscribers receive and how much state is retained.

## Mental model

Publish represents new events, Behavior the latest state, Replay bounded history, Multicast coordinated fan-out, and UnicastWork work distribution among consumers.

## Smallest API surface

Select one of `PublishSubject`, `BehaviorSubject`, `ReplaySubject`, `MulticastSubject`, and `UnicastWorkSubject` by delivery contract.

## Complete example

A callback bridge starts its collector, waits with `awaitCollector()`, forwards events, and closes both the subject and callback source with `complete()` or `error()`.

## Selection guide

Ask whether every subscriber receives the same item, whether late subscribers need state or history, and whether one worker should claim each item.

## Failure, cancellation, and lifecycle contract

Additional terminal calls do not reverse a terminal state. Do not treat history or buffer capacity as unbounded.

## Operations and diagnosis

Observe collector count, dropped or queued events, terminal state, and replay size. For startup loss, inspect `awaitCollector()` ordering first.

## Source and representative tests

The evidence is the [`subject`](../../../../../bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/flow/extensions/subject) implementation and its tests.

## Next chapter and runnable workshop

Use `flow-extensions-subject-bridge` for callback adaptation and continue to [Operations](./operations.md) for production signals.
