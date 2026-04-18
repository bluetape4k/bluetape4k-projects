# bluetape4k-csv Migration Guide

## univocity-parsers → 자체 엔진 마이그레이션

### 개요

`bluetape4k-csv` v1.5.0부터 내부 파서/라이터 엔진이 univocity-parsers에서 자체 구현으로 교체되었습니다.
공개 API는 대부분 유지되지만, 일부 univocity 타입을 직접 사용하던 경우 변경이 필요합니다.

---

### 제거된 API

#### `CvsParserDefaults.kt`의 univocity 기반 상수

| 제거된 상수 | 대체 |
|---|---|
| `DefaultCsvParserSettings: CsvParserSettings` | `CsvSettings.DEFAULT` |
| `DefaultTsvParserSettings: TsvParserSettings` | `TsvSettings.DEFAULT` |
| `DefaultCsvWriterSettings: CsvWriterSettings` | `CsvSettings.DEFAULT` |
| `DefaultTsvWriterSettings: TsvWriterSettings` | `TsvSettings.DEFAULT` |

#### Reader/Writer 생성자 변경

**이전 (univocity 기반):**
```kotlin
// Reader
CsvRecordReader(settings: CsvParserSettings)
TsvRecordReader(settings: TsvParserSettings)

// Writer
CsvRecordWriter(writer: Writer, settings: CsvWriterSettings)
TsvRecordWriter(writer: Writer, settings: TsvWriterSettings)
SuspendCsvRecordWriter(writer: Writer, settings: CsvWriterSettings)
SuspendTsvRecordWriter(writer: Writer, settings: TsvWriterSettings)
```

**이후 (자체 엔진):**
```kotlin
// Reader
CsvRecordReader(settings: CsvSettings = CsvSettings.DEFAULT)
TsvRecordReader(settings: TsvSettings = TsvSettings.DEFAULT)

// Writer
CsvRecordWriter(writer: Writer, settings: CsvSettings = CsvSettings.DEFAULT)
TsvRecordWriter(writer: Writer, settings: TsvSettings = TsvSettings.DEFAULT)
SuspendCsvRecordWriter(writer: Writer, settings: CsvSettings = CsvSettings.DEFAULT)
SuspendTsvRecordWriter(writer: Writer, settings: TsvSettings = TsvSettings.DEFAULT)
```

#### `RecordWriterSupport` / `SuspendRecordWriterSupport` 파라미터 변경

```kotlin
// 이전
fun File.writeCsvRecords(settings: CsvWriterSettings = DefaultCsvWriterSettings, ...)
fun File.writeTsvRecords(settings: TsvWriterSettings = DefaultTsvWriterSettings, ...)

// 이후
fun File.writeCsvRecords(settings: CsvSettings = CsvSettings.DEFAULT, ...)
fun File.writeTsvRecords(settings: TsvSettings = TsvSettings.DEFAULT, ...)
```

---

### 유지된 API (`Record` 인터페이스)

`com.univocity.parsers.common.record.Record` 대신 `io.bluetape4k.csv.Record`를 사용하세요.

```kotlin
// 이전
import com.univocity.parsers.common.record.Record

// 이후
import io.bluetape4k.csv.Record
```

메서드는 완전 호환됩니다:

| `Record` 메서드 | 설명 |
|---|---|
| `getString(index)` / `getString(name)` | 문자열 또는 null |
| `getValue(index, defaultValue)` / `getValue(name, defaultValue)` | 타입 변환 (T : Any) |
| `getIntOrNull(index/name)` | Int 또는 null |
| `getLongOrNull(index/name)` | Long 또는 null |
| `getDoubleOrNull(index/name)` | Double 또는 null |
| `getFloatOrNull(index/name)` | Float 또는 null |
| `getBigDecimalOrNull(index/name)` | BigDecimal 또는 null |
| `rowNumber` | 행 번호 (1-based) |
| `size` | 컬럼 수 |
| `values` | 원시 문자열 배열 |
| `headers` | 헤더 배열 (skipHeaders=true 시 설정됨) |

---

### Writer 동작 변경 사항

#### trailing space 처리

이전 univocity 구현은 쓰기 시 trailing space를 자동으로 trim했습니다.
새 구현은 RFC 4180에 따라 trailing/leading space가 있는 필드를 인용(quote) 처리합니다.

```kotlin
// 입력: "row2  " (trailing space 2개)

// 이전 출력: row2 (trim됨)
// 이후 출력: "row2  " (RFC 4180 인용 보존)
```

#### null vs 빈 문자열

- `null` → 인용 없는 빈 필드 (읽을 때 `null` 복원)
- `""` → `""` 인용 출력 (읽을 때 빈 문자열 복원)

---

### 빌드 의존성 변경

`build.gradle.kts`에서 univocity 의존성을 제거하세요:

```kotlin
// 제거
dependencies {
    implementation("com.univocity:univocity-parsers:2.9.1")
}
```

---

### Coroutines 개선 사항

- `SuspendCsvRecordReader` / `SuspendTsvRecordReader`: `channelFlow + ensureActive()` 기반으로 취소 협력(cooperative cancellation) 지원
- `SuspendCsvRecordWriter` / `SuspendTsvRecordWriter`: `Mutex` 기반 동시 쓰기 보호
