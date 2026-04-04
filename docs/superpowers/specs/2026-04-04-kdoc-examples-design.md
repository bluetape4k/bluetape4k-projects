# KDoc 예제 추가 설계 -- core / io 모듈 파일럿

**날짜**: 2026-04-04
**범위**: `bluetape4k-core` (157 파일), `bluetape4k-io` (47 파일)
**목표**: KDoc 인라인 코드 예제 추가로 라이브러리 사용성 개선

---

## 1. 현황 분석

### 파일 수 및 예제 커버리지

| 모듈 | 전체 파일 | 예제 있음 | 예제 없음 | 커버리지 |
|------|----------|----------|----------|---------|
| core | 157 | 112 | 45 | 71% |
| io | 47 | 40 | 7 | 85% |
| **합계** | **204** | **152** | **52** | **75%** |

### 예제가 없는 파일 (52개)

#### core 모듈 (45개)

**컬렉션 (13개)**
- `BoundedStack.kt` -- thread-safe 제한 크기 스택
- `RingBuffer.kt` -- thread-safe 링 버퍼
- `PaginatedList.kt` -- 페이지네이션 리스트
- `IteratorSupport.kt` -- Iterator 확장 함수
- `VarargSupport.kt` -- vararg 유틸리티
- `permutations/` 패키지 전체 (8개: Cons, FixedCons, Nil, Permutation, PermutationIterator, PermutationStream, PermutationStreamSupport, PermutationSupport)

**코덱 (2개)**
- `StringEncoder.kt` -- 바이트 배열 인코딩 인터페이스
- `StringEncoderSupport.kt` -- StringEncoder 확장 함수

**범위 (6개)**
- `Range.kt` -- Range 인터페이스
- `ClosedClosedRange.kt`, `ClosedRangeSupport.kt`, `OpenClosedRange.kt`, `OpenOpenRange.kt`, `InvalidRangeException.kt`

**시간 (4개)**
- `DateIterator.kt`, `TemporalIterator.kt`, `YearQuarter.kt`, `ZonedDateTimeSupport.kt`

**동시성 (4개)**
- `NamedThreadFactory.kt`, `ThreadSupport.kt`, `VirtualThreadDispatcher.kt`, `VirtualThreadReactorScheduler.kt`

**유틸 (5개)**
- `KotlinDelegates.kt`, `Wildcard.kt`, `XXHasher.kt`, `BigIntegerPair.kt`, `Systemx.kt`

**기타 (11개)**
- `DefaultFields.kt`, `SortDirection.kt`, `ValueObject.kt`
- `ApacheConstructorUtils.kt`, `ApacheEnumUtils.kt`, `ApacheExceptionUtils.kt`
- `BluetapeException.kt`, `NotSupportedException.kt`
- `AutoCloseableSupport.kt`, `ClassLoaderSupport.kt`, `JavaTypeSupport.kt`

#### io 모듈 (7개)

- `compressor/Compressors.kt` -- 압축기 레지스트리
- `compressor/StreamingCompressors.kt` -- 스트리밍 압축기 팩토리
- `compressor/ZipFileSupport.kt` -- ZIP 파일 유틸리티
- `serializer/BinarySerializers.kt` -- 직렬화기 레지스트리
- `serializer/BinarySerializationException.kt` -- 예외
- `serializer/KryoProvider.kt` -- Kryo 설정
- `apache/ApacheCommonIo.kt` -- Apache Commons IO 래퍼

### 기존 예제 스타일 관찰

**우수 사례 (참고 파일)**:
- `RequireSupport.kt`: 모든 메서드에 `## 동작/계약` + 코드 블록 패턴
- `Base62.kt`: 클래스 레벨 종합 예제 + 개별 메서드 예제
- `FutureUtils.kt`: 모든 메서드에 인라인 코드 예제
- `Compressor.kt`: 인터페이스 레벨 사용 예제 + Null 처리 정책

**예제 길이 분포**: 2~8줄 (메서드), 4~12줄 (클래스)

---

## 2. KDoc 예제 추가 전략 -- 3가지 접근

### 전략 A: 우선순위 파일 선별 처리 (권장)

**방법**: 사용 빈도 / 공개 API 영향도 기준으로 파일을 3등급으로 분류하고, 고우선순위부터 작업.

