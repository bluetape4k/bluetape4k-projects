# bluetape4k-rule-engine

[English](./README.md) | 한국어

Kotlin 기반의 경량 Rule Engine 라이브러리입니다. Easy Rules 패턴을 기반으로 하되, Kotlin DSL, 코루틴(SuspendRule), 어노테이션 기반 Rule 정의를 지원합니다.

## 아키텍처

### 개념 개요

세 가지 핵심 구성 요소와 상호 작용:

```mermaid
flowchart LR
    subgraph Facts["Facts (공유 상태)"]
        KV["age: 20\namount: 1500\n..."]
    end

    subgraph RS["RuleSet (우선순위 정렬)"]
        R1["Rule 1\ncondition → action"]
        R2["Rule 2\ncondition → action"]
        R3["Rule 3\ncondition → action"]
    end

    User -->|" 1. 생성 "| Facts
    User -->|" 2. 빌드 "| RS
    User -->|" 3. fire(ruleSet, facts) "| RE[RuleEngine]
    RE -->|" evaluate(facts) per rule "| RS
    RS -->|" true → execute(facts) "| Facts
    RE -.->|" 4. 결과 읽기 "| Facts

    classDef coreStyle fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32,font-weight:bold
    classDef serviceStyle fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    classDef dataStyle fill:#F57F17,stroke:#F57F17,color:#000000
    classDef dslStyle fill:#E0F7FA,stroke:#80DEEA,color:#00695C

    class RE coreStyle
    class R1,R2,R3 dslStyle
    class KV dataStyle
```

`Rule`은 **condition** (`Facts` 검사 Predicate)과 **action** (`Facts` 수정 함수)으로 구성됩니다.  
`RuleEngine.fire()`는 우선순위 순으로 Rule을 순회하며 조건을 평가하고, 만족하는 Rule의 Action을 실행합니다.

### 핵심 클래스 다이어그램

```mermaid
classDiagram
    class Rule {
        <<interface>>
        +name: String
        +description: String
        +priority: Int
        +evaluate(facts: Facts): Boolean
        +execute(facts: Facts)
        +compareTo(other: Rule): Int
    }

    class SuspendRule {
        <<interface>>
        +name: String
        +description: String
        +priority: Int
        +evaluate(facts: Facts): Boolean
        +execute(facts: Facts)
        +compareTo(other: SuspendRule): Int
    }

    class AbstractRule {
        <<abstract>>
        +name: String
        +description: String
        +priority: Int
        +equals(other: Any?): Boolean
        +hashCode(): Int
    }

    class DefaultRule {
        -condition: Condition
        -actions: List~Action~
        +evaluate(facts: Facts): Boolean
        +execute(facts: Facts)
    }

    class DefaultSuspendRule {
        -condition: SuspendCondition
        -actions: List~SuspendAction~
        +evaluate(facts: Facts): Boolean
        +execute(facts: Facts)
    }

    class Condition {
        <<fun interface>>
        +evaluate(facts: Facts): Boolean
        TRUE$
        FALSE$
    }

    class Action {
        <<fun interface>>
        +execute(facts: Facts)
    }

    class Facts {
        -facts: ConcurrentHashMap~String, Any?~
        +get(name, type): T?
        +set(name, value)
        +contains(name): Boolean
        +remove(name): Any?
        +of(vararg pairs)$
    }

    class RuleSet {
        -rules: TreeSet~Rule~
        +size: Int
        +add(rule: Rule): Boolean
        +iterator(): Iterator~Rule~
    }

    Rule <|.. AbstractRule
    AbstractRule <|-- DefaultRule
    DefaultRule ..> Condition : uses
    DefaultRule ..> Action : uses
    SuspendRule <|.. DefaultSuspendRule
    RuleSet o-- Rule : contains

    style Rule fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style SuspendRule fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    style AbstractRule fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style DefaultRule fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style DefaultSuspendRule fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    style Condition fill:#FFFDE7,stroke:#FFF176,color:#F57F17
    style Action fill:#FFFDE7,stroke:#FFF176,color:#F57F17
    style Facts fill:#FFF3E0,stroke:#FFCC80,color:#E65100
    style RuleSet fill:#E0F7FA,stroke:#80DEEA,color:#00695C
```

