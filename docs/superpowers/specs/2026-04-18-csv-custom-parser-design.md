# bluetape4k-csv 자체 CSV/TSV 파서 구현 설계

- **Date**: 2026-04-18
- **Module**: `bluetape4k-csv` (`io/csv/`)
- **Branch**: `feat/csv-custom-parser` (worktree: `.worktrees/csv-custom-parser`)
- **Goal**: `com.univocity:univocity-parsers` **파서 엔진** 제거, 자체 RFC 4180 준수 CSV/TSV 파서·라이터 구현
- **Compatibility**: ⚠️ **Source-level 마이그레이션** — univocity 타입 import를
  `io.bluetape4k.csv.*`로 변경. 동작·메서드명은 동일하므로 import 교체 수준.

> **[확정 전략]**: univocity-parsers 완전 제거. 자체 `Record` 인터페이스 + `CsvSettings`/`TsvSettings` 정의. V2(`FlowCsvReader`) 동시 제공.

---

## 0. 전략: univocity 완전 제거, 자체 타입 정의

### 핵심 결정 — 무엇을 교체하는가

| 항목                                           | 현재 (univocity)  | 변경 후 (자체 구현)                                                                 | 마이그레이션                           |
|----------------------------------------------|-----------------|------------------------------------------------------------------------------|----------------------------------|
| `com.univocity.parsers.common.record.Record` | univocity 인터페이스 | **`io.bluetape4k.csv.Record`** 자체 인터페이스                                      | import 교체                        |
| `CsvParserSettings`, `TsvParserSettings`     | univocity 클래스   | **`CsvSettings`, `TsvSettings`** data class                                  | 클래스명 + import 교체                 |
| `CsvWriterSettings`, `TsvWriterSettings`     | univocity 클래스   | **`CsvSettings`·`TsvSettings`** 공용 (별도 Writer 클래스 없음, `trimValues=false` 명시) | 클래스명 교체                          |
| `DefaultCsvParserSettings` 등 4개 `@JvmField`  | univocity 인스턴스  | **`DefaultCsvSettings` 등** 자체 상수                                             | 상수명 교체                           |
| `CsvParser`, `TsvParser` (내부)                | univocity 엔진    | **자체 `CsvLexer`, `TsvLexer`**                                                | 내부만 영향                           |
| `CsvWriter`, `TsvWriter` (내부)                | univocity 엔진    | **자체 `CsvLineWriter`, `TsvLineWriter`**                                      | 내부만 영향                           |
| `api(Libs.univocity_parsers)`                | 전체 의존성          | **완전 삭제**                                                                    | 사용자: univocity 직접 참조 시 의존성 직접 추가 |

> **[Codex Critical 1 해결]**: univocity 타입을 public API에서 유지한 채 `compileOnly`로 전환하면 runtime에
`ClassNotFoundException` 발생. 완전한 해결책은 자체 타입 도입뿐.

### 마이그레이션 범위 (사용자 관점)

```kotlin
// Before
import com.univocity.parsers.common.record.Record
import com.univocity.parsers.csv.CsvParserSettings

CsvRecordReader(CsvParserSettings().apply { trimValues(true) })
    .read(input) { record: Record -> record.getString("name") }

// After — import만 교체, 메서드명 동일
import io.bluetape4k.csv.Record
import io.bluetape4k.csv.CsvSettings

CsvRecordReader(CsvSettings(trimValues = true))
    .read(input) { record: Record -> record.getString("name") }
```

### 의존성 변화

```kotlin
// 기존
api(Libs.univocity_parsers)   // engine + types 모두 의존

// 변경 후 (PR 5)
// univocity 완전 삭제 — 의존성 0
```

---

## 1. 아키텍처 개요

### 패키지 구조

```
io.bluetape4k.csv                           ← public API (변경 없음)
├── Record                                  ← NEW: 자체 인터페이스 (univocity Record 대체)
├── CsvSettings, TsvSettings                ← NEW: 설정 데이터 클래스
├── DefaultCsvSettings, DefaultTsvSettings  ← NEW: const 기본값
├── RecordReader / RecordWriter             ← 기존 인터페이스 유지
├── CsvRecordReader / CsvRecordWriter       ← 기존 클래스명 유지, 내부 구현만 교체
├── TsvRecordReader / TsvRecordWriter       ← 기존 클래스명 유지, 내부 구현만 교체
├── RecordReaderSupport / RecordWriterSupport ← 기존 헬퍼 유지
│
├── coroutines/                             ← 기존 패키지 유지
│   ├── SuspendRecordReader / SuspendRecordWriter
│   ├── SuspendCsvRecordReader / SuspendCsvRecordWriter
│   └── SuspendTsvRecordReader / SuspendTsvRecordWriter
│
└── internal/                               ← NEW: 자체 파서 구현 (외부 노출 금지)
    ├── ArrayRecord                         ← Record 구현체 (배열 기반)
    ├── DelimitedLexer                      ← CSV/TSV 공통 추상 렉서
    ├── CsvLexer                            ← RFC 4180 상태 기계
    ├── TsvLexer                            ← TSV 이스케이프 처리
    ├── DelimitedWriter                     ← CSV/TSV 공통 추상 라이터
    ├── CsvLineWriter                       ← RFC 4180 인코딩
    ├── TsvLineWriter                       ← TSV 이스케이프 인코딩
    ├── HeaderIndex                         ← 헤더명→인덱스 룩업 (LinkedHashMap)
    ├── coroutines/
    │   ├── ProducerScopeCsvLexer           ← Channel 기반 네이티브 코루틴 파서
    │   └── FlowDelimitedWriter             ← Flow 기반 네이티브 라이터
    └── ParseException                      ← 위치 정보 포함 예외
```

### 계층 다이어그램

```
┌──────────────────────────────────────────────────────────────┐
│  Public API (io.bluetape4k.csv)                              │
│  ─ RecordReader/Writer, SuspendRecordReader/Writer           │
│  ─ Csv/TsvRecordReader/Writer (구현 클래스)                  │
│  ─ Record 인터페이스, *Settings 데이터 클래스                │
└─────────────────┬────────────────────────────────────────────┘
                  │ delegates
┌─────────────────▼────────────────────────────────────────────┐
│  Internal Engine (io.bluetape4k.csv.internal)                │
│  ─ DelimitedLexer (Reader → Iterator<ArrayRecord>)           │
│  ─ DelimitedWriter (Writer → 행 인코딩)                      │
│  ─ ArrayRecord (Record 구현)                                 │
└─────────────────┬────────────────────────────────────────────┘
                  │ uses
┌─────────────────▼────────────────────────────────────────────┐
│  bluetape4k-io (이미 의존성 존재)                             │
│  ─ File/Path: readAllBytesSuspending, writeSuspending        │
│  ─ Stream: ByteArray.toInputStream, InputStream.toByteArray  │
│  ─ Stream: InputStream.toLineSequence, copyTo                │
│  ─ ByteBufferInputStream / ByteBufferOutputStream           │
└─────────────────┬────────────────────────────────────────────┘
                  │ uses
┌─────────────────▼────────────────────────────────────────────┐
│  JDK I/O (최소 사용)                                         │
│  java.io.Reader / Writer / BufferedReader                    │
└──────────────────────────────────────────────────────────────┘
```

---

## 2. 핵심 내부 구현 설계

### 2.1 자체 `Record` 인터페이스 설계

> **[Codex Critical 1 + High 2 해결]**: univocity
`Record`를 구현하면 runtime에 univocity 클래스가 필요하고, 메서드 표면(RecordMetaData, Class<T> 오버로드, typed getter 20+개)이 매우 크다. 자체 인터페이스를 정의해
**실제 사용되는 메서드만** 포함.

```kotlin
package io.bluetape4k.csv

import java.io.Serializable

/**
 * CSV/TSV 한 행을 나타내는 레코드 인터페이스.
 * 인덱스 기반·헤더명 기반 두 가지 접근 방식을 모두 제공.
 */
interface Record : Serializable {
    /** 행 번호 (1부터 시작) */
    val rowNumber: Long

    /** 컬럼 수 */
    val size: Int

    /** 원시 문자열 값 배열 (null = 빈 비인용 필드) — 방어적 복사 */
    val values: Array<String?>

    /** 헤더 이름 배열 (헤더 없으면 null) */
    val headers: Array<String>?

    // ── 제네릭 getValue — non-null default만 지원 (<T : Any> 컴파일 타임 제약) ──
    // null default가 필요한 경우: getString()/getXxxOrNull() nullable getter 사용
    fun <T : Any> getValue(index: Int, defaultValue: T): T
    fun <T : Any> getValue(name: String, defaultValue: T): T

    // ── 인덱스 기반 typed getter (non-null default) ───
    fun getString(index: Int): String?
    fun getInt(index: Int, default: Int = 0): Int             = getValue(index, default)
    fun getLong(index: Int, default: Long = 0L): Long         = getValue(index, default)
    fun getDouble(index: Int, default: Double = 0.0): Double  = getValue(index, default)
    fun getFloat(index: Int, default: Float = 0f): Float      = getValue(index, default)
    fun getBoolean(index: Int, default: Boolean = false): Boolean = getValue(index, default)

    // ── 인덱스 기반 nullable getter ─────────────────
    // [getValue<T>(null) 타입 소거 회피]: getString()?.toXxxOrNull() 직접 호출
    fun getIntOrNull(index: Int): Int?              = getString(index)?.toIntOrNull()
    fun getLongOrNull(index: Int): Long?            = getString(index)?.toLongOrNull()
    fun getDoubleOrNull(index: Int): Double?        = getString(index)?.toDoubleOrNull()
    fun getFloatOrNull(index: Int): Float?          = getString(index)?.toFloatOrNull()
    fun getBigDecimal(index: Int): java.math.BigDecimal? = getString(index)?.toBigDecimalOrNull()

    // ── 헤더명 기반 typed getter (non-null default) ──
    fun getString(name: String): String?
    fun getInt(name: String, default: Int = 0): Int             = getValue(name, default)
    fun getLong(name: String, default: Long = 0L): Long         = getValue(name, default)
    fun getDouble(name: String, default: Double = 0.0): Double  = getValue(name, default)
    fun getFloat(name: String, default: Float = 0f): Float      = getValue(name, default)
    fun getBoolean(name: String, default: Boolean = false): Boolean = getValue(name, default)

    // ── 헤더명 기반 nullable getter ──────────────────
    fun getIntOrNull(name: String): Int?              = getString(name)?.toIntOrNull()
    fun getLongOrNull(name: String): Long?            = getString(name)?.toLongOrNull()
    fun getDoubleOrNull(name: String): Double?        = getString(name)?.toDoubleOrNull()
    fun getFloatOrNull(name: String): Float?          = getString(name)?.toFloatOrNull()
    fun getBigDecimal(name: String): java.math.BigDecimal? = getString(name)?.toBigDecimalOrNull()
}
```

