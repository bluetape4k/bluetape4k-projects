# Virtual Thread State Flake Lessons

## Context

After PR #427 merged, the develop push `Examples` workflow failed in
`Example4_VirtualThreadFactory`. The failing assertion expected a newly started
virtual thread to be `RUNNABLE`, but the runner observed `TIMED_WAITING`.

## Decision or Finding

Virtual thread scheduler state is an observation, not a stable contract. Tests
must not assert a single transient `Thread.State` immediately after `start()`.
Use lifecycle handshakes such as `CountDownLatch`, or a bluetape4k-junit5
concurrency harness when the behavior under test is concurrent execution.

## Outcome

The example now proves the stable lifecycle contract:

- the factory creates an unstarted virtual thread,
- the thread body starts and signals entry,
- the thread remains alive while the test holds a release latch,
- the thread terminates after release and `join`.

## Verification

Use targeted verification for this failure mode:

- `repo-test-summary -- ./gradlew :bluetape4k-examples-virtualthreads-demo:test --tests "io.bluetape4k.examples.virtualthreads.part1.Example4_VirtualThreadFactory" --rerun-tasks`

## Future Guidance

For virtual-thread examples, prefer deterministic lifecycle signals over
transient scheduler state assertions. `RUNNABLE`, `WAITING`, and
`TIMED_WAITING` can all be valid observations depending on runner timing.
