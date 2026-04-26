# utils/text-search 모듈 설계 (Spec)

- 작성일: 2026-04-26
- 브랜치: `feat/utils-text-search`
- Worktree: `.worktrees/feat-utils-text-search`
- 작성자: planner (Opus, OMC)

---

## 0. 문제 재서술 + 제약 + 미지수

### 문제
`x-obsoleted/ahocorasick`(11 main + 12 test) 의 검증된 Aho-Corasick 구현을 신규 `utils/text-search` 모듈로 승격(promotion)한다. 단순 이동에 그치지 않고, **value 매핑 generic** (`AhoCorasickAutomaton<V>`) · **Kotlin DSL** · **Coroutines `Flow<Match<V>>`** · **`replaceAll { ... }` 람다** · **선택적 Unicode 정규화** API를 추가한다. 한글 금칙어 검사, 대용량 텍스트 스트리밍 매칭, 사전 기반 토큰 치환 같은 실사용 시나리오를 한 모듈에서 만족시키는 것이 목표.

### 제약
- Kotlin 2.3, JVM 21, JUnit 5 + MockK + Kluent.
- `settings.gradle.kts` 의 `includeModules("utils", withBaseDir = false)` 패턴에 따라 디렉토리명 `utils/text-search/` → 아티팩트 `bluetape4k-text-search` 자동 등록.
- `bluetape4k-core`(`ValueObject`, `KLogging`)와 `bluetape4k-coroutines` 에 의존. `kotlinx_coroutines_core` + `kotlinx_coroutines_test` 사용 (`Libs` 에 정의됨).
- `kotlinx_coroutines_core` 는 `compileOnly`로 선언. Flow API(`matchesAsFlow`) 사용자는 자신의 프로젝트에 `implementation(Libs.kotlinx_coroutines_core)` 명시 필요. `bluetape4k-coroutines` 역시 `compileOnly`. Java 전용 사용자는 coroutines 의존성 없이 blocking API만 사용 가능.
- 포맷: IntelliJ + .editorconfig (ktlint 금지). KDoc 한글 허용. 테스트 출력은 production-quality.
- README.md + README.ko.md 양쪽 작성. Mermaid UML 포함.
- x-obsoleted 모듈은 이미 build에서 제외됨. 외부 코드(`x-obsoleted/` 외)에서 `io.bluetape4k.ahocorasick` 참조 0건 확인 → 삭제 안전.

### 미지수 (사용자 결정 필요는 아닌, spec 내에서 결단할 사항)
- A1. `MatchResult<V>` 이름이 `kotlin.text.MatchResult`(자동 임포트)와 충돌. → §3 에서 `AhoCorasickMatch<V>` 로 결정.
- A2. `firstMatch` 의 기존 quirk(allowOverlaps=true 시 시작 위치 기준이 아닌 character-position 기준 첫 emit). → §3 에서 신 API 정규화 결정.
- A3. `stopOnHit` 을 신 API에 노출할지. → §5 에서 Flow 협력 취소로 흡수.
- A4. Unicode 정규화 폼 기본값. → §6 에서 NFC 결정 (한글 합자 syllable 보존).
- A5. `Trie` (구 API) 를 `internal` 로 둘지 `public` 으로 둘지. → §3 에서 internal 채택 (접근법 B).

---

## 1. 설계 리스크 / 실패 모드

### R1. `MatchResult<V>` ↔ `kotlin.text.MatchResult` 이름 충돌 (HIGH)
- `kotlin.text.MatchResult` 는 `kotlin.text.*` 자동 임포트로 모든 파일에 노출됨.
- 사용자가 `import io.bluetape4k.text.search.MatchResult` 를 추가하면 shadowing/IDE warning 발생, 같은 파일에서 정규식 매치와 함께 쓰기 어려움.
- → 결단: `AhoCorasickMatch<V>` 로 명명. (대안 `Match<V>` 는 너무 generic해서 또 다른 충돌 우려.)

### R2. Generic erasure 로 인한 `Map<String, V>` value 보존 (MEDIUM)
- `V: Any` 로 reified 하지 않으면 런타임에 V 정보가 사라짐. 직렬화/로깅 시 `Any` 로 다뤄짐.
- 기존 `State.emits: TreeSet<String>` 은 generic 화 불가능 (정렬 비교자 변경 위험).
- → 결단: 접근법 B(adapter) 채택. `AhoCorasickAutomaton<V>` 가 내부 `Trie` + `Map<String, V>` 보유. State 구조는 그대로.

