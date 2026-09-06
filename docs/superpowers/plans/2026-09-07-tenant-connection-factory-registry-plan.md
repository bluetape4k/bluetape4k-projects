# tenant ConnectionFactory registry 구현 계획

> **For agentic workers:** 이 계획은 Type-A gate를 통과한 뒤 task-by-task로 실행한다. 각 단계는 체크박스로 추적하고, Kotlin pattern/TDD 규칙을 따른다.

**Goal:** `bluetape4k-r2dbc`에 불변 key registry와 명시적 borrowed/owned lifecycle을 추가한다.

**Architecture:** `R2dbcConnectionFactoryEntry`가 factory와 ownership/close action을 감싸고, `R2dbcConnectionFactoryRegistry<K>`가 immutable snapshot을 조회한다. registry는 R2DBC `Closeable`과 Reactor `Disposable` adapter만 제공하며 Spring/Ktor 타입은 참조하지 않는다.

**Tech Stack:** Kotlin 2.x, R2DBC SPI, Reactor Mono/Flux, JUnit 5, bluetape4k assertions, Gradle module `:bluetape4k-r2dbc`.

---

## 변경 파일 지도

- Create: `data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/pool/R2dbcConnectionFactoryRegistry.kt` — entry, ownership, registry public API.
- Create: `data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/pool/R2dbcConnectionFactoryRegistryTest.kt` — lookup, snapshot, routing, lifecycle, concurrency tests.
- Modify: `data/r2dbc/README.md` — English public API contract and example.
- Modify: `data/r2dbc/README.ko.md` — Korean equivalent contract and example.
- No Spring/Ktor, catalog, or dependency file changes are allowed in this provider PR.

### Task 1: RED 테스트로 public contract를 고정한다

**Files:**
- Create: `data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/pool/R2dbcConnectionFactoryRegistryTest.kt`

- [ ] **Step 1: fake factory/resource fixture 작성**

  `ConnectionFactory`의 `create()`는 `Mono.error(UnsupportedOperationException())`를 반환하고 `ConnectionFactoryMetadata`는 고정 이름을 반환한다. owned fixture는 R2DBC `Closeable`과 Reactor `Disposable`을 구현하고 close/dispose 호출 횟수와 지정 오류를 `AtomicInteger`/`AtomicReference`에 기록한다. dispose-first 오류 경로가 후속 `close()`에서 같은 cached error와 suppressed chain을 보존하는지도 고정한다.

- [ ] **Step 2: 다음 실패 테스트 작성**

  `borrowed registry는 lookup/keys/asMap snapshot을 제공한다`, `unknown key는 configured keys를 포함한 NoSuchElementException을 던진다`, `routingMap은 mapped key 충돌을 거부한다`, `owned registry는 같은 resource alias를 한 번만 닫는다`, `borrowed/owned alias 충돌은 생성 시 거부한다`, `borrowed registry는 close 후에도 resource를 닫지 않는다`, `close 후 lookup/map은 IllegalStateException을 던진다`, `close는 모든 오류를 시도하고 후속 오류를 suppressed로 보존한다`, `dispose-first 오류도 후속 close 관찰성을 유지한다`, `concurrent lookup은 A/B factory를 교차하지 않는다`, `동시 close는 cached operation으로 한 번만 실행된다`를 exact assertion으로 작성한다.

- [ ] **Step 3: RED 실행**

  Run: `./gradlew :bluetape4k-r2dbc:test --tests "io.bluetape4k.r2dbc.pool.R2dbcConnectionFactoryRegistryTest"`

  Expected: registry 타입/함수가 없어 compile failure.

### Task 2: entry와 registry의 최소 구현

**Files:**
- Create: `data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/pool/R2dbcConnectionFactoryRegistry.kt`

- [ ] **Step 1: ownership entry 구현**

  `ConnectionFactoryOwnership`는 `BORROWED`와 `OWNED`만 가진다. `borrowed(factory)`는 no-op close action을 만들고, 기본 `owned(factory)`는 R2DBC `Closeable`을 우선 사용하고 Reactor `Disposable`을 fallback으로 사용하며 둘 다 아니면 `IllegalArgumentException`을 던진다. 두 번째 `owned(factory, closeAction)`는 caller가 비동기 종료 동작을 명시한다. public entry는 factory/ownership만 노출하고 lifecycle callback과 identity는 private implementation/helper에 둔다. `javap -public`에 callback backdoor가 생기지 않는지 검증한다.