```kotlin
internal class ArrayRecord(
    private val rawValues: Array<String?>,
    private val _headers: Array<String>?,   // 방어적 복사를 위해 private 보관
    private val headerIndex: HeaderIndex?,
    override val rowNumber: Long,
) : Record {
    companion object : KLogging() {
        private const val serialVersionUID = 1L
    }
    override val size: Int get() = rawValues.size
    override val values: Array<String?> get() = rawValues.copyOf()     // 방어적 복사
    override val headers: Array<String>? get() = _headers?.copyOf()    // 방어적 복사

    override fun getString(index: Int): String? = rawValues.getOrNull(index)
    override fun getString(name: String): String? = headerIndex?.get(name)?.let { rawValues.getOrNull(it) }

    // getValue<T : Any>(index/name, defaultValue: T) — T : Any 제약으로 null 컴파일 타임 차단
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getValue(index: Int, defaultValue: T): T =
        convert(rawValues.getOrNull(index), defaultValue)

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getValue(name: String, defaultValue: T): T =
        convert(headerIndex?.get(name)?.let { rawValues.getOrNull(it) }, defaultValue)
}
```

### 2.2 `convert()` 타입 변환 구현 — companion private 함수

> **[Codex High 해결]**: `getValue<T>(index, null)` 호출 시 `null is String?`이 true → String 분기로 매칭되는 타입 소거 문제.
> **해결책**: nullable 반환이 필요한 경우 `getValue(index, null)` 대신
`getString(index)?.toXxxOrNull()` 패턴 전용 getter 사용 (Section 2.1 참조). `getValue`는 **non-null defaultValue만 지원**한다고 계약 명시.

`convert(raw: String?, defaultValue: T): T` 변환 규칙:

| 필드 상태         | defaultValue 타입 | 반환값                                                      |
|---------------|-----------------|----------------------------------------------------------|
| null 또는 blank | any non-null    | `defaultValue`                                           |
| 파싱 성공         | `String`        | 원본 문자열                                                   |
| 파싱 성공         | `Int`           | `toIntOrNull()` 성공 시 그 값, 실패 시 `defaultValue`            |
| 파싱 성공         | `Long`          | `toLongOrNull()` 성공 시 그 값, 실패 시 `defaultValue`           |
| 파싱 성공         | `Double`        | `toDoubleOrNull()` 성공 시 그 값, 실패 시 `defaultValue`         |
| 파싱 성공         | `Boolean`       | `"true"/"false"` (case-insensitive), 실패 시 `defaultValue` |
| 파싱 성공         | 기타 타입           | `defaultValue` (unsupported, log warning)                |

```kotlin
// ArrayRecord companion object에 private fun으로 정의
// — getValue(index, T : Any)와 getValue(name, T : Any) 두 오버로드에서 공유
// T : Any 제약으로 null defaultValue는 컴파일 타임에 차단됨
@Suppress("UNCHECKED_CAST")
private fun <T : Any> convert(raw: String?, defaultValue: T): T {
    val str = raw?.takeIf { it.isNotBlank() } ?: return defaultValue
    return when (defaultValue) {
        is String  -> str as T
        is Int     -> (str.toIntOrNull()    ?: defaultValue) as T
        is Long    -> (str.toLongOrNull()   ?: defaultValue) as T
        is Double  -> (str.toDoubleOrNull() ?: defaultValue) as T
        is Float   -> (str.toFloatOrNull()  ?: defaultValue) as T
        is Boolean -> (str.lowercase().toBooleanStrictOrNull() ?: defaultValue) as T
        else       -> defaultValue.also {
            // T : Any 제약으로 null 불가 — !! 불필요
            log.warn { "getValue: unsupported type ${defaultValue::class.simpleName}, raw='$str'" }
        }
    }
}
```

> **`T : Any` 제약 효과**: `getValue<Int?>(4, null)` 은 컴파일 에러 — `null`이 `T : Any`에 맞지 않음. nullable getter가 필요한 경우
`getIntOrNull(4)` 사용. `is String?` 타입 소거 문제도 함께 해결: non-nullable `is String`,
`is Int` 매칭이므로 null이 String 분기에 잘못 들어갈 수 없음.

### 2.3 자체 Settings 데이터 클래스 (univocity Settings 완전 교체)

> **[Codex High 1 해결]**: 스펙 내 CsvSettings 이중 정의 모순 해결. V1에 자체 Settings 도입, univocity Settings 완전 제거.

```kotlin
package io.bluetape4k.csv

/**
 * CSV 파서·라이터 공통 설정.
 * univocity CsvParserSettings 를 대체하는 자체 data class.
 */
data class CsvSettings(
    val delimiter: Char = ',',
    val quote: Char = '"',
    val quoteEscape: Char = '"',          // RFC 4180: "" 이스케이프
    val lineSeparator: String = "\r\n",
    val trimValues: Boolean = true,
    val skipEmptyLines: Boolean = true,
    val emptyValueAsNull: Boolean = true,
    val emptyQuotedAsNull: Boolean = false,
    val detectBom: Boolean = true,
    val maxCharsPerColumn: Int = 100_000,
    val maxColumns: Int = 512,
    val bufferSize: Int = 8 * 1024,
) {
    init {
        require(delimiter != quote) { "delimiter and quote must differ" }
    }
}

data class TsvSettings(
    val trimValues: Boolean = true,
    val skipEmptyLines: Boolean = true,
    val emptyValueAsNull: Boolean = true,
    val detectBom: Boolean = true,
    val maxCharsPerColumn: Int = 100_000,
    val maxColumns: Int = 512,
    val bufferSize: Int = 8 * 1024,
)

// 기존 Default* 상수 교체 — Reader/Writer 공용 (trimValues 기본값만 다름)
@JvmField val DefaultCsvSettings: CsvSettings = CsvSettings()                        // Reader 기본 (trimValues=true)
@JvmField val DefaultTsvSettings: TsvSettings = TsvSettings()                        // Reader 기본
@JvmField val DefaultCsvWriterSettings: CsvSettings = CsvSettings(trimValues = false) // Writer 기본 (공백 보존)
@JvmField val DefaultTsvWriterSettings: TsvSettings = TsvSettings(trimValues = false)  // Writer 기본
```

`CsvLexer`는 자체 `CsvSettings`를 직접 받아 사용:

```kotlin
internal class CsvLexer(
    reader: Reader,
    private val settings: CsvSettings,   // 자체 타입, univocity 불필요
): Iterator<Array<String?>>, Closeable {
    companion object: KLogging()

    private val delimiter = settings.delimiter
    private val quote = settings.quote
    ...
}
```

**기존 코드 마이그레이션**:

```kotlin
// Before
CsvRecordReader(CsvParserSettings().apply { trimValues(true) })
// After
CsvRecordReader(CsvSettings(trimValues = true))
```

### 2.4 공백 라운드트립 보장 (Codex [medium] 수정)

> **[Codex 지적]**: `" Alice "` 쓰기 후 `trimValues=true`로 읽으면 `"Alice"` 반환 — 라운드트립 깨짐.

**라이터 수정**: 앞뒤 공백이 있는 필드는 반드시 인용 처리:

```kotlin
internal fun needsQuoting(s: String): Boolean =
    s.any { it == delimiter || it == quote || it == '\r' || it == '\n' }
    || s.startsWith(' ') || s.endsWith(' ')    // 공백 보존 필요 시 인용
```

**문서화**: 라이터가 앞뒤 공백 필드를 인용 처리하므로
`trimValues=true` 리더와의 라운드트립은 보장됨. 단, 인용 없이 쓰인 데이터를 읽을 때는 trim이 적용되어 데이터 손실 가능 — 기존 univocity 동작과 동일.

### 2.5 `CsvLexer` 핵심 알고리즘 (RFC 4180)

**상태 기계 5개 상태**:

```
START_FIELD ──quote──→ IN_QUOTED ──quote──→ QUOTE_IN_QUOTED ──delim──→ START_FIELD
     │                     │                       │
     │                     ├──other──→ IN_QUOTED   ├──quote──→ IN_QUOTED (이스케이프된 ")
     │                     │                       └──other──→ ParseException
     │                     └──EOF──→ ParseException (unterminated quote)
     │
     ├──delim──→ START_FIELD (빈 필드)
     ├──CR/LF──→ END_ROW
     └──other──→ IN_UNQUOTED ──delim──→ START_FIELD
                              ├──CR/LF──→ END_ROW
                              └──quote──→ ParseException (unexpected quote in unquoted field)
```

**구현 의사 코드**:

