# Module bluetape4k-tokenizer-core

English | [한국어](./README.ko.md)

The `tokenizer/core` module provides the foundational infrastructure for morphological analysis and blocked-word (profanity) filtering.

## Overview

This module provides base classes and utilities for morphological analysis across various languages such as Korean and Japanese.

### Key Features

- **Communication models**: Request/response models for tokenization and blocked-word processing
- **Dictionary management**: High-performance `CharArray`-based dictionary data structures
- **Unicode support**: Utilities for working with Unicode code points
- **Compression support**: Load GZIP-compressed dictionary files

## Module Structure

```
tokenizer/core/
├── model/          # API request/response models
├── utils/          # Utility classes
└── exceptions/     # Exception classes
```

## Key Components

### 1. Model Package

#### BlockwordOptions

Configures options for blocked-word processing.

```kotlin
val options = blockwordOptionsOf(
    mask = "*",                    // Masking character
    locale = Locale.KOREAN,        // Locale
    severity = Severity.MIDDLE,    // Severity level
)
```

#### BlockwordRequest/Response

```kotlin
// Create a request
val request = blockwordRequestOf(
    text = "Text containing a blocked word",
    options = options,
)

// Create a response
val response = blockwordResponseOf(
    original = request.text,
    masked = "*** containing a blocked word",
    words = listOf("blocked word"),
)
```

#### TokenizeRequest/Response

```kotlin
// Tokenization request
val request = tokenizeRequestOf(
    text = "Text to tokenize",
    options = tokenizeOptionsOf(Locale.KOREAN),
)

// Response
val response = tokenizeResponseOf(
    original = request.text,
    tokens = listOf("Text", "to", "tokenize"),
)
```

#### Severity

Defines severity levels for blocked words.

```kotlin
enum class Severity(val level: Int) {
    LOW(1),     // Low severity
    MIDDLE(2),  // Medium severity
    HIGH(3),    // High severity
}
```

### 2. Utils Package

#### CharArraySet

A high-performance `Set` implementation backed by `CharArray`.

```kotlin
val set = CharArraySet(1000)

// Add
set.add("word")
set.add(charArrayOf('w', 'o', 'r', 'd'))

// Lookup
if (set.contains("word")) {
    // ...
}

// Remove (returns true only if actually removed)
set.remove("word")
set.removeAll(listOf("a", "b"))

// Unmodifiable set
val unmodifiable = CharArraySet.unmodifiableSet(set)
```

#### CharArrayMap

A high-performance `Map` implementation with `CharArray` keys.

```kotlin
val map = CharArrayMap<String>(1000)

// Put
map["key"] = "value"
map[charArrayOf('k', 'e', 'y')] = "value"

// Get
val value = map["key"]
val value = map[charArrayOf('k', 'e', 'y')]
val value = map.get("key")
val value2 = map.get(charArrayOf('k', 'e', 'y'), 0, 3)

// Unmodifiable map
val unmodifiable = CharArrayMap.unmodifiableMap(map)
```

#### CharacterUtils

Utility for working with Unicode characters.

```kotlin
val charUtils = CharacterUtils.getInstance()

// Get code point
val codePoint = charUtils.codePointAt("Text", 0)
val codePoint = charUtils.codePointAt(charArray, offset, limit)

// Count code points
val count = charUtils.codePointCount("Hello text")

// Case conversion
val buffer = "HELLO".toCharArray()
charUtils.toLowerCase(buffer, 0, buffer.size)

// Convert to code points
val src = "Hello".toCharArray()
val dest = IntArray(src.size)
val count = charUtils.toCodePoints(src, 0, src.size, dest, 0)

// Character buffer
val buffer = CharacterUtils.newCharacterBuffer(1024)
val reader = StringReader("text")
val filled = charUtils.fill(buffer, reader, 100)
```

#### DictionaryProvider

Utility for loading dictionary files.

```kotlin
// Load a plain text file
DictionaryProvider.readFileByLineFromResources("dictionary/noun/nouns.txt")
    .forEach { line ->
        // process
    }

// Load a GZIP-compressed file
DictionaryProvider.readFileByLineFromResources("dictionary/noun/nouns.txt.gz")
    .forEach { line ->
        // process
    }

// Load into a CharArraySet (suspend)
val set = DictionaryProvider.readWords("dictionary/noun/nouns.txt")

// Load a frequency map
val freqMap = DictionaryProvider.readWordFreqs("dictionary/freq/freq.txt")
```

### 3. Exceptions Package

```kotlin
// Tokenizer-related exception
try {
    // tokenizer operation
} catch (e: TokenizerException) {
    // handle exception
}

// Invalid request exception
try {
    // process request
} catch (e: InvalidTokenizeRequestException) {
    // handle exception
}
```

## Usage Examples

### Blocked-Word Processing

```kotlin
fun processBlockword(text: String): BlockwordResponse {
    val options = blockwordOptionsOf(
        mask = "*",
        severity = Severity.HIGH,
    )

    val request = blockwordRequestOf(text, options)

    val detectedWords = detectBlockwords(request.text)
    val maskedText = maskWords(request.text, detectedWords, request.options.mask)

    return blockwordResponseOf(
        original = request.text,
        masked = maskedText,
        words = detectedWords,
    )
}
```

### Morphological Analysis

```kotlin
fun tokenize(text: String): TokenizeResponse {
    val request = tokenizeRequestOf(text)

    val tokens = performTokenization(request.text)

    return tokenizeResponseOf(
        original = request.text,
        tokens = tokens,
    )
}
```

### Building a Dictionary

```kotlin
suspend fun buildDictionary(): CharArraySet {
    val dictionary = CharArraySet(10_000)

    DictionaryProvider.readWords("dictionary/noun/nouns.txt")
        .forEach { dictionary.add(it) }

    DictionaryProvider.readWords("dictionary/verb/verb.txt")
        .forEach { dictionary.add(it) }

    return dictionary
}
```

## Dependency

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-tokenizer-core:$version")
}
```

### Transitive Dependencies

- bluetape4k-logging
- bluetape4k-core

## Testing

```bash
# Run all tests
./gradlew :bluetape4k-tokenizer-core:test

# Run a specific test class
./gradlew :bluetape4k-tokenizer-core:test \
    --tests "io.bluetape4k.tokenizer.utils.CharArrayMapTest"
```

## Notes

- All APIs support Kotlin coroutines (suspend functions)
- Dictionary files must be placed under `src/main/resources/dictionary`
- GZIP-compressed dictionary files must have a `.gz` extension to be recognized automatically