### Rule Engine 클래스 다이어그램

```mermaid
classDiagram
    class RuleEngine {
        <<interface>>
        +config: RuleEngineConfig
        +check(rules: RuleSet, facts: Facts): Map~Rule, Boolean~
        +fire(rules: RuleSet, facts: Facts)
    }

    class DefaultRuleEngine {
        -ruleListeners: CopyOnWriteArrayList~RuleListener~
        -engineListeners: CopyOnWriteArrayList~RuleEngineListener~
        +fire(rules, facts)
        +addRuleListener(listener)
        +addRuleEngineListener(listener)
    }

    class DefaultSuspendRuleEngine {
        +fire(rules, facts)
    }

    class InferenceRuleEngine {
        +fire(rules: RuleSet, facts: Facts)
    }

    class RuleEngineConfig {
        +skipOnFirstAppliedRule: Boolean
        +skipOnFirstFailedRule: Boolean
        +skipOnFirstNonTriggeredRule: Boolean
        +priorityThreshold: Int
        DEFAULT$
    }

    class RuleListener {
        <<interface>>
        +beforeEvaluate(rule, facts): Boolean
        +onSuccess(rule, facts)
        +onFailure(rule, facts, ex)
    }

    RuleEngine <|.. DefaultRuleEngine
    RuleEngine <|.. InferenceRuleEngine
    DefaultRuleEngine o-- RuleEngineConfig
    DefaultRuleEngine o-- RuleListener : listeners

    style RuleEngine fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style DefaultRuleEngine fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style DefaultSuspendRuleEngine fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    style InferenceRuleEngine fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style RuleEngineConfig fill:#FFFDE7,stroke:#FFF176,color:#F57F17
    style RuleListener fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
```

### Composite Rule 다이어그램

```mermaid
classDiagram
    class CompositeRule {
        <<abstract>>
        #rules: SortedSet~Rule~
        +addRule(rule: Rule)
        +removeRule(rule: Rule)
    }

    class ActivationRuleGroup {
    }
    note for ActivationRuleGroup "조건 만족하는 Rule 중\n우선순위 가장 높은 것만 실행"

    class ConditionalRuleGroup {
    }
    note for ConditionalRuleGroup "최고 우선순위 Rule 조건 충족 시\n나머지 모든 Rule 실행"

    class UnitRuleGroup {
    }
    note for UnitRuleGroup "모든 Rule 조건 충족 시\n전체 실행 (원자적 단위)"

    CompositeRule <|-- ActivationRuleGroup
    CompositeRule <|-- ConditionalRuleGroup
    CompositeRule <|-- UnitRuleGroup
    CompositeRule o-- Rule : contains

    style CompositeRule fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style ActivationRuleGroup fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style ConditionalRuleGroup fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style UnitRuleGroup fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style Rule fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
```

### Rule 실행 시퀀스

```mermaid
sequenceDiagram
    box "소비자" #E8F5E9
    participant Caller
    end
    box "Rule Engine" #E3F2FD
    participant RuleEngine
    participant RuleListener
    end
    box "Rules" #FFF3E0
    participant Rule
    participant Facts
    end
    Caller ->> RuleEngine: fire(ruleSet, facts)

    loop rules (우선순위 순)
        RuleEngine ->> RuleListener: beforeEvaluate(rule, facts)
        RuleEngine ->> Rule: evaluate(facts)
        Rule -->> RuleEngine: true / false

        alt condition = true
            RuleEngine ->> Rule: execute(facts)
            Rule ->> Facts: 값 수정
            RuleEngine ->> RuleListener: onSuccess(rule, facts)
            Note over RuleEngine: skipOnFirstAppliedRule → 중단
        else condition = false
            RuleEngine ->> RuleListener: onFailure(rule, facts)
            Note over RuleEngine: skipOnFirstNonTriggeredRule → 중단
        end
    end

    RuleEngine -->> Caller: (완료)
```

