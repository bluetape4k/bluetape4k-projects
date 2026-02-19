# Module bluetape4k-naivebayes

## 개요

Naive Bayes 알고리즘을 이용하여 분류(Classification)를 수행하는 기능을 제공합니다. 확률 기반 분류기로, 스팸 필터링, 텍스트 분류, 감성 분석 등에 활용할 수 있습니다.

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-naivebayes:${version}")
}
```

## 주요 기능

- **분류기 생성**: 특징(Feature)과 카테고리(Category)를 학습하여 분류기 생성
- **확률 예측**: 주어진 특징에 대한 카테고리 확률 예측
- **관측치 제한**: 메모리 관리를 위한 관측치 개수 제한
- **Laplace Smoothing**: k1, k2 파라미터를 통한 스무딩 지원

## 사용 예시

### 기본 분류기 사용

```kotlin
import io.bluetape4k.naivebayes.NaiveBayesClassifier

// 문자열 특징과 문자열 카테고리를 가진 분류기 생성
val classifier = NaiveBayesClassifier<String, String>()

// 학습 데이터 추가 (스팸 분류 예시)
classifier.addObservation("spam", "buy", "now", "free", "money")
classifier.addObservation("spam", "free", "offer", "click", "here")
classifier.addObservation("spam", "limited", "time", "offer", "buy")
classifier.addObservation("ham", "meeting", "tomorrow", "office")
classifier.addObservation("ham", "project", "update", "team", "meeting")
classifier.addObservation("ham", "lunch", "today", "office", "team")

// 예측
val category1 = classifier.predict("free", "money", "now")  // "spam"
val category2 = classifier.predict("meeting", "team", "office")  // "ham"

// 확률과 함께 예측
val result = classifier.predictWithProbability("buy", "now", "free")
// CategoryProbability(category=spam, probability=0.85)
println("Category: ${result?.category}, Probability: ${result?.probability}")
```

### 데이터 클래스 분류

```kotlin
import io.bluetape4k.naivebayes.NaiveBayesClassifier

data class Email(val words: Set<String>, val isSpam: Boolean)

val trainingData = listOf(
    Email(setOf("buy", "now", "free"), true),
    Email(setOf("meeting", "team", "project"), false),
    Email(setOf("limited", "offer", "click"), true),
    Email(setOf("lunch", "today", "office"), false)
)

// 분류기 생성
val classifier = NaiveBayesClassifier<String, Boolean>()

// 학습
trainingData.forEach { email ->
    classifier.addObservation(email.isSpam, email.words)
}

// 새 이메일 분류
val newEmail = setOf("free", "offer", "buy")
val isSpam = classifier.predict(newEmail)  // true
```

### 컬렉션에서 분류기 생성

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

// 컬렉션에서 분류기 생성
val classifier = documents.toNaiveBayesClassifier(
    featuresSelector = { it.words },
    categorySelector = { it.category }
)

// 예측
val category = classifier.predict("code", "programming", "software")  // "technology"
```

### 관측치 제한 설정

```kotlin
import io.bluetape4k.naivebayes.NaiveBayesClassifier

// 최대 1000개의 관측치만 유지 (오래된 것부터 제거)
val classifier = NaiveBayesClassifier<String, String>(
    observationLimit = 1000,
    k1 = 0.5,  // Laplace smoothing 파라미터
    k2 = 1.0   // Laplace smoothing 파라미터
)

// 데이터 추가
repeat(2000) { i ->
    val category = if (i % 2 == 0) "A" else "B"
    classifier.addObservation(category, "feature$i")
}

// 가장 최근 1000개만 유지됨
println(classifier.population.size)  // 1000
```

### 확률 기반 의사결정

```kotlin
import io.bluetape4k.naivebayes.NaiveBayesClassifier

val classifier = NaiveBayesClassifier<String, String>()
// ... 학습 ...

val features = setOf("feature1", "feature2", "feature3")
val result = classifier.predictWithProbability(features)

if (result != null && result.probability > 0.8) {
    println("High confidence prediction: ${result.category}")
} else {
    println("Low confidence or unknown category")
}
```

## Smoothing 파라미터

| 파라미터 | 기본값 | 설명                               |
|------|-----|----------------------------------|
| `k1` | 0.5 | Laplace smoothing 파라미터 (특징 확률)   |
| `k2` | 1.0 | Laplace smoothing 파라미터 (카테고리 확률) |

- **k1, k2가 높으면**: 새로운 특징에 더 보수적 (기존 패턴 우선)
- **k1, k2가 낮으면**: 새로운 특징에 더 민감 (학습 데이터 의존)

## 활용 시나리오

1. **스팸 필터링**: 이메일/메시지 스팸 분류
2. **텍스트 분류**: 뉴스 기사 카테고리 분류
3. **감성 분석**: 긍정/부정 리뷰 분류
4. **언어 감지**: 텍스트의 언어 분류
5. **문서 분류**: 문서 유형 분류

## 주요 기능 상세

| 파일                        | 설명                 |
|---------------------------|--------------------|
| `NaiveBayesClassifier.kt` | Naive Bayes 분류기 구현 |
| `NaiveBayesSupport.kt`    | 분류기 생성 확장 함수       |

## 알고리즘 특징

### 장점

- 빠른 학습 및 예측 속도
- 적은 학습 데이터로도 효과적
- 다차원 데이터 처리 용이

### 단점

- 특징 간 독립성 가정 (현실에서는 위배될 수 있음)
- 학습 데이터에 없는 특징 조합에 취약할 수 있음
