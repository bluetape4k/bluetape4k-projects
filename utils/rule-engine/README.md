# bluetape4k-rule-engine

English | [한국어](./README.ko.md)

A lightweight rule engine library for Kotlin. It follows the Easy Rules pattern and adds Kotlin DSLs, coroutine support (`SuspendRule`), and annotation-based rule definitions.

## Architecture

### Concept Overview

The three core building blocks and how they interact:

```mermaid
flowchart LR
    subgraph Facts["Facts (shared state)"]
        KV["age: 20\namount: 1500\n..."]
    end

    subgraph RS["RuleSet (sorted by priority)"]
        R1["Rule 1\ncondition → action"]
        R2["Rule 2\ncondition → action"]
        R3["Rule 3\ncondition → action"]
    end

    User -->|" 1. create "| Facts
    User -->|" 2. build "| RS
    User -->|" 3. fire(ruleSet, facts) "| RE[RuleEngine]
    RE -->|" evaluate(facts) per rule "| RS
    RS -->|" true → execute(facts) "| Facts
    RE -.->|" 4. read results "| Facts
```

A `Rule` has a **condition** (predicate on `Facts`) and an **action** (mutates `Facts`).  
`RuleEngine.fire()` iterates rules in priority order, evaluates each condition, and runs matching actions.

### Core Class Diagram

```mermaid
classDiagram
    class Rule {
        <<interface>>
        +name: String
        +priority: Int
        +evaluate(facts: Facts): Boolean
        +execute(facts: Facts)
    }

    class SuspendRule {
        <<interface>>
        +name: String
        +priority: Int
        +evaluate(facts: Facts): Boolean
        +execute(facts: Facts)
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
<<funinterface>>
+evaluate(facts: Facts): Boolean
}

class Action {
<<funinterface>>
+execute(facts: Facts)
}

class Facts {
-map: ConcurrentHashMap~String, Any?~
+get(name): T?
+set(name, value)
+contains(name): Boolean
+of(vararg pairs)$
}

class RuleSet {
-rules: TreeSet~Rule~
+add(rule: Rule)
+iterator(): Iterator~Rule~
}

Rule <|.. DefaultRule
SuspendRule <|.. DefaultSuspendRule
DefaultRule ..> Condition: uses
DefaultRule ..> Action: uses
DefaultSuspendRule ..> Condition : uses
DefaultSuspendRule ..> Action: uses
RuleSet o-- Rule: sorted by priority
DefaultRule ..> Facts: reads/writes
```

### Rule Engine Class Diagram

```mermaid
classDiagram
    class RuleEngine {
        <<interface>>
        +config: RuleEngineConfig
        +fire(rules: RuleSet, facts: Facts)
        +check(rules: RuleSet, facts: Facts): Map~Rule,Boolean~
    }

    class DefaultRuleEngine {
        +fire(rules, facts)
        +addRuleListener(listener)
        +addRuleEngineListener(listener)
    }

    class InferenceRuleEngine {
        +fire(rules, facts)
    }

    class DefaultSuspendRuleEngine {
        +fire(rules, facts)
    }

    class RuleEngineConfig {
        +skipOnFirstAppliedRule: Boolean
        +skipOnFirstFailedRule: Boolean
        +skipOnFirstNonTriggeredRule: Boolean
        +priorityThreshold: Int
    }

    RuleEngine <|.. DefaultRuleEngine
    RuleEngine <|.. InferenceRuleEngine
    DefaultRuleEngine o-- RuleEngineConfig
```

### Composite Rules

```mermaid
classDiagram
    class CompositeRule {
        <<abstract>>
        #rules: SortedSet~Rule~
        +addRule(rule: Rule)
    }

    class ActivationRuleGroup {
    }
    note for ActivationRuleGroup "Executes only the highest-priority\nrule whose condition is true"

    class ConditionalRuleGroup {
    }
    note for ConditionalRuleGroup "If the highest-priority rule fires,\nexecutes all remaining rules"

    class UnitRuleGroup {
    }
    note for UnitRuleGroup "Executes all rules atomically\nonly if all conditions are true"
    CompositeRule <|-- ActivationRuleGroup
    CompositeRule <|-- ConditionalRuleGroup
    CompositeRule <|-- UnitRuleGroup
    CompositeRule o-- Rule: contains
```

### Rule Execution Sequence