### InferenceRuleEngine (Forward Chaining)

```mermaid
flowchart TD
    A[fire 시작] --> B[모든 Rule 평가]
    B --> C{조건 만족하는\nRule 있음?}
    C -->|yes| D[가장 높은 우선순위 Rule 실행]
    D --> E[Facts 업데이트]
    E --> B
    C -->|no| F[종료]

    classDef coreStyle fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32,font-weight:bold
    classDef serviceStyle fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    classDef dataStyle fill:#F57F17,stroke:#F57F17,color:#000000

    class A,B coreStyle
    class D,E serviceStyle
    class F dataStyle
```

### Rule Engine 선택 가이드

```mermaid
flowchart TD
    A[Rule Engine 선택] --> B{비동기 처리 필요?}
    B -->|yes| C[DefaultSuspendRuleEngine]
    B -->|no| D{Forward Chaining?}
    D -->|yes| E[InferenceRuleEngine]
    D -->|no| F[DefaultRuleEngine]
    C & E & F --> G{Rule 정의 방식}
    G --> H["DSL: rule{}"]
    G --> I["어노테이션: @Rule"]
    G --> J["스크립트: MVEL2 / SpEL / Janino / Groovy"]
    G --> K["파일: YAML / JSON / HOCON"]

    classDef coreStyle fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32,font-weight:bold
    classDef serviceStyle fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    classDef asyncStyle fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    classDef dslStyle fill:#E0F7FA,stroke:#80DEEA,color:#00695C
    classDef dataStyle fill:#F57F17,stroke:#F57F17,color:#000000

    class A coreStyle
    class E,F serviceStyle
    class C asyncStyle
    class H,I dslStyle
    class J,K dataStyle
```

## 핵심 기능

- **DSL 기반 Rule 정의**: `rule {}`, `suspendRule {}`, `ruleEngine {}` DSL
- **어노테이션 기반 Rule**: `@Rule`, `@Condition`, `@Action`, `@Fact` 어노테이션으로 POJO 클래스를 Rule로 변환
- **코루틴 지원**: `SuspendRule`, `SuspendRuleEngine`으로 비동기 Rule 실행
- **Cancellation 인지 suspend 엔진**: `DefaultSuspendRuleEngine`은 `CancellationException`을 일반 Rule 실패로 삼키지 않고 다시 던집니다
- **스크립트 엔진**: MVEL2, SpEL, Kotlin Script, Janino, Groovy 기반 동적 Rule 정의
- **Rule Reader**: YAML, JSON, HOCON 포맷으로 외부 파일에서 Rule 정의 로딩
- **Composite Rule**: `ActivationRuleGroup`, `ConditionalRuleGroup`, `UnitRuleGroup`으로 복합 Rule 조합
- **Forward Chaining**: `InferenceRuleEngine`으로 조건 만족 시 반복 실행

## 사용 예시

### DSL 기반 Rule

```kotlin
val discountRule = rule {
    name = "discount"
    description = "1000원 이상 구매 시 할인 적용"
    priority = 1
    condition { facts -> facts.get<Int>("amount")!! > 1000 }
    action { facts -> facts["discount"] = true }
}

val engine = ruleEngine { skipOnFirstAppliedRule = true }
val facts = Facts.of("amount" to 1500)
engine.fire(ruleSetOf(discountRule), facts)
```

### 어노테이션 기반 Rule

```kotlin
@Rule(name = "ageCheck", description = "성인 확인", priority = 1)
class AgeCheckRule {
    @Condition
    fun isAdult(facts: Facts): Boolean = facts.get<Int>("age")!! >= 18

    @Action
    fun allow(facts: Facts) {
        facts["allowed"] = true
    }
}

val rule = AgeCheckRule().asRule()
val facts = Facts.of("age" to 20)
DefaultRuleEngine().fire(ruleSetOf(rule), facts)
```

### 코루틴 기반 SuspendRule

