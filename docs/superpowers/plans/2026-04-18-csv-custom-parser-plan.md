# bluetape4k-csv 자체 CSV/TSV 파서 구현 플랜

- **Date**: 2026-04-18
- **Spec**: [2026-04-18-csv-custom-parser-design.md](../specs/2026-04-18-csv-custom-parser-design.md)
- **Module**: `bluetape4k-csv` (`io/csv/`)
- **Branch**: `feat/csv-custom-parser` (worktree: `.worktrees/csv-custom-parser`)
- **PR 수**: 6개 (순차 의존)

---

## 전략 요약

univocity-parsers 완전 제거 + 자체 `Record` 인터페이스/`CsvSettings`/`TsvSettings` 도입. V2(
`FlowCsvReader`) 동시 제공. PR 1 → 6 순으로 점진 이행. PR 1~4 기간 `api(Libs.univocity_parsers)` 유지(diff test 의존) → PR 5에서 완전 삭제.

**마이그레이션 범위**: import 치환 + `getValue<T?>(null)` → nullable getter 교체 + deprecated 생성자 교체. "import-only" 마이그레이션이 아닌 **소스
레벨 교체**이며 **실제 코드 수정이 필요**.
**PR 1 독립 머지 가능**: `CsvSettings`/`TsvSettings` 추가는 기존 univocity import에 영향 없이 공존. 신규 Settings는 PR 2 Reader 교체 전까지 inert.

태스크 의존성: **PR N 의 모든 태스크 완료 → PR N+1 시작**. PR 내 일부 태스크는 병렬 가능.

---

## PR 1: internal 엔진 + Settings + benchmark infra 구현

> `io.bluetape4k.csv.Record` 공개 추가는 PR 2에서 — 동일 패키지 단순명 충돌 방지

### Task 1.1: CsvSettings / TsvSettings data class + Default 상수

- **PR**: PR 1
- **complexity**: medium
- **파일**:
    - `io/csv/src/main/kotlin/io/bluetape4k/csv/CsvSettings.kt` (신규)
    - `io/csv/src/main/kotlin/io/bluetape4k/csv/TsvSettings.kt` (신규)
- **설명**:
  `CsvSettings(delimiter, quote, quoteEscape, lineSeparator, trimValues, skipEmptyLines, emptyValueAsNull, emptyQuotedAsNull, detectBom, maxCharsPerColumn, maxColumns, bufferSize)` data class.
  `init` 검증:
  ```
  require(delimiter != quote)
  require(quoteEscape == quote) { "V1은 RFC 4180 doubled-quote 이스케이프만 지원. quoteEscape는 quote와 동일해야 함. 임의 이스케이프 문자는 V2 이후 지원 예정" }
  require(maxCharsPerColumn > 0)
  require(maxColumns > 0)
  require(bufferSize > 0)
  require(lineSeparator.isNotEmpty())
  ```
  `TsvSettings`는 delimiter/quote 제외하되 **`lineSeparator: String`** 포함. **⚠️ 기본값 결정**: 기존 univocity
  `TsvWriter` 출력 행 구분자를 먼저 확인(기존 테스트 스냅샷 기준)한 후 `\r\n` 또는 `\n` 선택 — 변경 시 기존 스냅샷 테스트 전체 churn 발생. `@JvmField` 상수 4종 포함.
  `trimValues`는 reader 전용, writer-facing defaults에도 공존하나 writer에서는 무시됨, KDoc으로 명시.
  **empty string vs null roundtrip 정책**: `DelimitedWriter`는 null → 인용 없는 빈 필드, `""` → `""` 인용 출력. Reader의
  `emptyValueAsNull=true`는 인용 없는 빈 필드만 null로 변환하고 `""` 인용 필드는 빈 문자열 보존. `emptyQuotedAsNull=true`이면
  `""` → null 추가 변환. 이 정책을 KDoc에 명시.

### Task 1.2: ParseException + HeaderIndex

- **PR**: PR 1
- **complexity**: low
- **파일**:
    - `io/csv/src/main/kotlin/io/bluetape4k/csv/internal/ParseException.kt`
    - `io/csv/src/main/kotlin/io/bluetape4k/csv/internal/HeaderIndex.kt`
- **설명**: `ParseException(message, rowNumber, columnNumber, fieldIndex: Int = -1)` — `fieldIndex`는 **0-based
  ** (첫 번째 필드 = 0). `rowNumber`/`columnNumber`는 1-based human-facing. `HeaderIndex` — 헤더명→인덱스 LinkedHashMap. **정책
  **: case-sensitive, 중복 키는 first-wins. `companion object fun of(headers: Array<String>): HeaderIndex`.

### Task 1.3: ArrayRecord (Record 구현체, internal)

- **PR**: PR 1
- **complexity**: high
- **파일**: `io/csv/src/main/kotlin/io/bluetape4k/csv/internal/ArrayRecord.kt`
- **설명**: 내부 구현체(PR 1에서는 interface `Record` 없음). `rawValues`/`_headers`/`headerIndex`/`rowNumber` + 방어적 복사.
  `private fun <T : Any> convert(raw: String?, defaultValue: T): T` — `T : Any` 제약으로 `null` 컴파일 타임 차단. **PR
  1에서 `ArrayRecord : Serializable` 직접 구현** —
  `companion object : KLogging() { private const val serialVersionUID = 1L }`. PR 2에서 `Record` 인터페이스 구현으로 전환.

### Task 1.4: CsvLexer (RFC 4180 5상태 기계) — 핵심

