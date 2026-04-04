# bluetape4k-rule-engine / bluetape4k-states 구현 플랜

**작성일**: 2026-04-04
**스펙**: `docs/superpowers/specs/2026-04-04-rule-engine-states-design.md`

---

## 소스 참조

- `~/work/debop/kommons/kommons-rule-engine/` — 기존 구현
- `~/work/debop/kommons/kommons-states/` — 기존 구현
- `~/work/bluetape4k/clinic-appointment/` — AppointmentStateMachine 패턴
- `utils/idgenerators/build.gradle.kts` — build.gradle.kts 템플릿

---

## Phase 1: bluetape4k-states (우선 구현)

### Task 1: 모듈 초기화 [complexity: low]

- `utils/states/build.gradle.kts` 생성 (idgenerators 패턴 참조)
- 디렉토리 구조: `src/main/kotlin/io/bluetape4k/states/{api,core,coroutines}/`, `src/test/kotlin/io/bluetape4k/states/{core,coroutines,examples}/`
- 의존성: `bluetape4k-core`(api), `bluetape4k-coroutines`(impl), `kotlinx-coroutines-core`(impl), `bluetape4k-junit5`(test), `kotlinx-coroutines-test`(test)
- **AC**: `./gradlew :bluetape4k-states:dependencies` 성공

### Task 2: 핵심 인터페이스 + Data Class [complexity: high]

- `api/BaseStateMachine.kt` — 읽기 전용 공통 인터페이스 `<S: Any, E: Any>`
  - `currentState`, `initialState`, `finalStates`, `canTransition()`, `allowedEvents()`, `isInFinalState()`
- `api/StateMachine.kt` — `BaseStateMachine` 확장, `fun transition(event): TransitionResult`
- `api/SuspendStateMachineInterface.kt` — `BaseStateMachine` 확장, `suspend fun transition()`, `stateFlow: StateFlow<S>`
- `api/TransitionResult.kt` — `data class <S, E>(previousState, event, currentState) : Serializable`, serialVersionUID = 1L
- `api/StateMachineException.kt` — `RuntimeException` 하위
- `core/TransitionKey.kt` — `data class <S, E>(state, eventType: Class<out E>) : Serializable`, serialVersionUID = 1L
- `core/TransitionTarget.kt` — `data class <S, E>(state, guard?) : Serializable`, serialVersionUID = 1L
- **AC**: 모든 인터페이스 컴파일 성공, Korean KDoc 작성

### Task 3: DefaultStateMachine [complexity: high]

- `core/DefaultStateMachine.kt` — `AtomicReference<S>` + `Map<TransitionKey, TransitionTarget>` 기반
- CAS 전이: `_currentState.compareAndSet(previous, target.state)` 실패 시 `StateMachineException`
- KDoc에 CAS 동시성 정책 명시: "동시 전이 필요 시 SuspendStateMachine 사용 권장"
- `canTransition()`, `allowedEvents()`, `isInFinalState()` 구현
- **AC**: 단위 테스트 8케이스 통과

### Task 4: SuspendStateMachine [complexity: high]

- `coroutines/SuspendStateMachine.kt` — `SuspendStateMachineInterface<S, E>` 구현
- `Mutex` + `MutableStateFlow<S>` 기반
- `suspendTransition()` 메서드 (`mutex.withLock` 직렬화)
- `stateFlow: StateFlow<S>` 공개
- Guard 조건 평가 포함
- **AC**: 단위 테스트 6케이스 통과 (suspend 전이, Mutex 동시성, StateFlow 관찰, guard 통과/실패)

### Task 5: DSL 빌더 [complexity: medium]

- `core/StateMachineDsl.kt`:
  - `@StateMachineDsl` DslMarker
  - `StateMachineBuilder<S, E>` (initialState, finalStates, transitions, onTransition)
  - `SuspendStateMachineBuilder<S, E>` (suspend onTransition)
  - `TransitionBuilder<S, E>` (guard 설정)
  - `inline fun <reified S, E> stateMachine {}` 진입점
  - `inline fun <reified S, E> suspendStateMachine {}` 진입점
  - `inline fun <reified E> on(): Class<E>` 헬퍼
- **AC**: DSL 테스트 5케이스 + Guard 테스트 4케이스 통과

### Task 6: 예제 테스트 + README [complexity: low]

