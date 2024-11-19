# Module bluetape4k-units

## 개요

여러가지 단위(Unit)를 `value class`를 이용하여 표현하는 기능을 제공합니다.

## 지원 단위

현재 다음과 같은 단위를 제공합니다.

* 무게 (Weight)
* 길이 (Length)
* 면적 (Area)
* 부피 (Volume)
* 속도 (Velocity)
* 전력 (Watt)
* 저장단위 (Storage)

## 사용 예

다음은 Kotlin extension methods를 이용하여, 읽기 편한 코드를 만들 수 있다는 것을 보여줍니다.

```kotlin
val sum = 5.meter() + 35.centimeter() // 5.35 meter

1500.milimeter().inMeter()  // return 1.5
```

여러가지 단위에 대해 사칙연산을 수행할 수 있습니다.

```kotlin
@Test
fun `length oprators`() {
    val a = 100.0.meter()
    val b = 200.0.meter()

    a + a shouldBeEqualTo b
    b - a shouldBeEqualTo a
    a * 2 shouldBeEqualTo b
    2 * a shouldBeEqualTo b
    b / 2 shouldBeEqualTo a
}
```
