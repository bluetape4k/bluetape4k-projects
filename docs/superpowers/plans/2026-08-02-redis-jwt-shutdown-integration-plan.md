# Redis-backed JWT shutdown integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 실제 Redis/Redisson과 ToxiProxy를 통과하는 JWT provider shutdown, route recovery, borrowed-client ownership을 순차 integration test로 고정한다.

**Architecture:** JWT 모듈 테스트 classpath에 version catalog의 `testcontainers-toxiproxy` alias만 test scope로 연결한다. 새 테스트는 shared `Network` 안에서 Redis와 ToxiProxy를 명시적으로 시작하고, host-mapped proxy endpoint를 짧은 timeout의 Redisson client에 주입한다. `RedissonJwtProvider`는 delegate를 빌려 쓰고 `DefaultJwtProvider`가 rotation timer를 소유하므로, wrapper/delegate/repository/client의 실제 close 순서를 README 두 locale에 기록한다.

**Tech Stack:** Kotlin 2.3, JUnit 5, Kotest-style bluetape assertions, Testcontainers Redis/ToxiProxy, Redisson 4.6.x, Gradle version catalog.

---

## 변경 파일 구조

- Modify: `utils/jwt/build.gradle.kts` — ToxiProxy client를 test scope에만 추가한다.
- Create: `utils/jwt/src/test/kotlin/io/bluetape4k/jwt/provider/cache/RedisJwtShutdownIntegrationTest.kt` — Redis/ToxiProxy/Redisson/JWT lifecycle 및 interruption/recovery를 한 sequential integration test로 검증한다.
- Modify: `utils/jwt/README.md` — Redis-backed provider의 borrowed client ownership과 close 순서를 영어로 보강한다.
- Modify: `utils/jwt/README.ko.md` — 같은 내용을 한국어로 보강한다.
- Create: `docs/lessons/2026-08-02-issue-1295-redis-jwt-shutdown.md` — 재사용 가능한 Testcontainers/ownership 학습을 증거와 함께 기록한다.
- Existing: `docs/superpowers/specs/2026-08-02-redis-jwt-shutdown-integration-design.md` — 구현 중 확인된 delegate ownership을 반영한 설계.

Production Kotlin files, central catalog version definitions, and the shared `testing/testcontainers` API are intentionally unchanged unless a failing integration test proves a separate production defect.

## Task 1: test dependency와 RED 테스트 뼈대 추가

**Files:**
- Modify: `utils/jwt/build.gradle.kts`
- Create: `utils/jwt/src/test/kotlin/io/bluetape4k/jwt/provider/cache/RedisJwtShutdownIntegrationTest.kt`

- [ ] **Step 1: test dependency만 추가한다.**

`dependencies` 안의 Testcontainers test entries 뒤에 다음 한 줄만 추가한다.

```kotlin
testImplementation(libs.testcontainers.toxiproxy)
```

catalog alias는 이미 `gradle/libs.versions.toml`에 있으므로 version literal이나 새 repository를 추가하지 않는다.

- [ ] **Step 2: integration test의 첫 RED assertion을 작성한다.**

테스트는 `@Execution(SAME_THREAD)`를 사용하고, 각 테스트에서 새 `Network`, `RedisServer`, `ToxiproxyServer`를 직접 소유한다. 첫 테스트는 proxy를 통한 JWT parsing을 요구한다.

```kotlin
@Test
fun `proxied Redis supports JWT parsing and close ownership`() {
    Network.newNetwork().use { network ->
        RedisServer().withNetwork(network).withNetworkAliases("redis").use { redis ->
            ToxiproxyServer().apply { withNetwork(network) }.use { toxiproxy ->
                redis.start()
                toxiproxy.start()
                val proxyClient = ToxiproxyClient(toxiproxy.host, toxiproxy.controlPort)
                val proxy = proxyClient.createProxy("jwt-redis-${UUID.randomUUID()}", "0.0.0.0:8666", "redis:${RedisServer.PORT}")
                val address = "redis://${toxiproxy.host}:${toxiproxy.getMappedPort(8666)}"
                val redisson = Redisson.create(
                    RedisServer.Launcher.RedissonLib.getRedissonConfig(address).apply {
                        useSingleServer().apply {
                            timeout = 500
                            connectTimeout = 500
                            retryAttempts = 0
                        }
                    },
                )
                val repository = RedisKeyChainRepository(redisson, queueName = "test:jwt:shutdown")
                val delegate = DefaultJwtProvider.forTesting(
                    keyChainRepository = repository,
                    rotationIntervalMillis = 50,
                )
                val provider = RedissonJwtProvider(delegate, redisson)
                try {
                    val jwt = provider.compose { subject = "shutdown" }
                    provider.tryParse(jwt)?.subject shouldBeEqualTo "shutdown"
                    provider.close()
                    provider.close()
                    delegate.close()
                    delegate.close()
                    repository.close()
                    repository.close()
                    redisson.isShutdown.shouldBeFalse()
                } finally {
                    runCatching { proxy.delete() }
                    runCatching { provider.close() }
                    runCatching { delegate.close() }
                    runCatching { repository.close() }
                    runCatching { redisson.shutdown() }
                }
            }
        }
    }
}
```

