# bluetape4k-tokenizer-korean

한글 형태소 분석 기능과 그를 활용한 Normalization, 금칙어 처리, 특수 기능 (Hashtag, Email 추출 등)을 제공하는 Kotlin 라이브러리입니다.

## 개요

`bluetape4k-tokenizer-korean`은 Twitter에서 개발한 [open-korean-text](https://github.com/open-korean-text/open-korean-text) 라이브러리를 Kotlin으로 재작성하고 확장한 한글 형태소 분석기입니다.

### 주요 특징

- **형태소 분석**: 한글 문장을 품사 태깅과 함께 형태소 단위로 분석
- **텍스트 정규화**: 구어체/신조어/오타를 표준어로 변환
- **구문 추출**: 명사 중심의 의미 있는 구문 추출
- **금칙어 처리**: 비속어 및 부적절한 표현 마스킹
- **비동기 지원**: Kotlin Coroutines 기반의 비동기 처리
- **사용자 사전**: 동적인 단어 추가 및 관리

## 설치

### Gradle

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-tokenizer-korean:${bluetape4kVersion}")
}
```

### Maven

```xml

<dependency>
    <groupId>io.bluetape4k</groupId>
    <artifactId>bluetape4k-tokenizer-korean</artifactId>
    <version>${bluetape4kVersion}</version>
</dependency>
```

## 사용법

### 1. 형태소 분석 (Tokenization)

```kotlin
import io.bluetape4k.tokenizer.korean.KoreanProcessor

// 기본 형태소 분석
val tokens = KoreanProcessor.tokenize("한국어를 처리하는 예시입니다")
// 결과:
// [한국어(Noun: 0, 3), 를(Josa: 3, 1),  (Space: 4, 1), 
//  처리(Noun: 5, 2), 하는(Verb: 7, 2, stem=하다),  (Space: 9, 1),
//  예시(Noun: 10, 2), 입니다(Adjective: 12, 3, stem=이다)]

// 문자열 목록으로 변환
val strings = KoreanProcessor.tokensToStrings(tokens)
// ["한국어", "를", "처리", "하는", "예시", "입니다"]
```

### 2. 텍스트 정규화 (Normalization)

구어처이나 SNS에서 자주 사용되는 비표준 표현을 표준어로 변환합니다.

```kotlin
// 과도한 감정 표현 정규화
KoreanProcessor.normalize("안됔ㅋㅋㅋㅋㅋㅋ")  // "안돼ㅋㅋㅋ"
KoreanProcessor.normalize("그랰ㅋㅋㅋ")       // "그래ㅋㅋㅋ"

// 감정 표현 정규화
KoreanProcessor.normalize("만나무ㅜㅜㅠ")     // "만남ㅜㅜㅠ"
KoreanProcessor.normalize("예뿌ㅠㅠ")        // "예뻐ㅠㅠ"

// 문자 반복 정규화
KoreanProcessor.normalize("사브작사브작사브작사브작")  // "사브작사브작"

// 철자 오류 교정
KoreanProcessor.normalize("가쟝 좋아요")     // "가장 좋아요"
KoreanProcessor.normalize("하겟다")         // "하겠다"
```

### 3. 문장 분리 (Sentence Splitting)

```kotlin
val text = "안녕하세요. 반갑습니다! 어떻게 지내세요?"
val sentences = KoreanProcessor.splitSentences(text)

sentences.forEach { sentence ->
    println("${sentence.text} (${sentence.start}-${sentence.end})")
}
// 출력:
// 안녕하세요. (0-6)
// 반갑습니다! (7-13)
// 어떻게 지내세요? (14-24)
```

### 4. 구문 추출 (Phrase Extraction)

```kotlin
val tokens = KoreanProcessor.tokenize("성탄절 쇼핑을 즐기세요")
val phrases = KoreanProcessor.extractPhrases(tokens)

phrases.forEach { phrase ->
    println("${phrase.text} (${phrase.pos})")
}
// 출력:
// 성탄절 (Noun)
// 쇼핑 (Noun)
// 성탄절 쇼핑 (Noun)
```

### 5. 용언 원형 복원 (Stemming)

동사와 형용사를 원형(어간)으로 복원합니다.

```kotlin
val tokens = KoreanProcessor.tokenize("추천했습니다")
val stemmed = KoreanProcessor.stem(tokens)

stemmed.forEach { token ->
    println("${token.text} -> ${token.stem}")
}
// 출력:
// 추천 -> null
// 했습니다 -> 하다
```

### 6. 금칙어 처리 (Blockword Processing)

```kotlin
import io.bluetape4k.tokenizer.model.BlockwordRequest

