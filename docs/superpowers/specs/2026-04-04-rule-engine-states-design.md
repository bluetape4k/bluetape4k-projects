# bluetape4k-rule-engine / bluetape4k-states 모듈 설계 스펙

> **작성일**: 2026-04-04  
> **상태**: Draft  
> **소스**: kommons-rule-engine, kommons-states  
> **대상**: bluetape4k-projects `utils/rule-engine/`, `utils/states/`

---

## 1. 개요

kommons-rule-engine과 kommons-states를 bluetape4k 생태계로 마이그레이션한다.
기존 Java 스타일 코드를 완전 Kotlin화하고, 코루틴 지원 추가, Thread Safety 확보,
타입 안전성 개선, DSL 강화를 수행한다.
bluetape4k-states는 clinic-appointment 패턴(map 기반 전이 + suspend)을 채택한다.

### 범위

| 범위 | rule-engine | states |
|------|:-----------:|:------:|
| 핵심 Rule/FSM 인터페이스 | O | O |
| DSL 빌더 | O | O (신규) |
| 코루틴 지원 (suspend/Flow) | O (신규) | O (신규) |
| Thread Safety | O (개선) | O (개선) |
| Expression Language (MVEL2/SpEL/KotlinScript) | O | - |
| Annotation 기반 Rule | O | - |
| Composite Rule | O | - |
| Guard Condition | - | O (신규) |
| StateFlow 상태 관찰 | - | O (신규) |
| YAML/JSON/HOCON Rule Reader | O | - |
| clinic-appointment 마이그레이션 가이드 | - | O |

---

## 2. 모듈 위치

```
utils/rule-engine/   # bluetape4k-rule-engine
utils/states/        # bluetape4k-states
```

---

## 3. bluetape4k-rule-engine 설계

### 3.1 모듈 구조

```
utils/rule-engine/
├── build.gradle.kts
├── README.md
└── src/
    ├── main/kotlin/io/bluetape4k/rule/
    │   ├── RuleDefaults.kt
    │   ├── api/
    │   │   ├── Rule.kt               # 핵심 인터페이스
    │   │   ├── SuspendRule.kt        # 코루틴 Rule
    │   │   ├── Condition.kt
    │   │   ├── SuspendCondition.kt
    │   │   ├── Action.kt
    │   │   ├── SuspendAction.kt
    │   │   ├── Facts.kt              # Thread-safe ConcurrentHashMap 기반
    │   │   ├── RuleSet.kt
    │   │   ├── RuleEngine.kt
    │   │   ├── SuspendRuleEngine.kt
    │   │   ├── RuleEngineConfig.kt
    │   │   ├── RuleListener.kt
    │   │   └── RuleEngineListener.kt
    │   ├── annotation/
    │   │   ├── RuleAnnotation.kt     # @Rule
    │   │   ├── ConditionAnnotation.kt # @Condition
    │   │   ├── ActionAnnotation.kt   # @Action
    │   │   ├── PriorityAnnotation.kt # @Priority
    │   │   └── FactAnnotation.kt     # @Fact
    │   ├── core/
    │   │   ├── AbstractRule.kt
    │   │   ├── DefaultRule.kt
    │   │   ├── DefaultRuleEngine.kt  # CopyOnWriteArrayList 적용
    │   │   ├── DefaultSuspendRuleEngine.kt
    │   │   ├── InferenceRuleEngine.kt
    │   │   ├── RuleProxy.kt          # ConcurrentHashMap 캐싱
    │   │   ├── RuleDefinitionValidator.kt
    │   │   └── RuleDsl.kt
    │   ├── support/
    │   │   ├── CompositeRule.kt
    │   │   ├── ActivationRuleGroup.kt
    │   │   ├── ConditionalRuleGroup.kt
    │   │   └── UnitRuleGroup.kt
    │   ├── engines/
    │   │   ├── mvel2/
    │   │   │   ├── MvelCondition.kt  # lazy 컴파일 캐싱
    │   │   │   ├── MvelAction.kt
    │   │   │   ├── MvelRule.kt
    │   │   │   └── MvelSupport.kt
    │   │   ├── spel/
    │   │   │   ├── SpelCondition.kt  # Expression 객체 캐싱
    │   │   │   ├── SpelAction.kt
    │   │   │   ├── SpelRule.kt
    │   │   │   └── SpelSupport.kt
    │   │   └── kotlinscript/
    │   │       ├── KotlinScriptEngine.kt  # JSR223 → jvm-host 개선
    │   │       ├── KotlinScriptCondition.kt
    │   │       ├── KotlinScriptAction.kt
    │   │       ├── KotlinScriptRule.kt
    │   │       └── KotlinScriptSupport.kt
    │   ├── readers/
    │   │   ├── RuleReader.kt
    │   │   ├── YamlRuleReader.kt
    │   │   ├── JsonRuleReader.kt
    │   │   └── HoconRuleReader.kt
    │   └── exception/
    │       ├── RuleException.kt
    │       ├── InvalidRuleDefinitionException.kt
    │       └── NoSuchFactException.kt
    └── test/kotlin/io/bluetape4k/rule/
        ├── api/FactsTest.kt
        ├── core/
        │   ├── DefaultRuleEngineTest.kt
        │   ├── DefaultSuspendRuleEngineTest.kt
        │   ├── InferenceRuleEngineTest.kt
        │   ├── RuleDslTest.kt
        │   └── RuleProxyTest.kt
        ├── support/
        ├── engines/
        │   ├── mvel2/MvelRuleTest.kt
        │   ├── spel/SpelRuleTest.kt
        │   └── kotlinscript/KotlinScriptRuleTest.kt
        └── examples/
```