- [ ] **Step 3: RED를 확인한다.**

Run:

```bash
./gradlew :bluetape4k-jwt:test --tests io.bluetape4k.jwt.provider.cache.RedisJwtShutdownIntegrationTest --no-build-cache
```

Expected: dependency/classpath 또는 아직 구현되지 않은 lifecycle assertion으로 실패한다. Docker 미가동으로 실패하면 Docker 상태를 먼저 진단하고, 테스트 코드 오류가 아닌 환경 blocker는 별도로 기록한다.

## Task 2: 실제 proxy interruption/recovery와 timer 정지 assertion을 완성한다

**Files:**
- Modify: `utils/jwt/src/test/kotlin/io/bluetape4k/jwt/provider/cache/RedisJwtShutdownIntegrationTest.kt`

- [ ] **Step 1: 정상 경로와 cache parsing을 명시한다.**

```kotlin
val firstJwt = provider.compose { subject = "shutdown" }
provider.tryParse(firstJwt)?.subject shouldBeEqualTo "shutdown"
provider.forcedRotate().shouldBeTrue()
val rotatedJwt = provider.compose { subject = "rotated" }
provider.tryParse(rotatedJwt)?.subject shouldBeEqualTo "rotated"
```

- [ ] **Step 2: disable 중 bounded failure, enable 후 recovery를 작성한다.**

```kotlin
proxy.disable()
val interruptedAt = System.nanoTime()
provider.forcedRotate().shouldBeFalse()
val interruptedMillis = Duration.ofNanos(System.nanoTime() - interruptedAt).toMillis()
assertTrue(interruptedMillis < 5_000, "proxy interruption must remain bounded: ${interruptedMillis}ms")
proxy.enable()
provider.forcedRotate().shouldBeTrue()
provider.tryParse(provider.compose { subject = "recovered" })?.subject shouldBeEqualTo "recovered"
```

`shouldBeLessThan`가 현재 assertions surface에 없으면 표준 `assertTrue`로 대체하고, 그 이유를 evidence에 기록한다. route가 복구된 뒤 같은 Redisson client가 정상 동작해야 한다.

- [ ] **Step 3: delegate-owned timer가 close 후 Redis를 다시 회전시키지 않는지 고정한다.**

wrapper provider와 delegate를 각각 두 번 닫은 뒤 repository로 `KeyChain(expiredTtl = Duration.ofMillis(1))`을 강제로 저장한다. `rotationIntervalMillis * 5` 이상 기다리고도 repository의 current key id가 그대로인지 확인한다. 이 assertion은 thread-name enumeration 없이 delegate timer 취소를 입증한다.

- [ ] **Step 4: 외부 client ownership과 terminal shutdown을 확인한다.**

provider/delegate/repository 반복 close 직후 `redisson.isShuttingDown`과 `redisson.isShutdown`이 false인지 확인하고, finally에서 application owner로 `redisson.shutdown()`을 호출한 뒤 `isShutdown == true`를 확인한다. proxy, container, network 정리는 각각 외부 owner가 수행한다.

- [ ] **Step 5: targeted RED→GREEN을 실행한다.**

동일한 Gradle 명령을 다시 실행하여 테스트가 PASS하는지 확인한다. 실패하면 raw output에서 Docker readiness, Redisson timeout, ToxiProxy routing, ownership ordering을 분리해 수정하고 처음부터 targeted test를 재실행한다.

## Task 3: 문서와 lesson을 실제 코드에 맞춰 작성한다

**Files:**
- Modify: `utils/jwt/README.md`
- Modify: `utils/jwt/README.ko.md`
- Create: `docs/lessons/2026-08-02-issue-1295-redis-jwt-shutdown.md`

- [ ] **Step 1: README 두 locale에 Redis close 순서를 추가한다.**

```kotlin
val repository = RedisKeyChainRepository(redissonClient)
val delegate = DefaultJwtProvider(keyChainRepository = repository)
val provider = RedissonJwtProvider(delegate, redissonClient)
try {
    provider.tryParse(jwt)
} finally {
    provider.close()    // idempotent wrapper close; borrowed resources stay open
    delegate.close()    // delegate-owned rotation work
    repository.close()  // repository-owned refresh work
    redissonClient.shutdown() // application-owned client
}
```

영문 README는 public API identifier를 유지하고, 한국어 README는 동일한 순서/ownership을 번역한다. code fence와 heading 수를 임의로 바꾸지 않는다.

- [ ] **Step 2: lesson을 작성한다.**

