# Digest 공통 API와 Workshop 소비자 이전 구현 계획

> **구현 작업자 안내:** 각 작업은 이 문서의 순서와 write scope를 따르고, TDD와 spec 우선 검토를 완료한 뒤 코드 품질 리뷰를 받는다. 구현 subagent를 한 번에 하나만 실행한다.

**Goal:** Projects `io/io`에 공통 SHA-256 API를 제공하고 기존 `TinkDigester` 계약을 보존하며, 이슈 #1917에 기록된 Workshop 소비자 29개 파일·35개 호출을 byte-for-byte 호환되게 이전한다.

**Architecture:** `io/io`가 `Digester`, 독립 상태 `DigestSession`, JDK 구현과 엄격한 Base64 codec을 소유한다. 공개 SHA-256 기본 접근점은 `io.bluetape4k.io.digest.Digesters.SHA256`이고 파일은 `Digesters.kt`다. `io/tink`는 기존 `TinkDigester`의 API·JVM descriptor·출력을 유지하는 adapter가 되어 `io/io`에 의존한다. Workshop은 기존 framing과 결과 표현을 보존하며 Projects worktree를 composite build로 사용해 검증한다.

**Tech Stack:** Kotlin 2.4, JDK 25, Gradle Wrapper 9.8.1, JUnit 5, 기존 bluetape4k assertions, `java.security.MessageDigest`, JDK `java.util.Base64`, kotlinx-benchmark.

---

## 승인 경계와 근거