### 3.2 핵심 인터페이스

#### Rule

```kotlin
interface Rule : Comparable<Rule>, Serializable {
    val name: String
    val description: String
    val priority: Int

    fun evaluate(facts: Facts): Boolean
    fun execute(facts: Facts)

    override fun compareTo(other: Rule): Int {
        val c = priority.compareTo(other.priority)
        return if (c != 0) c else name.compareTo(other.name)
    }

    companion object
}
```

#### SuspendRule

```kotlin
interface SuspendRule : Comparable<SuspendRule>, Serializable {
    val name: String
    val description: String
    val priority: Int
    suspend fun evaluate(facts: Facts): Boolean
    suspend fun execute(facts: Facts)
    companion object
}
```

#### Facts (ConcurrentHashMap 기반)

```kotlin
class Facts private constructor(
    private val facts: ConcurrentHashMap<String, Any?>,
) : Serializable {
    companion object : KLogging() {
        private const val serialVersionUID = 1L

        @JvmStatic fun empty(): Facts = Facts(ConcurrentHashMap())
        @JvmStatic fun of(vararg pairs: Pair<String, Any?>): Facts = ...
        @JvmStatic fun from(map: Map<String, Any?>): Facts = ...
    }

    // 타입 안전 접근
    fun <T : Any> get(name: String, type: Class<T>): T? {
        name.requireNotBlank("name")
        return facts[name]?.let { type.cast(it) }
    }

    inline fun <reified T : Any> get(name: String): T? = get(name, T::class.java)

    operator fun set(name: String, value: Any?) {
        name.requireNotBlank("name")
        if (value != null) facts[name] = value else facts.remove(name)
    }
}
```

#### RuleEngine / SuspendRuleEngine

```kotlin
interface RuleEngine {
    val config: RuleEngineConfig
    fun check(rules: RuleSet, facts: Facts): Map<Rule, Boolean>
    fun fire(rules: RuleSet, facts: Facts)
}

interface SuspendRuleEngine {
    val config: RuleEngineConfig
    suspend fun check(rules: RuleSet, facts: Facts): Map<Rule, Boolean>
    suspend fun fire(rules: RuleSet, facts: Facts)
}
```

### 3.3 DSL 설계

```kotlin
@DslMarker
annotation class RuleDsl

@RuleDsl
class RuleBuilder {
    var name: String = DEFAULT_RULE_NAME
    var description: String = DEFAULT_RULE_DESCRIPTION
    var priority: Int = DEFAULT_RULE_PRIORITY

    private var condition: Condition = Condition.FALSE
    private val actions: MutableList<Action> = mutableListOf()

    fun condition(evaluator: (Facts) -> Boolean) { this.condition = Condition { evaluator(it) } }
    fun action(executor: (Facts) -> Unit) { this.actions.add(Action { executor(it) }) }
    internal fun build(): DefaultRule = DefaultRule(name, description, priority, condition, actions)
}

// 동기 규칙 빌더
// 사용 예:
//   val discountRule = rule {
//       name = "discount"
//       condition { facts -> facts.get<Int>("amount")!! > 1000 }
//       action { facts -> facts["discount"] = true }
//   }
fun rule(setup: RuleBuilder.() -> Unit): Rule = RuleBuilder().apply(setup).build()

@RuleDsl
class SuspendRuleBuilder {
    // ... 동일한 구조로 suspend 람다 사용
    fun condition(evaluator: suspend (Facts) -> Boolean) { ... }
    fun action(executor: suspend (Facts) -> Unit) { ... }
}

// 코루틴 규칙 빌더
fun suspendRule(setup: SuspendRuleBuilder.() -> Unit): SuspendRule = ...

// 규칙 엔진 빌더
fun ruleEngine(setup: RuleEngineConfig.() -> Unit = {}): RuleEngine = ...
```