### R3. Unicode 정규화 비대칭 (HIGH)
- `normalizeUnicode = true` 인데 keyword 등록 시는 정규화하지 않고 input text 만 정규화하면 한글 자모 분리 입력에서 **모든 매치가 깨짐**.
- 정규화는 keyword 등록 시점 + 검색 시점 양쪽에 동일한 form 으로 적용해야 함.
- → 결단: 정규화는 `AhoCorasickAutomaton.Builder` 에서 키워드 등록 시 한 번, `parse*/firstMatch/replaceAll` 진입점에서 입력 텍스트 한 번. invariant 로 §6 에 명시.

### R4. Flow cancellation + `stopOnHit` (MEDIUM)
- `Trie.runParseText` 의 `stopOnHit` 은 emit handler 가 false 반환 시 즉시 abort. Flow 패러다임에서는 `take(1)` 로 downstream cancel 하는 것이 자연스럽다.
- 두 메커니즘을 동시에 노출하면 어느 쪽이 우선인지 모호함. Flow 컨슈머가 cancel 했는데 producer 가 stopOnHit 도 별도 처리하면 race.
- → 결단: Flow API 는 내부에서 항상 `stopOnHit = false` 로 호출하고, 협력 취소(`ensureActive()`)로 종료. blocking API에서만 `stopOnHit` 옵션 유지.

### R5. `firstMatch` quirk 보존 vs 정규화 (MEDIUM)
- 기존 `firstMatch(allowOverlaps=true)` 는 입력 character position 순회 중 처음 만난 emit을 반환 → `"ushers"` + `["he","she","hers"]` 시 `he` (pos=2-3) 가 먼저 나오지만, `she` (pos=1-3) 가 시작 위치는 더 이르다.
- 신 API 사용자는 "leftmost match"를 기대할 가능성 높음.
- → 결단: 신 `AhoCorasickAutomaton.firstMatch` 는 `parseText` 한 후 `start` 기준 최소를 반환 (semantics 정규화). 구 `Trie.firstMatch` quirk 는 `internal` 로 묻힘. KDoc + 테스트로 신 동작 명시.

### R6. 성능: 1MB 텍스트 × 10K 키워드에서 메모리 (LOW)
- `parseText` 가 모든 emit을 list 로 모음. 대용량 입력에서 GC 압박. Flow API 는 emit 당 하나씩 흘려보낼 수 있어야 GC 절감.
- → 결단: §5 의 Flow 구현은 `channelFlow` 로 producer-consumer, 중간 list 누적 없음. (단 `allowOverlaps=false` 모드는 IntervalTree 로 전체 수집 후 정렬해야 하므로 list 누적 불가피 → 이 경우 수집 후 emit 으로 fall-back.)

---

## 2. 접근법 비교

### 접근법 A: Adapter wrapper (구 Trie 그대로 public)
- `AhoCorasickAutomaton<V>` 가 `Trie` 와 `Map<String, V>` 를 위임으로 합성.
- Pros: 변경량 최소, 기존 구현 검증 그대로 활용.
- Cons: API 표면 두 개(`Trie` + `AhoCorasickAutomaton<V>`) 노출. 사용자가 어느 쪽 쓸지 혼란. 향후 deprecation 부채.

### 접근법 B: 신 API 단일 노출 (구 Trie 는 internal)
- `AhoCorasickAutomaton<V>` 만 public. 내부 구현은 기존 `Trie`/`State`/`IntervalTree` 를 `internal` 로 재배치.
- Pros: 단일 진입점. 미래 변경 자유. 신모듈에 기존 사용자 없으므로 deprecation 비용 0.
- Cons: 구현 약간 더 들임. value 없이 keyword 만 필요한 경우 `AhoCorasickAutomaton<Unit>` 또는 `AhoCorasickAutomaton<String>` 형태로 사용해야 (DSL 헬퍼로 완화 가능).

### 접근법 C: `Trie<V>` 풀 generic 화
- `State.emits` 를 `TreeSet<EmitEntry<V>>` 로 변경, Trie 자체에 V 주입.
- Pros: 가장 깔끔. 중간 매핑 불필요.
- Cons: `State` 직렬화 형태 변경, TreeSet 비교자 재작성 필요. 변경 범위 광역. 신규 모듈에서 굳이 risk 감수할 이유 없음.

### 권고: **접근법 B**
신규 모듈로 외부 사용자 0이고, 단일 진입점이 장기 유지보수에 유리. C 의 generic 깊이까지 가지 않아도 B 의 adapter 만으로 사용성 목표 달성 가능. 구 `Trie` 는 `internal class TrieCore` 로 rename + 패키지 `io.bluetape4k.text.search.internal` 격리.

---

## 3. API Design (§3)