**분류 기준**:
- **P0 (필수)**: 인터페이스, 팩토리/레지스트리, 핵심 자료구조 -- 사용자가 가장 먼저 접하는 API
- **P1 (중요)**: 확장 함수 모음, 유틸리티 클래스 -- 일상적으로 사용하는 API
- **P2 (선택)**: 예외 클래스, 내부 구현체, Apache 래퍼 -- 직접 사용 빈도 낮음

| 장점 | 단점 |
|------|------|
| 가장 임팩트 높은 파일부터 완성 | 우선순위 판단 기준이 주관적일 수 있음 |
| 파일럿 범위를 좁힐 수 있음 | P2 파일이 방치될 가능성 |
| 점진적 병합 가능 | |

### 전략 B: 카테고리별 일괄 처리

**방법**: 패키지 단위(codec, collections, compressor, serializer 등)로 그룹화하여 한 카테고리씩 완성.

| 장점 | 단점 |
|------|------|
| 같은 도메인 파일 간 예제 일관성 확보 용이 | 카테고리마다 난이도 편차 큼 |
| 리뷰 단위가 명확 | 한 카테고리 완성 전 다른 카테고리 작업 불가 |
| 패키지 README와 연계 가능 | |

### 전략 C: 모듈 전체 일괄 처리

**방법**: 예제 없는 52개 파일 전체를 한 번에 작업.

| 장점 | 단점 |
|------|------|
| 한 번에 완료 | 대규모 PR 리뷰 부담 |
| 전체 일관성 검증 한 번에 가능 | 중간 피드백 반영 어려움 |
| | 작업 시간 예측 어려움 |

**권장: 전략 A (우선순위 선별)** -- 파일럿 특성상 빠른 피드백 루프가 중요하며, P0 파일 5~10개로 예제 품질 기준을 확립한 후 확장하는 것이 효율적.

---

## 3. 예제 작성 가이드라인 초안

### 3.1 예제 길이 기준

| 대상 | 길이 | 설명 |
|------|------|------|
| 단일 메서드 (간단) | 2~4줄 | 호출 + 결과 확인 |
| 단일 메서드 (파라미터 조합) | 4~6줄 | 주요 파라미터 변형 포함 |
| 클래스/인터페이스 (종합) | 6~12줄 | 생성 + 핵심 메서드 조합 |
| DSL/빌더 패턴 | 6~10줄 | 빌더 체이닝 전체 흐름 |

### 3.2 import 규칙

- **import 생략**: KDoc 예제에서 import는 생략한다. 동일 패키지 내 클래스는 당연히 생략, 외부 패키지도 사용법 집중을 위해 생략.
- **예외**: 사용하는 외부 클래스가 예제 이해에 필수적인 경우(예: `java.util.UUID`) 주석으로 언급.

### 3.2-A 코드 블록 언어 태그 규칙

- **필수**: 모든 KDoc 코드 블록은 ` ```kotlin ` 언어 태그를 사용한다. 언어 태그가 없는 ` ``` ` 는 사용 금지.
- **기존 파일 정규화**: 파일 편집 시 해당 파일의 언어 태그 없는 코드 블록도 ` ```kotlin ` 으로 함께 수정한다.
- **이유**: IDE 구문 강조 및 코드 포매팅 지원을 위해 언어 태그를 명시한다.

### 3.2-B @sample 대신 인라인 예제를 선택한 이유

- `@sample`은 IDE에서 제대로 렌더링되지 않아 KDoc 툴팁에서 예제가 보이지 않음
- 인라인 예제는 IDE 툴팁, 문서 생성(Dokka), 코드 읽기 모두에서 즉시 확인 가능
- 단점(컴파일 타임 검증 불가)은 테스트 코드와 cross-check로 보완

### 3.2-C suspend 함수 예제 가이드라인

- `suspend` 함수 예제는 `runBlocking { ... }` 으로 감싸서 실행 가능함을 명시
- 코루틴 컨텍스트가 필요한 경우 `runBlocking(Dispatchers.IO) { ... }` 사용
- Flow 반환 함수는 `collect { }` 또는 `toList()` 사용 예시 포함

```kotlin
// suspend 함수 예제
val result = runBlocking {
    myRepository.findById(1L)
}