### 3.4 코루틴 통합

DefaultSuspendRuleEngine은 SuspendRule과 일반 Rule 모두 지원한다.
일반 Rule은 withContext(Dispatchers.IO)로 자동 래핑된다.

### 3.5 Expression Language 개선

#### MVEL2: lazy 컴파일 캐시

```kotlin
class MvelCondition(val expression: String) : Condition {
    private val compiled: Serializable by lazy { MVEL.compileExpression(expression) }
    override fun evaluate(facts: Facts): Boolean =
        MVEL.executeExpression(compiled, facts.asMap()) as? Boolean ?: false
}
```

#### Kotlin Script: JSR223 → kotlin-scripting-jvm-host 교체

기존 JSR223(`KotlinJsr223DefaultScriptEngineFactory`)의 문제:
- 동일한 ScriptEngine 공유 → 바인딩 충돌
- 스크립트별 독립 환경 없음

개선: `BasicJvmScriptingHost`로 독립 실행 환경 제공
- `kotlin-scripting-jvm-host` (compileOnly)
- `ScriptCompilationConfiguration`에 `providedProperties("facts" to Facts::class)`
- 컴파일 결과를 `ConcurrentHashMap`으로 캐싱

#### SpEL: Expression 객체 생성 캐싱

```kotlin
class SpelCondition(val expression: String) : Condition {
    private val compiledExpr: Expression = SpelExpressionParser().parseExpression(expression)
    override fun evaluate(facts: Facts): Boolean {
        val ctx = StandardEvaluationContext().apply { setVariables(facts.asMap()) }
        return compiledExpr.getValue(ctx, Boolean::class.java) ?: false
    }
}
```

### 3.6 Thread Safety 개선 요약

| 항목 | 기존 | 개선 |
|------|------|------|
| listener 컬렉션 | `mutableListOf()` | `CopyOnWriteArrayList()` |
| Facts 내부 맵 | `HashMap` | `ConcurrentHashMap` |
| RuleProxy 메서드 조회 | 매번 linear scan | `ConcurrentHashMap` lazy 캐싱 |
| Kotlin Script | 공유 ScriptEngine (충돌) | BasicJvmScriptingHost (독립) |

---

## 4. bluetape4k-states 설계

### 4.1 모듈 구조

```
utils/states/
├── build.gradle.kts
├── README.md
└── src/
    ├── main/kotlin/io/bluetape4k/states/
    │   ├── api/
    │   │   ├── StateMachine.kt          # 제네릭 인터페이스
    │   │   ├── TransitionResult.kt       # 전이 결과 data class
    │   │   └── StateMachineException.kt  # 예외
    │   ├── core/
    │   │   ├── DefaultStateMachine.kt    # AtomicReference + Map 기반
    │   │   ├── StateMachineDsl.kt        # DSL 빌더
    │   │   ├── TransitionKey.kt          # (S, Class<E>) 키
    │   │   └── TransitionTarget.kt       # (targetState, guard?) 값
    │   └── coroutines/
    │       └── SuspendStateMachine.kt    # Mutex + StateFlow
    └── test/kotlin/io/bluetape4k/states/
        ├── core/
        │   ├── DefaultStateMachineTest.kt
        │   ├── StateMachineDslTest.kt
        │   └── GuardedTransitionTest.kt
        ├── coroutines/SuspendStateMachineTest.kt
        └── examples/
            ├── TurnstileExample.kt
            ├── OrderExample.kt
            └── AppointmentExample.kt
```

### 4.2 핵심 인터페이스

