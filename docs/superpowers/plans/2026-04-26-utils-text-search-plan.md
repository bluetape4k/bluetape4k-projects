---
title: "utils/text-search Implementation Plan"
spec: docs/superpowers/specs/2026-04-26-utils-text-search-design.md
branch: feat/utils-text-search
worktree: .worktrees/feat-utils-text-search
created: 2026-04-26
author: planner (Opus, OMC)
---

# utils/text-search Implementation Plan

> **For agentic
workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `x-obsoleted/ahocorasick`(11 main + 12 test) 의 검증된 Aho-Corasick 구현을 신규 `utils/text-search` 모듈로 승격 (promotion)한다. 단순 이동에 그치지 않고 — value 매핑 generic (`AhoCorasickAutomaton<V>`) · Kotlin DSL · Coroutines `Flow<AhoCorasickMatch<V>>` · `replaceAll { ... }` 람다 · 선택적 Unicode 정규화 (NFC/NFKC) API를 신규 추가한다. `kotlinx-benchmark`(JMH) 를 monorepo 최초로 본 모듈에서 도입한다 (단, `utils/batch` 의 검증된 패턴을 따른다).

**Architecture:**

- 접근법 B (spec §2): 신 API `AhoCorasickAutomaton<V>` 만 public, 구 `Trie/State/Emit/Interval*` 는 `internal class TrieCore` 등으로 격리.
- 패키지 구조 (spec §3.1):
  ```
  io.bluetape4k.text.search                  // public API
  io.bluetape4k.text.search.flow             // Flow 확장
  io.bluetape4k.text.search.internal         // TrieCore, State, Emit, EmitHandler, OffsetMapping
  io.bluetape4k.text.search.internal.interval// Interval, IntervalNode, IntervalTree, Intervalable
  ```
- 모든 public 데이터 타입: `Serializable` + `serialVersionUID = 1L` + `companion object : KLogging()`.
- DSL: `@DslMarker annotation class AhoCorasickDsl` + `fun <V> ahoCorasick { }` + `ahoCorasickOf(vararg keywords: String)` 헬퍼.
- `kotlinx-coroutines-core`/`bluetape4k-coroutines`: `compileOnly` (Flow API 사용자만 추가 의존). Java/blocking-only 사용자는 coroutines 없이도 빌드 가능.

**Tech
Stack:** Kotlin 2.3, JVM 21, Gradle multi-module, JUnit 5 + MockK + bluetape4k-assertions, kotlinx-coroutines (compileOnly), kotlinx-benchmark (JMH).

---

## 파일 구조 맵

### 생성할 파일 (소스)

- `utils/text-search/build.gradle.kts`
- `utils/text-search/README.md`
- `utils/text-search/README.ko.md`
- **public** (`io.bluetape4k.text.search`)
    - `src/main/kotlin/io/bluetape4k/text/search/AhoCorasickMatch.kt`
    - `src/main/kotlin/io/bluetape4k/text/search/SearchToken.kt`
    - `src/main/kotlin/io/bluetape4k/text/search/SearchOptions.kt` (`WordBoundary`, `NormalizationForm`, `SearchOptions`)
    - `src/main/kotlin/io/bluetape4k/text/search/AhoCorasickAutomaton.kt`
    - `src/main/kotlin/io/bluetape4k/text/search/AhoCorasickBuilder.kt` (DSL + `ahoCorasick { }` + `ahoCorasickOf(...)`)
- **flow** (`io.bluetape4k.text.search.flow`)
    - `src/main/kotlin/io/bluetape4k/text/search/flow/AhoCorasickFlowExtensions.kt`
- **internal** (`io.bluetape4k.text.search.internal`)
    - `src/main/kotlin/io/bluetape4k/text/search/internal/TrieCore.kt` (구 `Trie.kt`)
    - `src/main/kotlin/io/bluetape4k/text/search/internal/State.kt`
    - `src/main/kotlin/io/bluetape4k/text/search/internal/Emit.kt`
    - `src/main/kotlin/io/bluetape4k/text/search/internal/EmitHandler.kt`
    - `src/main/kotlin/io/bluetape4k/text/search/internal/InternalToken.kt` (구 `Token.kt` 의 internal 사본)
    - `src/main/kotlin/io/bluetape4k/text/search/internal/OffsetMapping.kt`
    - `src/main/kotlin/io/bluetape4k/text/search/internal/Normalizers.kt` (정규화 + ignoreCase 파이프라인)
- **internal.interval** (`io.bluetape4k.text.search.internal.interval`)
    - `src/main/kotlin/io/bluetape4k/text/search/internal/interval/Interval.kt`
    - `src/main/kotlin/io/bluetape4k/text/search/internal/interval/IntervalNode.kt`
    - `src/main/kotlin/io/bluetape4k/text/search/internal/interval/IntervalTree.kt`
    - `src/main/kotlin/io/bluetape4k/text/search/internal/interval/Intervalable.kt`
    - `src/main/kotlin/io/bluetape4k/text/search/internal/interval/IntervalableComparators.kt`

### 생성할 파일 (테스트)

- `src/test/resources/junit-platform.properties`
- `src/test/resources/logback-test.xml`
- `src/test/kotlin/io/bluetape4k/text/search/AbstractAhoCorasickTest.kt`
- `src/test/kotlin/io/bluetape4k/text/search/AhoCorasickAutomatonTest.kt`
- `src/test/kotlin/io/bluetape4k/text/search/AhoCorasickDslTest.kt`
- `src/test/kotlin/io/bluetape4k/text/search/AhoCorasickReplaceTest.kt`
- `src/test/kotlin/io/bluetape4k/text/search/AhoCorasickOptionsTest.kt`
- `src/test/kotlin/io/bluetape4k/text/search/AhoCorasickKoreanTest.kt`
- `src/test/kotlin/io/bluetape4k/text/search/AhoCorasickFirstMatchTest.kt`
- `src/test/kotlin/io/bluetape4k/text/search/AhoCorasickMigrationTest.kt`
- `src/test/kotlin/io/bluetape4k/text/search/AhoCorasickScenarioTest.kt`
- `src/test/kotlin/io/bluetape4k/text/search/flow/AhoCorasickFlowTest.kt`
- `src/test/kotlin/io/bluetape4k/text/search/internal/OffsetMappingTest.kt`

### 생성할 파일 (벤치마크)

- `src/benchmark/kotlin/io/bluetape4k/text/search/benchmark/AhoCorasickBenchmark.kt`

### 수정할 파일

- `README.md` — 모듈 표 `utils/` 행에 `text-search` 추가
- `README.ko.md` — 동일
- `docs/superpowers/index/2026-04.md` — spec/plan 항목 추가
- `docs/superpowers/INDEX.md` — count 갱신

### 삭제할 파일/디렉토리

- `x-obsoleted/ahocorasick/` (Task 13에서 git rm)

### 명시적 범위 규칙

- 이 작업은 **Testcontainers 가 필요 없는 순수 모듈 작업**이다. 외부 인프라 없이 JUnit + bluetape4k-assertions 만으로 검증한다.
- `.kt` 파일을 만들거나 수정할 때마다 `mcp__intellij-index__ide_diagnostics` 로 import/deprecation 문제를 확인하고, 필요 시 `ide_optimize_imports` 를 적용한 뒤에만 build/test 로 진행한다.
- `kotlinx_coroutines_core` / `bluetape4k-coroutines` 는 `compileOnly` 로 선언한다. 테스트에서는 `testImplementation(Libs.kotlinx_coroutines_test)` 가 transitive 로 가져온다.
- `kotlinx-benchmark` 는 monorepo 최초 적용 — `utils/batch/build.gradle.kts` 의 검증된 패턴을 그대로 모방한다 (`sourceSets { create("benchmark") }` + `compilations.getByName("benchmark").associateWith(...main)` + `allOpen { annotation("org.openjdk.jmh.annotations.State") }`). spec §7.5 의 PoC 단순화 버전은 채택하지 않는다.
- 벤치마크 파일은 `src/main` 이 아니라 `src/benchmark` 에 둔다 (utils/batch 와 동일). spec §7.5 의 `src/main` 권고는 utils/batch 검증 결과에 의해 override 된다.
- 결과/옵션 데이터 타입 (`AhoCorasickMatch<V>`, `SearchToken<V>`, `SearchOptions`) 은 `: java.io.Serializable` + `private const val serialVersionUID: Long = 1L` 을 가진다.
    - **⚠️ `AhoCorasickAutomaton<V>` 는 `Serializable`
      아님** — 내부 `TrieCore`/`State` 그래프가 failure link 순환 참조를 가져 Java 직렬화가 불가. spec §3.4 도 `Serializable` 요구 없음.