// Flow 예제
val items = runBlocking {
    myRepository.queryFlow().toList()
}
```

### 3.3 클래스 레벨 vs 메서드 레벨 구분

| 기준 | 클래스 레벨 예제 | 메서드 레벨 예제 |
|------|----------------|----------------|
| 대상 | 클래스/인터페이스/object 선언부 | 개별 함수/메서드 |
| 내용 | 인스턴스 생성 + 2~3개 핵심 메서드 조합 | 단일 메서드 호출과 반환값 |
| 목적 | "이 클래스를 어떻게 사용하는지" 전체 흐름 | "이 메서드가 무엇을 하는지" 구체적 동작 |
| 조건 | (a) 인터페이스/추상 클래스, (b) 빌더/DSL, (c) stateful 클래스 | 모든 public 메서드/확장 함수 |

**규칙**: 클래스에 종합 예제가 있더라도, 개별 메서드 예제는 별도로 추가한다 (메서드 단위 검색 편의).

### 3.4 에러/예외 케이스

- **기본 정책**: 정상 동작 예제만 포함.
- **예외적 포함 대상**:
  - `require*` 계열 함수 -- 실패 시 예외 발생이 핵심 동작
  - `Compressor`/`BinarySerializer` -- Null 입력 시 동작 (이미 문서화된 패턴)
  - 빈 컬렉션/빈 문자열 입력 등 경계값이 중요한 함수
- **표기 방법**: `// throws IllegalArgumentException` 주석

### 3.4-A KDoc 블록 내 순서 (필수)

KDoc 블록 내 순서는 반드시 다음을 따른다:
1. 설명 텍스트
2. 빈 줄
3. ` ```kotlin ` 예제 블록
4. 빈 줄
5. `@param`, `@return`, `@throws` 태그

**이유**: @param/@return 태그 뒤에 예제 코드 블록이 오면 IDE(IntelliJ)에서 들여쓰기가 걸려 보인다.

```kotlin
// ✅ 올바른 순서
/**
 * 두 수를 더합니다.
 *
 * ```kotlin
 * val result = add(1, 2)  // 3
 * ```
 *
 * @param a 첫 번째 수
 * @param b 두 번째 수
 * @return 합계
 */

// ❌ 잘못된 순서 (IDE에서 예제가 들여쓰기되어 보임)
/**
 * 두 수를 더합니다.
 *
 * @param a 첫 번째 수
 * @param b 두 번째 수
 * @return 합계
 *
 * ```kotlin
 * val result = add(1, 2)  // 3
 * ```
 */
```

### 3.5 예제 코드 스타일

```kotlin
// 좋은 예: 결과를 주석으로 표시
val encoded = UUID.randomUUID().encodeBase62()  // "3lO7ysTzNOGrjT4vadTPio"

// 좋은 예: 변수명으로 의도 전달
val buffer = RingBuffer<String>(maxSize = 3)
buffer.add("a")
buffer.add("b")
buffer.add("c")
buffer.add("d")  // "a"가 덮어씌워짐
buffer.toList()   // ["b", "c", "d"]

// 나쁜 예: 변수명이 불명확
val x = RingBuffer<String>(3)
x.add("a")
```

### 3.6 기존 패턴 참고

이미 예제가 있는 파일들의 패턴을 일관성 있게 따른다:
- `RequireSupport.kt` 패턴: `## 동작/계약` 섹션 후 코드 블록
- `Base62.kt` 패턴: 클래스 독에 출력 예시 포함
- `Compressor.kt` 패턴: `## Null 처리 정책` + `## 사용 예시`
- `FutureUtils.kt` 패턴: 메서드마다 독립적인 코드 블록

---

## 4. 파일럿 구현 대상 추천 (10개)

### P0 -- 핵심 인터페이스/자료구조 (5개)

| # | 파일 | 이유 |
|---|------|------|
| 1 | `core/.../collections/RingBuffer.kt` | Thread-safe 자료구조, 생성+add+overflow 종합 예제 필요 |
| 2 | `core/.../collections/BoundedStack.kt` | Thread-safe 자료구조, push/pop/peek 종합 예제 필요 |
| 3 | `core/.../codec/StringEncoder.kt` | 최상위 인터페이스, 구현체 사용법 안내 필요 |
| 4 | `io/.../compressor/Compressors.kt` | 레지스트리 객체, 어떤 압축기를 선택해야 하는지 안내 필요 |
| 5 | `io/.../serializer/BinarySerializers.kt` | 레지스트리 객체, 직렬화기 선택 가이드 필요; 참고 테스트: `AbstractBinarySerializerTest.kt`, `BinarySerializerSupportTest.kt` |