```kotlin
internal class CsvLexer(
    reader: Reader,
    private val settings: CsvSettings,
): Iterator<Array<String?>>, Closeable {
    companion object: KLogging()   // bluetape4k-patterns: KLogging 필수

    private val buf = if (reader is BufferedReader) reader
    else BufferedReader(reader, settings.bufferSize)
    private val field = StringBuilder(256)
    private val row = ArrayList<String?>(16)
    private var nextRow: Array<String?>? = null
    private var rowNumber = 0L
    private var ended = false
    private var lastFieldWasQuoted = false  // finishField()에서 emptyQuotedAsNull 분기 판단용

    override fun hasNext(): Boolean {
        if (nextRow != null) return true
        if (ended) return false
        nextRow = readRow()
        return nextRow != null
    }

    override fun next(): Array<String?> {
        if (!hasNext()) throw NoSuchElementException()
        val r = nextRow!!; nextRow = null; return r
    }

    private fun readRow(): Array<String?>? {
        row.clear()
        var state = State.START_FIELD
        while (true) {
            val ch = buf.read()
            if (ch == -1) {
                ended = true
                return when (state) {
                    State.IN_QUOTED   -> throw ParseException("Unterminated quoted field at row $rowNumber")
                    State.START_FIELD -> if (row.isEmpty()) null else {
                        finishField(); finishRow()
                    }
                    else              -> {
                        finishField(); finishRow()
                    }
                }
            }
            // ... state transitions ... (see table above)
            // Enforce settings.maxCharsPerColumn while appending to `field`
            // Enforce settings.maxColumns while adding to `row`
            // Handle skipEmptyLines: if row.size == 1 && row[0] == null → skip
        }
    }

    private fun finishField() {
        val raw = if (settings.trimValues) field.toString().trim() else field.toString()
        val value = when {
            raw.isEmpty() && settings.emptyValueAsNull && !lastFieldWasQuoted -> null
            raw.isEmpty() && lastFieldWasQuoted && settings.emptyQuotedAsNull -> null
            else                                                              -> raw
        }
        row.add(value)
        field.setLength(0)
        lastFieldWasQuoted = false
    }

    private fun finishRow(): Array<String?> {
        rowNumber++
        return row.toTypedArray()  // 새 배열 — Record 간 공유 금지 (immutability)
    }
}
```

**RFC 4180 엣지 케이스 처리**:

- `"He said ""Hello"""` → `He said "Hello"` (이중 따옴표 이스케이프)
- `"line1\nline2"` → 인용 안의 개행 그대로 보존
- `\r\n` / `\n` / `\r` 모두 행 종결자로 인식
- 마지막 행에 LF 없어도 정상 처리

### 2.6 `TsvLexer` 차이점

TSV는 따옴표 없음 — 대신 백슬래시 이스케이프:

- `\t` → 탭, `\n` → 개행, `\r` → CR, `\\` → 백슬래시
- 단순 상태 기계: `START_FIELD ──tab──→ START_FIELD`, `START_FIELD ──\\──→ ESCAPE`
- 따옴표 처리 없음 → 코드 복잡도 1/3

### 2.7 `CsvWriter` 내부 구현

```kotlin
internal class DelimitedWriter(
    private val writer: Writer,
    private val delimiter: Char,
    private val quote: Char,
    private val quoteEscape: Char,
    private val lineSeparator: String,
) {
    private val needsQuoteChars = charArrayOf(delimiter, quote, '\r', '\n')

    fun writeRow(values: List<Any?>) {
        for ((i, v) in values.withIndex()) {
            if (i > 0) writer.append(delimiter)
            val s = v?.toString() ?: ""
            if (needsQuoting(s)) writeQuoted(s) else writer.append(s)
        }
        writer.append(lineSeparator)
    }

    // Section 2.4 공백 라운드트립 규칙 반영: 앞뒤 공백도 인용 필요
    private fun needsQuoting(s: String): Boolean =
        s.indexOfAny(needsQuoteChars) >= 0 || s.startsWith(' ') || s.endsWith(' ')

    private fun writeQuoted(s: String) {
        writer.append(quote)
        for (c in s) {
            if (c == quote) writer.append(quoteEscape)  // RFC 4180 escape
            writer.append(c)
        }
        writer.append(quote)
    }
}
```

**플러시 정책**: `Writer`가 `BufferedWriter`인지 확인. 아니면 `BufferedWriter(writer, bufferSize)`로 래핑. `close()`는 flush 후 close, 예외는
`runCatching`으로 무시 (기존 동작 유지).

---

## 3. 코루틴 네이티브 구현 설계

### 3.1 트레이드오프: Channel vs Flow

| 접근                                                | 장점                                                                                        | 단점                                                                   | 결정                 |
|---------------------------------------------------|-------------------------------------------------------------------------------------------|----------------------------------------------------------------------|--------------------|
| **A. `flow { }` + 동기 lexer 직접 호출**                | 가장 단순. cold stream. backpressure 자동.                                                      | I/O가 collect 코루틴에서 동기 실행 → blocking 가능. `flowOn(Dispatchers.IO)` 필요. | **선택** (기본값)       |
| **B. `channelFlow { }.buffer(0)` + producer 코루틴** | 별도 producer 코루틴이 lexer 실행. `.buffer(0)` = RENDEZVOUS 경계 명시 → 엄격한 pull-style backpressure. | 약간 더 무거움. cold이지만 launch 오버헤드 있음.                                    | **선택** (대용량 입력 옵션) |
| **C. `Channel` 직접 + `produce { }`**               | 가장 세밀한 backpressure 제어 (BUFFERED/CONFLATED 선택).                                           | API 복잡, Flow 외부 노출 인터페이스 호환성 깨짐.                                     | 거부                 |

**결정 근거**: 인터페이스가 `Flow`를 반환하므로 A/B 모두 호환. 기본은 단순한 A, 엄격한 backpressure가 필요한 대용량 입력은 B(`.buffer(0)` 명시).

> **[Codex 수정]**: `channelFlow { }` 기본값은 **버퍼 64개** (RENDEZVOUS가 아님). 진정한 pull-style backpressure를 위해
`.buffer(Channel.RENDEZVOUS)` 또는 `.buffer(0)` 명시 필요.

### 3.2 `SuspendCsvRecordReader` 리팩토링

**현재 (univocity 기반)**:

```kotlin
override fun <T> read(input, encoding, skipHeaders, transform): Flow<T> =
    CsvParser(settings).iterateRecords(input, encoding).asFlow().drop(...).map(transform)
```

문제: `iterateRecords()`가 동기 Iterator → `asFlow()`는 collect 시점에 동기 호출 (사실상 blocking I/O).

**신규 설계 (네이티브 Channel/Flow)**:

```kotlin
class SuspendCsvRecordReader(
    private val settings: CsvSettings = DefaultCsvSettings,
): SuspendRecordReader {
    override fun <T> read(
        input: InputStream,
        encoding: Charset,
        skipHeaders: Boolean,
        transform: suspend (Record) -> T,
    ): Flow<T> = channelFlow {
        val reader = input.bufferedReader(encoding)
        val lexer = CsvLexer(reader, settings)
        try {
            var headerIndex: HeaderIndex? = null
            var headers: Array<String>? = null
            var rowNum = 0L
            var first = true
            while (lexer.hasNext()) {
                ensureActive()                         // cancellation 협력
                val raw = lexer.next()
                rowNum++
                if (first) {
                    first = false
                    headers = raw.requireNoNulls()
                    headerIndex = HeaderIndex.of(headers)
                    if (skipHeaders) continue
                }
                val record = ArrayRecord(raw, headers, headerIndex, rowNum)
                send(record as Record)                 // suspend → backpressure
            }
        } finally {
            runCatching { reader.close() }
        }
    }.buffer(Channel.RENDEZVOUS)                       // [Codex 수정] channelFlow 기본은 버퍼64 — RENDEZVOUS 명시 필수
        .flowOn(Dispatchers.IO)                           // I/O는 IO 디스패처에서
        .map { transform(it) }                            // transform은 collect 디스패처에서 (CPU)
}
```

**핵심 포인트**:

- `channelFlow { }.buffer(Channel.RENDEZVOUS)` → consumer가 느리면 producer가 자동으로 멈춤 (pull-style backpressure)
    - **주의**: `channelFlow { }` 기본 버퍼는 64개 — RENDEZVOUS를 원하면 반드시 `.buffer(Channel.RENDEZVOUS)` 또는 `.buffer(0)` 명시
- `flowOn(Dispatchers.IO)` 경계 = I/O와 transform 분리
- `ensureActive()` = 행 단위 cancellation 협력 (수만 행 처리 중 빠른 취소 가능)
- 기존 `withContext(Dispatchers.IO)` 래핑 패턴 제거 — collect 시점에 재개되는 진짜 코루틴 친화적 흐름

### 3.3 `SuspendCsvRecordWriter` 리팩토링

```kotlin
class SuspendCsvRecordWriter private constructor(
    private val writer: Writer,
    private val settings: CsvSettings,
) : SuspendRecordWriter {
    private val mutex = Mutex()                      // 동시 write 직렬화
    private val engine = DelimitedWriter(writer, ...)

    override suspend fun writeHeaders(headers: Iterable<String>) =
        withContext(Dispatchers.IO) { mutex.withLock { engine.writeRow(headers.toList()) } }

    override suspend fun writeRow(row: Iterable<*>) =
        withContext(Dispatchers.IO) { mutex.withLock { engine.writeRow(row.toList()) } }

    override suspend fun writeAll(rows: Flow<Iterable<*>>) =
        rows.flowOn(Dispatchers.IO).collect { writeRow(it) }
}
```

`Mutex` 사용 이유: 단일 Writer 인스턴스에 동시 호출이 들어오면 출력이 인터리빙됨. 코틀린 가이드라인 (Virtual Thread에서 `@Synchronized` 금지) 준수.

### 3.4 Backpressure 전략

| 시나리오             | 채널 타입                                         | 비고                                            |
|------------------|-----------------------------------------------|-----------------------------------------------|
| 기본 (Read)        | **64 (BUFFERED)** — `channelFlow { }` 기본      | `.buffer(Channel.RENDEZVOUS)` 명시해야 pull-style |
| 엄격한 backpressure | `.buffer(Channel.RENDEZVOUS)` 또는 `.buffer(0)` | consumer 속도에 producer가 맞춤                     |
| 명시적 버퍼 옵션        | `.buffer(capacity = 256)` extension           | 호출자가 선택                                       |
| 대용량 dump (Write) | Flow 기반 collect, 별도 채널 없음                     | Writer 한 곳에서 직렬화                              |

> **[Codex Medium 1 해결]**:
`channelFlow { }` 기본 버퍼는 64개 (BUFFERED). RENDEZVOUS(=0)가 아님. 진정한 pull-style backpressure 원하면 반드시
`.buffer(Channel.RENDEZVOUS)` 명시.