- `companion object : KLogging()` 은 **behavioral classes
  만** 적용한다: `AhoCorasickAutomaton`, `AhoCorasickBuilder`, `TrieCore`, `OffsetMapping.Companion`, `Normalizers` 등 logger 를 실제로 사용하는 클래스.
  **data class (`AhoCorasickMatch`, `SearchToken.Match/Fragment`, `SearchOptions`, `WordBoundary`, `NormalizationForm`)
  에는 KLogging 불필요** — logger 없는 value object 에 companion 추가는 project pattern 범위 초과 (`exposed.model` 패키지 전용 규칙임).

### 자료 검증 / 외부 의존성

- 구 `Trie.kt` 의 알고리즘은 그대로 유지 (검증된 구현). 패키지/이름만 변경.
- `kotlinx-benchmark` PoC 가 실패할 경우 — fallback 으로 spec §7.5 의 `@Tag("performance")` JUnit 측정 이 아니라 **utils/batch 패턴을
  디버깅** 한다. utils/batch 가 working state 이므로 동일 설정이 실패할 이유가 없다.

---

## Task Table of Contents

|   # | Task                                                                               | Complexity | Hours | Depends |
|----:|------------------------------------------------------------------------------------|:----------:|------:|:-------:|
|   1 | 모듈 골격 생성 (build.gradle.kts + benchmark 설정 + 테스트 리소스)                 |   medium   |   1.5 |    —    |
|   2 | 구 ahocorasick → internal 패키지 이전 (`Trie` → `TrieCore` rename, 컴파일 통과)    |   medium   |   1.0 |    1    |
|   3 | `SearchOptions` + `WordBoundary` + `NormalizationForm` (immutable)                 |    low     |   0.5 |    1    |
|   4 | `AhoCorasickMatch<V>` + `SearchToken<V>` sealed                                    |    low     |   0.5 |    1    |
|   5 | `AhoCorasickAutomaton<V>` 코어 (parseText / firstMatch / containsMatch / tokenize) |    high    |   2.0 |  2,3,4  |
|   6 | `replaceAll { ... }` 람다 구현 + 단위 테스트                                       |   medium   |   0.5 |    5    |
|   7 | DSL `ahoCorasick { }` + `ahoCorasickOf(...)` 헬퍼                                  |   medium   |   1.0 |    5    |
|   8 | Unicode normalization (NFC/NFKC) + `OffsetMapping` (원본 offset 보존)              |    high    |   3.0 |    5    |
|   9 | Flow `matchesAsFlow` (channelFlow + ensureActive 협력 취소)                        |    high    |   1.0 |    5    |
|  10 | 단위 / 옵션 매트릭스 / 한글 / Flow / scenario 테스트 (7 파일)                      |   medium   |   4.0 |   5–9   |
| 10b | `AhoCorasickBenchmark.kt` (JMH: parseText / matchesAsFlow / naive 3개)             |   medium   |   1.0 |   5–9   |
|  11 | 마이그레이션 동등성 테스트 (구 `TrieTest` 12 케이스 재작성)                        |   medium   |   1.5 |   10    |
|  12 | README.md + README.ko.md (Mermaid class + sequence diagram)                        |    low     |   1.5 |   5–9   |
|  13 | `x-obsoleted/ahocorasick/` git rm + 외부 참조 0 검증                               |    low     |   0.5 |   11    |
|  14 | 루트 README + superpowers index + `/wiki-update`                                   |    low     |   0.5 |  12,13  |
|  15 | OMC code-reviewer 실행 + HIGH/CRITICAL fix                                         |    high    |   1.0 |   14    |
|  16 | PR 생성 (테스트 결과 + 변경 사유 + verification commands)                          |    low     |   0.5 |   15    |

**Total estimate:** ~22 시간

**Complexity routing:**

- `high` → Opus (Task 5, 8, 9, 15) — 코어 알고리즘, OffsetMapping, Flow API, 코드리뷰 fix
- `medium` → Sonnet (Task 1, 2, 6, 7, 10, 10b, 11) — 모듈 스캐폴딩, DSL, 테스트
- `low` → Haiku (Task 3, 4, 12, 13, 14, 16) — 데이터 클래스, README, 정리, PR

---

## Task 1: 모듈 골격 생성 (build.gradle.kts + benchmark 설정 + 테스트 리소스)

- **Complexity:** medium
- **Estimated:** 1.5h
- **Files:**
    - Create: `utils/text-search/build.gradle.kts`
    - Create: `utils/text-search/src/test/resources/junit-platform.properties`
    - Create: `utils/text-search/src/test/resources/logback-test.xml`
    - Create: `utils/text-search/src/main/kotlin/.gitkeep` (Gradle 인식용 임시)
    - Create: `utils/text-search/src/test/kotlin/.gitkeep`
    - Create: `utils/text-search/src/benchmark/kotlin/.gitkeep`

### Steps

- [ ] **1.1** `utils/text-search/build.gradle.kts` 작성 — `utils/batch/build.gradle.kts` 패턴 차용:
    - `plugins { kotlin("plugin.allopen"); id(Plugins.kotlinx_benchmark) }`
    - `allOpen { annotation("org.openjdk.jmh.annotations.State") }`
    - `sourceSets { create("benchmark") }` + `kotlin.target.compilations.getByName("benchmark").associateWith(compilations.getByName("main"))`
    - `configurations { testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get()); named("benchmarkImplementation") { extendsFrom(implementation, compileOnly, testImplementation) } }`
    - `benchmark { targets { register("benchmark") { ... jmhVersion = Versions.jmh } } configurations { register("ahocorasick") { warmups=2; iterations=5; iterationTime=1; iterationTimeUnit="s"; mode="thrpt"; outputTimeUnit="s"; reportFormat="json" } } }`
    - dependencies:
      ```kotlin
      api(project(":bluetape4k-core"))
      compileOnly(project(":bluetape4k-coroutines"))
      compileOnly(Libs.kotlinx_coroutines_core)
  
      testImplementation(project(":bluetape4k-junit5"))
      testImplementation(project(":bluetape4k-coroutines"))
      testImplementation(Libs.kotlinx_coroutines_core)
      testImplementation(Libs.kotlinx_coroutines_test)
  
      add("benchmarkImplementation", Libs.kotlinx_benchmark_runtime)
      add("benchmarkImplementation", Libs.kotlinx_benchmark_runtime_jvm)
      add("benchmarkImplementation", Libs.jmh_core)
      ```
- [ ] **1.2** `src/test/resources/junit-platform.properties` (lingua plan 의 표준):
  ```
  junit.jupiter.extensions.autodetection.enabled=true
  junit.jupiter.testinstance.lifecycle.default=per_class
  junit.jupiter.execution.parallel.enabled=false
  junit.jupiter.execution.parallel.mode.default=same_thread
  junit.jupiter.execution.parallel.mode.classes.default=concurrent
  ```
- [ ] **1.3** `src/test/resources/logback-test.xml` — `io.bluetape4k.text.search` logger DEBUG, root INFO.
- [ ] **1.4** `./gradlew projects | rg "bluetape4k-text-search"` → 출력 확인.
- [ ] 
  **1.5** `./gradlew :bluetape4k-text-search:compileKotlin :bluetape4k-text-search:compileTestKotlin :bluetape4k-text-search:compileBenchmarkKotlin` → BUILD SUCCESSFUL (소스 0개 상태).
- [ ] 
  **1.6** kotlinx-benchmark 메타 task 노출 확인: `./gradlew :bluetape4k-text-search:tasks --group benchmark | rg "benchmark"`.

### Done when:

- `bluetape4k-text-search` 가 settings.gradle.kts 자동 등록 → `gradle projects` 에 노출.
- 빈 모듈 상태에서도 `compileKotlin/compileTestKotlin/compileBenchmarkKotlin` 모두 BUILD SUCCESSFUL.
- benchmark task 가 `./gradlew :bluetape4k-text-search:tasks` 출력에 등장.
- 커밋: `chore(text-search): utils/text-search 모듈 골격 추가 (kotlinx-benchmark 포함)` ※ 단, 첫 커밋은 Task 2 통과 후로 미룬다 (compile-only 스캐폴딩은 unstable).

---