- **PR**: PR 1
- **complexity**: high
- **파일**: `io/csv/src/main/kotlin/io/bluetape4k/csv/internal/CsvLexer.kt`
- **설명**:
  `Iterator<Array<String?>> + Closeable` 상태 기계 (START_FIELD / IN_QUOTED / QUOTE_IN_QUOTED / IN_UNQUOTED / END_ROW). BOM 자동 감지·제거.
  `maxCharsPerColumn` / `maxColumns` 검증 → `ParseException(message, rowNumber, columnNumber, fieldIndex)`.
  **empty line 정의**: 물리적 빈 줄(CRLF/LF/CR만 있는 줄)만 empty line으로 취급. `,, `처럼 구분자만 있는 줄은 null 필드 3개짜리 레코드.
  `skipEmptyLines=true`는 물리적 빈 줄만 건너뜀.
  **skipHeaders 동작**: `skipHeaders=true`이면 첫 번째 행을 `Array<String>` 헤더 메타데이터로 저장하고 레코드 행은 반환하지 않음. 저장된 헤더는 각
  `ArrayRecord` 생성 시 `_headers`/`headerIndex`로 전달 → `getString(name)` 정상 동작 보장.
  `skipHeaders=false`이면 모든 행을 헤더 없는 레코드로 반환.
  `finishField()` — `emptyValueAsNull`, `emptyQuotedAsNull`, `trimValues` 분기 + empty string vs null roundtrip 정책 적용.
  `lastFieldWasQuoted` 추적. CRLF/LF/CR 모두 행 종결자. 마지막 행 LF 없어도 정상 처리.

### Task 1.5: TsvLexer (백슬래시 이스케이프)

- **PR**: PR 1
- **complexity**: medium
- **파일**: `io/csv/src/main/kotlin/io/bluetape4k/csv/internal/TsvLexer.kt`
- **설명**: TSV 전용 — 따옴표 없음, `\t`/`\n`/`\r`/`\\` 백슬래시 이스케이프. 단순 상태 기계(START_FIELD ↔ ESCAPE). `TsvSettings` 사용.
  **⚠️ TSV 이스케이프 = PR 1 게이트**: Task 1.9 `TsvLexerTest`에서 기존 `TsvRecordReaderTest` 케이스를 동일 입력으로 실행. 결과가 다르면 **테스트를 실패로 둔
  채 PR을 머지하지 않음** — "문서화로 우회" 불가. 동작이 다를 경우 univocity 기본값에 맞게 `TsvLexer` 수정 후 통과시키거나, 팀이 의도적 변경을 승인한 경우에만 테스트를 수정하고
  `MIGRATION.md`에 명시.

### Task 1.6: DelimitedWriter / CsvLineWriter / TsvLineWriter

- **PR**: PR 1
- **complexity**: medium
- **파일**:
    - `io/csv/src/main/kotlin/io/bluetape4k/csv/internal/DelimitedWriter.kt`
    - `io/csv/src/main/kotlin/io/bluetape4k/csv/internal/CsvLineWriter.kt`
    - `io/csv/src/main/kotlin/io/bluetape4k/csv/internal/TsvLineWriter.kt`
- **설명**: `DelimitedWriter(writer, delimiter, quote, quoteEscape, lineSeparator)`.
  `needsQuoting(s)` — delimiter/quote/CR/LF/앞뒤 공백. `writeQuoted(s)` — RFC 4180 `""` 이스케이프. **null → 인용 없는 빈
  필드, `""` → `""` 인용 출력** (roundtrip 보장). `close()` — flush 포함. `TsvLineWriter`는 `TsvSettings.lineSeparator`를 전달.

### Task 1.7: RecordFactory (internal 팩토리)

- **PR**: PR 1
- **complexity**: low
- **파일**: `io/csv/src/main/kotlin/io/bluetape4k/csv/RecordFactory.kt`
- **설명**: **PR 1에서 반환 타입 `ArrayRecord`** — `internal fun recordOf(values, headers, rowNumber): ArrayRecord`.
  `Record` 인터페이스는 PR 2에서 추가. PR 2(Task 2.2)에서 반환 타입을 `io.bluetape4k.csv.Record`로 변경.

### Task 1.8: kotlinx-benchmark Gradle 설정

- **PR**: PR 1
- **complexity**: low
- **파일**: `io/csv/build.gradle.kts` (수정)
- **설명**: `kotlin("plugin.allopen")`, `id(Plugins.kotlinx_benchmark)`.
  `allOpen { annotation("org.openjdk.jmh.annotations.State") }`. `benchmark { targets { register("test") { ... } } }`.
  `testImplementation(Libs.kotlinx_benchmark_runtime, Libs.jmh_core)`. **즉시 검증**:
  `./gradlew :bluetape4k-csv:compileTestKotlin` 통과 확인 후 내부 테스트 진행 — build-system 변경은 blast radius가 넓으므로 컴파일 검증을 앞에 배치.

### Task 1.9: internal 단위 테스트 5종

- **PR**: PR 1
- **complexity**: medium
- **파일**:
    - `io/csv/src/test/kotlin/io/bluetape4k/csv/internal/CsvLexerTest.kt`
    - `io/csv/src/test/kotlin/io/bluetape4k/csv/internal/TsvLexerTest.kt`
    - `io/csv/src/test/kotlin/io/bluetape4k/csv/internal/DelimitedWriterTest.kt`
    - `io/csv/src/test/kotlin/io/bluetape4k/csv/internal/ArrayRecordTest.kt`
    - `io/csv/src/test/kotlin/io/bluetape4k/csv/internal/HeaderIndexTest.kt`
