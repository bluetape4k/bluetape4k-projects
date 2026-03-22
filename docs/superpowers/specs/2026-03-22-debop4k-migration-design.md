# debop4k → bluetape4k-core 유용 기능 이관 설계 명세

## 1. 개요

debop4k-core에서 bluetape4k-core로 유용한 컬렉션, 압축, 유틸리티 기능을 이관합니다. Java 코드를 Kotlin 2.3으로 현대화하고, thread-safety를 ReentrantLock 기반으로 강화합니다.

## 2. 이관 대상 분석

### 2.1 Permutation (지연 평가 순열 — 8개 파일)

**원본 패키지**: `debop4k.core.collections.permutations`
**대상 패키지**: `io.bluetape4k.collections.permutations`

| 파일                       | 역할                                                                                          | LOC  | 핵심 관심사                                                                      |
|--------------------------|---------------------------------------------------------------------------------------------|------|-----------------------------------------------------------------------------|
| `Permutation.kt`         | 추상 기반 클래스, `AbstractList<E>` + `Sequence<E>`                                                | ~320 | map/filter/flatMap/take/drop/zip/sliding/grouped/scan/distinct/sorted 지연 연산 |
| `Nil.kt`                 | 빈 순열 (종료 노드), 싱글턴                                                                           | ~85  | 모든 연산 NoSuchElement/빈 결과 반환                                                 |
| `Cons.kt`                | 지연 평가 노드 — head + tailFunc 람다                                                               | ~73  | **ReentrantLock + @Volatile + DCL로 thread-safe tail 평가**                    |
| `FixedCons.kt`           | 이미 평가된 tail 노드                                                                              | ~57  | tail이 즉시값이므로 동기화 불필요                                                        |
| `PermutationIterator.kt` | MutableIterator 구현                                                                          | ~44  | 단순 위임                                                                       |
| `Permutationx.kt`        | 팩토리 함수 (`permutationOf`, `cons`, `concat`, `iterate`, `tabulate`, `continually`, `numbers`) | ~145 | 확장 함수 + 톱레벨 함수                                                              |
| `PermutationStream.kt`   | Java Stream 래퍼                                                                              | ~204 | `java.util.stream.Stream<T>` 구현                                             |
| `PermutationStreamx.kt`  | Stream 변환 확장 함수                                                                             | ~53  | `toStream()`, `toPermutation()`                                             |

**외부 의존성 분석**:

- `debop4k.core.collections.fastListOf` → Eclipse Collections `FastList` — bluetape4k-core에 이미 eclipse-collections 있음
- `debop4k.core.utils.hashOf` → `Objects.hash()` 대체 가능 또는 자체 구현
- `debop4k.core.collections.stream` → `java.util.Collection.stream()` 래퍼 — 직접 호출로 대체

**thread-safety 패턴 (Cons.kt)**:

```kotlin
// 현재: synchronized(lock) + @Volatile + lazy
// 변환: ReentrantLock.withLock {} 패턴으로 개선
private val lock = ReentrantLock()
@Volatile private var tailOrNull: Permutation<T>? = null

val tail: Permutation<T>
    get() {
        tailOrNull?.let { return it }
        lock.withLock {
            tailOrNull?.let { return it }
            return tailFunc().also { tailOrNull = it }
        }
    }
```

**테스트**: 41개 테스트 파일 존재 (JUnit 4 + Mockito + AssertJ → JUnit 5 + Kluent 변환)

### 2.2 BoundedStack (크기 제한 스택)

**원본**: `debop4k.core.collections.BoundedStack`
**대상**: `io.bluetape4k.collections.BoundedStack`

- `java.util.Stack<E>` 상속 — 원형 배열 기반 LIFO
- 모든 변경 메서드에 `@Synchronized` 사용 → **ReentrantLock으로 교체**
- Eclipse Collections `FastList` 의존 (`toList()`) → `ArrayList` 또는 `mutableListOf`로 대체 가능
- `maxSize` 초과 시 가장 오래된 요소 자동 제거 (ring buffer 방식)

**테스트**: Java 파일 1개 (JUnit 4 + AssertJ) → Kotlin JUnit 5 + Kluent 변환

### 2.3 PaginatedList (페이징 데이터)

**원본**: `debop4k.core.collections.PaginatedList`
**대상**: `io.bluetape4k.collections.PaginatedList`

