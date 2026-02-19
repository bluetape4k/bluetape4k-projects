# Module bluetape4k-money

## 개요

Java 표준 Money API (JSR-354)를 기반으로 금융 및 통화 연산을 쉽게 수행할 수 있는 라이브러리입니다.
[JavaMoney Moneta](https://javamoney.github.io/ri.html) 구현체를 사용하여 통화 단위, 금액 계산, 환율 변환을 지원합니다.

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-money:${version}")
}
```

## 주요 기능

- **통화 단위 (CurrencyUnit)**: KRW, USD, EUR, CNY, JPY 등 주요 통화 지원
- **금액 (Money)**: 통화 금액 생성 및 연산
- **고성능 금액 (FastMoney)**: Long 기반의 고성능 금액 연산
- **환율 변환**: ECB, IMF 환율 데이터를 이용한 통화 변환

## 사용 예시

### 통화 단위 생성

```kotlin
import io.bluetape4k.money.*

// 통화 코드로 생성
val krw = currencyUnitOf("KRW")
val usd = currencyUnitOf("USD")
val eur = currencyUnitOf("EUR")

// Locale로 생성
val usCurrency = currencyUnitOf(Locale.US)        // USD
val koreaCurrency = currencyUnitOf(Locale.KOREA)  // KRW

// 사전 정의된 통화 단위
val koreanWon = KRW      // 한국 원화
val usDollar = USD       // 미국 달러
val euro = EUR           // 유로
val chineseYuan = CNY    // 중국 위안
val japaneseYen = JPY    // 일본 엔

// 시스템 기본 통화
val defaultCurrency = DefaultCurrencyUnit
val defaultCode = DefaultCurrencyCode
```

### 금액 생성 (Money)

```kotlin
import io.bluetape4k.money.*

// 기본 방식
val won = moneyOf(1024L, KRW)       // 1,024 KRW
val dollar = moneyOf(1.05, USD)     // 1.05 USD

// 통화 코드 문자열로 생성
val yen = moneyOf(1000, "JPY")      // 1,000 JPY

// 확장 함수 사용
val won2 = 1024L.toMoney(KRW)       // 1,024 KRW
val dollar2 = 1.05.toMoney("USD")   // 1.05 USD

// 편의 함수
val krw = 10000.inKRW()             // 10,000 KRW
val usd = 100.50.inUSD()            // 100.50 USD
val eur = 50.inEUR()                // 50 EUR
```

### 고성능 금액 (FastMoney)

`FastMoney`는 내부적으로 Long 타입만 사용하여 고성능 연산을 제공합니다.

```kotlin
import io.bluetape4k.money.*

// FastMoney 생성
val fastWon = fastMoneyOf(1024L, KRW)
val fastDollar = fastMoneyOf(1.05, USD)

// 확장 함수
val fastKrw = 10000.toFastMoney("KRW")
val fastUsd = 100.50.toFastMoney(USD)

// Minor 단위로 생성 (소수점 포함 금액)
// 1245를 소수점 2자리로 해석 = 12.45
val money = fastMoneyMinorOf("USD", 1245L, 2)  // $12.45
val money2 = 1245L.toFastMoneyMinor(USD, 2)    // $12.45

// 편의 함수
val fastKrw2 = 10000.inFastKRW()
val fastUsd2 = 100.50.inFastUSD()
val fastEur2 = 50.inFastEUR()
```

### 환율 변환

```kotlin
import io.bluetape4k.money.*

// USD를 EUR로 변환
val usd = 100.0.toMoney(USD)
val eur = usd.convertTo(EUR)

// USD를 KRW로 변환
val krw = usd.convertTo(KRW)

// 역변환 검증
eur.convertTo(USD).doubleValue shouldBeNear usd.doubleValue
krw.convertTo(USD).doubleValue shouldBeNear usd.doubleValue

// CurrencyConversion 직접 사용
val conversion = CurrencyConvertor.getConversion(USD)
val converted = conversion.conversion(KRW).apply(usd)
```

### 금액 연산

```kotlin
import io.bluetape4k.money.*

val price1 = 1000.toMoney(KRW)
val price2 = 500.toMoney(KRW)

// 덧셈
val total = price1 + price2  // 1,500 KRW

// 뺄셈
val diff = price1 - price2   // 500 KRW

// 곱셈
val doubled = price1 * 2     // 2,000 KRW

// 나눗셈
val half = price1 / 2        // 500 KRW

// 비교
price1 > price2  // true
price1 == 1000.toMoney(KRW)  // true
```

## 주요 기능 상세

| 파일                             | 설명                                 |
|--------------------------------|------------------------------------|
| `CurrencySupport.kt`           | 통화 단위 생성 (KRW, USD, EUR, CNY, JPY) |
| `MoneySupport.kt`              | Money 인스턴스 생성 확장 함수                |
| `FastMoneySupport.kt`          | FastMoney 인스턴스 생성 확장 함수            |
| `MoneyAmountSupport.kt`        | 금액 연산 확장 함수                        |
| `CurrencyConverter.kt`         | 환율 변환기 (ECB, IMF 데이터 사용)           |
| `CurrencyConversionSupport.kt` | 통화 변환 확장 함수                        |

## Money vs FastMoney

| 특징    | Money            | FastMoney    |
|-------|------------------|--------------|
| 내부 타입 | BigDecimal       | Long         |
| 정밀도   | 무제한              | 소수점 5자리까지    |
| 성능    | 보통               | 매우 빠름        |
| 사용 예시 | 금융 계산, 높은 정밀도 필요 | 대량 연산, 성능 중요 |