- `examples/TurnstileExample.kt` — Turnstile FSM (Locked/Unlocked, Coin/Push)
- `examples/OrderExample.kt` — Order FSM (CREATED/PAID/SHIPPED/DELIVERED/CANCELLED)
- `examples/AppointmentExample.kt` — clinic-appointment 마이그레이션 데모
- `README.md` — 개요, Quick Start, DSL 사용법, 코루틴 FSM, clinic-appointment 마이그레이션 가이드
- **AC**: 예제 테스트 6케이스 통과, README 완성

---

## Phase 2: bluetape4k-rule-engine 핵심

### Task 7: 모듈 초기화 + Libs.kt 추가 [complexity: low]

- `buildSrc/src/main/kotlin/Libs.kt`에 추가:
  - `const val typesafe_config = "com.typesafe:config:1.4.3"`
  - `val kotlin_scripting_jvm_host = kotlin("scripting-jvm-host")`
- `utils/rule-engine/build.gradle.kts` 생성:
  - `configurations { testImplementation.get().extendsFrom(compileOnly.get()) }` (runtimeOnly 불필요)
  - Spring BOM: `implementation(platform(Libs.spring_boot3_dependencies))`
  - `compileOnly`: mvel2, kotlin_scripting_common/jvm/jvm_host, spring-expression, jackson_dataformat_yaml, typesafe_config
  - `testImplementation`: mvel2, kotlin_scripting_jvm_host, spring-context
- 디렉토리 구조: `src/main/kotlin/io/bluetape4k/rule/{api,annotation,core,support,engines/{mvel2,spel,kotlinscript},readers,exception}/`
- **AC**: `./gradlew :bluetape4k-rule-engine:dependencies` 성공

### Task 8: 핵심 인터페이스 [complexity: high]

- `api/Rule.kt` — `Comparable<Rule>, Serializable`, `evaluate(facts)`, `execute(facts)`, `compareTo()` 기본 구현
- `api/SuspendRule.kt` — suspend 버전, `compareTo()` 기본 구현 (필수)
- `api/Condition.kt` — `fun interface`, `companion object { TRUE, FALSE }`
- `api/SuspendCondition.kt` — suspend fun interface, `companion object { TRUE, FALSE }`
- `api/Action.kt`, `api/SuspendAction.kt` — fun interface
- `api/Facts.kt` — `ConcurrentHashMap` 기반, `get<T>()`, `set()`, `asMap()`, `empty()`, `of()`, `from()`, `remove()`, Serializable
- `api/RuleSet.kt` — `TreeSet<Rule>` 기반, `Iterable<Rule>`, `ruleSetOf()` 확장
- `api/RuleEngineConfig.kt` — `data class`: skipOnFirstApplied/Failed/NonTriggered, priorityThreshold, Serializable
- `api/RuleEngine.kt`, `api/SuspendRuleEngine.kt` — `check()`, `fire()`
- `api/RuleListener.kt`, `api/RuleEngineListener.kt`
- `RuleDefaults.kt` — 기본 상수 (DEFAULT_RULE_NAME, DEFAULT_RULE_PRIORITY 등)
- `exception/{RuleException, InvalidRuleDefinitionException, NoSuchFactException}.kt`
- **AC**: 컴파일 성공, Korean KDoc

### Task 9: AbstractRule + DefaultRule + DSL [complexity: medium]

- `core/AbstractRule.kt` — name, description, priority, compareTo, equals/hashCode
- `core/DefaultRule.kt` — condition + actions 기반 Rule 구현
- `core/DefaultSuspendRule.kt` — SuspendCondition + SuspendAction 기반
- `core/RuleDsl.kt`:
  - `@RuleDsl` DslMarker
  - `RuleBuilder`, `SuspendRuleBuilder`
  - `fun rule {}`, `fun suspendRule {}`, `fun ruleEngine {}`
- **AC**: Facts + Rule DSL 테스트 12케이스 통과

### Task 10: DefaultRuleEngine + DefaultSuspendRuleEngine [complexity: high]

- `core/DefaultRuleEngine.kt` — `CopyOnWriteArrayList` 리스너, priority threshold, skipOnFirst* 3종
  - `beforeEvaluate`, `afterEvaluate`, `beforeExecute`, `afterExecute` 리스너 훅
- `core/DefaultSuspendRuleEngine.kt` — `SuspendRule`은 직접 호출, 일반 `Rule`은 `withContext(Dispatchers.IO)`
- `core/DefaultRuleListener.kt`, `core/DefaultRuleEngineListener.kt` — 기본 로깅 구현
- **AC**: DefaultRuleEngine 10케이스 + SuspendRuleEngine 6케이스 통과

---