- `PaginatedList<T>` 인터페이스 + `SimplePaginatedList<T>` data class
- 단순 데이터 클래스, thread-safety 이슈 없음
- `Serializable` 구현

**테스트**: 기존 테스트 없음 → 신규 작성 필요

### 2.4 RingBuffer (원형 버퍼)

**원본**: `debop4k.core.collections.RingBuffer`
**대상**: `io.bluetape4k.collections.RingBuffer`

- 고정 크기 원형 버퍼, `Iterable<E>` 구현
- `@Synchronized` 없음 — **thread-safety 보강 필요** (ReentrantLock 추가)
- Eclipse Collections `FastList` + `Predicate` 의존 → 표준 Kotlin으로 대체
- `removeIf(predicate)`, `drop(n)`, `next()`, `clear()` 제공

**테스트**: Java 파일 1개 → Kotlin 변환

### 2.5 ZipCompressor (ZIP 압축기)

**원본**: `debop4k.core.compress.ZipCompressor`
**대상**: `io.bluetape4k.io.compress.ZipCompressor`

- `java.util.zip.ZipOutputStream`/`ZipInputStream` 사용
- debop4k의 `Compressor` 인터페이스 구현 → bluetape4k에는 `io/io` 모듈에 `Compressor` 인터페이스 존재
- **bluetape4k-core는 io/io에 의존하지 않음** → 독립적인 ZipCompressor 구현 필요 (compress/decompress 직접 정의)
- 추가 의존성 없음 (`java.util.zip` 표준 라이브러리)

**테스트**: 기존 테스트 없음 → 신규 작성

### 2.6 ZipBuilder (ZIP 빌더)

**원본**: `debop4k.core.compress.ZipBuilder`
**대상**: `io.bluetape4k.io.compress.ZipBuilder`

- 메모리 내 또는 파일 시스템에 ZIP 생성하는 빌더 패턴
- 내부 클래스 `AddFileToZip`, `AddContentToZip`
- debop4k 전용 유틸 사용: `touch()`, `closeSafe()`, `toUtf8Bytes()` → bluetape4k 유틸로 대체

**테스트**: 기존 테스트 없음 → 신규 작성

### 2.7 ZipFilex (ZipFileSupport.kt)

**원본**: `debop4k.core.compress.ZipFilex`
**대상**: `io.bluetape4k.io.compress.ZipFileSupport.kt`

- 톱레벨 함수: `zlib()`, `gzip()`, `ungzip()`, `zip()`, `unzip()`
- 헬퍼: `addToZip()`, `addFolderToZip()`, `ZipFile.closeSafe()`
- `Wildcard.matchPathOne` 사용 (unzip 필터링) → Wildcard와 함께 이관
- 파일 I/O 집중 → 에러 처리 강화 필요

**테스트**: 기존 테스트 없음 → 신규 작성

### 2.8 Wildcard (와일드카드 매칭)

**원본**: `debop4k.core.utils.Wildcard`
**대상**: `io.bluetape4k.utils.Wildcard`