- **설명**: CsvLexer — 5상태 전이 전체, empty line vs
  `,, ` 구분, skipHeaders 헤더 저장 검증, ParseException(rowNumber, columnNumber, fieldIndex 0-based). TsvLexer — 백슬래시 이스케이프 케이스
  **+ 기존 `TsvRecordReaderTest` 케이스 동일 입력으로 대조 (PR 1 게이트)**. DelimitedWriter — needsQuoting, null vs
  `""` 출력 구분, 공백 라운드트립. ArrayRecord — typed getter, convert(). HeaderIndex — lookup, first-wins, case-sensitive. *
  *seeded deterministic generator** 사용 (Kotlin 표준 `kotlin.random.Random(seed)` — Hypothesis/property-based 라이브러리 불필요).

### Task 1.10: CsvParserBenchmark.kt 초안

- **PR**: PR 1
- **complexity**: low
- **파일**: `io/csv/src/test/kotlin/io/bluetape4k/csv/benchmark/CsvParserBenchmark.kt`
- **설명**: `@State(Scope.Benchmark)`, `@BenchmarkMode(Throughput)`, `@Warmup(2)`, `@Measurement(5, 1s)`.
  `nativeCsvRead_small`/`nativeCsvRead_medium` — **`CsvLexer` 직접 호출
  ** (PR 1 시점 CsvRecordReader는 univocity 사용 중). univocity `@Benchmark`(`univocityCsvRead_*`) 실제 활성 메서드로 포함. **PR 2 완료 후
  주의**: Task 2.8에서 `nativeCsvRead_*`를 CsvRecordReader로 교체 (내부 엔진 직접 측정 → public API 측정).

### Task 1.11: bluetape4k-patterns 체크리스트 (PR 1)

- **PR**: PR 1
- **complexity**: low
- **파일**: (코드 검토)
- **설명**: `ide_diagnostics` 경고 0건. `ArrayRecord` — `companion object : KLogging()`, `serialVersionUID = 1L`,
  `Serializable` 직접 구현. `CsvSettings`/`TsvSettings` — 한국어 KDoc. `CsvLexer`/`TsvLexer` —
  `companion object : KLogging()`. 모든 internal 클래스 `private val` 가시성 최소화.

---

## PR 2: Record 공개 + Reader 엔진 교체 + 전체 import 마이그레이션

> `Record.kt` 추가와 기존 import 교체를 **동일 PR에서 원자적으로** 처리 (단순명 충돌 방지)
> 마이그레이션은 **import 교체 + nullable getter 교체** 두 단계 필요

### Task 2.1: io.bluetape4k.csv.Record 공개 인터페이스

- **PR**: PR 2
- **complexity**: high
- **파일**: `io/csv/src/main/kotlin/io/bluetape4k/csv/Record.kt` (신규)
- **설명**: `interface Record : Serializable`. `rowNumber: Long`, `size: Int`, `values: Array<String?>`,
  `headers: Array<String>?`. `<T : Any> getValue(index/name, defaultValue: T): T` — T : Any 제약.
  `getString(index/name): String?`. typed non-null default getter. nullable getter (`getIntOrNull`, `getLongOrNull`,
  `getDoubleOrNull`, `getFloatOrNull`, `getBigDecimal`) — `getString(i)?.toXxxOrNull()`. ArrayRecord가 이 인터페이스 구현으로 전환.

### Task 2.2: ArrayRecord → Record 구현으로 전환 + RecordFactory 반환 타입 변경

- **PR**: PR 2
- **complexity**: medium
- **파일**:
    - `io/csv/src/main/kotlin/io/bluetape4k/csv/internal/ArrayRecord.kt` (수정)
    - `io/csv/src/main/kotlin/io/bluetape4k/csv/RecordFactory.kt` (수정)
- **설명**: `internal class ArrayRecord : Record`. `recordOf()` 반환 타입 `ArrayRecord` → `io.bluetape4k.csv.Record`.

### Task 2.3: CsvRecordReader / TsvRecordReader 엔진 교체 + 생성자 오버로드

- **PR**: PR 2
- **complexity**: high
- **파일**:
    - `io/csv/src/main/kotlin/io/bluetape4k/csv/CsvRecordReader.kt` (수정)
    - `io/csv/src/main/kotlin/io/bluetape4k/csv/TsvRecordReader.kt` (수정)
- **설명**: `CsvRecordReader(settings: CsvSettings = DefaultCsvSettings)` 신규 생성자. 내부 `CsvParser` → `CsvLexer` 교체.
  `read(input, encoding, skipHeaders, transform)` 내부에서 `CsvLexer` 순회 →
  `ArrayRecord(values, headers, headerIndex, rowNumber)` 생성 → `transform`. **Reader 계약**:
  `skipHeaders=true`이면 첫 행 헤더 메타데이터 저장 → 이후 레코드 `getString(name)` 동작 보장. 기존 `CsvRecordReader(CsvParserSettings)`
  `@Deprecated` 마킹. TSV 동일.

### Task 2.4: 모듈 내 모든 Record import 교체

- **PR**: PR 2
- **complexity**: medium
- **파일**: `io/csv/src/**/*.kt` 전체
- **설명**: `import com.univocity.parsers.common.record.Record` →
  `import io.bluetape4k.csv.Record` 전체 교체. Task 2.1과 원자적으로 처리. grep으로 전수 조사.

### Task 2.5: getValue<T?>(null) 콜사이트 nullable getter 교체

- **PR**: PR 2
- **complexity**: medium
- **파일**: `io/csv/src/main/**/*.kt`, `io/csv/src/test/**/*.kt`, `io/csv/README.md`,
  `io/csv/README.ko.md`, 마이그레이션 예시 파일 전체
