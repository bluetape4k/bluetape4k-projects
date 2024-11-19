# Module bluetape4k-money

## 개요

Java 표준 Money 를 쉽게 사용하기 위한 라이브러리입니다.

## 사용법

Money 의 표현 및 계산 및 환전을 손쉽게 표현할 수 있습니다.

```kotlin
val usd = 1.0.toMoney(USD)
val eur = usd.convertTo(EUR)
val krw = usd.convertTo(KRW)

eur.convertTo(USD).doubleValue.shouldBeNear(usd.doubleValue, 1e-2)
krw.convertTo(USD).doubleValue.shouldBeNear(usd.doubleValue, 1e-2)
```
