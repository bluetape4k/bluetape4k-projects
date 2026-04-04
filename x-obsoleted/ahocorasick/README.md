# Module bluetape4k-ahocorasick

English | [한국어](./README.ko.md)

> **⚠️ Deprecated**: This module has been deprecated and excluded from the build. Maintenance will be discontinued.

A Kotlin implementation of the Aho-Corasick string search algorithm. It enables efficient simultaneous searching of multiple keywords and is optimized for processing large volumes of text.

## Features

- **Multi-keyword search**: Search for multiple keywords in a single pass.
- **Fast search speed**: Uses the Aho-Corasick algorithm with O(n) time complexity.
- **Flexible configuration**: Supports case-insensitive matching, whole-word matching, overlap removal, and more.
- **Kotlin-friendly**: Provides an intuitive API using Kotlin DSL and extension functions.

## Dependency

```kotlin
dependencies {
  implementation("io.github.bluetape4k:bluetape4k-ahocorasick:${bluetape4kVersion}")
}
```

## Basic Usage

### Building a Trie

```kotlin
import io.bluetape4k.ahocorasick.trie.Trie

val trie = Trie.builder()
  .addKeyword("NYC")
  .addKeyword("APPL")
  .addKeyword("java_2e")
  .addKeywords("PM", "product manager")
  .build()
```

### Searching for Keywords in Text

```kotlin
val text = "I am a PM for a java_2e platform working from APPL, NYC"
val emits = trie.parseText(text)

// Result: [Emit(7, 8, "PM"), Emit(16, 22, "java_2e"), Emit(46, 49, "APPL"), Emit(52, 54, "NYC")]
emits.forEach { emit ->
  println("Found '${emit.keyword}' at position ${emit.start}-${emit.end}")
}
```

### Finding the First Match Only

```kotlin
val firstMatch = trie.firstMatch(text)
// Result: Emit(7, 8, "PM")
```

### Checking for Any Match

```kotlin
val hasMatch = trie.containsMatch(text)
// Result: true
```

## Advanced Configuration

### Case-Insensitive Matching

```kotlin
val trie = Trie.builder()
  .ignoreCase()
  .addKeywords("Hello", "World")
  .build()

val emits = trie.parseText("HELLO world")
// Both "Hello" and "World" match
```

### Whole-Word Matching Only

```kotlin
val trie = Trie.builder()
  .onlyWholeWords()
  .addKeyword("sugar")
  .build()

val emits = trie.parseText("sugarcane sugar canesugar")
// Only "sugar" matches (sugarcane and canesugar are excluded)
```

### Whitespace-Separated Whole Words

```kotlin
val trie = Trie.builder()
  .onlyWholeWordsWhiteSpaceSeparated()
  .addKeyword("#sugar-123")
  .build()

val emits = trie.parseText("#sugar-123 #sugar-1234")
// Only the first "#sugar-123" matches
```

### Removing Overlapping Matches

```kotlin
val trie = Trie.builder()
  .ignoreOverlaps()  // Remove overlapping matches
  .addKeyword("ab")
  .addKeyword("cba")
  .addKeyword("ababc")
  .build()

val emits = trie.parseText("ababcbab")
// After overlap removal, only the larger intervals are kept
```

### Stop on First Hit

```kotlin
val trie = Trie.builder()
  .stopOnHit()
  .addKeywords("he", "she", "hers")
  .build()

val emits = trie.parseText("ushers")
// Stops after first match ("he"), result: [Emit(2, 3, "he")]
```

## Tokenization

Splits text into matched keyword tokens and non-keyword fragments.

```kotlin
val trie = Trie.builder()
  .addKeywords("Alpha", "Beta", "Gamma")
  .build()

val tokens = trie.tokenize("Hear: Alpha team first, Beta from the rear, Gamma in reserve")

// Result:
// FragmentToken("Hear: ")
// MatchToken("Alpha", Emit(...))
// FragmentToken(" team first, ")
// MatchToken("Beta", Emit(...))
// FragmentToken(" from the rear, ")
// MatchToken("Gamma", Emit(...))
// FragmentToken(" in reserve")
```

## Keyword Replacement

```kotlin
val trie = Trie.builder()
  .addKeywords("APPL", "NYC", "PM")
  .build()

val map = mapOf(
  "APPL" to "Apple",
  "NYC" to "New York",
  "PM" to "Product Manager"
)

val replaced = trie.replace("I am a PM from APPL, NYC", map)
// Result: "I am a Product Manager from Apple, New York"
```

## Using EmitHandler

Process matched results as they are found.

```kotlin
val trie = Trie.builder()
  .addKeywords("he", "she", "hers")
  .build()

// Default handler
val handler = DefaultEmitHandler()
trie.runParseText("ushers", handler)
println(handler.emits)  // All matched results

// Custom handler
val customHandler = EmitHandler { emit ->
  if (emit.keyword?.length ?: 0 >= 3) {
    println("Long keyword found: ${emit.keyword}")
    true  // Continue processing
  } else {
    false // Stop
  }
}
trie.runParseText("ushers", customHandler)
```

## IntervalTree

A tree structure for efficiently finding overlaps between intervals.

```kotlin
import io.bluetape4k.ahocorasick.interval.Interval
import io.bluetape4k.ahocorasick.interval.IntervalTree

val intervals = listOf(
  Interval(0, 2),
  Interval(4, 6),
  Interval(1, 3)
)

val tree = IntervalTree(intervals)

// Find overlaps
val overlaps = tree.findOverlaps(Interval(0, 2))
// Result: [Interval(1, 3)]

// Remove overlaps (larger intervals take priority)
val nonOverlapping = tree.removeOverlaps(intervals)
```

## Performance

- **Time complexity**: O(n + m + z)
  - n: text length
  - m: total length of all keywords
  - z: number of matches
- **Space complexity**: O(m)

Operates efficiently even on large texts (over 1 million characters).

## References

- [Aho-Corasick Algorithm (Wikipedia)](https://en.wikipedia.org/wiki/Aho%E2%80%93Corasick_algorithm)
- [Bell Technologies White Paper](http://cr.yp.to/bib/1975/aho.pdf)
