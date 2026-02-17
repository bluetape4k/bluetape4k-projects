# Module bluetape4k-tokenizer-japanese

Kuromoji IPAdic 기반 일본어 형태소 분석 및 금칙어(blockword) 처리 라이브러리입니다.

JVM 환경에서 가볍게 일본어 형태소 분석하기 위해 [Kuromoji](https://github.com/atilika/kuromoji) 라이브러리를 사용합니다.

## 의존성

```kotlin
dependencies {
    implementation(project(":bluetape4k-tokenizer-japanese"))
}
```

내부적으로 [Kuromoji IPAdic](https://github.com/atilika/kuromoji) 0.9.0을 사용합니다.

## 주요 기능

### 형태소 분석

일본어 텍스트를 형태소 단위로 분리하고 품사 정보를 제공합니다.

```kotlin
val tokens = JapaneseProcessor.tokenize("お寿司が食べたい。")
// お: 接頭詞, 寿司: 名詞, が: 助詞, 食べ: 動詞, たい: 助動詞, 。: 記号
```

### 품사별 필터링

명사, 동사, 형용사 등 품사별로 토큰을 필터링할 수 있습니다.

```kotlin
val tokens = JapaneseProcessor.tokenize("私は、日本語の勉強をしています。")
val nouns = JapaneseProcessor.filterNoun(tokens)  // [私, 日本語, 勉強]

// 커스텀 필터링
val verbs = JapaneseProcessor.filter(tokens) { it.isVerb() }
```

### 금칙어 감지

텍스트에서 금칙어를 검출합니다. 단일 단어뿐 아니라 복합명사(명사+동사) 금칙어도 처리합니다.

```kotlin
val blockwords = JapaneseProcessor.findBlockwords("ホモの男性を理解できない")
// [ホモ]
```

### 금칙어 마스킹

금칙어를 마스크 문자로 치환합니다.

```kotlin
val request = blockwordRequestOf("ホモの男性を理解できない")
val response = JapaneseProcessor.maskBlockwords(request)
println(response.maskedText) // **の男性を理解できない
```

### 사전 관리

금칙어 사전을 동적으로 관리할 수 있습니다.

```kotlin
// 금칙어 추가
JapaneseProcessor.addBlockwords(listOf("新禁止語"))

// 금칙어 제거
JapaneseProcessor.removeBlockwords(listOf("新禁止語"))

// 전체 금칙어 삭제
JapaneseProcessor.clearBlockwords()
```

## 패키지 구조

| 패키지                                          | 설명                                        |
|----------------------------------------------|-------------------------------------------|
| `io.bluetape4k.tokenizer.japanese`           | `JapaneseProcessor` - 통합 파사드              |
| `io.bluetape4k.tokenizer.japanese.tokenizer` | `JapaneseTokenizer` - 형태소 분석, 품사 판별 확장 함수 |
| `io.bluetape4k.tokenizer.japanese.block`     | `JapaneseBlockwordProcessor` - 금칙어 검출/마스킹 |
| `io.bluetape4k.tokenizer.japanese.utils`     | `JapaneseDictionaryProvider` - 사전 로딩/관리   |

## 테스트

```bash
./gradlew :bluetape4k-tokenizer-japanese:test
```

## Resources

* [kuromoji](https://www.atilika.org/) - Kuromoji is an open source Japanese morphological analyzer written in Java.
