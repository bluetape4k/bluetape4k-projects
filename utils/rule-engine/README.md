# bluetape4k-rule-engine

Kotlin 기반의 경량 Rule Engine 라이브러리입니다. Easy Rules 패턴을 기반으로 하되, Kotlin DSL, 코루틴(SuspendRule), 어노테이션 기반 Rule 정의를 지원합니다.

## 핵심 기능

- **DSL 기반 Rule 정의**: `rule {}`, `suspendRule {}`, `ruleEngine {}` DSL
- **어노테이션 기반 Rule**: `@Rule`, `@Condition`, `@Action`, `@Fact` 어노테이션으로 POJO 클래스를 Rule로 변환
- **코루틴 지원**: `SuspendRule`, `SuspendRuleEngine`으로 비동기 Rule 실행
- **스크립트 엔진**: MVEL2, SpEL, Kotlin Script 기반 동적 Rule 정의
- **Rule Reader**: YAML, JSON, HOCON 포맷으로 외부 파일에서 Rule 정의 로딩
- **Composite Rule**: `ActivationRuleGroup`, `ConditionalRuleGroup`, `UnitRuleGroup`으로 복합 Rule 조합
- **Forward Chaining**: `InferenceRuleEngine`으로 조건 만족 시 반복 실행

## 의존성

```kotlin
implementation(project(":bluetape4k-rule-engine"))

// 선택적 (compileOnly)
implementation("org.mvel:mvel2:2.5.2.Final")              // MVEL2 엔진
implementation("org.springframework:spring-expression")     // SpEL 엔진
implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host") // Kotlin Script 엔진
implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml") // YAML Reader
implementation("com.typesafe:config:1.4.3")                // HOCON Reader
```

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

// facts.get<Boolean>("discount") == true
```

### 어노테이션 기반 Rule

```kotlin
@Rule(name = "ageCheck", description = "성인 확인", priority = 1)
class AgeCheckRule {
    @Condition
    fun isAdult(facts: Facts): Boolean = facts.get<Int>("age")!! >= 18

    @Action
    fun allow(facts: Facts) { facts["allowed"] = true }
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
        delay(100) // 비동기 작업
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

### YAML에서 Rule 로딩

```yaml
# rules.yml
rules:
  - name: "discount"
    condition: "amount > 1000"
    actions:
      - "discount = true"
```

```kotlin
val reader = YamlRuleReader()
val definitions = reader.readAll(source).toList()
val mvelRules = definitions.map { it.toMvelRule() }
```

## 아키텍처

### 전체 패키지 구조

```mermaid
graph TD
    M[bluetape4k-rule-engine] --> API[api/]
    M --> ANN[annotation/]
    M --> CORE[core/]
    M --> SUP[support/]
    M --> ENG[engines/]
    M --> READ[readers/]
    M --> EX[exception/]

    API --> R[Rule.kt]
    API --> SR[SuspendRule.kt]
    API --> F[Facts.kt]
    API --> RS[RuleSet.kt]
    API --> SRS[SuspendRuleSet.kt]
    API --> REC[RuleEngineConfig.kt]
    API --> RE[RuleEngine.kt]
    API --> SRE[SuspendRuleEngine.kt]

    ANN --> AR["@Rule"]
    ANN --> AC["@Condition"]
    ANN --> AA["@Action"]
    ANN --> AF["@Fact"]
    ANN --> AP["@Priority"]

    CORE --> DR[DefaultRule.kt]
    CORE --> DSR[DefaultSuspendRule.kt]
    CORE --> DRE[DefaultRuleEngine.kt]
    CORE --> DSRE[DefaultSuspendRuleEngine.kt]
    CORE --> IRE[InferenceRuleEngine.kt]
    CORE --> RP[RuleProxy.kt]
    CORE --> DSL[RuleDsl.kt]

    SUP --> ACT[ActivationRuleGroup.kt]
    SUP --> COND[ConditionalRuleGroup.kt]
    SUP --> UNIT[UnitRuleGroup.kt]

    ENG --> MV[mvel2/]
    ENG --> SPE[spel/]
    ENG --> KS[kotlinscript/]

    READ --> YR[YamlRuleReader.kt]
    READ --> JR[JsonRuleReader.kt]
    READ --> HR[HoconRuleReader.kt]
```

---

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

    class SuspendCondition {
        <<fun interface>>
        +evaluate(facts: Facts): Boolean
        TRUE$
        FALSE$
    }

    class Action {
        <<fun interface>>
        +execute(facts: Facts)
    }

    class SuspendAction {
        <<fun interface>>
        +execute(facts: Facts)
    }

    class Facts {
        -facts: ConcurrentHashMap~String, Any?~
        +get(name, type): T?
        +set(name, value)
        +contains(name): Boolean
        +remove(name): Any?
        +asMap(): Map~String, Any?~
        +empty()$
        +of(vararg pairs)$
        +from(map)$
    }

