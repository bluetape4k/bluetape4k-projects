# debop4k → bluetape4k-core 이관 구현 계획

## 전제 조건

- 설계 명세: `docs/superpowers/specs/2026-03-22-debop4k-migration-design.md`
- 대상 모듈: `bluetape4k/core`
- 소스 기준: `~/work/debop/debop4k/debop4k-core/src/main/kotlin/debop4k/core/`

---

## Phase 1: 기반 작업

### Task 1.1: build.gradle.kts 의존성 추가

- **complexity: low**
- `bluetape4k/core/build.gradle.kts`에 `compileOnly(Libs.lz4_java)` 추가 (XXHasher용)
- 기존 의존성 확인: eclipse-collections(있음), commons-compress(있음, ZipCompressor에는 불필요)

### Task 1.2: 패키지 디렉토리 생성

- **complexity: low**
- `io/bluetape4k/collections/permutations/` 디렉토리 생성
- `io/bluetape4k/io/compress/` 디렉토리 생성
- `io/bluetape4k/utils/`는 이미 존재

---

## Phase 2: 유틸리티 (의존 없는 독립 모듈 먼저)

### Task 2.1: Wildcard 이관

- **complexity: medium**
- `debop4k.core.utils.Wildcard` → `io.bluetape4k.utils.Wildcard`
- **변환 내용**:
    - 패키지 변경
    - `String?.splits(PATH_SEPARATORS)` → 표준 Kotlin `split(*charArrayOf('/', '\\'))` 대체
    - KDoc 한국어 작성
