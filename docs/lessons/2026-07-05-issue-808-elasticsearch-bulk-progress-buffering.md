# Issue 808 - Elasticsearch Bulk Progress Buffering

## Context

`bulkProgressListener` used `Channel.UNLIMITED`, so slow or absent collectors could retain bulk requests, responses, failures, and caller contexts without a hard bound.

## Decision

Use a bounded `Channel` with default capacity 256 and `BufferOverflow.SUSPEND`. Keep listener callbacks non-blocking by using `trySend`, and log failed sends so overflow is visible.

## Outcome

The progress listener retains a finite number of events by default, exposes capacity and overflow tuning, and has a regression test for overflow behavior without requiring Elasticsearch I/O.

## Future Guidance

Listener-to-Flow adapters should avoid unbounded channels unless the caller explicitly opts in. Prefer bounded buffers, non-blocking callback paths, and visible drop/overflow behavior.
