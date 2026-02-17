# bluetape4k-tokenizer-core

tokenizer/core 모듈은 형태소 분석 및 금칙어 처리를 위한 기본 인프라를 제공합니다.

## 개요

이 모듈은 한국어/일본어 등 다양한 언어에 대한 형태소 분석을 위한 기반 클래스와 유틸리티를 제공합니다.

### 주요 특징

- **통신 모델**: 형태소 분석 및 금칙어 처리를 위한 요청/응답 모델
- **사전 관리**: CharArray 기반의 고성능 사전 데이터 구조
- **유니코드 지원**: Unicode code point 처리 유틸리티
- **압축 지원**: GZIP 압축된 사전 파일 로드 지원

## 모듈 구조

```
tokenizer/core/
├── model/          # API 요청/응답 모델
├── utils/          # 유틸리티 클래스
└── exceptions/     # 예외 클래스
```

## 주요 컴포넌트

### 1. Model 패키지

#### BlockwordOptions

금칙어 처리를 위한 옵션을 설정합니다.

```kotlin
val options = blockwordOptionsOf(
    mask = "*",                    // 마스킹 문자
    locale = Locale.KOREAN,        // 로케일
    severity = Severity.MIDDLE,    // 심각도 수준
)
```

#### BlockwordRequest/Response

```kotlin
// 요청 생성
val request = blockwordRequestOf(
    text = "금칙어가 포함된 텍스트",
    options = options,
)

// 응답 생성
val response = blockwordResponseOf(
    original = request.text,
    masked = "***가 포함된 텍스트",
    words = listOf("금칙어"),
)
```

#### TokenizeRequest/Response

```kotlin
// 형태소 분석 요청
val request = tokenizeRequestOf(
    text = "형태소 분석할 텍스트",
    options = tokenizeOptionsOf(Locale.KOREAN),
)

// 응답
val response = tokenizeResponseOf(
    original = request.text,
    tokens = listOf("형태소", "분석할", "텍스트"),
)
```

#### Severity

금칙어 심각도 수준을 정의합니다.

```kotlin
enum class Severity(val level: Int) {
    LOW(1),     // 낮은 수준
    MIDDLE(2),  // 중간 수준
    HIGH(3),    // 높은 수준
}
```

### 2. Utils 패키지

#### CharArraySet

CharArray 기반의 고성능 Set 구현체입니다.

```kotlin
val set = CharArraySet(1000)

// 추가
set.add("word")
set.add(charArrayOf('w', 'o', 'r', 'd'))
set.add(text, offset, length)

// 조회
if (set.contains("word")) {
    // ...
}

// 수정 불가능한 Set
val unmodifiable = CharArraySet.unmodifiableSet(set)
```

#### CharArrayMap

CharArray 키를 사용하는 고성능 Map 구현체입니다.

```kotlin
val map = CharArrayMap<String>(1000)

// 추가
map["key"] = "value"
map[charArrayOf('k', 'e', 'y')] = "value"
map.put(text, offset, length, "value")

// 조회
val value = map["key"]
val value = map[charArrayOf('k', 'e', 'y')]
val value = map.get(text, offset, length)

// 수정 불가능한 Map
val unmodifiable = CharArrayMap.unmodifiableMap(map)
```

#### CharacterUtils

유니코드 문자 처리 유틸리티입니다.

```kotlin
val charUtils = CharacterUtils.getInstance()

// Code point 조회
val codePoint = charUtils.codePointAt("Text", 0)
val codePoint = charUtils.codePointAt(charArray, offset, limit)

// Code point 개수
val count = charUtils.codePointCount("한글 텍스트")

// 대소문자 변환
val buffer = "HELLO".toCharArray()
charUtils.toLowerCase(buffer, 0, buffer.size)

// Code points 변환
val src = "Hello".toCharArray()
val dest = IntArray(src.size)
val count = charUtils.toCodePoints(src, 0, src.size, dest, 0)

// Character 버퍼
val buffer = CharacterUtils.newCharacterBuffer(1024)
val reader = StringReader("text")
val filled = charUtils.fill(buffer, reader, 100)
```

#### DictionaryProvider

사전 파일을 로드하는 유틸리티입니다.

```kotlin
// 일반 파일 로드
DictionaryProvider.readFileByLineFromResources("dictionary/noun/nouns.txt")
    .forEach { line ->
        // 처리
    }

// GZIP 압축 파일 로드
DictionaryProvider.readFileByLineFromResources("dictionary/noun/nouns.txt.gz")
    .forEach { line ->
        // 처리
    }

// CharArraySet으로 로드 (suspend)
val set = DictionaryProvider.readWords("dictionary/noun/nouns.txt")

// 빈도수 맵 로드
val freqMap = DictionaryProvider.readWordFreqs("dictionary/freq/freq.txt")
```

### 3. Exceptions 패키지

```kotlin
// 토크나이저 관련 예외
try {
    // tokenizer 작업
} catch (e: TokenizerException) {
    // 예외 처리
}

// 잘못된 요청 예외
try {
    // 요청 처리
} catch (e: InvalidTokenizeRequestException) {
    // 예외 처리
}
```

## 사용 예시

### 금칙어 처리

```kotlin
fun processBlockword(text: String): BlockwordResponse {
    val options = blockwordOptionsOf(
        mask = "*",
        severity = Severity.HIGH,
    )

    val request = blockwordRequestOf(text, options)

    // 금칙어 검출 및 마스킹 처리
    val detectedWords = detectBlockwords(request.text)
    val maskedText = maskWords(request.text, detectedWords, request.options.mask)

    return blockwordResponseOf(
        original = request.text,
        masked = maskedText,
        words = detectedWords,
    )
}
```

### 형태소 분석

```kotlin
fun tokenize(text: String): TokenizeResponse {
    val request = tokenizeRequestOf(text)

    // 형태소 분석 수행
    val tokens = performTokenization(request.text)

    return tokenizeResponseOf(
        original = request.text,
        tokens = tokens,
    )
}
```

### 사전 구축

```kotlin
suspend fun buildDictionary(): CharArraySet {
    val dictionary = CharArraySet(10_000)

    // 명사 사전 로드
    DictionaryProvider.readWords("dictionary/noun/nouns.txt")
        .forEach { dictionary.add(it) }

    // 동사 사전 로드
    DictionaryProvider.readWords("dictionary/verb/verb.txt")
        .forEach { dictionary.add(it) }

    return dictionary
}
```

## 의존성

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-tokenizer-core:$version")
}
```

### 전이 의존성

- bluetape4k-logging
- bluetape4k-core

## 테스트

```bash
# 모든 테스트 실행
./gradlew :bluetape4k-tokenizer-core:test

# 특정 테스트 클래스 실행
./gradlew :bluetape4k-tokenizer-core:test \
    --tests "io.bluetape4k.tokenizer.utils.CharArrayMapTest"
```

## 참고

- 모든 API는 Kotlin 코루틴을 지원합니다 (suspend 함수)
- 사전 파일은 `src/main/resources/dictionary` 경로에 위치해야 합니다
- GZIP 압축 사전 파일은 `.gz` 확장자로 끝나야 자동으로 인식됩니다