```kotlin
/**
 * 유한 상태 머신 인터페이스.
 * S: 상태 타입 (보통 enum class)
 * E: 이벤트 타입 (보통 sealed class)
 */
interface StateMachine<S : Any, E : Any> {
    val currentState: S
    val initialState: S
    val finalStates: Set<S>

    fun transition(event: E): TransitionResult<S, E>
    fun canTransition(event: E): Boolean
    fun allowedEvents(): Set<Class<out E>>
    fun isInFinalState(): Boolean = finalStates.contains(currentState)
}

data class TransitionResult<S : Any, E : Any>(
    val previousState: S,
    val event: E,
    val currentState: S,
) : Serializable {
    companion object { private const val serialVersionUID = 1L }
}
```

### 4.3 DefaultStateMachine (clinic-appointment 패턴 채택)

```kotlin
class DefaultStateMachine<S : Any, E : Any>(
    override val initialState: S,
    override val finalStates: Set<S>,
    private val transitions: Map<TransitionKey<S, E>, TransitionTarget<S, E>>,
    private val onTransition: ((S, E, S) -> Unit)? = null,
) : StateMachine<S, E> {

    companion object : KLogging()

    private val _currentState = AtomicReference(initialState)
    override val currentState: S get() = _currentState.get()

    override fun transition(event: E): TransitionResult<S, E> {
        val previous = _currentState.get()
        if (finalStates.contains(previous)) throw StateMachineException("...")

        val key = TransitionKey(previous, event::class.java)
        val target = transitions[key] ?: throw StateMachineException("...")

        if (target.guard != null && !target.guard.invoke(previous, event)) {
            throw StateMachineException("Guard 조건 미충족: ...")
        }

        // CAS로 Thread-safe 전이
        if (!_currentState.compareAndSet(previous, target.state)) {
            throw StateMachineException("동시 전이 충돌: ...")
        }

        onTransition?.invoke(previous, event, target.state)
        return TransitionResult(previous, event, target.state)
    }
}

data class TransitionKey<S : Any, E : Any>(val state: S, val eventType: Class<out E>)
data class TransitionTarget<S : Any, E : Any>(
    val state: S,
    val guard: ((S, E) -> Boolean)? = null,
)
```

### 4.4 DSL 설계

```kotlin
@DslMarker
annotation class StateMachineDsl

@StateMachineDsl
class StateMachineBuilder<S : Any, E : Any> {
    var initialState: S? = null
    var finalStates: Set<S> = emptySet()
    private val transitions = mutableMapOf<TransitionKey<S, E>, TransitionTarget<S, E>>()
    private var onTransition: ((S, E, S) -> Unit)? = null

    fun transition(from: S, eventType: Class<out E>, to: S) {
        transitions[TransitionKey(from, eventType)] = TransitionTarget(to)
    }

    fun transition(from: S, eventType: Class<out E>, to: S, setup: TransitionBuilder<S, E>.() -> Unit) {
        val builder = TransitionBuilder<S, E>().apply(setup)
        transitions[TransitionKey(from, eventType)] = TransitionTarget(to, builder.guard)
    }

    fun onTransition(handler: (S, E, S) -> Unit) { this.onTransition = handler }
    internal fun build(): DefaultStateMachine<S, E> = ...
}

@StateMachineDsl
class TransitionBuilder<S : Any, E : Any> {
    var guard: ((S, E) -> Boolean)? = null
    fun guard(predicate: (S, E) -> Boolean) { this.guard = predicate }
}

// 이벤트 타입 헬퍼
inline fun <reified E : Any> on(): Class<E> = E::class.java

// DSL 진입점
// 사용 예:
//   val fsm = stateMachine<OrderState, OrderEvent> {
//       initialState = OrderState.CREATED
//       finalStates = setOf(OrderState.DELIVERED, OrderState.CANCELLED)
//       transition(OrderState.CREATED, on<OrderEvent.Pay>(), to = OrderState.PAID)
//       onTransition { prev, event, next -> log.info { "$prev --> $next" } }
//   }
inline fun <reified S : Any, reified E : Any> stateMachine(
    setup: StateMachineBuilder<S, E>.() -> Unit,
): StateMachine<S, E> = StateMachineBuilder<S, E>().apply(setup).build()
```

### 4.5 SuspendStateMachine (코루틴 + StateFlow)