## Task 2: 구 ahocorasick → internal 패키지 이전 (`Trie` → `TrieCore` rename)

- **Complexity:** medium
- **Estimated:** 1.0h
- **Files:**
    - Create: `utils/text-search/src/main/kotlin/io/bluetape4k/text/search/internal/TrieCore.kt` (구 `Trie.kt`)
    - Create: `utils/text-search/src/main/kotlin/io/bluetape4k/text/search/internal/State.kt`
    - Create: `utils/text-search/src/main/kotlin/io/bluetape4k/text/search/internal/Emit.kt`
    - Create: `utils/text-search/src/main/kotlin/io/bluetape4k/text/search/internal/EmitHandler.kt`
    - Create: `utils/text-search/src/main/kotlin/io/bluetape4k/text/search/internal/InternalToken.kt` (구 `Token.kt`)
    - Create: `utils/text-search/src/main/kotlin/io/bluetape4k/text/search/internal/interval/Interval.kt`
    - Create: `utils/text-search/src/main/kotlin/io/bluetape4k/text/search/internal/interval/IntervalNode.kt`
    - Create: `utils/text-search/src/main/kotlin/io/bluetape4k/text/search/internal/interval/IntervalTree.kt`
    - Create: `utils/text-search/src/main/kotlin/io/bluetape4k/text/search/internal/interval/Intervalable.kt`
    - Create: `utils/text-search/src/main/kotlin/io/bluetape4k/text/search/internal/interval/IntervalableComparators.kt`
    - (옵션) Create: `utils/text-search/src/main/kotlin/io/bluetape4k/text/search/internal/InternalTrieConfig.kt` (구 `TrieConfig.kt` — Task 5 에서 SearchOptions 로 대체되므로 internal 로만 잠시 유지하다 제거)

### Steps

- [ ] 
  **2.1** 11 개 source 파일을 `x-obsoleted/ahocorasick/src/main/kotlin/io/bluetape4k/ahocorasick/{trie,interval}/*.kt` 에서 새 패키지로 복사 (소스를 그대로 두고 `cp` — Task 13 에서 git rm).
- [ ] **2.2** package 선언 일괄 변경:
    - `io.bluetape4k.ahocorasick.trie` → `io.bluetape4k.text.search.internal`
    - `io.bluetape4k.ahocorasick.interval` → `io.bluetape4k.text.search.internal.interval`
- [ ] 
  **2.3** `class Trie` → `internal class TrieCore` rename (선언 + 자기 참조). `class State`, `data class Emit`, `class IntervalTree`, `class IntervalNode`, `class Interval`, `interface Intervalable`, `object IntervalableComparators`, `fun interface EmitHandler` 모두 `internal` 가시성으로 강등.
- [ ] **2.4** 구 `Token` (sealed class) → `internal sealed class InternalToken` rename. (외부에는 신 `SearchToken<V>` 만 노출됨.)
- [ ] **2.5** 구 `TrieConfig` 는 **Task 5 에서 TrieCore 가 직접 SearchOptions 를 받도록 시그니처
  변경** 하고 폐기한다. `InternalTrieConfig` 임시 래퍼 경로는 사용하지 않음 (Appendix B 항목 3).
- [ ] **2.6** import 정리: `mcp__intellij-index__ide_optimize_imports` 를 이전된 모든 파일에 적용.
- [ ] **2.7** `mcp__intellij-index__ide_diagnostics` 를 모든 파일에 실행, error 0 확인.
- [ ] **2.8** `./gradlew :bluetape4k-text-search:compileKotlin` → BUILD SUCCESSFUL.

### Done when:

- 신 `internal` 패키지 11 파일이 컴파일 통과.
- `rg "class Trie\b" utils/text-search/src/main` → 0 hits, `rg "class TrieCore\b"` → 1 hit.
- 모든 이전 클래스가 `internal` 가시성을 가져 외부 `io.bluetape4k.text.search.*` 사용자에게 노출되지 않음.
- 커밋: `feat(text-search): x-obsoleted/ahocorasick 을 internal 패키지로 이전 (Trie→TrieCore)`.

---

## Task 3: `SearchOptions` + `WordBoundary` + `NormalizationForm` (immutable)

- **Complexity:** low
- **Estimated:** 0.5h
- **Files:**
    - Create: `utils/text-search/src/main/kotlin/io/bluetape4k/text/search/SearchOptions.kt`

### Steps

- [ ] 
  **3.1** spec §3.3 의 enum 두 개 + data class `SearchOptions` 작성. 모든 필드 `val`. `Serializable` + `serialVersionUID = 1L` + `companion object : KLogging()`.
- [ ] **3.2** `SearchOptions` 의 KDoc 한글 작성: 각 필드의 의미/기본값/주의 (Flow API 에서 stopOnFirstMatch 무시) 설명.
- [ ] **3.3** `WordBoundary` 의 `LATIN_ALPHA` 가 한글에 잘못 적용될 수 있다는 경고 KDoc 추가 (Korean → `WHITESPACE_SEPARATED` 권장).
- [ ] **3.4** `ide_diagnostics` 실행 → 0 error.

### Done when:

- 3 개 public 타입 컴파일 통과.
- 모든 public 멤버에 한글 KDoc.
- `data class SearchOptions(...)` 가 `copy()` 호출 가능 (data class 기본 동작).

---

## Task 4: `AhoCorasickMatch<V>` + `SearchToken<V>` sealed

- **Complexity:** low
- **Estimated:** 0.5h
- **Files:**
    - Create: `utils/text-search/src/main/kotlin/io/bluetape4k/text/search/AhoCorasickMatch.kt`
    - Create: `utils/text-search/src/main/kotlin/io/bluetape4k/text/search/SearchToken.kt`

### Steps

- [ ] 
  **4.1** spec §3.2 그대로 작성. `data class AhoCorasickMatch<out V>(start, end, keyword, value): Serializable`. `length` 계산 프로퍼티. companion `KLogging` + `serialVersionUID`.
- [ ] 
  **4.2** `sealed interface SearchToken<out V>: Serializable` + `data class Match<out V>` + `data class Fragment` 분리.
    - `Fragment` 는 `SearchToken<Nothing>` 으로 variance 처리 — value 없이도 사용 가능.
- [ ] **4.3** R1 (kotlin.text.MatchResult 충돌) 회피 의도를 KDoc 에 명시.
- [ ] **4.4** `ide_diagnostics` 실행 → 0 error.

### Done when:

- 두 public 타입 컴파일 통과.
- `AhoCorasickMatch` 가 generic out-projected, Java 에서도 사용 가능 (`@JvmField` 없이도 reified V).
- KDoc 에 R1 회피 이유 (kotlin.text.MatchResult 충돌) 명시.

---

## Task 5: `AhoCorasickAutomaton<V>` 코어 (parseText / firstMatch / containsMatch / tokenize)

- **Complexity:** high (Opus 권장)
- **Estimated:** 2.0h
- **Files:**
    - Create: `utils/text-search/src/main/kotlin/io/bluetape4k/text/search/AhoCorasickAutomaton.kt`

### Steps

- [ ] **5.1** spec §3.4 시그니처 그대로 구현 (`Serializable` 제외 — 명시 규칙 참조):
  ```kotlin
  class AhoCorasickAutomaton<V> internal constructor(
      private val core: io.bluetape4k.text.search.internal.TrieCore,
      private val values: Map<String, V>,
      internal val options: SearchOptions,         // internal: Flow 확장(같은 모듈)에서 사용. public 비노출.
  ) {                                              // NOT Serializable — TrieCore 내부 순환 참조 때문
      companion object : KLogging() {
          @JvmStatic fun <V> builder(): Builder<V> = Builder()
      }
      ...
  }
  ```
    - `options` 는 `internal val` — spec §3.4 `private` 대비 visibility 완화이나 Flow 확장 사용을 위해 필요. Appendix B 에 기록.
- [ ] 
  **5.2** `parseText(text: CharSequence): List<AhoCorasickMatch<V>>` — 입력 텍스트에 정규화 + ignoreCase 변환 (Task 8 에서 OffsetMapping 도입 전에는 우선 일반 `text.toString()` 으로). 내부 `core.runParseText(text) { emit -> emits.add(emit) }` 호출 후 `Emit → AhoCorasickMatch<V>(start, end, keyword, values[keyword]!!)` 매핑.
- [ ] **5.3** `firstMatch(text: CharSequence): AhoCorasickMatch<V>?` — **R5 leftmost
  semantics**: `parseText(text).minByOrNull { it.start }` (start 가 같으면 더 긴 것이 우선 → secondary `-it.length`).