### 3.1 패키지 구조
```
io.bluetape4k.text.search                 // public: AhoCorasickAutomaton, AhoCorasickMatch, MatchToken, dsl
io.bluetape4k.text.search.flow            // public: Flow extensions
io.bluetape4k.text.search.internal        // internal: TrieCore, State, Emit, EmitHandler
io.bluetape4k.text.search.internal.interval // internal: Interval, IntervalNode, IntervalTree, Intervalable
```

### 3.2 핵심 타입

```kotlin
package io.bluetape4k.text.search

/**
 * Aho-Corasick 매칭 결과. start/end 는 inclusive (기존 Emit 호환).
 *
 * NOTE (R1): kotlin.text.MatchResult 와의 이름 충돌을 피하기 위해
 * AhoCorasickMatch 라는 명시적 이름을 사용한다.
 */
data class AhoCorasickMatch<out V>(
    val start: Int,
    val end: Int,
    val keyword: String,
    val value: V,
) : java.io.Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
    val length: Int get() = end - start + 1
}

/**
 * 토큰화 결과. 매칭 구간(MatchToken) 또는 비매칭 구간(FragmentToken).
 */
sealed interface SearchToken<out V> : java.io.Serializable {
    val fragment: String
    val isMatch: Boolean

    data class Match<out V>(
        override val fragment: String,
        val match: AhoCorasickMatch<V>,
    ) : SearchToken<V> { override val isMatch get() = true }

    data class Fragment(
        override val fragment: String,
    ) : SearchToken<Nothing> { override val isMatch get() = false }
}
```

### 3.3 옵션 (immutable)

```kotlin
package io.bluetape4k.text.search

/**
 * 단어 경계 판정 정책.
 * - LATIN_ALPHA: Character.isAlphabetic 기반 (기존 Trie 동작과 동일, Latin 위주)
 * - WHITESPACE_SEPARATED: 양쪽이 공백/문자열 끝
 * - NONE: 경계 검사 안 함 (모든 매치 허용)
 */
enum class WordBoundary { LATIN_ALPHA, WHITESPACE_SEPARATED, NONE }

/**
 * Unicode 정규화 옵션.
 * - NONE: 정규화 안 함 (기본)
 * - NFC: 한글 자모 합성 (한글 권장)
 * - NFKC: 호환 합성
 */
enum class NormalizationForm { NONE, NFC, NFKC }

/**
 * 검색 옵션. 모든 필드는 `val` (불변). copy() 로 변형. Builder 는 Java 호환용으로만 제공.
 *
 * NOTE (R2 의 §8 migration): 구 TrieConfig 의 mutable var → 불변 val 로 변경.
 */
data class SearchOptions(
    val ignoreCase: Boolean = false,
    val allowOverlaps: Boolean = true,
    val wordBoundary: WordBoundary = WordBoundary.NONE,
    val normalization: NormalizationForm = NormalizationForm.NONE,
    /** blocking API 한정. Flow API 에서는 무시되고 협력 취소 사용. */
    val stopOnFirstMatch: Boolean = false,
) : java.io.Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
```

### 3.4 메인 API

```kotlin
package io.bluetape4k.text.search

class AhoCorasickAutomaton<V> internal constructor(
    private val core: io.bluetape4k.text.search.internal.TrieCore,
    private val values: Map<String, V>,    // normalized keyword -> value
    private val options: SearchOptions,
) {
    companion object : KLogging() {
        @JvmStatic
        fun <V> builder(): Builder<V> = Builder()
    }

    // --- blocking ---
    fun parseText(text: CharSequence): List<AhoCorasickMatch<V>>
    fun firstMatch(text: CharSequence): AhoCorasickMatch<V>?    // R5: leftmost (start 기준 최소)
    fun containsMatch(text: CharSequence): Boolean
    fun tokenize(text: CharSequence): List<SearchToken<V>>

    /**
     * 매칭 구간을 람다 결과로 치환. 매칭이 없으면 입력 그대로 반환.
     *
     * @param text 원본
     * @param transform (match) -> 치환 문자열. value/keyword/start/end 활용 가능.
     */
    fun replaceAll(text: CharSequence, transform: (AhoCorasickMatch<V>) -> CharSequence): String

    // Java-friendly Builder. Kotlin 사용자는 §4 DSL 권장.
    class Builder<V> {
        /** `add(keyword, value)` 호출 시 `keyword.requireNotBlank("keyword")` 검증. 빈 텍스트 입력은 매치 없음으로 정상 처리. */
        fun add(keyword: String, value: V): Builder<V>
        fun addAll(map: Map<String, V>): Builder<V>
        fun options(options: SearchOptions): Builder<V>
        fun build(): AhoCorasickAutomaton<V>
    }
}
```

### 3.5 String 키워드만 필요할 때 헬퍼

