# KDoc 인라인 예제 추가 -- core / io 모듈 파일럿

**날짜**: 2026-04-04
**설계 문서**: `docs/superpowers/specs/2026-04-04-kdoc-examples-design.md`
**범위**: P0 5개 + P1 5개 = 10개 파일
**예상 복잡도**: MEDIUM

---

## Context

bluetape4k-core (157 파일)와 bluetape4k-io (47 파일) 모듈에서 KDoc 예제 커버리지가 각각 71%, 85%이다. 파일럿으로 선정된 10개 파일에 인라인 코드 예제를 추가하여 예제 품질 기준을 확립한다.

## Work Objectives

- P0/P1 10개 파일에 클래스 레벨 종합 예제 + 메서드별 예제 추가
- 모든 코드 블록에 ` ```kotlin ` 언어 태그 사용
- 설계 문서 3장 가이드라인 준수 (예제 길이, import 생략, 스타일)
- 빌드 통과 확인

## Guardrails

### Must Have
- 모든 public 메서드/프로퍼티에 인라인 예제 1개 이상
- 클래스/인터페이스 레벨 종합 예제 (생성 + 핵심 메서드 조합)
- 예제 결과를 주석으로 표시 (`// "result"` 또는 `// [a, b, c]`)
- 기존 테스트 코드와 API 시그니처 정합성 확인
- 코드 블록 언어 태그 ` ```kotlin ` 필수

### Must NOT Have
- import 문 포함 (동일/외부 패키지 모두 생략)
- @sample 태그 사용 (인라인 예제만)
- 기존 KDoc 텍스트 삭제/변경 (예제만 추가)
- 코드 로직 변경

---

## Task Flow

```
T1 ──┐
T2 ──┤ (P0, 병렬 가능)
T3 ──┤
T4 ──┤
T5 ──┘
     │
T6 ──► 빌드/테스트 검증 (core + io)
     │
T7 ──┐
T8 ──┤ (P1, 병렬 가능)
T9 ──┤
T10 ─┤
T11 ─┘
     │
T12 ─► 빌드/테스트 검증 (core + io)
     │
