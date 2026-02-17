# Module bluetape4k-core

Kotlin Backend 개발을 위한 핵심 유틸리티 라이브러리입니다. Bluetape4k 프로젝트의 모든 모듈이 의존하는 기본 기능들을 제공합니다.

## 주요 기능

- **Validation (RequireSupport)**: 파라미터 검증을 위한 Contract 기반 함수들
- **Encoding/Decoding (Codec)**: Base58, Base62, Hex, URL62 등 다양한 인코딩
- **Type Extensions**: 모든 기본 타입에 대한 Kotlin 스타일 확장 함수
- **Ranges**: 다양한 Range 타입 (OpenOpen, ClosedOpen, OpenClosed, ClosedClosed)
- **Collections**: 컬렉션 유틸리티
- **Concurrent**: 동시성 처리 유틸리티
- **Functional**: 함수형 프로그래밍 지원
- **Java Time DSL**: `java.time` 기초 확장 함수 (Duration/Period DSL, Temporal 유틸리티, Quarter 등)

## 의존성 추가

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-core:${version}")
}
```

## 주요 기능 상세

### 1. Validation (RequireSupport)

Kotlin Contract를 활용하여 타입 안전성을 보장하는 파라미터 검증 함수들입니다.

#### Null 체크

```kotlin
import io.bluetape4k.support.requireNotNull
import io.bluetape4k.support.requireNull

fun processUser(user: User?) {
    val validUser = user.requireNotNull("user")
    // validUser는 non-null로 스마트 캐스팅됨
    println(validUser.name)
}

fun ensureNoValue(value: String?) {
    value.requireNull("value")
    // value가 null이 아니면 IllegalArgumentException 발생
}
```

#### 문자열 검증

```kotlin
import io.bluetape4k.support.requireNotEmpty
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requireContains
import io.bluetape4k.support.requireStartsWith
import io.bluetape4k.support.requireEndsWith

fun createUser(username: String?, email: String?) {
    val validUsername = username.requireNotEmpty("username")
    // username이 null이거나 empty면 예외 발생

    val validEmail = email
        .requireNotBlank("email")
        .requireContains("@", "email")
        .requireEndsWith(".com", "email")
}
```

#### 숫자 비교 검증

```kotlin
import io.bluetape4k.support.requireGt
import io.bluetape4k.support.requireGe
import io.bluetape4k.support.requireLt
import io.bluetape4k.support.requireLe
import io.bluetape4k.support.requireEquals

fun setAge(age: Int) {
    age.requireGe(0, "age")
        .requireLt(150, "age")
}

fun setQuantity(quantity: Int) {
    quantity.requireGt(0, "quantity")  // 0보다 커야 함
}

fun validateScore(score: Double) {
    score.requireGe(0.0, "score")
        .requireLe(100.0, "score")
}
```

**특징:**

- Kotlin Contract 사용으로 스마트 캐스팅 지원
- 체이닝 가능 (fluent API)
- 명확한 에러 메시지 자동 생성

### 2. Encoding/Decoding (Codec)

다양한 인코딩 방식을 통일된 인터페이스로 제공합니다.

#### Base64 인코딩

```kotlin
import io.bluetape4k.codec.encodeBase64String
import io.bluetape4k.codec.decodeBase64

val text = "Hello, World!"
val encoded = text.encodeBase64String()  // "SGVsbG8sIFdvcmxkIQ=="
val decoded = encoded.decodeBase64().decodeToString()  // "Hello, World!"

// URL-safe Base64
val urlSafe = text.encodeBase64UrlSafeString()
```

#### Base58 인코딩 (Bitcoin 스타일)

```kotlin
import io.bluetape4k.codec.Base58

val data = "Hello".toByteArray()
val encoded = Base58.encode(data)  // "9Ajdvzr"
val decoded = Base58.decode(encoded).decodeToString()  // "Hello"
```

**사용 사례:**

- Bitcoin 주소
- 짧은 ID 생성
- URL에 안전한 식별자

#### Base62 인코딩

```kotlin
import io.bluetape4k.codec.Base62

val number = 123456789L
val encoded = Base62.encode(number)  // "8M0kX"
val decoded = Base62.decode(encoded)  // 123456789
```

**사용 사례:**

- 짧은 URL (Short URL)
- ID 난독화
- 파일명 생성

#### Hex 인코딩

```kotlin
import io.bluetape4k.codec.encodeHexString
import io.bluetape4k.codec.decodeHex

val data = byteArrayOf(0x48, 0x65, 0x6C, 0x6C, 0x6F)
val hex = data.encodeHexString()  // "48656c6c6f"
val decoded = hex.decodeHex()  // [72, 101, 108, 108, 111]
```

#### URL62 인코딩

```kotlin
import io.bluetape4k.codec.Url62

val url = "https://example.com/path?query=value"
val encoded = Url62.encode(url)  // URL에 안전한 문자열
val decoded = Url62.decode(encoded)  // 원본 URL
```

### 3. Type Extensions (Support)

모든 기본 타입에 대한 유용한 확장 함수들을 제공합니다.

#### Any Extensions

```kotlin
import io.bluetape4k.support.hashOf
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.support.toUtf8String

