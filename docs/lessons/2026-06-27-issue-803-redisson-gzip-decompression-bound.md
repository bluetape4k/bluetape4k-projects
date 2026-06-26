# Lessons Learned - Issue #803 Redisson GZip Decompression Bound (2026-06-27)

## Context

Issue #803 identified that `GzipCodec` decompressed Redis payloads through
`Compressors.GZip.decompress(bytes)`, and `GZipCompressor` used
`GZIPInputStream.readBytes()` with no decompressed-size bound.

## Decision

- Put the defensive size limit in `GZipCompressor`, not only in Redisson, so
  every JDK GZip caller gets the same default protection.
- Keep the default limit at 256 MiB, matching the existing Snappy/Zstd defensive
  limits in `bluetape4k-io`.
- Let `GzipCodec` expose `maxDecompressedSize` so Redis trust boundaries can use
  a smaller deployment-specific maximum.

## Outcome

- `GZipCompressor` now rejects decompressed output larger than
  `maxDecompressedSize` before writing the chunk into the output buffer.
- `GzipCodec` preserves `maxDecompressedSize` through the Redisson copy
  constructor path.
- README pairs document the default limit and custom limit examples.

## Verification

- Red test: new tests failed before implementation because `maxDecompressedSize`
  constructor parameters did not exist.
- `./gradlew :bluetape4k-io:compileTestKotlin :bluetape4k-redisson:compileTestKotlin --warning-mode all --no-daemon --no-configuration-cache`: PASS.
- `./gradlew :bluetape4k-redisson:test --tests "*Gzip*" :bluetape4k-io:test --tests "*Compressor*" --no-build-cache --no-daemon --no-configuration-cache`: PASS.
- `./gradlew :bluetape4k-io:test :bluetape4k-redisson:test --no-daemon --no-configuration-cache`: PASS, `io/io` 1005 tests and `infra/redisson` 290 tests with 0 failures/errors/skips.
- `git diff --check`: PASS.

## Future Guard

- Any compressor that cannot know the decompressed size from a trusted header
  must copy streams through a bounded loop, not `readBytes()`.
- For concurrency helper selection: this change adds no shared mutable state,
  coroutine lifecycle, or structured task scope behavior. `MultithreadingTester`,
  `SuspendedJobTester`, and `StructuredTaskScopeTester` are not applicable for
  the new regression tests; existing compressor concurrency coverage remains in
  `CompressorEdgeCaseTest`.
