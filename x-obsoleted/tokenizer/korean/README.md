# Module bluetape4k-tokenizer-korean

English | [한국어](./README.ko.md)

A Kotlin library for Korean morphological analysis with support for text normalization, blocked-word filtering, and special features such as hashtag and email extraction.

## Overview

`bluetape4k-tokenizer-korean` is a Korean morphological analyzer written and extended in Kotlin, based on the [open-korean-text](https://github.com/open-korean-text/open-korean-text) library originally developed by Twitter.

### Key Features

- **Morphological analysis**: Tokenizes Korean sentences with part-of-speech tagging
- **Text normalization**: Converts informal/colloquial/misspelled expressions to standard forms
- **Phrase extraction**: Extracts meaningful, noun-centered phrases
- **Blocked-word filtering**: Masks profanity and inappropriate expressions
- **Async support**: Non-blocking processing via Kotlin Coroutines
- **User dictionary**: Dynamically add and manage custom words

## Installation

### Gradle

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-tokenizer-korean:${bluetape4kVersion}")
}
```

### Maven

```xml
<dependency>
    <groupId>io.github.bluetape4k</groupId>
    <artifactId>bluetape4k-tokenizer-korean</artifactId>
    <version>${bluetape4kVersion}</version>
</dependency>
```

## Usage

### 1. Morphological Analysis (Tokenization)

```kotlin
import io.bluetape4k.tokenizer.korean.KoreanProcessor

val tokens = KoreanProcessor.tokenize("한국어를 처리하는 예시입니다")
// Result:
// [한국어(Noun: 0, 3), 를(Josa: 3, 1),  (Space: 4, 1), 
//  처리(Noun: 5, 2), 하는(Verb: 7, 2, stem=하다),  (Space: 9, 1),
//  예시(Noun: 10, 2), 입니다(Adjective: 12, 3, stem=이다)]

// Convert to a list of strings
val strings = KoreanProcessor.tokensToStrings(tokens)
// ["한국어", "를", "처리", "하는", "예시", "입니다"]
```

### 2. Text Normalization

Converts non-standard expressions commonly found in colloquial speech and social media to standard forms.

```kotlin
KoreanProcessor.normalize("안됔ㅋㅋㅋㅋㅋㅋ")  // "안돼ㅋㅋㅋ"
KoreanProcessor.normalize("그랰ㅋㅋㅋ")       // "그래ㅋㅋㅋ"
KoreanProcessor.normalize("만나무ㅜㅜㅠ")     // "만남ㅜㅜㅠ"
KoreanProcessor.normalize("예뿌ㅠㅠ")        // "예뻐ㅠㅠ"
KoreanProcessor.normalize("사브작사브작사브작사브작")  // "사브작사브작"
KoreanProcessor.normalize("가쟝 좋아요")     // "가장 좋아요"
KoreanProcessor.normalize("하겟다")         // "하겠다"
```

### 3. Sentence Splitting

```kotlin
val text = "안녕하세요. 반갑습니다! 어떻게 지내세요?"
val sentences = KoreanProcessor.splitSentences(text)

sentences.forEach { sentence ->
    println("${sentence.text} (${sentence.start}-${sentence.end})")
}
// Output:
// 안녕하세요. (0-6)
// 반갑습니다! (7-13)
// 어떻게 지내세요? (14-24)
```

### 4. Phrase Extraction

```kotlin
val tokens = KoreanProcessor.tokenize("성탄절 쇼핑을 즐기세요")
val phrases = KoreanProcessor.extractPhrases(tokens)

phrases.forEach { phrase ->
    println("${phrase.text} (${phrase.pos})")
}
// Output:
// 성탄절 (Noun)
// 쇼핑 (Noun)
// 성탄절 쇼핑 (Noun)
```

### 5. Stemming

Restores verbs and adjectives to their base (dictionary) form.

```kotlin
val tokens = KoreanProcessor.tokenize("추천했습니다")
val stemmed = KoreanProcessor.stem(tokens)

stemmed.forEach { token ->
    println("${token.text} -> ${token.stem}")
}
// Output:
// 추천 -> null
// 했습니다 -> 하다
```

### 6. Blocked-Word Processing

```kotlin
import io.bluetape4k.tokenizer.model.BlockwordRequest

