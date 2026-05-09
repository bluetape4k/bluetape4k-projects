# bluetape4k-assertions

[English](README.md)

JUnit 5용 bluetape4k-assertions 호환 assertion DSL. api scope에 `bluetape4k-*` 의존성 없음 — import만 교체하면 마이그레이션 완료.

## 아키텍처

```mermaid
graph TD
    A["bluetape4k-assertions"] --> B["junit-jupiter-api"]
    A --> C["opentest4j"]
    A --> D["kotlinx-coroutines-core"]
    A -.->|compileOnly| E["turbine"]
    F["bluetape4k-junit5"] -->|api| A
    G["bluetape4k-coroutines"] -.->|@Deprecated bridge| A
    
    classDef api fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    classDef optional fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    classDef consumer fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    
    class A,B,C,D api
    class E optional
    class F,G consumer
```

## 기능

- **bluetape4k-assertions 호환** infix DSL — 같은 함수 이름, import만 교체하면 완료
- **기본**: `shouldBe` (ref ===), `shouldBeEqualTo` (value ==), `shouldNotBeNull` 스마트 캐스트 지원
- **숫자 비교**: `shouldBeLessThan`, `shouldBeGreaterThanOrEqualTo`, 부호 확인, 범위 포함 확인
- **컬렉션/배열/맵**: 내용 동등성, 포함 검증 (`shouldContainAll`, `shouldNotContainAny`)
- **문자열**: `shouldStartWith`, `shouldEndWith`, `shouldContain`, 대소문자 무시 검증
- **예외**: `invoking { }` / `shouldThrow`, 메시지 검증, 원인 검사
- **비동기 예외**: `coInvoking { }` / `shouldThrow` — CancellationException 안전 coroutine 지원
- **리플렉션**: `shouldBeInstanceOf<T>` 스마트 캐스트 지원
- **날짜시간**: `shouldBeAfter`, `shouldBeBefore`, `shouldBeOnOrAfter` (7가지 java.time 타입)
- **Softly**: `assertSoftly { add { } }` — 가상 스레드 안전, `MultipleFailuresError` 수집
- **Flow 검증**: `assertEmpty`, `assertResult`, `assertResultSet`, `assertFailure`, `assertError`
- **Turbine 통합**: `awaitItemAndAssert`, `awaitItemMatching`, `awaitErrorOfType` (선택사항, compileOnly)

## 빠른 시작

### Gradle 의존성

```kotlin
// build.gradle.kts
testImplementation(project(":bluetape4k-assertions"))
// 또는 bluetape4k-junit5를 통해 전이적 포함
testImplementation(project(":bluetape4k-junit5"))
```

### 사용 예시

```kotlin
import io.bluetape4k.assertions.*
import io.bluetape4k.junit5.BlueConf
import org.junit.jupiter.api.Test

class MyTest {
    @Test
    fun `기본 검증`() {
        // 기본
        "hello" shouldBeEqualTo "hello"
        "hello" shouldNotBeEqualTo "world"
        
        // shouldNotBeNull 후 스마트 캐스트
        val name: String? = "John"
        name.shouldNotBeNull().length shouldBeGreaterThan 0
        
        // 컬렉션
        listOf(1, 2, 3) shouldContainAll listOf(1, 2)
        listOf(1, 2, 3) shouldNotContainAny listOf(4, 5)
        
        // 문자열
        "hello".shouldStartWith("he")
        "hello".shouldEndWith("lo")
        
        // 숫자 비교
        5 shouldBeLessThan 10
        5 shouldBeGreaterThanOrEqualTo 5
        5.0 shouldBeNear 5.1 tolerance 0.2
        
        // 예외
        invoking { error("boom") } shouldThrow IllegalStateException::class
        coInvoking { delay(100) } shouldNotThrow
        
        // 리플렉션
        listOf(1, 2, 3).shouldBeInstanceOf<List<*>>()
        
        // 날짜시간
        val now = LocalDateTime.now()
        now.shouldBeAfter(now.minusSeconds(1))
        now.shouldBeOnOrBefore(now.plusSeconds(1))
    }
    
    @Test
    fun `softly 검증`() {
        assertSoftly {
            add { 1 shouldBeEqualTo 1 }
            add { "a" shouldStartWith "a" }
            add { listOf(1) shouldContainAll listOf(1) }
        }
        // 모든 검증 수집, 실패 시 MultipleFailuresError 발생
    }
    
    @Test
    fun `flow 검증`() {
        flowOf(1, 2, 3).assertResult(1, 2, 3)
        flowOf(1, 2).assertNotEmpty()
        flow<Int> { }.assertEmpty()
    }
}
```

## API 레퍼런스

### 기본 검증