```mermaid
sequenceDiagram
    participant Caller
    participant RuleEngine
    participant RuleListener
    participant Rule
    participant Facts
    Caller ->> RuleEngine: fire(ruleSet, facts)

    loop each Rule (priority order)
        RuleEngine ->> RuleListener: beforeEvaluate(rule, facts)
        RuleEngine ->> Rule: evaluate(facts)
        Rule -->> RuleEngine: true / false

        alt condition = true
            RuleEngine ->> Rule: execute(facts)
            Rule ->> Facts: modify values
            RuleEngine ->> RuleListener: onSuccess(rule, facts)
            Note over RuleEngine: skipOnFirstAppliedRule → stop
        else condition = false
            RuleEngine ->> RuleListener: onFailure(rule, facts)
            Note over RuleEngine: skipOnFirstNonTriggeredRule → stop
        end
    end

    RuleEngine -->> Caller: done
```

### InferenceRuleEngine (Forward Chaining)

```mermaid
flowchart TD
    A[fire start] --> B[evaluate all rules]
    B --> C{any condition\ntrue?}
    C -->|yes| D[execute highest-priority matching rule]
    D --> E[facts updated]
    E --> B
    C -->|no| F[done]
```

### Rule Engine Selection Guide

```mermaid
flowchart TD
    A[Choose Rule Engine] --> B{async needed?}
    B -->|yes| C[DefaultSuspendRuleEngine]
    B -->|no| D{forward chaining?}
    D -->|yes| E[InferenceRuleEngine]
    D -->|no| F[DefaultRuleEngine]
    C & E & F --> G{rule definition}
    G --> H["DSL: rule{}"]
    G --> I["Annotation: @Rule"]
    G --> J["Script: MVEL2 / SpEL"]
    G --> K["File: YAML / JSON / HOCON"]
```

## Core Features

- **DSL-based rule definitions**: `rule {}`, `suspendRule {}`, and `ruleEngine {}` DSLs
- **Annotation-based rules**: convert POJO classes into rules with `@Rule`, `@Condition`, `@Action`, and `@Fact`
- **Coroutine support**: asynchronous rule execution with `SuspendRule` and `SuspendRuleEngine`
- **Script engines**: dynamic rule definitions based on MVEL2, SpEL, and Kotlin Script
- **Rule readers**: load rule definitions from YAML, JSON, and HOCON files
- **Composite rules**: combine multiple rules with `ActivationRuleGroup`, `ConditionalRuleGroup`, and `UnitRuleGroup`
- **Forward chaining**: repeatedly execute while conditions are satisfied through `InferenceRuleEngine`

## Usage Examples

### DSL-Based Rule

```kotlin
val discountRule = rule {
    name = "discount"
    description = "Apply discount for orders above 1000 KRW"
    priority = 1
    condition { facts -> facts.get<Int>("amount")!! > 1000 }
    action { facts -> facts["discount"] = true }
}

val engine = ruleEngine { skipOnFirstAppliedRule = true }
val facts = Facts.of("amount" to 1500)
engine.fire(ruleSetOf(discountRule), facts)
```

### Annotation-Based Rule

```kotlin
@Rule(name = "ageCheck", description = "Adult check", priority = 1)
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

### Coroutine-Based `SuspendRule`

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

### MVEL2 Script Rule

```kotlin
val rule = MvelRule(name = "discount", priority = 1)
    .whenever("amount > 1000")
    .then("discount = true")
```

### SpEL Script Rule

```kotlin
val rule = SpelRule(name = "discount", priority = 1)
    .whenever("#amount > 1000")
    .then("#discount = true")
```

### Load Rules from YAML

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

## Configuration Options

| Option                        | Description                         | Default         |
|-------------------------------|-------------------------------------|-----------------|
| `skipOnFirstAppliedRule`      | Stop after first rule fires         | `false`         |
| `skipOnFirstFailedRule`       | Stop after first rule throws        | `false`         |
| `skipOnFirstNonTriggeredRule` | Stop after first condition is false | `false`         |
| `priorityThreshold`           | Ignore rules above this priority    | `Int.MAX_VALUE` |

## Dependency

```kotlin
implementation(project(":bluetape4k-rule-engine"))

// optional (compileOnly)
implementation("org.mvel:mvel2:2.5.2.Final")              // MVEL2 engine
implementation("org.springframework:spring-expression")     // SpEL engine
implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host") // Kotlin Script engine
implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml") // YAML reader
implementation("com.typesafe:config:1.4.3")                // HOCON reader
```
