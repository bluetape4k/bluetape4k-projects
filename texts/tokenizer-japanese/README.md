# Module bluetape4k-tokenizer-japanese

English | [한국어](./README.ko.md)

A Japanese morphological analysis and blocked-word filtering library based on Kuromoji IPAdic.

Uses the [Kuromoji](https://github.com/atilika/kuromoji) library for lightweight Japanese morphological analysis on the JVM.

## Dependency

```kotlin
dependencies {
    implementation(project(":bluetape4k-tokenizer-japanese"))
}
```

Internally uses [Kuromoji IPAdic](https://github.com/atilika/kuromoji) 0.9.0.

## Key Features

### Morphological Analysis

Splits Japanese text into morphemes and provides part-of-speech information.

```kotlin
val tokens = JapaneseProcessor.tokenize("お寿司が食べたい。")
// お: 接頭詞, 寿司: 名詞, が: 助詞, 食べ: 動詞, たい: 助動詞, 。: 記号
```

### Filtering by Part of Speech

Filter tokens by part of speech such as nouns, verbs, and adjectives.

```kotlin
val tokens = JapaneseProcessor.tokenize("私は、日本語の勉強をしています。")
val nouns = JapaneseProcessor.filterNoun(tokens)  // [私, 日本語, 勉強]

// Custom filtering
val verbs = JapaneseProcessor.filter(tokens) { it.isVerb() }
```

### Blocked-Word Detection

Detects blocked words in text, including compound nouns (noun + verb combinations).

```kotlin
val blockwords = JapaneseProcessor.findBlockwords("ホモの男性を理解できない")
// [ホモ]
```

### Blocked-Word Masking

Replaces blocked words with a mask character.

```kotlin
val request = blockwordRequestOf("ホモの男性を理解できない")
val response = JapaneseProcessor.maskBlockwords(request)
println(response.maskedText) // **の男性を理解できない
```

### Dictionary Management

Dynamically manage the blocked-word dictionary.

```kotlin
// Add blocked words
JapaneseProcessor.addBlockwords(listOf("新禁止語"))

// Remove blocked words
JapaneseProcessor.removeBlockwords(listOf("新禁止語"))

// Clear all blocked words
JapaneseProcessor.clearBlockwords()
```

## Package Structure

| Package                                        | Description                                                     |
|----------------------------------------------|------------------------------------------------------------------|
| `io.bluetape4k.tokenizer.japanese`           | `JapaneseProcessor` — unified facade                             |
| `io.bluetape4k.tokenizer.japanese.tokenizer` | `JapaneseTokenizer` — morphological analysis, POS extension fns  |
| `io.bluetape4k.tokenizer.japanese.block`     | `JapaneseBlockwordProcessor` — blocked-word detection/masking    |
| `io.bluetape4k.tokenizer.japanese.utils`     | `JapaneseDictionaryProvider` — dictionary loading/management     |

## Testing

```bash
./gradlew :bluetape4k-tokenizer-japanese:test
```

## Resources

* [kuromoji](https://www.atilika.org/) - Kuromoji is an open source Japanese morphological analyzer written in Java.