```kotlin
/** value 가 keyword 자신인 사전 (단순 사전). */
fun ahoCorasickOf(
    vararg keywords: String,
    options: SearchOptions = SearchOptions(),
): AhoCorasickAutomaton<String>

fun ahoCorasickOf(
    keywords: Collection<String>,
    options: SearchOptions = SearchOptions(),
): AhoCorasickAutomaton<String>
```

### 3.6 변경 사항 요약 (구→신)
| 구 (`x-obsoleted/ahocorasick`) | 신 (`utils/text-search`) |
|---|---|
| `Trie` (public) | `internal class TrieCore` |
| `TrieConfig` (var, public) | `data class SearchOptions` (val, public) |
| `Emit` (Interval 상속) | `data class AhoCorasickMatch<V>` (Interval 미상속) |
| `Token`, `MatchToken`, `FragmentToken` | `sealed interface SearchToken<V>` |
| `EmitHandler` fun interface | (internal) — public API 는 Flow 사용 |
| `firstMatch` quirk | leftmost (start 기준) 로 정규화 |

### 3.7 고급 옵션 예제 (사용자 요청)

아래 6개 시나리오를 KDoc 예제와 테스트로 모두 제공해야 한다.

1. **4-옵션 동시 적용** (ignoreCase + allowOverlaps=false + wordBoundary=WHITESPACE + normalization=NFC)
   ```kotlin
   val ac = ahoCorasick<String> {
       ignoreCase = true; allowOverlaps = false
       wordBoundary = WordBoundary.WHITESPACE_SEPARATED
       normalization = NormalizationForm.NFC
       keyword("욕설1", "BLOCKED"); keyword("Bad Word", "BLOCKED")
   }
   ```

2. **replaceAll 마스킹** (매칭 구간을 `***`로 대체)
   ```kotlin
   val cleaned = ac.replaceAll(userInput) { "*".repeat(it.keyword.length) }
   ```

3. **Flow + take(N) 조기 종료**
   ```kotlin
   val first3 = runBlocking { ac.matchesAsFlow(text).take(3).toList() }
   ```

4. **NFKC 호환문자 매칭** (`㈜` → `(주)` 정규화 후 매칭)
   ```kotlin
   val ac = ahoCorasick<String> { normalization = NormalizationForm.NFKC; keyword("(주)", "corporation") }
   ```

5. **Builder Java interop**
   ```kotlin
   val ac: AhoCorasickAutomaton<String> = AhoCorasickAutomaton.builder<String>()
       .add("APPL", "Apple").add("NYC", "New York")
       .options(SearchOptions(ignoreCase = true))
       .build()
   ```

6. **tokenize 후처리** (텍스트를 매치/비매치 조각으로 분리)
   ```kotlin
   val tokens = ac.tokenize(articleBody)
   val highlighted = tokens.joinToString("") { token ->
       if (token is SearchToken.Match) "<b>${token.fragment}</b>" else token.fragment
   }
   ```

---

## 4. DSL (§4)

```kotlin
package io.bluetape4k.text.search

@DslMarker
annotation class AhoCorasickDsl

@AhoCorasickDsl
class AhoCorasickBuilder<V> internal constructor() {
    companion object : KLogging()

    var ignoreCase: Boolean = false
    var allowOverlaps: Boolean = true
    var wordBoundary: WordBoundary = WordBoundary.NONE
    var normalization: NormalizationForm = NormalizationForm.NONE
    var stopOnFirstMatch: Boolean = false

    private val entries = mutableMapOf<String, V>()

    fun keyword(keyword: String, value: V) { entries[keyword] = value }
    // value-less 오버로드 없음 — 타입 안전 문제(UNCHECKED_CAST). ahoCorasickOf() 헬퍼 사용.

    fun keywords(vararg keywords: Pair<String, V>) { entries.putAll(keywords) }
    fun keywords(map: Map<String, V>) { entries.putAll(map) }

    internal fun build(): AhoCorasickAutomaton<V> { /* ... */ }
}

fun <V> ahoCorasick(block: AhoCorasickBuilder<V>.() -> Unit): AhoCorasickAutomaton<V> =
    AhoCorasickBuilder<V>().apply(block).build()
```

사용 예:
```kotlin
val ac = ahoCorasick<String> {
    ignoreCase = true
    allowOverlaps = false
    wordBoundary = WordBoundary.LATIN_ALPHA

    keyword("APPL", "Apple Inc.")
    keyword("NYC", "New York City")
    keyword("PM", "product manager")
}

val matches = ac.parseText("APPL HQ in NYC, PM team")
// [AhoCorasickMatch(0,3,"APPL","Apple Inc."), ...]

val replaced = ac.replaceAll("APPL HQ in NYC") { match -> match.value }
```

---

## 5. Flow API (§5)