```kotlin
class SuspendStateMachine<S : Any, E : Any>(
    override val initialState: S,
    override val finalStates: Set<S>,
    private val transitions: Map<TransitionKey<S, E>, TransitionTarget<S, E>>,
    private val onTransition: (suspend (S, E, S) -> Unit)? = null,
) : StateMachine<S, E> {

    companion object : KLogging()

    private val mutex = Mutex()
    private val _stateFlow = MutableStateFlow(initialState)

    /** 현재 상태를 StateFlow로 관찰합니다. */
    val stateFlow: StateFlow<S> = _stateFlow.asStateFlow()

    override val currentState: S get() = _stateFlow.value

    /**
     * Mutex로 직렬화된 코루틴 기반 상태 전이.
     */
    suspend fun suspendTransition(event: E): TransitionResult<S, E> = mutex.withLock {
        val previous = _stateFlow.value
        if (finalStates.contains(previous)) throw StateMachineException("...")

        val key = TransitionKey(previous, event::class.java)
        val target = transitions[key] ?: throw StateMachineException("...")

        if (target.guard != null && !target.guard.invoke(previous, event)) {
            throw StateMachineException("...")
        }

        _stateFlow.value = target.state
        onTransition?.invoke(previous, event, target.state)
        TransitionResult(previous, event, target.state)
    }
}

// 코루틴 FSM DSL 진입점
// 사용 예:
//   val fsm = suspendStateMachine<State, Event> { ... }
//   fsm.stateFlow.collect { state -> updateUI(state) }
//   fsm.suspendTransition(SomeEvent())
inline fun <reified S : Any, reified E : Any> suspendStateMachine(
    setup: SuspendStateMachineBuilder<S, E>.() -> Unit,
): SuspendStateMachine<S, E> = SuspendStateMachineBuilder<S, E>().apply(setup).build()
```

---

## 5. 테스트 전략

### 5.1 bluetape4k-rule-engine (목표: 64+ 케이스)

| 카테고리 | 주요 테스트 케이스 | 수량 |
|----------|-----------------|:----:|
| Facts | 생성, 타입 안전 접근, thread-safe 동시 접근 | 6 |
| Rule DSL | 동기/코루틴 rule, 복수 action, 우선순위 | 6 |
| DefaultRuleEngine | fire, check, skipOnFirst*, priorityThreshold, 리스너 | 10 |
| DefaultSuspendRuleEngine | suspend fire/check, 동기 Rule 코루틴 래핑 | 6 |
| InferenceRuleEngine | 반복 실행, 후보 선택 | 4 |
| RuleProxy | 어노테이션 변환, @Fact 주입, 캐싱, 잘못된 정의 | 8 |
| CompositeRule | ActivationGroup, ConditionalGroup, UnitGroup | 6 |
| MVEL2 | 조건 평가, 액션 실행, 컴파일 캐시 | 5 |
| SpEL | 조건 평가, 변수 바인딩, BeanResolver | 5 |
| KotlinScript | 조건 평가, 스레드 안전, 컴파일 캐싱 | 5 |
| RuleReader | YAML, JSON, HOCON | 3 |
| **합계** | | **64** |

### 5.2 bluetape4k-states (목표: 32+ 케이스)

| 카테고리 | 주요 테스트 케이스 | 수량 |
|----------|-----------------|:----:|
| DefaultStateMachine | 전이, 유효하지 않은 전이, 최종 상태, CAS 동시성 | 8 |
| StateMachine DSL | 기본 빌더, 복수 전이, finalStates, onTransition | 5 |
| Guard Condition | guard 통과, guard 실패, guard 없는 전이 | 4 |
| SuspendStateMachine | suspend 전이, Mutex 동시성, StateFlow 관찰 | 6 |
| 예제 테스트 | Turnstile, Order, Appointment | 6 |
| 엣지 케이스 | 빈 전이맵, 자기 자신으로 전이 | 3 |
| **합계** | | **32** |

---

## 6. Gradle 의존성

### bluetape4k-rule-engine build.gradle.kts

```kotlin
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-core"))
    testImplementation(project(":bluetape4k-junit5"))

    implementation(project(":bluetape4k-coroutines"))
    implementation(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)

    compileOnly(Libs.mvel2)
    compileOnly(Libs.kotlin_scripting_common)
    compileOnly(Libs.kotlin_scripting_jvm)
    compileOnly(kotlin("scripting-jvm-host"))
    compileOnly("org.springframework:spring-expression")
    compileOnly(Libs.jackson_dataformat_yaml)
    compileOnly(Libs.typesafe_config)

    testImplementation(Libs.mvel2)
    testImplementation(kotlin("scripting-jvm-host"))
    testImplementation("org.springframework:spring-context")
}
```

### bluetape4k-states build.gradle.kts

```kotlin
dependencies {
    api(project(":bluetape4k-core"))
    testImplementation(project(":bluetape4k-junit5"))

    implementation(project(":bluetape4k-coroutines"))
    implementation(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)
}
```