- **설명**: `getValue<String?>(n, null)` → `getString(n)`, `getValue<Int?>(n, null)` → `getIntOrNull(n)` 등. **grep
  범위: `src/main`, `src/test`, `README.md`, `README.ko.md`, examples 전체** — 테스트만 확인하면 main/docs에 잔존 가능.
  `T : Any` 제약으로 기존 nullable 호출은 컴파일 에러.

### Task 2.6: UnivocityVsNativeDiffTest.kt (internal writer diff)

- **PR**: PR 2
- **complexity**: medium
- **파일**: `io/csv/src/test/kotlin/io/bluetape4k/csv/UnivocityVsNativeDiffTest.kt` (신규)
- **설명**: seeded `Random(42)` 기반 CSV 1000개 생성. univocity `CsvParser`와 자체 `CsvLexer` reader 결과 비교. **TSV 포함**: univocity
  `TsvParser` vs `TsvLexer` 비교. **Writer diff**: **내부 `DelimitedWriter`** vs univocity
  `CsvWriter` 출력 문자열 비교 — 이 시점에 public `CsvRecordWriter`는 아직 univocity 사용 중이므로 internal writer diff만 가능. **public writer
  diff는 PR 3 Task 3.2로 이동**. PR 5에서 삭제.

### Task 2.7: RFC4180ComplianceTest.kt

- **PR**: PR 2
- **complexity**: medium
- **파일**: `io/csv/src/test/kotlin/io/bluetape4k/csv/RFC4180ComplianceTest.kt` (신규)
- **설명**: RFC 4180 7개 규칙, BOM 케이스 2개(`skipHeaders=true`/`false`). null vs
  `""` roundtrip 케이스 포함 — 인용 빈 필드와 무인용 빈 필드가 다르게 읽히는지 확인.

### Task 2.8: CsvParserBenchmark → CsvRecordReader 전환

- **PR**: PR 2
- **complexity**: low
- **파일**: `io/csv/src/test/kotlin/io/bluetape4k/csv/benchmark/CsvParserBenchmark.kt` (수정)
- **설명**: PR 2에서 `CsvRecordReader`가 CsvLexer로 전환됐으므로 `nativeCsvRead_*` 벤치마크를
  `CsvRecordReader.read(...)` 호출로 교체. 이를 통해 public API overhead(header handling 포함)까지 측정. univocity
  `@Benchmark` 메서드는 그대로 유지.

---

## PR 3: RecordWriter 엔진 교체

### Task 3.1: CsvRecordWriter / TsvRecordWriter 엔진 교체

- **PR**: PR 3
- **complexity**: medium
- **파일**:
    - `io/csv/src/main/kotlin/io/bluetape4k/csv/CsvRecordWriter.kt` (수정)
    - `io/csv/src/main/kotlin/io/bluetape4k/csv/TsvRecordWriter.kt` (수정)
- **설명**: 내부 univocity `CsvWriter` → `CsvLineWriter` 교체 (TSV는 `TsvLineWriter`).
  `CsvRecordWriter(settings: CsvSettings = DefaultCsvWriterSettings)` 신규. `writeHeaders/writeRow/writeAll` 모두
  `DelimitedWriter.writeRow(List<Any?>)` 경유. 기존 `CsvWriterSettings` 생성자 `@Deprecated` 마킹. *
  *`CsvRecordWriter.close()` → `DelimitedWriter.close()` 명시적 위임** — flush 포함. TSV 동일.

### Task 3.2: 기존 Writer 회귀 테스트 + public writer output diff

- **PR**: PR 3
- **complexity**: low
- **파일**: `io/csv/src/test/kotlin/io/bluetape4k/csv/CsvRecordWriterTest.kt`, `TsvRecordWriterTest.kt`,
  `RecordWriterSupportTest.kt` (기존)
- **설명**: import 교체 후 기존 Writer 테스트 통과 확인. **public writer output diff 추가**: `CsvRecordWriter.write(rows)`와 univocity
  `CsvWriter.write(rows)` 출력 문자열 비교 (PR 2 Task 2.6은 internal DelimitedWriter diff였으므로, PR 3에서 public API diff 완성). 공백 라운드트립, null vs
  `""` 구분 확인.

### Task 3.3: @Deprecated Quick Fix 완료

- **PR**: PR 3
- **complexity**: low
- **파일**: 모듈 전체 `@Deprecated` 사용처
- **설명**: `ide_diagnostics`로 경고 0건 확인. Quick Fix 일괄 적용. **nontrivial CsvParserSettings 필드 정책**: `numberFormat`,
  `columnSelector`, `columnProcessor` 등은 V1에서 **미지원 — compile-time 제거
  **. MIGRATION.md(Task 5.1)에 "제거된 Settings 필드" 목록으로 문서화. Quick Fix만으로 완전 마이그레이션 불가함을 사용자에게 명시.

### Task 3.4: CsvWriterBenchmark.kt 작성

- **PR**: PR 3
- **complexity**: low
- **파일**: `io/csv/src/test/kotlin/io/bluetape4k/csv/benchmark/CsvWriterBenchmark.kt` (신규)
- **설명**: `nativeWrite_small`/`nativeWrite_medium` + univocity `@Benchmark` (
  `univocityWrite_*`) 실제 활성 메서드. PR 5 Task 5.4에서 univocity 메서드 삭제.

---

## PR 4: 코루틴 네이티브화

### Task 4.1: SuspendCsvRecordReader 교체 (channelFlow + while loop)

