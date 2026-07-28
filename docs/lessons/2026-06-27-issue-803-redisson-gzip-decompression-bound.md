# 교훈: 이슈 #803 Redisson GZip decompression bound (2026-06-27)

## 배경

이슈 #803은 `GzipCodec`이 Redis payload를 `Compressors.GZip.decompress(bytes)`로
압축 해제하고, `GZipCompressor`가 decompressed-size bound 없이
`GZIPInputStream.readBytes()`를 사용한다는 점을 확인했다.

## 결정

- 방어적 size limit은 Redisson에만 두지 않고 `GZipCompressor`에 둔다. 그러면 모든 JDK GZip caller가 같은 기본 보호를 받는다.
- 기본 limit은 기존 `bluetape4k-io`의 Snappy/Zstd defensive limit과 맞춰 256 MiB로 유지한다.
- `GzipCodec`은 Redis trust boundary에서 더 작은 deployment-specific maximum을 사용할 수 있도록 `maxDecompressedSize`를 노출한다.

## 결과

- `GZipCompressor`는 chunk를 output buffer에 쓰기 전에 `maxDecompressedSize`보다 큰 decompressed output을 거부한다.
- `GzipCodec`은 Redisson copy constructor 경로에서도 `maxDecompressedSize`를 보존한다.
- README pair는 기본 limit과 custom limit 예시를 문서화한다.

## 검증

- Red test: `maxDecompressedSize` constructor parameter가 없어 새 테스트가 구현 전 실패했다.
- `./gradlew :bluetape4k-io:compileTestKotlin :bluetape4k-redisson:compileTestKotlin --warning-mode all --no-daemon --no-configuration-cache`: PASS.
- `./gradlew :bluetape4k-redisson:test --tests "*Gzip*" :bluetape4k-io:test --tests "*Compressor*" --no-build-cache --no-daemon --no-configuration-cache`: PASS.
- `./gradlew :bluetape4k-io:test :bluetape4k-redisson:test --no-daemon --no-configuration-cache`: PASS, `io/io` 1005 tests와 `infra/redisson` 290 tests 모두 0 failures/errors/skips.
- `git diff --check`: PASS.

## 향후 방지책

- Trusted header에서 decompressed size를 알 수 없는 compressor는 `readBytes()`가 아니라 bounded loop로 stream을 copy해야 한다.
- Concurrency helper 선택 기준: 이 변경은 shared mutable state, coroutine lifecycle, structured task scope behavior를 추가하지 않는다. 새 regression test에는 `MultithreadingTester`, `SuspendedJobTester`, `StructuredTaskScopeTester`가 적용되지 않으며, 기존 compressor concurrency coverage는 `CompressorEdgeCaseTest`에 남아 있다.
