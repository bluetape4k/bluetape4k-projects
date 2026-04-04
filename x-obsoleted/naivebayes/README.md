# Module bluetape4k-naivebayes

English | [한국어](./README.ko.md)

> **⚠️ Deprecated**: This module has been deprecated and excluded from the build. Maintenance will be discontinued.

## Overview

Provides classification using the Naive Bayes algorithm. This probability-based classifier can be applied to spam filtering, text categorization, sentiment analysis, and more.

## Dependency

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-naivebayes:${version}")
}
```

## Key Features

- **Classifier training**: Learns from features and categories to build a classifier
- **Probabilistic prediction**: Predicts the most likely category and its probability for given features
- **Observation limit**: Caps the number of stored observations for memory management
- **Laplace Smoothing**: Smoothing support via the `k1` and `k2` parameters

## Usage Examples

### Basic Classifier

```kotlin
import io.bluetape4k.naivebayes.NaiveBayesClassifier

// Create a classifier with String features and String categories
val classifier = NaiveBayesClassifier<String, String>()

// Add training data (spam classification example)
classifier.addObservation("spam", "buy", "now", "free", "money")
classifier.addObservation("spam", "free", "offer", "click", "here")
classifier.addObservation("spam", "limited", "time", "offer", "buy")
classifier.addObservation("ham", "meeting", "tomorrow", "office")
classifier.addObservation("ham", "project", "update", "team", "meeting")
classifier.addObservation("ham", "lunch", "today", "office", "team")

// Predict
val category1 = classifier.predict("free", "money", "now")  // "spam"
val category2 = classifier.predict("meeting", "team", "office")  // "ham"

// Predict with probability
val result = classifier.predictWithProbability("buy", "now", "free")
// CategoryProbability(category=spam, probability=0.85)
println("Category: ${result?.category}, Probability: ${result?.probability}")
```

### Classifying Data Classes

```kotlin
import io.bluetape4k.naivebayes.NaiveBayesClassifier

data class Email(val words: Set<String>, val isSpam: Boolean)

val trainingData = listOf(
    Email(setOf("buy", "now", "free"), true),
    Email(setOf("meeting", "team", "project"), false),
    Email(setOf("limited", "offer", "click"), true),
    Email(setOf("lunch", "today", "office"), false)
)

val classifier = NaiveBayesClassifier<String, Boolean>()

trainingData.forEach { email ->
    classifier.addObservation(email.isSpam, email.words)
}

val newEmail = setOf("free", "offer", "buy")
val isSpam = classifier.predict(newEmail)  // true
```

### Building a Classifier from a Collection

```kotlin
import io.bluetape4k.naivebayes.*

data class Document(val words: List<String>, val category: String)

val documents = listOf(
    Document(listOf("sports", "football", "game"), "sports"),
    Document(listOf("sports", "basketball", "team"), "sports"),
    Document(listOf("tech", "computer", "software"), "technology"),
    Document(listOf("tech", "programming", "code"), "technology"),
    Document(listOf("politics", "election", "vote"), "politics")
)

val classifier = documents.toNaiveBayesClassifier(
    featuresSelector = { it.words },
    categorySelector = { it.category }
)

val category = classifier.predict("code", "programming", "software")  // "technology"
```

### Setting an Observation Limit

```kotlin
import io.bluetape4k.naivebayes.NaiveBayesClassifier

// Keep only the most recent 1000 observations (oldest are evicted)
val classifier = NaiveBayesClassifier<String, String>(
    observationLimit = 1000,
    k1 = 0.5,  // Laplace smoothing parameter
    k2 = 1.0   // Laplace smoothing parameter
)

repeat(2000) { i ->
    val category = if (i % 2 == 0) "A" else "B"
    classifier.addObservation(category, "feature$i")
}

println(classifier.population.size)  // 1000
```

### Probability-Based Decision Making

```kotlin
import io.bluetape4k.naivebayes.NaiveBayesClassifier

val classifier = NaiveBayesClassifier<String, String>()
// ... training ...

val features = setOf("feature1", "feature2", "feature3")
val result = classifier.predictWithProbability(features)

if (result != null && result.probability > 0.8) {
    println("High confidence prediction: ${result.category}")
} else {
    println("Low confidence or unknown category")
}
```

## Smoothing Parameters

| Parameter | Default | Description                                     |
|------|-----|--------------------------------------------------|
| `k1` | 0.5 | Laplace smoothing parameter (feature probability)  |
| `k2` | 1.0 | Laplace smoothing parameter (category probability) |

- **Higher k1, k2**: More conservative toward new features (existing patterns take precedence)
- **Lower k1, k2**: More sensitive to new features (relies heavily on training data)

## Use Cases

1. **Spam filtering**: Classifying email or message spam
2. **Text categorization**: Categorizing news articles
3. **Sentiment analysis**: Classifying positive/negative reviews
4. **Language detection**: Identifying the language of text
5. **Document classification**: Classifying document types

## Key Files

| File                        | Description                        |
|---------------------------|------------------------------------|
| `NaiveBayesClassifier.kt` | Naive Bayes classifier implementation |
| `NaiveBayesSupport.kt`    | Extension functions for building classifiers |

## Algorithm Characteristics

### Advantages

- Fast training and prediction
- Effective with small amounts of training data
- Handles high-dimensional data well

### Disadvantages

- Assumes feature independence (often violated in practice)
- May be unreliable for feature combinations not seen during training