---

### 3.5 파일 기반 I/O — `bluetape4k-io` AsynchronousFileChannel 활용

> 파일 읽기/쓰기 확장 함수(`SuspendRecordReaderSupport`, `SuspendRecordWriterSupport`)에서 JVM 블로킹 `FileInputStream`/
`FileOutputStream` 대신 `bluetape4k-io`의 `AsynchronousFileChannel` 기반 API를 사용한다.

#### 3.5.1 배경 — 왜 AsynchronousFileChannel인가

`channelFlow + flowOn(Dispatchers.IO)` 패턴도 VirtualThread가 올라타서 블로킹을 처리하므로 충분하지만, `AsynchronousFileChannel`을 쓰면 **OS 수준
비동기 I/O (AIO)** 로 처리되어 실제 OS 스레드를 점유하지 않는다. `bluetape4k-io`가 이를 이미 `CompletableFuture`로 감싸고 있으므로
`.await()`로 코루틴에서 자연스럽게 사용 가능.

#### 3.5.2 파일 읽기 전략

| 파일 크기              | 접근                            | 사용 API                                                        |
|--------------------|-------------------------------|---------------------------------------------------------------|
| 소~중 파일 (< ~256 MB) | 전체 읽기 후 in-memory 파싱          | `path.readAllBytesSuspending()` → `ByteArrayInputStream`      |
| 대용량 파일 (≥ 256 MB)  | channelFlow + flowOn(IO) 스트리밍 | `FileInputStream → BufferedReader → CsvLexer` (VirtualThread) |

**소~중 파일 구현 예시** (`SuspendRecordReaderSupport.kt` 확장):

```kotlin
/**
 * [Path]의 CSV 파일을 비동기(AsynchronousFileChannel)로 읽어 [Flow]로 반환합니다.
 *
 * @param path 읽을 CSV 파일 경로
 * @param encoding 파일 인코딩 (기본 UTF-8)
 * @param skipHeaders 첫 행을 헤더로 간주하고 레코드에서 제외할지 여부
 */
fun <T> SuspendCsvRecordReader.readFile(
    path: Path,
    encoding: Charset = Charsets.UTF_8,
    skipHeaders: Boolean = true,
    transform: suspend (Record) -> T,
): Flow<T> = flow {
    // bluetape4k-io의 AsynchronousFileChannel 기반 API — OS AIO 사용
    val bytes = path.readAllBytesSuspending()           // suspend, non-blocking
    emitAll(read(bytes.inputStream(), encoding, skipHeaders, transform))
}
```

**대용량 파일** — `channelFlow + flowOn(Dispatchers.IO)` + 표준 `FileInputStream` → `CsvLexer`:

> **[Codex Critical 2 + High 3 해결]**: `readUtf8LinesAsFlow()`는 물리적 라인 단위로 분리하므로 RFC 4180의 인용 필드 내 개행(
`"line1\nline2"`)이 깨짐. CsvLexer는 반드시 문자 스트림(Reader)을 순서대로 받아야 함. okio `SuspendedFileChannelSource` API도 실제 시그니처 불일치(
`AsynchronousFileChannel` 필요, Path 직접 불가). **대용량 파일은 `channelFlow + flowOn(IO)` 방식이 유일하게 RFC-correct한 선택.**

```kotlin
fun <T> SuspendCsvRecordReader.readLargeFile(
    path: Path,
    encoding: Charset = Charsets.UTF_8,
    skipHeaders: Boolean = true,
    transform: suspend (Record) -> T,
): Flow<T> = channelFlow {
    // flowOn(Dispatchers.IO) 내에서 실행 → VirtualThread가 블로킹 I/O 처리
    // FileInputStream → BufferedReader → CsvLexer: 상태 기계가 문자 스트림 순서대로 처리
    path.toFile().inputStream().bufferedReader(encoding).use { reader ->
        val lexer = CsvLexer(reader, settings)
        var headers: Array<String>? = null
        var headerIndex: HeaderIndex? = null
        var rowNum = 0L
        var first = true
        while (lexer.hasNext()) {
            ensureActive()
            val raw = lexer.next()
            rowNum++
            if (first) {
                first = false
                headers = raw.requireNoNulls()
                headerIndex = HeaderIndex.of(headers!!)
                if (skipHeaders) continue
            }
            send(ArrayRecord(raw, headers, headerIndex, rowNum))
        }
    }
}.buffer(Channel.RENDEZVOUS).flowOn(Dispatchers.IO).map { transform(it) }
```

> **`bluetape4k-io` vs `bluetape4k-okio` 역할**:
> - `bluetape4k-io`: 소·중간 파일 (`readAllBytesSuspending()`) — OS AIO, 전체 메모리 로드
> - 대용량 파일: `channelFlow + flowOn(IO) + FileInputStream` — VirtualThread 블로킹, 메모리 O(행)
> - `bluetape4k-okio`: V2 FlowCsvReader에서 코루틴 네이티브 lexer 설계 시 재검토 (V1 범위 외)

#### 3.5.3 파일 쓰기 전략

파일 쓰기는 **버퍼 수집 후 단일 비동기 쓰기** 패턴 사용.

```kotlin
// SuspendCsvRecordWriter의 멤버 메서드 — settings에 직접 접근 가능
class SuspendCsvRecordWriter(
    private val settings: CsvSettings = DefaultCsvWriterSettings,
) : SuspendRecordWriter {
    companion object : KLogging()

    /**
     * [Flow]의 레코드를 CSV로 직렬화하여 [path]에 비동기(AsynchronousFileChannel)로 씁니다.
     * 멤버 메서드이므로 private settings에 직접 접근 가능.
     *
     * @return 쓰여진 바이트 수
     */
    suspend fun writeFile(
        path: Path,
        headers: List<String>,
        rows: Flow<Iterable<*>>,
        append: Boolean = false,
        encoding: Charset = Charsets.UTF_8,
    ): Long {
        val buffer = ByteArrayOutputStream()
        val writer = buffer.writer(encoding)
        val engine = CsvLineWriter(writer, settings)     // settings 직접 접근 (멤버 메서드)
        engine.writeRow(headers)
        rows.collect { engine.writeRow(it.toList()) }
        writer.flush()
        return path.writeSuspending(buffer.toByteArray(), append)
    }
}
```

#### 3.5.4 트레이드오프 비교

| 접근                                                 | 장점                                             | 단점                                   | 결정                       |
|----------------------------------------------------|------------------------------------------------|--------------------------------------|--------------------------|
| **AIO 전체 읽기 (`bluetape4k-io`)**                    | OS AIO, IO 스레드 비점유, 기존 의존성 재사용                 | 파일 크기만큼 메모리 필요                       | **기본 (< 256 MB)**        |
| **`channelFlow + flowOn(IO)` + `FileInputStream`** | RFC 4180 정확 (상태 기계 lexer), 추가 의존성 없음, 메모리 O(행) | VirtualThread 점유 (허용 가능)             | **대용량 파일 기본 (≥ 256 MB)** |
| okio `SuspendedFileChannelSource` + 라인 기반          | —                                              | RFC 4180 위반 (인용 필드 내 개행 파괴), API 불일치 | **거부 (V1)**              |
| `AsynchronousFileChannel` 직접 청크 스트리밍               | OS AIO + 메모리 효율 최대                             | 구현 복잡 (청크 경계 파서 상태 유지)               | 향후 V2 옵션                 |

#### 3.5.5 의존성 추가

```kotlin
// io/csv/build.gradle.kts
api(project(":bluetape4k-io"))     // 기존 의존성 — 소·중간 파일 AIO + Stream 유틸
// bluetape4k-okio: V1 범위에서 제외 (RFC 위반 + API 불일치). V2 FlowCsvReader 설계 시 재검토.
```

---

### 3.6 bluetape4k-io Stream 유틸리티 전반 활용 규칙

> **원칙**: `java.io.*` 또는 JDK 표준 I/O 직접 사용이 필요할 때, `bluetape4k-io`가 동일 기능을 제공하면 반드시 `bluetape4k-io` API를 사용한다.

#### 3.6.1 사용 가능 API 매핑표

| 기존 JDK 패턴                                     | bluetape4k-io 대체                               | 적용 위치                                      |
|-----------------------------------------------|------------------------------------------------|--------------------------------------------|
| `ByteArrayInputStream(bytes).buffered()`      | `bytes.toInputStream()`                        | `RecordReaderSupport`, 내부 lexer 생성         |
| `inputStream.readAllBytes()`                  | `inputStream.toByteArray()`                    | `RecordReaderSupport.readAll(InputStream)` |
| `inputStream.bufferedReader().lineSequence()` | `inputStream.toLineSequence(cs)`               | 헤더/라인 기반 유틸리티                              |
| `inputStream.bufferedReader().readText()`     | `inputStream.toString(cs)`                     | 소용량 in-memory 처리                           |
| `Files.readAllBytes(path)`                    | `path.readAllBytesSuspending()`                | suspend 확장 함수                              |
| `Files.write(path, bytes)`                    | `path.writeSuspending(bytes)`                  | suspend 확장 함수                              |
| `Files.write(path, lines)`                    | `path.writeLinesSuspending(lines)`             | suspend 확장 함수                              |
| `Files.readAllLines(path)`                    | `path.readAllLinesSuspending(cs)`              | suspend 확장 함수                              |
| `outputStream.write(str.toByteArray(cs))`     | `outputStream.write(str, cs)`                  | `RecordWriterSupport` 내부                   |
| `inputStream.copyTo(outputStream)`            | `inputStream.copyTo(outputStream, bufferSize)` | 스트림 복사 유틸리티                                |
| `inputStream.copyTo(writer)`                  | `inputStream.copyTo(out, cs, bufferSize)`      | 인코딩 포함 복사                                  |

#### 3.6.2 동기 구현 (`RecordReaderSupport`, `RecordWriterSupport`)

**현재 패턴 (교체 대상)**:

```kotlin
// 현재: JDK 직접 사용
fun RecordReader.readAll(file: File): Sequence<Record> =
    read(FileInputStream(file))

fun RecordWriter.writeAll(file: File, rows: Sequence<Iterable<*>>) =
    FileOutputStream(file).use { write(it, rows) }
```