| 함수 | 설명 |
|------|------|
| `shouldBe(expected)` | 참조 동일성 (===) |
| `shouldNotBe(expected)` | 참조 다름 (!==) |
| `shouldBeEqualTo(expected)` | 값 동등성 (==) |
| `shouldNotBeEqualTo(expected)` | 값 다름 (!=) |
| `shouldBeNull()` | null 확인 |
| `shouldNotBeNull()` | null 아님 확인 (스마트 캐스트) |

### 숫자 비교

| 함수 | 설명 |
|------|------|
| `shouldBeLessThan(bound)` | < |
| `shouldBeLessThanOrEqualTo(bound)` | <= |
| `shouldBeGreaterThan(bound)` | > |
| `shouldBeGreaterThanOrEqualTo(bound)` | >= |
| `shouldBePositive()` | > 0 |
| `shouldBeNegative()` | < 0 |
| `shouldBeNear(expected, tolerance)` | 부동소수점 근사 동등 |
| `BigDecimal shouldBeEqualTo expected` | scale 무관 동등 (`compareTo`) |
| `BigDecimal shouldNotBeEqualTo expected` | scale 무관 비동등 (`compareTo`) |

### 컬렉션 & 배열

| 함수 | 설명 |
|------|------|
| `shouldBeEmpty()` | 빈 컬렉션 |
| `shouldNotBeEmpty()` | 비어있지 않음 |
| `shouldContainAll(elements)` | 모든 요소 포함 (⊇) |
| `shouldNotContainAny(elements)` | 어떤 요소도 미포함 (∩ = ∅) |
| `shouldHaveSize(size)` | 크기 확인 |
| `shouldContain(element)` | 요소 포함 |
| `IntArray shouldBeEqualTo expected` | primitive 배열 내용 동등 (`contentEquals`) |
| `ByteArray shouldBeEqualTo expected` | primitive 배열 내용 동등 (`contentEquals`) |
| `Array<T> shouldBeEqualTo expected` | 객체 배열 deep 내용 동등 (`contentDeepEquals`) |

### 예외

| 함수 | 설명 |
|------|------|
| `invoking { }.shouldThrow<E>()` | 동기 블록이 E 예외 발생 |
| `invoking { }.shouldNotThrow()` | 동기 블록이 예외 미발생 |
| `coInvoking { }.shouldThrow<E>()` | 비동기 블록이 E 예외 발생 |
| `.withMessage(msg)` | 정확한 메시지 일치 (체이닝) |
| `.withMessageContaining(substring)` | 메시지 부분 포함 (체이닝) |

### 리플렉션

| 함수 | 설명 |
|------|------|
| `shouldBeInstanceOf<T>()` | 인스턴스 확인 (스마트 캐스트) |
| `shouldNotBeInstanceOf<T>()` | 인스턴스 아님 확인 |

## bluetape4k-assertions에서 마이그레이션

`import io.bluetape4k.assertions.*`을 `import io.bluetape4k.assertions.*`으로 교체하세요.

| bluetape4k-assertions | bluetape4k-assertions | 주의사항 |
|--------|----------------------|---------|
| `shouldBe` (value ==) | `shouldBeEqualTo` | bluetape4k에서는 의미가 다름 |
| `shouldBe` (ref ===) | `shouldBe` | 동일한 동작 |
| `shouldNotBeNull()` | `shouldNotBeNull()` | + 스마트 캐스트 지원 |
| `shouldThrow<E>()` | `invoking { }.shouldThrow(E::class)` | 명시적 블록 래퍼 |
| `shouldHaveMessage()` | `.withMessage()` | InvokingBlock에서 체이닝 |
| `coInvoking { }` | `coInvoking { }` | 완전한 coroutine 지원 |

### 중요한 의미 변화

**bluetape4k-assertions의 `shouldBe`는 모든 타입에 대해 `==` (값 동등성)를 사용합니다.**
**bluetape4k-assertions의 `shouldBe`는 `===` (참조 동일성)를 사용합니다.**

이는 의도적인 설계로, 값 동등성과 참조 동일성 모두를 명확하게 제공하기 위함입니다. bluetape4k-assertions의 `shouldBe`를 마이그레이션할 때는 값 동등성 검증에 항상 `shouldBeEqualTo`를 사용하세요.

```kotlin
// bluetape4k-assertions
"a" shouldBe "a"  // 통과 (== 비교)

// bluetape4k-assertions
"a" shouldBe "a"  // 실패! (=== 비교)
"a" shouldBeEqualTo "a"  // 통과 (== 비교)
```

---

**유지보수자**: Sunghyouk Bae (@sunghyouk.bae@gmail.com)
**라이선스**: Apache 2.0