### P1 -- 자주 사용하는 유틸리티 (5개)

| # | 파일 | 이유 |
|---|------|------|
| 6 | `core/.../collections/PaginatedList.kt` | 페이지네이션 패턴은 실무에서 매우 빈번 |
| 7 | `core/.../utils/KotlinDelegates.kt` | Kotlin 위임 패턴, 사용법이 직관적이지 않음 |
| 8 | `core/.../utils/Wildcard.kt` | 와일드카드 매칭, 입출력 예제가 있어야 동작 파악 가능 |
| 9 | `core/.../ranges/Range.kt` | 4가지 Range 타입의 진입점, 전체 계층 소개 필요 |
| 10 | `io/.../compressor/ZipFileSupport.kt` | ZIP 파일 조작은 실무 활용 빈도 높음 |

---

## 5. 태스크 초안

### Phase 1: 가이드라인 확정 + 파일럿 (P0 5개)

| # | 태스크 | 수용 기준 |
|---|--------|----------|
| 1 | 예제 작성 가이드라인 최종 확정 | 위 3장 내용을 팀 리뷰 후 확정 |
| 2 | P0 파일 5개에 KDoc 예제 추가 | 클래스 레벨 종합 예제 + 메서드별 예제 (예제 없는 public 메서드 0개), ```kotlin 언어 태그 사용 |
| 3 | 기존 테스트 코드와 예제 정합성 검증 | 예제의 API 호출 시그니처 및 반환값이 실제 테스트 파일의 패턴과 일치 확인 |
| 4 | detekt / 빌드 통과 확인 | `./gradlew :bluetape4k-core:build :bluetape4k-io:build` 성공 |
| 5 | `bluetape4k-core` / `bluetape4k-io` README.md 갱신 | KDoc 예제 커버리지 현황 및 스타일 가이드라인 반영 |
| 6 | `docs/testlog.md` 기록 | 빌드 및 테스트 결과 기록 |

### Phase 2: P1 확장 (5개)

| # | 태스크 | 수용 기준 |
|---|--------|----------|
| 7 | P1 파일 5개에 KDoc 예제 추가 | Phase 1과 동일 기준 |
| 8 | Phase 1 피드백 반영 | 가이드라인 수정사항 P1에도 일괄 적용 |
| 9 | README.md 갱신 | 커버리지 수치 업데이트 |

### Phase 3: 잔여 파일 처리 (선택)

파일럿 10개 완료 후 잔여 42개 파일(52 - 10 = 42; P2로 분류된 Apache 래퍼 등 일부 제외 시 조정 가능).

| # | 태스크 | 수용 기준 |
|---|--------|----------|
| 10 | P2 잔여 파일 예제 추가 | 최소 클래스 레벨 예제 1개 |
| 11 | 전체 일관성 리뷰 | 모든 public API에 예제 존재 확인 |

---

## 6. 리스크 및 열린 질문

### 리스크
- **예제 유지보수 부담**: API 시그니처 변경 시 예제도 함께 수정 필요 (컴파일 검증 불가)
- **예제 정확성**: `@sample`과 달리 인라인 예제는 컴파일 타임 검증이 없음 -- 테스트와 cross-check 필수

### 열린 질문 — 결정 사항

- **`permutations/` 패키지**: 사용자가 직접 사용하는 진입점 클래스(`PermutationSupport`, `PermutationStream` 등)에 예제 집중. `Cons`, `FixedCons`, `Nil` 등 내부 구현체는 간략한 1~2줄 예제로 충분.
- **Phase 3 타임라인**: usage limit을 고려해 점진적으로 진행. 파일럿 완료 후 다른 모듈 작업과 병행하며 세션 여유 시 추가.
- **단순 래퍼 파일** (`ApacheConstructorUtils`, `ApacheEnumUtils`, `ApacheExceptionUtils` 등): 원본 Apache Commons 문서 링크(`@see`) 추가로 충분. 별도 예제 불필요.