- **PR**: PR 4
- **complexity**: high
- **파일**: `io/csv/src/main/kotlin/io/bluetape4k/csv/coroutines/SuspendCsvRecordReader.kt` (수정)
- **설명**: `read(input, encoding, skipHeaders, transform): Flow<T>`:
  ```kotlin
  channelFlow {
      val reader = input.bufferedReader(encoding)
      val lexer = CsvLexer(reader, settings)
      val headers: Array<String>? = if (skipHeaders && lexer.hasNext())
          lexer.next().map { it ?: "" }.toTypedArray()
      else null
      val headerIndex = headers?.let { HeaderIndex.of(it) }
      var rowNum = 0L
      while (lexer.hasNext()) {
          ensureActive()
          val values = lexer.next()
          send(ArrayRecord(values, headers, headerIndex, ++rowNum))
      }
  }.buffer(Channel.RENDEZVOUS).flowOn(Dispatchers.IO).map { transform(it) }
  ```
  **`forEach` 절대 금지** — `ensureActive()` 누락으로 취소 협력 불가.
  `ArrayRecord` 생성 시 headers + HeaderIndex 반드시 포함. SuspendTsvRecordReader 동일 패턴.

### Task 4.2: SuspendCsvRecordWriter 교체 (Mutex 기반)

- **PR**: PR 4
- **complexity**: medium
- **파일**: `io/csv/src/main/kotlin/io/bluetape4k/csv/coroutines/SuspendCsvRecordWriter.kt` (수정)
- **설명**: `Mutex` 직렬화. `writeHeaders/writeRow` —
  `withContext(Dispatchers.IO) { mutex.withLock { engine.writeRow(...) } }`. `writeAll(rows: Flow<Iterable<*>>)` —
  `rows.collect { writeRow(it) }` (**`flowOn` 제거** — upstream 컨텍스트 변경 부작용 방지). 기존 `@Synchronized`/
  `synchronized {}` 제거. SuspendTsvRecordWriter 동일.

### Task 4.3: readLargeFile() — 멤버 메서드

- **PR**: PR 4
- **complexity**: medium
- **파일**: `io/csv/src/main/kotlin/io/bluetape4k/csv/coroutines/SuspendCsvRecordReader.kt` (수정)
- **설명**: `SuspendCsvRecordReader` 클래스 내 멤버 메서드 — private `settings` 접근 필요.
  `fun readLargeFile(path, encoding, skipHeaders, transform): Flow<T>` —
  `channelFlow { path.toFile().inputStream().bufferedReader(encoding).use { val lexer = CsvLexer(it, settings); while (lexer.hasNext()) { ensureActive(); ... } } }.buffer(Channel.RENDEZVOUS).flowOn(Dispatchers.IO)`.
  **FileInputStream 사용 이유**: RFC 4180 인용 필드 내 개행이 존재할 수 있어 line-based reader(`readLines`,
  `readUtf8LinesAsFlow`)는 필드를 파괴함. `bluetape4k-io`의 `readAllBytesSuspending()`은 전체 파일을 메모리에 올리므로 대용량 파일에 부적합.
  `FileInputStream + BufferedReader`만이 RFC 4180을 지키면서 스트리밍 처리 가능 — 이 예외를 KDoc에 명시. okio `readUtf8LinesAsFlow()` 절대 금지.
  **TSV 동일**: `SuspendTsvRecordReader.readLargeFile()` 추가.

### Task 4.4: readFile() / writeFile() — 멤버 메서드

- **PR**: PR 4
- **complexity**: medium
- **파일**:
    - `io/csv/src/main/kotlin/io/bluetape4k/csv/coroutines/SuspendCsvRecordReader.kt` (수정)
    - `io/csv/src/main/kotlin/io/bluetape4k/csv/coroutines/SuspendCsvRecordWriter.kt` (수정)
- **설명**: `readFile` — `flow { val bytes = path.readAllBytesSuspending(); emitAll(read(bytes.inputStream(), ...)) }`.
  `writeFile` — `rows.collect` → ByteArrayOutputStream → `path.writeSuspending(bytes, append)`. **⚠️ writeFile은 스트리밍이 아님
  ** — 전체 Flow를 메모리에 수집. 대용량 쓰기는 `OutputStreamWriter`에 `writeRow` 직접 사용 권장. KDoc에 명시. TSV 동일.

### Task 4.5: SuspendCsvNativeTest.kt (cancellation + backpressure)

- **PR**: PR 4
- **complexity**: medium
- **파일**: `io/csv/src/test/kotlin/io/bluetape4k/csv/coroutines/SuspendCsvNativeTest.kt` (신규)
- **설명**: **cancellation**: 1M 행, 10ms 후 취소 → `isCancelled == true` + 파싱된 행 수 < 전체의 1%.
  **backpressure**: producer 측 계수를 위해 *
  *`internal class InstrumentedCsvLexer(delegate: CsvLexer, val parseCount: AtomicInteger)`** 를 테스트 소스(
  `src/test`)에 정의하거나, `SuspendCsvRecordReader`의 `internal` 생성자 파라미터로
  `lexerFactory: (Reader, CsvSettings) -> Iterator<Array<String?>> = ::CsvLexer` 주입. **프로덕션 코드에 test-only hook 노출 금지** —
  `internal` 생성자 또는 별도 fake lexer를 test source set에만 위치. `take(10).collect { delay(100) }` → produced < 20 확인.

### Task 4.6: CsvFileBenchmark.kt

