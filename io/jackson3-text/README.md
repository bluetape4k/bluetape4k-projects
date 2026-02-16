# Module bluetape4k-jackson3-text

## 개요

`bluetape4k-jackson3-text`는 [Jackson 3.x](https://github.com/FasterXML/jackson) 텍스트 데이터 포맷(CSV, Properties, TOML, YAML)을 Kotlin 환경에서 편리하게 사용할 수 있도록 래핑한 모듈입니다.

Jackson 2.x(`bluetape4k-jackson-text`)와 동일한 기능 구조를 제공하면서, Jackson 3.x의 새로운 API와 패키지 구조(`tools.jackson.*`)를 따릅니다.

## Jackson 2.x vs 3.x

| 항목 | Jackson 2.x | Jackson 3.x |
|------|------------|------------|
| 패키지 | `com.fasterxml.jackson.*` | `tools.jackson.*` |
| 모듈 | bluetape4k-jackson-text | bluetape4k-jackson3-text |
| JsonGenerator Feature | `JsonGenerator.Feature` | 제거 (StreamWriteFeature로 통합) |
| Mapper 빌드 | 동일 (`builder()` 패턴) | 동일 (`builder()` 패턴) |

## 지원 텍스트 포맷

| 포맷 | 설명 | 용도 |
|------|------|------|
| **CSV** | Comma-Separated Values | 테이블 형태 데이터, 스프레드시트 호환 |
| **Properties** | Java Properties | 설정 파일, 키-값 쌍 데이터 |
| **TOML** | Tom's Obvious, Minimal Language | 설정 파일, 섹션 기반 구조화 데이터 |
| **YAML** | YAML Ain't Markup Language | 설정 파일, 계층적 데이터 |

## 주요 기능

### 1. JacksonText 싱글턴

```kotlin
import io.bluetape4k.jackson3.text.JacksonText

// CSV
val csvMapper = JacksonText.Csv.defaultMapper
val csvSerializer = JacksonText.Csv.defaultSerializer

// Properties
val propsMapper = JacksonText.Props.defaultMapper
val propsSerializer = JacksonText.Props.defaultSerializer

// TOML
val tomlMapper = JacksonText.Toml.defaultMapper
val tomlSerializer = JacksonText.Toml.defaultSerializer

// YAML
val yamlMapper = JacksonText.Yaml.defaultMapper
val yamlSerializer = JacksonText.Yaml.defaultSerializer
```

### 2. YAML 직렬화/역직렬화

```kotlin
import io.bluetape4k.jackson3.text.JacksonText

val yamlMapper = JacksonText.Yaml.defaultMapper

val yaml = """
    |name: debop
    |age: 30
    |job: developer
    """.trimMargin()

val map = yamlMapper.readValue<Map<String, Any>>(yaml)
```

### 3. Properties 직렬화/역직렬화

```kotlin
import io.bluetape4k.jackson3.text.JacksonText

val propsMapper = JacksonText.Props.defaultMapper

val props = """
    |map=first
    |map.b = second
    |map.xyz = third
    """.trimMargin()

val wrapper = propsMapper.readValue<MapWrapper>(props)
```

### 4. YAML 유틸리티

```kotlin
import io.bluetape4k.jackson3.text.trimYamlDocMarker

val yaml = "---\nname: debop\nage: 30"
val trimmed = yaml.trimYamlDocMarker()  // "name: debop\nage: 30"
```

### 5. Properties 유틸리티

```kotlin
import io.bluetape4k.jackson3.text.getNode

val map = mapOf("db" to mapOf("host" to "localhost", "port" to 5432))
val dbNode = map.getNode("db")  // { host: localhost, port: 5432 }
```

## 의존성

모든 텍스트 포맷 라이브러리는 `compileOnly`로 선언되어 있어, 사용자가 필요한 포맷만 런타임 의존성으로 추가하면 됩니다.

```kotlin
dependencies {
    implementation(project(":bluetape4k-jackson3-text"))

    // 사용하는 포맷만 추가 (compileOnly이므로 런타임 의존성 필요)
    implementation("tools.jackson.dataformat:jackson-dataformat-csv")
    implementation("tools.jackson.dataformat:jackson-dataformat-properties")
    implementation("tools.jackson.dataformat:jackson-dataformat-toml")
    implementation("tools.jackson.dataformat:jackson-dataformat-yaml")
}
```

## 모듈 구조

```
io.bluetape4k.jackson3.text
├── JacksonText.kt                # 텍스트 포맷 Mapper/Serializer 싱글턴
│   ├── Csv                       # CSV 포맷 (CsvMapper, CsvFactory)
│   ├── Props                     # Properties 포맷 (JavaPropsMapper, JavaPropsFactory)
│   ├── Toml                      # TOML 포맷 (TomlMapper, TomlFactory)
│   └── Yaml                      # YAML 포맷 (YAMLMapper, YAMLFactory)
├── CsvJacksonSerializer.kt       # CSV Serializer 구현체
├── PropsJacksonSerializer.kt     # Properties Serializer 구현체
├── TomlJacksonSerializer.kt      # TOML Serializer 구현체
├── YamlJacksonSerializer.kt      # YAML Serializer 구현체
├── YamlSupport.kt                # YAML 문서 마커 제거 유틸리티
└── PropertySupport.kt            # Map 경로 탐색 유틸리티
```

## 테스트

```bash
./gradlew :bluetape4k-jackson3-text:test
```

## 참고

- [Jackson 3.x](https://github.com/FasterXML/jackson)
- [Jackson 3.x Release Notes](https://github.com/FasterXML/jackson/wiki/Jackson-Release-3.0)
- [Jackson Dataformat CSV](https://github.com/FasterXML/jackson-dataformats-text/tree/master/csv)
- [Jackson Dataformat Properties](https://github.com/FasterXML/jackson-dataformats-text/tree/master/properties)
- [Jackson Dataformat TOML](https://github.com/FasterXML/jackson-dataformats-text/tree/master/toml)
- [Jackson Dataformat YAML](https://github.com/FasterXML/jackson-dataformats-text/tree/master/yaml)