---

## 7. clinic-appointment 마이그레이션 가이드

### 기존 코드 패턴

```kotlin
class AppointmentStateMachine(
    private val onTransition: (suspend (AppointmentState, AppointmentEvent, AppointmentState) -> Unit)? = null,
) {
    private val transitions: Map<Pair<AppointmentState, Class<out AppointmentEvent>>, AppointmentState> =
        buildMap { put(PENDING to Request::class.java, REQUESTED) /* ... 25개 */ }

    suspend fun transition(currentState: AppointmentState, event: AppointmentEvent): AppointmentState { ... }
}
```

### 마이그레이션 후

```kotlin
// bluetape4k-states 사용
val appointmentFsm = suspendStateMachine<AppointmentState, AppointmentEvent> {
    initialState = PENDING
    finalStates = setOf(COMPLETED, CANCELLED, RESCHEDULED)

    transition(PENDING,   on<Request>(),           to = REQUESTED)
    transition(REQUESTED, on<Confirm>(),            to = CONFIRMED)
    transition(CONFIRMED, on<CheckIn>(),            to = CHECKED_IN)
    transition(CHECKED_IN, on<StartTreatment>(),    to = IN_PROGRESS)
    transition(IN_PROGRESS, on<Complete>(),         to = COMPLETED)
    transition(PENDING,   on<Cancel>(),             to = CANCELLED)
    transition(REQUESTED, on<Cancel>(),             to = CANCELLED)
    transition(CONFIRMED, on<Cancel>(),             to = CANCELLED)
    transition(CONFIRMED, on<MarkNoShow>(),         to = NO_SHOW)
    transition(CONFIRMED, on<RequestReschedule>(),  to = PENDING_RESCHEDULE)
    transition(PENDING_RESCHEDULE, on<ConfirmReschedule>(), to = RESCHEDULED)

    onTransition { prev, event, next ->
        log.info { "예약 상태 전이: $prev --[$event]--> $next" }
    }
}

// 사용
val result = appointmentFsm.suspendTransition(AppointmentEvent.Request)
val allowed = appointmentFsm.allowedEvents()
appointmentFsm.stateFlow.collect { state -> ... }
```

---

## 8. 트레이드오프

| 설계 결정 | 장점 | 단점 |
|-----------|------|------|
| Rule/SuspendRule 인터페이스 분리 | 동기 코드에 코루틴 의존 없음 | 인터페이스 2벌 유지 |
| Facts에 ConcurrentHashMap | Thread Safety, 빠른 동시 읽기 | null value 저장 불가 → remove 사용 |
| Map 기반 FSM (clinic-appointment 패턴) | O(1) 전이, 간결함, DSL 친화적 | hierarchical state 미지원 |
| AtomicReference CAS (동기 FSM) | 락 없이 빠름 | CAS 실패 시 예외 → 동시 전이 불허 의도적 설계 |
| Kotlin Script → jvm-host | 스레드 안전, 독립 환경, 컴파일 캐싱 | classpath 설정 복잡, 첫 컴파일 느림 |
| EL compileOnly | 사용자가 필요한 EL만 선택 | 런타임 ClassNotFoundException 가능 (문서 안내) |

---

## 9. 구현 우선순위

### Phase 1: 핵심
1. `bluetape4k-states` 전체 (core + coroutines + DSL + 테스트 + README)
2. `bluetape4k-rule-engine` api + core (Rule, Facts, RuleEngine, DSL)

### Phase 2: Expression Language
3. MVEL2 마이그레이션
4. SpEL 마이그레이션
5. Kotlin Script 개선 (jvm-host)

### Phase 3: 부가 기능
6. 어노테이션 기반 RuleProxy (캐싱 포함)
7. Composite Rule 그룹
8. Rule Reader (YAML, JSON, HOCON)
9. SuspendRuleEngine

### Phase 4: 문서 및 마이그레이션
10. README.md 최종 작성
11. clinic-appointment 마이그레이션 실행
12. CLAUDE.md 업데이트

---

## 10. 스펙 리뷰 수정사항 (2026-04-04)

### [high] 수정 1: StateMachine 인터페이스 계층 재설계

`SuspendStateMachine : StateMachine` 구현은 컴파일 오류다.
`StateMachine.transition()`은 non-suspend이지만 `SuspendStateMachine`은 `suspendTransition()`만 제공한다.

**수정된 인터페이스 계층**:

```kotlin
/**
 * 상태 머신의 읽기 전용 기본 인터페이스.
 * 동기/코루틴 상태 머신의 공통 프로퍼티와 조회 메서드를 정의합니다.
 */
interface BaseStateMachine<S : Any, E : Any> {
    val currentState: S
    val initialState: S
    val finalStates: Set<S>

    fun canTransition(event: E): Boolean
    fun allowedEvents(): Set<Class<out E>>
    fun isInFinalState(): Boolean = finalStates.contains(currentState)
}

/**
 * 동기 상태 머신 인터페이스.
 */
interface StateMachine<S : Any, E : Any> : BaseStateMachine<S, E> {
    fun transition(event: E): TransitionResult<S, E>
}

/**
 * 코루틴 기반 상태 머신 인터페이스.
 */
interface SuspendStateMachineInterface<S : Any, E : Any> : BaseStateMachine<S, E> {
    /** 코루틴 환경에서 상태를 전이합니다. */
    suspend fun transition(event: E): TransitionResult<S, E>
    
    /** StateFlow로 상태 변화를 관찰합니다. */
    val stateFlow: StateFlow<S>
}
```

`DefaultStateMachine`은 `StateMachine<S, E>`를 구현하고,
`SuspendStateMachine`은 `SuspendStateMachineInterface<S, E>`를 구현한다. 두 클래스는 `BaseStateMachine`을 공유한다.

**DSL 업데이트**:

```kotlin
// 동기 FSM
inline fun <reified S : Any, reified E : Any> stateMachine(
    setup: StateMachineBuilder<S, E>.() -> Unit,
): StateMachine<S, E> = StateMachineBuilder<S, E>().apply(setup).build()

// 코루틴 FSM
inline fun <reified S : Any, reified E : Any> suspendStateMachine(
    setup: SuspendStateMachineBuilder<S, E>.() -> Unit,
): SuspendStateMachineInterface<S, E> = SuspendStateMachineBuilder<S, E>().apply(setup).build()
```

### [high] 수정 2: Gradle 의존성 버전 명시

Spring BOM을 적용하거나 Libs.kt에 상수 추가:

```kotlin
// bluetape4k-rule-engine build.gradle.kts
dependencies {
    // Spring BOM으로 버전 관리
    implementation(platform(Libs.spring_boot3_dependencies))  // Spring 버전 통합 관리
    compileOnly("org.springframework:spring-expression")       // BOM 버전 자동 적용
    testImplementation("org.springframework:spring-context")

    // HOCON: Libs.kt에 추가 후 참조
    // Libs.kt: const val typesafe_config = "com.typesafe:config:1.4.3"
    compileOnly(Libs.typesafe_config)
}
```

### [medium] 수정 3: 누락된 인터페이스 정의 보완

#### Condition / Action (fun interface)

```kotlin
/** 규칙의 조건을 정의하는 함수형 인터페이스. SAM 변환 지원. */
fun interface Condition {
    fun evaluate(facts: Facts): Boolean
    companion object {
        @JvmField val TRUE: Condition = Condition { true }
        @JvmField val FALSE: Condition = Condition { false }
    }
}

fun interface SuspendCondition {
    suspend fun evaluate(facts: Facts): Boolean
    companion object {
        @JvmField val TRUE: SuspendCondition = SuspendCondition { true }
        @JvmField val FALSE: SuspendCondition = SuspendCondition { false }
    }
}

fun interface Action { fun execute(facts: Facts) }
fun interface SuspendAction { suspend fun execute(facts: Facts) }
```

#### RuleSet

```kotlin
/**
 * 규칙 집합. 우선순위 순으로 정렬된 규칙 컬렉션입니다.
 */
class RuleSet private constructor(private val rules: TreeSet<Rule>) : Iterable<Rule> by rules {
    companion object : KLogging() {
        @JvmStatic fun of(vararg rules: Rule): RuleSet = RuleSet(TreeSet(rules.toList()))
        @JvmStatic fun of(rules: Collection<Rule>): RuleSet = RuleSet(TreeSet(rules))
    }

    val size: Int get() = rules.size
    fun isEmpty(): Boolean = rules.isEmpty()
    fun add(rule: Rule): Boolean = rules.add(rule)
}

fun ruleSetOf(vararg rules: Rule): RuleSet = RuleSet.of(*rules)
```

#### RuleEngineConfig