- **PR**: PR 4
- **complexity**: low
- **파일**: `io/csv/src/test/kotlin/io/bluetape4k/csv/benchmark/CsvFileBenchmark.kt` (신규)
- **설명**: `readFile_bluetape4kIo` vs `readLargeFile_channelFlow` AverageTime 비교. `runBlocking { ... collect { } }`.

---

## PR 5: univocity 완전 삭제

> **⚠️ 실행 순서 엄수**: MIGRATION.md 초안 → deprecated 생성자 삭제 → baseline → @Benchmark 삭제 → 의존성 삭제.
> MIGRATION.md를 먼저 작성해야 삭제 대상 API 전체가 보이는 상태에서 검토 가능.

### Task 5.1: MIGRATION.md 초안 작성 (삭제 전 선행)

- **PR**: PR 5
- **complexity**: medium
- **파일**: `io/csv/MIGRATION.md` (신규)
- **설명**: **삭제 작업 전에 먼저 작성** — 삭제 후에는 univocity 타입이 보이지 않아 누락 가능. ① import 치환 표. ② Settings 수동 변환표 (
  `CsvParserSettings` 필드 → `CsvSettings` 매핑; nontrivial 자동화 불가). ③ **제거된 Settings 필드 목록**: `numberFormat`,
  `columnSelector`, `columnProcessor` 등 V1 미지원 필드 전체 — 각 필드별 "대안 없음/부분 대안/수동 구현 필요" 명시. ④
  `getValue<T?>(null)` 치환 표. ⑤ deprecated 생성자 → 대체 생성자. ⑥ Before/After 코드 예시. ⑦ TSV 이스케이프 시맨틱 변경 여부.

### Task 5.2: deprecated univocity 생성자 삭제 (의존성 삭제 전 필수)

- **PR**: PR 5
- **complexity**: medium
- **파일**:
    - `io/csv/src/main/kotlin/io/bluetape4k/csv/CsvRecordReader.kt` (수정)
    - `io/csv/src/main/kotlin/io/bluetape4k/csv/TsvRecordReader.kt` (수정)
    - `io/csv/src/main/kotlin/io/bluetape4k/csv/CsvRecordWriter.kt` (수정)
    - `io/csv/src/main/kotlin/io/bluetape4k/csv/TsvRecordWriter.kt` (수정)
    - `io/csv/src/main/kotlin/io/bluetape4k/csv/coroutines/SuspendCsvRecordReader.kt` (수정)
    - `io/csv/src/main/kotlin/io/bluetape4k/csv/coroutines/SuspendCsvRecordWriter.kt` (수정)
    - `io/csv/src/main/kotlin/io/bluetape4k/csv/coroutines/SuspendTsvRecordReader.kt` (수정)
    - `io/csv/src/main/kotlin/io/bluetape4k/csv/coroutines/SuspendTsvRecordWriter.kt` (수정)
- **설명**: PR 2~3에서 `@Deprecated` 마킹된 univocity 타입 파라미터 생성자·메서드 **전체 삭제**. `SuspendTsvRecordReader`/
  `SuspendTsvRecordWriter` 포함 누락 주의. `ide_find_references`로 univocity 타입 참조 0건 확인. **이 단계를 빠뜨리면 Task 5.5 의존성 삭제 후 컴파일 에러
  **.

### Task 5.3: 벤치마크 baseline 기록 (univocity @Benchmark 삭제 전)

- **PR**: PR 5
- **complexity**: low
- **파일**: `build/reports/benchmarks/` (JSON)
- **설명**: `./gradlew :bluetape4k-csv:benchmark`. **repo-local 기록 필수**: PR description에 baseline 수치 붙여넣기 (repo 외부
  `wiki/testlogs`는 선택적). 수치가 PR에 남아야 PR 5 리뷰 시 baseline 확인 가능.

### Task 5.4: CsvParserBenchmark / CsvWriterBenchmark 에서 univocity @Benchmark 삭제

- **PR**: PR 5
- **complexity**: low
- **파일**:
    - `io/csv/src/test/kotlin/io/bluetape4k/csv/benchmark/CsvParserBenchmark.kt` (수정)
    - `io/csv/src/test/kotlin/io/bluetape4k/csv/benchmark/CsvWriterBenchmark.kt` (수정)
- **설명**: univocity `@Benchmark` 메서드(`univocityCsvRead_*`, `univocityWrite_*`) 삭제. **의존성 삭제(Task 5.5) 이전에 수행 필수**.

### Task 5.5: api(Libs.univocity_parsers) + UnivocityVsNativeDiffTest 삭제

- **PR**: PR 5
- **complexity**: low
- **파일**:
    - `io/csv/build.gradle.kts` (수정)
    - `buildSrc/src/main/kotlin/Libs.kt` (수정)
    - `io/csv/src/test/kotlin/io/bluetape4k/csv/UnivocityVsNativeDiffTest.kt` (삭제)
- **설명**: `api(Libs.univocity_parsers)` 제거. `./gradlew :bluetape4k-csv:dependencies` transitive 0건 확인. DiffTest 삭제.

### Task 5.6: CvsParserDefaults.kt 파일 삭제

- **PR**: PR 5
- **complexity**: low
- **파일**: `io/csv/src/main/kotlin/io/bluetape4k/csv/CvsParserDefaults.kt` (삭제)
- **설명**: Glob으로 실제 파일명 확인(오타 여부). `ide_find_references`로 사용처 0건 확인 후 삭제.

### Task 5.7: compileKotlin 검증 — univocity 참조 0건 확인