**신규 패턴 (bluetape4k-io 사용)**:

```kotlin
// 신규: bluetape4k-io stream 유틸리티
fun RecordReader.readAll(bytes: ByteArray, encoding: Charset = Charsets.UTF_8): Sequence<Record> =
    read(bytes.toInputStream(), encoding)   // ByteArray.toInputStream()

fun RecordReader.readAll(text: String, encoding: Charset = Charsets.UTF_8): Sequence<Record> =
    read(text.toInputStream(encoding), encoding)  // String.toInputStream()
```

#### 3.6.3 코루틴 구현 (`SuspendRecordReaderSupport`, `SuspendRecordWriterSupport`)

**파일 읽기** — `Path.readAllBytesSuspending()` → `ByteArray.toInputStream()` 체이닝:

```kotlin
suspend fun <T> SuspendRecordReader.readFileSuspending(
    path: Path,
    encoding: Charset = Charsets.UTF_8,
    skipHeaders: Boolean = true,
    transform: suspend (Record) -> T,
): Flow<T> {
    // bluetape4k-io: AsynchronousFileChannel + suspend
    val bytes = path.readAllBytesSuspending()
    // bluetape4k-io: ByteArray → buffered InputStream
    return read(bytes.toInputStream(), encoding, skipHeaders, transform)
}
```

**파일 쓰기** — 버퍼 수집 후 `Path.writeSuspending()`:

```kotlin
suspend fun SuspendRecordWriter.writeFileSuspending(
    path: Path,
    rows: Flow<Iterable<*>>,
    encoding: Charset = Charsets.UTF_8,
    append: Boolean = false,
): Long {
    val baos = ByteArrayOutputStream()
    write(baos, rows, encoding)           // internal write to ByteArrayOutputStream
    // bluetape4k-io: AsynchronousFileChannel + suspend
    return path.writeSuspending(baos.toByteArray(), append)
}
```

**라인 단위 읽기** (`toLineSequence` 활용):

```kotlin
// 헤더만 미리 읽을 때 사용 가능
fun peekHeaders(bytes: ByteArray, encoding: Charset = Charsets.UTF_8): List<String> =
    bytes.toInputStream()
         .toLineSequence(encoding)    // bluetape4k-io
         .take(1)
         .toList()
```

#### 3.6.4 ByteBufferInputStream / ByteBufferOutputStream (고성능 경로)

대용량 처리 시 `java.nio.ByteBuffer`를 직접 다루는 경우 `bluetape4k-io`의 `ByteBufferInputStream` / `ByteBufferOutputStream` 활용:

```kotlin
// ByteBuffer 기반 zero-copy 읽기 (향후 확장 옵션)
val buf = ByteBuffer.allocateDirect(CHUNK_SIZE)
val inputStream = ByteBufferInputStream(buf)   // bluetape4k-io
```

> 현재 PR 범위에서는 불필요하나, 향후 AsynchronousFileChannel 청크 스트리밍 구현 시 활용.

---

## 4. 마이그레이션 전략 (소스 레벨 교체)

### 4.1 단계별 PR 분할 (수정된 전략)

| PR       | 내용                                                                                                                                                                                                                     | 공개 API 변경                                 | univocity 상태              |
|----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------|---------------------------|
| **PR 1** | `CsvSettings`/`TsvSettings` data class + `internal/` 엔진 (`CsvLexer`, `TsvLexer`, `CsvLineWriter`, `TsvLineWriter`, `ArrayRecord`, `HeaderIndex`) + kotlinx-benchmark 설정                                                | **추가** (Settings 신규 — `Record` 아직 미포함)    | `api()` 유지                |
| **PR 2** | **`io.bluetape4k.csv.Record` 공개 인터페이스 추가** + 기존 파일의 `com.univocity...Record` import → `io.bluetape4k.csv.Record` 동시 교체 + `CsvRecordReader`/`TsvRecordReader` 내부 엔진 교체 + `getValue<T?>(null)` 콜사이트 → nullable getter 교체 | **추가** (Record) + **변경** (Settings 타입 교체) | `api()` 유지 (diff test 필요) |
| **PR 3** | `CsvRecordWriter`, `TsvRecordWriter` 내부 엔진 교체 (`CsvWriter` → `CsvLineWriter`)                                                                                                                                          | **변경** (Settings 타입 교체)                   | `api()` 유지                |
| **PR 4** | 코루틴 네이티브 교체 + `readLargeFile()` (channelFlow+IO) + bluetape4k-io 파일 I/O 적용                                                                                                                                             | **없음**                                    | `api()` 유지                |
| **PR 5** | univocity 완전 삭제, `DefaultCsvParserSettings` 등 제거, `MIGRATION.md`, 통합 테스트                                                                                                                                               | **삭제** (univocity 상수 제거)                  | **완전 삭제**                 |
| **PR 6** | V2 인터페이스 추가 (Section 5 참조)                                                                                                                                                                                             | 신규 추가                                     | 없음                        |

> **[High 1 해결]**: PR1에서 `io.bluetape4k.csv.Record`를 추가하면 기존 파일이 `com.univocity...Record`를 `Record` 단순명으로 import하고 있어 *
*동일 패키지 내 단순명 충돌** 발생. 해결: PR2에서 `Record.kt` 추가와 모든 import 교체를 **동일 PR에서 원자적으로 처리**.

### 4.2 마이그레이션 범위 (사용자 관점)

**① import/클래스명 교체 (단순 치환)**:

- `com.univocity.parsers.common.record.Record` → `io.bluetape4k.csv.Record`
- `com.univocity.parsers.csv.CsvParserSettings` → `io.bluetape4k.csv.CsvSettings`
- `com.univocity.parsers.tsv.TsvParserSettings` → `io.bluetape4k.csv.TsvSettings`
- `DefaultCsvParserSettings` → `DefaultCsvSettings`, `DefaultTsvParserSettings` → `DefaultTsvSettings` 등
- `CsvRecordReader(CsvParserSettings())` → `CsvRecordReader(CsvSettings())`

**② 콜사이트 수정 필요 (단순 치환 불가)** — `getValue<T?>(index, null)` 제거:

- `record.getValue<String?>(2, null)` → `record.getString(2)`
- `record.getValue<Int?>(4, null)` → `record.getIntOrNull(4)`
- `record.getValue<Long?>(4, null)` → `record.getLongOrNull(4)`
- `record.getValue<Double?>(4, null)` → `record.getDoubleOrNull(4)`

> **[High 2 해결]**: 기존 18개 테스트 파일 중 `getValue<T?>(null)` 패턴이 있는 곳은 위 치환도 동시에 수행해야 함 — import 교체만으로는 불충분. PR 2 DoD에 포함.

**③ 보존되는 것** (메서드명·시그니처 동일):

- `record.getString(index)`, `record.getString(name)`
- `record.getInt(index, 0)`, `record.getLong(name, 0L)` 등 non-null default getter
- `record.getValue(index, nonNullDefault)`, `record.getValue(name, nonNullDefault)` — `T : Any` 제약
- `record.values`, `record.headers`, `record.rowNumber`, `record.size`
- `RecordReader.read(InputStream, Charset, Boolean, (Record)→T): Sequence<T>` 패턴
- 클래스명: `CsvRecordReader`, `TsvRecordReader`, `CsvRecordWriter`, `TsvRecordWriter`, `Suspend*` 전체

**④ 제거되는 것** (univocity 전용):

- `record.getValue<T?>(index, null)` — nullable default 패턴 지원 종료 (`getXxxOrNull()` 대체)
- univocity no-default typed getter (univocity에서 `record.getInt(index): Int?` 형태) — 새 API에서는 `getIntOrNull(index)` 사용

**`MIGRATION.md`**: import 치환 예시, settings 변환표, `getValue<T?>(null)` 치환 표 포함. PR 5에서 작성.

### 4.3 univocity 의존성 변화 타임라인

```
PR 1: api(Libs.univocity_parsers) 유지 — CsvSettings/internal 엔진만 추가, Record 단순명 충돌 없음
PR 2: api() 유지 — Record 공개 + 전체 import 마이그레이션 원자적 처리 + Reader 엔진 교체
PR 3: api() 유지 — Writer 엔진 교체 + @Deprecated Quick Fix
PR 4: api() 유지 — 코루틴 네이티브 교체
PR 5: univocity 완전 삭제. UnivocityVsNativeDiffTest 삭제. MIGRATION.md 작성.
PR 6: V2 인터페이스 (univocity 없음)
```

> **PR 1~4 기간 `api(Libs.univocity_parsers)` 유지 이유**:
`UnivocityVsNativeDiffTest`가 univocity에 의존 (동작 비교). PR 5에서 diff test + 의존성 동시 삭제.

### 4.4 BOM (Byte Order Mark) 처리 (Codex [medium] 수정)

UTF-8 BOM(`\uFEFF`) 미처리 시 첫 헤더 컬럼에 `\uFEFF` 포함 → 헤더명 불일치 버그.

**처리 전략**:

```kotlin
// CsvLexer 초기화 시 BOM 자동 감지·제거
private fun stripBomIfPresent(reader: BufferedReader): BufferedReader {
    reader.mark(1)
    if (reader.read() != BOM_CHAR) reader.reset()   // BOM 없으면 원위치
    return reader
}
private const val BOM_CHAR = '\uFEFF'.code
```

`bluetape4k-io`의 `BOMSupport.kt` 활용 여부 확인 후 재사용.

**테스트**: `RFC4180ComplianceTest`에 BOM 케이스 추가:

> **[Medium 4 해결]**: `skipHeaders=true`(기본값) 시, 리더는 첫 행을 **헤더 메타데이터**로 저장하여 이후 Record에서
`getString("name")` 으로 접근 가능. 이 동작을 명시하지 않으면 테스트 의도가 불분명. 두 가지 시나리오로 분리.

