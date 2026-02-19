# Module bluetape4k-lingua

## 개요

[Lingua](https://github.com/pemistahl/lingua) 라이브러리를 기반으로 텍스트에서 언어를 자동 감지하는 기능을 제공합니다. 75개 이상의 언어를 고정밀도로 감지할 수 있습니다.

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-lingua:${version}")
}
```

## 주요 기능

- **언어 감지**: 텍스트에서 언어 자동 감지 (75+ 언어 지원)
- **유니코드 감지**: 문자열에서 특정 언어 문자 필터링
- **다양한 설정**: 감지 정확도, 최소 상대 거리 등 설정 가능

## 사용 예시

### 기본 언어 감지

```kotlin
import io.bluetape4k.lingua.*
import com.github.pemistahl.lingua.api.Language

// 모든 언어 대상 감지기 생성
val detector = allLanguageDetector {
    withPreloadedLanguageModels()
    withMinimumRelativeDistance(0.0)
}

// 언어 감지
val lang1 = detector.detectLanguageOf("Hello, World!")      // Language.ENGLISH
val lang2 = detector.detectLanguageOf("안녕하세요.")         // Language.KOREAN
val lang3 = detector.detectLanguageOf("こんにちは")          // Language.JAPANESE
val lang4 = detector.detectLanguageOf("你好，世界")         // Language.CHINESE
val lang5 = detector.detectLanguageOf("Bonjour le monde")   // Language.FRENCH
```

### 특정 언어만 감지

```kotlin
import io.bluetape4k.lingua.*
import com.github.pemistahl.lingua.api.Language

// 영어, 한국어만 감지
val languages = setOf(Language.ENGLISH, Language.KOREAN)
val detector = languageDetectorOf(languages) {
    withPreloadedLanguageModels()
    withMinimumRelativeDistance(0.0)
}

detector.detectLanguageOf("Hello")       // Language.ENGLISH
detector.detectLanguageOf("안녕")         // Language.KOREAN
detector.detectLanguageOf("こんにちは")    // Language.UNKNOWN (감지 대상 아님)
```

### ISO 코드로 언어 감지기 생성

```kotlin
import io.bluetape4k.lingua.*
import com.github.pemistahl.lingua.api.IsoCode639_1

// ISO 639-1 코드로 감지기 생성
val isoCodes = setOf(IsoCode639_1.EN, IsoCode639_1.KO, IsoCode639_1.JA)
val detector = languageDetectorOf(isoCodes) {
    withPreloadedLanguageModels()
}
```

### 특정 언어 제외

```kotlin
import io.bluetape4k.lingua.*
import com.github.pemistahl.lingua.api.Language

// 독일어, 태국어 제외
val exceptLanguages = setOf(Language.GERMAN, Language.THAI)
val detector = allLanguageWithoutDetector(exceptLanguages) {
    withPreloadedLanguageModels()
}
```

### 구어체만 감지

```kotlin
import io.bluetape4k.lingua.*

val detector = allSpokenLanguageDetector {
    withPreloadedLanguageModels()
    withLowAccuracyMode()
}
```

### 간편한 감지기 생성

```kotlin
import io.bluetape4k.lingua.*
import com.github.pemistahl.lingua.api.Language

// 기본 설정으로 감지기 생성
val detector = languageDetectorOf(
    languages = Language.all().toSet(),
    minimulRelativeDistance = 0.0,
    isEveryLangageModelPreloaded = true,
    isLowAccuracyModeEnabled = false
)
```

### 신뢰도 확인

```kotlin
import io.bluetape4k.lingua.*

val detector = allLanguageDetector { withPreloadedLanguageModels() }

// 신뢰도 맵 조회
val confidence = detector.computeLanguageConfidence("Hello", Language.ENGLISH)
// 0.0 ~ 1.0 사이의 값 반환

// 모든 언어에 대한 신뢰도 조회
val allConfidences = detector.computeLanguageConfidences("Hello")
```

## 유니코드 감지

```kotlin
import io.bluetape4k.lingua.*
import java.util.Locale

val detector = UnicodeDetector()

// 특정 언어 문자만 필터링
val koreanOnly = detector.filterString("Hello 안녕 こんにちは", Locale.KOREAN)
// Result: ['안', '녕']

// 특정 언어 문자 포함 여부 확인
val hasKorean = detector.containsAny("Hello 안녕", Locale.KOREAN)  // true
val hasJapanese = detector.containsAny("Hello 안녕", Locale.JAPANESE)  // false

// 모든 문자가 특정 언어인지 확인
val allKorean = detector.containsAll("안녕하세요", Locale.KOREAN)  // true
```

## 지원 언어

한국어, 영어, 일본어, 중국어, 프랑스어, 독일어, 스페인어, 이탈리아어, 포르투갈어, 러시아어, 아랍어, 힌디어, 태국어, 베트남어 등 75개 이상의 언어를 지원합니다.

## 주요 기능 상세

| 파일                    | 설명               |
|-----------------------|------------------|
| `LanguageDetector.kt` | 언어 감지기 생성 확장 함수  |
| `UnicodeDetector.kt`  | 유니코드 문자 기반 언어 감지 |
| `UnicodeSupport.kt`   | 유니코드 문자 판별 확장 함수 |

## 감지 정확도 향상 팁

1. **최소 상대 거리 설정**: `withMinimumRelativeDistance(0.25)`로 짧은 텍스트의 오감지 감소
2. **언어 모델 프리로드**: `withPreloadedLanguageModels()`로 초기 감지 속도 향상
3. **저정확도 모드**: `withLowAccuracyMode()`로 메모리 사용량 감소 (속도는 빠르지만 정확도 하락)
4. **대상 언어 제한**: 감지할 언어를 제한하면 정확도 향상