- 기준 명세: `docs/superpowers/specs/2026-11-23-shared-digester-design.md`, SHA-256 `a8926875835e2b3744c721936e0fcebf83961013f10774d4c5dbe6b1b3ebbd39`.
- 이슈: [Projects #1917](https://github.com/bluetape4k/bluetape4k-projects/issues/1917), 열린 `2.2.0` milestone 이슈, assignee `debop`.
- 사용자 승인: 명세 갱신 및 구현 계획 실행을 승인했다. 변경 목표는 기존 `DigestUtils` 초안 이름을 `Digesters`로 바꾸는 것이며, 본문에 기술된 #1917 API와 Workshop 이전도 완료한다.
- PR, push, merge, tag, release, artifact publication, 이슈 종료는 이 계획의 권한에 포함하지 않는다.
- `TinkDigesters`는 기존 `io.bluetape4k.tink.digest` API이고 신규 `io.bluetape4k.io.digest.Digesters`와 별개다. 새 API에 `DigestUtils` compatibility alias를 만들지 않는다.
- 이전 시 HMAC, Tink MAC, test oracle, fixture 데이터와 저장된 digest/wire 표현은 바꾸지 않는다. 구분자·길이 접두사·정렬·nullable marker·truncation·byte order는 각 호출부에서 보존한다.

## 파일 및 모듈 지도

| 영역 | 파일 | 책임 |
|---|---|---|
| API 계약 | `io/io/src/main/kotlin/io/bluetape4k/io/digest/Digester.kt`, `DigestSession.kt` | 알고리즘 제공자와 상태 있는 세션의 public contract |
| JDK 구현 | `io/io/src/main/kotlin/io/bluetape4k/io/digest/JdkDigester.kt` | 독립 `MessageDigest` 세션 생성과 one-shot 구현 |
| 기본 접근점 | `io/io/src/main/kotlin/io/bluetape4k/io/digest/Digesters.kt` | `@JvmField SHA256` 기본 인스턴스 |
| Base64 | `io/io/src/main/kotlin/io/bluetape4k/io/digest/Base64Codec.kt`, `Base64Format.kt` | 4개 alphabet/padding 조합과 canonical 입력 검증 |
| API tests | `io/io/src/test/kotlin/io/bluetape4k/io/digest/` | 알려진 벡터, 형식, 예외, session 재사용·동시 격리 |
| Java contract | `io/io/src/test/java/io/bluetape4k/io/digest/DigestersJavaContractTest.java` | 실제 Java consumer와 public JVM descriptor |
| README sample contract | `io/io/src/test/kotlin/io/bluetape4k/io/digest/DigestersReadmeExampleTest.kt` | 양 locale의 코드 예제를 compile/run fixture와 일치 검증 |
| Tink adapter | `io/tink/src/main/kotlin/io/bluetape4k/tink/digest/TinkDigester.kt`, `io/tink/src/test/kotlin/io/bluetape4k/tink/digest/TinkDigesterTest.kt` | 기존 public API를 공통 구현에 위임하고 ABI/output 보존 |
| 의존성 | `io/io/build.gradle.kts`, `io/tink/build.gradle.kts` | `io/io`의 Tink `compileOnly` 제거, `io/tink -> io/io` 공개 의존성 |
| 사용자 문서 | `io/io/README.md`, `io/io/README.ko.md` | locale별 `Digesters` 사용 예와 형식 선택 |
| benchmark | `io/io/src/test/kotlin/io/bluetape4k/io/benchmark/DigestBenchmark.kt`, `io/io/src/test/kotlin/io/bluetape4k/io/benchmark/README.md` | 동일 payload의 네 경로 측정과 재현 가능한 결과 기록 |
| Workshop | 명세 §“Workshop 호출별 migration mapping”의 9 modules, 29 production files | `bluetape4k-io` 의존성과 기존 입력·출력 표현 보존 |
| 계획 통합 리뷰 | `docs/superpowers/reviews/2026-11-24-issue-1917-plan-review.md` | 6개 독립 관점, 통합 판정, SPW-01…05와 위험 예측 |
| 구현 통합 증거 | `docs/review/2026-11-24-issue-1917-digest-migration-review.md`, `docs/lessons/` | baseline manifest, 검증 결과, 최종 7-tier 리뷰와 레슨 |

Workshop 모듈별 Gradle 파일은 아래와 같다. `pre-generated-voucher-pool`은 이미 기존 catalog alias `libs.bluetape4k.io`를 사용하므로 추가 중복 선언을 하지 않는다. 나머지 8개는 각 파일의 기존 dependency scope를 확인해 `bluetape4k-io` alias를 추가한다.

- `commerce/concert-ticket-flash-sale/build.gradle.kts` (`:commerce-concert-ticket-flash-sale`)
- `commerce/event-sourced-promotion-voucher-campaign/build.gradle.kts` (`:commerce-event-sourced-promotion-voucher-campaign`)
- `commerce/order-lifecycle-fulfillment/build.gradle.kts` (`:commerce-order-lifecycle-fulfillment`)
- `commerce/pre-generated-voucher-pool/build.gradle.kts` (`:commerce-pre-generated-voucher-pool`; 기존 의존성 확인만)
- `commerce/promotion-voucher-campaign/build.gradle.kts` (`:commerce-promotion-voucher-campaign`)
- `commerce/usage-metering-billing-event-sourcing/build.gradle.kts` (`:commerce-usage-metering-billing-event-sourcing`)
- `commerce/usage-metering-billing-ledger/build.gradle.kts` (`:commerce-usage-metering-billing-ledger`)
- `commerce/shared/build.gradle.kts` (`:commerce-shared`)
- `commerce/reservation-control-plane/build.gradle.kts` (`:commerce-reservation-control-plane`)

## 작업 순서

### 구현 전 게이트 — 계획 통합 리뷰와 문서 커밋

코드나 테스트 파일을 수정하기 전에 최신 계획 해시를 고정해 성능, 안정성, 보안, 운영·이전, 개발자 API, 사용자·호출자 여섯 관점의 독립 리뷰와 main-session 통합 리뷰를 완료한다. 각 리뷰는 정확한 plan/spec hash와 근거가 있는 finding을 기록한다. 모든 P0/P1을 해결하고 영향을 받은 관점을 같은 최신 해시에서 재검토한다. P2/P3도 계획에 반영하거나 이월 이유를 기록한다.

- [ ] **Step 1: 통합 계획 리뷰와 위험 표 작성**

`docs/superpowers/reviews/2026-11-24-issue-1917-plan-review.md`에 최신 spec/plan SHA-256, 여섯 관점별 reviewer role·판정·P0…P3, 통합 finding/disposition, traceability 결과와 `SPW-01`…`SPW-05`를 기록한다. 아래 A-05 위험 표의 signal, mitigation, stop/rollback/rerun 조건을 구체화한다. 통합 결과가 P0=0, P1=0이며 writer gate가 모두 PASS일 때만 다음 단계로 간다.

- [ ] **Step 2: 승인된 설계 문서를 구현 전에 로컬 커밋**

spec, spec review, plan, plan review 네 파일만 stage하고 Lore protocol을 따르는 한국어 intent commit을 만든다. 커밋 내용이 정확히 이 네 문서인지 `git show --stat --oneline HEAD`와 `git diff HEAD^ HEAD --name-only`로 읽어 확인한다. 이 커밋에는 코드, 테스트, Workshop 파일이 포함되지 않는다. 구현 단계는 이 커밋이 존재하고 작업 트리가 깨끗한 뒤에만 시작한다. Push/PR/merge는 하지 않는다.

### A-05 — 위험 예측

| 위험 | 징후 | 완화 | 중지·롤백·재검증 |
|---|---|---|---|
| 공유 SHA-256 접근점에서 세션 상태가 새거나 동시 호출이 간섭한다. | 반복 digest/reset 상태가 불명확하거나 동시 one-shot/matcher 결과가 독립 oracle과 다르다. | public reset은 두지 않고 성공 digest 후 빈 상태, 범위 실패 후 기존 상태 유지, provider 실패 후 session 폐기를 문서화한다. singleton one-shot/matcher 및 독립 session barrier 테스트를 둔다. | API 구현을 멈추고 exact state 전이 테스트를 먼저 고친다. task-owned 변경만 보정한 뒤 digest contract suite와 관련 Tink tests를 순차 재실행한다. |
| 엄격한 입력 검증이 인증 API처럼 오해되거나 key-material 생명주기가 약화된다. | decoder 외 예외를 삼키거나 provider 선택·MAC/HMAC·zeroization 경로가 달라진다. | malformed Base64 `IllegalArgumentException`만 false로 바꾸고 나머지는 전파한다. provider parity, 신뢰된 algorithm 설정, 반환 배열 소유권과 키 byte `finally` zeroization을 보존한다. | 보안 변경을 중단하고 baseline/provider 및 exact diff를 대조한다. 취약 경로를 복구한 뒤 digest/Tink와 해당 Workshop module tests를 직렬 재실행한다. |
| benchmark 경로가 서로 다른 payload 또는 비용 범위를 측정한다. | one-shot과 incremental 결과를 한 순위로 비교하거나 timed region, JMH 환경·오차·원시 출력이 빠진다. | raw-digest characterization으로 한정하고 workload별 timed region과 session 생성 포함 여부를 명시한다. 단일 thread·고정 fork·입력·JMH 환경·score error와 저장소에 보존할 원시 출력을 기록한다. | 성능 우열이나 consumer 성능 결론을 쓰지 않는다. 동일 설정으로 compile/run을 재수행하고 결과 README의 원시 출력 및 환경 evidence를 남긴다. |
| 중단 시 task-owned 변경 경로가 누락되거나 기존 상태를 덮어쓴다. | 실제 staged/unstaged/untracked 집합이 write-ahead manifest와 다르거나 baseline이 clean하지 않다. | 편집 전 정확한 경로를 journal에 기록하고 Projects와 Workshop rollback 범위를 분리한다. | 불일치 시 자동 restore/delete를 멈춘다. 확인된 task-owned 경로만 복구하고 baseline·타 저장소·공유 Docker 자원은 보존한다. |
| 두 저장소 composite substitution이나 dependency 선언이 drift한다. | `dependencyInsight`가 published `2.1.0`을 선택하거나 catalog/settings diff가 생기거나 기존 dirty path와 겹친다. | Workshop 작업 전에 clean baseline manifest 및 별도 workflow를 만들고, 기존 catalog alias만 사용해 9개 모듈을 순차 검증한다. | 해당 module에서 중지한다. baseline manifest의 task-owned 경로만 되돌리고 baseline status와 비교한 뒤 `--include-build` compile, `dependencyInsight`, test를 그 모듈부터 순차 재실행한다. |
| Docker/Testcontainers 또는 이전 module의 누적 상태가 후속 검증을 오염시킨다. | 동시 container task, baseline 실패를 새 회귀로 분류, 또는 이전 module 실패 후 다음 module 진행. | 기존 테스트와 migration 테스트를 모두 module 단위로 순차 실행하고 baseline 실패를 먼저 분리 기록한다. `set -euo pipefail` loop는 첫 실패에서 멈춘다. | 다음 module로 진행하지 않는다. 실패 원인과 baseline 여부를 기록하고 해당 module을 단독 재실행한 뒤 전체 migration loop를 재개한다. |

### Task 1 — 기준 상태와 기존 Tink JVM 계약 고정

**Files / write scope:** baseline review evidence와 새 Workshop task worktree만 다룬다. Projects branch `feat/digest-api`는 이미 격리된 task worktree다. Workshop source/build 파일은 별도 Type A workflow를 시작한 다음 Task 6에서 수정한다.

- [ ] **Step 1: 기존 Projects task worktree 기준 확인**

```bash
git status --short --branch
git rev-parse HEAD
git rev-parse HEAD^
git diff HEAD^ HEAD --name-only
git diff HEAD^ HEAD -- io/io io/tink
```

Expected: 문서 게이트 커밋의 부모가 source baseline `06634e9ebf985258f8f1c48aac3b6c6ff1853d7a`이며, 현재 작업 트리가 clean하다. 이전 커밋 경로 목록은 계획 문서 네 개뿐이고 마지막 source diff가 비어 있어 코드/test 변경이 없다.

- [ ] **Step 2: Workshop setup workflow 시작 후 isolated worktree 생성**

```bash
git -C /Users/debop/work/bluetape4k/bluetape4k-workshop status --short --branch
git -C /Users/debop/work/bluetape4k/bluetape4k-workshop rev-parse HEAD
git -C /Users/debop/work/bluetape4k/bluetape4k-workshop check-ignore -q .worktrees
git -C /Users/debop/work/bluetape4k/bluetape4k-workshop worktree list --porcelain
git -C /Users/debop/work/bluetape4k/bluetape4k-workshop branch --list feat/digest-api-consumers
test ! -e /Users/debop/work/bluetape4k/bluetape4k-workshop/.worktrees/feat/digest-api-consumers
```

canonical Workshop 저장소의 현재 branch, HEAD, status, ignored `.worktrees`, 기존 worktree와 branch 유무를 먼저 확인한다. worktree/branch 생성은 Workshop repository state를 바꾸므로 canonical checkout에서 별도 Type E `digest-api-worktree-setup` run을 먼저 initialize·approve·start하고 `worktree-creation` component/check를 등록한다. 정확한 새 path에 `mutation-check`를 통과한 다음에만 다음 명령을 실행한다.

```bash
git -C /Users/debop/work/bluetape4k/bluetape4k-workshop worktree add \
  .worktrees/feat/digest-api-consumers -b feat/digest-api-consumers develop
export WORKSHOP_WORKTREE=/Users/debop/work/bluetape4k/bluetape4k-workshop/.worktrees/feat/digest-api-consumers
export PROJECTS_WORKTREE=/Users/debop/work/bluetape4k/bluetape4k-projects/.worktrees/feat-digest-api
```

Expected: 새 branch/worktree는 `develop`에서 시작하며 기존 `open-next-development` worktree와 분리된다. `git worktree list --porcelain`과 새 worktree의 `git status --short --branch`를 읽어 확인한 뒤 setup run에 결과를 기록하고 종료한다. canonical `develop`은 수정하지 않는다. 이후 source/build 수정 전에 Task 6에서 새 worktree 전용 Type A run을 시작한다.

- [ ] **Step 3: Tink 기존 공개 JVM signature 기록**

```bash
./gradlew :bluetape4k-tink:classes
javap -classpath io/tink/build/classes/kotlin/main -public -s \
  io.bluetape4k.tink.digest.TinkDigester \
  io.bluetape4k.tink.digest.TinkDigesters \
  | tee /private/tmp/issue-1917-tink-public-before.txt
```

Expected: constructor, `getAlgorithmName()`, digest/matches overload, `toString()`, preset 접근 결과를 `/private/tmp/issue-1917-tink-public-before.txt`에 보존하고 핵심 descriptor와 기존 테스트 결과를 baseline review evidence에 기록한다. 기존 소스가 각 호출에서 `MessageDigest.getInstance(algorithmName)`을 사용하는 JDK provider 경로임을 확인하고 MD5, SHA-1, SHA-256, SHA-384, SHA-512별 provider name/class 및 unsupported algorithm의 실패 시점을 함께 기록한다. 기존 소스·테스트 결과도 다음 Task 전에 확인한다. 같은 path/branch가 이미 있으면 덮어쓰거나 재사용하지 않고 현재 상태를 확인해 task-owned worktree인지 증명한 뒤에만 이어간다.

### Task 2 — 공통 API contract tests를 먼저 작성하고 실패 확인

**Files:** 신규 Kotlin tests `DigesterTest.kt`, `DigestSessionTest.kt`, `Base64CodecTest.kt`; Java fixture `DigestersJavaContractTest.java`.

- [ ] **Step 1: SHA-256과 Base64 기대값 고정**

```kotlin
Digesters.SHA256.digest("abc".toByteArray(Charsets.UTF_8)) shouldBeEqualTo
    HexFormat.of().parseHex("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")
Digesters.SHA256.digestHexUtf8("abc") shouldBeEqualTo
    "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
Digesters.SHA256.digestBase64Utf8("abc") shouldBeEqualTo
    "ungWv48Bz-pBQUDeXa4iI7ADYaOWF3qctBD_YfIAFa0="
Base64Codec.encode(HexFormat.of().parseHex("fbff")) shouldBeEqualTo "-_8="
Base64Codec.decode("-_8=") shouldBeEqualTo HexFormat.of().parseHex("fbff")
```

위 두 assertion은 format을 생략한 `Base64Codec` 기본 encode/decode overload가 `URL_SAFE_PADDED`임을 직접 고정한다. Standard/URL-safe padded/unpadded 4형식의 왕복과 wrong alphabet, padding mismatch, whitespace, malformed input, noncanonical pad bits 거부도 모두 고정한다. `matchesBase64` 두 overload는 각 형식의 canonical 입력 및 wrong alphabet, padding mismatch, whitespace, noncanonical pad bits를 테스트한다. decoder/canonical-input `IllegalArgumentException`만 `false`로 매핑하고 digest/provider 및 다른 예외는 전달하는지 확인한다.

- [ ] **Step 2: session lifecycle·입력 범위·동시 격리 테스트 추가**

`DigestSession`에는 public `reset()`이 없다. 성공한 `digest()`/`digestHex()`는 JDK `MessageDigest.digest()`처럼 세션을 빈 입력 상태로 되돌린다. 따라서 반복 digest는 빈 입력 결과를 내고 digest 후 update는 새 계산을 시작한다. 이 상태표와 `update(bytes)`, `update(bytes, offset, length)`, `update(byte)`, 다중 update 결합, 빈 입력을 각각 테스트한다. 잘못된 범위 입력은 기존 JDK 범위 예외를 전달하고 byte를 추가하지 않아 세션을 재사용할 수 있음을 검증한다. provider 예외는 숨기지 않으며 해당 session은 결과 상태를 보장하지 않으므로 폐기한다. 지원하지 않는 알고리즘은 `JdkDigester("unsupported")` 생성은 성공하고 `newSession()` 및 one-shot/matcher 경로에서 `NoSuchAlgorithmException`이 발생하며 fallback하지 않는지 확인한다. 반환 배열은 caller-owned mutable 값이고 자동 zeroization하지 않는다는 KDoc을 검증한다. 공유 `Digesters.SHA256`의 one-shot/matcher 호출과 독립 session을 barrier로 동시에 실행해 독립 oracle과 비교한다. 임의의 algorithm 이름은 신뢰된 설정에서만 받으며 사용자 입력으로 선택하지 않는 정책도 확인한다.

- [ ] **Step 3: Java fixture에서 실제 필드와 overload 호출**

```java
byte[] digest = Digesters.SHA256.digest("abc".getBytes(StandardCharsets.UTF_8));
String defaultBase64 = Digesters.SHA256.digestBase64("abc".getBytes(StandardCharsets.UTF_8));
String explicitBase64 = Digesters.SHA256.digestBase64(
    "abc".getBytes(StandardCharsets.UTF_8), Base64Format.URL_SAFE_UNPADDED);
String codecDefault = Base64Codec.encode(new byte[] {(byte) 0xfb, (byte) 0xff});
byte[] codecDecoded = Base64Codec.decode("-_8=");
String codecExplicit = Base64Codec.encode(
    new byte[] {(byte) 0xfb, (byte) 0xff}, Base64Format.STANDARD_UNPADDED);
byte[] codecExplicitDecoded = Base64Codec.decode("+/8", Base64Format.STANDARD_UNPADDED);
assertEquals("ungWv48Bz-pBQUDeXa4iI7ADYaOWF3qctBD_YfIAFa0=", defaultBase64);
assertEquals("ungWv48Bz+pBQUDeXa4iI7ADYaOWF3qctBD/YfIAFa0", explicitBase64);
assertEquals("-_8=", codecDefault);
assertArrayEquals(new byte[] {(byte) 0xfb, (byte) 0xff}, codecDecoded);
assertEquals("+/8", codecExplicit);
assertArrayEquals(new byte[] {(byte) 0xfb, (byte) 0xff}, codecExplicitDecoded);
```

fixture는 `Digesters.SHA256`의 `@JvmField` 정적 필드, Base64 기본/명시 overload, `Base64Codec`의 `@JvmStatic` 기본/format overload가 `URL_SAFE_PADDED` 및 지정 형식을 사용하는지 compile/run하고 `javap -public -s` descriptor를 확인한다.

- [ ] **Step 4: API가 없는 상태에서 새 테스트가 실패하는지 확인**

```bash
./gradlew :bluetape4k-io:test --tests 'io.bluetape4k.io.digest.*'
```

Expected: 아직 API type이 없으므로 compile 단계에서 `Digesters`/`Digester` unresolved reference로 실패한다. 이 결과는 TDD red evidence로 기록하고 이 단계만을 완료 검증으로 간주하지 않는다.

### Task 3 — `io/io` 구현과 Tink 의존 방향 변경

**Files:** Task 2의 테스트, `io/io/src/main/kotlin/io/bluetape4k/io/digest/{Digester,DigestSession,JdkDigester,Digesters,Base64Codec,Base64Format}.kt`, `io/io/build.gradle.kts`.

- [ ] **Step 1: public contract를 명세와 동일하게 구현**

```kotlin
interface Digester {
    val algorithm: String
    fun newSession(): DigestSession
    fun digest(data: ByteArray): ByteArray
    fun digestUtf8(data: String): ByteArray
    fun digestBase64(data: ByteArray): String
    fun digestBase64(data: ByteArray, format: Base64Format): String
    fun digestBase64Utf8(data: String): String
    fun digestBase64Utf8(data: String, format: Base64Format): String
    fun digestHex(data: ByteArray): String
    fun digestHexUtf8(data: String): String
    fun matches(data: ByteArray, expectedDigest: ByteArray): Boolean
    fun matchesHex(data: ByteArray, expected: String): Boolean
    fun matchesHexUtf8(data: String, expected: String): Boolean
    fun matchesBase64(data: ByteArray, expected: String): Boolean
    fun matchesBase64(data: ByteArray, expected: String, format: Base64Format): Boolean
}
```

각 public type/method에는 한국어 KDoc을 작성한다. 세션은 mutable·non-thread-safe이며 `JdkDigester.newSession()`마다 새 JDK `MessageDigest`를 만든다. public `reset()`은 없고 성공 `digest()`/`digestHex()`가 현재 상태를 소비한 뒤 빈 상태로 돌린다. 범위 검증 실패는 기존 JDK 예외를 내고 상태를 보존하며, provider 예외 후에는 session을 버려야 한다. 이 전이를 테스트한다. reset은 secure erasure가 아님을 밝히고 입력 배열은 저장하지 않으며 반환 배열은 caller-owned mutable 값이고 자동 zeroization하지 않는다고 쓴다. 임의 algorithm 이름은 신뢰된 설정에서만 허용하며 user input으로 선택하지 않는다. 지원하지 않는 이름은 생성 시 검증하지 않고 `newSession()`/연산에서 `NoSuchAlgorithmException`으로 드러나며 fallback하지 않는다. 보안 민감 unkeyed digest는 `Digesters.SHA256`, MD5/SHA-1은 비보안 레거시 checksum만 사용한다. 인증·무결성에는 MAC/signature를 선택한다. API는 동기 CPU 연산으로 cancellation-aware하지 않음을 KDoc에 밝히고 coroutine caller는 적절한 dispatcher를 선택하며 `CancellationException`을 삼키지 않는다. digest 비교는 `MessageDigest.isEqual`을 사용한다.

- [ ] **Step 2: strict Base64 format 처리와 `Digesters` 구현**

`Base64Format`은 `STANDARD_PADDED`, `STANDARD_UNPADDED`, `URL_SAFE_PADDED`, `URL_SAFE_UNPADDED` 네 값을 가진다. `Base64Codec`의 format 생략 encode/decode overload는 spec의 기본값 `URL_SAFE_PADDED`를 명시적으로 사용하며 Kotlin test와 Java fixture가 default direct call을 확인한다. 명시 overload는 선택한 JDK decoder 뒤에 같은 format으로 재인코딩해 canonical equality를 확인한다. `matchesBase64`는 malformed/canonical 검사에서 발생한 `IllegalArgumentException`만 `false`로 바꾸고 다른 예외는 전달한다. `Digesters.kt`에는 `@JvmField val SHA256: Digester = JdkDigester("SHA-256")`만 공개한다. 별칭·추가 알고리즘 preset·새 dependency는 추가하지 않는다.

- [ ] **Step 3: 역방향 `io/io -> io/tink` compile-only 의존성 제거**

`io/io/build.gradle.kts`에서 `compileOnly(project(":bluetape4k-tink"))`를 삭제한다. 새 digest source가 Tink type을 참조하지 않는지 확인한다.

- [ ] **Step 4: API contract tests를 통과시킨다**

```bash
./gradlew :bluetape4k-io:test --tests 'io.bluetape4k.io.digest.*'
./gradlew :bluetape4k-io:detekt
```

```bash
set -o pipefail
rg -n '\bDigestUtils\b' io/io/src io/tink/src
symbol_status=$?
rg --files io/io/src io/tink/src | rg '(^|/)DigestUtils[^/]*$'
file_status=$?
test "$symbol_status" -eq 1
test "$file_status" -eq 1
```

Expected: Kotlin contract tests와 Java fixture가 모두 통과한다. 두 `rg` 검색은 결과 없음(exit 1)만 성공으로 취급하고 검색 오류(exit 2 이상)는 실패시킨다. 실패 시 해당 test부터 수정한 뒤 같은 명령을 다시 실행한다.

### Task 4 — 기존 `TinkDigester` adapter와 ABI 호환성 보존

**Files:** `io/tink/build.gradle.kts`, `io/tink/src/main/kotlin/io/bluetape4k/tink/digest/TinkDigester.kt`, 기존 `TinkDigesterTest.kt`, 필요한 ABI test.

- [ ] **Step 1: `io/tink -> io/io` 공개 의존성 추가**

`io/tink/build.gradle.kts`에서 `api(project(":bluetape4k-io"))`를 선언하고 기존 Tink dependencies는 보존한다. `io/io/build.gradle.kts`에는 Tink 의존이 남지 않아야 한다.

- [ ] **Step 2: 기존 모든 메서드를 공통 구현에 위임**

`TinkDigester`는 `Digester`를 구현하고 `override val algorithm`을 기존 `algorithmName`과 같은 값으로 노출한다. 기존 생성자 `TinkDigester(val algorithmName: String)`, Kotlin property/JVM `getAlgorithmName()` ABI, 기존 digest/matches overload와 `toString()` 형식을 유지한다. 내부에는 기존과 같은 JDK provider selection을 하는 비공개 `JdkDigester`를 두고 `algorithmName`을 변경 가능하게 만들지 않는다. Task 1에서 기록한 MD5, SHA-1, SHA-256, SHA-384, SHA-512 provider name/class 및 known vector를 pre/post로 대조한다. 다섯 기존 preset의 모든 기존 digest/matcher overload와 adapter의 `newSession()`/신규 interface 경로에서 결과, `NoSuchAlgorithmException` 실패 시점과 fallback 부재를 테스트한다. `TinkDigesterTest.kt`에는 다음 compile-time assignment와 Kotlin property 검증을 둔다. Kotlin에서 Java accessor 이름을 직접 호출하지 않으며, JVM `getAlgorithmName()`은 Java fixture와 `javap`로 검증한다.

```kotlin
val adapter = TinkDigester("SHA-256")
val contract: Digester = adapter
contract.algorithm shouldBeEqualTo adapter.algorithmName
```

`Digester` 구현으로 인해 아래에 명시한 새 public JVM descriptor는 추가될 수 있다. 기존 descriptor는 모두 보존하고 신규 descriptor는 allowlist와 정확히 일치해야 한다. `digest(String)`은 UTF-8 + `URL_SAFE_PADDED`; malformed legacy Base64 expected는 기존처럼 `false`; hex는 lowercase·정확한 길이 계약을 유지한다. `TinkDigesters` preset, lazy 초기화와 property type은 바꾸지 않는다.

- [ ] **Step 3: 기존 tests와 신규 interface 경로를 확인**

```bash
./gradlew :bluetape4k-tink:test --tests 'io.bluetape4k.tink.digest.TinkDigesterTest'
./gradlew :bluetape4k-tink:test --tests 'io.bluetape4k.tink.digest.*'
```

Expected: 기존 golden Base64·hex·malformed input 결과와 각 preset 동작이 그대로 통과한다. 신규 interface fixture가 adapter의 `algorithm`/digest 결과를 기존 public 호출과 비교한다. 별도로 `:bluetape4k-io`의 `compileClasspath`에는 `bluetape4k-tink`가 없어야 하고, `:bluetape4k-tink`의 `compileClasspath`는 `project :bluetape4k-io`를 선택해야 한다. Task 7에서 두 `dependencyInsight` 결과를 기계적으로 검사한다.

- [ ] **Step 4: 기존 ABI baseline과 신규 bytecode 대조**

```bash
javap -classpath io/tink/build/classes/kotlin/main -public -s \
  io.bluetape4k.tink.digest.TinkDigester \
  io.bluetape4k.tink.digest.TinkDigesters \
  | tee /private/tmp/issue-1917-tink-public-after.txt
```

before/after `javap -public -s` 출력과 unified diff를 보존한다. `TinkDigester`에서 허용하는 class header 변경은 `implements io.bluetape4k.io.digest.Digester`뿐이다. 기존 descriptor는 모두 유지하고, 기존에 없던 public method descriptor는 아래 13개와 정확히 같아야 한다. 이 allowlist는 구현 계약의 `getAlgorithm`, `newSession`, `digestUtf8`, `digestBase64` 두 overload, `digestBase64Utf8` 두 overload, `digestHex(ByteArray)`, `digestHexUtf8`, `matchesHex(ByteArray, String)`, `matchesHexUtf8`, `matchesBase64` 두 overload에 대응한다.

```text
getAlgorithm()Ljava/lang/String;
newSession()Lio/bluetape4k/io/digest/DigestSession;
digestUtf8(Ljava/lang/String;)[B
digestBase64([B)Ljava/lang/String;
digestBase64([BLio/bluetape4k/io/digest/Base64Format;)Ljava/lang/String;
digestBase64Utf8(Ljava/lang/String;)Ljava/lang/String;
digestBase64Utf8(Ljava/lang/String;Lio/bluetape4k/io/digest/Base64Format;)Ljava/lang/String;
digestHex([B)Ljava/lang/String;
digestHexUtf8(Ljava/lang/String;)Ljava/lang/String;
matchesHex([BLjava/lang/String;)Z
matchesHexUtf8(Ljava/lang/String;Ljava/lang/String;)Z
matchesBase64([BLjava/lang/String;)Z
matchesBase64([BLjava/lang/String;Lio/bluetape4k/io/digest/Base64Format;)Z
```

아래 Python 검증기는 `javap`의 public field/method name과 descriptor를 정규화한다. 기존 public symbol이 제거되면 실패하고, 신규 method symbol 집합이 위 allowlist와 다르면 실패한다. `TinkDigesters` public symbol 집합은 baseline과 완전히 같아야 한다. `diff -u` 종료 코드 0/1/>1을 명시 분기하고 `set -euo pipefail` 아래에서 비교 결과를 보존하며, 예상 외 차이를 출력 후 성공 처리하지 않는다.

```bash
set -euo pipefail
if diff -u /private/tmp/issue-1917-tink-public-before.txt /private/tmp/issue-1917-tink-public-after.txt > /private/tmp/issue-1917-tink-public-diff.txt; then
  diff_status=0
else
  diff_status=$?
fi
if [[ "$diff_status" -gt 1 ]]; then
  cat /private/tmp/issue-1917-tink-public-diff.txt
  exit "$diff_status"
fi
python3 - /private/tmp/issue-1917-tink-public-before.txt /private/tmp/issue-1917-tink-public-after.txt <<'PY'
from pathlib import Path
import re
import sys

before, after = (Path(path).read_text() for path in sys.argv[1:])
allowed = {
    "getAlgorithm()Ljava/lang/String;",
    "newSession()Lio/bluetape4k/io/digest/DigestSession;",
    "digestUtf8(Ljava/lang/String;)[B",
    "digestBase64([B)Ljava/lang/String;",
    "digestBase64([BLio/bluetape4k/io/digest/Base64Format;)Ljava/lang/String;",
    "digestBase64Utf8(Ljava/lang/String;)Ljava/lang/String;",
    "digestBase64Utf8(Ljava/lang/String;Lio/bluetape4k/io/digest/Base64Format;)Ljava/lang/String;",
    "digestHex([B)Ljava/lang/String;",
    "digestHexUtf8(Ljava/lang/String;)Ljava/lang/String;",
    "matchesHex([BLjava/lang/String;)Z",
    "matchesHexUtf8(Ljava/lang/String;Ljava/lang/String;)Z",
    "matchesBase64([BLjava/lang/String;)Z",
    "matchesBase64([BLjava/lang/String;Lio/bluetape4k/io/digest/Base64Format;)Z",
}

def classes(output):
    result = {}
    active = None
    previous = ""
    for line in output.splitlines():
        current = line.strip()
        if current.startswith("public ") and current.endswith("{") and " class " in current:
            name = re.search(r"\bclass\s+([A-Za-z0-9_.$]+)", current).group(1)
            active = name
            result[active] = {"header": current[:-1].strip(), "symbols": set()}
            previous = ""
            continue
        if active is None:
            continue
        if current.startswith("descriptor:"):
            descriptor = current.split(":", 1)[1].strip()
            if "(" in previous:
                name = re.search(r"([A-Za-z_$][A-Za-z0-9_$]*)\(", previous).group(1)
                result[active]["symbols"].add(name + descriptor)
            elif previous.endswith(";"):
                result[active]["symbols"].add("FIELD:" + previous + descriptor)
            previous = ""
            continue
        if current.endswith(";"):
            previous = current
    return result

old_classes, new_classes = classes(before), classes(after)
if set(old_classes) != set(new_classes):
    raise SystemExit(f"ABI class set changed: before={sorted(old_classes)}, after={sorted(new_classes)}")
tink_name = "io.bluetape4k.tink.digest.TinkDigester"
tink_digesters_name = "io.bluetape4k.tink.digest.TinkDigesters"
old_tink, new_tink = old_classes[tink_name], new_classes[tink_name]
old_digesters, new_digesters = old_classes[tink_digesters_name], new_classes[tink_digesters_name]
expected_header = old_tink["header"] + " implements io.bluetape4k.io.digest.Digester"
if new_tink["header"] != expected_header:
    raise SystemExit(f"unexpected TinkDigester class header: {new_tink['header']}")
if old_digesters != new_digesters:
    raise SystemExit("TinkDigesters public header or symbols changed")
old_symbols, new_symbols = old_tink["symbols"], new_tink["symbols"]
removed = old_symbols - new_symbols
added = new_symbols - old_symbols
if removed or added != allowed:
    raise SystemExit(f"ABI mismatch: removed={sorted(removed)}, unexpected_added={sorted(added - allowed)}, missing_added={sorted(allowed - added)}")
print(f"PASS: existing {len(old_symbols)} TinkDigester public symbols and TinkDigesters API preserved; {len(added)} allowlisted Digester methods added")
PY
```

기존 constructor, 모든 기존 method descriptor, `getAlgorithmName(): java.lang.String`, `TinkDigester(algorithm=<name>)`, `TinkDigesters` preset field/getter/property type는 그대로여야 한다. 이 검증이 실패하거나 `TinkDigesters`가 달라지면 task를 중단한다.

### Task 5 — README, Java ABI, benchmark와 Projects 검증

**Files:** `io/io/README.md`, `io/io/README.ko.md`, `io/io/src/test/kotlin/io/bluetape4k/io/digest/DigestersReadmeExampleTest.kt`, `io/io/src/test/kotlin/io/bluetape4k/io/benchmark/DigestBenchmark.kt`, `io/io/src/test/kotlin/io/bluetape4k/io/benchmark/README.md`, Java fixture. 기존 `io/io/Benchmark.md`는 명령 참고용이며 결과를 기록할 두 번째 문서는 만들지 않는다.

- [ ] **Step 1: 양 언어 README에 명세 예제 추가**

영문 `README.md`와 한국어 `README.ko.md`의 기존 언어에 맞춰 `Digesters.SHA256`의 raw bytes, `digestUtf8`, 형식 명시 Base64, 세션 incremental update 예제를 추가한다. 두 README에서 marker로 감싼 Kotlin code fence를 두고, `DigestersReadmeExampleTest.kt`의 같은 marker 사이에 둔 compileable API sample body와 공백까지 정확히 비교한다. JUnit test는 해당 sample function을 실제 호출하고 각 README code fence가 함수 body와 byte-for-byte 일치하는지 검증한다. 기본 overload가 `URL_SAFE_PADDED`를 사용한다는 점과 영속 데이터·wire format·외부 계약은 형식을 명시해야 한다는 안내를 README 두 locale과 format 생략 메서드 KDoc에 쓴다. 각 사용 예는 기존 입력 framing을 그대로 전달하며 인증에는 MAC/signature가 필요하다는 경계를 설명한다. 모든 API KDoc은 한국어로 작성한다.

- [ ] **Step 2: 동등한 benchmark 입력과 네 측정 경로 구현**

`DigestBenchmark.kt`는 `@State(Scope.Thread)`, `@BenchmarkMode(Mode.Throughput)`, warmup 2회/1초, measurement 5회/1초, `@Fork(2)`, `@Threads(1)`과 `@Param("64", "1024", "65536")`를 사용한다. 이 결과는 단일 thread의 raw-digest microbenchmark이며 Workshop 소비자 성능을 대변하지 않는다. payload는 `ByteArray(size) { index -> ((index * 31 + size) % 256).toByte() }`로 만들고 `splitIndex = size / 2`에서 나눈다. `@Setup`에서 각 경로의 결과 equality를 검증한다. one-shot family의 직접 JDK 경로는 timed method 안에서 `MessageDigest.getInstance("SHA-256")`를 만들고 digest하며, API 경로는 `Digesters.SHA256.digest(payload)`를 호출한다. incremental family는 새-session 경로에서 timed method 안에 `newSession()`과 두 조각 update/digest를 모두 포함한다. thread-scoped 재사용-session 경로는 `@Setup(Level.Trial)`에서 session을 만들고 timed method에는 두 조각 update/digest만 포함한다. 성공한 `digest()`가 session을 빈 상태로 되돌리는 계약에 따라 매 invocation의 입력은 독립적이다. 각 경로의 timed region을 기록하고, JDK one-shot/API one-shot 및 새 session/재사용 session처럼 동일 family 안에서도 초기화 비용 포함 여부가 다른 수치는 비용 귀속을 분리해 표시한다. 두 family를 서로 순위화하지 않는다. 각 benchmark는 raw digest `ByteArray`를 반환한다. 결과는 characterization-only로 제한해 성능 향상·회귀 또는 adapter 우열 결론을 내리지 않는다. 결과 README에 Git HEAD와 `DigestBenchmark.kt` SHA-256, Gradle Wrapper·Kotlinx Benchmark·JMH 버전, JDK vendor/version/flags, OS/kernel/architecture, CPU model/core count, 정확한 명령, workload와 payload별 SHA-256, fork/thread/warmup/measurement 설정, 모든 fork의 score±error를 기록한다. 원시 JSON 또는 완전한 JMH stdout도 같은 `io/io/src/test/kotlin/io/bluetape4k/io/benchmark/README.md`에 보존해 임시 파일만 남지 않게 한다. 실행 명령은 `io/io/Benchmark.md`를 참고하고 결과·명령·원시 출력의 기준 저장소 위치는 이 README 한 곳으로 고정한다.

- [ ] **Step 3: Java descriptor 및 Projects targeted verification**

```bash
./gradlew :bluetape4k-io:test --tests 'io.bluetape4k.io.digest.*'
./gradlew :bluetape4k-tink:test --tests 'io.bluetape4k.tink.digest.*'
./gradlew :bluetape4k-io:testBenchmarkCompile
./gradlew :bluetape4k-io:testBenchmark
git diff --check
```

Expected: 모든 targeted test, Java consumer fixture, 양 README의 compile/run sample 일치 확인, benchmark compile/run 통과. 지정한 단일 결과 README에 timed region 및 session 생성 비용 포함 여부, deterministic payload SHA-256, Git/source hash, Gradle·Kotlinx Benchmark·JMH·JDK 버전, OS/kernel/architecture, CPU/core, JVM flags, workload family별 payload/fork score±error, 정확한 명령과 저장소 보존 원시 결과를 기록한다. one-shot/incremental family의 성능 우열이나 실제 consumer 성능 주장은 쓰지 않는다. 실패한 benchmark는 재실행 전 원인을 분류한다.

### Task 6 — Workshop 소비자 9 modules / 29 files / 35 calls migration

**Files:** 아래 appendix의 정확한 production source paths와 앞선 파일 지도에 적은 9개 module Gradle files. test oracle, HMAC, fixture 데이터, 사전 생성된 저장 값은 수정하지 않는다.

- [ ] **Step 1: 별도 Workshop Type A workflow run 시작**

새로운 Workshop task worktree를 생성하고 status/HEAD가 clean인지 확인한 다음, Workshop worktree에서 `$bluetape-workflow`를 독립 실행한다. 이 run은 Projects worktree에 등록된 receipt를 대체하지 않으며 Workshop 저장소의 source/build 파일 변경만 담당한다. 승인된 이 plan과 Projects spec SHA-256을 근거로 Type A `digest-api-consumers` component를 등록하고 `baseline`, `migration`, `dependency-selection`, `sequential-tests`, `rollback`, `seven-tier-review`, `lesson` check를 요구한다. `init → run-approve → run-start → main lane 생성·시작·startup-ack → topology-register` 순서를 따라 main lane을 component owner로 지정하고 모든 component가 포함된 topology를 등록한 뒤 Workshop worktree 내부의 receipt/owner handle을 확인한다. 첫 Workshop source 또는 Gradle 파일 수정 전에 모든 write target에 `mutation-check`를 통과시키고 변경 전후 lane evidence와 changed paths를 기록한다. Workflow receipt/owner secret은 리뷰 문서나 터미널 증거에 복사하지 않고 run ID만 최종 보고서에 기록한다.

- [ ] **Step 2: module별 pre-migration baseline manifest 작성**

각 module을 수정하기 전에 `docs/review/2026-11-24-issue-1917-digest-migration-review.md`에 module 표와 callsite parity 표를 기록한다. module 표 열은 `Module path | Gradle project | Baseline HEAD | Baseline status paths | Task-owned paths | Rollback result`다. 새 task worktree가 clean하지 않으면 변경을 시작하지 않고 기존 경로를 보존한다. parity 표는 35개 각각에 대해 `id | file/function | legacy call | ordered input/framing | output representation | deterministic test | pre-migration bytes/value | post-migration bytes/value`를 기록한다. 기존 테스트가 callsite를 실행하지 않으면 source 편집 전에 좁은 deterministic regression fixture를 추가하고 baseline output을 저장한다. 각 경로의 결과 byte/string/wire value가 기록되지 않으면 다음 module로 진행하지 않는다. `Task-owned paths`에는 source/test/build/review artifact를 편집 전에 먼저 추가하는 write-ahead journal을 사용하며 task file은 검증 완료 전 stage하지 않는다.

```bash
git -C "$WORKSHOP_WORKTREE" rev-parse HEAD
git -C "$WORKSHOP_WORKTREE" status --short --untracked-files=all
```

Expected: `develop`에서 분기한 task worktree가 clean이고 baseline HEAD/status가 아홉 module 행에 기록된다.

- [ ] **Step 3: 기존 consumer module tests를 변경 전에 순차 실행**

각 명령을 단독으로 실행하고 결과를 manifest 리뷰 artifact에 기록한다. 이전 baseline test failure와 이번 migration으로 생긴 failure를 구분한다.

```bash
"$WORKSHOP_WORKTREE/gradlew" -p "$WORKSHOP_WORKTREE" :commerce-concert-ticket-flash-sale:test
"$WORKSHOP_WORKTREE/gradlew" -p "$WORKSHOP_WORKTREE" :commerce-event-sourced-promotion-voucher-campaign:test
"$WORKSHOP_WORKTREE/gradlew" -p "$WORKSHOP_WORKTREE" :commerce-order-lifecycle-fulfillment:test
"$WORKSHOP_WORKTREE/gradlew" -p "$WORKSHOP_WORKTREE" :commerce-pre-generated-voucher-pool:test
"$WORKSHOP_WORKTREE/gradlew" -p "$WORKSHOP_WORKTREE" :commerce-promotion-voucher-campaign:test
"$WORKSHOP_WORKTREE/gradlew" -p "$WORKSHOP_WORKTREE" :commerce-usage-metering-billing-event-sourcing:test
"$WORKSHOP_WORKTREE/gradlew" -p "$WORKSHOP_WORKTREE" :commerce-usage-metering-billing-ledger:test
"$WORKSHOP_WORKTREE/gradlew" -p "$WORKSHOP_WORKTREE" :commerce-shared:test
"$WORKSHOP_WORKTREE/gradlew" -p "$WORKSHOP_WORKTREE" :commerce-reservation-control-plane:test
```

Docker/Testcontainers tests remain sequential. A baseline failure is diagnosed and recorded before source migration proceeds.

- [ ] **Step 4: 각 consumer module에 기존 catalog alias 연결**

기존 dependency scope를 따라 8개 build file에 이미 등록된 `libs.bluetape4k.io` alias를 선언한다. 변경 전에 alias의 module 좌표와 요구 version을 catalog에서 읽어 review artifact에 기록하고, `pre-generated-voucher-pool/build.gradle.kts`의 기존 선언은 유지한다. 사용 중인 old digest API나 신규 call site에서 deprecation/compiler warning이 확인되면 선언 위치와 실제 대체 필요성을 기록하고, 필요한 대체만 official source 근거와 함께 적용한다. 새 dependency, 중앙 catalog alias/version, `settings.gradle.kts` 또는 `gradle/libs.versions.toml` 변경은 이 계획에 포함되지 않는다.

- [ ] **Step 5: digest 호출별로 framing과 결과 표현 보존**

명세의 module/file별 migration table과 35-row parity 표를 한 호출씩 대조한다. 문자열은 `digestUtf8`/`digestHexUtf8`; bytes는 raw `digest`/`digestHex`; protocol output은 명시 format Base64를 사용한다. 여러 조각 입력은 기존 byte order, separators, length prefixes, sorted fields, nullable markers를 보존하고 필요하면 `newSession().update(...)`로 표현한다. 호출 인자·입력 조합·직렬화·출력 인코딩을 바꾸지 않고 digest expression만 교체하며 pre/post deterministic test output을 byte-for-byte 대조한다. Voucher key-material 처리의 digest byte zeroization과 Base58/checksum 변환도 그대로 보존한다. `VoucherPoolKeyMaterialConfiguration.kt`와 `VoucherCodeService.kt`의 기존 `finally` zeroization을 line-level diff로 기록한다. HMAC/Tink-MAC, test oracle, fixture와 stored value 경로는 변경되지 않았는지 별도 diff check한다. suspend/coroutine caller에서는 `CancellationException`을 보존하고 broad catch/retry를 새로 넣지 않는다. API는 동기 CPU 연산이며 cancellation/cleanup은 N/A, caller dispatcher 선택만 review artifact에 명시한다. 변경 전 각 경로를 task-owned manifest에 추가한다.

- [ ] **Step 6: 단일 consumer 모듈씩 composite dependency와 tests 실행**

아래 순차 loop를 사용해 compileKotlin → dependencyInsight → test를 한 모듈씩 끝낸다. `set -e`로 실패한 task에서 loop가 멈추며 Docker/Testcontainers 모듈을 다른 모듈과 병렬 실행하지 않는다.

```bash
set -euo pipefail
modules=(
  :commerce-concert-ticket-flash-sale
  :commerce-event-sourced-promotion-voucher-campaign
  :commerce-order-lifecycle-fulfillment
  :commerce-pre-generated-voucher-pool
  :commerce-promotion-voucher-campaign
  :commerce-usage-metering-billing-event-sourcing
  :commerce-usage-metering-billing-ledger
  :commerce-shared
  :commerce-reservation-control-plane
)
for module in "${modules[@]}"; do
  "$WORKSHOP_WORKTREE/gradlew" --include-build "$PROJECTS_WORKTREE" -p "$WORKSHOP_WORKTREE" "${module}:compileKotlin"
  module_name="${module#:}"
  "$WORKSHOP_WORKTREE/gradlew" --include-build "$PROJECTS_WORKTREE" -p "$WORKSHOP_WORKTREE" \
    "${module}:dependencyInsight" --dependency io.github.bluetape4k:bluetape4k-io --configuration compileClasspath --console=plain \
    | tee "/private/tmp/issue-1917-workshop-dep-${module_name}.txt"
  "$WORKSHOP_WORKTREE/gradlew" --include-build "$PROJECTS_WORKTREE" -p "$WORKSHOP_WORKTREE" "${module}:test"
done
```

아래 assertion은 module별 결과를 직접 읽는다. 각 파일에서 선택 component가 `project :bluetape4k-io`인지 확인하고, requested Maven coordinate가 출력에 나타나면 같은 선택 줄에 `-> project :bluetape4k-io`가 있어야 한다. 이 기준은 requested coordinate와 최종 selected component를 구분한다. assertion 하나라도 실패하면 다음 module로 진행하지 않고 원인과 해당 module rollback을 migration review에 기록한다. 아홉 결과 파일과 baseline/final output을 모두 보존한다.

```bash
python3 - <<'PY'
from pathlib import Path
import re
modules = [
    "commerce-concert-ticket-flash-sale",
    "commerce-event-sourced-promotion-voucher-campaign",
    "commerce-order-lifecycle-fulfillment",
    "commerce-pre-generated-voucher-pool",
    "commerce-promotion-voucher-campaign",
    "commerce-usage-metering-billing-event-sourcing",
    "commerce-usage-metering-billing-ledger",
    "commerce-shared",
    "commerce-reservation-control-plane",
]
for module in modules:
    path = Path(f"/private/tmp/issue-1917-workshop-dep-{module}.txt")
    output = path.read_text()
    if "project :bluetape4k-io" not in output:
        raise SystemExit(f"{module}: composite project was not selected")
    for line in output.splitlines():
        if "io.github.bluetape4k:bluetape4k-io:2.1.0" in line and "-> project :bluetape4k-io" not in line:
            raise SystemExit(f"{module}: published 2.1.0 remains selected: {line}")
print("PASS: all nine modules select project :bluetape4k-io")
PY
```

- [ ] **Step 7: failure 시 module-scoped rollback 수행**

Projects Task 2–5와 Workshop Task 6 모두 편집 전 clean baseline 및 write-ahead journal을 요구한다. 각 source/test/build/docs 경로는 편집 전에 journal에 등록하고 검증 전에는 stage하지 않는다. `ERR`, `INT`, `TERM`은 현재 task와 다음 module 실행을 멈추고 status/evidence를 보존한다. rollback 전에 `git diff --name-only HEAD`와 `git ls-files --others --exclude-standard`의 합집합을 journal과 대조한다. 집합이 다르면 자동 restore/delete를 중단하고 경로를 조사한다. 정확히 일치하면 Projects는 문서 전용 커밋 이후의 baseline, Workshop은 최초 clean task HEAD를 기준으로 task-owned tracked path만 `git restore --source=<baseline> --staged --worktree -- <path>`로 되돌리고, baseline에 없던 정확한 untracked 파일만 symlink가 아님을 확인한 뒤 삭제한다. staged task 파일이 발견되면 자동 복구를 멈춰 index와 경로를 따로 확인한다. rollback 후 status가 각 clean baseline과 같은지 검증한다. 전체 `git clean`, `reset --hard`, module-wide restore, `killall` 또는 공유 Docker cache 정리는 금지한다. 중단된 Gradle/Testcontainers 프로세스가 종료된 뒤 task-owned container만 식별된 경우 정리하고, 공유 자원은 보존한다. bytes/wire mismatch, compile/test failure 또는 dependency substitution failure가 나면 해당 module 작업을 즉시 중단하고 task-owned 경로를 기준으로 위 절차를 적용한다. rollback 결과가 baseline과 다르면 해당 lane을 완료로 표시하지 않고 남은 정확한 경로를 조사한다.

### Task 7 — 전체 영향 범위, API/ABI와 benchmark 검증

- [ ] **Step 1: 이전 범위 완결성 검사**

Projects의 의존 방향도 독립적으로 확인한다. `:bluetape4k-io`의 `compileClasspath`에는 `bluetape4k-tink`가 없어야 하고 `:bluetape4k-tink`의 `compileClasspath`에는 `project :bluetape4k-io`가 있어야 한다. 검증 출력을 저장하고 no-match 문구 및 project 선택을 Python assertion으로 확인한다.

```bash
set -euo pipefail
./gradlew :bluetape4k-io:dependencyInsight --configuration compileClasspath \
  --dependency bluetape4k-tink --console=plain \
  | tee /private/tmp/issue-1917-io-tink-dependency.txt
./gradlew :bluetape4k-tink:dependencyInsight --configuration compileClasspath \
  --dependency bluetape4k-io --console=plain \
  | tee /private/tmp/issue-1917-tink-io-dependency.txt
python3 - /private/tmp/issue-1917-io-tink-dependency.txt /private/tmp/issue-1917-tink-io-dependency.txt <<'PY'
from pathlib import Path
import sys

io_to_tink, tink_to_io = (Path(path).read_text() for path in sys.argv[1:])
if "No dependencies matching given input were found" not in io_to_tink:
    raise SystemExit("io compileClasspath must have no bluetape4k-tink dependency")
if "project :bluetape4k-io" not in tink_to_io:
    raise SystemExit("tink compileClasspath must select project :bluetape4k-io")
print("PASS: dependency direction is io/tink -> io/io with no reverse edge")
PY
```

Projects source naming guard는 `io/io/src` 및 `io/tink/src`에서 `\bDigestUtils\b`를 검색하고 `DigestUtils*` 파일을 검사한다. 일치가 하나라도 있거나 source root를 읽지 못하면 실패한다.

```bash
python3 - "$PROJECTS_WORKTREE" <<'PY'
from pathlib import Path
import re
import sys

projects = Path(sys.argv[1])
roots = [projects / "io/io/src", projects / "io/tink/src"]
stale_symbols, stale_files = [], []
for root in roots:
    if not root.is_dir():
        raise SystemExit(f"missing source root: {root}")
    for path in root.rglob("*"):
        if not path.is_file():
            continue
        if path.name.startswith("DigestUtils"):
            stale_files.append(str(path.relative_to(projects)))
        if path.suffix in {".kt", ".java", ".md"} and re.search(r"\bDigestUtils\b", path.read_text(errors="replace")):
            stale_symbols.append(str(path.relative_to(projects)))
if stale_symbols or stale_files:
    raise SystemExit(f"stale DigestUtils references/files: symbols={stale_symbols}, files={stale_files}")
print("PASS: no DigestUtils symbol or DigestUtils* source file in Projects io/io or io/tink")
PY
```

```bash
git -C "$WORKSHOP_WORKTREE" diff --check
git -C "$WORKSHOP_WORKTREE" diff -- settings.gradle.kts gradle/libs.versions.toml
git -C "$PROJECTS_WORKTREE" diff --check
python3 - "$WORKSHOP_WORKTREE" "$PROJECTS_WORKTREE" <<'PY'
from pathlib import Path
import re
import subprocess
import sys

workshop, projects = map(Path, sys.argv[1:])
plan = (projects / "docs/superpowers/plans/2026-11-24-issue-1917-shared-digester-plan.md").read_text()
appendix = plan.split("## Workshop migration path appendix\n", 1)[1].split("\n## Traceability", 1)[0]
expected = set(re.findall(r"^\d+\. `([^`]+\.kt)`$", appendix, re.M))
tracked = subprocess.check_output(["git", "-C", str(workshop), "diff", "--name-only", "HEAD"], text=True).splitlines()
untracked = subprocess.check_output(["git", "-C", str(workshop), "ls-files", "--others", "--exclude-standard"], text=True).splitlines()
changed = set(tracked) | set(untracked)
actual_sources = {path for path in changed if path.startswith("commerce/") and "/src/main/kotlin/" in path}
if actual_sources != expected:
    raise SystemExit(f"migration source path mismatch: expected={len(expected)}, actual={len(actual_sources)}")
expected_builds = {
    "commerce/concert-ticket-flash-sale/build.gradle.kts",
    "commerce/event-sourced-promotion-voucher-campaign/build.gradle.kts",
    "commerce/order-lifecycle-fulfillment/build.gradle.kts",
    "commerce/promotion-voucher-campaign/build.gradle.kts",
    "commerce/usage-metering-billing-event-sourcing/build.gradle.kts",
    "commerce/usage-metering-billing-ledger/build.gradle.kts",
    "commerce/shared/build.gradle.kts",
    "commerce/reservation-control-plane/build.gradle.kts",
}
actual_builds = {path for path in changed if path.startswith("commerce/") and path.endswith("/build.gradle.kts")}
if actual_builds != expected_builds:
    raise SystemExit(f"dependency file mismatch: expected={len(expected_builds)}, actual={len(actual_builds)}")
for relative in sorted(expected):
    source = (workshop / relative).read_text()
    if not re.search(r"Digesters\.SHA256\.(?:newSession|digest\w*|matches\w*)", source):
        raise SystemExit(f"missing Digesters.SHA256 API use: {relative}")
    if "DigestUtils" in source:
        raise SystemExit(f"stale DigestUtils reference: {relative}")
    if re.search(r"MessageDigest|TinkDigester|TinkDigesters", source):
        raise SystemExit(f"unmigrated digest implementation: {relative}")
if len(expected) != 29:
    raise SystemExit(f"expected 29 source files, found {len(expected)}")
print(f"PASS: {len(expected)} exact source files use Digesters.SHA256; no stale DigestUtils or old digest symbols")
PY
```

Expected: 명세에 든 29 production files와 35개 논리적 API callsite가 이전된다. 위 정규식은 파일별 API 사용 여부와 오래된 구현의 제거 여부만 확인하며 호출 수는 산출하지 않는다. session chain의 `newSession()`과 뒤이은 `DigestSession.digest*()`는 한 논리 호출에서 여러 표현으로 나타날 수 있으므로, 35개 callsite의 완료 여부는 authoritative한 35-row parity 표의 source locator와 각 pre/post deterministic bytes/string/wire 결과로 증명한다. 표의 모든 행이 일치하고 test oracle/HMAC/fixture 및 명시 제외 경로가 그대로여야 한다. `VoucherPoolKeyMaterialConfiguration.kt`와 `VoucherCodeService.kt`의 `finally` zeroization, coroutine caller의 cancellation 보존, HMAC/Tink-MAC 제외 경로를 독립 diff review로 확인한다. catalog/settings diff는 비어 있어야 한다. 이 중 하나라도 빠지거나 다르면 migration 완료로 처리하지 않는다.

이 검증에 포함된 compile output에서 deprecation warning을 확인해 대상 API와 파일/line을 review artifact에 기록한다. Workshop catalog alias가 요청하는 module coordinate/version은 Step 4의 catalog read-back과 각 module `dependencyInsight` 결과에 기록하고, composite build 선택이 version catalog를 수정하지 않고 해결됐음을 확인한다.

- [ ] **Step 2: 의존 경로와 API descriptor 최종 확인**

각 영향 module에 대해 `:commerce-concert-ticket-flash-sale`, `:commerce-event-sourced-promotion-voucher-campaign`, `:commerce-order-lifecycle-fulfillment`, `:commerce-pre-generated-voucher-pool`, `:commerce-promotion-voucher-campaign`, `:commerce-usage-metering-billing-event-sourcing`, `:commerce-usage-metering-billing-ledger`, `:commerce-shared`, `:commerce-reservation-control-plane` 순서로 `dependencyInsight`를 실행해 `project :bluetape4k-io` 선택을 기록한다. Projects descriptor 확인은 다음처럼 실제 classpath와 네 public type을 고정하고 출력을 review artifact와 대조한다.

```bash
./gradlew :bluetape4k-io:classes
javap -classpath io/io/build/classes/kotlin/main -public -s \
  io.bluetape4k.io.digest.Digesters \
  io.bluetape4k.io.digest.Base64Codec \
  io.bluetape4k.io.digest.Digester \
  io.bluetape4k.io.digest.DigestSession \
  | tee /private/tmp/issue-1917-projects-digest-public.txt
```

Review artifact는 `Digesters.SHA256`의 static field, `Base64Codec` format 생략/명시 overload, `Digester`와 `DigestSession`의 모든 public descriptor를 기록한다. Tink `javap` 결과도 앞서 저장한 baseline과 allowlist parser로 비교한다.

- [ ] **Step 3: 테스트·정적 분석을 저장소 단위로 최종 실행**

```bash
./gradlew :bluetape4k-io:test
./gradlew :bluetape4k-tink:test
./gradlew :bluetape4k-io:detekt
./gradlew :bluetape4k-io:testBenchmarkCompile
./gradlew :bluetape4k-io:testBenchmark
```

모든 Projects 및 Workshop 모듈 test task는 한 번에 한 모듈씩 순차 실행한다. 각 모듈 결과, Java fixture, ABI 비교, benchmark와 두 worktree `git diff --check`가 최종 리뷰 artifact에 기록되기 전에는 검증 완료로 처리하지 않는다.

### Task 8 — 독립 7-tier diff review, Korean lesson과 DoD

- [ ] **Step 1: exact final head 7-tier review**

최종 Projects/Workshop branch 각각에서 `git status`, `git diff --stat`, `git diff --check`, staged/unstaged diff를 읽는다. Source; failed/retried/skipped tests; CI/toolchain; API/ABI; lifecycle/security; migration/operational evidence; history/issue scope를 분리해 검토한다. P0/P1을 모두 수정하고 영향 관점을 같은 exact head에서 다시 검토한다. 원격 PR/CI 증거가 없는 것은 local verification과 분리해 기록한다.

- [ ] **Step 2: Korean review artifact 및 lesson 작성**

`docs/review/2026-11-24-issue-1917-digest-migration-review.md`에 이 계획의 baseline manifest, 모듈별 의존성·test 결과, ABI 비교, benchmark, 최종 판정과 미실행 항목을 기록한다. `docs/lessons/`에는 문제 맥락, 사용자가 바로잡은 `Digesters` 이름, 결정, 결과, 검증 근거, 최초 리뷰에서 발견한 benchmark task와 stability 누락, 다음 변경자용 가드를 한국어로 작성한다.

- [ ] **Step 3: final docs check 및 local commit**

```bash
node /Users/debop/.codex/skills/bluetape-writer/scripts/audit-korean-terms.mjs \
  docs/superpowers/specs/2026-11-23-shared-digester-design.md \
  docs/superpowers/reviews/2026-11-23-shared-digester-spec-review.md \
  docs/superpowers/plans/2026-11-24-issue-1917-shared-digester-plan.md \
  docs/review/2026-11-24-issue-1917-digest-migration-review.md
git diff --check
```

Expected: 용어 감사 0건, whitespace 오류 0건, task-owned paths 밖의 변경 0건. 양 저장소에서 각각 `completion-check` 결과에 누락 lane/check/evidence가 없음을 확인한 뒤 해당 run의 `complete`를 기록한다. Lore protocol을 지키는 한국어 intent line과 decision trailers를 사용해 Projects/Workshop feature branches에 local commit한다. Push, PR, issue close, merge, tag, publish, release는 실행하지 않는다.

## Workshop migration path appendix

명세의 migration table을 기준으로 아래 29 production files만 이전한다.

1. `commerce/concert-ticket-flash-sale/src/main/kotlin/io/bluetape4k/workshop/commerce/ticket/config/TicketMigrationRunner.kt`
2. `commerce/concert-ticket-flash-sale/src/main/kotlin/io/bluetape4k/workshop/commerce/ticket/idempotency/IdempotencyFingerprint.kt`
3. `commerce/concert-ticket-flash-sale/src/main/kotlin/io/bluetape4k/workshop/commerce/ticket/purchase/internal/PurchaseService.kt`
4. `commerce/concert-ticket-flash-sale/src/main/kotlin/io/bluetape4k/workshop/commerce/ticket/purchase/internal/RefundService.kt`
5. `commerce/event-sourced-promotion-voucher-campaign/src/main/kotlin/io/bluetape4k/workshop/commerce/voucher/eventsourced/domain/EventContract.kt`
6. `commerce/event-sourced-promotion-voucher-campaign/src/main/kotlin/io/bluetape4k/workshop/commerce/voucher/eventsourced/idempotency/EventSourcedIdempotency.kt`
7. `commerce/event-sourced-promotion-voucher-campaign/src/main/kotlin/io/bluetape4k/workshop/commerce/voucher/eventsourced/snapshot/EventSnapshots.kt`
8. `commerce/order-lifecycle-fulfillment/src/main/kotlin/io/bluetape4k/workshop/commerce/order/idempotency/IdempotencyFingerprint.kt`
9. `commerce/pre-generated-voucher-pool/src/main/kotlin/io/bluetape4k/workshop/commerce/voucherpool/admission/VoucherPoolRedisSignals.kt`
10. `commerce/pre-generated-voucher-pool/src/main/kotlin/io/bluetape4k/workshop/commerce/voucherpool/application/CampaignBatchCommandService.kt`
11. `commerce/pre-generated-voucher-pool/src/main/kotlin/io/bluetape4k/workshop/commerce/voucherpool/application/VoucherPoolRevocationService.kt`
12. `commerce/pre-generated-voucher-pool/src/main/kotlin/io/bluetape4k/workshop/commerce/voucherpool/config/VoucherPoolKeyMaterialConfiguration.kt`
13. `commerce/pre-generated-voucher-pool/src/main/kotlin/io/bluetape4k/workshop/commerce/voucherpool/config/VoucherPoolMigrationRunner.kt`
14. `commerce/pre-generated-voucher-pool/src/main/kotlin/io/bluetape4k/workshop/commerce/voucherpool/idempotency/VoucherPoolFingerprint.kt`
15. `commerce/pre-generated-voucher-pool/src/main/kotlin/io/bluetape4k/workshop/commerce/voucherpool/idempotency/VoucherPoolIdempotencyRepository.kt`
16. `commerce/pre-generated-voucher-pool/src/main/kotlin/io/bluetape4k/workshop/commerce/voucherpool/web/OperatorAccessFilter.kt`
17. `commerce/promotion-voucher-campaign/src/main/kotlin/io/bluetape4k/workshop/commerce/voucher/application/AllocationService.kt`
18. `commerce/promotion-voucher-campaign/src/main/kotlin/io/bluetape4k/workshop/commerce/voucher/config/VoucherMigrationRunner.kt`
19. `commerce/promotion-voucher-campaign/src/main/kotlin/io/bluetape4k/workshop/commerce/voucher/idempotency/IdempotencyFingerprint.kt`
20. `commerce/promotion-voucher-campaign/src/main/kotlin/io/bluetape4k/workshop/commerce/voucher/persistence/EventInboxRepository.kt`
21. `commerce/promotion-voucher-campaign/src/main/kotlin/io/bluetape4k/workshop/commerce/voucher/security/VoucherCodeService.kt`
22. `commerce/usage-metering-billing-event-sourcing/src/main/kotlin/io/bluetape4k/workshop/commerce/metering/eventsourcing/application/BillingCommandServices.kt`
23. `commerce/usage-metering-billing-event-sourcing/src/main/kotlin/io/bluetape4k/workshop/commerce/metering/eventsourcing/application/CorrectionReconciliationServices.kt`
24. `commerce/usage-metering-billing-event-sourcing/src/main/kotlin/io/bluetape4k/workshop/commerce/metering/eventsourcing/eventstore/CanonicalEventHash.kt`
25. `commerce/usage-metering-billing-event-sourcing/src/main/kotlin/io/bluetape4k/workshop/commerce/metering/eventsourcing/idempotency/CommandFingerprint.kt`
26. `commerce/usage-metering-billing-event-sourcing/src/main/kotlin/io/bluetape4k/workshop/commerce/metering/eventsourcing/worker/ProjectionWorker.kt`
27. `commerce/usage-metering-billing-ledger/src/main/kotlin/io/bluetape4k/workshop/commerce/metering/idempotency/CommandFingerprint.kt`
28. `commerce/shared/src/main/kotlin/io/bluetape4k/workshop/commerce/shared/integration/CommerceIntegrationContracts.kt`
29. `commerce/reservation-control-plane/src/main/kotlin/io/bluetape4k/workshop/commerce/reservation/idempotency/IdempotencyFingerprint.kt`

## Traceability

| Spec requirement | Plan task | Evidence |
|---|---|---|
| `Digesters.SHA256`, Java fields/overloads, four Base64 formats | 2–3 | Kotlin tests and Java contract fixture |
| 세션 분리·초기화/재사용·동시 격리·예외 의미 | 2–3 | session 및 실패 경로 테스트 |
| `TinkDigester` source/JVM/output compatibility | 1, 4 | pre/post `javap`, existing tests and golden output |
| dependency 방향 `io/tink -> io/io` | 3–4 | Gradle dependencyInsight와 `io/io -> io/tink` 역방향 없음 확인 |
| Korean KDoc and both README locales | 5 | docs read-back and terminology audit |
| benchmark 네 측정 경로와 Gradle task 유효성 | 5, 7 | `testBenchmarkCompile`, `testBenchmark`, 환경·원시 결과 기록 |
| 9개 모듈·29개 파일·35개 호출과 bytes/wire 값 보존 | 6–7 | 모듈 매핑, 순차 테스트, dependencyInsight |
| provider 선택, matcher 예외 경계, 배열 소유권, MAC/키 정리와 coroutine 취소 | 2–6 | provider/known vector 대조, malformed Base64 테스트, zeroization 및 callsite diff 증거 |
| rollback 및 외부 publish/CI 경계 | 1, 6, 8 | baseline manifest, rollback 결과, 미검증 외부 증거 구분 |
| 최종 리뷰와 향후 변경자 가드 | 8 | 독립 7-tier 리뷰와 한국어 lesson |

## Writer DoD

- `SPW-01`: 독자, 구현 목적, source spec/hash, issue, 정확한 API 이름, 실행 제한과 남은 external evidence 경계를 고정한다.
- `SPW-02`: 파일 지도를 먼저 제시하고 순서, TDD, 실패 경로, 검증 명령, rollback, dependency 및 전체 spec-to-task traceability를 작성한다.
- `SPW-03`: 사용자/프로젝트 plan의 한국어 문장을 읽고 API·명령·파일 경로·숫자·URL을 그대로 보존한다.
- `SPW-04`: 모든 spec acceptance와 29-file/35-call inventory, Gradle task명, 공개 descriptor, 비공개 side effect 경계를 다시 대조한다.
- `SPW-05`: 완성한 Markdown을 다시 읽고 표/목록/명령의 일관성과 빈 placeholder 부재를 확인한 뒤 plan review receipt에 기록한다.
