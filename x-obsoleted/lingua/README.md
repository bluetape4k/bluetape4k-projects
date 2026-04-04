# Module bluetape4k-lingua

English | [한국어](./README.ko.md)

> **⚠️ Deprecated**: This module has been deprecated and excluded from the build. Maintenance will be discontinued.

## Overview

Provides automatic language detection for text, powered by the [Lingua](https://github.com/pemistahl/lingua) library. Supports high-accuracy detection of 75+ languages.

## Dependency

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-lingua:${version}")
}
```

## Key Features

- **Language detection**: Automatic language detection from text (75+ languages supported)
- **Unicode detection**: Filter characters belonging to a specific language from a string
- **Configurable**: Adjust detection accuracy, minimum relative distance, and more

## Usage Examples

### Basic Language Detection

```kotlin
import io.bluetape4k.lingua.*
import com.github.pemistahl.lingua.api.Language

// Create a detector for all languages
val detector = allLanguageDetector {
    withPreloadedLanguageModels()
    withMinimumRelativeDistance(0.0)
}

// Detect language
val lang1 = detector.detectLanguageOf("Hello, World!")      // Language.ENGLISH
val lang2 = detector.detectLanguageOf("안녕하세요.")         // Language.KOREAN
val lang3 = detector.detectLanguageOf("こんにちは")          // Language.JAPANESE
val lang4 = detector.detectLanguageOf("你好，世界")         // Language.CHINESE
val lang5 = detector.detectLanguageOf("Bonjour le monde")   // Language.FRENCH
```

### Detecting Only Specific Languages

```kotlin
import io.bluetape4k.lingua.*
import com.github.pemistahl.lingua.api.Language

// Detect only English and Korean
val languages = setOf(Language.ENGLISH, Language.KOREAN)
val detector = languageDetectorOf(languages) {
    withPreloadedLanguageModels()
    withMinimumRelativeDistance(0.0)
}

detector.detectLanguageOf("Hello")       // Language.ENGLISH
detector.detectLanguageOf("안녕")         // Language.KOREAN
detector.detectLanguageOf("こんにちは")    // Language.UNKNOWN (not in target set)
```

### Building a Detector from ISO Codes

```kotlin
import io.bluetape4k.lingua.*
import com.github.pemistahl.lingua.api.IsoCode639_1

// Build a detector using ISO 639-1 codes
val isoCodes = setOf(IsoCode639_1.EN, IsoCode639_1.KO, IsoCode639_1.JA)
val detector = languageDetectorOf(isoCodes) {
    withPreloadedLanguageModels()
}
```

### Excluding Specific Languages

```kotlin
import io.bluetape4k.lingua.*
import com.github.pemistahl.lingua.api.Language

// Exclude German and Thai
val exceptLanguages = setOf(Language.GERMAN, Language.THAI)
val detector = allLanguageWithoutDetector(exceptLanguages) {
    withPreloadedLanguageModels()
}
```

### Spoken Languages Only

```kotlin
import io.bluetape4k.lingua.*

val detector = allSpokenLanguageDetector {
    withPreloadedLanguageModels()
    withLowAccuracyMode()
}
```

### Convenient Detector Builder

```kotlin
import io.bluetape4k.lingua.*
import com.github.pemistahl.lingua.api.Language

// Build a detector with default settings
val detector = languageDetectorOf(
    languages = Language.all().toSet(),
    minimulRelativeDistance = 0.0,
    isEveryLangageModelPreloaded = true,
    isLowAccuracyModeEnabled = false
)
```

### Checking Confidence Scores

```kotlin
import io.bluetape4k.lingua.*

val detector = allLanguageDetector { withPreloadedLanguageModels() }

// Get confidence for a specific language
val confidence = detector.computeLanguageConfidence("Hello", Language.ENGLISH)
// Returns a value between 0.0 and 1.0

// Get confidence for all languages
val allConfidences = detector.computeLanguageConfidences("Hello")
```

## Unicode Detection

```kotlin
import io.bluetape4k.lingua.*
import java.util.Locale

val detector = UnicodeDetector()

// Filter characters of a specific language
val koreanOnly = detector.filterString("Hello 안녕 こんにちは", Locale.KOREAN)
// Result: ['안', '녕']

// Check if a string contains characters of a specific language
val hasKorean = detector.containsAny("Hello 안녕", Locale.KOREAN)  // true
val hasJapanese = detector.containsAny("Hello 안녕", Locale.JAPANESE)  // false

// Check if all characters belong to a specific language
val allKorean = detector.containsAll("안녕하세요", Locale.KOREAN)  // true
```

## Supported Languages

Supports 75+ languages including Korean, English, Japanese, Chinese, French, German, Spanish, Italian, Portuguese, Russian, Arabic, Hindi, Thai, Vietnamese, and more.

## Key Files

| File                    | Description                                        |
|-----------------------|--------------------------------------------------|
| `LanguageDetector.kt` | Extension functions for building language detectors |
| `UnicodeDetector.kt`  | Unicode character-based language detection          |
| `UnicodeSupport.kt`   | Extension functions for identifying Unicode scripts |

## Tips for Improving Accuracy

1. **Set minimum relative distance**: Use `withMinimumRelativeDistance(0.25)` to reduce misdetection of short texts
2. **Preload language models**: Use `withPreloadedLanguageModels()` to speed up initial detection
3. **Low accuracy mode**: Use `withLowAccuracyMode()` to reduce memory usage (faster but less accurate)
4. **Restrict target languages**: Limiting the set of languages to detect improves accuracy