- [ ] **Step 2: immutable lookup 구현**

  생성 시 `LinkedHashMap`/unmodifiable wrapper로 snapshot을 만들고 `keys`/`asMap()`은 내부 변경이 외부에 노출되지 않는 읽기 전용 copy를 반환한다. close state가 설치되면 `get`, `asMap`, `routingMap`은 `IllegalStateException`으로 거부하고 `keys`만 유지한다. `operator fun get`은 열린 registry에서 누락 key를 `NoSuchElementException`으로 fail-fast한다. `routingMap`은 mapper 결과 중복을 `IllegalArgumentException`으로 거부하고 관찰된 snapshot iteration order를 보존한다.

- [ ] **Step 3: lifecycle 구현**

  owned entry를 resource identity로 deduplicate한다. 같은 identity의 alias는 ownership와 custom close-action identity가 같아야 하며, 혼합이면 생성 시 fail-fast한다. `close()`는 `AtomicReference<Mono<Void>>` compare-and-set으로 `cache()`된 operation을 한 번만 설치하고, 그 reference를 lifecycle state의 단일 source of truth로 사용한다. 각 close action을 순차 실행하면서 synchronous throw와 `Mono.error`를 모두 수집한 뒤 첫 오류에 나머지를 suppressed로 붙인다. `dispose()`는 같은 operation을 subscribe하는 fire-and-forget adapter이고 오류를 logger에 기록하며, `isDisposed()`는 close signal 설치 여부를 반영한다.

- [ ] **Step 4: companion convenience factory 구현**

  `R2dbcConnectionFactoryRegistry.borrowed(entries: Map<K, ConnectionFactory>)`와 `owned(entries: Map<K, ConnectionFactory>)`가 entry map을 생성하도록 한다. Kotlin `Map` 자체가 값 타입에 공변이므로 subtype factory map도 호출부에서 허용된다.

### Task 3: GREEN 및 동시성/ABI 경계 검증

- [ ] **Step 1: targeted test 실행**

  Run: `./gradlew :bluetape4k-r2dbc:test --tests "io.bluetape4k.r2dbc.pool.R2dbcConnectionFactoryRegistryTest"`

  Expected: 모든 registry 테스트 PASS.

- [ ] **Step 2: module compile/static checks**

  Run: `./gradlew :bluetape4k-r2dbc:compileKotlin :bluetape4k-r2dbc:compileTestKotlin :bluetape4k-r2dbc:detekt`

  Expected: compile/detekt PASS, 새 registry source에 Spring/Ktor import 없음. 기존 module baseline의 unrelated detekt finding은 별도로 기록한다.

- [ ] **Step 3: publication surface 확인**

  Run: `./gradlew :bluetape4k-r2dbc:jar :bluetape4k-r2dbc:generateMetadataFileForBluetape4kPublication :bluetape4k-r2dbc:generatePomFileForBluetape4kPublication :bluetape4k-r2dbc:checkPomFileForBluetape4kPublication`

  Expected: JAR/POM/module metadata 생성과 POM check 성공; 생성된 POM은 provider baseline과 비교해 새 framework dependency를 추가하지 않는다.

- [ ] **Step 4: bytecode/dependency guard**

  Run:

  ```bash
  set -euo pipefail
  guard_dir="$(mktemp -d)"
  guard_jar="$guard_dir/bluetape4k-r2dbc-registry-guard.jar"
  trap 'rm -rf "$guard_dir"' EXIT
  test -f data/r2dbc/build/classes/kotlin/main/io/bluetape4k/r2dbc/pool/R2dbcConnectionFactoryRegistry.class
  jar --create --file "$guard_jar" \
    -C data/r2dbc/build/classes/kotlin/main io/bluetape4k/r2dbc/pool/R2dbcConnectionFactoryRegistry.class \
    -C data/r2dbc/build/classes/kotlin/main io/bluetape4k/r2dbc/pool/R2dbcConnectionFactoryEntry.class \
    -C data/r2dbc/build/classes/kotlin/main io/bluetape4k/r2dbc/pool/ConnectionFactoryOwnership.class
  jar tf "$guard_jar" | rg 'R2dbcConnectionFactoryRegistry|R2dbcConnectionFactoryEntry'
  jdeps_output="$guard_dir/jdeps.txt"
  jdeps --multi-release 21 --recursive --ignore-missing-deps "$guard_jar" > "$jdeps_output"
  if rg -n 'org.springframework|io.ktor' "$jdeps_output"; then exit 1; fi
  ```

  Expected: public classes are in the JAR and no Spring/Ktor bytecode reference exists. `Mono`/Reactor linkage is checked through the generated POM and the workshop compile gate, not by adding a framework dependency to this PR.

