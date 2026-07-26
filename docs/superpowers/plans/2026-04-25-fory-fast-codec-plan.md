# FastFory Codec 추가 — 구현 계획서

-

**Spec**: [`docs/superpowers/specs/2026-04-25-fory-fast-codec-design.md`](../specs/2026-04-25-fory-fast-codec-design.md)
- **Issue**: #113
- **Branch/worktree**: `.worktrees/fory-fast-codec`
- **Date**: 2026-04-25
- **Approach**: A (각 모듈 개별 심볼, 추상화 없음)
- **Total tasks**: 21
- **Complexity**: low 13 / medium 8 / high 0
- **Gzip 결정**: A — io/io `GZipFastFory` + Redisson `GzipFastFory` + Lettuce `gzipFastFory()` 추가 (모듈별 기존 관례 준수, 사용자 확정)

## 실행 규칙

- 각 task는 "입력 (선행 조건) → 작업 → 출력 (완료 기준)" 형식이다.
- Phase 내부는 순차 실행이 기본. "parallel 가능" 표기가 있는 task만 병렬 실행 가능.
- **Phase 2와 Phase 3은 서로 독립**이므로 병렬 실행 가능. 단, 둘 다 Phase 1 (io/io)의 심볼에 의존하므로 Phase 1 완료 후 시작한다.
- Phase 4는 전체 구현이 끝난 뒤 실행하는 마무리 단계이다.
- Kotlin 편집 워크플로 (CLAUDE.md): 편집 즉시 `ide_diagnostics` → 필요 시 `ide_optimize_imports` / `lsp_code_actions` → 빌드/테스트.

---

## Phase 1 — io/io 기반 배선 (모든 하위 Phase의 전제)

### Task 1 — `BinarySerializers`에 FastFory 5종 val 추가 (spec §5.1, §6)

- **complexity**: low
- **parallel 가능**: No (Phase 1 내 순차)
- **입력**:
    - `io/io/src/main/kotlin/io/bluetape4k/io/serializer/BinarySerializers.kt` 기존 `Fory`, `LZ4Fory`, `ZstdFory`, `SnappyFory` 심볼 확인
    - `ForyBinarySerializer.fast()` 이미 구현됨 (spec §1.3, §12 해결된 가정)
- **작업**:
    - `FastFory`, `LZ4FastFory`, `ZstdFastFory`, `SnappyFastFory`, `GZipFastFory`
      **5개** `by lazy` val 추가 (spec §5.1 코드 블록 그대로)
    - 각 val에 한국어 KDoc + spec §6 경고 블록 포함 (비호환 경고, 휘발성 캐시 전용, refTracking=false, 스키마 진화 불가)
- **출력 (완료 기준)**:
    - `BinarySerializers.FastFory` 5종이 `by lazy` + 한국어 KDoc + §6 경고 블록 포함으로 노출
    - `ide_diagnostics` 오류 0
    - `./gradlew :bluetape4k-io:compileKotlin` 성공

### Task 2 — `FastForyCompatibilityTest.kt` (io/io) 작성 (spec §7.2)

- **complexity**: medium
- **parallel 가능**: No
- **입력**: Task 1 완료 (FastFory 심볼 사용 가능)
- **작업**:
    - 파일 위치: `io/io/src/test/kotlin/io/bluetape4k/io/serializer/FastForyCompatibilityTest.kt`
    - 테스트 3종:
        - roundtrip PASS (FastFory encode → FastFory decode 성공)
        - **방향
          A**: `BinarySerializers.Fory.serialize(obj)` 바이트 → `BinarySerializers.FastFory.deserialize(...)` → `assertThrows` (io/io는 fallback 없음, spec §7.2)
        - **방향
          B**: `BinarySerializers.FastFory.serialize(obj)` 바이트 → `BinarySerializers.Fory.deserialize(...)` → `assertThrows`
    - JUnit 5 + bluetape4k-assertions, KLogging companion
- **출력 (완료 기준)**:
    - 3개 테스트 모두 PASS
    - 순환 참조 객체 사용 금지 (spec §7.3 경고 준수)

### Task 3 — io/io 테스트 실행 + testlog 기록