```kotlin
/**
 * 규칙 엔진 설정.
 */
data class RuleEngineConfig(
    /** 첫 번째로 적용된 규칙이 있으면 나머지 규칙 평가를 중단합니다. */
    var skipOnFirstAppliedRule: Boolean = false,
    /** 첫 번째로 실패한 규칙이 있으면 나머지 규칙 평가를 중단합니다. */
    var skipOnFirstFailedRule: Boolean = false,
    /** 첫 번째로 조건을 만족하지 않는 규칙이 있으면 나머지 평가를 중단합니다. */
    var skipOnFirstNonTriggeredRule: Boolean = false,
    /** 이 우선순위보다 낮은 규칙은 실행하지 않습니다. */
    var priorityThreshold: Int = Int.MAX_VALUE,
) : Serializable {
    companion object : KLogging() {
        private const val serialVersionUID = 1L
        val DEFAULT = RuleEngineConfig()
    }
}
```

#### Facts.asMap() 추가

```kotlin
// Facts 클래스에 asMap() 추가 (스냅샷 복사 반환)
fun asMap(): Map<String, Any?> = facts.toMap()  // 불변 스냅샷
```

#### SuspendRule.compareTo() 기본 구현

```kotlin
interface SuspendRule : Comparable<SuspendRule>, Serializable {
    val name: String
    val description: String
    val priority: Int
    suspend fun evaluate(facts: Facts): Boolean
    suspend fun execute(facts: Facts)

    // compareTo 기본 구현 필수 (Rule과 동일)
    override fun compareTo(other: SuspendRule): Int {
        val c = priority.compareTo(other.priority)
        return if (c != 0) c else name.compareTo(other.name)
    }
}
```

#### SuspendStateMachineBuilder 정의

```kotlin
@StateMachineDsl
class SuspendStateMachineBuilder<S : Any, E : Any> {
    var initialState: S? = null
    var finalStates: Set<S> = emptySet()

    private val transitions = mutableMapOf<TransitionKey<S, E>, TransitionTarget<S, E>>()
    private var onTransition: (suspend (S, E, S) -> Unit)? = null

    fun transition(from: S, eventType: Class<out E>, to: S) {
        transitions[TransitionKey(from, eventType)] = TransitionTarget(to)
    }

    fun transition(
        from: S, eventType: Class<out E>, to: S,
        setup: TransitionBuilder<S, E>.() -> Unit,
    ) {
        val builder = TransitionBuilder<S, E>().apply(setup)
        transitions[TransitionKey(from, eventType)] = TransitionTarget(to, builder.guard)
    }

    fun onTransition(handler: suspend (S, E, S) -> Unit) { this.onTransition = handler }

    internal fun build(): SuspendStateMachine<S, E> {
        val initial = initialState
        initial.requireNotNull("initialState")
        return SuspendStateMachine(initial!!, finalStates, transitions.toMap(), onTransition)
    }
}
```

#### TransitionKey / TransitionTarget Serializable 추가

```kotlin
data class TransitionKey<S : Any, E : Any>(
    val state: S,
    val eventType: Class<out E>,
) : Serializable {
    companion object { private const val serialVersionUID = 1L }
}

data class TransitionTarget<S : Any, E : Any>(
    val state: S,
    val guard: ((S, E) -> Boolean)? = null,
) : Serializable {
    companion object { private const val serialVersionUID = 1L }
}
```

### [medium] 수정 4: CAS 실패 정책 명시

`DefaultStateMachine.transition()` KDoc에 명시:
- 동시에 두 스레드가 같은 상태에서 전이를 시도하면 한 쪽은 `StateMachineException` 발생
- 재시도가 필요한 경우 호출자가 처리
- 동시 전이가 예상되는 환경에서는 `SuspendStateMachine` (Mutex 직렬화) 사용 권장

### [low] 수정 5: Kotlin Script `by lazy` 스레드 안전 모드

```kotlin
// LazyThreadSafetyMode.SYNCHRONIZED (기본값) → 첫 컴파일은 직렬화, 이후 캐시 활용
// MvelCondition의 경우 PUBLICATION 모드 고려 (경쟁 시 중복 컴파일 허용, 이후 동일 결과)
private val compiled: Serializable by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    MVEL.compileExpression(expression)
}
```

### 모듈 이름 확정

- `utils/rule-engine/` → 모듈명 `bluetape4k-rule-engine`
  (타 `utils/` 모듈 이름보다 길지만 기능 명확성 우선. `rules`로 줄일 경우 추후 변경 가능)