```kotlin
@Test
fun `UTF-8 BOM이 있는 CSV - skipHeaders=true 시 헤더에 BOM 없이 저장된다`() {
    // skipHeaders=true: 첫 행(BOM+헤더)을 메타데이터로 저장, 이후 Record에서 헤더명 조회 가능
    val csv = "\uFEFFname,age\nAlice,30".byteInputStream()
    val records = CsvRecordReader().read(csv, skipHeaders = true).toList()
    records shouldHaveSize 1
    records.first().getString("name") shouldBeEqualTo "Alice"  // \uFEFFname 아님
    records.first().headers?.first() shouldBeEqualTo "name"   // 헤더에도 BOM 없음
}

@Test
fun `UTF-8 BOM이 있는 CSV - skipHeaders=false 시 첫 값에 BOM 없음을 확인`() {
    // skipHeaders=false: 첫 행이 데이터로 취급 — 첫 값이 "\uFEFFname"이 아닌 "name"
    val csv = "\uFEFFname,age\nAlice,30".byteInputStream()
    val records = CsvRecordReader().read(csv, skipHeaders = false).toList()
    records shouldHaveSize 2
    records.first().getString(0) shouldBeEqualTo "name"  // BOM 제거 확인
}
```

---

## 5. V2 인터페이스 설계 (PR 6)

> **목적**: V1(`io.bluetape4k.csv.Record` + `CsvSettings`) 위에 **Flow-native 편의 API
** 추가. V1과 다른 타입 계층이 아니라, V1 엔진을 재사용하는 더 편한 진입점.

### 5.1 설계 원칙

| V1 (PR 1~5)                | V2 (PR 6)                                   |
|----------------------------|---------------------------------------------|
| `io.bluetape4k.csv.Record` | 자체 `CsvRow` (List 기반, 불변)                   |
| `CsvSettings` data class   | `CsvReaderConfig` (var 기반 mutable builder)  |
| `Sequence<Record>`         | `Flow<CsvRow>` (네이티브)                       |
| `InputStream` 기반           | `Path`/`File`/`InputStream` + bluetape4k-io |
| BOM: `detectBom` 옵션        | BOM: 기본 `true`                              |
| `getValue<T>`: 런타임 캐스팅     | `CsvRow.getString()`, `getInt()` 타입 안전      |

### 5.2 `CsvRow` — V2 레코드 타입

```kotlin
package io.bluetape4k.csv.v2

import java.io.Serializable

/**
 * V2 레코드 타입. 타입 안전 접근자와 불변 구조 제공.
 * [Serializable]: 분산 캐시(Lettuce/Redisson) 저장 지원 (CLAUDE.md Record Pattern).
 */
data class CsvRow(
    val values: List<String?>,          // List (불변, Codex [medium] 수정: Array 직접 노출 금지)
    val headers: List<String>?,
    val rowNumber: Long,
) : Serializable {
    companion object : KLogging() {
        private const val serialVersionUID = 1L
    }

    fun getString(index: Int): String? = values.getOrNull(index)
    fun getString(name: String): String? = headers?.indexOf(name)?.takeIf { it >= 0 }?.let { values.getOrNull(it) }

    fun getInt(index: Int, default: Int = 0): Int = getString(index)?.toIntOrNull() ?: default
    fun getLong(index: Int, default: Long = 0L): Long = getString(index)?.toLongOrNull() ?: default
    fun getDouble(index: Int, default: Double = 0.0): Double = getString(index)?.toDoubleOrNull() ?: default
    fun getBoolean(index: Int, default: Boolean = false): Boolean =
        getString(index)?.lowercase()?.toBooleanStrictOrNull() ?: default

    fun getInt(name: String, default: Int = 0): Int = getString(name)?.toIntOrNull() ?: default
    fun getLong(name: String, default: Long = 0L): Long = getString(name)?.toLongOrNull() ?: default
    // ... 동일 패턴
}
```

### 5.3 `CsvReaderConfig` — V2 설정 (mutable builder)

> **[Codex Medium 4 해결]**: `val` + `apply {}` 패턴은 컴파일 불가. `var` 기반 mutable builder로 변경.

```kotlin
package io.bluetape4k.csv.v2

// var 기반 mutable builder — apply { delimiter = '\t' } 패턴 동작
class CsvReaderConfig {
    var delimiter: Char = ','
    var quote: Char = '"'
    var trimValues: Boolean = true
    var skipEmptyLines: Boolean = true
    var emptyValueAsNull: Boolean = true
    var emptyQuotedAsNull: Boolean = false
    var detectBom: Boolean = true
    var maxCharsPerColumn: Int = 100_000
    var maxColumns: Int = 512
    var bufferSize: Int = 8 * 1024

    // ⚠️ init 블록 검증은 초기화 시점만. apply { delimiter = ... } 후 재검증 안 됨.
    // → toCsvSettings()에서 다시 검증 (CsvSettings.init이 재호출됨)
    init { require(delimiter != quote) { "delimiter and quote must differ" } }

    // V1 CsvSettings로 변환 (내부 엔진 재사용) — 변환 시 CsvSettings.init에서 재검증됨
    internal fun toCsvSettings(): CsvSettings = CsvSettings(
        delimiter = delimiter, quote = quote, trimValues = trimValues,
        skipEmptyLines = skipEmptyLines, emptyValueAsNull = emptyValueAsNull,
        emptyQuotedAsNull = emptyQuotedAsNull, detectBom = detectBom,
        maxCharsPerColumn = maxCharsPerColumn, maxColumns = maxColumns, bufferSize = bufferSize,
    )
}

// DSL 빌더
fun csvReaderConfig(block: CsvReaderConfig.() -> Unit = {}): CsvReaderConfig =
    CsvReaderConfig().apply(block)
```

### 5.4 `FlowCsvReader` — V2 주 인터페이스

```kotlin
package io.bluetape4k.csv.v2

/**
 * V2 CSV 리더. Flow 네이티브, bluetape4k-io 파일 I/O, BOM 자동 처리.
 * 내부에서 V1의 CsvLexer 재사용 (엔진 중복 없음).
 */
interface FlowCsvReader {
    val config: CsvReaderConfig

    /** InputStream 기반 — channelFlow + buffer(RENDEZVOUS) */
    fun read(
        input: InputStream,
        encoding: Charset = Charsets.UTF_8,
        skipHeaders: Boolean = true,
    ): Flow<CsvRow>

    /** Path 기반 — bluetape4k-io AsynchronousFileChannel (소·중간 파일) */
    fun readFile(
        path: Path,
        encoding: Charset = Charsets.UTF_8,
        skipHeaders: Boolean = true,
    ): Flow<CsvRow>
}

// DSL 생성자 — var 기반이므로 apply { ... } 정상 컴파일
fun csvReader(block: CsvReaderConfig.() -> Unit = {}): FlowCsvReader =
    FlowCsvReaderImpl(csvReaderConfig(block))

// tsvReader: block 실행 후 delimiter를 '\t'로 강제 고정
// — block에서 delimiter를 바꾸더라도 TSV 파서는 항상 tab 구분자를 사용
fun tsvReader(block: CsvReaderConfig.() -> Unit = {}): FlowCsvReader =
    FlowCsvReaderImpl(csvReaderConfig(block).also { it.delimiter = '\t' })
```

### 5.5 `FlowCsvWriter` — V2 라이터

```kotlin
interface FlowCsvWriter : Closeable {
    val config: CsvWriterConfig   // 별도 write config (quote 정책 등)

    suspend fun writeHeaders(headers: List<String>)
    suspend fun writeRow(row: List<*>)
    suspend fun writeAll(rows: Flow<List<*>>)

    /** Path 기반 쓰기 — bluetape4k-io writeSuspending */
    suspend fun writeFile(
        path: Path,
        headers: List<String>,
        rows: Flow<List<*>>,
        append: Boolean = false,
        encoding: Charset = Charsets.UTF_8,
    ): Long
}
```

### 5.6 V2 패키지 구조

```
io.bluetape4k.csv.v2/                 ← NEW (PR 6)
├── CsvRow.kt                          ← 불변 레코드 타입
├── CsvReaderConfig.kt                 ← DSL 설정
├── CsvWriterConfig.kt                 ← 라이터 설정
├── FlowCsvReader.kt                   ← 리더 인터페이스
├── FlowCsvWriter.kt                   ← 라이터 인터페이스
├── FlowCsvReaderImpl.kt               ← internal 구현 (CsvLexer 재사용)
├── FlowCsvWriterImpl.kt               ← internal 구현 (CsvLineWriter 재사용)
└── CsvExtensions.kt                   ← `csvReader {}`, `tsvReader {}` DSL 함수
```

**핵심**: V2는 내부에서 PR 1에서 만든 `CsvLexer`/`CsvLineWriter` 엔진을 **재사용**. 엔진 구현 중복 없음.

### 5.7 V1 ↔ V2 상호 변환

> **[Medium 3 해결]**: `CsvRow.toRecord()`가 `ArrayRecord`·
`HeaderIndex`(internal 타입)를 직접 호출하면 public extension에서 internal 타입을 노출하는 계약 위반. 해결: `io.bluetape4k.csv` 패키지 레벨의
`internal` 팩토리 함수로 감싸고, v2 패키지에서 이 팩토리만 호출.

```kotlin
// io/csv/src/main/kotlin/io/bluetape4k/csv/RecordFactory.kt
// internal — 모듈 내부에서만 사용 가능 (v2 패키지 포함)
internal fun recordOf(
    values: Array<String?>,
    headers: Array<String>?,
    rowNumber: Long,
): Record = ArrayRecord(
    rawValues = values,
    _headers  = headers,
    headerIndex = headers?.let { HeaderIndex.of(it) },
    rowNumber = rowNumber,
)
```

```kotlin
// V2 CsvRow ↔ V1 Record (모두 자체 타입 — univocity 없음)
// internal factory 경유 — ArrayRecord/HeaderIndex 직접 노출 없음
internal fun CsvRow.toRecord(): io.bluetape4k.csv.Record =
    recordOf(values.toTypedArray(), headers?.toTypedArray(), rowNumber)

// Record → CsvRow: public API 표면에서 Record.values/headers만 사용 (public API만 참조)
fun io.bluetape4k.csv.Record.toCsvRow(): CsvRow =
    CsvRow(values.toList(), headers?.toList(), rowNumber)
```