- **complexity**: low
- **parallel 가능**: No
- **입력**: Task 1, 2 완료
- **작업**:
    - `./gradlew :bluetape4k-io:test` 실행
    - `docs/testlogs/2026-04.md` 이달 파일 표 맨 위에 행 추가 (모듈: bluetape4k-io, 결과: pass 수/fail 수, 내용: FastFory 심볼 + 교차 검증)
- **출력 (완료 기준)**:
    - 기존 테스트 + 신규 FastForyCompatibilityTest 모두 PASS
    - testlog 엔트리 기록 완료

---

## Phase 2 — infra/redisson (Phase 3과 **parallel 가능**)

선행 조건: Phase 1 완료.

### Task 4 — `FastForyCodec.kt` 신규 작성 (spec §5.2, §D1, §R2)

- **complexity**: medium
- **parallel 가능**: No (Phase 2 내 선행)
- **입력**:
    - `infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/codec/ForyCodec.kt` 읽고 바이트 단위 복제 준비
    - `BinarySerializers.FastFory` 사용 가능 (Phase 1)
- **작업**:
    - 위치: `infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/codec/FastForyCodec.kt`
    - `ForyCodec.kt` 바이트 단위 복제 (spec §D1 — 상속 아님)
    - `private val fory by lazy { BinarySerializers.FastFory }`
    - `class FastForyCodec(private val fallbackCodec: Codec = RedissonCodecs.Fory) : BaseCodec()` (fallback=Kryo5 아님, spec §R2)
    - 보조 생성자: `(classLoader: ClassLoader)`, `(classLoader: ClassLoader, codec: FastForyCodec)` (spec §5.2)
    - encoder/decoder try/catch → fallback 경로, 로그 메시지 "FastForyCodec" 명시
    - `companion object: KLogging()` (bluetape4k-patterns)
    - 한국어 KDoc + §6 경고 블록 (비대칭 계약 명시)
- **출력 (완료 기준)**:
    - `FastForyCodec` 컴파일 성공, `ide_diagnostics` 오류 0
    - 2 보조 생성자 포함, fallback 기본값 = `RedissonCodecs.Fory`

### Task 5 — `RedissonCodecs.kt`에 FastFory val 10개 추가 (spec §5.2)

- **complexity**: low
- **parallel 가능**: No
- **입력**: Task 4 완료
- **작업**:
    - `FastFory`, `LZ4FastFory`, `ZstdFastFory`, `SnappyFastFory`, `GzipFastFory` + 각 `*Composite` 총
      **10개** val 추가 (spec §5.2 코드 블록)
    - Gzip 포함: `GzipFastFory` + `GzipFastForyComposite` 추가 (기존 Redisson `GzipFory` 관례 준수)
    - `FastFory` val은 가독성상 기존 `Fory` 선언 이후에 배치 (Kotlin `by lazy`는 런타임 참조 순서로 동작하므로 선언 순서 강제는 불필요 — 가독성 목적)
    - 각 val에 한국어 KDoc + §6 경고 블록
- **출력 (완료 기준)**:
    - 10개 val 노출 (Gzip 포함)
    - `./gradlew :bluetape4k-redisson:compileKotlin` 성공

### Task 6 — `RedissonCodecsTest.getRedissonBinaryCodecs()` 파라미터 추가 (spec §7.4)

- **complexity**: low
- **parallel 가능**: No
- **입력**: Task 5 완료
- **작업**:
    - `infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/codec/RedissonCodecsTest.kt` 의 `getRedissonBinaryCodecs()` 파라미터 제공자에 FastFory
      **10종** 추가
        - plain: `FastFory`, `LZ4FastFory`, `ZstdFastFory`, `SnappyFastFory`, `GzipFastFory`
        - composite: `FastForyComposite`, `LZ4FastForyComposite`, `ZstdFastForyComposite`, `SnappyFastForyComposite`, `GzipFastForyComposite`
    - 기존 RedissonCodecsTest는 plain+composite 모두 포함 — 새 family도 동일하게 편입 (Codex 리뷰 확정)
- **출력 (완료 기준)**:
    - 파라미터화 테스트가 10종 추가 codec 전수에 대해 실행됨

### Task 7 — `FastForyCompatibilityTest.kt` (infra/redisson) 작성 (spec §7.2, §7.3)