// 객체의 hashCode 계산
val hash = hashOf(obj1, obj2, obj3)

// 문자열 <-> ByteArray 변환
val bytes = "Hello".toUtf8Bytes()
val str = bytes.toUtf8String()
```

#### Boolean Extensions

```kotlin
import io.bluetape4k.support.ifTrue
import io.bluetape4k.support.ifFalse
import io.bluetape4k.support.not

val isAdmin = true
isAdmin.ifTrue { println("Admin access granted") }

val isGuest = false
isGuest.ifFalse { println("Not a guest") }
```

#### Number Extensions

```kotlin
import io.bluetape4k.support.coerceIn
import io.bluetape4k.support.isEven
import io.bluetape4k.support.isOdd

val age = 25
age.isOdd()  // true
age.isEven()  // false

val value = 150
val bounded = value.coerceIn(0, 100)  // 100
```

#### String Extensions

```kotlin
import io.bluetape4k.support.isWhiteSpace
import io.bluetape4k.support.replaceFirst
import io.bluetape4k.support.toUtf8Bytes

val text = "  Hello  "
text.isWhiteSpace()  // false

val trimmed = text.trim()
```

#### Array Extensions

```kotlin
import io.bluetape4k.support.toHexString
import io.bluetape4k.support.isNullOrEmpty

val array = byteArrayOf(1, 2, 3, 4)
val hex = array.toHexString()  // "01020304"

val empty = emptyArray<String>()
empty.isNullOrEmpty()  // true
```

### 4. Ranges

Java/Kotlin의 기본 Range는 ClosedRange만 지원하지만, 다양한 Range 타입을 제공합니다.

```kotlin
import io.bluetape4k.ranges.*

// Closed-Closed Range: [1, 10] (1과 10 포함)
val closedClosed = 1 rangeTo 10
closedClosed.contains(1)   // true
closedClosed.contains(10)  // true

// Open-Open Range: (1, 10) (1과 10 제외)
val openOpen = 1 rangeOpen 10
openOpen.contains(1)   // false
openOpen.contains(10)  // false

// Closed-Open Range: [1, 10) (1 포함, 10 제외)
val closedOpen = 1 rangeUntil 10
closedOpen.contains(1)   // true
closedOpen.contains(10)  // false

// Open-Closed Range: (1, 10] (1 제외, 10 포함)
val openClosed = 1 openRangeTo 10
openClosed.contains(1)   // false
openClosed.contains(10)  // true
```

**사용 사례:**

- 수학적 구간 표현
- 검증 범위 지정
- 날짜/시간 범위

### 5. Collections

컬렉션 처리를 위한 다양한 유틸리티를 제공합니다.

```kotlin
import io.bluetape4k.collections.*

// Immutable Empty Collections
val emptyList = emptyList<String>()
val emptySet = emptySet<Int>()
val emptyMap = emptyMap<String, Int>()

// Safe operations
val list = listOf(1, 2, 3)
val first = list.firstOrNull()  // 1
val last = list.lastOrNull()    // 3

// Chunking
val chunked = (1..10).toList().chunked(3)
// [[1,2,3], [4,5,6], [7,8,9], [10]]

// Partitioning
val (even, odd) = (1..10).toList().partition { it % 2 == 0 }
// even = [2,4,6,8,10], odd = [1,3,5,7,9]
```

### 6. Lazy Initialization

Thread-safe lazy initialization 패턴을 제공합니다.

```kotlin
import io.bluetape4k.support.lazy

// Thread-safe lazy
val expensive by lazy {
    println("Computing expensive value...")
    computeExpensiveValue()
}

// 사용 시에만 계산됨
println(expensive)  // "Computing expensive value..." 출력 후 값 반환
println(expensive)  // 이미 계산된 값 반환 (재계산 안 됨)
```

### 7. Value Objects

불변 객체를 쉽게 만들기 위한 베이스 클래스입니다.

```kotlin
import io.bluetape4k.ValueObject
import io.bluetape4k.AbstractValueObject

data class Money(
    val amount: BigDecimal,
    val currency: String
): AbstractValueObject() {
    override fun equalProperties(other: Any): Boolean {
        return other is Money &&
                amount == other.amount &&
                currency == other.currency
    }
}

val money1 = Money(100.toBigDecimal(), "USD")
val money2 = Money(100.toBigDecimal(), "USD")

money1 == money2  // true
money1.hashCode() == money2.hashCode()  // true
```

### 8. AutoCloseable Support

Java의 `try-with-resources`를 Kotlin 스타일로 사용합니다.

```kotlin
import io.bluetape4k.support.closeSafe
import io.bluetape4k.support.use

// 자동 close (예외 무시)
resource.closeSafe()

// use 블록 (자동 close)
FileInputStream("file.txt").use { stream ->
    stream.read()
}

// 여러 리소스
use(resource1, resource2) { r1, r2 ->
    // 둘 다 자동으로 close됨
}
```

## 전체 예시

```kotlin
import io.bluetape4k.support.*
import io.bluetape4k.codec.*
import io.bluetape4k.ranges.*

