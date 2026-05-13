# DAEAD Chunk and Virtual Thread Follow-up Lessons

## Context

Post-merge review of PRs #242, #264, #271, and #293 found that the DAEAD chunk
format authenticated each chunk independently but did not bind chunk order or a
final-frame marker. The same review also found that Virtual Thread tests covered
direct fork/join flows but did not use the bluetape4k-junit5 stress harnesses.

## Decision or Finding

Security-sensitive chunked encryption must authenticate stream structure, not
only each ciphertext payload. Per-frame DAEAD associated data must include at
least the caller-provided associated data, a monotonically increasing chunk
index, and final-frame state. Tests must include reorder, duplicate,
whole-frame truncation, and trailing-data-after-final cases.

For Virtual Thread and StructuredTaskScope changes, direct examples are not
enough. Add `StructuredTaskScopeTester`, `MultithreadingTester`, or
`SuspendedJobTester` coverage when the behavior depends on concurrency,
coroutine scheduling, or virtual-thread propagation.

## Outcome

- DAEAD chunk frames now carry a final flag and bind chunk index/final state into
  DAEAD associated data. The v2 frame format is intentionally not
  wire-compatible with the previous length-only frames.
- DAEAD tests now reject reordered, duplicated, final-frame-dropped, and
  trailing-data-after-final streams.
- `StructuredTaskScopeTester` coverage was added for `StructuredTaskScopes` and
  `TaskContext` propagation.
- StructuredTaskScope provider discovery now skips broken ServiceLoader entries
  instead of stopping discovery.

## Verification

Targeted verification should include:

- `repo-test-summary -- ./gradlew :bluetape4k-okio:test --tests "io.bluetape4k.okio.tink.DaeadChunk*Test"`
- `repo-test-summary -- ./gradlew :bluetape4k-virtualthread-api:test --tests "io.bluetape4k.concurrent.virtualthread.*Test"`
- `git diff --check`

## Future Guidance

When adding framed encryption formats, include adversarial frame-level tests:
reorder, duplicate, drop final, truncate header/body, wrong associated data, and
trailing data after final. Keep empty payload coverage because empty streams
still need an authenticated final marker. When reviewing concurrency changes,
explicitly check whether the appropriate bluetape4k-junit5 harness is used;
otherwise add it in the follow-up PR rather than relying only on single-shot
examples.