// 기본 마스킹 (***)
val request = BlockwordRequest("미니미와 니미")  // '니미'는 비속어
val response = KoreanProcessor.maskBlockwords(request)
println(response.maskedText)  // "미니미와 **"

// 커스텀 마스크 문자
val customRequest = BlockwordRequest(
    "미니미와 니미",
    BlockwordOptions(mask = "###")
)
val customResponse = KoreanProcessor.maskBlockwords(customRequest)
println(customResponse.maskedText)  // "미니미와 ###"
```

### 7. 사용자 사전 등록

```kotlin
// 단일 단어 등록
KoreanProcessor.addNounsToDictionary("블루테이프")

// 여러 단어 등록
KoreanProcessor.addNounsToDictionary("코틀린", "스프링", "마이크로서비스")
KoreanProcessor.addNounsToDictionary(listOf("단어1", "단어2", "단어3"))

// 금칙어 등록
KoreanProcessor.addBlockwords(
    listOf("금칙어1", "금칙어2"),
    severity = Severity.HIGH
)
```

### 8. 분석 프로필 설정

```kotlin
import io.bluetape4k.tokenizer.korean.tokenizer.TokenizerProfile

// 사용자 정의 프로필
val customProfile = TokenizerProfile(
    unknownPosCount = 1.0f,
    allNoun = 10.0f,
    preferredPattern = 4.0f
)

val tokens = KoreanProcessor.tokenize("분석할 텍스트", profile = customProfile)
```

### 9. 다중 후보 분석

```kotlin
// 상위 3개 분석 후보 반환
val candidates = KoreanProcessor.tokenizeTopN("대학", n = 3)

candidates.forEachIndexed { index, candidate ->
    println("후보 ${index + 1}: ${candidate.joinToString(", ")}")
}
// 출력 예시:
// 후보 1: 대학(Noun)
// 후보 2: 대(Modifier), 학(Noun)
// 후보 3: 대(Verb), 학(Noun)
```

## 지원 품사 (Part-of-Speech)

| 품사          | 설명     | 예시                  |
|-------------|--------|---------------------|
| Noun        | 명사     | 사과, 학교, 컴퓨터         |
| Verb        | 동사     | 가다, 먹다, 하다          |
| Adjective   | 형용사    | 크다, 작다, 예쁘다         |
| Adverb      | 부사     | 매우, 빨리, 잘           |
| Determiner  | 관형사    | 이, 그, 저, 새, 헌       |
| Exclamation | 감탄사    | 헐, ㅋㅋㅋ, 어머나         |
| Josa        | 조사     | 의, 에, 에서, 은, 는      |
| Eomi        | 어말어미   | 다, 요, 여             |
| PreEomi     | 선어말어미  | 었, 았, 겠             |
| Suffix      | 접미사    | 적, 들                |
| Modifier    | 관형사    | 초(대박), 완전           |
| VerbPrefix  | 동사 접두어 | 쳐(먹어)               |
| Space       | 공백     |                     |
| Hashtag     | 해시태그   | #Korean             |
| ScreenName  | 멘션     | @username           |
| Email       | 이메일    | test@example.com    |
| URL         | URL    | https://example.com |

## 성능

- 일반적인 문장(20~30자): 약 1~3ms
- 긴 문장(100자 이상): 약 5~15ms
- 동시 처리: Coroutines 기반으로 효율적인 동시 처리 가능

## 스레드 안전성

`KoreanProcessor`의 모든 메서드는 스레드 안전합니다. 병렬 환경에서 안전하게 사용할 수 있습니다.

```kotlin
// 병렬 처리 예시
val texts = listOf("텍스트1", "텍스트2", "텍스트3", ...)

runBlocking(Dispatchers.Default) {
    texts.map { text ->
        async {
            KoreanProcessor.tokenize(text)
        }
    }.awaitAll()
}
```

## 테스트

```bash
# 모든 테스트 실행
./gradlew :bluetape4k-tokenizer-korean:test

# 특정 테스트 클래스 실행
./gradlew :bluetape4k-tokenizer-korean:test --tests "KoreanProcessorCoreTest"
```

## 참고

- 기본 기능은 [open-korean-text](https://github.com/open-korean-text/open-korean-text)를 참고하여 구현
- Kotlin Coroutines 기반으로 Scala 버전 대비 향상된 성능 제공
- Chat 서비스 및 SNS 텍스트 분석에 최적화

## 라이선스

Apache License 2.0
