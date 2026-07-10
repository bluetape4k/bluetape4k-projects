# Issue #785: Near-cache retry failure compensation

## Context

Bounded queue overflow and close draining were already handled, but a backend
write could still exhaust all retries after the public write-behind operation
had updated the front cache. This left uncommitted front values, permanent
tombstones, or a permanently enabled `clearPending` flag.

## Decision

- Keep the existing write-behind API and compensate terminal backend failures.
- Invalidate failed `Put` front entries so later reads repopulate from the back cache.
- Release failed `Remove` tombstones and failed `ClearBack` read blocking.
- Serialize queue acceptance with optimistic front-state mutation so the consumer
  cannot complete a command before its local state is installed.
- Associate each accepted command with ownership tokens. A stale completion must
  not compensate state installed by a newer command for the same key or clear.
- Apply token completion and compensation under the same lock or mutex used by
  enqueue state installation, and snapshot caller-owned bulk collections.
- Guard read-through population with a monotonic state version so a backend read
  started before a newer mutation cannot overwrite the newer front state.
- Serialize synchronous replace with pending write state and advance the same
  version so delayed reads cannot restore the replaced value.
- Preserve the logical value in mutation tokens so `putIfAbsent` can return an
  in-flight Put value or enqueue after an in-flight remove/clear without reordering.
- Compare mutation and clear command sequences so a newer clear supersedes an
  older pending Put when evaluating `putIfAbsent`.

## Outcome

Blocking and suspend implementations now converge to observable backend state
after retry exhaustion instead of retaining a permanently divergent local view.
Terminal failures remain logged because an asynchronous write-behind call cannot
retroactively fail after it has returned.

## Verification

- RED: six retry-exhaustion tests failed with `ConditionTimeoutException` before the fix.
- GREEN: the same six tests passed after compensation was implemented.
- RED: mutable `PutAll` inputs terminated both consumers before snapshotting was added.
- `MultithreadingTester` and `SuspendedJobTester` verified mixed same-key
  put/remove/clear completion against the final backend state.
- Deterministic blocked-read tests verified stale read-through results are not
  repopulated after a concurrent put or replace.
- Queued-remove tests verified replace cannot bypass a pending tombstone.
- Queued remove/clear tests verified `putIfAbsent` preserves mutation order.
- Pending Put then Clear tests verified the newer clear wins before `putIfAbsent`.
- Both resilient near-cache test classes: 68 tests passed.
- `:bluetape4k-cache-core:test`: 502 tests passed.

## Future Guard

When optimistic state is associated with queued asynchronous work, completion
and compensation must be conditional on command ownership. Do not use an
unversioned `remove` or boolean reset that can overwrite a newer accepted state.
