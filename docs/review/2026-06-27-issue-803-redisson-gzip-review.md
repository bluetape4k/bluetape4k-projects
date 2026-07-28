# Review - Issue #803 Redisson GZip Decompression Bound (2026-06-27)

## Scope

- Issue: #803 `P1: Bound Redisson GZip decompression output`
- Modules: `:bluetape4k-io`, `:bluetape4k-redisson`
- Changed runtime paths:
  - `GZipCompressor.doDecompress`
  - `GzipCodec.valueDecoder`

## 발견 사항

No P0/P1 findings remain.

## Review Notes

- Security/trust boundary: `GZipCompressor` now enforces a positive
  `maxDecompressedSize` and checks each decompressed chunk before appending it
  to the `ByteArrayOutputStream`. This blocks gzip expansion beyond the
  configured bound.
- Redisson integration: `GzipCodec` uses the bounded `GZipCompressor` instance
  and preserves the configured limit through the Redisson `(ClassLoader,
  GzipCodec)` copy constructor.
- API compatibility: the original `GZipCompressor(bufferSize)` and
  `GzipCodec(innerCodec)` call shapes remain valid through default parameters
  and Java overloads.
- Documentation: `README.md` and `README.ko.md` pairs document the default
  256 MiB limit and custom limit examples.
- Tests/evidence: new tests cover invalid limits, oversized decompression,
  corrupt gzip payloads, and successful bounded roundtrip.
- Concurrency helper gate: no new shared mutable state, coroutine lifecycle, or
  structured concurrency behavior was introduced. `MultithreadingTester`,
  `SuspendedJobTester`, and `StructuredTaskScopeTester` are not applicable to
  the new regression tests; existing compressor multithread coverage remains
  unchanged in `CompressorEdgeCaseTest`.

## 검증 Evidence

- Red test before implementation: compile failed on missing
  `maxDecompressedSize` constructor parameters.
- Compile: `./gradlew :bluetape4k-io:compileTestKotlin :bluetape4k-redisson:compileTestKotlin --warning-mode all --no-daemon --no-configuration-cache` PASS.
- Targeted tests: `./gradlew :bluetape4k-redisson:test --tests "*Gzip*" :bluetape4k-io:test --tests "*Compressor*" --no-build-cache --no-daemon --no-configuration-cache` PASS.
- Full affected module tests: `./gradlew :bluetape4k-io:test :bluetape4k-redisson:test --no-daemon --no-configuration-cache` PASS; `io/io` 1005 tests and `infra/redisson` 290 tests with 0 failures/errors/skips.
- Whitespace: `git diff --check` PASS.

## Gate Verdict

P0/P1 = 0. Proceed to PR after final diff and metadata verification.