// Default masking (***)
val request = BlockwordRequest("미니미와 니미")  // '니미' is profanity
val response = KoreanProcessor.maskBlockwords(request)
println(response.maskedText)  // "미니미와 **"

// Custom mask character
val customRequest = BlockwordRequest(
    "미니미와 니미",
    BlockwordOptions(mask = "###")
)
val customResponse = KoreanProcessor.maskBlockwords(customRequest)
println(customResponse.maskedText)  // "미니미와 ###"
```

### 7. User Dictionary Registration

```kotlin
// Register a single word
KoreanProcessor.addNounsToDictionary("블루테이프")

// Register multiple words
KoreanProcessor.addNounsToDictionary("코틀린", "스프링", "마이크로서비스")
KoreanProcessor.addNounsToDictionary(listOf("word1", "word2", "word3"))

// Register blocked words
KoreanProcessor.addBlockwords(
    listOf("blockedWord1", "blockedWord2"),
    severity = Severity.HIGH
)
```

### 8. Configuring the Analysis Profile

```kotlin
import io.bluetape4k.tokenizer.korean.tokenizer.TokenizerProfile

val customProfile = TokenizerProfile(
    unknownPosCount = 1.0f,
    allNoun = 10.0f,
    preferredPattern = 4.0f
)

val tokens = KoreanProcessor.tokenize("text to analyze", profile = customProfile)
```

### 9. Multiple Analysis Candidates

```kotlin
// Return the top 3 analysis candidates
val candidates = KoreanProcessor.tokenizeTopN("대학", n = 3)

candidates.forEachIndexed { index, candidate ->
    println("Candidate ${index + 1}: ${candidate.joinToString(", ")}")
}
// Example output:
// Candidate 1: 대학(Noun)
// Candidate 2: 대(Modifier), 학(Noun)
// Candidate 3: 대(Verb), 학(Noun)
```

The candidate count `n` for `tokenizeTopN` must be at least 1; passing 0 or a negative value throws an `IllegalArgumentException`.

## Supported Parts of Speech

| POS         | Description           | Examples                |
|-------------|----------------------|-------------------------|
| Noun        | Noun                 | 사과, 학교, 컴퓨터          |
| Verb        | Verb                 | 가다, 먹다, 하다           |
| Adjective   | Adjective            | 크다, 작다, 예쁘다          |
| Adverb      | Adverb               | 매우, 빨리, 잘             |
| Determiner  | Determiner           | 이, 그, 저, 새, 헌         |
| Exclamation | Exclamation          | 헐, ㅋㅋㅋ, 어머나           |
| Josa        | Particle             | 의, 에, 에서, 은, 는       |
| Eomi        | Sentence-final ending | 다, 요, 여               |
| PreEomi     | Prefinal ending      | 었, 았, 겠               |
| Suffix      | Suffix               | 적, 들                   |
| Modifier    | Modifier             | 초(대박), 완전             |
| VerbPrefix  | Verb prefix          | 쳐(먹어)                 |
| Space       | Whitespace           |                         |
| Hashtag     | Hashtag              | #Korean                 |
| ScreenName  | Mention              | @username               |
| Email       | Email address        | test@example.com        |
| URL         | URL                  | https://example.com     |

## Performance

- Typical sentence (20–30 characters): ~1–3ms
- Long sentence (100+ characters): ~5–15ms
- Concurrent processing: Efficient via Kotlin Coroutines

## Thread Safety

All methods on `KoreanProcessor` are thread-safe and can be used safely in parallel environments.

```kotlin
val texts = listOf("text1", "text2", "text3", ...)

runBlocking(Dispatchers.Default) {
    texts.map { text ->
        async {
            KoreanProcessor.tokenize(text)
        }
    }.awaitAll()
}
```

## Testing

```bash
# Run all tests
./gradlew :bluetape4k-tokenizer-korean:test

# Run a specific test class
./gradlew :bluetape4k-tokenizer-korean:test --tests "KoreanProcessorCoreTest"
```

## Notes

- Core functionality is implemented based on [open-korean-text](https://github.com/open-korean-text/open-korean-text)
- Delivers improved performance over the Scala version through Kotlin Coroutines
- Optimized for chat service and SNS text analysis

## License

Apache License 2.0