- [ ] **5.4** `containsMatch(text: CharSequence): Boolean` — `core.containsMatch(text)` 위임.
- [ ] 
  **5.5** `tokenize(text: CharSequence): List<SearchToken<V>>` — `parseText` 결과를 사용해 입력을 매치/비매치 구간으로 split. 매치 사이의 빈 구간도 빠짐없이 `Fragment` 로 emit.
- [ ] **5.6** `Builder<V>` 구현:
    - `add(keyword: String, value: V): Builder<V>` — `keyword.requireNotBlank("keyword")` 검증 (`io.bluetape4k.support.requireNotBlank`).
    - `addAll(map: Map<String, V>)`.
    - `options(options: SearchOptions)`.
    - `build(): AhoCorasickAutomaton<V>` — 내부적으로 `TrieCore` 인스턴스 생성, 모든 keyword 등록 후 `failureStates` 계산 (구 `Trie.constructFailureStates()` 호출), `values` map 구축, `AhoCorasickAutomaton(core, values, opts)` 반환.
- [ ] 
  **5.7** Builder 가 SearchOptions 의 ignoreCase / wordBoundary / allowOverlaps / stopOnFirstMatch 를 internal `TrieCore` 에 전달. (정규화는 Task 8 에서.)
- [ ] **5.8** 모든 public 멤버에 한글 KDoc + 사용 예제 1 줄 이상.
- [ ] **5.9** `replaceAll` 시그니처는 5.x 단계에서 stub 만 두고 Task 6 에서 본 구현. (또는 이 Task 에서 미선언 → Task 6 에서 추가.)
- [ ] **5.10** `ide_diagnostics` 실행 → 0 error. `./gradlew :bluetape4k-text-search:compileKotlin` BUILD SUCCESSFUL.

### Done when:

- `AhoCorasickAutomaton<V>` 컴파일 통과 + 4 개 메서드 (parseText/firstMatch/containsMatch/tokenize) 동작.
- Builder 가 1 개 keyword 등록 + build () → parseText 가 매치 1 개 반환하는 micro-test 가 inline `fun main` 에서 통과 (정식 단위 테스트는 Task 10).
- `firstMatch` 가 leftmost (start 기준 최소) 를 반환 — spec §7.6 R5 구현.

---

## Task 6: `replaceAll { ... }` 람다 구현 + 단위 테스트

- **Complexity:** medium
- **Estimated:** 0.5h
- **Files:**
    - Modify: `utils/text-search/src/main/kotlin/io/bluetape4k/text/search/AhoCorasickAutomaton.kt`
    - Create: `utils/text-search/src/test/kotlin/io/bluetape4k/text/search/AhoCorasickReplaceTest.kt`

### Steps

- [ ] **6.1** `fun replaceAll(text: CharSequence, transform: (AhoCorasickMatch<V>) -> CharSequence): String` 구현:
    - `parseText(text)` 호출 → 결과 list 가 비면 `text.toString()` 즉시 반환.
    - 결과를 `start ASC, length DESC` 정렬. (allowOverlaps=true 시 overlap 있는 매치는 첫 매치만 적용 — `previousEnd` 까지 skip.)
    - `StringBuilder` 로 비매치 구간 + `transform(match)` 결과를 순차 append, 마지막 꼬리 append.
    - 단, `allowOverlaps=false` 인 경우 IntervalTree 가 이미 non-overlapping 을 보장 → skip 로직 불필요.
- [ ] **6.2** `AhoCorasickReplaceTest.kt` 단위 테스트:
    - 기본 마스킹: `ac.replaceAll("APPL HQ in NYC") { "*".repeat(it.keyword.length) }` → 정확한 출력 검증.
    - `transform` 이 `match.value` 를 사용 (예: 약어 → 풀네임 치환).
    - 매치가 0 개일 때 입력 그대로 반환.
    - overlapping 매치 처리 (allowOverlaps=true 인데 단일 결과만 치환되도록).
    - 한글 입력 + ascii keyword 혼합.

### Done when:

- `replaceAll` 동작 확인 (5 개 케이스).
- `./gradlew :bluetape4k-text-search:test --tests "*AhoCorasickReplaceTest"` PASS.

---

## Task 7: DSL `ahoCorasick { }` + `ahoCorasickOf(...)` 헬퍼

- **Complexity:** medium
- **Estimated:** 1.0h
- **Files:**
    - Create: `utils/text-search/src/main/kotlin/io/bluetape4k/text/search/AhoCorasickBuilder.kt`
    - Create: `utils/text-search/src/test/kotlin/io/bluetape4k/text/search/AhoCorasickDslTest.kt`

### Steps

- [ ] **7.1** spec §4 그대로 구현:
    - `@DslMarker annotation class AhoCorasickDsl`.
    - `@AhoCorasickDsl class AhoCorasickBuilder<V> internal constructor()` — `companion object : KLogging()` 포함.
    - 5 개 mutable property (ignoreCase, allowOverlaps, wordBoundary, normalization, stopOnFirstMatch) 기본값 spec §3.3 과 동일.
    - `fun keyword(keyword: String, value: V)` — **`keyword.requireNotBlank("keyword")` 검증
      포함** (Builder.add 와 동일한 검증). `keywords(vararg pairs)`, `keywords(map)` 도 동일 검증.
    - **value-less 오버로드 없음** — type-safe 위반 (UNCHECKED_CAST) 회피.
    - `internal fun build()` — `AhoCorasickAutomaton.builder<V>().addAll(entries).options(SearchOptions(...)).build()`.
- [ ] **7.2** top-level `fun <V> ahoCorasick(block: AhoCorasickBuilder<V>.() -> Unit): AhoCorasickAutomaton<V>` 함수.
- [ ] 
  **7.3** `ahoCorasickOf(vararg keywords: String, options: SearchOptions = SearchOptions()): AhoCorasickAutomaton<String>` + `ahoCorasickOf(keywords: Collection<String>, options: SearchOptions): AhoCorasickAutomaton<String>` (spec §3.5).
- [ ] **7.4** `AhoCorasickDslTest.kt`:
    - DSL 로 옵션 + keyword 5 개 등록 → parseText 결과 검증.
    - `ahoCorasickOf("a", "b", "c")` 헬퍼 → keyword==value 검증.
    - DslMarker 가 nested 빌더 충돌 차단하는지 (compile-time, KDoc 만으로 충분).
    - Map keyword 등록 검증.
    - **blank keyword → IllegalArgumentException** (DSL `keyword("  ", ...)` 호출 시) — Builder.add 와 동일 검증 경로 확인.
- [ ] 
  **7.5** spec §3.7 의 6 가지 시나리오 중 4-옵션 동시 적용 / Builder Java interop / tokenize 후처리 — 이 테스트에 inline 으로 추가 (나머지 3 개는 Task 10 의 OptionsTest / FlowTest / KoreanTest 에서 다룸).

### Done when:

- DSL/헬퍼 4 개 케이스 + spec §3.7 시나리오 3 개 PASS.
- `./gradlew :bluetape4k-text-search:test --tests "*AhoCorasickDslTest"` PASS.

---

## Task 8: Unicode normalization (NFC/NFKC) + `OffsetMapping`

- **Complexity:** high (Opus 권장)
- **Estimated:** 3.0h
- **Files:**
    - Create: `utils/text-search/src/main/kotlin/io/bluetape4k/text/search/internal/Normalizers.kt`
    - Create: `utils/text-search/src/main/kotlin/io/bluetape4k/text/search/internal/OffsetMapping.kt`
    - Modify: `utils/text-search/src/main/kotlin/io/bluetape4k/text/search/AhoCorasickAutomaton.kt`
    - Modify: `utils/text-search/src/main/kotlin/io/bluetape4k/text/search/AhoCorasickBuilder.kt`
    - Create: `utils/text-search/src/test/kotlin/io/bluetape4k/text/search/internal/OffsetMappingTest.kt`
    - Create: `utils/text-search/src/test/kotlin/io/bluetape4k/text/search/AhoCorasickKoreanTest.kt`

### Steps

- [ ] **8.1** `Normalizers.kt`: `internal fun applyPipeline(s: CharSequence, opts: SearchOptions): String` — 순서 =
  **정규화 (NFC/NFKC) → ignoreCase 적용 (
  Locale-independent: `String.lowercase(Locale.ROOT)`)** (spec §6 invariant: 정규화 후 lowercase). `NormalizationForm.NONE` 일 때 정규화 skip.