```kotlin
package io.bluetape4k.text.search.flow

import io.bluetape4k.text.search.AhoCorasickAutomaton
import io.bluetape4k.text.search.AhoCorasickMatch
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow

/**
 * 매치를 비동기 스트림으로 방출.
 * - allowOverlaps=true: emit 즉시 흘려보냄 (메모리 절감, R6)
 * - allowOverlaps=false: 내부에서 IntervalTree 로 전체 수집 후 정렬 emit
 *
 * 협력 취소(R4):
 * - 다운스트림이 cancel 되면 ensureActive() 가 CancellationException 던지고 producer 종료
 * - SearchOptions.stopOnFirstMatch 는 이 API에서 무시됨. take(1)/first() 사용.
 */
fun <V> AhoCorasickAutomaton<V>.matchesAsFlow(text: CharSequence): Flow<AhoCorasickMatch<V>>
```

NOTE: `matchesAsFlow`는 `io.bluetape4k.text.search.flow` 패키지에 확장 함수로 위치. Flow API가 필요 없으면 import 없이 blocking API만 사용 가능.

구현 스케치:
```kotlin
fun <V> AhoCorasickAutomaton<V>.matchesAsFlow(text: CharSequence): Flow<AhoCorasickMatch<V>> = flow {
    if (!options.allowOverlaps) {
        // Overlap 제거는 전체 수집 필요 → 일괄 emit
        parseText(text).forEach {
            currentCoroutineContext().ensureActive()
            emit(it)
        }
    } else {
        // 한 번에 한 개씩 흘려보냄
        runStream(text) { match ->
            currentCoroutineContext().ensureActive()
            emit(match)
        }
    }
}
```

`runStream` 은 internal 헬퍼: `TrieCore.runParseText` 를 suspend EmitHandler 로 감싸 호출. 단, EmitHandler 가 fun interface(non-suspend) 이므로 emit 람다는 `runBlocking` 없이 직접 호출 가능한 형태로 inline 된다 (collector 가 suspend 인 점은 외부 `flow { }` 가 처리).

---

## 6. Unicode 정규화 옵션 (§6)

### 정책
- `SearchOptions.normalization` 이 `NFC` 또는 `NFKC` 이면:
  1. **빌드 시점**: `Builder.add(keyword, value)` 호출 시 `Normalizer.normalize(keyword, form)` 적용 후 내부 keyword 와 `values` map key 로 사용.
  2. **검색 시점**: `parseText/tokenize/replaceAll/matchesAsFlow` 진입에서 입력 `text` 를 동일 form 으로 정규화. NFC/NFKC 정규화는 길이를 변경할 수 있다 (예: `㈜` NFKC → `(주)`, +2 chars). 이를 처리하기 위해 `OffsetMapping` 내부 클래스가 **원본 텍스트 offset → 정규화 텍스트 offset 변환 테이블**을 유지한다. `parseText`/`replaceAll`/`tokenize` 모두 원본 offset 기준 `AhoCorasickMatch.start/end`를 반환한다.

     구현 스케치:
     ```kotlin
     internal class OffsetMapping(original: CharSequence, normalized: CharSequence) {
         // normOffset[i] = normalized[i]가 original[?]에 해당하는 position
         fun toOriginal(normalizedOffset: Int): Int
         fun toNormalized(originalOffset: Int): Int
     }
     ```
     Task #8에 offset 매핑 구현 포함 (+2h, 기존 1h → 3h).
- `NONE` (기본) 이면 어떤 정규화도 적용하지 않고 원본 그대로 처리.

### Invariant (R3)
- 키워드와 입력 텍스트의 정규화 form 은 **반드시 동일** 하다. 빌드 후 옵션을 변경할 수 없다 (immutable SearchOptions).
- `ignoreCase` 와 결합 시 처리 순서: **정규화 → lowercase**. (NFC 후 lowercase 가 안전. lowercase 후 NFC 는 일부 언어에서 다른 결과.)

### 권장 사용 (한글)
```kotlin
val ac = ahoCorasick<String> {
    normalization = NormalizationForm.NFC
    wordBoundary = WordBoundary.WHITESPACE_SEPARATED
    keyword("욕설1", "BLOCKED")
    keyword("욕설2", "BLOCKED")
}
```

---

## 7. 테스트 전략 (§7)

### 7.1 디렉토리
```
utils/text-search/src/test/kotlin/io/bluetape4k/text/search/
├── AhoCorasickAutomatonTest.kt         // 기본 매치/value 매핑
├── AhoCorasickDslTest.kt                // DSL 사용성
├── AhoCorasickReplaceTest.kt            // replaceAll 람다
├── AhoCorasickOptionsTest.kt            // ignoreCase / allowOverlaps / wordBoundary 매트릭스
├── AhoCorasickKoreanTest.kt             // NFC + 한글 금칙어
├── AhoCorasickFirstMatchTest.kt         // R5 leftmost semantics
├── flow/AhoCorasickFlowTest.kt          // matchesAsFlow + 협력 취소
└── perf/AhoCorasickPerformanceTest.kt   // @Tag("performance"), 1MB × 10K
```