```kotlin
val asyncRule = suspendRule {
    name = "asyncProcess"
    condition { facts -> facts.get<Int>("value")!! > 0 }
    action { facts ->
        delay(100)
        facts["processed"] = true
    }
}

val engine = DefaultSuspendRuleEngine()
engine.fire(suspendRuleSetOf(asyncRule), facts)
```

### MVEL2 스크립트 Rule

```kotlin
val rule = MvelRule(name = "discount", priority = 1)
    .whenever("amount > 1000")
    .then("discount = true")
```

### SpEL 스크립트 Rule

```kotlin
val rule = SpelRule(name = "discount", priority = 1)
    .whenever("#amount > 1000")
    .then("#discount = true")
```

### Janino 스크립트 Rule (바이트코드 컴파일 Java)

Janino는 런타임에 Java 표현식을 바이트코드로 컴파일하여 네이티브에 가까운 속도로 실행합니다.
대량의 룰 반복 평가(가격 계산, 유효성 검증, 할인 정책)에 최적입니다.

```kotlin
val rule = JaninoRule(name = "discount", priority = 1)
    .whenever("((Integer)facts.get(\"amount\")).intValue() > 1000")
    .then("facts.put(\"discount\", Boolean.TRUE);")
```

**Janino 작성 시 주의사항:**

- **Condition은 순수 표현식만 지원**: `ExpressionEvaluator` 기반이므로 변수 선언(`int x = ...`)이 불가합니다.
  복잡한 조건은 인라인으로 작성하세요.
  ```java
  // ✅ 올바른 Condition
  "((Integer)facts.get(\"age\")).intValue() >= 18 && ((Integer)facts.get(\"age\")).intValue() <= 65"
  
  // ❌ 컴파일 오류 — 변수 선언은 표현식이 아님
  "int age = ((Integer)facts.get(\"age\")).intValue(); age >= 18 && age <= 65"
  ```
- **Action은 문장(statement) 블록 지원**: `ScriptEvaluator` 기반이므로 변수 선언, if-else, for/while 루프 모두 가능합니다.
- **명시적 타입 캐스팅 필수**: `facts`는 `Map<String, Object>` 타입이므로 `facts.get()` 결과를 반드시 캐스팅해야 합니다.
- **복잡한 조건 로직이 필요하면 Groovy를 추천합니다** — Groovy는 변수 직접 접근, 범위 연산자(`in 18..65`), 클로저를 지원합니다.

### Groovy 스크립트 Rule

Groovy는 동적 타이핑, 클로저, Java 호환 문법을 제공합니다.
복잡한 룰 로직을 표현력 높은 문법으로 작성할 수 있습니다.

```kotlin
val rule = GroovyRule(name = "discount", priority = 1)
    .whenever("amount > 1000")
    .then("discount = true")

// Groovy는 클로저와 풍부한 표현식 지원
val tierRule = GroovyRule(name = "tier")
    .whenever("amount > 0")
    .then("tier = amount > 5000 ? 'gold' : amount > 2000 ? 'silver' : 'bronze'")
```

**Groovy 편의 기능:**

- **Null 안전 바인딩**: `NullSafeBinding`을 사용하여 Facts에 없는 키를 참조하면 예외 대신 `null`을 반환합니다.
  Elvis 연산자와 safe navigation이 자연스럽게 동작합니다.
  ```groovy
  // Facts에 'name' 키가 없어도 MissingPropertyException 발생하지 않음
  displayName = name ?: 'Guest'         // Elvis — null이면 기본값
  upper = name?.toUpperCase()           // safe navigation — null이면 null
  ```
- **GString 자동 변환**: Groovy 문자열 보간(`"Hello, ${name}!"`) 결과는 `GString` 타입인데,
  Facts에 반영 시 자동으로 `String`으로 변환되므로 `facts.get<String>()`이 안전합니다.
- **변수 직접 접근**: Facts의 키가 Groovy 변수로 바인딩되어 `facts.get("amount")` 대신 `amount`로 바로 접근합니다.
- **새 변수 자동 반영**: 스크립트에서 대입한 변수(`discount = true`)는 자동으로 Facts에 저장됩니다.

### 스크립트 엔진 비교