class UserService {
    fun createUser(
        username: String?,
        email: String?,
        age: Int,
        bio: String?
    ): User {
        // Validation
        val validUsername = username
            .requireNotBlank("username")
            .requireStartsWith("user_", "username")

        val validEmail = email
            .requireNotBlank("email")
            .requireContains("@", "email")

        age.requireGe(18, "age")
            .requireLt(100, "age")

        bio?.requireLe(500, "bio length") { it.length }

        // ID 생성 (Base62 인코딩)
        val userId = Base62.encode(System.currentTimeMillis())

        return User(
            id = userId,
            username = validUsername,
            email = validEmail,
            age = age,
            bio = bio
        )
    }

    fun validateAge(age: Int): Boolean {
        val adultRange = 18 rangeUntil 65  // [18, 65)
        return age in adultRange
    }
}

// Usage
val service = UserService()
val user = service.createUser(
    username = "user_john",
    email = "john@example.com",
    age = 25,
    bio = "Software developer"
)
```

## 모범 사례

### 1. Validation 일관성

```kotlin
// ✅ 좋은 예: 메서드 시작 부분에서 모든 검증
fun processOrder(orderId: String?, items: List<Item>?) {
    val validOrderId = orderId.requireNotBlank("orderId")
    val validItems = items.requireNotNull("items")
    validItems.requireNotEmpty("items")

    // 비즈니스 로직
}

// ❌ 나쁜 예: 검증이 흩어져 있음
fun processOrder(orderId: String?, items: List<Item>?) {
    val id = orderId!!  // 위험!
    // ... some code
    if (items == null) throw Exception()  // 일관성 없음
}
```

### 2. 인코딩 선택

```kotlin
// Base64: 이진 데이터 전송
val imageData = image.encodeBase64String()

// Base58: Bitcoin 주소, 읽기 쉬운 ID
val shortId = Base58.encode(uuid.toByteArray())

// Base62: URL 단축, 숫자 ID 인코딩
val shortUrl = Base62.encode(urlId)

// Hex: 디버깅, 로그 출력
val debug = data.toHexString()
```

### 3. Range 활용

```kotlin
// 날짜 범위 검증
fun isWithinPeriod(date: LocalDate, start: LocalDate, end: LocalDate): Boolean {
    val period = start rangeTo end
    return date in period
}

// 점수 범위
val passingScore = 60 rangeUntil 100  // [60, 100)
val score = 75
if (score in passingScore) {
    println("Passed!")
}
```

## 성능 고려사항

### Validation 비용

```kotlin
// require 계열 함수는 인라인이므로 오버헤드 최소
val name = username.requireNotBlank("username")  // ✅ 빠름

// 복잡한 검증은 lazy 평가 고려
val valid = data.validate {
    expensive Validation ()
}
```

### 인코딩 성능

- **Base64**: 가장 빠름
- **Hex**: 빠름
- **Base58/Base62**: 중간 (BigInteger 연산)

### 9. Java Time DSL (javatimes)

`java.time` API를 Kotlin 스타일로 사용하기 위한 기초 확장 함수들을 제공합니다. 고급 기능(Interval, Period Framework, Temporal Range)은
`bluetape4k-javatimes` 모듈을 참조하세요.

#### Duration/Period DSL

```kotlin
import io.bluetape4k.javatimes.*

// Duration 생성
val duration = 5.days() + 3.hours() + 30.minutes() + 45.seconds()
val shortDuration = 100.millis() + 500.nanos()

// Period 생성
val period = 2.yearPeriod() + 6.monthPeriod() + 15.dayPeriod()
```

#### Duration 유틸리티

```kotlin
// Duration 생성
val d1 = durationOfDay(1, 2, 3, 4, 5)  // 1일 2시간 3분 4초 5나노초
val d2 = durationOfHour(2, 30, 15)     // 2시간 30분 15초

// 포맷팅
duration.formatHMS()   // "26:03:04.000"
duration.formatISO()   // "P0Y0M1DT2H3M4.000S"
```

#### Temporal 확장

```kotlin
val now = nowZonedDateTime()

// 시작 시각
now.startOfYear()     // 연초
now.startOfMonth()    // 월초
now.startOfDay()      // 당일 시작

// 현재 시각 생성
val instant = nowInstant()
val localDateTime = nowLocalDateTime()
val zonedDateTime = nowZonedDateTime()

// 특정 시각 생성
val date = localDateOf(2024, 10, 14)
val dateTime = localDateTimeOf(2024, 10, 14, 15, 30, 45)
```

#### Quarter (분기) 지원

```kotlin
val q1 = Quarter.Q1
val q2 = Quarter.of(2)
val q3 = Quarter.ofMonth(7)  // 7월 -> Q3

val yq = YearQuarter(2024, Quarter.Q1)
yq.addQuarters(2)  // 2024-Q3
```

## 참고 자료

- [Kotlin Contracts](https://kotlinlang.org/docs/whatsnew13.html#contracts)
- [Kotlin Ranges](https://kotlinlang.org/docs/ranges.html)
- [Apache Commons Codec](https://commons.apache.org/proper/commons-codec/)
- [Java Time API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/package-summary.html)