- [ ] **8.2** `OffsetMapping.kt`:
  ```kotlin
  internal class OffsetMapping(original: CharSequence, normalized: CharSequence) {
      // normToOrig[i] = normalized[i] 가 original 의 어느 position 에서 유래했는가
      private val normToOrig: IntArray
      private val origToNorm: IntArray
      fun toOriginal(normOffset: Int): Int
      fun toOriginalEndExclusive(normEndInclusive: Int): Int  // end 는 exclusive 로 변환 후 -1 보정
      fun toNormalized(origOffset: Int): Int

      companion object {
          fun build(original: CharSequence, form: NormalizationForm): Pair<String, OffsetMapping?>
      }
  }
  ```
    - `build` 알고리즘: `Normalizer.normalize(original, form)` 를 호출하기 전에 **char 단위
      sliding** 으로 각 원본 char 를 단독 정규화한 결과의 길이를 누적해 매핑 배열을 구축한다 (전체 한 번 normalize 와 결과는 동일하지만 offset 추적 가능).
    - NFC/NFKC 가 길이를 변경할 수 있는 케이스: `㈜` (NFKC) → `(주)` (+2 chars), 자모 분리 `ㄴㅏ` (NFC) → `나` (-1 char).
    - `NormalizationForm.NONE` → `OffsetMapping.identity(length)` 반환 (zero-cost wrapper).
- [ ] **8.3** `Builder.add(keyword, value)` 가 정규화 적용 후 internal map 에 등록:
    - `val normalized = applyPipeline(keyword, options)`.
    - `core.addKeyword(normalized)`, `values[normalized] = value`.
- [ ] **8.4** `AhoCorasickAutomaton.parseText/tokenize/replaceAll/firstMatch/containsMatch` 가 입력 텍스트 진입 시:
    - `val (norm, mapping) = OffsetMapping.build(text, options.normalization)`.
    - `applyPipeline(norm, options)` 로 ignoreCase 적용 (이미 build 안에서 함께 처리해도 됨).
    - 내부 `TrieCore.runParseText(norm) { emit -> ... }` 호출.
    - 결과 `Emit(start, end)` 의 offset 을 `mapping.toOriginal(start)` / `mapping.toOriginalEndExclusive(end + 1) - 1` 로 변환해 `AhoCorasickMatch.start/end` 에 저장 (원본 offset 기준).
    - `AhoCorasickMatch.keyword` 는 정규화된 키 그대로 (Task 7 의 KDoc 에 "정규화된 키워드" 로 명시).
- [ ] **8.5** `replaceAll` 의 비매치 구간 추출은 **원본
  텍스트** 로부터 `text.subSequence(prevEnd+1, match.start)` — 정규화로 변형된 텍스트가 사용자에게 보이지 않도록.
- [ ] **8.6** `OffsetMappingTest.kt` 단위 테스트:
    - identity (NONE) — toOriginal (i)==i.
    - NFC: 자모 분리 `ㄴㅏ나ㄷㅏ나` → `나나다나` 매핑 검증 (length 감소).
    - NFKC: `㈜삼성` → `(주)삼성` 매핑 검증 (length 증가).
    - 빈 문자열 / 단일 char / ascii-only (정규화 무영향).
    - end-exclusive 변환 invariant: `toOriginalEndExclusive(toNormalized(origEnd+1)) - 1 == origEnd`.
- [ ] **8.7** `AhoCorasickKoreanTest.kt`:
    - NFC + 자모 분리 입력에서 `욕설1` 매치 (자모 분리 `ㅇㅛㄱ설1` → 정규화 후 매치, 원본 offset 반환).
    - NFC + `WHITESPACE_SEPARATED` + `"오늘 욕설1 했다"` → 단일 매치 + 원본 offset (3..6).
    - NFKC + `㈜` 정규화 후 `(주)` keyword 매치.
    - LATIN_ALPHA boundary 가 한글에 잘못 적용되는 edge case 1 개 → KDoc 권장사항 검증.
    - 한글 + replaceAll 마스킹 (`***`).
- [ ] 
  **8.8** `ide_diagnostics` + `./gradlew :bluetape4k-text-search:test --tests "*Korean*" --tests "*OffsetMapping*"` PASS.

### Done when:

- `OffsetMapping` 단위 테스트 5 개 PASS.
- 한글 시나리오 5 개 PASS.
- spec §6 invariant 충족: 키워드 + 입력 양쪽에 동일 form 적용, 결과 offset 은 항상 원본 텍스트 기준.
- 정규화 → lowercase 순서 검증 테스트 1 개 추가 (예: `İ` (Turkish I) edge case 는 NFC 후 lowercase).

---

## Task 9: Flow `matchesAsFlow` (channelFlow + ensureActive 협력 취소)

- **Complexity:** high (Opus 권장)
- **Estimated:** 1.0h
- **Files:**
    - Create: `utils/text-search/src/main/kotlin/io/bluetape4k/text/search/flow/AhoCorasickFlowExtensions.kt`
    - Create: `utils/text-search/src/test/kotlin/io/bluetape4k/text/search/flow/AhoCorasickFlowTest.kt`

### Steps

- [ ] **9.1** spec §5 시그니처 + **`channelFlow` 패턴으로 구현** (R6 메모리 절감, `flow { }` 와 혼용 금지):
  ```kotlin
  fun <V> AhoCorasickAutomaton<V>.matchesAsFlow(text: CharSequence): Flow<AhoCorasickMatch<V>> =
      channelFlow {
          val matches = parseText(text)          // allowOverlaps 여부와 무관하게 parseText 위임
          for (match in matches) {
              currentCoroutineContext().ensureActive()
              send(match)
          }
      }.flowOn(Dispatchers.Default)              // CPU-bound 격리 (성능 측정 후 조정 가능)
  ```
    - `allowOverlaps=false` 시 `parseText` 가 이미 IntervalTree 로 필터링 → channelFlow 단일 경로.
    - streaming 버전 (EmitHandler 콜백 직접 사용) 은 구현 복잡도 대비 이점이 없어 포기. Task 10b benchmark 로 성능 검증.
- [ ] **9.2** `AhoCorasickAutomaton.options` 는 Task 5.1 에서 `internal val` 로 선언됐으므로 별도 추가 불필요.
- [ ] **9.3** `AhoCorasickFlowTest.kt`:
    - 정상 collect — `runTest(timeout = 30.seconds)` 로 모든 매치 수집.
    - `take(2)` 로 조기 종료 — producer 가 cancel 되는지 확인 (R4).
    - 빈 text → empty Flow.
    - `allowOverlaps = false` → IntervalTree 결과를 일괄 emit (length 검증).
    - 1 만 매치 throughput micro-test (단순 측정, assert 없이 로그 출력).
    - `currentCoroutineContext().cancel()` 후 `ensureActive()` 가 CancellationException 던지는지.
    - `SearchOptions.stopOnFirstMatch=true` 가 Flow API 에서 무시되는지 (take (1) 와의 동작 비교).
- [ ] **9.4** `ide_diagnostics` + `./gradlew :bluetape4k-text-search:test --tests "*FlowTest"` PASS.

### Done when:

- Flow 7 개 케이스 PASS.
- `take(N)` 조기 종료 시 producer 가 즉시 멈춤 (CancellationException 정상 throw).
- spec §5 의 R4 (stopOnFirstMatch 무시 + 협력 취소만 사용) 검증.

---

## Task 10: 단위 / 옵션 매트릭스 / 한글 / Flow / scenario 테스트 (7 파일)

- **Complexity:** medium
- **Estimated:** 4.0h
- **Files:**
    - Create: `utils/text-search/src/test/kotlin/io/bluetape4k/text/search/AbstractAhoCorasickTest.kt`
    - Create: `utils/text-search/src/test/kotlin/io/bluetape4k/text/search/AhoCorasickAutomatonTest.kt`
    - Create: `utils/text-search/src/test/kotlin/io/bluetape4k/text/search/AhoCorasickOptionsTest.kt`
    - Create: `utils/text-search/src/test/kotlin/io/bluetape4k/text/search/AhoCorasickFirstMatchTest.kt`
    - Create: `utils/text-search/src/test/kotlin/io/bluetape4k/text/search/AhoCorasickScenarioTest.kt`
    - (이미 Task 6/7/8/9 에서 생성: ReplaceTest, DslTest, KoreanTest, FlowTest)

