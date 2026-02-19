# Module bluetape4k-math

## 개요

Apache Commons Math3를 기반으로 수학/통계 연산, 보간, 적분, 방정식 해법, 클러스터링 등 다양한 수학 기능을 제공하는 라이브러리입니다.

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-math:${version}")
}
```

## 주요 기능

### 통계 및 기술통계

- 기술통계 (평균, 분산, 표준편차, 왜도, 첨도)
- 히스토그램 (Double, BigDecimal, Comparable)
- 이동평균, 이동합
- 순위, 상관계수

### 수학 함수

- 특수 함수 (Gamma, Beta, Factorial, Harmonic)
- 확률 분포
- 조합/순열
- 소수 판정

### 보간 및 적분

- 선형/스플라인/Loess 보간
- Romberg/Simpson/Trapezoid 적분

### 방정식 해법

- 이분법, 브렌트법, 시컨트법 등

### 선형대수

- 행렬/벡터 연산

### 머신러닝

- 클러스터링 (K-Means 등)
- 거리 측정

## 사용 예시

### 기술통계

```kotlin
import io.bluetape4k.math.*

val data = doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0)
val stats = data.descriptives()

stats.mean           // 평균: 5.5
stats.variance       // 분산
stats.standardDeviation  // 표준편차
stats.skewness       // 왜도
stats.kurtosis       // 첨도
stats.min            // 최소값: 1.0
stats.max            // 최대값: 10.0
stats.sum            // 합계: 55.0
stats.percentile(50) // 중앙값 (50th percentile)
```

### 집계 (Aggregation)

```kotlin
import io.bluetape4k.math.*
import java.time.Instant

data class Event(val eventTimestamp: Instant, val durationMs: Long)

val events: List<Event> = // ...

// 시간대별 duration 합계
val sumByHour = events.aggregateBy(
    keySelector = { it.eventTimestamp.truncatedTo(ChronoUnit.HOURS) },
    valueTransform = { it.durationMs },
    aggregator = { it.sum() }
)

// 시간대별 duration 평균
val avgByHour = events.aggregateBy(
    keySelector = { it.eventTimestamp.truncatedTo(ChronoUnit.HOURS) },
    valueTransform = { it.durationMs },
    aggregator = { it.average() }
)
```

### 랜덤 샘플링

```kotlin
import io.bluetape4k.math.*

val items = listOf("a", "b", "c", "d", "e", "f", "g", "h")

// 무작위 1개 선택
val randomItem = items.randomFirst()

// 복원 추출 (중복 허용) - 3개 샘플
val samples = items.random(3)

// 비복원 추출 (중복 없음) - 3개 샘플
val distinctSamples = items.randomDistinct(3)

// 가중치 동전 던지기
val isHeads = weightedCoinFlip(0.7)  // 70% 확률로 true

// 가중치 주사위
val dice = WeightedDice(
    "A" to 0.5,   // 50% 확률
    "B" to 0.3,   // 30% 확률
    "C" to 0.2    // 20% 확률
)
val result = dice.roll()  // "A", "B", "C" 중 하나 반환
```

### 히스토그램

```kotlin
import io.bluetape4k.math.*

val data = doubleArrayOf(1.0, 2.0, 2.5, 3.0, 3.5, 4.0, 5.0, 5.5, 6.0)

// Double 히스토그램
val histogram = DoubleHistogram.of(data, numBins = 5)
histogram.bins.forEach { bin ->
    println("Range: ${bin.lowerBound} - ${bin.upperBound}, Count: ${bin.count}")
}

// BigDecimal 히스토그램
val bdHistogram = BigDecimalHistogram.of(bigDecimalData, numBins = 10)

// Comparable 히스토그램
val compHistogram = ComparableHistogram.of(comparableData, numBins = 5)
```

### 보간 (Interpolation)

```kotlin
import io.bluetape4k.math.interpolation.*

val x = doubleArrayOf(0.0, 1.0, 2.0, 3.0, 4.0)
val y = doubleArrayOf(0.0, 1.0, 4.0, 9.0, 16.0)

// 선형 보간
val linear = linearInterpolatorOf(x, y)
linear.interpolate(1.5)  // 2.5

// 스플라인 보간
val spline = splineInterpolatorOf(x, y)
spline.interpolate(1.5)

// Loess 보간 (국소 회귀)
val loess = loessInterpolatorOf(x, y)
loess.interpolate(1.5)

// Akima 스플라인 보간
val akima = akimaSplineInterpolatorOf(x, y)
akima.interpolate(1.5)
```

### 적분 (Integration)

```kotlin
import io.bluetape4k.math.integration.*

// f(x) = x^2 함수
val function = { x: Double -> x * x }

// Romberg 적분
val romberg = rombergIntegratorOf()
val result1 = romberg.integrate(function, 0.0, 2.0)  // 약 2.667

// Simpson 적분
val simpson = simpsonIntegratorOf()
val result2 = simpson.integrate(function, 0.0, 2.0)

// Trapezoid 적분
val trapezoid = trapezoidIntegratorOf()
val result3 = trapezoid.integrate(function, 0.0, 2.0)

// MidPoint 적분
val midpoint = midPointIntegratorOf()
val result4 = midpoint.integrate(function, 0.0, 2.0)
```

### 방정식 해법

```kotlin
import io.bluetape4k.math.equation.*

// f(x) = x^2 - 4, 해는 x = 2 또는 x = -2
val function = { x: Double -> x * x - 4.0 }

// 이분법 (Bisection)
val bisection = bisectionEquatorOf(function, 0.0, 3.0, 1e-10)
val root1 = bisection.solve()  // 약 2.0

// 브렌트법 (Brent) - 빠르고 안정적
val brent = brentEquatorOf(function, 0.0, 3.0, 1e-10)
val root2 = brent.solve()

// 시컨트법 (Secant)
val secant = secantEquatorOf(function, 0.0, 3.0, 1e-10)
val root3 = secant.solve()
```

### 특수 함수

```kotlin
import io.bluetape4k.math.special.*

// 팩토리얼
factorial(5)     // 120
factorial(10)    // 3,628,800

// 감마 함수
gamma(5.0)       // 24.0 (= 4!)

// 베타 함수
beta(2.0, 3.0)

// 조합
combinations(10, 3)  // 120

// 순열
permutations(5, 3)   // 60
```

## 주요 기능 상세

| 파일                        | 설명                                              |
|---------------------------|-------------------------------------------------|
| `Aggregation.kt`          | 컬렉션 집계 함수                                       |
| `Descriptives.kt`         | 기술통계 인터페이스                                      |
| `DoubleStatistics.kt`     | Double 통계                                       |
| `BigDecimalStatistics.kt` | BigDecimal 통계                                   |
| `DoubleHistogram.kt`      | Double 히스토그램                                    |
| `RandomSupport.kt`        | 랜덤 샘플링                                          |
| `interpolation/*.kt`      | 보간 알고리즘 (Linear, Spline, Loess, Akima)          |
| `integration/*.kt`        | 적분 알고리즘 (Romberg, Simpson, Trapezoid, MidPoint) |
| `equation/*.kt`           | 방정식 해법 (Bisection, Brent, Secant, Ridders)      |
| `special/*.kt`            | 특수 함수 (Gamma, Beta, Factorial)                  |
| `linear/*.kt`             | 선형대수 (Matrix, Vector)                           |
| `ml/clustering/*.kt`      | 클러스터링 알고리즘                                      |
| `ml/distance/*.kt`        | 거리 측정 방법                                        |
| `commons/*.kt`            | Apache Commons Math 유틸리티                        |
