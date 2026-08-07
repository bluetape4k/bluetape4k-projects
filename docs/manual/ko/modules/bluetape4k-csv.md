---
manualId: bluetape4k-csv
title: "CSV 데이터 처리"
description: "bluetape4k-csv는 RFC 4180을 준수하는 자체 구현 엔진을 사용하는 Kotlin 네이티브 CSV/TSV 파싱 라이브러리입니다."
kind: library
group: io
learningOrder: 320
---

# CSV 데이터 처리

## 해결하는 문제 {#problem}

bluetape4k-csv는 RFC 4180을 준수하는 자체 구현 엔진을 사용하는 Kotlin 네이티브 CSV/TSV 파싱 라이브러리입니다. 이 매뉴얼은 README의 기능 목록을 반복하지 않고 현재 build, source entry point, test, 설정 resource, lifecycle 근거를 연결합니다.

## 사용 시점 {#when-to-use}

애플리케이션에 encoding boundary, resource ownership, streaming, 호환성, malformed input이 필요할 때 `bluetape4k-csv`를 선택합니다. 아래 source entry point에서 시작해 ownership과 failure 계약이 caller lifecycle에 맞는지 확인합니다. 표준 API나 이미 도입한 더 작은 모듈이 같은 계약을 만족한다면 그쪽을 우선합니다.

## 의존성 좌표 {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-csv")
}
```

Gradle project path는 `:bluetape4k-csv`, source directory는 `io/csv`입니다.

## 핵심 개념 {#concepts}

먼저 확인할 source 개념은 `CsvRecordReader`, `CsvRecordWriter`, `CsvSettings`, `CvsParserDefaults`, `Record`, `RecordFactory`, `RecordReader`, `RecordReaderSupport`입니다. 파일 이름은 탐색 anchor일 뿐이므로 public 계약으로 사용하기 전에 선언과 test를 함께 읽습니다.

## 빠른 시작 {#quick-start}

위 좌표를 추가하고 Gradle을 refresh한 뒤 필요한 작업을 소유한 가장 작은 entry point에서 시작합니다. 먼저 [`CsvRecordReader`](../../../../io/csv/src/main/kotlin/io/bluetape4k/csv/CsvRecordReader.kt)를 확인합니다. 이 파일이 모듈의 구체적인 source entry point입니다.

## 작업별 API {#api-by-task}

| Entry point | 확인할 내용 |
| --- | --- |
| [`CsvRecordReader`](../../../../io/csv/src/main/kotlin/io/bluetape4k/csv/CsvRecordReader.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`CsvRecordWriter`](../../../../io/csv/src/main/kotlin/io/bluetape4k/csv/CsvRecordWriter.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`CsvSettings`](../../../../io/csv/src/main/kotlin/io/bluetape4k/csv/CsvSettings.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`CvsParserDefaults`](../../../../io/csv/src/main/kotlin/io/bluetape4k/csv/CvsParserDefaults.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`Record`](../../../../io/csv/src/main/kotlin/io/bluetape4k/csv/Record.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`RecordFactory`](../../../../io/csv/src/main/kotlin/io/bluetape4k/csv/RecordFactory.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`RecordReader`](../../../../io/csv/src/main/kotlin/io/bluetape4k/csv/RecordReader.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`RecordReaderSupport`](../../../../io/csv/src/main/kotlin/io/bluetape4k/csv/RecordReaderSupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`RecordWriter`](../../../../io/csv/src/main/kotlin/io/bluetape4k/csv/RecordWriter.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`RecordWriterSupport`](../../../../io/csv/src/main/kotlin/io/bluetape4k/csv/RecordWriterSupport.kt) | constructor, function, ownership 계약을 확인합니다. |

## 권장 패턴 {#patterns}

README 근거는 **개요**, **아키텍처**, **클래스 구조**, **CSV/TSV 처리 흐름**, **주요 기능**, **동기 vs 비동기 API 비교**, **설정 옵션**, **null vs 빈 문자열**, **UTF-8 Writer Fast Path**, **사용 예제** 순서로 탐색할 수 있습니다. 이 항목으로 방향을 잡고 source와 test에서 동작을 확인합니다. 도입 범위는 좁게 유지하고 소유한 resource를 caller lifecycle에 연결합니다.

## 연동 {#integrations}

현재 build에 선언된 integration edge는 다음과 같습니다.

```kotlin
api(project(":bluetape4k-io"))
implementation(libs.okio)
compileOnly(project(":bluetape4k-coroutines"))
compileOnly(libs.kotlinx.coroutines.core)
```

`compileOnly` edge는 caller가 제공해야 하는 capability이므로 API를 사용하기 전에 runtime에 실제 dependency가 있는지 확인합니다.

## 설정 {#configuration}

`src/main/resources` 아래에서 모듈 수준 설정 resource를 찾지 못했습니다. constructor, builder, function argument, 연동 framework로 설정하며 default는 source에서 확인합니다.

## 실패 동작 {#failures}

failure 의미는 artifact 이름이 아니라 아래 entry point와 test가 결정합니다. cancellation과 timeout signal을 보존하고 소유한 resource를 닫습니다. backend exception은 안정된 domain 계약을 추가할 수 있는 boundary에서만 변환합니다. retry나 fallback을 넣기 전에 test anchor로 실제 동작을 확인합니다.

## 운영 {#operations}

payload 크기, allocation, latency, malformed input 비율, resource close, protocol 오류를 관찰합니다. capacity, timeout, retry, shutdown 설정은 resource를 소유한 component 가까이에 둡니다. 누가 trade-off를 받아들였는지 알 수 없는 process-wide default는 피합니다.

## 테스트 {#testing}

모듈 test task는 다음과 같습니다.

```bash
./gradlew :bluetape4k-csv:test --no-configuration-cache
```

대표 test anchor는 다음과 같습니다.

- [`AbstractRecordReaderTest`](../../../../io/csv/src/test/kotlin/io/bluetape4k/csv/AbstractRecordReaderTest.kt)
- [`CsvEdgeCaseTest`](../../../../io/csv/src/test/kotlin/io/bluetape4k/csv/CsvEdgeCaseTest.kt)
- [`CsvRecordReaderTest`](../../../../io/csv/src/test/kotlin/io/bluetape4k/csv/CsvRecordReaderTest.kt)
- [`CsvRecordWriterTest`](../../../../io/csv/src/test/kotlin/io/bluetape4k/csv/CsvRecordWriterTest.kt)
- [`NativeCsvRecordReaderTest`](../../../../io/csv/src/test/kotlin/io/bluetape4k/csv/NativeCsvRecordReaderTest.kt)
- [`RFC4180ComplianceTest`](../../../../io/csv/src/test/kotlin/io/bluetape4k/csv/RFC4180ComplianceTest.kt)
- [`RecordReaderSupportTest`](../../../../io/csv/src/test/kotlin/io/bluetape4k/csv/RecordReaderSupportTest.kt)
- [`RecordWriterSupportTest`](../../../../io/csv/src/test/kotlin/io/bluetape4k/csv/RecordWriterSupportTest.kt)

## 워크숍 {#workshops}

manual manifest에 등록된 전용 workshop path가 없습니다. 모듈 README와 위 representative test를 실행 근거로 사용합니다.

## 제한 사항 {#limitations}

이 페이지는 연결된 source와 test가 나타내는 현재 저장소 상태를 설명합니다. optional backend를 애플리케이션 기본값으로 만들거나 benchmark artifact 없이 성능을 단정하지 않습니다. 모듈 버전이 바뀌면 호환성과 lifecycle 설명을 다시 확인해야 합니다.

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `1.12.1` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### CSV/TSV 클래스 구조 다이어그램

[![CSV/TSV 클래스 구조 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-csv-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-csv-diagram-01.svg)

_배포본 README: [`io/csv/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/csv/README.ko.md)_