lesson에는 issue/PR 연결, local Docker prerequisite, shared network proxy 설정, `retryAttempts = 0`의 bounded-failure 이유, provider/repository/client ownership order, 실제 명령과 결과, hosted CI에서 Docker가 unavailable할 때 보고해야 할 보류 상태를 짧게 기록한다. filler 문장은 금지한다.

- [ ] **Step 3: docs parity와 diff-check를 실행한다.**

```bash
git diff --check
```

README 두 파일의 heading/fenced-code 수, lifecycle marker, local link를 기존 repo 방식으로 비교하고, 기술 앵커/버전/명령/링크가 변하지 않았는지 확인한다.

## Task 4: proportional verification과 pre-PR review

**Files:** all changed paths

- [ ] **Step 1: sequential targeted Testcontainers test를 재실행한다.**

```bash
./gradlew :bluetape4k-jwt:test --tests io.bluetape4k.jwt.provider.cache.RedisJwtShutdownIntegrationTest --no-build-cache
```

이 명령은 Docker-backed test이므로 다른 Testcontainers module/worktree와 동시에 실행하지 않는다.

- [ ] **Step 2: JWT module tests와 detekt를 실행한다.**

```bash
./gradlew :bluetape4k-jwt:test --no-build-cache
./gradlew :bluetape4k-jwt:detekt --no-build-cache
```

- [ ] **Step 3: dependency scope와 변경 범위를 확인한다.**

```bash
./gradlew :bluetape4k-jwt:dependencies --configuration testCompileClasspath --no-build-cache
git diff --check
git status --short
git diff --stat origin/develop...HEAD
```

`testcontainers-toxiproxy`가 test classpath에만 있고 production runtime/API 변경이 없는지 확인한다. untracked/generated output은 PR에 포함하지 않는다.

- [ ] **Step 4: issue acceptance와 exact diff를 대조한다.**

각 acceptance criterion을 test/docs/dependency evidence에 매핑하고, production ownership 변경이 없다는 것을 diff로 확인한다. P0/P1 finding은 없어야 하며, Docker unavailable 같은 환경 gap은 PR DoD의 `PENDING` 행으로 남긴다.

## Task 5: lesson commit, PR, live CI, merge-ready report

- [ ] **Step 1: lesson과 구현을 Lore commit protocol로 commit한다.**

commit message는 intent line을 먼저 쓰고 `Constraint`, `Rejected`, `Confidence`, `Scope-risk`, `Directive`, `Tested`, `Not-tested` trailers를 포함한다. spec/plan commit과 implementation commit을 구분한다.

- [ ] **Step 2: issue metadata와 PR body를 live 검증한다.**

PR title/body는 영어로 작성하고 issue #1295, milestone `1.12.0`, assignee `debop`, label `test`를 유지한다. body 마지막은 반드시 `## DoD Status`이며, exact head SHA, targeted/module/detekt/diff evidence, Docker CI N/A 사유, unchecked CG-16/17/18을 표시한다.

- [ ] **Step 3: CI와 review를 exact head에서 확인한다.**

`gh pr view`, `gh pr checks`, review/thread 상태, mergeability, branch head를 재조회한다. stale head, unresolved review, missing required check는 merge-ready가 아니다.

- [ ] **Step 4: merge gate에서 멈춘다.**

CG-16은 fresh explicit user approval이 있어야 하므로 merge-ready DoD를 보고하고 멈춘다. 승인 전에는 merge, branch deletion, worktree cleanup을 수행하지 않는다.

## Risk prediction

| Risk | Signal | Mitigation | Rerun point |
| --- | --- | --- | --- |
| Docker/ToxiProxy readiness flake | proxy creation 또는 Redisson warmup timeout | shared Network, explicit start order, short retry, finally cleanup | Task 2 Step 5 |
| disable failure가 너무 오래 걸림 | targeted test가 5초 threshold 초과 | `timeout = 500`, `retryAttempts = 0`, one command at a time | Task 2 Step 2 |
| borrowed client가 provider close로 종료됨 | close 직후 `isShutdown` true | provider/repository close와 client shutdown assertion 분리 | Task 2 Step 4 |
| timer가 close 후 Redis를 변경함 | expired key id가 interval 뒤 변경 | delegate close를 먼저 수행하고 Redis current id를 재검증 | Task 2 Step 3 |
| docs/production scope drift | changed path에 catalog/runtime API가 나타남 | exact diff/stat와 dependency configuration 확인 | Task 4 Step 3 |

## Rollback / rerun

- 실패한 테스트와 새 dependency를 feature branch에서 되돌리고, 공용 `testing/testcontainers` API를 확장하지 않는다.
- production contract 결함이 발견되면 이 계획의 범위를 확장하지 않고, 새 issue/설계 승인으로 분리한다.
- Docker가 없는 환경이면 code compile/static checks만 수행하고 integration acceptance는 `PENDING`으로 남긴다.