- **PR**: PR 5
- **complexity**: low
- **파일**: (빌드)
- **설명**: `./gradlew :bluetape4k-csv:compileKotlin :bluetape4k-csv:compileTestKotlin` → 오류 0건.
  `rg "univocity" io/csv/src/ --include="*.kt"` 잔존 참조 0건. 실패 시 Task 5.2 누락 파일 추가 삭제.

### Task 5.8: 벤치마크 재실행 + ±20% 확인

- **PR**: PR 5
- **complexity**: low
- **파일**: `build/reports/benchmarks/` (JSON)
- **설명**: `./gradlew :bluetape4k-csv:benchmark`. Task 5.3 baseline 대비 ±20% 이내. **결과는 testlog(`wiki/testlogs/2026-04.md`)
  및 PR description 모두에 기록** — MIGRATION.md는 API 마이그레이션 가이드 전용, 벤치마크 수치 미포함.

### Task 5.9: README.md / README.ko.md 업데이트

- **PR**: PR 5
- **complexity**: medium
- **파일**:
    - `io/csv/README.md` (수정)
    - `io/csv/README.ko.md` (수정)
- **설명**: 언어 전환 링크, Architecture → UML → Features → Examples 순서. univocity 제거 반영, MIGRATION.md 링크. V1 API 예시. **V2 프리뷰
  링크 금지** — dead link.

### Task 5.10: superpowers index + testlog 업데이트

- **PR**: PR 5
- **complexity**: low
- **파일**:
    - `docs/superpowers/index/2026-04.md` (수정)
    - `docs/superpowers/INDEX.md` (수정)
    - Obsidian `wiki/testlogs/2026-04.md` (선택적, 외부 경로)
- **설명**: index 항목 추가, 카운트 갱신, testlog 표 맨 위 행 추가.

---

## PR 6: V2 인터페이스

### Task 6.1: CsvRow (V2 불변 레코드 타입)

- **PR**: PR 6
- **complexity**: medium
- **파일**: `io/csv/src/main/kotlin/io/bluetape4k/csv/v2/CsvRow.kt` (신규)
- **설명**: `data class CsvRow(values: List<String?>, headers: List<String>?, rowNumber: Long) : Serializable`.
  `companion object : KLogging() { private const val serialVersionUID = 1L }`. `getString(index/name): String?`,
  `getInt/Long/Double/Boolean(index/name, default)`. 헤더명 룩업 — `headers?.indexOf(name)` + `getOrNull`.

### Task 6.2: CsvReaderConfig / CsvWriterConfig (var 기반 mutable builder)

- **PR**: PR 6
- **complexity**: medium
- **파일**:
    - `io/csv/src/main/kotlin/io/bluetape4k/csv/v2/CsvReaderConfig.kt` (신규)
    - `io/csv/src/main/kotlin/io/bluetape4k/csv/v2/CsvWriterConfig.kt` (신규)
- **설명**:
  `class CsvReaderConfig { var delimiter = ','; var quote = '"'; var trimValues = false; var skipEmptyLines = true; var emptyValueAsNull = true; var detectBom = true; ... }`.
  `init { require(delimiter != quote) }`. `internal fun toCsvSettings(): CsvSettings`.
  **CsvWriterConfig 구체 필드**:
    - `var quoteAll: Boolean = false` — true이면 모든 필드를 인용 출력 (`DelimitedWriter.writeQuoted()` 항상 호출); false이면
      `needsQuoting()` 판단.
    - `var lineSeparator: String = "\r\n"`
    - `internal fun toDelimitedWriterSettings()` — V1 `CsvLineWriter` 생성자 파라미터로 변환.
      **V2 전용 필드 검증**: `quoteAll` 등은 V1 `CsvSettings.init`에 위임 불가 → `CsvWriterConfig.init { }` 에서 직접 검증.

### Task 6.3: FlowCsvReader / FlowCsvReaderImpl

- **PR**: PR 6
- **complexity**: high
- **파일**:
    - `io/csv/src/main/kotlin/io/bluetape4k/csv/v2/FlowCsvReader.kt` (신규)
    - `io/csv/src/main/kotlin/io/bluetape4k/csv/v2/FlowCsvReaderImpl.kt` (신규, internal)
- **설명**:
  `interface FlowCsvReader { val config; fun read(input, encoding, skipHeaders): Flow<CsvRow>; fun readFile(path, encoding, skipHeaders): Flow<CsvRow> }`.
  `internal class FlowCsvReaderImpl(config)` — V1 `CsvLexer` 재사용. `channelFlow + buffer(RENDEZVOUS) + flowOn(IO)`.
  `while (lexer.hasNext()) { ensureActive(); ... }` 루프 필수. ArrayRecord → CsvRow 변환 포함.
  **`readFile` 스트리밍 정책**: V1 `readLargeFile` 패턴 적용 — `FileInputStream(path.toFile())` + `channelFlow` 스트리밍.
  `readAllBytesSuspending()` 또는 `Path.readBytes()` 사용 금지(전체 파일 메모리 로드). RFC 4180 멀티라인 인용 필드 보장을 위해
  `FileInputStream` 직접 사용.

### Task 6.4: FlowCsvWriter / FlowCsvWriterImpl

- **PR**: PR 6
- **complexity**: medium
- **파일**:
    - `io/csv/src/main/kotlin/io/bluetape4k/csv/v2/FlowCsvWriter.kt` (신규)
    - `io/csv/src/main/kotlin/io/bluetape4k/csv/v2/FlowCsvWriterImpl.kt` (신규, internal)
