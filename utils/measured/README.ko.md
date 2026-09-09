# Module bluetape4k-measured

[English](./README.md) | 한국어

`bluetape4k-measured`는 조합 가능한 단위 타입(`Units`)과 측정값(`Measure`) 기반으로, 복합 단위(`m/s`, `kg*m/s^2`)를 타입 안전하게 표현하기 위한 모듈입니다.

## 핵심 개념

- `Units`: 단위의 정의(접미사, 기준 단위 대비 배율)
- `Measure<T: Units>`: 값 + 단위
- `UnitsProduct`, `UnitsRatio`, `InverseUnits`: 복합 단위 표현

## 제공 단위

- 길이: `Length`
- 시간: `Time`
- 질량: `Mass`
- 부피: `Volume`
- 온도: `Temperature` / `TemperatureDelta`
- 각도: `Angle`
- 면적: `Area`
- 저장용량: `Storage`
- 디지털 크기: `BinarySize`
- 주파수: `Frequency`
- 에너지/전력: `Energy`, `Power`
- 전기 물리량: `Current`, `Charge`, `Voltage`, `Resistance`
- 데이터 전송률: `DataRate`, `DataRateFormat`
- 역학: `Force`, `Torque`
- 운동량 단위 유틸: `MotionUnits`, `Velocity`, `Acceleration`
- 그래픽 길이: `GraphicsLength`
- 압력: `Pressure`

## 빠른 예제

```kotlin
import io.bluetape4k.measured.*
import io.bluetape4k.measured.Length.Companion.meters
import io.bluetape4k.measured.Time.Companion.seconds

val speed = 10 * meters / seconds
val duration = 5 * seconds
val distance = speed * duration

println(distance `as` meters) // 50.0 m
println(distance.toHuman())    // 50.0 m
```

전기 물리량과 역학 측정값은 서로 다른 의미 타입으로 유지됩니다.

```kotlin
val voltage = 2.kiloWatts() / 500.milliAmps()
val force = 1.kilograms() * 9.8.metersPerSecondSquared()
val torque = force.torqueAt(2.meters())
val rate = 10.megabytesPerSecond()

println(voltage.toHuman()) // 4.0 kV
println(torque.toHuman())  // 19.6 N·m
println(rate.toHuman())    // 10.0 MB/s
println(rate.toHuman(DataRateFormat.DECIMAL_BITS)) // 80.0 Mbit/s
```

`DataRate.toHuman()`은 기본으로 10진 바이트 단위를 사용합니다. 표시 정책을
명시해야 한다면 `DataRateFormat.DECIMAL_BITS` 또는
`DataRateFormat.BINARY_BYTES`를 전달합니다. 힘은 수직 모멘트암을 요구하는
`torqueAt`을 통해서만 토크가 되며, 일반 힘/길이 곱은 복합 단위로 유지됩니다.
전송률에 시간을 곱하면 `BinarySize`를 계산하고, 데이터 크기에서 전송률을
계산할 때는 `binarySize.toDataRate(duration)`을 사용합니다. 제네릭 `/` 연산자는
기존 `UnitsRatio<BinarySize, Time>` 반환형을 유지합니다.

## 테스트

```bash
./gradlew :bluetape4k-measured:test
```

## 클래스 다이어그램

![measured Class Structure diagram](../../docs/images/readme-diagrams/utils-measured-diagram-01.png)

## 단위 조합 흐름

![Unit Composition Flow diagram](../../docs/images/readme-diagrams/utils-measured-diagram-02.png)

## units 호환 어댑터

`bluetape4k-units`에서 `bluetape4k-measured`로 점진 전환할 수 있도록 호환 확장 함수를 제공합니다.

```kotlin
import io.bluetape4k.measured.*

val legacyLength = io.bluetape4k.units.Length(1500.0, io.bluetape4k.units.LengthUnit.METER)
val measuredLength = legacyLength.toMeasuredLength()
val roundTrip = measuredLength.toLegacyLength()

println(measuredLength.toHuman())   // 1.5 km
println(roundTrip.inMeter())        // 1500.0
```