| 엔진 | 언어 | 컴파일 방식 | 표현식 문법 | 적합한 용도 |
|------|------|-----------|----------|-----------|
| MVEL2 | MVEL | 하이브리드 (인터프리터 + 바이트코드) | `amount > 1000` | 간단한 동적 표현식 |
| SpEL | Spring EL | 하이브리드 (컴파일 옵션) | `#amount > 1000` | Spring 생태계 통합 |
| Janino | Java 서브셋 | **바이트코드** (네이티브 속도) | `((Integer)facts.get("amount")).intValue() > 1000` | 고빈도 반복 평가, 단순 조건 |
| Groovy | Groovy | **바이트코드** | `amount > 1000` | 클로저/컬렉션 활용 복잡한 로직 |
| Kotlin Script | Kotlin | 바이트코드 (콜드스타트 느림) | 전체 Kotlin 문법 | 타입 안전 Kotlin 표현식 |

### 스크립트 엔진 선택 가이드

```mermaid
flowchart TD
    A[스크립트 엔진 선택] --> B{표현식 복잡도?}
    B -->|"단순 (변수 비교, 대입)"| C{Spring 프로젝트?}
    C -->|yes| D[SpEL]
    C -->|no| E{성능 최우선?}
    E -->|yes| F[Janino]
    E -->|no| G[MVEL2]
    B -->|"복잡 (컬렉션, 클로저, 분기)"| H[Groovy]
    B -->|"Kotlin 타입 안전 필요"| I[Kotlin Script]

    classDef coreStyle fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32,font-weight:bold
    classDef serviceStyle fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    classDef extStyle fill:#ECEFF1,stroke:#B0BEC5,color:#37474F

    class A coreStyle
    class D,F,G serviceStyle
    class H,I extStyle
```

| 시나리오 | 추천 엔진 | 이유 |
|---------|---------|------|
| 가격 비교, 임계값 체크 | Janino | 바이트코드 컴파일, 최고 성능 |
| Spring 컨텍스트 내 빈 참조 | SpEL | `#bean.method()` 직접 호출 |
| 할인 정책, 등급 분류 | MVEL2 / Groovy | 간결한 문법 |
| 컬렉션 필터/변환, 복잡한 분기 | Groovy | `collect`, `findAll`, `switch-range`, 클로저 |
| optional 필드 처리 | Groovy | `NullSafeBinding` + Elvis/safe navigation |
| 타입 안전 표현식 | Kotlin Script | 전체 Kotlin 문법 (콜드스타트 느림) |

### YAML에서 Rule 로딩

```yaml
# rules.yml
rules:
    -   name: "discount"
        condition: "amount > 1000"
        actions:
            - "discount = true"
```

```kotlin
val reader = YamlRuleReader()
val definitions = reader.readAll(source).toList()
val mvelRules = definitions.map { it.toMvelRule() }
```

## 설정 옵션

| 옵션                            | 설명                   | 기본값             |
|-------------------------------|----------------------|-----------------|
| `skipOnFirstAppliedRule`      | 첫 번째 성공 Rule 이후 중단   | `false`         |
| `skipOnFirstFailedRule`       | 첫 번째 실패 Rule 이후 중단   | `false`         |
| `skipOnFirstNonTriggeredRule` | 첫 번째 미트리거 Rule 이후 중단 | `false`         |
| `priorityThreshold`           | 이 값 초과 우선순위 Rule 무시  | `Int.MAX_VALUE` |

## 의존성

```kotlin
implementation(project(":bluetape4k-rule-engine"))

// optional (compileOnly)
implementation("org.mvel:mvel2:2.5.2.Final")              // MVEL2 엔진
implementation("org.codehaus.janino:janino:3.1.12")        // Janino 엔진
implementation("org.apache.groovy:groovy:4.0.27")          // Groovy 엔진
implementation("org.springframework:spring-expression")     // SpEL 엔진
implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host") // Kotlin Script 엔진
implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml") // YAML 리더
implementation("com.typesafe:config:1.4.3")                // HOCON 리더
```