## Phase 3: Annotation + Composite + Expression Language

### Task 11: Annotation + RuleProxy [complexity: medium]

- `annotation/` — `@Rule`, `@Condition`, `@Action`, `@Priority`, `@Fact` 어노테이션
- `core/RuleProxy.kt` — 리플렉션 기반, `ConcurrentHashMap` 메서드 조회 캐싱 (`by lazy`)
- `core/RuleDefinitionValidator.kt` — 어노테이션 유효성 검증
- `.asRule()` 확장 함수
- **AC**: RuleProxy 테스트 8케이스 통과

### Task 12: Composite Rule 그룹 [complexity: medium]

- `support/CompositeRule.kt` — 복합 규칙 기본 클래스
- `support/ActivationRuleGroup.kt` — 첫 번째 적용 규칙만 실행
- `support/ConditionalRuleGroup.kt` — 최고 우선순위 규칙 조건 충족 시 나머지 실행
- `support/UnitRuleGroup.kt` — 모든 규칙 조건 충족 시 전체 실행
- **AC**: CompositeRule 테스트 6케이스 통과

### Task 13: InferenceRuleEngine [complexity: medium]

- `core/InferenceRuleEngine.kt` — 후보 규칙이 없을 때까지 반복 실행 (forward chaining)
- **AC**: InferenceRuleEngine 테스트 4케이스 통과

### Task 14: MVEL2 Expression Engine [complexity: medium]

- `engines/mvel2/MvelCondition.kt` — `lazy(SYNCHRONIZED)` 컴파일 캐싱
- `engines/mvel2/MvelAction.kt`, `MvelRule.kt`, `MvelSupport.kt`
- **AC**: MVEL2 테스트 5케이스 통과

### Task 15: SpEL Expression Engine [complexity: medium]

- `engines/spel/SpelCondition.kt` — `Expression` 객체 생성 캐싱 (`by lazy`)
- `engines/spel/SpelAction.kt`, `SpelRule.kt`, `SpelSupport.kt`
- **AC**: SpEL 테스트 5케이스 통과

### Task 16: Kotlin Script Expression Engine [complexity: medium]

- `engines/kotlinscript/KotlinScriptEngine.kt` — `BasicJvmScriptingHost` 기반
  - `ScriptCompilationConfiguration`: `dependenciesFromCurrentContext(wholeClasspath = true)`, `providedProperties("facts" to Facts::class)`
  - 컴파일 결과 `ConcurrentHashMap` 캐싱
- `engines/kotlinscript/KotlinScriptCondition.kt`, `KotlinScriptAction.kt`, `KotlinScriptRule.kt`, `KotlinScriptSupport.kt`
- **AC**: KotlinScript 테스트 5케이스 통과

### Task 17: Rule Reader [complexity: low]

- `readers/RuleReader.kt` — 인터페이스 (load by filename/stream)
- `readers/YamlRuleReader.kt` — Jackson YAML 기반
- `readers/JsonRuleReader.kt` — Jackson JSON 기반
- `readers/HoconRuleReader.kt` — Typesafe Config 기반
- **AC**: Reader 테스트 3케이스 통과

---

## Phase 4: 문서 및 마무리

### Task 18: README + CLAUDE.md [complexity: low]

- `utils/rule-engine/README.md` — 개요, Quick Start, DSL, Annotation, Composite, EL, Reader
- 루트 `CLAUDE.md` `utils/` 테이블에 `rule-engine`, `states` 항목 추가
- **AC**: README 완성, CLAUDE.md 업데이트

---

## 성공 기준

- [ ] `./gradlew :bluetape4k-states:test` — 32+ 테스트 통과
- [ ] `./gradlew :bluetape4k-rule-engine:test` — 64+ 테스트 통과
- [ ] `./gradlew :bluetape4k-states:build :bluetape4k-rule-engine:build` — 빌드 성공
- [ ] 모든 public API Korean KDoc 작성
- [ ] `detekt` 정적 분석 통과
- [ ] `docs/testlog.md` 테스트 결과 기록
- [ ] CLAUDE.md `utils/` 섹션 업데이트

---

## 의존 관계

```
Task 1 → Task 2 → Task 3 → Task 4 → Task 5 → Task 6   (states, 순차)
Task 7 → Task 8 → Task 9 → Task 10                     (rule-engine 핵심, 순차)
Task 10 → Task 11, 12, 13                               (병렬 가능)
Task 14, 15, 16                                         (병렬 가능)
Task 17                                                 (독립)
Task 18                                                 (마지막)
```