- **complexity**: medium
- **parallel 가능**: No
- **입력**: Task 4, 5 완료
- **작업**:
    - 위치: `infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/codec/FastForyCompatibilityTest.kt`
    - **방향 A**: `ForyCodec` 인코드 바이트 → `FastForyCodec` decode → **성공
      assert** (FastFory 실패 → Fory fallback 성공 경로 검증, spec §R2, §7.2)
    - **방향 B**: `FastForyCodec` 인코드 바이트 → `ForyCodec` decode → `assertThrows` (Fory 실패 → Kryo5 fallback 실패)
    - **encode fallback 범위
      명시** (Codex 리뷰 확정): encode fallback (FastForyCodec encode 실패 → ForyCodec encode 재시도)은 코드 패리티 (ForyCodec과 동일 try/catch 구조)이며
      **테스트로 증명하지
      않음**. 안전한 fixture 정의 불가 (순환 참조→StackOverflowError, 직렬화 불가 타입→Fory/Kryo5도 실패 가능). 방향 A는 decode fallback만 검증.
    - 순환 참조·직렬화 불가 타입 사용 금지 (spec §7.3)
- **출력 (완료 기준)**: 2개 방향 테스트 모두 PASS

### Task 8 — `RedissonCodecBenchmark.kt` 확장 (spec §8.1)

- **complexity**: medium
- **parallel 가능**: No
- **입력**: Task 5 완료
- **작업**:
    - `infra/redisson/src/benchmark/kotlin/io/bluetape4k/redis/redisson/benchmark/RedissonCodecBenchmark.kt` 기존 codec 필드 블록에 `fastForyCodec`, `lz4FastForyCodec`, `zstdFastForyCodec`, `gzipFastForyCodec` 추가
    - 기존 `@Benchmark fun foryEncodeDecode()` 등 실제 메서드명 패턴 복제해 FastFory 계열 `@Benchmark` 4개 추가 (`fastForyEncodeDecode`, `lz4FastForyEncodeDecode`, `zstdFastForyEncodeDecode`, `gzipFastForyEncodeDecode`)
    - 기존 fixture (값 분포) **그대로 재사용** (spec §R3)
- **출력 (완료 기준)**: `./gradlew :bluetape4k-redisson:compileBenchmarkKotlin` 성공

### Task 9 — infra/redisson 테스트 실행 + testlog 기록

- **complexity**: low
- **parallel 가능**: No
- **입력**: Task 4~8 완료
- **작업**:
    - `./gradlew :bluetape4k-redisson:test` 실행
    - `docs/testlogs/2026-04.md` 맨 위 행 추가
- **출력 (완료 기준)**: 전체 테스트 PASS, testlog 엔트리 추가

### Task 10 — infra/redisson 벤치마크 실행 + 결과 기록 (spec §8.3)

- **complexity**: medium
- **parallel 가능**: No
- **입력**: Task 9 완료
- **작업**:
    - **선행
      확인**: `infra/redisson/build.gradle.kts` 또는 `buildSrc`에서 `benchmark` gradle task 정의 확인 (없으면 Task 8 컴파일 성공으로 대체)
    - `./gradlew :bluetape4k-redisson:benchmark` 실행
    - 출력 (ops/s)을 마크다운 표로 정리 (Fory vs FastFory, LZ4/Zstd 변형 비교)
    - 측정 환경 (CPU/메모리/JVM) 명기
    - 결과를 Task 18 README 반영용 임시 파일 또는 PR description 초안으로 보존
- **출력 (완료 기준)**: 마크다운 표 생성 완료, 벤치마크 로그 아카이브

---

## Phase 3 — infra/lettuce (Phase 2와 **parallel 가능**)

선행 조건: Phase 1 완료. Phase 2와 독립 실행 가능.

### Task 11 — `LettuceBinaryCodecs.kt`에 fastFory 팩토리 5개 추가 (spec §5.3)

- **complexity**: low
- **parallel 가능**: No (Phase 3 내 선행)
- **입력**: Phase 1 완료 (`BinarySerializers.FastFory` 사용 가능)
- **작업**:
    - `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceBinaryCodecs.kt` 에 `fastFory()`, `lz4FastFory()`, `zstdFastFory()`, `snappyFastFory()`, `gzipFastFory()`
      **5개** 제네릭 팩토리 추가 (spec §5.3 코드)
    - 각 함수에 한국어 KDoc + §6 경고 블록 (io/lettuce 경로는 **fallback 없음** 명시)