### Steps

- [ ] 
  **10.1** `AbstractAhoCorasickTest.kt`: `companion object: KLogging()`, 공통 헬퍼 (예: `expectMatches(actual, vararg expected: Triple<Int,Int,String>)`).
- [ ] **10.2** `AhoCorasickAutomatonTest.kt` — 기본 동작:
    - `parseText` 빈 keyword set → empty result.
    - `parseText` 빈 input → empty result.
    - `parseText` 단일 keyword 매치 (start/end/keyword/value 검증).
    - `parseText` 다중 매치 정렬 순서.
    - `containsMatch` true/false.
    - `tokenize` Match + Fragment 정확히 alternating.
    - Builder.add 빈 keyword → IllegalArgumentException.
    - Serializable round-trip: `AhoCorasickMatch` / `SearchToken.Match` / `SearchToken.Fragment` / `SearchOptions` 직렬화 + 역직렬화 후 동등성 검증 (ObjectOutputStream → ByteArrayOutputStream → ObjectInputStream).
- [ ] **10.3** `AhoCorasickOptionsTest.kt` — spec §7.2 매트릭스 6 행 모두 (NFC 행은 Korean 으로 위임 가능):
  | # | ignoreCase | allowOverlaps | wordBoundary | normalization | 기대 | | 1 | F | T | NONE | NONE | overlap 포함 모든 매치 | | 2 | T | T | NONE | NONE | 대소문자 무시 | | 3 | F | F | NONE | NONE | IntervalTree 가 큰 키워드 우선 | | 4 | F | T | LATIN_ALPHA | NONE | 부분 단어 제외 | | 5 | F | T | WHITESPACE_SEPARATED | NONE | 공백 경계만 | | 6 | F | T | NONE | NONE | stopOnFirstMatch=T (blocking) → 첫 매치 후 중단 | 각 행 1 개 이상 케이스. (NFC 행은 Task 8 의 KoreanTest 에서 커버.)
- [ ] **10.4** `AhoCorasickFirstMatchTest.kt` — R5 leftmost semantics:
    - `"ushers"` + `["he","she","hers"]` (allowOverlaps=true) → `firstMatch == AhoCorasickMatch(start=1, end=3, "she", ...)` (구 API 와 다름 — `he` 가 아님).
    - 동일 start 면 더 긴 keyword 우선.
    - 매치 없음 → null.
    - allowOverlaps=false 와 true 동작 비교.
- [ ] **10.5** `AhoCorasickScenarioTest.kt` — spec §7.7 의 5 개 시나리오:
    1. **금칙어 검열** — 한글 욕설 사전 100 개, replaceAll 마스킹 검증 (출력에 욕설 0 hits).
    2. **자동완성 사전** — 제품명 1,000 개 사전, 긴 문서에서 추출 후 distinct + 정렬 검증.
    3. **로그 키워드 알람** — ERROR/WARN/FATAL 패턴, `matchesAsFlow + take(1)` 으로 첫 매치만 가져오기 + producer 취소 검증.
    4. **URL 추출** — `http://`, `https://`, `ftp://`, `wordBoundary=NONE`, parseText 결과 검증.
    5. **코드 키워드
       highlight** — Kotlin 예약어 50 개 (val/var/fun/class/...), `ignoreCase=false`, tokenize → HTML 변환 (`<b>...</b>`) 후 문자열 비교.
- [ ] **10.6** `./gradlew :bluetape4k-text-search:test` 전체 PASS, ≥ 50 개 케이스, JaCoCo coverage **≥ 80% (binding — spec §9
  DoD + 프로젝트 표준)**.
- [ ] 
  **10.7** `./gradlew :bluetape4k-text-search:detekt` PASS. `TrieCore` 길이 초과 시 `@Suppress("LongClass")` 추가 + KDoc 에 사유 기재.

### Done when:

- 7 개 테스트 파일 모두 작성, `:bluetape4k-text-search:test` BUILD SUCCESSFUL.
- 옵션 매트릭스 6 행 + stopOnFirstMatch 행 (7행) 모두 커버.
- 시나리오 5 개 모두 end-to-end PASS.
- Serializable round-trip 4 타입 모두 PASS.
- JaCoCo coverage ≥ 80%.
- `./gradlew :bluetape4k-text-search:detekt` PASS.
- 커밋: `test(text-search): 전체 테스트 suite 추가 (옵션매트릭스/시나리오/직렬화/Flow)`.

---

## Task 10b: `AhoCorasickBenchmark.kt` (JMH: parseText / matchesAsFlow / naive)

- **Complexity:** medium
- **Estimated:** 1.0h
- **Files:**
    - Create: `utils/text-search/src/benchmark/kotlin/io/bluetape4k/text/search/benchmark/AhoCorasickBenchmark.kt`

### Steps

- [ ] **10b.1** `@State(Scope.Benchmark) open class AhoCorasickBenchmark` — spec §7.5 sketch 그대로:
  ```kotlin
  @State(Scope.Benchmark)
  open class AhoCorasickBenchmark {
      lateinit var matcher: AhoCorasickAutomaton<String>
      lateinit var keywords: List<String>
      lateinit var largeText: String

      @Setup
      fun setup() {
          keywords = (1..10_000).map { "keyword$it" }
          matcher = ahoCorasickOf(keywords)
          largeText = buildString {
              repeat(10_000) { append("some keyword${it % 10_000} text ") }
          }
      }

      @Benchmark
      fun parseText(): List<AhoCorasickMatch<String>> = matcher.parseText(largeText)

      @Benchmark
      fun matchesAsFlowCollect(): Int =
          runBlocking { matcher.matchesAsFlow(largeText).toList().size }

      @Benchmark
      fun naiveContains(): Int =
          keywords.count { it in largeText }
  }
  ```
- [ ] **10b.2** `./gradlew :bluetape4k-text-search:benchmarkClasses` BUILD SUCCESSFUL.
- [ ] 
  **10b.3** `./gradlew :bluetape4k-text-search:ahocorasickBenchmark` (Task 1.1 의 register 명) 1 회 실행, JMH JSON 결과 확인 (`build/reports/benchmarks/ahocorasick/*.json`).
- [ ] **10b.4** ops/s 결과 3 개 (parseText / matchesAsFlow / naive) 를 README 에 기록할 수 있는 형태로 추출 — Task 12 에서 README 갱신.
- [ ] 
  **10b.5** 만약 kotlinx-benchmark 설정이 실패하면, utils/batch 의 build.gradle.kts 를 1:1 비교 + `./gradlew :bluetape4k-batch:tasks --group benchmark` 로 reference 동작 재확인. spec §7.5 의 fallback (`@Tag("performance")` JUnit) 은 사용하지 않는다.

### Done when:

- `AhoCorasickBenchmark` 클래스 컴파일 통과.
- benchmark 1 회 실행 성공, JSON 결과 생성.
- 3 개 ops/s 수치 확보 (README 에 기록할 준비).

---

## Task 11: 마이그레이션 동등성 테스트 (구 `TrieTest` 12 케이스 재작성)

- **Complexity:** medium
- **Estimated:** 1.5h
- **Files:**
    - Create: `utils/text-search/src/test/kotlin/io/bluetape4k/text/search/AhoCorasickMigrationTest.kt`

### Steps

- [ ] 
  **11.1** spec §7.6 의 12 케이스 재작성. 각 case 마다 `@Test fun`. 구 `TrieTest.kt` 를 `x-obsoleted/ahocorasick/src/test/kotlin/io/bluetape4k/ahocorasick/trie/TrieTest.kt` 에서 참고:

  | # | 케이스                                  | 구→신 매핑 / 비고 |
      |---|----------------------------------------|-----------------|
  | 1 | keyword and text are same              | `ahoCorasickOf("hello")` + `"hello"` → 1 매치 |
  | 2 | ushers overlaps (he/she/hers)           | `parseText("ushers")` → 3 매치 (he, she, hers) |
  | 3 | food recipes                            | 다중 keyword + 다중 매치 |
  | 4 | start of churchill speech               | 긴 텍스트 + 다중 매치 정렬 |
  | 5 | partial match exclusion (onlyWholeWords)| `wordBoundary = LATIN_ALPHA` 로 변경 |
  | 6 | tokenize full sentence                  | `tokenize` 결과 정확히 같은 sequence |
  | 7 | ignoreCase                              | `SearchOptions(ignoreCase=true)` |
  | 8 | replace with map                        | `replaceAll { match -> map[match.keyword]!! }` 람다로 재작성 |
  | 9 | containsMatch                           | 동일 |
  | 10| ignoreOverlaps (IntervalTree)           | `allowOverlaps=false` → 큰 keyword 우선 |
  | 11| **firstMatch — ushers**                 | ⚠️ R5: 신 API 는 `she`(start=1) 반환, 구 API 는 `he`(start=2) 였음. 신 동작을 정답으로 단정. |
  | 12| **firstMatch — unicode**                | ⚠️ R5 동일. 신 leftmost 동작 검증. |

