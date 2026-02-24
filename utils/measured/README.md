# bluetape4k-measured

`bluetape4k-measured`는 조합 가능한 단위 타입(`Units`)과 측정값(`Measure`) 기반으로,
복합 단위(`m/s`, `kg*m/s^2`)를 타입 안전하게 표현하기 위한 모듈입니다.

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

## 테스트

```bash
./gradlew :bluetape4k-measured:test
```

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