- **출력 (완료 기준)**: 5개 팩토리 노출, 컴파일 성공

### Task 12 — `LettuceBinaryCodecTest.getRedisCodecs()` 파라미터 추가 (spec §7.4)

- **complexity**: low
- **parallel 가능**: No
- **입력**: Task 11 완료
- **작업**:
    - `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceBinaryCodecTest.kt` 의 `getRedisCodecs()` 에 `fastFory()`, `lz4FastFory()`, `zstdFastFory()`, `snappyFastFory()`, `gzipFastFory()`
      **5종** 추가
- **출력 (완료 기준)**: 파라미터화 테스트가 신규 5종에서 실행됨

### Task 13 — `FastForyCompatibilityTest.kt` (infra/lettuce) 작성 (spec §7.2)

- **complexity**: medium
- **parallel 가능**: No
- **입력**: Task 11 완료
- **작업**:
    - 위치: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/codec/FastForyCompatibilityTest.kt`
    - **방향 A**: Fory 바이트 → fastFory decode → `assertThrows` (lettuce는 fallback 없음, spec §7.2)
    - **방향 B**: FastFory 바이트 → fory decode → `assertThrows`
    - roundtrip PASS 포함
- **출력 (완료 기준)**: 모든 테스트 PASS (실패 고정 assert 포함)

### Task 14 — `LettuceCodecBenchmark.kt` 확장 (spec §8.2)

- **complexity**: medium
- **parallel 가능**: No
- **입력**: Task 11 완료
- **작업**:
    - `infra/lettuce/src/benchmark/kotlin/io/bluetape4k/redis/lettuce/benchmark/LettuceCodecBenchmark.kt` 에 `fastForyCodec`, `lz4FastForyCodec`, `zstdFastForyCodec`, `gzipFastForyCodec` 필드 + `@Benchmark` 4개 추가 (`fastForyEncodeDecode`, `lz4FastForyEncodeDecode`, `zstdFastForyEncodeDecode`, `gzipFastForyEncodeDecode`)
    - 기존 fixture 재사용 (spec §R3)
- **출력 (완료 기준)**: `./gradlew :bluetape4k-lettuce:compileBenchmarkKotlin` 성공

### Task 15 — infra/lettuce 테스트 실행 + testlog 기록

- **complexity**: low
- **parallel 가능**: No
- **입력**: Task 11~14 완료
- **작업**:
    - `./gradlew :bluetape4k-lettuce:test` 실행
    - `docs/testlogs/2026-04.md` 맨 위 행 추가
- **출력 (완료 기준)**: 전체 테스트 PASS, testlog 엔트리 추가

### Task 16 — infra/lettuce 벤치마크 실행 + 결과 기록 (spec §8.3)

- **complexity**: medium
- **parallel 가능**: No
- **입력**: Task 15 완료
- **작업**:
    - `./gradlew :bluetape4k-lettuce:benchmark` 실행
    - 마크다운 표 작성, 측정 환경 명기
- **출력 (완료 기준)**: 결과 표 생성, 로그 보존

---

## Phase 4 — 문서화 / 마무리

선행 조건: Phase 1, 2, 3 모두 완료.

### Task 17 — io/io README 업데이트 (spec §9)

- **complexity**: low
- **parallel 가능**: Yes (Task 18, 19와 병렬 가능 — 모듈별 README 독립)
- **입력**: Phase 1 완료
- **작업**:
    - `io/io/README.md` + `io/io/README.ko.md` 에 BinarySerializers 섹션에 FastFory 계열 5종 추가 (GZipFastFory 포함) + §6 비호환 경고 박스
    - Architecture→UML→Features→Examples 구조 유지 (memory: feedback_readme_maintenance)
    - 언어 전환 링크 유지
- **출력 (완료 기준)**: 양 언어 README 동기화, FastFory 섹션 포함

### Task 18 — infra/redisson README 업데이트 (spec §9)

- **complexity**: low
- **parallel 가능**: Yes (Task 17, 19와 병렬)
- **입력**: Task 10 완료 (벤치마크 결과 확보)
- **작업**:
    - `infra/redisson/README.md` + `README.ko.md` Codec 표에 FastFory/LZ4FastFory/ZstdFastFory/SnappyFastFory/GzipFastFory (plain 5 + composite 5) row 추가
    - Task 10의 마크다운 벤치마크 결과 표 삽입
    - §6 경고 박스 삽입 (비대칭 계약 명시)
- **출력 (완료 기준)**: 양 언어 README 동기화, 벤치마크 표 포함

### Task 19 — infra/lettuce README 업데이트 (spec §9)

- **complexity**: low
- **parallel 가능**: Yes (Task 17, 18과 병렬)
- **입력**: Task 16 완료
- **작업**:
    - `infra/lettuce/README.md` + `README.ko.md` 팩토리 표에 fastFory 계열 5개 추가 (gzipFastFory 포함)
    - 벤치마크 결과 표 삽입
    - Issue #113 원문의 `foryFast` → **`fastFory`** 명명 변경 사항 기재 (spec §9)
- **출력 (완료 기준)**: 양 언어 README 동기화, 벤치마크 표 포함

### Task 20 — superpowers INDEX 갱신

- **complexity**: low
- **parallel 가능**: Yes (Task 17-19와 병렬)
- **입력**: Phase 1~3 완료
- **작업**:
    - `docs/superpowers/index/2026-04.md` 맨 위에 본 스펙/플랜 엔트리 추가
    - `docs/superpowers/INDEX.md` 카운트 갱신
    - 필요 시 `/wiki-update` 실행 (spec §11 task 19)
- **출력 (완료 기준)**: INDEX 엔트리 추가, 카운트 일치

### Task 21 — PR 생성 전 최종 체크리스트 통과

- **complexity**: medium
- **parallel 가능**: No (마지막 게이트)
- **입력**: Task 1~20 모두 완료
- **작업**:
    - `./gradlew detekt` 전역 실행
    - 3개 모듈 테스트 재실행으로 regression 확인
    - CLAUDE.md "Before Creating a PR" 체크리스트 전항 통과:
        - 변경 모듈 테스트 PASS
        - README.md + README.ko.md 양쪽 업데이트
        - KDoc 추가/갱신 (신규 public API 전수)
        - 작업 위치가 `.worktrees/fory-fast-codec` 인지 확인
        - superpowers index 갱신 완료
        - `/oh-my-claudecode:code-reviewer` 실행 권장
    - PR description 초안에 테스트 결과/벤치마크 표/비대칭 계약 경고 포함
- **출력 (완료 기준)**: 체크리스트 100% 통과, PR 본문 초안 준비 완료

---

## Task × Spec 매핑 요약

| Task | Spec 섹션                | Complexity | Parallel |
|------|--------------------------|------------|----------|
| 1    | §5.1, §6                 | low        | No       |
| 2    | §7.2                     | medium     | No       |
| 3    | §11 (Phase 1)            | low        | No       |
| 4    | §5.2, §D1, §R2           | medium     | No       |
| 5    | §5.2                     | low        | No       |
| 6    | §7.4                     | low        | No       |
| 7    | §7.2, §7.3               | medium     | No       |
| 8    | §8.1, §R3                | medium     | No       |
| 9    | §11 (Phase 2)            | low        | No       |
| 10   | §8.3                     | medium     | No       |
| 11   | §5.3                     | low        | No       |
| 12   | §7.4                     | low        | No       |
| 13   | §7.2                     | medium     | No       |
| 14   | §8.2, §R3                | medium     | No       |
| 15   | §11 (Phase 3)            | low        | No       |
| 16   | §8.3                     | medium     | No       |
| 17   | §9                       | low        | **Yes**  |
| 18   | §9, §8.3                 | low        | **Yes**  |
| 19   | §9, §8.3                 | low        | **Yes**  |
| 20   | §11 (Phase 4)            | low        | **Yes**  |
| 21   | §11 (Phase 4), CLAUDE.md | medium     | No       |

**Complexity 분포**: low 13 · medium 8 · high 0 (총 21)
**Parallel 가능 task**: 4개 (Task 17, 18, 19, 20 — Phase 4 문서화)
**병렬 실행 가능 Phase**: Phase 2 ↔ Phase 3 (Phase 1 완료 후)