- [ ] **11.2** 케이스 11/12 의 KDoc 에 "구 API 와 의도적으로 다른 leftmost semantics — spec R5/§7.6 참조" 기재.
- [ ] **11.3** `./gradlew :bluetape4k-text-search:test --tests "*MigrationTest"` PASS.

### Done when:

- 12 케이스 모두 PASS (11/12 는 신 동작 기준).
- 구 동작과 의도적으로 다른 2 개 케이스의 사유가 KDoc 에 명시.

---

## Task 12: README.md + README.ko.md (Mermaid class + sequence diagram)

- **Complexity:** low
- **Estimated:** 1.5h
- **Files:**
    - Create: `utils/text-search/README.md`
    - Create: `utils/text-search/README.ko.md`

### Steps

- [ ] **12.1** README 표준 구조 (Architecture → UML → Features → Examples → Benchmark):
    - **Architecture** — Aho-Corasick automaton 짧은 설명 + 모듈 책임.
    - **UML** —
        - Mermaid `classDiagram`: `AhoCorasickAutomaton<V>`, `AhoCorasickMatch<V>`, `SearchToken<V>` (Match/Fragment), `SearchOptions`, `WordBoundary`, `NormalizationForm`, `AhoCorasickBuilder<V>`.
        - Mermaid `sequenceDiagram`: User → Builder.add → AhoCorasickAutomaton → TrieCore → Emit → AhoCorasickMatch.
    - **Features**:
        - `parseText` / `firstMatch` (leftmost) / `containsMatch` / `tokenize`
        - `replaceAll { ... }` 람다
        - DSL `ahoCorasick { }` + `ahoCorasickOf(...)`
        - `Flow<AhoCorasickMatch<V>>` (matchesAsFlow, 협력 취소)
        - Unicode normalization (NFC/NFKC, 한글 권장 NFC)
        - Word boundary (LATIN_ALPHA / WHITESPACE_SEPARATED / NONE)
        - Java interop Builder
    - **Examples** — spec §3.7 의 6 가지 시나리오 코드 블록 그대로 게재.
    - **Benchmark** — Task 10b 의 ops/s 결과 표:
      ```
      | Method               | ops/s | 상대 |
      |----------------------|-------|------|
      | parseText            | X.XXe6| 1.0  |
      | matchesAsFlowCollect | Y.YYe6| 0.X  |
      | naiveContains        | Z.ZZe5| 0.0X |
      ```
- [ ] **12.2** `README.md` (영문) — 동일 구조, 영문 본문, 예제 코드는 KDoc 한글이라도 영문 주석 권장.
- [ ] **12.3** README 상단에 언어 전환 링크: `한국어 | [English](./README.md)` / `[한국어](./README.ko.md) | English`.
- [ ] **12.4** Vega-Lite 사용 금지 (memory). Mermaid 만 사용. xychart-beta 도 가능.
- [ ] **12.5** `bat utils/text-search/README.ko.md` 로 사람이 읽을 수 있는지 검토.

### Done when:

- 두 README 가 작성되고, Mermaid 다이어그램 2 개 (class + sequence) 포함.
- spec §3.7 의 6 가지 예제가 모두 README 에 게재.
- Benchmark 표가 실측 ops/s 로 채워짐.

---

## Task 13: `x-obsoleted/ahocorasick/` git rm + 외부 참조 0 검증

- **Complexity:** low
- **Estimated:** 0.5h
- **Files:**
    - Delete: `x-obsoleted/ahocorasick/` (전체 디렉토리)

### Steps

- [ ] **13.1** 외부 참조 검증:
  ```bash
  rg 'io\.bluetape4k\.ahocorasick' \
    --glob '!docs/**' \
    --glob '!.worktrees/**' \
    --glob '!x-obsoleted/**'
  ```
  Expected: 0 hits.
- [ ] 
  **13.2** `bluetape4k-bom`, settings.gradle.kts, root build 등에 `ahocorasick` 또는 `bluetape4k-ahocorasick` 참조 없는지 추가 확인:
  ```bash
  rg 'ahocorasick' --glob '!docs/**' --glob '!.worktrees/**' --glob '!x-obsoleted/**'
  ```
- [ ] **13.3** `git rm -r x-obsoleted/ahocorasick/` 로 삭제.
- [ ] **13.4** `./gradlew clean build -x test` BUILD SUCCESSFUL (참조 0 의 안전망).
- [ ] 
  **13.4b** BOM 검증: `./gradlew :bluetape4k-bom:generatePomFileForBluetape4kPublication` 후 `rg 'bluetape4k-text-search' bluetape4k/bom/build/publications/` → 1 hit 확인. 없으면 BOM `build.gradle.kts` 수동 추가.
- [ ] **13.5** 커밋: `chore(text-search): x-obsoleted/ahocorasick 제거 (utils/text-search 로 승격)`.

### Done when:

- `rg 'io\.bluetape4k\.ahocorasick'` 0 hits (allowed dirs 제외).
- `x-obsoleted/ahocorasick/` 디렉토리 부재.
- 전체 build BUILD SUCCESSFUL.

---

## Task 14: 루트 README + superpowers index + `/wiki-update`

- **Complexity:** low
- **Estimated:** 0.5h
- **Files:**
    - Modify: `README.md` (root)
    - Modify: `README.ko.md` (root)
    - Modify: `docs/superpowers/index/2026-04.md`
    - Modify: `docs/superpowers/INDEX.md`

### Steps

- [ ] **14.1** 루트 `README.md` / `README.ko.md` 의 Module Groups 표 `utils/` 행에 `text-search` 추가 (CLAUDE.md 의 표와 동일 형식).
- [ ] **14.1b** 루트 `CLAUDE.md` 의 Module Groups 표 `utils/` 행에 `text-search` 추가.
- [ ] **14.2** `docs/superpowers/index/2026-04.md` 맨 위에 spec + plan 항목 추가:
  ```
  | 2026-04-26 | utils-text-search | spec | docs/superpowers/specs/2026-04-26-utils-text-search-design.md |
  | 2026-04-26 | utils-text-search | plan | docs/superpowers/plans/2026-04-26-utils-text-search-plan.md |
  ```
- [ ] **14.3** `docs/superpowers/INDEX.md` 의 count 갱신.
- [ ] **14.4** `/oh-my-claudecode:wiki-update` (또는 `wiki-update` 스킬) 실행 — Obsidian wiki 페이지 동기화 + GNO 재인덱싱.
- [ ] **14.5** 커밋: `docs(text-search): 루트 README + superpowers index 갱신`.

### Done when:

- 4 개 문서 갱신 완료.
- `/wiki-update` 정상 종료.

---

## Task 15: OMC code-reviewer 실행 + HIGH/CRITICAL fix

- **Complexity:** high (Opus 권장)
- **Estimated:** 1.0h

### Steps

- [ ] **15.1** `/oh-my-claudecode:code-reviewer` (또는 `pr-review-toolkit:code-reviewer`) 스킬을 변경된 파일 전체에 대해 실행.
- [ ] 
  **15.1b** `bluetape4k-patterns` checklist 실행 — argument validation / logging / magic literal / AtomicFU / exception handling 위반 0 확인.
- [ ] 
  **15.1c** KDoc completeness 집계: `rg 'fun [a-zA-Z]|val [a-zA-Z]|class [A-Z]' utils/text-search/src/main/kotlin` 로 public 멤버 목록 추출 → KDoc `/**` 없는 멤버 0 확인.
- [ ] **15.2** HIGH/CRITICAL 이슈 0 으로 만들기 — MEDIUM 도 가능하면 fix.
- [ ] **15.3** 보안 review 별도 실행 (정규식/입력 검증/리소스 leak):
    - `keyword.requireNotBlank` 가 모든 진입점 (`Builder.add`, DSL `keyword()`) 에서 호출되는지 재확인.
    - `replaceAll` 의 transform 이 throw 시 자원 leak 없는지.
    - Flow 의 producer 가 cancel 시 thread 누수 없는지.