### CSV/TSV 처리 흐름 다이어그램

[![CSV/TSV 처리 흐름 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-csv-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-csv-diagram-02.svg)

_배포본 README: [`io/csv/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/csv/README.ko.md)_

<!-- release-readme-diagrams:end -->

## 근거 {#sources}

- [모듈 README](../../../../io/csv/README.ko.md)
- [모듈 build](../../../../io/csv/build.gradle.kts)
- [`CsvRecordReader`](../../../../io/csv/src/main/kotlin/io/bluetape4k/csv/CsvRecordReader.kt)
- [`CsvRecordWriter`](../../../../io/csv/src/main/kotlin/io/bluetape4k/csv/CsvRecordWriter.kt)
- [`CsvSettings`](../../../../io/csv/src/main/kotlin/io/bluetape4k/csv/CsvSettings.kt)
- [`CvsParserDefaults`](../../../../io/csv/src/main/kotlin/io/bluetape4k/csv/CvsParserDefaults.kt)
- [`Record`](../../../../io/csv/src/main/kotlin/io/bluetape4k/csv/Record.kt)
- [`RecordFactory`](../../../../io/csv/src/main/kotlin/io/bluetape4k/csv/RecordFactory.kt)
- [`RecordReader`](../../../../io/csv/src/main/kotlin/io/bluetape4k/csv/RecordReader.kt)
- [`RecordReaderSupport`](../../../../io/csv/src/main/kotlin/io/bluetape4k/csv/RecordReaderSupport.kt)
- [`RecordWriter`](../../../../io/csv/src/main/kotlin/io/bluetape4k/csv/RecordWriter.kt)
- [`RecordWriterSupport`](../../../../io/csv/src/main/kotlin/io/bluetape4k/csv/RecordWriterSupport.kt)
- [`AbstractRecordReaderTest`](../../../../io/csv/src/test/kotlin/io/bluetape4k/csv/AbstractRecordReaderTest.kt)
- [`CsvEdgeCaseTest`](../../../../io/csv/src/test/kotlin/io/bluetape4k/csv/CsvEdgeCaseTest.kt)