T13 ─► README 갱신 + testlog 기록
```

---

## Detailed TODOs

### Phase 1: P0 -- 핵심 인터페이스/자료구조

#### T1. RingBuffer KDoc 예제 추가
- **complexity**: medium
- **대상 파일**: `/Users/debop/work/bluetape4k/bluetape4k-projects/bluetape4k/core/src/main/kotlin/io/bluetape4k/collections/RingBuffer.kt`
- **참고 테스트**: `/Users/debop/work/bluetape4k/bluetape4k-projects/bluetape4k/core/src/test/kotlin/io/bluetape4k/collections/RingBufferTest.kt`
- **의존성**: 없음
- **추가할 예제 목록**:
  - 클래스 `RingBuffer` — 종합 예제 (생성, add, overflow 동작, toList)
  - `add(item)` — 단일 추가 + 가득 찬 경우 덮어쓰기 동작
  - `addAll(vararg)` / `addAll(Collection)` — 다중 추가
  - `get(index)` — 인덱스 접근 (0 = 가장 오래된 요소)
  - `set(index, elem)` — 요소 교체
  - `drop(n)` — 앞에서 n개 제거
  - `removeIf(predicate)` — 조건부 제거
  - `next()` — dequeue 동작
  - `clear()` — 초기화
  - `toList()` — 스냅샷
  - `size` / `isEmpty` — 상태 조회
- **수용 기준**: 모든 public 메서드/프로퍼티에 예제 존재, 예제 내 API 호출이 테스트와 일치

#### T2. BoundedStack KDoc 예제 추가
- **complexity**: medium
- **대상 파일**: `/Users/debop/work/bluetape4k/bluetape4k-projects/bluetape4k/core/src/main/kotlin/io/bluetape4k/collections/BoundedStack.kt`
- **참고 테스트**: `/Users/debop/work/bluetape4k/bluetape4k-projects/bluetape4k/core/src/test/kotlin/io/bluetape4k/collections/BoundedStackTest.kt`
- **의존성**: 없음
- **추가할 예제 목록**:
  - 클래스 `BoundedStack` — 종합 예제 (생성, push, pop, peek)
  - `push(item)` — push + 가득 찬 경우 오래된 요소 제거
  - `pushAll(vararg)` / `pushAll(Collection)` — 다중 push
  - `pop()` — top 제거 및 반환
  - `peek()` — top 확인 (제거 안 함)
  - `get(index)` — 인덱스 접근 (0 = top)
  - `insert(index, elem)` — 중간 삽입
  - `update(index, elem)` — 요소 교체
  - `clear()` — 초기화
  - `toList()` — 스냅샷 (top → bottom 순서)
  - `size` / `isEmpty` — 상태 조회
- **수용 기준**: 모든 public 메서드/프로퍼티에 예제 존재

#### T3. StringEncoder KDoc 예제 추가
- **complexity**: low
- **대상 파일**: `/Users/debop/work/bluetape4k/bluetape4k-projects/bluetape4k/core/src/main/kotlin/io/bluetape4k/codec/StringEncoder.kt`
- **참고 파일**: `Base64StringEncoder.kt`, `HexStringEncoder.kt` (구현체 참고)
- **의존성**: 없음
- **추가할 예제 목록**:
  - 인터페이스 `StringEncoder` — 종합 예제 (Base64StringEncoder 또는 HexStringEncoder를 사용한 encode/decode 흐름)
  - `encode(bytes)` — 바이트 배열 → 문자열
  - `decode(encoded)` — 문자열 → 바이트 배열
- **수용 기준**: 인터페이스 레벨에서 구현체 사용 방법 안내, encode→decode 왕복 예제 포함

#### T4. Compressors KDoc 예제 추가
- **complexity**: medium
- **대상 파일**: `/Users/debop/work/bluetape4k/bluetape4k-projects/io/io/src/main/kotlin/io/bluetape4k/io/compressor/Compressors.kt`
- **참고 테스트**: `/Users/debop/work/bluetape4k/bluetape4k-projects/io/io/src/test/kotlin/io/bluetape4k/io/compressor/CompressorsTest.kt`
- **의존성**: 없음
- **추가할 예제 목록**:
  - object `Compressors` — 종합 예제 (압축기 선택 가이드 + 기본 compress/decompress 흐름)
  - 주요 프로퍼티 (`LZ4`, `Zstd`, `Snappy`, `GZip`) — 각각 1줄 사용 예시
  - object `Streaming` — 스트리밍 압축 사용 예시 (InputStream/OutputStream 래핑)
- **수용 기준**: 압축기 선택 기준 안내, compress→decompress 왕복 예제, Streaming 사용법 포함

#### T5. BinarySerializers KDoc 예제 추가
- **complexity**: medium
- **대상 파일**: `/Users/debop/work/bluetape4k/bluetape4k-projects/io/io/src/main/kotlin/io/bluetape4k/io/serializer/BinarySerializers.kt`
- **참고 테스트**: `/Users/debop/work/bluetape4k/bluetape4k-projects/io/io/src/test/kotlin/io/bluetape4k/io/serializer/AbstractBinarySerializerTest.kt`, `BinarySerializerSupportTest.kt`
- **의존성**: 없음
- **추가할 예제 목록**:
  - object `BinarySerializers` — 종합 예제 (직렬화기 선택 가이드 + serialize/deserialize 흐름)
  - `Jdk` / `Kryo` / `Fory` — 기본 직렬화기 사용 예시
  - `LZ4Kryo` / `ZstdFory` 등 — 압축 직렬화기 사용 예시 (대표 2개)
  - 중복 KDoc 블록 정리 (현재 `/** */` 2개 연속)
- **수용 기준**: 직렬화기 선택 기준 안내, serialize→deserialize 왕복 예제, 압축 직렬화 예제 포함

#### T6. Phase 1 빌드/테스트 검증
- **complexity**: low
- **의존성**: T1~T5 완료
- **수행 내용**:
  - `./gradlew :bluetape4k-core:build :bluetape4k-io:build` 성공 확인
  - detekt 경고 없음 확인
- **수용 기준**: 빌드 성공, 테스트 전체 통과

---

### Phase 2: P1 -- 자주 사용하는 유틸리티

#### T7. PaginatedList KDoc 예제 추가
- **complexity**: low
- **대상 파일**: `/Users/debop/work/bluetape4k/bluetape4k-projects/bluetape4k/core/src/main/kotlin/io/bluetape4k/collections/PaginatedList.kt`
- **참고 테스트**: `/Users/debop/work/bluetape4k/bluetape4k-projects/bluetape4k/core/src/test/kotlin/io/bluetape4k/collections/PaginatedListTest.kt`
- **의존성**: T6 완료 (Phase 1 피드백 반영)
- **추가할 예제 목록**:
  - 인터페이스 `PaginatedList` — 종합 예제 (SimplePaginatedList 생성 + 프로퍼티 접근)
  - `SimplePaginatedList` — data class 생성 예시 + totalPageCount 계산 결과
  - `contents` / `pageNo` / `pageSize` / `totalItemCount` / `totalPageCount` — 프로퍼티별 예시
- **수용 기준**: 페이지네이션 전체 흐름 예제, totalPageCount 계산 결과 주석 포함

#### T8. KotlinDelegates KDoc 예제 추가
- **complexity**: medium
- **대상 파일**: `/Users/debop/work/bluetape4k/bluetape4k-projects/bluetape4k/core/src/main/kotlin/io/bluetape4k/utils/KotlinDelegates.kt`
- **참고 테스트**: `/Users/debop/work/bluetape4k/bluetape4k-projects/bluetape4k/core/src/test/kotlin/io/bluetape4k/utils/KotlinDelegatesTest.kt`
- **의존성**: T6
- **추가할 예제 목록**:
  - object `KotlinDelegates` — 종합 예제 (primaryConstructor 조회 + instantiateClass)
  - 확장함수 `Class<T>.primaryConstructor()` — 사용 예시
  - 확장함수 `Class<T>.findPrimaryConstructor()` — null 반환 케이스 포함
  - 확장함수 `Constructor<T>.instantiateClass(vararg args)` — 인스턴스 생성 예시
  - `KotlinDelegates.primaryConstructor(clazz)` — 직접 호출 예시
  - `KotlinDelegates.findPrimaryConstructor(clazz)` — 직접 호출 예시
  - `KotlinDelegates.instantiateClass(constructor, args)` — optional 파라미터 활용
- **수용 기준**: 리플렉션 기반이므로 data class 예시로 직관적 이해 가능하도록 작성

#### T9. Wildcard KDoc 예제 추가
- **complexity**: low
- **대상 파일**: `/Users/debop/work/bluetape4k/bluetape4k-projects/bluetape4k/core/src/main/kotlin/io/bluetape4k/utils/Wildcard.kt`
- **참고 테스트**: `/Users/debop/work/bluetape4k/bluetape4k-projects/bluetape4k/core/src/test/kotlin/io/bluetape4k/utils/WildcardTest.kt`
- **의존성**: T6
- **추가할 예제 목록**:
  - object `Wildcard` — 종합 예제 (?, *, ** 패턴 각각 1줄씩)
  - `match(string, pattern)` — `?`와 `*` 패턴 예시
  - `equalsOrMatch(string, pattern)` — 동일 문자열 + 패턴 매칭 예시
  - `matchOne(src, patterns)` — 여러 패턴 중 매칭 인덱스 반환 예시
  - `matchPath(path, pattern)` — Ant 스타일 `**` 경로 매칭 예시
  - `matchPathOne(path, patterns)` — 여러 경로 패턴 매칭 예시
- **수용 기준**: 각 와일드카드 패턴(`?`, `*`, `\*`, `**`)에 대한 입출력 예시 포함

#### T10. Range KDoc 예제 추가
- **complexity**: medium
- **대상 파일**: `/Users/debop/work/bluetape4k/bluetape4k-projects/bluetape4k/core/src/main/kotlin/io/bluetape4k/ranges/Range.kt`
- **참고 테스트**: `/Users/debop/work/bluetape4k/bluetape4k-projects/bluetape4k/core/src/test/kotlin/io/bluetape4k/ranges/ClosedClosedRangeTest.kt`, `OpenOpenRangeTest.kt` 등
- **참고 구현체**: `ClosedClosedRange.kt`, `ClosedOpenRange.kt`, `OpenClosedRange.kt`, `OpenOpenRange.kt`
- **의존성**: T6
- **추가할 예제 목록**:
  - 인터페이스 `Range` — 종합 예제 (4가지 Range 타입 생성 + contains 비교)
  - `contains(value)` — 값 포함 여부
  - 확장함수 `contains(other: Range)` — 범위 포함 여부 + 경계 타입 영향 예시
  - 확장함수 `overlaps(other: Range)` — 겹침 판단 + 경계 타입 영향 예시
  - 확장함수 `isAscending()` — 범위 컬렉션 정렬 검증 예시
- **수용 기준**: 4가지 Range 타입 차이를 예제로 명확히 보여줌, 경계 조건(Open/Closed) 차이 시연

#### T11. ZipFileSupport KDoc 예제 추가
- **complexity**: low
- **대상 파일**: `/Users/debop/work/bluetape4k/bluetape4k-projects/io/io/src/main/kotlin/io/bluetape4k/io/compressor/ZipFileSupport.kt`
- **참고 테스트**: `/Users/debop/work/bluetape4k/bluetape4k-projects/io/io/src/test/kotlin/io/bluetape4k/io/compressor/ZipFileSupportTest.kt`
- **의존성**: T6
- **추가할 예제 목록**:
  - 최상위 함수 `gzip(file)` / `ungzip(file)` — GZIP 압축/해제 흐름
  - 최상위 함수 `zlib(file)` — ZLIB 압축
  - 최상위 함수 `zip(file)` / `unzip(zipFile, destDir)` — ZIP 압축/해제 흐름
  - `addToZip(zos, file)` — ZipOutputStream에 파일 추가
  - `addToZip(zos, content, path)` — 바이트 배열 추가
  - `addFolderToZip(zos, path)` — 빈 폴더 엔트리 추가
- **수용 기준**: 파일 기반 압축/해제 전체 흐름 예제, ZipOutputStream 직접 사용 예제 포함

#### T12. Phase 2 빌드/테스트 검증
- **complexity**: low
- **의존성**: T7~T11 완료
- **수행 내용**:
  - `./gradlew :bluetape4k-core:build :bluetape4k-io:build` 성공 확인
  - detekt 경고 없음 확인
- **수용 기준**: 빌드 성공, 테스트 전체 통과

---

### Phase 3: 마무리

#### T13. README 갱신 + testlog 기록
- **complexity**: low
- **의존성**: T12 완료
- **수행 내용**:
  - `bluetape4k/core/README.md` — KDoc 예제 커버리지 수치 업데이트 (45 → 35 예제 없음)
  - `io/io/README.md` — KDoc 예제 커버리지 수치 업데이트 (7 → 5 예제 없음)
  - `docs/testlog.md` — 빌드/테스트 결과 기록
- **수용 기준**: README에 최신 커버리지 반영, testlog 기록 완료

---

## Success Criteria

1. P0 5개 + P1 5개 = 10개 파일의 모든 public API에 인라인 예제 존재
2. 모든 코드 블록에 ` ```kotlin ` 언어 태그 사용
3. 예제 스타일이 설계 문서 3장 가이드라인과 일치
4. `./gradlew :bluetape4k-core:build :bluetape4k-io:build` 성공
5. detekt 경고 없음
6. README 및 testlog 갱신 완료