> `CsvRow.toRecord()`를 `internal`로 표시 — 외부 모듈에서 V1 Record 직접 생성이 필요한 경우 `CsvRecordReader`를 통해 파싱하는 것이 정석이므로 공개 불필요.

---

## 6. 테스트 전략

### 6.1 기존 18개 테스트 파일 재활용 계획

| 테스트 파일                        | 재활용 가능성                | 수정 사항      |
|-------------------------------|------------------------|------------|
| `AbstractRecordReaderTest.kt` | 100%                   | import 변경만 |
| `CsvRecordReaderTest.kt`      | 100%                   | import 변경만 |
| `CsvRecordWriterTest.kt`      | 100%                   | import 변경만 |
| `TsvRecordReaderTest.kt`      | 100%                   | import 변경만 |
| `TsvRecordWriterTest.kt`      | 100%                   | import 변경만 |
| `CsvEdgeCaseTest.kt`          | 100% (RFC 4180 검증의 핵심) | import 변경만 |
| `RecordReaderSupportTest.kt`  | 100%                   | import 변경만 |
| `RecordWriterSupportTest.kt`  | 100%                   | import 변경만 |
| `coroutines/*Test.kt` (10개)   | 100%                   | import 변경만 |

**유지 정책**: 기존 18개 파일은 **회귀 테스트 베이스라인**으로 사용. 자체 구현이 univocity와 동일한 동작을 내야 통과.

**알려진 동작 차이 후보** (테스트 검토 필요):

- `빈 비-인용 필드 → null`: 기존 univocity는 항상 null. 자체 구현은 `emptyValueAsNull` 옵션으로 동일 동작 보장
- `trimValues=true`: 양쪽 공백 제거. 자체 구현은 `String.trim()` 사용 (Unicode whitespace 동일)
- `skipEmptyLines`: 빈 줄(컬럼 전부 null) 스킵 — 기존과 동일

### 6.2 추가 신규 테스트

#### A. 단위 테스트 (internal 패키지)

```
internal/
├── CsvLexerTest.kt             ← 상태 기계 직접 검증
│   ├── 5개 상태 전이 모두 커버
│   ├── unterminated quote → ParseException
│   ├── unexpected quote in unquoted field → ParseException
│   ├── maxCharsPerColumn 초과 → ParseException
│   └── maxColumns 초과 → ParseException
├── TsvLexerTest.kt             ← 백슬래시 이스케이프 처리
├── DelimitedWriterTest.kt      ← needsQuoting 분기 검증
├── ArrayRecordTest.kt          ← Record 인터페이스 계약
└── HeaderIndexTest.kt          ← 헤더 lookup
```

#### B. RFC 4180 준수 테스트 (`RFC4180ComplianceTest.kt`)

RFC 4180의 7개 규칙을 각각 별도 테스트로:

1. CRLF 행 종결자 (마지막 행 CRLF 선택)
2. 헤더 행은 데이터 행과 동일 포맷
3. 마지막 필드 뒤에 쉼표 없음
4. 필드는 큰따옴표로 감쌀 수 있음
5. 쉼표/CR/LF 포함 필드는 반드시 인용
6. 필드 내 큰따옴표는 `""`로 이스케이프
7. 필드는 공백 포함 가능 (인용 여부 무관)

#### C. 코루틴 네이티브 동작 테스트 (`SuspendCsvNativeTest.kt`)

```kotlin
@Test
fun `cancellation propagates within long parse`() = runTest {
    val csv = (1..1_000_000).joinToString("\n") { "row,$it" }
    val job = launch {
        SuspendCsvRecordReader().read(csv.byteInputStream()).collect { delay(1) }
    }
    delay(10); job.cancelAndJoin()
    job.isCancelled shouldBe true
}

@Test
fun `backpressure throttles producer when consumer is slow`() = runTest {
    var produced = 0
    val flow = SuspendCsvRecordReader()
        .read(largeCsv.byteInputStream())
        .onEach { produced++ }
    flow.take(10).collect { delay(100) }
    // buffer(Channel.RENDEZVOUS) → produced 수 ≈ 10 (엄격한 pull-style)
    // buffer(64) 기본이면 produced 최대 74개까지 가능 — RENDEZVOUS 명시 필수
    produced shouldBeLessThan 20
}
```

#### D. 차등 테스트 (마이그레이션 기간만)

`UnivocityVsNativeDiffTest.kt` — **PR 1~4** 진행 중 임시 운영 (univocity 의존성은 PR 5까지 유지됨):

- PR 1(internal 엔진) 작성 시 추가 → PR 4 코루틴 교체 완료 후 PR 5에서 삭제
- 무작위 CSV 1000개 생성 (Hypothesis 스타일) → univocity와 자체 파서 결과 비교
- **주의**: PR 2에서 `api(Libs.univocity_parsers)` 제거 불가 — diff test가 univocity에 의존. PR 5에서만 제거

### 6.3 성능 벤치마크 (`kotlinx-benchmark`)

`bluetape4k-io`의 패턴 (`io/io/src/test/kotlin/io/bluetape4k/io/benchmark/`) 그대로 적용.

#### 5.3.1 `build.gradle.kts` 설정

```kotlin
// io/csv/build.gradle.kts
plugins {
    kotlin("plugin.allopen")          // @State 클래스 open 필요
    id(Plugins.kotlinx_benchmark)
}

allOpen {
    annotation("org.openjdk.jmh.annotations.State")   // kotlinx-benchmark 필수
}

benchmark {
    targets {
        register("test") {
            this as kotlinx.benchmark.gradle.JvmBenchmarkTarget
            jmhVersion = Versions.jmh
        }
    }
}

dependencies {
    // 기존 의존성에 추가
    testImplementation(Libs.kotlinx_benchmark_runtime)
    testImplementation(Libs.kotlinx_benchmark_runtime_jvm)
    testImplementation(Libs.jmh_core)
}
```

#### 5.3.2 벤치마크 파일 구조

```
io/csv/src/test/kotlin/io/bluetape4k/csv/benchmark/
├── CsvParserBenchmark.kt        ← 파서 처리량 (univocity vs 자체 구현)
├── CsvFileBenchmark.kt          ← 파일 I/O 전략 비교
└── CsvWriterBenchmark.kt        ← 라이터 처리량
```

#### 5.3.3 `CsvParserBenchmark.kt`

```kotlin
package io.bluetape4k.csv.benchmark

import kotlinx.benchmark.*
import java.util.concurrent.TimeUnit

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 2)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
class CsvParserBenchmark {

    // 10MB CSV: 100,000행 × 10컬럼
    private val smallCsv: ByteArray   = generateCsv(rows = 10_000)
    private val mediumCsv: ByteArray  = generateCsv(rows = 100_000)

    @Benchmark
    fun nativeCsvRead_small() {
        CsvRecordReader().read(smallCsv.inputStream()).forEach { /* consume */ }
    }

    @Benchmark
    fun nativeCsvRead_medium() {
        CsvRecordReader().read(mediumCsv.inputStream()).forEach { /* consume */ }
    }

    // PR 1~4 기간에만 활성화 — PR 5에서 univocity 제거 시 삭제
    // @Benchmark
    // fun univocityCsvRead_medium() { ... }
}
```

#### 5.3.4 `CsvFileBenchmark.kt` — I/O 전략 비교

```kotlin
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
class CsvFileBenchmark {

    private val tempFile: Path = createTempCsvFile(rows = 100_000)

    @Benchmark
    fun readFile_bluetape4kIo() = runBlocking {
        // 소~중간 파일 경로: bluetape4k-io readAllBytesSuspending → in-memory 파싱
        SuspendCsvRecordReader().readFile(tempFile).collect { }
    }

    @Benchmark
    fun readLargeFile_channelFlow() = runBlocking {
        // 대용량 파일 경로: channelFlow + flowOn(IO) + FileInputStream → CsvLexer
        // okio readUtf8LinesAsFlow() 사용 금지 — RFC 4180 위반
        SuspendCsvRecordReader().readLargeFile(tempFile).collect { }
    }
}
```

#### 5.3.5 성능 목표

| 지표         | 목표                                         |
|------------|--------------------------------------------|
| 자체 구현 처리량  | univocity 대비 ±20% 이내                       |
| 대용량 파일 메모리 | `channelFlow+IO` 경로 = 일정 (행 수 무관, O(행))    |
| 소파일 지연     | bluetape4k-io AIO 경로 ≤ 블로킹 FileInputStream |

> 벤치마크 실행: `./gradlew :bluetape4k-csv:benchmark`
> 결과는 `build/reports/benchmarks/` 에 JSON으로 저장됨.

---

## 7. 트레이드오프 종합

### 자체 구현 vs 대안 라이브러리

| 옵션                      | 장점                                 | 단점                                    |
|-------------------------|------------------------------------|---------------------------------------|
| **자체 구현** (선택)          | 의존성 0, RFC 4180 정확 제어, 코루틴 네이티브 통합 | 구현/테스트 비용, univocity 만큼 빠르진 않음        |
| **Apache Commons CSV**  | 검증된 라이브러리                          | 새 의존 추가 (univocity 교체일 뿐 의존성 0 목표 미달) |
| **FastCSV**             | 매우 빠름, 작은 의존성                      | 새 의존 추가, RFC 4180만 지원 (TSV 별도)        |
| **kotlinx-io 기반 자체 구현** | 코루틴 친화적                            | kotlinx-io는 stable 직전 — 위험            |

**결정**: 의존성 제거가 1순위 목표 → 자체 구현. 성능보다 정확성·유지보수성 우선.

### Lexer: 상태 기계 vs 정규표현식 vs 토큰 스플리터

| 접근                  | 장점                        | 단점                             | 결정 |
|---------------------|---------------------------|--------------------------------|----|
| **상태 기계 (선택)**      | RFC 4180 모든 케이스 정확. 스트리밍. | 코드 길이.                         | 채택 |
| 정규표현식               | 짧은 코드                     | 인용 안의 개행 처리 시 정규식 폭발. 메모리 비효율. | 거부 |
| `String.split()` 기반 | 매우 단순                     | 인용/이스케이프 처리 불가                 | 거부 |