- **테스트**: `WildcardTest.kt` 신규 작성
    - `match()`: 단일 `*`, `?`, 이스케이프 `\*`, 빈 문자열, 빈 패턴
    - `matchPath()`: `**` deep tree, 경로 구분자 `/`, `\`
    - `matchOne()`, `matchPathOne()`: 여러 패턴 매칭
    - edge case: 연속 `**`, 패턴만 있는 경우

### Task 2.2: XXHasher 이관

- **complexity: high**
- `debop4k.core.utils.XXHasher` → `io.bluetape4k.utils.XXHasher`
- **변환 내용**:
    - 패키지 변경
    - **thread-safety 개선**: `StreamingXXHash32`가 stateful이므로 `ThreadLocal` 적용
      ```kotlin
      private val hash32: ThreadLocal<StreamingXXHash32> = ThreadLocal.withInitial {
          factory.newStreamingHash32(DEFAULT_SEED)
      }
      ```
    - `Any?.hash()` 확장 → private 헬퍼로 유지
    - `Int.toByteArray()` 변환 로직은 표준 ByteBuffer 사용
    - KDoc 한국어 작성
- **테스트**: `XXHasherTest.kt` 신규 작성 (기존 Kotlin 테스트 변환)
    - primitive 해시 충돌 없음 검증
    - null 처리
    - concurrent 테스트 (여러 스레드에서 동시 호출)

---

## Phase 3: 압축 유틸리티

### Task 3.1: ZipCompressor 이관

- **complexity: medium**
- `debop4k.core.compress.ZipCompressor` → `io.bluetape4k.io.compress.ZipCompressor`
- **변환 내용**:
    - io/io 모듈의 `Compressor` 인터페이스에 의존하지 않고 독립 구현
    - `compress(ByteArray?): ByteArray` / `decompress(ByteArray?): ByteArray` 직접 정의
    - debop4k 유틸(`emptyByteArray`, `isNullOrEmpty`, `fastByteArrayOutputStreamOf`,
      `toByteArray`) → 표준 Kotlin/bluetape4k 유틸로 대체
    - `ByteArrayOutputStream` 직접 사용
    - KDoc 한국어 작성
- **테스트**: `ZipCompressorTest.kt` 신규 작성
    - 빈 입력, null 입력
    - 라운드트립 (compress → decompress)
    - 다양한 크기 데이터

### Task 3.2: ZipBuilder 이관

- **complexity: medium**
- `debop4k.core.compress.ZipBuilder` → `io.bluetape4k.io.compress.ZipBuilder`
- **변환 내용**:
    - `touch()` → `file.createNewFile()` 또는 `file.parentFile?.mkdirs()`
    - `closeSafe()` → `runCatching { close() }` 또는 `.use {}` 패턴
    - `toUtf8Bytes()` → `.toByteArray(Charsets.UTF_8)` 또는 bluetape4k의 `toUtf8Bytes()`
    - 내부 클래스 `AddFileToZip`, `AddContentToZip` 유지 (빌더 패턴)
    - 에러 처리 강화: `FileNotFoundException` 실제 throw, `IOException` 명확한 메시지
    - KDoc 한국어 작성
- **테스트**: `ZipBuilderTest.kt` 신규 작성
    - 인메모리 ZIP 생성 → `toBytes()` → 검증
    - 파일 기반 ZIP 생성 → `toZipFile()` → 검증
    - 폴더 추가, 콘텐트 추가
    - `@TempDir` 활용

### Task 3.3: ZipFileSupport 이관

- **complexity: high**
- `debop4k.core.compress.ZipFilex` → `io.bluetape4k.io.compress.ZipFileSupport.kt`
- **변환 내용**:
    - 톱레벨 함수: `zlib()`, `gzip()`, `ungzip()`, `zip()`, `unzip()`
    - 헬퍼: `addToZip()`, `addFolderToZip()`, `ZipFile.closeSafe()`
    - `debop4k.core.io.copy` → `InputStream.copyTo(OutputStream)` (Kotlin stdlib)
    - `debop4k.core.io.removeExtension` → `File.nameWithoutExtension` 또는 자체 구현
    - `Wildcard.matchPathOne` → 이관된 Wildcard 사용
    - **Zip Slip 방어 추가**: `unzip`에서 `destDir.canonicalPath` 기반 경로 검증
      ```kotlin
      val destCanonical = destDir.canonicalPath
      require(file.canonicalPath.startsWith(destCanonical)) {
          "Zip entry is outside of the target dir: ${entry.name}"
      }
      ```
    - `@file:JvmName("ZipFileSupport")` 어노테이션
    - KDoc 한국어 작성
- **테스트**: `ZipFileSupportTest.kt` 신규 작성
    - gzip/ungzip 라운드트립
    - zip/unzip 라운드트립
    - 패턴 필터 unzip
    - `@TempDir` 활용
    - Zip Slip 방어 테스트

---

## Phase 4: 컬렉션 (단순 → 복잡 순서)

### Task 4.1: PaginatedList 이관

- **complexity: low**
- `debop4k.core.collections.PaginatedList` → `io.bluetape4k.collections.PaginatedList`
- **변환 내용**:
    - `PaginatedList<T>` 인터페이스 + `SimplePaginatedList<T>` data class 유지
    - `@JvmOverloads` → Kotlin 기본값 사용
    - `totalPageCount` 계산을 `init` 블록 대신 computed property로 변경 가능
    - `Serializable` 유지
    - KDoc 한국어 작성
- **테스트**: `PaginatedListTest.kt` 신규 작성
    - 기본 생성, 페이지 수 계산
    - 경계값: totalItemCount=0, pageSize=1, 딱 나누어 떨어지는 경우
    - contents 검증

### Task 4.2: RingBuffer 이관

- **complexity: high**
- `debop4k.core.collections.RingBuffer` → `io.bluetape4k.collections.RingBuffer`
- **변환 내용**:
    - Eclipse Collections 의존 제거: `FastList` → `mutableListOf()`, `Predicate<E>` → `(E) -> Boolean`
    - **thread-safety 추가**: `ReentrantLock` + `withLock {}` 적용 (모든 변경 메서드 + 읽기 메서드)
    - `_count` → `count` (private backing field)
    - `toList()` 반환 타입: `List<E>` (Eclipse `FastList` 대신)
    - `toArray()` inline reified 유지
    - KDoc 한국어 작성
- **테스트**: `RingBufferTest.kt` 신규 작성 (기존 Java 테스트 변환)
    - empty, singleElement, multipleElement
    - overwrite (크기 초과)
    - removeIf
    - drop, next, clear
    - **concurrent 테스트**: 여러 스레드에서 동시 add/next 호출

### Task 4.3: BoundedStack 이관

- **complexity: high**
- `debop4k.core.collections.BoundedStack` → `io.bluetape4k.collections.BoundedStack`
- **변환 내용**:
    - `java.util.Stack<E>` 상속 제거 → 독립 클래스로 구현 (`Iterable<E>` 구현)
        - `Stack`은 deprecated 패턴 (Vector 기반), 직접 구현이 더 깔끔
    - Eclipse Collections `FastList` → `mutableListOf()`
    - 모든 `@Synchronized` → `ReentrantLock.withLock {}`
    - `insert()` 재귀 → 반복문 변환 (stack overflow 방지)
    - `addElement()` UnsupportedOperationException → 제거 (Stack 상속 제거 시 불필요)
    - `addAll(index, elements)` UnsupportedOperationException → 제거
    - KDoc 한국어 작성
- **테스트**: `BoundedStackTest.kt` 신규 작성 (기존 Java 테스트 변환)
    - emptyStack, singleElement, multipleElements
    - handleOverwrite (크기 초과)
    - insert, update
    - pop, peek 예외
    - **concurrent 테스트**: 여러 스레드에서 동시 push/pop

---

## Phase 5: Permutation (가장 복잡, 마지막)

### Task 5.1: 핵심 클래스 이관 (Permutation, Nil, Cons, FixedCons, PermutationIterator)

- **complexity: high**
- **변환 내용**:
    - 패키지 변경: `debop4k.core.collections.permutations` → `io.bluetape4k.collections.permutations`
    - `Permutation.kt`:
        - `hashOf(head, tail)` → `Objects.hash(head, tail)` 또는 커스텀 구현
        - `fastListOf<E>(this.force())` → `mutableListOf<E>().also { list -> this.force().forEach { list.add(it) } }`
        - `AbstractList<E>` + `Sequence<E>` 다중 구현 유지
        - KDoc 한국어 작성
    - `Nil.kt`:
        - 싱글턴 패턴 유지
        - `@Suppress("UNCHECKED_CAST")` 유지
    - `Cons.kt`:
        - **핵심**: `synchronized(lock)` → `ReentrantLock().withLock {}` 변환
        - `@Volatile` + DCL 패턴 유지
        - `by lazy` 제거 → 직접 getter 구현 (lazy와 lock이 중복됨)
        - `fastListOf` → `mutableListOf`
    - `FixedCons.kt`:
        - `fastListOf` → `mutableListOf`
    - `PermutationIterator.kt`:
        - `MutableIterator<E>` → `Iterator<E>` (remove 미지원이므로)
        - 또는 `MutableIterator` 유지 + `remove()` = `UnsupportedOperationException`

### Task 5.2: 팩토리 함수 이관 (PermutationSupport.kt)

- **complexity: medium**
- `Permutationx.kt` → `PermutationSupport.kt`
- **변환 내용**:
    - 파일명 변경 (`*x.kt` → `*Support.kt` bluetape4k 규칙)
    - 모든 톱레벨 함수/확장 함수 유지
    - `@JvmOverloads` 유지 (Java interop)
    - KDoc 한국어 작성

### Task 5.3: Stream 지원 이관 (PermutationStream, PermutationStreamSupport)

- **complexity: medium**
- `PermutationStream.kt` + `PermutationStreamx.kt` → `PermutationStream.kt` + `PermutationStreamSupport.kt`
- **변환 내용**:
    - `underlying.stream()` 호출 → `java.util.Collection.stream()` 또는 `StreamSupport.stream()` 직접 사용
    - `@file:JvmName("PermutationStreamSupport")` 추가
    - KDoc 한국어 작성

### Task 5.4: Permutation 테스트 이관

- **complexity: medium**
- 41개 테스트 파일 → Kotlin JUnit 5 + Kluent 변환
- **변환 전략**:
    - `AbstractPermutationTest` 기반 클래스 먼저 변환
    - JUnit 4 `@Test` → JUnit 5 `@Test` (import 변경)
    - `@Before` → `@BeforeEach`
    - Mockito → MockK (필요 시)
    - AssertJ → Kluent (`assertThat(x).isEqualTo(y)` → `x shouldBeEqualTo y`)
    - `@Test(expected = ...)` → `shouldThrow<Exception> { ... }`
    - 테스트 파일 목록:
        - 기본 연산: HeadTest, TailTest, GetTest, SizeTest, ForEachTest
        - 생성: BuidlingTest, ContinuallyTest, IterateTest, LazySeqTabulateTest
        - 변환: MapTest, FilterTest, FlatMapTest
        - 추출: LazySeqTakeTest, DropTest, SliceTest, TakeWhileTest, DropWhileTest
        - 집계: ReduceTest, MinMaxTest, AnyMatchTest, AllMatchTest, NoneMatchTest, ContainsTest
        - 고급: ZipTest, SlidingTest, GroupedTest, ScanTest, DistinctTest, SortedTest
        - 유틸: ToStringTest, MkStringTest, EqualsHashcodeTest, ForceTest, StartsWithTest
        - Stream: StreamTest, IteratorTest
        - 예제: FibonacciTest, CollatzConjectureTest, LazyPagingTest, RandomCollectionElementLazySeqTest
- **concurrent 테스트 추가**:
    - Cons의 tail 동시 평가 테스트
    - 여러 스레드에서 동시 순열 순회

---

## Phase 6: 통합 검증

### Task 6.1: 전체 빌드 및 테스트 실행

- **complexity: low**
- `./gradlew :bluetape4k-core:test`
- 모든 이관 테스트 통과 확인
- 기존 테스트 회귀 없음 확인

### Task 6.2: Detekt 정적 분석

- **complexity: low**
- `./gradlew :bluetape4k-core:detekt`
- 경고/에러 확인 및 수정

---

## 작업 순서 요약

```
Phase 1 (기반)
  1.1 build.gradle.kts 의존성       [low]
  1.2 디렉토리 생성                  [low]

