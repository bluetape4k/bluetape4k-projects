# bluetape4k-okio DAEAD 청크 스트리밍 암호화 구현 계획

Spec: `docs/superpowers/specs/2026-04-29-daead-chunk-okio-design.md`
Issue: #240

## T1. DAEAD 청크 Sink 구현

- complexity: high
- expected files:
  - `io/okio/src/main/kotlin/io/bluetape4k/okio/tink/DaeadChunkEncryptSink.kt`
- work:
  - `DEFAULT_DAEAD_CHUNK_SIZE = 64 * 1024` 상수 추가
  - `DaeadChunkEncryptSink`를 `ForwardingSink` 기반으로 구현
  - 입력을 내부 plain buffer에 누적하고 `chunkSize` 이상일 때만 완성 청크 암호화
  - 각 청크를 `[8-byte big-endian ciphertext_len][ciphertext]`로 delegate에 기록
  - `flush()`는 완성 청크만 기록하고 partial chunk는 유지
  - `close()`는 남은 partial chunk를 기록한 뒤 delegate close
  - `chunkSize > 0` 검증과 associatedData 방어적 복사
  - `Sink.asDaeadChunkEncryptSink(...)` 확장 함수 제공
- verification:
  - `./bin/repo-test-summary -- ./gradlew :bluetape4k-okio:compileKotlin`
- docs impact:
  - 공개 API 한국어 KDoc 필요
  - README에 `close()`/`use {}` 필요성 기록

## T2. DAEAD 청크 Source 구현

- complexity: high
- expected files:
  - `io/okio/src/main/kotlin/io/bluetape4k/okio/tink/DaeadChunkDecryptSource.kt`
- work:
  - `DaeadChunkDecryptSource`를 `ForwardingSource` 기반으로 구현
  - 내부 plain buffer가 비었을 때 다음 청크 하나만 읽어 복호화
  - header 첫 바이트 전 EOF는 정상 EOF, partial header/body는 `EOFException`
  - ciphertext length는 `1..Int.MAX_VALUE`로 제한하고 0/음수는 `IOException`
  - `byteCount` 음수 거부, 0이면 0 반환
  - associatedData 방어적 복사
  - `Source.asDaeadChunkDecryptSource(...)` 확장 함수 제공
- verification:
  - `./bin/repo-test-summary -- ./gradlew :bluetape4k-okio:compileKotlin`
- docs impact:
  - 공개 API 한국어 KDoc 필요
  - README에 기존 `TinkDecryptSource`와의 wire format 비호환 기록

## T3. DAEAD 청크 테스트 작성

- complexity: medium
- expected files:
  - `io/okio/src/test/kotlin/io/bluetape4k/okio/tink/DaeadChunkEncryptSinkTest.kt`
  - `io/okio/src/test/kotlin/io/bluetape4k/okio/tink/DaeadChunkDecryptSourceTest.kt`
- work:
  - empty input, single chunk, multi chunk round-trip
  - 여러 small `write()` 호출을 하나의 stream으로 복호화
  - incremental read와 EOF 계약 검증
  - chunkSize 검증 실패 테스트
  - wire format header가 big-endian length를 쓰는지 검증
  - truncated header, truncated ciphertext, invalid length 실패 테스트
  - wrong associated data 실패 테스트
  - associatedData 방어적 복사 테스트
  - 복호화 첫 read가 전체 ciphertext를 소비하지 않는지 counting Source로 검증
- verification:
  - `./bin/repo-test-summary -- ./gradlew :bluetape4k-okio:test --tests "io.bluetape4k.okio.tink.DaeadChunk*Test"`
- docs impact:
  - 테스트명은 계약 중심으로 작성

## T4. README 업데이트

- complexity: low
- expected files:
  - `io/okio/README.md`
  - `io/okio/README.ko.md`
- work:
  - 기존 Tink AEAD 어댑터 섹션에 전체로드/호환성 주의 추가
  - DAEAD 청크 스트리밍 섹션 추가
  - 사용 예시, wire format, deterministic leakage, `close()` 필요성, associated data 제약 기록
- verification:
  - Markdown 코드 블록과 링크 육안 검토
- docs impact:
  - module behavior 변경이므로 README.md/README.ko.md 업데이트 필수
  - 새 durable convention은 없으므로 AGENTS.md 업데이트 불필요

## T5. Targeted verification

- complexity: medium
- expected files:
  - 변경된 source/test/docs 파일
- work:
  - targeted compile/test 실행
  - 실패 시 원인 분석 후 수정하고 재실행
  - Step 4-S 조건 판단: 구현 규모가 200 lines 이상이면 cleanup plan 후 중복/과잉 추상화 제거
  - Step 4-P 조건 판단: 암호화/IO/resource lifecycle 변경이므로 performance/stability scan 수행
- verification:
  - `./bin/repo-test-summary -- ./gradlew :bluetape4k-okio:test --tests "io.bluetape4k.okio.tink.DaeadChunk*Test"`
  - `./bin/repo-test-summary -- ./gradlew :bluetape4k-okio:compileKotlin :bluetape4k-okio:compileTestKotlin`
- docs impact:
  - docs/superpowers index 디렉터리는 현재 repo에 없으므로 index 업데이트는 N/A로 기록

## T6. Review, commit, PR

- complexity: medium
- expected files:
  - spec/plan
  - implementation/test/docs files
- work:
  - Step 5 verifier로 spec/plan 대비 구현 확인
  - Step 6 최종 체크리스트 수행
  - Step 6-R 6개 리뷰 티어 수행
  - Lore trailer 포함 커밋 작성
  - branch push 및 Issue #240을 닫는 PR 생성
  - Step 7 DoD 보고서 작성
- verification:
  - `./bin/repo-status`
  - `git log develop..HEAD --oneline`
  - `gh pr view --json number,url,title,state`
- docs impact:
  - Step 8 knowledge capture에서 새 반복 워크플로우가 없으면 skill/wiki/module AGENTS 업데이트 N/A 기록

## Pre-Implementation Risk Mitigations

- 기존 `TinkEncryptSink` / `TinkDecryptSource` 동작을 바꾸지 않는다.
- DAEAD 청크 포맷은 기존 Tink AEAD single-ciphertext format과 호환되지 않음을 KDoc/README에 명시한다.
- Sink는 partial chunk를 `close()`에서 확정하므로 테스트와 README에서 `use {}` 패턴을 고정한다.
- Source는 header/body exact read helper를 사용해 partial EOF를 명확히 실패시킨다.
- length header는 `Long`으로 읽되 `1..Int.MAX_VALUE` 범위를 넘어가면 `IOException`으로 거부한다.
- `associatedData`는 생성 시 `copyOf()`를 수행해 mutable array 변경 영향을 차단한다.
- 복호화 스트리밍성은 counting Source 테스트로 "첫 read가 전체 ciphertext를 소비하지 않음"을 검증한다.