### 7.2 테스트 매트릭스 (옵션 조합)
| ignoreCase | allowOverlaps | wordBoundary       | normalization | 기대 |
|------------|---------------|---------------------|---------------|------|
| false      | true          | NONE                | NONE          | 모든 매치 (오버랩 포함) |
| true       | true          | NONE                | NONE          | 대소문자 무시 모든 매치 |
| false      | false         | NONE                | NONE          | IntervalTree 로 큰 키워드 우선 |
| false      | true          | LATIN_ALPHA         | NONE          | 부분 단어 매치 제외 |
| false      | true          | WHITESPACE_SEPARATED| NONE          | 공백 경계만 |
| false      | true          | WHITESPACE_SEPARATED| NFC           | 한글 자모 정규화 |
| false      | true          | NONE                | NONE          | stopOnFirstMatch=true → 첫 매치에서 중단 (blocking) |

각 행 1개 이상 테스트.

### 7.3 한글 시나리오
- 자모 분리 입력(`ㄴㅏ`) vs 합성 입력(`나`) → NFC 정규화 시 동일 매치 검증.
- `WHITESPACE_SEPARATED` + 한글 다중 어절 (`"오늘 욕설1 했다"` → `욕설1` 매치).
- LATIN_ALPHA boundary 가 한글에 잘못 적용되지 않는지(한글이 alphabetic 으로 판정되는 점 명시 + 한글에는 WHITESPACE 권장 KDoc).

### 7.4 Flow 테스트
- 정상 collect, 중간 cancel (`take(2)`), 빈 결과, allowOverlaps=false 일괄 emit, 1만 매치 throughput.
- `runTest(timeout = 30.seconds)` 사용.
- `currentCoroutineContext().cancel()` 후 `ensureActive()` 가 CancellationException 던지는지 검증.

### 7.5 Benchmark (kotlinx-benchmark / JMH)

**build.gradle.kts 추가 설정** (monorepo 최초 도입 — PoC 먼저 확인):
```kotlin
plugins {
    kotlin("plugin.allopen")
    id(Plugins.kotlinx_benchmark)
}

allOpen {
    // JMH bytecode 생성을 위해 @State 클래스를 open으로 처리
    annotation("org.openjdk.jmh.annotations.State")
}

benchmark {
    configurations {
        named("main") {
            warmups = 2
            iterations = 5
            mode = "thrpt"          // Throughput (ops/s)
        }
    }
    targets {
        register("main") {
            this as kotlinx.benchmark.gradle.JvmBenchmarkTarget
            jmhVersion = Versions.jmh
        }
    }
}

dependencies {
    // ...기존 의존성...
    // benchmark runtime은 kotlinx-benchmark plugin이 자동 추가
}
```

**Benchmark 파일 경로**:
`utils/text-search/src/main/kotlin/io/bluetape4k/text/search/perf/AhoCorasickBenchmark.kt`
(NOTE: `src/main` 사용 — kotlinx-benchmark는 src/test 대신 src/main 대상 Benchmark를 권장)

```kotlin
@State(Scope.Benchmark)
open class AhoCorasickBenchmark {
    lateinit var matcher: AhoCorasickAutomaton<String>
    lateinit var largeText: String

    @Setup
    fun setup() {
        val keywords = (1..10_000).map { "keyword$it" }
        matcher = ahoCorasickOf(keywords)
        largeText = buildString { repeat(10_000) { append("some keyword${it % 10_000} text ") } }
    }

    @Benchmark fun parseText(): List<AhoCorasickMatch<String>> = matcher.parseText(largeText)

    @Benchmark fun matchesAsFlowCollect(): Int = runBlocking { matcher.matchesAsFlow(largeText).toList().size }

    @Benchmark fun naiveContains(): Int = matcher.keywords.count { it in largeText }
}
```

- 실행: `./gradlew :bluetape4k-text-search:benchmark`
- 결과: JMH HTML 리포트 + 주요 ops/s 수치를 `README.md` `## Benchmark` 섹션에 표로 기록
- **Fallback**: kotlinx-benchmark 도입 실패 시 `@Tag("performance")` JUnit 측정(`System.nanoTime`)으로 대체. Task #10b에 PoC 성공 여부 확인 필수.
- CI: `benchmark` task는 기본 `test` task에 포함되지 않음.

### 7.6 마이그레이션 동등성 테스트