    class RuleSet {
        -rules: TreeSet~Rule~
        +size: Int
        +isEmpty(): Boolean
        +add(rule: Rule): Boolean
        +iterator(): Iterator~Rule~
    }

    Rule <|.. AbstractRule
    AbstractRule <|-- DefaultRule
    DefaultRule ..> Condition : uses
    DefaultRule ..> Action : uses
    SuspendRule <|.. DefaultSuspendRule
    DefaultSuspendRule ..> SuspendCondition : uses
    DefaultSuspendRule ..> SuspendAction : uses
    RuleSet o-- Rule : contains
```

---

### Rule Engine 클래스 다이어그램

```mermaid
classDiagram
    class RuleEngine {
        <<interface>>
        +config: RuleEngineConfig
        +check(rules: RuleSet, facts: Facts): Map~Rule, Boolean~
        +fire(rules: RuleSet, facts: Facts)
    }

    class SuspendRuleEngine {
        <<interface>>
        +config: RuleEngineConfig
        +check(rules: Iterable~SuspendRule~, facts: Facts): Map~SuspendRule, Boolean~
        +fire(rules: Iterable~SuspendRule~, facts: Facts)
    }

    class DefaultRuleEngine {
        -ruleListeners: CopyOnWriteArrayList~RuleListener~
        -engineListeners: CopyOnWriteArrayList~RuleEngineListener~
        +check(rules, facts): Map~Rule, Boolean~
        +fire(rules, facts)
        +addRuleListener(listener)
        +addRuleEngineListener(listener)
    }

    class DefaultSuspendRuleEngine {
        +check(rules, facts): Map~SuspendRule, Boolean~
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

    class RuleEngineListener {
        <<interface>>
        +beforeFire(rules, facts)
        +afterFire(rules, facts)
    }

    RuleEngine <|.. DefaultRuleEngine
    RuleEngine <|.. InferenceRuleEngine
    SuspendRuleEngine <|.. DefaultSuspendRuleEngine
    DefaultRuleEngine o-- RuleEngineConfig
    DefaultRuleEngine o-- RuleListener : listeners
    DefaultRuleEngine o-- RuleEngineListener : listeners
```

---

### Composite Rule 다이어그램

```mermaid
classDiagram
    class Rule {
        <<interface>>
        +evaluate(facts): Boolean
        +execute(facts)
    }

    class CompositeRule {
        <<abstract>>
        #rules: SortedSet~Rule~
        +addRule(rule: Rule)
        +removeRule(rule: Rule)
    }

    class ActivationRuleGroup {
        +evaluate(facts): Boolean
        +execute(facts)
    }
    note for ActivationRuleGroup "조건 만족하는 Rule 중\n우선순위 가장 높은 것만 실행"

    class ConditionalRuleGroup {
        +evaluate(facts): Boolean
        +execute(facts)
    }
    note for ConditionalRuleGroup "최고 우선순위 Rule 조건 충족 시\n나머지 모든 Rule 실행"

    class UnitRuleGroup {
        +evaluate(facts): Boolean
        +execute(facts)
    }
    note for UnitRuleGroup "모든 Rule 조건 충족 시\n전체 실행 (원자적 단위)"

    Rule <|.. CompositeRule
    CompositeRule <|-- ActivationRuleGroup
    CompositeRule <|-- ConditionalRuleGroup
    CompositeRule <|-- UnitRuleGroup
    CompositeRule o-- Rule : contains
```

---

### Expression Engine 클래스 다이어그램

```mermaid
classDiagram
    class Condition {
        <<fun interface>>
        +evaluate(facts: Facts): Boolean
    }

    class Action {
        <<fun interface>>
        +execute(facts: Facts)
    }

    class MvelCondition {
        -expression: String
        -compiled: Serializable
        +evaluate(facts: Facts): Boolean
    }

    class MvelAction {
        -expression: String
        -compiled: Serializable
        +execute(facts: Facts)
    }

    class MvelRule {
        -conditionExpression: String
        -actionExpressions: List~String~
        +evaluate(facts: Facts): Boolean
        +execute(facts: Facts)
    }

    class SpelCondition {
        -expression: String
        -compiledExpr: Expression
        +evaluate(facts: Facts): Boolean
    }

    class SpelAction {
        -expression: String
        -compiledExpr: Expression
        +execute(facts: Facts)
    }

    class SpelRule {
        -conditionExpression: String
        -actionExpressions: List~String~
    }

    class KotlinScriptCondition {
        -script: String
        +evaluate(facts: Facts): Boolean
    }

    class KotlinScriptEngine {
        <<object>>
        -host: BasicJvmScriptingHost
        -compilationCache: ConcurrentHashMap
        +execute(script, facts): T?
    }

