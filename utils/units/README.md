# bluetape4k-units

Kotlin `value class`를 이용하여 타입 안전한 단위 변환 및 연산을 제공하는 라이브러리입니다.

## Deprecated 안내

`bluetape4k-units`는 점진적으로 deprecated 처리 중이며, 신규 개발은 `bluetape4k-measured` 사용을 권장합니다.

- 기존 코드는 즉시 제거하지 않고 유지됩니다.
- 공개 타입(`Length`, `Weight`, `Area`, `Volume`, `Angle`, `Pressure`, `Storage`, `Temperature` 및 unit enum)에는 `@Deprecated` 경고가 추가되었습니다.
- `bluetape4k-measured`의 호환 어댑터를 통해 점진적으로 전환할 수 있습니다.

```kotlin
import io.bluetape4k.measured.toMeasuredLength
import io.bluetape4k.measured.toLegacyLength

val legacy = 1.5.kilometer()
val measured = legacy.toMeasuredLength()
val back = measured.toLegacyLength()
```

## 개요

`bluetape4k-units`는 다양한 물리량(길이, 무게, 면적, 부피 등)을 타입 안전하게 표현하고, 서로 다른 단위 간의 변환과 연산을 직관적으로 수행할 수 있게 해줍니다.

### 주요 특징

- **타입 안전성**: 컴파일 타임에 단위 검증
- **메모리 효율**: `@JvmInline value class`로 런타임 오버헤드 최소화
- **직관적인 문법**: Kotlin 확장 함수를 활용한 자연스러운 연산
- **단위 간 연산**: 길이 × 길이 = 면적, 면적 × 길이 = 부피 등의 연산 지원
- **확장 가능**: 새로운 단위 타입 쉽게 추가 가능

## 지원 단위

### 기본 물리량

| 단위 타입           | 기본 단위   | 지원 단위                  |
|-----------------|---------|------------------------|
| **Length** (길이) | mm      | mm, cm, m, km          |
| **Weight** (무게) | g       | mg, g, kg, ton         |
| **Area** (면적)   | mm²     | mm², cm², m²           |
| **Volume** (부피) | cc (ml) | cc, cm³, ml, dl, l, m³ |

### 전문적 단위

| 단위 타입                | 기본 단위 | 지원 단위                                                         |
|----------------------|-------|---------------------------------------------------------------|
| **Storage** (저장용량)   | byte  | B, KB, MB, GB, TB, PB, XB, ZB, YB                             |
| **Temperature** (온도) | K     | K, °C, °F                                                     |
| **Pressure** (압력)    | Pa    | Pa, hPa, kPa, MPa, GPa, bar, dbar, mbar, atm, psi, torr, mmHg |
| **Angle** (각도)       | deg   | deg, rad                                                      |

## 설치

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-units:$version")
}
```

## 사용 예시

### 기본 사용법

```kotlin
import io.bluetape4k.units.*

// 확장 함수로 생성
val length = 100.0.meter()
val weight = 50.0.kilogram()
val area = 25.0.meter2()

// 단위 변환
val lengthInKm = length.inKilometer()  // 0.1
val weightInGrams = weight.inGram()     // 50000.0
val areaInCm2 = area.inCentimeter2()    // 250000.0
```

### 단위 연산

```kotlin
// 길이 연산
val totalLength = 5.meter() + 35.centimeter()  // 5.35m
val doubled = 100.meter() * 2                  // 200m

// 단위 간 연산
val length = 10.0.meter()
val width = 5.0.meter()
val area = length * width          // 50.0 m²

val height = 3.0.meter()
val volume = area * height         // 150.0 m³

// 역 연산
val calculatedHeight = volume / area    // 3.0 m
val calculatedWidth = volume / height   // 50.0 m²
```

### 파싱 및 포맷팅

```kotlin
// 문자열 파싱
val length = Length.parse("100.5 m")
val weight = Weight.parse("50.0 kg")
val temp = Temperature.parse("25.0 °C")