- [ ] **15.4** 모든 fix 후 `./gradlew :bluetape4k-text-search:test :bluetape4k-text-search:detekt` PASS.
- [ ] 
  **15.5b** testlog 기록: `docs/testlogs/2026-04.md` 맨 위에 새 행 추가 (형식: `| 날짜 | 작업 | :bluetape4k-text-search | N passing, M skipped | ✅ | Xs | notes |`).
- [ ] **15.5** 커밋: `fix(text-search): code review HIGH/CRITICAL 이슈 해소`.

### Done when:

- code-reviewer 산출물 (markdown/json) 에 HIGH/CRITICAL 0 건.
- 모든 추가 fix 가 commit 되고 test PASS.

---

## Task 16: PR 생성 (테스트 결과 + 변경 사유 + verification commands)

- **Complexity:** low
- **Estimated:** 0.5h

### Steps

- [ ] **16.1** PR 생성 전 final 테스트:
  ```bash
  ./gradlew :bluetape4k-text-search:test
  ./gradlew :bluetape4k-text-search:build
  ./gradlew :bluetape4k-text-search:ahocorasickBenchmark   # benchmark 회귀 확인
  ```
  passing count + duration 기록.
- [ ] **16.2** PR description 에 다음 포함:
    - **변경 사유** — spec link + 핵심 결정 (R1/R2/R3/R4/R5/R6 요약).
    - **테스트 결과** — 전체 test count + PASS, benchmark 3 개 ops/s.
    - **API 변경** — spec §3.6 의 구→신 매핑 표 그대로.
    - **Migration** — x-obsoleted/ahocorasick git rm, 외부 참조 0.
    - **Breaking changes** — `firstMatch` semantics R5, `TrieConfig` immutable.
    - **Verification commands** — 위 16.1 그대로.
    - **Module group 표** 갱신, README 영/한 모두 갱신.
- [ ] 
  **16.3** `gh pr create --title "feat(text-search): utils/text-search 모듈 추가 (Aho-Corasick automaton + Coroutines Flow + Unicode 정규화)" --base develop --body-file <(작성한 description)`.
- [ ] **16.4** PR # 을 메모리에 기록 (`mcp__plugin_oh-my-claudecode_t__project_memory_add_note`).

### Done when:

- PR 생성, CI 트리거.
- 모든 사전체크리스트 항목 (CLAUDE.md "Before Creating a PR") 충족.

---

## 부록 A. spec § 매핑 (검증용)

| spec §                 | 다루는 task                                        | 검증 방법                                    |
|------------------------|----------------------------------------------------|----------------------------------------------|
| §3 API design          | Task 3, 4, 5                                       | unit test, KDoc 확인                         |
| §3.7 6개 시나리오      | Task 7 (3개) + Task 8 (NFKC) + Task 10 (옵션/Flow) | scenario test 통과                           |
| §4 DSL                 | Task 7                                             | `AhoCorasickDslTest`                         |
| §5 Flow                | Task 9                                             | `AhoCorasickFlowTest`                        |
| §6 Unicode             | Task 8                                             | `AhoCorasickKoreanTest`, `OffsetMappingTest` |
| §7.2 옵션 매트릭스 7행 | Task 10                                            | `AhoCorasickOptionsTest`                     |
| §7.5 Benchmark         | Task 1 (build setup) + Task 10b                    | `AhoCorasickBenchmark` JSON                  |
| §7.6 마이그레이션 12   | Task 11                                            | `AhoCorasickMigrationTest`                   |
| §7.7 시나리오 5        | Task 10                                            | `AhoCorasickScenarioTest`                    |

### §3.7 시나리오 커버리지 맵

| #            | 시나리오                                                 | Task                  | 테스트 파일                                      | 테스트 메서드                              |
|--------------|----------------------------------------------------------|-----------------------|--------------------------------------------------|--------------------------------------------|
| 1            | 4-옵션 동시 적용 (ignoreCase+wordBoundary+overlaps+stop) | Task 7.5              | `AhoCorasickDslTest`                             | `4개 옵션 동시 적용`                       |
| 2            | replaceAll 마스킹 (`***`)                                | Task 6.2, Task 7.5    | `AhoCorasickReplaceTest`, `AhoCorasickDslTest`   | `replaceAll 마스킹`                        |
| 3            | Flow + take(1) 조기 종료 (로그 알람 패턴)                | Task 9.3, Task 10.5   | `AhoCorasickFlowTest`, `AhoCorasickScenarioTest` | `take로 조기 종료`, `로그 키워드 알람`     |
| 4            | NFKC 정규화 (`㈜` → `(주)` 매치)                         | Task 8.7              | `AhoCorasickKoreanTest`                          | `NFKC 정규화 매치`                         |
| 5            | Java interop (Builder chain)                             | Task 7.5              | `AhoCorasickDslTest`                             | `Builder Java interop`                     |
| 6            | tokenize 후처리 (HTML highlight)                         | Task 7.5, Task 10.5   | `AhoCorasickDslTest`, `AhoCorasickScenarioTest`  | `tokenize 후처리`, `코드 키워드 highlight` |
| §8 migration | Task 13                                                  | `rg` 검증, build 통과 |
| §9 DoD       | Task 16                                                  | PR checklist          |

---

## 부록 B. spec 미반영 / 의도적 변경 사항

1. **Benchmark 파일
   위치**: spec §7.5 는 `src/main/kotlin/.../perf/AhoCorasickBenchmark.kt` 권고. 본 plan 은 `utils/batch` 패턴 (`src/benchmark/kotlin/...`) 을 채택 — production main 코드와 분리해 graphical separation 유지. 검증된 패턴 우선.
2. **Benchmark Gradle
   config**: spec §7.5 의 단순 PoC (`benchmark { configurations { named("main") { ... } } }`) 가 아닌 `utils/batch` 의 register 패턴 (`register("ahocorasick") { ... reportFormat = "json" }`) 사용. JMH JSON 산출물을 위해.
3. **`InternalTrieConfig` 폐기**: Task 2 에서 임시 유지 없이, Task 5 에서 TrieCore 가 직접 SearchOptions 를 받도록 단일 경로로 결정. 중복 타입 잔존 방지.
4. **`OffsetMapping`
   알고리즘**: spec §6 의 "char 단위 sliding 정규화" 를 명시. 단순 `Normalizer.normalize` 한 번 호출만으로는 offset 매핑이 불가능하기 때문.
5. **Flow API
   의 `flowOn(Dispatchers.Default)`**: spec §5 의 sketch 에는 dispatcher 명시 없음. 본 plan 은 `channelFlow { }.flowOn(Dispatchers.Default)` 패턴 사용 — CPU-bound 격리. Task 10b benchmark 결과로 조정 가능.
6. **`AhoCorasickAutomaton.options`
   visibility**: spec §3.4 는 `private val options`. 본 plan 은 `internal val options` — 같은 모듈의 Flow 확장 함수가 접근 가능해야 하기 때문. 외부 사용자 API surface 는 변경 없음.
7. **`AhoCorasickAutomaton<V>` 에서 `Serializable`
   제거**: spec §3.4 에 명시 없음. plan 초안이 추가했으나 TrieCore 내부 failure link 순환 참조로 직렬화 불가 → 제거. 결과 타입 (AhoCorasickMatch/SearchToken/SearchOptions) 만 Serializable 유지.

---

## 부록 C. 위험 / 모니터링 포인트

- **kotlinx-benchmark 1회차 실패 가능성**: monorepo 최초 도입. utils/batch 가 reference. 실패 시 **fallback 금지** — 디버깅으로 해결.
- **`OffsetMapping` 의 NFKC 길이
  변경**: `(주)` 같은 케이스는 +2 chars. mapping 배열 크기 mismatch 시 silent corruption. → 강한 단위 테스트 필수 (Task 8.6).
- **Flow `take(N)` 협력 취소**: `ensureActive()` 가 호출되지 않는 한 producer 가 영원히 돔. emit 마다 호출 보장.
- **Builder
  thread-safety**: 본 plan 은 single-threaded build → 멀티스레드 사용 시 KDoc 에 "thread-unsafe builder, immutable result" 명시 필요. (Task 5.8.)
- **detekt 미통과 가능성**: `internal class TrieCore` 의 길이 (15K) 가 detekt threshold 초과 가능. `@Suppress` 필요 시 KDoc 에 사유 기재.

---

(EOF)