    Condition <|.. MvelCondition
    Action <|.. MvelAction
    Condition <|.. SpelCondition
    Action <|.. SpelAction
    Condition <|.. KotlinScriptCondition
    KotlinScriptCondition ..> KotlinScriptEngine : uses
    MvelRule ..> MvelCondition : uses
    MvelRule ..> MvelAction : uses
    SpelRule ..> SpelCondition : uses
    SpelRule ..> SpelAction : uses
```

---

## Rule 실행 흐름

### DefaultRuleEngine.fire() 시퀀스

```mermaid
sequenceDiagram
    participant Caller
    participant DefaultRuleEngine
    participant RuleEngineListener
    participant RuleListener
    participant Rule
    participant Facts

    Caller->>DefaultRuleEngine: fire(rules, facts)
    DefaultRuleEngine->>RuleEngineListener: beforeFire(rules, facts)

    loop rules (우선순위 순)
        DefaultRuleEngine->>DefaultRuleEngine: rule.priority > threshold?
        alt 임계값 초과
            DefaultRuleEngine-->>DefaultRuleEngine: 중단
        end

        DefaultRuleEngine->>RuleListener: beforeEvaluate(rule, facts)
        alt listener returns false
            DefaultRuleEngine-->>DefaultRuleEngine: skip this rule
        end

        DefaultRuleEngine->>Rule: evaluate(facts)
        Rule-->>DefaultRuleEngine: Boolean

        alt condition = true
            DefaultRuleEngine->>RuleListener: onSuccess(rule, facts)
            DefaultRuleEngine->>Rule: execute(facts)
            Rule->>Facts: modify facts
            alt skipOnFirstAppliedRule
                DefaultRuleEngine-->>DefaultRuleEngine: 중단
            end
        else condition = false
            DefaultRuleEngine->>RuleListener: onFailure(rule, facts)
            alt skipOnFirstNonTriggeredRule
                DefaultRuleEngine-->>DefaultRuleEngine: 중단
            end
        end
    end

    DefaultRuleEngine->>RuleEngineListener: afterFire(rules, facts)
    DefaultRuleEngine-->>Caller: (완료)
```

### InferenceRuleEngine (Forward Chaining) 흐름

```mermaid
flowchart TD
    A[fire 시작] --> B[모든 Rule 평가]
    B --> C{조건 만족하는\nRule 있음?}
    C -- Yes --> D[가장 높은 우선순위 Rule 실행]
    D --> E[Facts 업데이트]
    E --> B
    C -- No --> F[종료]
```

### 어노테이션 → Rule 변환 (RuleProxy)

```mermaid
sequenceDiagram
    participant User
    participant RuleProxy
    participant MethodCache
    participant AnnotatedObject

    User->>RuleProxy: annotatedObj.asRule()
    RuleProxy->>RuleProxy: RuleDefinitionValidator.validate(obj)
    Note over RuleProxy: @Rule, @Condition, @Action 어노테이션 검증

    RuleProxy->>MethodCache: computeIfAbsent(obj::class)
    MethodCache-->>RuleProxy: Map~String, Method~
    Note over MethodCache: 메서드 목록 캐싱 (재사용)

    RuleProxy->>RuleProxy: Proxy.newProxyInstance(Rule::class)
    RuleProxy-->>User: Rule (Proxy)

    Note over User: Rule처럼 사용
    User->>RuleProxy: evaluate(facts)
    RuleProxy->>AnnotatedObject: @Condition 메서드 호출
    AnnotatedObject-->>RuleProxy: Boolean

    User->>RuleProxy: execute(facts)
    RuleProxy->>AnnotatedObject: @Action 메서드들 order 순 호출
```

---

## 설정 옵션

| 옵션 | 설명 | 기본값 |
|------|------|--------|
| `skipOnFirstAppliedRule` | 첫 번째 성공 Rule 이후 중단 | `false` |
| `skipOnFirstFailedRule` | 첫 번째 실패 Rule 이후 중단 | `false` |
| `skipOnFirstNonTriggeredRule` | 첫 번째 미트리거 Rule 이후 중단 | `false` |
| `priorityThreshold` | 이 값 초과 우선순위 Rule 무시 | `Int.MAX_VALUE` |

## Rule Engine 선택 가이드

```mermaid
flowchart TD
    A[Rule Engine 선택] --> B{비동기 처리 필요?}
    B -- Yes --> C[DefaultSuspendRuleEngine]
    B -- No --> D{Forward Chaining?}
    D -- Yes --> E[InferenceRuleEngine]
    D -- No --> F[DefaultRuleEngine]

    C --> G{Rule 정의 방식}
    F --> G
    E --> G

    G --> H[DSL: rule&#123;&#125;]
    G --> I["어노테이션: @Rule"]
    G --> J[스크립트: MVEL2 / SpEL]
    G --> K[외부 파일: YAML / JSON / HOCON]
```