- **설명**:
  `interface FlowCsvWriter : Closeable { val config: CsvWriterConfig; suspend fun writeHeaders(...); suspend fun writeRow(...); suspend fun writeAll(rows: Flow<Iterable<*>>); suspend fun writeFile(...): Long }`.
  `internal class FlowCsvWriterImpl(config)` — V1 `CsvLineWriter` 재사용. `config.quoteAll`에 따라 `writeQuoted` vs
  `needsQuoting` 분기. `Mutex` 직렬화.
  **⚠️ `writeFile`은 스트리밍이 아님**: `rows: Flow<Iterable<*>>`를 `collect`로 전체 순회하며
  `OutputStreamWriter`에 순차 기록. Flow 자체가 lazy이므로 메모리 폭발 없음. 단, 쓰기 중 예외 시 부분 파일이 남음 — 호출 측에서 임시 파일 + rename 패턴 권장. 대용량 쓰기는
  `FlowCsvWriter.writeRow()`를 직접 반복 사용 권장.

### Task 6.5: CsvExtensions (DSL 생성자 + V1↔V2 변환)

- **PR**: PR 6
- **complexity**: medium
- **파일**: `io/csv/src/main/kotlin/io/bluetape4k/csv/v2/CsvExtensions.kt` (신규)
- **설명**: `fun csvReader(block: CsvReaderConfig.() -> Unit = {}): FlowCsvReader`.
  `fun tsvReader(block: CsvReaderConfig.() -> Unit = {}): FlowCsvReader = FlowCsvReaderImpl(csvReaderConfig(block).also { it.delimiter = '\t' })` —
  **`= {}` 필수**, block 후 delimiter 강제. `csvWriter`/`tsvWriter` 동일. `fun Record.toCsvRow(): CsvRow` — **public**.
  `internal fun CsvRow.toRecord(): Record` — **internal**, ArrayRecord/HeaderIndex 캡슐화.

### Task 6.6: V2 통합 테스트

- **PR**: PR 6
- **complexity**: medium
- **파일**:
    - `io/csv/src/test/kotlin/io/bluetape4k/csv/v2/FlowCsvReaderTest.kt` (신규)
    - `io/csv/src/test/kotlin/io/bluetape4k/csv/v2/FlowCsvWriterTest.kt` (신규)
    - `io/csv/src/test/kotlin/io/bluetape4k/csv/v2/CsvRowTest.kt` (신규)
- **설명**: DSL 빌더 동작, `tsvReader { delimiter = ',' }` → delimiter `\t` 강제 확인. `Record.toCsvRow()` roundtrip.
  `CsvRow.toRecord()` — **internal이므로 동일 Gradle 모듈 내 테스트** (별도 integration test 모듈 금지). BOM, null vs `""` roundtrip.

### Task 6.7: README에 V2 섹션 추가

- **PR**: PR 6
- **complexity**: low
- **파일**:
    - `io/csv/README.md` (수정)
    - `io/csv/README.ko.md` (수정)
- **설명**: V2 소개, V1 vs V2 비교 표, DSL 예시, `readFile(path)` 예시. **`Record.toCsvRow()` (public) 예시만** —
  `CsvRow.toRecord()` (internal)은 README 제외.

---

## 태스크 목록 요약

**총 태스크 수: 46개**

| PR     | 태스크 수  | high                          | medium | low    |
|--------|--------|-------------------------------|--------|--------|
| PR 1   | 11     | 2 (CsvLexer, ArrayRecord)     | 4      | 5      |
| PR 2   | 8      | 2 (Record 인터페이스, Reader 교체)   | 5      | 1      |
| PR 3   | 4      | 0                             | 1      | 3      |
| PR 4   | 6      | 1 (SuspendReader channelFlow) | 4      | 1      |
| PR 5   | 10     | 0                             | 3      | 7      |
| PR 6   | 7      | 1 (FlowCsvReader)             | 5      | 1      |
| **합계** | **46** | **6**                         | **22** | **18** |

### 복잡도 분포

- **high (6)
  **: CsvLexer RFC 4180 상태 기계, ArrayRecord 타입 시스템, Record 공개 인터페이스, Reader 엔진 교체, SuspendReader channelFlow, FlowCsvReader V2
- **medium (22)**: Settings/Writer/Config, 테스트, 마이그레이션, deprecated 삭제
- **low (18)**: Quick Fix, 파일 삭제, README, 벤치마크, 의존성

### PR 간 의존성

PR 1 → PR 2 → PR 3 → PR 4 → PR 5 → PR 6

### 핵심 원자적 처리 지점

- **PR 2**: `Record.kt` + import 교체 + `getValue<T?>(null)` 교체를 한 커밋에서
- **PR 5**: MIGRATION.md 초안(5.1) → deprecated 삭제(5.2) → baseline(5.3) → @Benchmark 삭제(5.4) → 의존성 삭제(5.5)

### Worktree 셋업

```bash
# plan/spec은 develop에서 편집. 구현 시작 전 worktree 생성.
git worktree add .worktrees/csv-custom-parser -b feat/csv-custom-parser develop
```

### 테스트 검증 게이트

- PR 1: internal 단위 테스트 5종 + `compileTestKotlin` + TSV 이스케이프 게이트 통과
- PR 2: 기존 18개 회귀 테스트 + UnivocityVsNativeDiffTest (CSV+TSV, internal writer) + RFC4180 통과
- PR 3: Writer 회귀 테스트 + public writer output diff + `@Deprecated` 경고 0건
- PR 4: SuspendCsvNativeTest (cancellation + backpressure hook) 통과
- PR 5: 빌드 통과, univocity 참조 0건, 벤치마크 ±20% 이내
- PR 6: V2 통합 테스트 + V1↔V2 roundtrip