- `object Wildcard` — `match()`, `matchPath()`, `matchOne()`, `matchPathOne()`
- 순수 문자열 처리, 외부 의존성 없음
- `String?.splits()` 헬퍼 사용 → 표준 `split()` 대체
- 재귀적 와일드카드 매칭 알고리즘 (`*`, `?`, `**`, `\` 이스케이프)

**테스트**: 기존 테스트 없음 → 신규 작성 (edge case 포함)

### 2.9 XXHasher (고속 해시)

**원본**: `debop4k.core.utils.XXHasher`
**대상**: `io.bluetape4k.utils.XXHasher`

- `net.jpountz.xxhash.XXHashFactory` 사용 (lz4-java 라이브러리)
- `StreamingXXHash32`로 vararg 해싱
- **lz4-java 의존성**: `Libs.lz4_java` 존재 (`org.lz4:lz4-java:1.8.0`) — bluetape4k-core build.gradle.kts에 **아직 없음** →
  `compileOnly` 추가 필요
- `hash32`가 stateful (reset → update 패턴) — **thread-safety 이슈**: `synchronized` 또는 `ThreadLocal` 필요

**테스트**: 기존 Kotlin 테스트 1개 → JUnit 5 + Kluent 변환

## 3. 의존성 영향 분석

### 3.1 build.gradle.kts 변경

```kotlin
// 추가 필요 (XXHasher용)
compileOnly(Libs.lz4_java)
```

기존 의존성으로 충분한 것:

- `eclipse_collections` — 이미 `implementation`
- `java.util.zip.*` — JDK 표준
- `commons_compress` — 이미 `compileOnly` (ZipCompressor에는 불필요)

### 3.2 패키지 구조

```
bluetape4k/core/src/main/kotlin/io/bluetape4k/
├── collections/
│   ├── BoundedStack.kt         (신규)
│   ├── PaginatedList.kt        (신규)
│   ├── RingBuffer.kt           (신규)
│   └── permutations/           (신규 디렉토리)
│       ├── Permutation.kt
│       ├── Nil.kt
│       ├── Cons.kt
│       ├── FixedCons.kt
│       ├── PermutationIterator.kt
│       ├── PermutationSupport.kt  (Permutationx → renamed)
│       ├── PermutationStream.kt
│       └── PermutationStreamSupport.kt  (PermutationStreamx → renamed)
├── io/
│   └── compress/               (신규 디렉토리)
│       ├── ZipCompressor.kt
│       ├── ZipBuilder.kt
│       └── ZipFileSupport.kt
└── utils/
    ├── Wildcard.kt             (신규)
    └── XXHasher.kt             (신규)
```

## 4. 설계 결정

### 4.1 Permutation의 fastListOf 의존

debop4k에서 `fastListOf()`는 Eclipse Collections의 `FastList.newList()`를 래핑합니다. bluetape4k-core에도 eclipse-collections가 있으므로
`FastList` 직접 사용 가능합니다. 다만 bluetape4k 스타일에 맞게 `mutableListOf()` 또는 기존 `fastListOf()` 확장을 사용합니다.

### 4.2 ZipCompressor와 io/io Compressor 인터페이스 관계

bluetape4k-core는 io/io 모듈에 의존하지 않으므로, ZipCompressor는 독립적으로 구현합니다.
`compress(ByteArray?): ByteArray` / `decompress(ByteArray?): ByteArray` 시그니처만 유지합니다. io/io의
`Compressor` 인터페이스를 구현하지 않습니다.

### 4.3 PermutationStream 유지 여부

Java Stream API 래퍼(`PermutationStream`)는 Java interop에 유용하므로 유지합니다. Kotlin에서는 `Sequence`와
`asSequence()`를 권장하되, Stream 변환도 제공합니다.

### 4.4 thread-safety 전략

| 클래스          | 현재                                      | 변환 후                                                |
|--------------|-----------------------------------------|-----------------------------------------------------|
| Cons         | `synchronized(lock)` + `@Volatile`      | `ReentrantLock.withLock {}` + `@Volatile`           |
| BoundedStack | `@Synchronized` (15개 메서드)               | `ReentrantLock.withLock {}`                         |
| RingBuffer   | thread-safe 아님                          | `ReentrantLock.withLock {}` 추가                      |
| XXHasher     | thread-safe 아님 (`hash32.reset()` 공유 상태) | `ThreadLocal<StreamingXXHash32>` 또는 `ReentrantLock` |

### 4.5 파일 이름 규칙

bluetape4k 네이밍 규칙에 따라:

- `Permutationx.kt` → `PermutationSupport.kt`
- `PermutationStreamx.kt` → `PermutationStreamSupport.kt`
- `ZipFilex.kt` → `ZipFileSupport.kt`

## 5. 위험 요소

1. **Permutation의 `sorted()`**: Eclipse Collections `FastList.sortThis()` 사용 — 표준 `sortedWith()` 대체 필요
2. **XXHasher의 thread-safety**: `StreamingXXHash32`가 stateful이므로 concurrent 사용 시 데이터 오염 가능 — ThreadLocal 패턴 적용
3. **ZipFileSupport의 Path Traversal**: `unzip`에서 경로 검증 없음 — Zip Slip 취약점 방어 추가 필요
4. **BoundedStack의 insert 재귀**: 큰 인덱스에서 stack overflow 가능 — 반복문으로 변환 검토
5. **PermutationStream**: Java Stream의 `close()` 시멘틱이 완전하지 않음 — 현재 구현 유지 (best-effort)
