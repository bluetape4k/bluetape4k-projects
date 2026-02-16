# Module bluetape4k-csv

## 개요

`bluetape4k-csv`는 [Univocity Parsers](https://github.com/uniVocity/univocity-parsers) 라이브러리를 Kotlin 환경에서 편리하게 사용할 수 있도록 래핑한 모듈입니다.

CSV와 TSV 포맷의 읽기/쓰기를 위한 `RecordReader`/`RecordWriter` 인터페이스를 제공하며, Kotlin Coroutines 기반의 비동기 버전(`SuspendRecordReader`/`SuspendRecordWriter`)도 지원합니다.

## 주요 기능

### 1. CSV 읽기

```kotlin
import io.bluetape4k.csv.CsvRecordReader

val reader = CsvRecordReader()
val items: Sequence<Item> = reader.read(inputStream, Charsets.UTF_8, skipHeaders = true) { record ->
    Item(record.getString("name"), record.getInt("age"))
}
```

### 2. CSV 쓰기

```kotlin
import io.bluetape4k.csv.CsvRecordWriter

val writer = CsvRecordWriter(outputWriter)
writer.writeHeaders("name", "age")
writer.writeRow(listOf("Alice", 20))
writer.writeRow(listOf("Bob", 30))
writer.close()
```

### 3. TSV 읽기/쓰기

CSV와 동일한 API로 TSV 포맷을 지원합니다.

```kotlin
import io.bluetape4k.csv.TsvRecordReader
import io.bluetape4k.csv.TsvRecordWriter

// 읽기
val reader = TsvRecordReader()
val records = reader.read(inputStream)

// 쓰기
val writer = TsvRecordWriter(outputWriter)
writer.writeHeaders("name", "age")
writer.writeRow(listOf("Alice", 20))
writer.close()
```

### 4. File/InputStream 확장 함수

```kotlin
import io.bluetape4k.csv.readAsCsvRecords
import io.bluetape4k.csv.readAsTsvRecords
import io.bluetape4k.csv.writeCsvRecords
import io.bluetape4k.csv.writeTsvRecords

// File에서 직접 읽기
val csvRecords = File("data.csv").readAsCsvRecords()
val tsvRecords = File("data.tsv").readAsTsvRecords()

// File에서 transform으로 읽기
val items = File("data.csv").readAsCsvRecords(skipHeader = true) { record ->
    Item(record.getString("name"), record.getInt("age"))
}

// InputStream에서 읽기
val records = inputStream.readAsCsvRecords(Charsets.UTF_8, skipHeader = true)

// InputStream에서 transform으로 읽기
val items2 = inputStream.readAsCsvRecords(Charsets.UTF_8, skipHeader = true) { record ->
    Item(record.getString("name"), record.getInt("age"))
}

// File에 직접 쓰기
File("output.csv").writeCsvRecords(
    headers = listOf("name", "age"),
    rows = listOf(listOf("Alice", 20), listOf("Bob", 30))
)

// File에 엔티티를 변환하여 쓰기
File("output.csv").writeCsvRecords(
    headers = listOf("name", "age"),
    entities = people,
) { person -> listOf(person.name, person.age) }
```

### 5. Coroutines 비동기 읽기

Kotlin Flow 기반으로 CSV/TSV 데이터를 비동기로 읽어들입니다.

```kotlin
import io.bluetape4k.csv.coroutines.SuspendCsvRecordReader

val reader = SuspendCsvRecordReader()
val items: Flow<Item> = reader.read(inputStream, Charsets.UTF_8, skipHeaders = true) { record ->
    Item(record.getString("name"), record.getInt("age"))
}

items.collect { item -> println(item) }
```

### 6. Coroutines 비동기 쓰기

Flow를 포함한 다양한 데이터 소스에서 비동기 쓰기를 지원합니다.

```kotlin
import io.bluetape4k.csv.coroutines.SuspendCsvRecordWriter

val writer = SuspendCsvRecordWriter(outputWriter)
writer.writeHeaders("name", "age")
writer.writeRow(listOf("Alice", 20))

// Flow를 통한 대량 쓰기
val dataFlow: Flow<List<Any>> = flowOf(listOf("Bob", 30), listOf("Charlie", 25))
writer.writeAll(dataFlow)
writer.close()
```

## 동기 vs 비동기 API 비교

| 기능 | 동기 (Sequence) | 비동기 (Flow) |
|------|----------------|--------------|
| CSV 읽기 | `CsvRecordReader` | `SuspendCsvRecordReader` |
| CSV 쓰기 | `CsvRecordWriter` | `SuspendCsvRecordWriter` |
| TSV 읽기 | `TsvRecordReader` | `SuspendTsvRecordReader` |
| TSV 쓰기 | `TsvRecordWriter` | `SuspendTsvRecordWriter` |
| 반환 타입 | `Sequence<T>` | `Flow<T>` |
| 쓰기 함수 | 일반 함수 | `suspend` 함수 |

## 기본 파서 설정

모든 파서/Writer는 다음 기본 설정을 사용합니다:

- **컬럼당 최대 문자 수**: 100,000자
- **값 트리밍**: 활성화 (파서만)

커스텀 설정이 필요하면 `CsvParserSettings`/`TsvParserSettings`를 직접 전달할 수 있습니다.

```kotlin
val customSettings = CsvParserSettings().apply {
    trimValues(false)
    maxCharsPerColumn = 500_000
}
val reader = CsvRecordReader(customSettings)
```

## 의존성

```kotlin
dependencies {
    implementation(project(":bluetape4k-csv"))

    // Coroutines 비동기 API 사용 시
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
}
```

## 모듈 구조

```
io.bluetape4k.csv
├── CvsParserDefaults.kt              # CSV/TSV 파서 기본 설정
├── RecordReader.kt                   # 읽기 인터페이스 (Sequence 기반)
├── RecordWriter.kt                   # 쓰기 인터페이스
├── CsvRecordReader.kt                # CSV 읽기 구현체
├── CsvRecordWriter.kt                # CSV 쓰기 구현체
├── TsvRecordReader.kt                # TSV 읽기 구현체
├── TsvRecordWriter.kt                # TSV 쓰기 구현체
├── RecordReaderSupport.kt            # File/InputStream 읽기 확장 함수
├── RecordWriterSupport.kt            # File 쓰기 확장 함수
└── coroutines/                       # Coroutines 비동기 지원
    ├── SuspendRecordReader.kt        # 비동기 읽기 인터페이스 (Flow 기반)
    ├── SuspendRecordWriter.kt        # 비동기 쓰기 인터페이스
    ├── SuspendCsvRecordReader.kt     # 비동기 CSV 읽기 구현체
    ├── SuspendCsvRecordWriter.kt     # 비동기 CSV 쓰기 구현체
    ├── SuspendTsvRecordReader.kt     # 비동기 TSV 읽기 구현체
    └── SuspendTsvRecordWriter.kt     # 비동기 TSV 쓰기 구현체
```

## 테스트

```bash
./gradlew :bluetape4k-csv:test
```

## 참고

- [Univocity Parsers](https://github.com/uniVocity/univocity-parsers)
- [CSV (RFC 4180)](https://datatracker.ietf.org/doc/html/rfc4180)
