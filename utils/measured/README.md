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
- 각도: `Angle`
- 면적: `Area`
- 저장용량: `Storage`
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
```

## 테스트

```bash
./gradlew :bluetape4k-measured:test
```