// 사람이 읽기 쉬운 형식으로 변환
println(1500.0.meter().toHuman())      // "1.5 km"
println(500.0.gram().toHuman())        // "500.0 g"
println(1.5.ton().toHuman())           // "1.5 ton"

// 특정 단위로 변환
println(1000.0.meter().toHuman(LengthUnit.KILOMETER))  // "1.0 km"
```

### 특수 값 처리

```kotlin
// 영 (Zero)
val zero = Length.ZERO

// NaN (Not a Number)
val invalid = Length.parse("invalid")  // Length.NaN
val empty = Length.parse(null)         // Length.NaN

// 무한대
val infinite = Length(Double.POSITIVE_INFINITY)
```

### 온도 변환 (특별 케이스)

온도는 절대값(offset) 기반 변환이 필요하므로 특별한 처리가 필요합니다:

```kotlin
val celsius = 25.0.celsius()
println(celsius.inKelvin())      // 298.15
println(celsius.inFahrenheit())  // 77.0

val kelvin = 273.15.kelvin()
println(kelvin.inCelcius())      // 0.0 (섭씨 영도)
```

### 압력 변환

```kotlin
val atm = 1.0.atm()
println(atm.inPascal())     // 101325.0
println(atm.inBar())        // 1.01325
println(atm.inPsi())        // 14.6959...

val bar = 1.0.bar()
println(bar.inKiloPascal()) // 100.0
```

### 저장용량 계산

```kotlin
val file1 = 500.0.mbytes()
val file2 = 750.0.mbytes()
val total = file1 + file2

println(total.inGBytes())   // 1.2207...
println(total.toHuman())    // "1.2 GB"

// 큰 단위
val bigData = 1.0.pbytes()
println(bigData.inBytes())  // 1125899906842624.0
```

### 각도 변환

```kotlin
val rightAngle = 90.0.degree()
println(rightAngle.inRadian())  // 1.5707... (π/2)

val piRad = Math.PI.radian()
println(piRad.inDegree())       // 180.0

// 각도 정규화
println(450.0.degree().toHuman())  // "90.0 deg" (450° = 90°)
```

## 확장 가이드

새로운 단위 타입을 추가하려면 다음 인터페이스를 구현하세요:

```kotlin
// 1. 단위 인터페이스 구현
enum class MyUnit(
    override val unitName: String,
    override val factor: Double
): MeasurableUnit {
    UNIT1("u1", 1.0),
    UNIT2("u2", 100.0);

    companion object {
        fun parse(unitStr: String): MyUnit {
            // 파싱 로직
        }
    }
}

// 2. 측정값 클래스 구현
@JvmInline
value class MyQuantity(
    override val value: Double = 0.0
): Measurable<MyUnit> {
    // 연산자 구현
    operator fun plus(other: MyQuantity): MyQuantity = MyQuantity(value + other.value)
    // ... 기타 연산자

    // 변환 메서드
    override fun convertTo(newUnit: MyUnit): MyQuantity =
        MyQuantity(valueBy(newUnit), newUnit)

    override fun toHuman(): String {
        // 포맷팅 로직
    }

    companion object {
        val ZERO = MyQuantity(0.0)
        val NaN = MyQuantity(Double.NaN)
    }
}

// 3. 확장 함수 제공
fun <T: Number> T.myUnit(): MyQuantity = MyQuantity(this.toDouble(), MyUnit.UNIT1)
```

## 테스트

```bash
# 모든 테스트 실행
./gradlew :bluetape4k-units:test

# 특정 테스트 클래스 실행
./gradlew :bluetape4k-units:test --tests "io.bluetape4k.units.LengthTest"
```

## 참고

- 모든 단위는 기본 단위를 기준으로 내부적으로 저장됩니다 (예: Length는 mm, Weight는 g)
- `Double` 정밀도를 사용하므로 극단적인 값에서는 부동소수점 오차가 발생할 수 있습니다
- `NaN` 값은 연산에서 전파됩니다