Phase 2 (유틸리티 — 독립)
  2.1 Wildcard                      [medium]
  2.2 XXHasher                      [high]

Phase 3 (압축 — Wildcard 의존)
  3.1 ZipCompressor                 [medium]
  3.2 ZipBuilder                    [medium]
  3.3 ZipFileSupport                [high]   ← Wildcard 필요, Zip Slip 방어

Phase 4 (컬렉션 — 독립)
  4.1 PaginatedList                 [low]
  4.2 RingBuffer                    [high]   ← thread-safety 추가
  4.3 BoundedStack                  [high]   ← Stack 상속 제거, thread-safety

Phase 5 (Permutation — 가장 복잡)
  5.1 핵심 클래스                    [high]   ← Cons thread-safety
  5.2 팩토리 함수                    [medium]
  5.3 Stream 지원                   [medium]
  5.4 테스트 41개 변환              [medium]

Phase 6 (검증)
  6.1 전체 빌드/테스트              [low]
  6.2 Detekt                       [low]
```

## 예상 산출물

| 카테고리          | 파일 수 |
|---------------|------|
| 소스 코드 (main)  | 13개  |
| 테스트 코드 (test) | ~50개 |
| 빌드 설정 변경      | 1개   |
| **합계**        | ~64개 |

## 리스크 및 완화 전략

| 리스크                                             | 확률 | 영향 | 완화                                |
|-------------------------------------------------|----|----|-----------------------------------|
| Permutation의 sorted()에서 FastList.sortThis() 미존재 | 높음 | 중  | `toMutableList().sortedWith()` 대체 |
| XXHasher thread-safety 미비                       | 높음 | 높  | ThreadLocal 패턴 적용                 |
| ZipFileSupport Zip Slip 취약점                     | 중간 | 높  | canonicalPath 검증 추가               |
| BoundedStack insert 재귀 stack overflow           | 낮음 | 중  | 반복문 변환                            |
| Permutation 테스트 41개 변환 누락                       | 중간 | 중  | 체크리스트 기반 진행                       |

## Phase 2~5 병렬화 가능 여부

- Phase 2 (Wildcard, XXHasher)와 Phase 4 (PaginatedList, RingBuffer, BoundedStack)는 **병렬 진행 가능**
- Phase 3 (ZipFileSupport)는 Wildcard 완료 후 진행
- Phase 5 (Permutation)는 독립적이므로 Phase 2~4와 **병렬 진행 가능**