구 `TrieTest.kt` 케이스 12개를 신 API로 재작성. `AhoCorasickMigrationTest.kt`에 작성.

| # | 케이스 이름 | 동일 결과? |
|---|------------|-----------|
| 1 | keyword and text are same | ✅ 동일 |
| 2 | ushers overlaps (he/she/hers) | ✅ 동일 |
| 3 | food recipes | ✅ 동일 |
| 4 | start of churchill speech | ✅ 동일 |
| 5 | partial match exclusion (onlyWholeWords) | ✅ 동일 |
| 6 | tokenize full sentence | ✅ 동일 |
| 7 | ignoreCase | ✅ 동일 |
| 8 | replace with map | ✅ 동일 (replaceAll 람다로 재작성) |
| 9 | containsMatch | ✅ 동일 |
| 10 | ignoreOverlaps (IntervalTree 최대 구간 우선) | ✅ 동일 |
| 11 | firstMatch — ushers 케이스 | ⚠️ **다름** (R5: start 기준 leftmost → `she`(pos=1), 구 API는 `he`(pos=2)) |
| 12 | firstMatch — unicode 케이스 | ⚠️ **다름** (R5 동일 이유) |

### 7.7 실전 시나리오 테스트 (사용자 요청)

테스트 파일: `AhoCorasickScenarioTest.kt`

5개 end-to-end 시나리오:

1. **금칙어 검열** — 한글 욕설 사전 100개, 사용자 입력 replaceAll로 `***` 마스킹 검증
2. **자동완성 사전** — 제품명 1,000개 사전, 긴 문서에서 제품명 추출 후 distinct 정렬
3. **로그 키워드 알람** — ERROR/WARN/FATAL 패턴 감지, matchesAsFlow + take(1) 조기 종료
4. **URL 추출** — `http://`, `https://`, `ftp://` prefix 매칭, wordBoundary=NONE
5. **코드 키워드 highlight** — 예약어 50개 (val/var/fun/class 등), ignoreCase=false, tokenize → HTML 변환

---

## 8. Migration (§8)

### 8.1 x-obsoleted 처리
1. 모든 신규 코드 작성 + 테스트 통과 확인.
2. `x-obsoleted/ahocorasick/` 디렉토리 git rm.
3. Verification: `rg 'io.bluetape4k.ahocorasick' --glob '!docs/**' --glob '!.worktrees/**'` 0 hits 확인.
4. 커밋: `chore: x-obsoleted/ahocorasick 제거 (utils/text-search 로 승격)`.

### 8.2 settings.gradle.kts
- `utils/text-search/` 디렉토리 생성 시 자동 등록 (수동 변경 불필요).
- BOM은 `utils/` 모듈을 자동 수집하므로 별도 수정 불필요.

### 8.3 의도적 breaking changes
- `TrieConfig` (mutable) → `SearchOptions` (immutable). 외부 사용자 0이므로 영향 없음.
- 패키지 `io.bluetape4k.ahocorasick.*` → `io.bluetape4k.text.search.*`.
- `Emit` 타입 제거, `AhoCorasickMatch<V>` 도입. (구 사용자 0)
- `firstMatch` semantics 변경 (R5).

### 8.4 README/문서
- `utils/text-search/README.md` (영문) + `README.ko.md` (한글). Mermaid class diagram 1개 + sequence diagram 1개.
- 루트 `README.md` 모듈 그룹 표 `utils/` 행에 `text-search` 추가.
- `docs/superpowers/index/2026-04.md` 맨 위에 spec 항목 추가.

---

## 9. DoD (§9)

### 9.1 코드
- [ ] `utils/text-search/build.gradle.kts` 작성 (utils/states 패턴 참고)
- [ ] `internal` 패키지에 `TrieCore`, `State`, `Emit`, `EmitHandler`, `Interval*` 이전 (이름 충돌 회피 위해 일부 rename)
- [ ] `public` 패키지에 `AhoCorasickAutomaton`, `AhoCorasickMatch`, `SearchToken`, `SearchOptions`, `WordBoundary`, `NormalizationForm` 작성
- [ ] DSL `ahoCorasick { }` + `ahoCorasickOf(...)` 헬퍼
- [ ] Flow 확장 `matchesAsFlow` (`text-search/flow/` 패키지)
- [ ] `replaceAll { ... }` 람다 구현
- [ ] 모든 public API 에 한글 KDoc + 사용 예제