### Record: 배열 기반 vs Map 기반

| 접근                      | 장점                                            | 단점                                                    | 결정 |
|-------------------------|-----------------------------------------------|-------------------------------------------------------|----|
| **배열 기반 (선택)**          | 메모리 효율 (column 수 × String ref). 헤더 인덱스 공유 가능. | 헤더명 lookup이 한 단계 추가                                   | 채택 |
| Map<String, String?> 기반 | name lookup 직접                                | 행마다 HashMap = 메모리 5~10배. 컬럼 순서 추적 위해 LinkedHashMap 필요 | 거부 |

---

## 8. 구현 체크리스트 (PR별 DoD)

### PR 1: internal 엔진 + Settings 구현 (public `Record` 미포함)

> `io.bluetape4k.csv.Record` 공개 추가는 PR 2에서 — 동일 패키지 단순명 충돌 방지

- [ ] `internal/CsvLexer`, `TsvLexer`, `DelimitedWriter`, `ArrayRecord`, `HeaderIndex`, `ParseException` 구현
- [ ] `CsvSettings`, `TsvSettings` data class + `DefaultCsvSettings` 등 상수 추가
- [ ] `RecordFactory.kt` — `internal fun recordOf(...)` 팩토리 추가
- [ ] internal 단위 테스트 통과 (5개 파일: `CsvLexerTest`, `TsvLexerTest`, `DelimitedWriterTest`, `ArrayRecordTest`,
  `HeaderIndexTest`)
- [ ] `kotlinx-benchmark` 플러그인 설정 (`build.gradle.kts`에 `allOpen` + `benchmark` 블록)
- [ ] `CsvParserBenchmark.kt` 초안 작성
- [ ] univocity 의존성 유지, 기존 public 클래스 변경 없음, `Record` 단순명 충돌 없음

### PR 2: `io.bluetape4k.csv.Record` 공개 추가 + Reader 엔진 교체 + 전체 import 마이그레이션

> `Record.kt` 추가와 기존 import 교체를 **동일 PR에서 원자적으로** 처리하여 충돌 방지

- [ ] `io.bluetape4k.csv.Record` 인터페이스 (`Record.kt`) 추가
- [ ] 모듈 내 모든 파일에서 `com.univocity.parsers.common.record.Record` import → `io.bluetape4k.csv.Record` 교체
- [ ] 기존 테스트의 `getValue<T?>(index, null)` 패턴 → nullable getter로 교체:
    - `getValue<String?>(n, null)` → `getString(n)`
    - `getValue<Int?>(n, null)` → `getIntOrNull(n)`
    - `getValue<Long?>(n, null)` → `getLongOrNull(n)` 등
- [ ] `CsvRecordReader(CsvSettings)` 생성자 추가, `CsvRecordReader(CsvParserSettings)` → `@Deprecated(ReplaceWith)` 마킹
- [ ] TSV 동일 처리
- [ ] 내부 `CsvParser` → `CsvLexer` 교체
- [ ] `UnivocityVsNativeDiffTest.kt` 추가 (univocity vs 자체 결과 비교)
- [ ] `RFC4180ComplianceTest.kt` 추가 (BOM 케이스 2개 포함)
- [ ] 기존 회귀 테스트 통과 (import 교체 + 콜사이트 수정 후)

### PR 3: RecordWriter 내부 엔진 교체 + @Deprecated Quick Fix

> Settings 작업은 PR 1~2에서 완료됨 — PR 3은 Writer 엔진 교체에 집중.

- [ ] `CsvRecordWriter`, `TsvRecordWriter` 내부 `CsvWriter` → `CsvLineWriter` 교체
- [ ] Writer 기존 회귀 테스트 통과
- [ ] PR 2에서 마킹한 `@Deprecated` 사용처 전체 Quick Fix 완료 (`ide_code_actions`) — 경고 0건
- [ ] `wiki/testlogs/2026-04.md` 상단에 PR 3 테스트 결과 기록

### PR 4: 코루틴 네이티브화

- [ ] `SuspendCsvRecordReader/Writer` `channelFlow` + `Mutex` 기반
- [ ] cancellation/backpressure 테스트 통과 (`SuspendCsvNativeTest.kt`)
- [ ] `withContext(Dispatchers.IO)` 단순 래핑 패턴 제거 검증
- [ ] `readLargeFile()` — `channelFlow + flowOn(Dispatchers.IO) + FileInputStream → CsvLexer` 구현
    - `readUtf8LinesAsFlow()` (okio) 금지 — RFC 4180 위반 (인용 필드 내 개행 파괴)
- [ ] `CsvFileBenchmark.kt` — `readFile()` (bluetape4k-io AIO) vs `readLargeFile()` (channelFlow+IO) I/O 전략 비교 벤치마크 추가

### PR 5: univocity 완전 삭제

- [ ] **삭제 전** `./gradlew :bluetape4k-csv:benchmark` 실행 — univocity 기준 처리량 baseline 기록 (
  `build/reports/benchmarks/` JSON 보존)
- [ ] `build.gradle.kts`에서 `api(Libs.univocity_parsers)` 완전 삭제 (compileOnly 전환 아님)
- [ ] `Libs.kt`에서 `univocity_parsers` 항목 제거
- [ ] `UnivocityVsNativeDiffTest.kt` 삭제
- [ ] `CvsParserDefaults.kt` (`DefaultCsvParserSettings` 등) 파일 삭제 — PR 2~3에서 `@Deprecated` + Quick Fix 완료 후
- [ ] `CsvParserBenchmark.kt`에서 univocity `@Benchmark` 메서드 삭제
- [ ] `./gradlew :bluetape4k-csv:benchmark` 재실행 — baseline 대비 ±20% 이내 확인
- [ ] `MIGRATION.md` 작성 — import/Settings 교체 가이드 + 예시
- [ ] 전체 모듈 빌드 + 테스트 (`./gradlew :bluetape4k-csv:build`)
- [ ] `wiki/testlogs/2026-04.md` 상단에 최종 결과 기록
- [ ] `README.md` + `README.ko.md` 업데이트:
    - Architecture → UML (Mermaid 클래스/시퀀스 다이어그램) → Features → Examples 순서 준수
    - 제목 바로 아래 언어 전환 링크: `[한국어](./README.ko.md) | English` / `한국어 | [English](./README.md)`
    - 의존성 섹션: univocity 제거 반영, bluetape4k-io 활용 명시
    - `MIGRATION.md` 링크 포함
- [ ] `docs/superpowers/index/2026-04.md` 항목 추가
- [ ] `docs/superpowers/INDEX.md` ✅/⏳ 카운트 갱신

### Worktree 셋업

```bash
git worktree add .worktrees/csv-custom-parser -b feat/csv-custom-parser develop
```

---

## 9. 위험과 대응

| 위험                                          | 대응                                                                                     |
|---------------------------------------------|----------------------------------------------------------------------------------------|
| univocity의 미세한 동작 차이 (BOM, 빈 행, 인용 필드 내 개행) | PR 2~4 `UnivocityVsNativeDiffTest`로 차이 식별 후 `CsvSettings` 옵션화                          |
| 성능 회귀                                       | PR 5 삭제 전 baseline 기록 → 삭제 후 ±20% 재확인. `./gradlew :bluetape4k-csv:benchmark`           |
| 다른 모듈이 univocity를 transitive로 사용            | `./gradlew :bluetape4k-csv:dependencies`로 확인. csv 모듈만 의존하므로 영향 없음                      |
| 자체 `Record` 메서드 누락                          | 기존 18개 테스트 + 내부 사용 케이스 전수 조사. `RecordReader.read { record -> record.X() }` 패턴 전체 grep  |
| 코루틴 cancellation 미협력                        | `ensureActive()` 행 단위 + `take(N).collect` 테스트로 검증                                      |
| 대용량 파일 RFC 4180 준수                          | `channelFlow + flowOn(IO) + FileInputStream → CsvLexer` 패턴 — 라인 기반 파싱 절대 금지            |
| 벤치마크 환경 편차                                  | `@Warmup(2)`, `@Measurement(5, 1s)` 고정. baseline JSON은 `build/reports/benchmarks/`에 보존 |

---

## 부록 A: 파일 경로 목록 (예상)

**신규 작성**:

- `io/csv/src/main/kotlin/io/bluetape4k/csv/Record.kt`
- `io/csv/src/main/kotlin/io/bluetape4k/csv/CsvSettings.kt`
- `io/csv/src/main/kotlin/io/bluetape4k/csv/TsvSettings.kt`
-
`io/csv/src/main/kotlin/io/bluetape4k/csv/internal/{ArrayRecord,HeaderIndex,CsvLexer,TsvLexer,DelimitedWriter,CsvLineWriter,TsvLineWriter,ParseException}.kt`
-
`io/csv/src/test/kotlin/io/bluetape4k/csv/internal/{CsvLexerTest,TsvLexerTest,DelimitedWriterTest,ArrayRecordTest,HeaderIndexTest}.kt`
- `io/csv/src/test/kotlin/io/bluetape4k/csv/RFC4180ComplianceTest.kt`
- `io/csv/src/test/kotlin/io/bluetape4k/csv/coroutines/SuspendCsvNativeTest.kt`
- `io/csv/src/test/kotlin/io/bluetape4k/csv/benchmark/{CsvParserBenchmark,CsvFileBenchmark,CsvWriterBenchmark}.kt`
- `io/csv/MIGRATION.md`

**수정**:

-
`io/csv/src/main/kotlin/io/bluetape4k/csv/{CsvRecordReader,CsvRecordWriter,TsvRecordReader,TsvRecordWriter,RecordReader,RecordWriter,CvsParserDefaults}.kt`
- `io/csv/src/main/kotlin/io/bluetape4k/csv/coroutines/Suspend*.kt` (6개)
- `io/csv/build.gradle.kts`
- `io/csv/README.md`, `io/csv/README.ko.md`

**삭제**:

- 없음 (deprecation으로 처리)
