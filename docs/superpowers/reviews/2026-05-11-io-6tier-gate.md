# bluetape4k-io 6-Tier Review Gate

**날짜**: 2026-05-11
**모듈**: `bluetape4k-io`
**결론**: PASS
**P0/P1**: 0

## Tier 1. Public API / Contract

- `Path.combineSafe(String|Path)`와 `File.combineSafe(Path)`가 이름과 KDoc대로 parent traversal 및 absolute path를 거부한다.
- README의 `ZipBuilder` / `ZipFileSupport` 예제를 실제 public API에 맞게 갱신했다.
- P0/P1: 없음

## Tier 2. Security / Boundary Safety

- `combineSafe("../...")` 경로 탈출을 차단했다.
- `combineSafe("/absolute/path")`는 더 이상 base 경로 아래 상대 경로처럼 변환하지 않는다.
- `unzip()`의 zip bomb 한도를 ZIP metadata뿐 아니라 실제 추출 바이트 수로도 검증한다.
- P0/P1: 없음

## Tier 3. Failure Semantics

- `FileSupportResult`의 `runCatching` 경로는 sync/async 파일 실패를 `Result<T>`로 값화하는 의도적 API다.
- `ZipBuilder` close `runCatching`은 cleanup 경로이며 실패를 warning으로 제한하는 기존 정책을 유지한다.
- P0/P1: 없음

## Tier 4. Tests / Edge Cases

- 추가 edge tests:
  - `Path.combineSafe(String)`의 parent traversal 거부
  - `Path.combineSafe(Path)`의 absolute path 거부
  - `File.combineSafe(Path)`의 absolute path 거부
- Verification:
  - `./gradlew :bluetape4k-io:test --tests "io.bluetape4k.io.PathSupportTest"`: PASS, 8 tests
  - `./gradlew :bluetape4k-io:test --tests "io.bluetape4k.io.PathSupportTest" --tests "io.bluetape4k.io.compressor.ZipFileSupportTest" --tests "io.bluetape4k.io.compressor.ZipBombProtectionTest"`: PASS, 24 tests
  - `./gradlew :bluetape4k-io:test`: PASS, 913 tests
- P0/P1: 없음

## Tier 5. Documentation / KDoc

- `Path.combineSafe(String)` KDoc에 traversal/absolute path rejection contract와 예외를 명시했다.
- `README.md`와 `README.ko.md`에 safe path combination, actual-byte ZIP limit, 최신 `ZipBuilder` 예제를 동기화했다.
- P0/P1: 없음

## Tier 6. Maintainability / Patterns

- 새 dependency는 추가하지 않았다.
- ZIP actual-byte 검증은 private helper로 제한해 public API를 늘리지 않았다.
- 변경 범위는 path confinement, unzip extraction safety, tests, docs에 제한된다.
- P0/P1: 없음