### 9.2 테스트 (80%+ 커버리지)
- [ ] 7개 테스트 파일 모두 작성 (성능 제외 일반 테스트 ≥ 50개 케이스)
- [ ] 옵션 매트릭스 6개 행 모두 커버
- [ ] 한글 NFC + 자모분리 입력 케이스 ≥ 3개
- [ ] Flow cancel 케이스 ≥ 2개
- [ ] 마이그레이션 동등성 케이스 12개
- [ ] `./gradlew :bluetape4k-text-search:test` 100% pass
- [ ] `./gradlew :bluetape4k-text-search:build` 빌드 통과
- [ ] `AhoCorasickBenchmark.kt` 작성 (`@State`+`@Benchmark`, parseText/matchesAsFlow/naive 3개)
- [ ] `./gradlew :bluetape4k-text-search:benchmark` 실행 성공
- [ ] benchmark 결과를 `README.md` `## Benchmark` 섹션에 기록

### 9.3 문서
- [ ] `README.md` + `README.ko.md` (Mermaid UML 포함, Architecture→UML→Features→Examples 순서)
- [ ] 루트 `README.md` 모듈 표 업데이트
- [ ] `docs/superpowers/index/2026-04.md` 항목 추가
- [ ] `/wiki-update` 실행

### 9.4 정리
- [ ] x-obsoleted/ahocorasick git rm + 외부 참조 0 verify
- [ ] OMC code-reviewer agent 실행, HIGH/CRITICAL 이슈 0 확인
- [ ] PR 생성 (테스트 결과 + 변경 사유 + verification commands 포함)

---

## 부록 A. Draft Task List (구현 단계용)

| # | Task | 의존 | 추정 |
|---|------|------|------|
| 1 | `utils/text-search/build.gradle.kts` + 디렉토리 스캐폴딩 (kotlinx-benchmark PoC 포함) | — | 1.5h |
| 2 | internal 패키지로 Trie/State/Emit/Interval* 이전 (rename: Trie→TrieCore) | 1 | 1h |
| 3 | `SearchOptions` + `WordBoundary` + `NormalizationForm` (immutable) | 1 | 0.5h |
| 4 | `AhoCorasickMatch<V>` + `SearchToken<V>` sealed | 1 | 0.5h |
| 5 | `AhoCorasickAutomaton<V>` (parseText/firstMatch/contains/tokenize) | 2,3,4 | 2h |
| 6 | `replaceAll { ... }` 람다 구현 | 5 | 0.5h |
| 7 | DSL `ahoCorasick { }` + `ahoCorasickOf(...)` | 5 | 1h |
| 8 | Unicode normalization (빌드 시점 + 검색 시점 양쪽) + OffsetMapping 구현 | 5 | 3h |
| 9 | Flow `matchesAsFlow` (channelFlow + ensureActive) | 5 | 1h |
| 10 | 테스트 7개 파일 (옵션 매트릭스 + 한글 + Flow) + §3.7 예제 + §7.7 시나리오 5개 | 5–9 | 4h |
| 10b | `AhoCorasickBenchmark.kt` (JMH: parseText/matchesAsFlow/naive) | 5–9 | 1h |
| 11 | 마이그레이션 동등성 테스트 (구 TrieTest 12 케이스 재작성) | 10 | 1.5h |
| 12 | README.md + README.ko.md (Mermaid 다이어그램) | 5–9 | 1.5h |
| 13 | x-obsoleted/ahocorasick 삭제 + 외부 참조 verify | 11 | 0.5h |
| 14 | 루트 README + superpowers index + /wiki-update | 12,13 | 0.5h |
| 15 | OMC code-reviewer + HIGH/CRITICAL fix | 14 | 1h |
| 16 | PR 생성 (테스트 결과 + 변경 사유) | 15 | 0.5h |

총 추정: ~22h.

---

## 부록 B. 결정 사항 요약 (사용자 확인용)

1. 신 API 이름은 `AhoCorasickAutomaton<V>` + `AhoCorasickMatch<V>` (kotlin.text.MatchResult 충돌 회피).
2. 구 `Trie` 는 `internal class TrieCore` 로 격리 (접근법 B).
3. `SearchOptions` immutable (mutable `TrieConfig` 폐기).
4. `firstMatch` 는 leftmost (start 기준) 로 semantics 정규화.
5. Unicode 정규화 NFC/NFKC, 키워드+입력 양쪽 동일 적용 invariant.
6. Flow API 는 `stopOnFirstMatch` 무시, 협력 취소 사용.
7. 성능 측정은 kotlinx-benchmark(JMH) 사용 — parseText/matchesAsFlow/naive 3개 Benchmark, 결과를 README `## Benchmark` 섹션에 기록. JUnit `@Tag("performance")` 없음.
8. DSL `keyword(String)` value-less 오버로드 없음. `ahoCorasickOf(vararg keywords: String)` 헬퍼로 대체.
9. BOM 자동 수집 — `bluetape4k-bom` 수동 수정 불필요.
10. x-obsoleted/ahocorasick 은 신 모듈 통과 후 git rm.