- [ ] **Step 5: public signature snapshot**

  Run: `set -euo pipefail; artifact_jar="$(find data/r2dbc/build/libs -maxdepth 1 -type f -name 'bluetape4k-r2dbc-*.jar' ! -name '*-sources.jar' | head -n 1)"; test -n "$artifact_jar"; mkdir -p build; javap -classpath "$artifact_jar" -public io.bluetape4k.r2dbc.pool.R2dbcConnectionFactoryRegistry io.bluetape4k.r2dbc.pool.R2dbcConnectionFactoryEntry > build/registry-public-api.txt; if rg -n 'closeResource\\$|lifecycleIdentity\\$|synthetic' build/registry-public-api.txt; then exit 1; fi` and inspect that the documented methods (`get`, `asMap`, `routingMap`, `close`, `dispose`, `isDisposed`) are present. If the repository ABI plugin becomes available, run its canonical task in addition; otherwise retain this exact signature output as the additive ABI evidence.

### Task 4: README locale parity 문서화

**Files:**
- Modify: `data/r2dbc/README.md`
- Modify: `data/r2dbc/README.ko.md`

- [ ] **Step 1: 동일 예제 추가**

  두 README에 `R2dbcConnectionFactoryRegistry`의 borrowed/owned 생성, `registry[key]`, `routingMap`, `close()`/`dispose()` 차이와 unknown-key fail-fast를 같은 구조로 추가한다. 코드·URL·명령은 그대로 두고 설명 언어만 locale에 맞춘다.

- [ ] **Step 2: 문서/공백 검증**

  Run: `git diff --check` 및 README code fence/locale diff를 확인한다.

### Task 5: verifier 및 rollback

- [ ] **Step 1: 전체 provider 검증**

  Run: `./gradlew :bluetape4k-r2dbc:test :bluetape4k-r2dbc:koverVerify`

  Expected: targeted 및 모듈 회귀 테스트 PASS. Docker-backed DB가 필요한 기존 테스트가 실패하면 환경과 코드 원인을 분리 진단하고 재실행하며, 실패가 남아 있는 동안 provider 완료 상태를 `PENDING`으로 유지한다. registry 테스트만 성공한 상태로 전체 provider를 완료로 선언하지 않는다.

- [ ] **Step 2: diff/ownership 확인**

  Run: `git diff --check`, `git status --short`, `git diff --stat`, `git diff --name-only origin/develop...HEAD`

  Expected: 위 지도에 있는 provider 파일만 변경되고, downstream/catalog 변경은 없다.

- [ ] **Step 3: rollback 지점**

  provider contract가 publication/CI에서 실패하면 dirty worktree와 현재 HEAD를 먼저 기록한 뒤, 정확한 implementation commit SHA만 `git revert <sha>`로 되돌린다. 이미 게시된 artifact는 git revert로 회수되지 않으므로 snapshot/catalog 소비를 중단하고 immutable 이전 version/SHA로 되돌린다. downstream migration은 차단하고 상태를 `PENDING`으로 남긴다. public API를 바꾸면 spec/plan과 테스트를 먼저 갱신하고 review gate를 다시 연다.

- [ ] **Step 4: downstream security hold 기록**

  provider PR에서는 consumer를 수정하지 않지만, 후속 #233 plan은 인증 tenant와 요청 tenant 불일치 및 미인증 요청이 `registry.get` 전에 401/403으로 끝나는 negative integration test를 포함해야 한다. 이 증거가 없으면 provider publication 이후에도 consumer gate를 열지 않는다.

## 수용 기준 추적

| Spec 기준 | 검증 task |
| --- | --- |
| lookup/keys/snapshot/routing/unknown key | Task 1, 2, 3 |
| borrowed/owned/custom lifecycle | Task 1, 2 |
| idempotency/dedup/concurrent close/suppressed | Task 1, 2, 3 |
| A/B concurrent isolation | Task 1, 3 |
| 새 registry bytecode에 Spring/Ktor 없음, baseline 대비 새 framework dependency 없음 | Task 3 |
| README 영/국문 parity | Task 4 |

## 위험 예측

- Reactor `Mono`를 public API로 노출하므로 `close()`가 cold/cached 동작인지 테스트에서 subscription까지 확인한다.
- `Disposable.dispose()`는 오류를 동기 throw하지 않는 fire-and-forget adapter이므로 logger에 기록하고, 상세 원인/suppressed chain 관찰 계약은 cached `close()`에 둔다.
- `ConnectionFactory` alias가 identity dedup되지 않으면 pool double-close가 발생하므로 동일 인스턴스 fixture를 반드시 사용한다.
- `Map.toMap()`이 caller 변경을 노출하지 않는지 원본 map mutate 후 snapshot을 확인한다.
