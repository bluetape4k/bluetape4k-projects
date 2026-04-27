[한국어](./README.ko.md) | English

# bluetape4k-tokenizer-korean

Korean NLP library for morphological analysis, normalization, phrase extraction, stemming, sentence splitting, and blockword masking — with no `twitter-text` dependency (URL/Hashtag/Mention/CashTag patterns are implemented internally via `TwitterCompatPatterns.kt`).

## Architecture

```mermaid
classDiagram
    class KoreanProcessor {
        +normalize(text) CharSequence
        +tokenize(text, profile) List~KoreanToken~
        +tokenizeForNoun(text, profile) List~KoreanToken~
        +tokenizeTopN(text, n, profile) List
        +extractPhrases(tokens, filterSpam) List~KoreanPhrase~
        +extractPhrasesForNoun(tokens) List~KoreanPhrase~
        +stem(tokens) List~KoreanToken~
        +splitSentences(text) Sequence~Sentence~
        +detokenize(tokens) String
        +tokensToStrings(tokens) List~String~
        +addNounsToDictionary(words)
        +addBlockwords(words, severity)
        +removeBlockwords(words, severity)
        +clearBlockwords(severity)
        +maskBlockwords(request) BlockwordResponse
    }

    class KoreanNormalizer {
        +normalize(input) CharSequence
        +correctTypo(chunk) CharSequence
        +normalizeCodaN(chunk) CharSequence
    }

    class KoreanTokenizer {
        +tokenize(text, profile) List~KoreanToken~
        +tokenizeTopN(text, n, profile) List
    }

    class KoreanChunker {
        +chunk(input) List~KoreanToken~
        +getChunks(input, keepSpace) List~String~
        +getChunksByPos(input, pos) List~KoreanToken~
        +POS_PATTERNS Map~KoreanPos, Pattern~
    }

    class TwitterCompatPatterns {
        <<internal>>
        +VALID_URL Pattern
        +VALID_HASHTAG Pattern
        +VALID_MENTION_OR_LIST Pattern
        +VALID_CASHTAG Pattern
    }

    class KoreanToken {
        +text String
        +pos KoreanPos
        +offset Int
        +length Int
        +stem String?
        +unknown Boolean
        +copyWithNewPos(pos) KoreanToken
    }

    class KoreanPos {
        <<enumeration>>
        Noun Verb Adjective Adverb
        Josa Eomi PreEomi Suffix
        Hashtag ScreenName URL Email
        CashTag Number Korean Alpha
        KoreanParticle Punctuation Space Foreign
        Unknown ProperNoun
    }

    class KoreanBlockwordProcessor {
        +findBlockwords(text) List~KoreanToken~
        +maskBlockwords(request) BlockwordResponse
    }

    KoreanProcessor --> KoreanNormalizer
    KoreanProcessor --> KoreanTokenizer
    KoreanProcessor --> KoreanBlockwordProcessor
    KoreanTokenizer --> KoreanChunker
    KoreanChunker --> TwitterCompatPatterns
    KoreanChunker --> KoreanToken
    KoreanToken --> KoreanPos
```

## Features

- **Text normalization** — Collapses colloquial repetitions (`ㅋㅋㅋㅋ → ㅋㅋㅋ`), corrects typos (`가쟝 → 가장`), and repairs consonant-coda errors (`버슨가 → 버스인가`)
- **Morphological analysis** — 1-best and top-N morpheme tokenization with 26-class POS tagging
- **Noun-focused tokenizer** — Lightweight tokenization path for phrase extraction
- **Phrase extraction** — Extracts noun phrases from full or noun-focused token streams; supports hashtag phrases
- **Stemming** — Restores verb/adjective to base form (`가느다란 → stem: 갈다`)
- **Sentence splitting** — Splits a text into a `Sequence<Sentence>`
- **Detokenization** — Reconstructs a natural sentence from a token list
- **Blockword masking** — Severity-layered (`LOW`/`MIDDLE`/`HIGH`) dictionary management and `**`-masking
- **Chunking** — Pre-tokenization chunking by POS pattern: URL, Email, Hashtag, ScreenName, CashTag, Number, Korean, Alpha, Punctuation
- **No twitter-text dependency** — URL/Hashtag/Mention/CashTag regex patterns are self-contained in `TwitterCompatPatterns.kt`
- **Runtime dictionary update** — Add nouns or blockwords at runtime without restart
- **Thread-safe** — All `KoreanProcessor` methods are safe for concurrent use

## Usage

```kotlin
import io.bluetape4k.tokenizer.korean.KoreanProcessor
import io.bluetape4k.tokenizer.korean.tokenizer.TokenizerProfile
import io.bluetape4k.tokenizer.model.BlockwordRequest
import io.bluetape4k.tokenizer.model.Severity

// 1. Normalize colloquial text
val normalized = KoreanProcessor.normalize("안됔ㅋㅋㅋㅋㅋ")
// → "안돼ㅋㅋㅋ"

// 2. Morphological tokenization
val tokens = KoreanProcessor.tokenize("주말특가 쇼핑몰")
KoreanProcessor.tokensToStrings(tokens)
// ["주말", "특가", "쇼핑몰"]

// 3. Stemming
val stemmed = KoreanProcessor.stem(KoreanProcessor.tokenize("가느다란"))
println(stemmed.first().stem)  // → "갈다"

// 4. Phrase extraction
val phrases = KoreanProcessor.extractPhrases(
    KoreanProcessor.tokenize("성탄절 쇼핑"),
    filterSpam = false
)
phrases.forEach { println(it.text) }

// 5. Sentence splitting
val sentences = KoreanProcessor.splitSentences("안녕? 세상아?").toList()
// size == 2

// 6. Detokenization
val text = KoreanProcessor.detokenize(listOf("뭐", "완벽", "하진", "않", "지만"))
// → "뭐 완벽하진 않지만"

// 7. Runtime noun dictionary extension
KoreanProcessor.addNounsToDictionary("블루테이프4K", "주말특가")

// 8. Blockword masking
KoreanProcessor.addBlockwords(listOf("욕설"), Severity.HIGH)
val response = KoreanProcessor.maskBlockwords(BlockwordRequest("이 욕설은 나쁜 말이야"))
// response.text → "이 **은 나쁜 말이야"

// 9. Top-N tokenization
val topN = KoreanProcessor.tokenizeTopN("대학", n = 2)

// 10. Parallel tokenization with coroutines
runBlocking(Dispatchers.Default) {
    listOf("텍스트1", "텍스트2", "텍스트3").map { text ->
        async { KoreanProcessor.tokenize(text) }
    }.awaitAll()
}
```

## Dependencies

| Dependency | Purpose |
|---|---|
| `bluetape4k-tokenizer-core` | `BlockwordRequest`, `Severity`, tokenizer contract interfaces |
| `bluetape4k-coroutines` | Coroutines-based chunker pipeline |
| `bluetape4k-io` | I/O utilities |
| `eclipse-collections` | High-performance collections for dictionary storage |
| `commons-collections4` | Collection utilities |

```kotlin
// build.gradle.kts
implementation("io.bluetape4k:bluetape4k-tokenizer-korean:1.7.0-SNAPSHOT")
```
