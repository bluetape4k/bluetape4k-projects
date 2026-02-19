# bluetape4k-ahocorasick

Aho-Corasick 문자열 검색 알고리즘을 Kotlin으로 구현한 모듈입니다. 여러 키워드를 동시에 효율적으로 검색할 수 있으며, 대용량 텍스트 처리에 최적화되어 있습니다.

## 특징

- **다중 키워드 검색**: 여러 키워드를 한 번에 검색할 수 있습니다.
- **빠른 검색 속도**: Aho-Corasick 알고리즘을 사용하여 O(n) 시간 복잡도로 검색합니다.
- **유연한 설정**: 대소문자 구분, 전체 단어 매칭, 중첩 매칭 등 다양한 옵션을 제공합니다.
- **Kotlin 친화적**: Kotlin DSL과 확장 함수를 활용한 직관적인 API를 제공합니다.

## 의존성 추가

```kotlin
dependencies {
  implementation("io.bluetape4k:bluetape4k-ahocorasick:${bluetape4kVersion}")
}
```

## 기본 사용법

### Trie 생성

```kotlin
import io.bluetape4k.ahocorasick.trie.Trie

val trie = Trie.builder()
  .addKeyword("NYC")
  .addKeyword("APPL")
  .addKeyword("java_2e")
  .addKeywords("PM", "product manager")
  .build()
```

### 텍스트에서 키워드 검색

```kotlin
val text = "I am a PM for a java_2e platform working from APPL, NYC"
val emits = trie.parseText(text)

// 결과: [Emit(7, 8, "PM"), Emit(16, 22, "java_2e"), Emit(46, 49, "APPL"), Emit(52, 54, "NYC")]
emits.forEach { emit ->
  println("Found '${emit.keyword}' at position ${emit.start}-${emit.end}")
}
```

### 첫 번째 매치만 찾기

```kotlin
val firstMatch = trie.firstMatch(text)
// 결과: Emit(7, 8, "PM")
```

### 매칭 여부 확인

```kotlin
val hasMatch = trie.containsMatch(text)
// 결과: true
```

## 고급 설정

### 대소문자 무시

```kotlin
val trie = Trie.builder()
  .ignoreCase()
  .addKeywords("Hello", "World")
  .build()

val emits = trie.parseText("HELLO world")
// "Hello"와 "World" 모두 매칭됨
```

### 전체 단어만 매칭

```kotlin
val trie = Trie.builder()
  .onlyWholeWords()
  .addKeyword("sugar")
  .build()

val emits = trie.parseText("sugarcane sugar canesugar")
// "sugar"만 매칭됨 (sugarcane, canesugar는 제외)
```

### 공백으로 구분된 전체 단어

```kotlin
val trie = Trie.builder()
  .onlyWholeWordsWhiteSpaceSeparated()
  .addKeyword("#sugar-123")
  .build()

val emits = trie.parseText("#sugar-123 #sugar-1234")
// 첫 번째 "#sugar-123"만 매칭됨
```

### 중첩 매칭 제거

```kotlin
val trie = Trie.builder()
  .ignoreOverlaps()  // 중첩된 매칭 제거
  .addKeyword("ab")
  .addKeyword("cba")
  .addKeyword("ababc")
  .build()

val emits = trie.parseText("ababcbab")
// 중첩 제거 후 큰 Interval만 유지
```

### 첫 매칭 시 중단

```kotlin
val trie = Trie.builder()
  .stopOnHit()
  .addKeywords("he", "she", "hers")
  .build()

val emits = trie.parseText("ushers")
// 첫 번째 매치("he") 후 중단, 결과: [Emit(2, 3, "he")]
```

## 토큰화 (Tokenize)

텍스트를 키워드와 비키워드로 분리합니다.

```kotlin
val trie = Trie.builder()
  .addKeywords("Alpha", "Beta", "Gamma")
  .build()

val tokens = trie.tokenize("Hear: Alpha team first, Beta from the rear, Gamma in reserve")

// 결과:
// FragmentToken("Hear: ")
// MatchToken("Alpha", Emit(...))
// FragmentToken(" team first, ")
// MatchToken("Beta", Emit(...))
// FragmentToken(" from the rear, ")
// MatchToken("Gamma", Emit(...))
// FragmentToken(" in reserve")
```

## 키워드 치환

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
// 결과: "I am a Product Manager from Apple, New York"
```

## EmitHandler 사용

매칭된 결과를 실시간으로 처리할 수 있습니다.

```kotlin
val trie = Trie.builder()
  .addKeywords("he", "she", "hers")
  .build()

// 기본 핸들러
val handler = DefaultEmitHandler()
trie.runParseText("ushers", handler)
println(handler.emits)  // 모든 매칭 결과

// 커스텀 핸들러
val customHandler = EmitHandler { emit ->
  if (emit.keyword?.length ?: 0 >= 3) {
    println("Long keyword found: ${emit.keyword}")
    true  // 계속 처리
  } else {
    false // 중단
  }
}
trie.runParseText("ushers", customHandler)
```

## IntervalTree

Interval 간의 오버랩을 효율적으로 찾기 위한 트리 구조입니다.

```kotlin
import io.bluetape4k.ahocorasick.interval.Interval
import io.bluetape4k.ahocorasick.interval.IntervalTree

val intervals = listOf(
  Interval(0, 2),
  Interval(4, 6),
  Interval(1, 3)
)

val tree = IntervalTree(intervals)

// 오버랩 찾기
val overlaps = tree.findOverlaps(Interval(0, 2))
// 결과: [Interval(1, 3)]

// 오버랩 제거
val nonOverlapping = tree.removeOverlaps(intervals)
// 결과: 크기가 큰 Interval 우선 유지
```

## 성능

- **시간 복잡도**: O(n + m + z)
  - n: 텍스트 길이
  - m: 모든 키워드 길이의 합
  - z: 매칭 결과의 수
- **공간 복잡도**: O(m)

대용량 텍스트(100만 문자 이상)에서도 효율적으로 동작합니다.

## 참고 자료

- [Aho-Corasick Algorithm (Wikipedia)](https://en.wikipedia.org/wiki/Aho%E2%80%93Corasick_algorithm)
- [Bell Technologies White Paper](http://cr.yp.to/bib/1975/aho.pdf)
