# NearJCache shared back cache destructive clear authority Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 공유될 수 있는 JCache back namespace의 destructive operation을 기본적으로 거부하고, 독점 owner가 명시한 runtime authority에서만 `clear()` 계열을 허용한다. 기존 key-scoped remove, read/write 경로, listener generation barrier, public ABI와 serialization 계약은 보존한다.

**Architecture:** `NearJCacheClearAuthority`를 `NearJCacheConfig`와 분리된 runtime-only enum으로 추가한다. 기존 constructor와 provider factory는 `DENY`로 위임하고, 새 explicit overload만 `EXCLUSIVE_BACK_CACHE`를 전달한다. 세 namespace-wide operation은 첫 상태 변경보다 앞에서 공통 guard를 통과해야 한다. management snapshot과 MXBean에는 stable token만 노출하며 cache name, key, value, provider payload는 노출하지 않는다.

**Tech Stack:** Kotlin 2.4, Java 25, JCache (`javax.cache`), JMX (`javax.management`), Caffeine, Lettuce, Hazelcast, Redisson, JUnit 5, MockK, Kluent, Gradle 9.7, Detekt, Testcontainers

**Source of truth:**

- Issue [#1368](https://github.com/bluetape4k/bluetape4k-projects/issues/1368)
- Epic [#1408](https://github.com/bluetape4k/bluetape4k-projects/issues/1408)
- Approved design: `docs/superpowers/specs/2026-08-16-epic-1408-nearjcache-safety-tail-design.md`
- Predecessor contract: [#1363](https://github.com/bluetape4k/bluetape4k-projects/issues/1363)
- Compound-operation boundary: [#1355](https://github.com/bluetape4k/bluetape4k-projects/issues/1355)
- Stacked train predecessor: [#1369](https://github.com/bluetape4k/bluetape4k-projects/issues/1369)

**Stack:** `develop` → `fix/1369-nearcache-bounded-bulk` → `fix/1368-nearcache-clear-authority`

**현재 source ledger (PR1 head 기준):**

| 주장 | 현재 근거 |
| --- | --- |
| public front/back constructor와 immutable configuration snapshot | `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCache.kt:95-124` |
| destructive `clear()`가 listener detach와 epoch/generation 변경 뒤 mutation을 수행 | `NearJCache.kt:345-379` |
| public back reference가 wrapper guard와 별도 caller-owned 경계를 만듦 | `NearJCache.kt:95-100` |
| `clearAllCache()`가 `clear()` alias임 | `NearJCache.kt:425-445` |
| no-arg `removeAll()`가 front와 back namespace를 순회 삭제함 | `NearJCache.kt:922-934` |
| key-scoped `removeAll(keys)`는 별도 경로임 | `NearJCache.kt:936-952` |
| config에는 현재 bulk policy만 있고 authority가 없음 | `NearJCacheConfig.kt:45-146`, `NearJCacheConfigurationSnapshot.kt:16-28` |
| management stable attributes의 현재 shape | `NearJCacheConfigurationMXBean.kt:11-23` |
| Hazelcast factory는 listener-free `NearJCache(front, back, config)`를 사용함 | `cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/HazelcastCaches.kt:261-303`, `cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/HazelcastNearJCache.kt:52-62` |

---

## 실행 경계와 완료 조건

- [ ] PR2는 #1369의 exact head 위에서만 구현한다. #1369가 merge되면 PR2 base를 `develop`으로 재설정하고 동일한 검증을 다시 수행한다.
- [ ] 사용자의 현재 실행 승인(`좋아 진행해`)은 이 plan의 계획·RED slice와 승인 설계의 구현 시작을 허용한다. API/serialization/non-goal을 바꾸는 material finding이 나오면 구현을 멈추고 새 승인을 받는다.
- [ ] `NearJCacheConfig`의 Serializable shape, existing constructor/copy/synthetic descriptor, serialVersionUID와 legacy stream 동작을 변경하지 않는다.
- [ ] `SuspendNearJCache`, compound operation atomicity, provider dependency, tenant protocol, `close()`의 back/provider lifecycle ownership은 변경하지 않는다.
- [ ] 권한 guard는 `NearJCache` wrapper를 통해 호출되는 `clear()`, `clearAllCache()`, no-arg `removeAll()`에 한정한다. 기존 public `backCache` reference에서 호출하는 `nearCache.backCache.clear()`는 provider 직접 호출이라는 별도 caller-owned 경계이며, ABI를 깨지 않고 library-level ACL로 가로채지 않는다. 문서에는 이 escape hatch를 untrusted caller에 노출하지 말아야 한다고 명시한다.
- [ ] `ResilientNearJCache`와 `ResilientSuspendNearJCache`의 자체 `clearAll()`/`ClearBack` command는 별도 wrapper 계약으로 inventory하고 이번 PR2의 non-goal로 고정한다. 이 경로까지 shared-back authority를 주장하지 않으며, 후속 issue 후보를 lesson에 기록한다.
- [ ] 기존 3-argument `NearJCache(front, back, config)`와 `NearJCache(config, backCache)`, 기존 Lettuce/Hazelcast/Redisson factory signature는 유지하고 기본 `DENY`로 동작한다.
- [ ] 새 explicit authority overload는 runtime-only 값을 받아 immutable instance state에 저장한다. factory가 cache를 생성하거나 `getOrCreate`했다는 이유로 owner를 추론하지 않는다.
- [ ] `clear()`, `clearAllCache()`, no-argument `removeAll()`은 공통 guard를 사용한다. `removeAll(keys)`와 single-key `remove`는 기존 계약으로 남긴다.
- [ ] `DENY`는 `compoundGate`, listener detach, epoch/generation 증가, front/back 호출 이전에 `SecurityException`을 던진다.
- [ ] `EXCLUSIVE_BACK_CACHE`는 기존 front/back clear 순서와 listener generation barrier, primary/suppressed failure 복구를 그대로 사용한다.
- [ ] 문서와 KDoc은 한국어 reader-facing prose로 작성하고, README 영어/한글 쌍과 manual 영어/한글 쌍의 의미를 일치시킨다.
- [ ] core 테스트와 Detekt를 먼저 통과시킨 뒤 Lettuce → Hazelcast → Redisson Testcontainers 검증을 순차 실행한다.
- [ ] 구현 완료 후 독립 perspective review, lesson, `git diff --check`, exact head/CI/review read-back을 남긴다. PR 생성과 merge는 별도 승인 이후에만 수행한다.

## 대상 파일과 책임

| 구분 | 파일 | 책임 |
| --- | --- | --- |
| 공개 runtime API | `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheClearAuthority.kt` | `DENY`, `EXCLUSIVE_BACK_CACHE` enum과 authority 의미/KDoc |
| core 구현 | `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCache.kt` | constructor/companion overload, immutable authority, 공통 guard, destructive operation 연결 |
| management | `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheConfigurationSnapshot.kt` | stable clear-authority token snapshot |
| management | `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheConfigurationMXBean.kt` | `getClearAuthority(): String` 공개 계약 |
| management | `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheManagementMXBean.kt` 및 adapter/registration 관련 파일 | snapshot 값의 실제 MBean read-back |
| core RED/회귀 | `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheClearAuthorityContractTest.kt` | guard, pre-mutation 불변성, owner/비-owner destructive 계약 |
| core 기존 fixture | `cache/cache-core/src/testFixtures/kotlin/io/bluetape4k/cache/nearcache/jcache/AbstractNearJCacheTest.kt` 및 영향받는 core tests | cleanup 의도에는 explicit `EXCLUSIVE_BACK_CACHE`를 주입하고 shared/default 경로는 `DENY`를 고정 |
| core management tests | `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/management/*` | snapshot/MXBean token, 실제 MBeanServer read-back |
| Lettuce | `cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/LettuceCaches.kt` 및 near-cache 계약 테스트 | 기존 default와 explicit authority overload |
| Hazelcast | `cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/HazelcastCaches.kt`, `cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/HazelcastNearJCache.kt` 및 테스트 | listener-free factory, default/explicit authority, peer-data 보존 |
| Redisson | `cache/cache-redisson/src/main/kotlin/io/bluetape4k/cache/RedissonCaches.kt` 및 계약 테스트 | 기존 overload 보존과 explicit authority 전달 |
| reader docs | `cache/cache-core/README.md`, `cache/cache-core/README.ko.md`, `docs/cache/near-cache-capability-matrix.md` | default DENY, exclusive owner migration, tenant boundary |
| provider docs | `cache/cache-lettuce/README.md`, `cache/cache-lettuce/README.ko.md`, `cache/cache-hazelcast/README.md`, `cache/cache-hazelcast/README.ko.md`, `cache/cache-redisson/README.md`, `cache/cache-redisson/README.ko.md` | 실제 factory 예제의 default DENY와 explicit owner migration; Redisson에 blocking `NearJCache` 예제가 없으면 inventory에서 N/A 근거를 기록 |
| manuals | `docs/manual/en/modules/bluetape4k-cache-core/near-cache-semantics.md`, `docs/manual/ko/modules/bluetape4k-cache-core/near-cache-semantics.md`, `docs/manual/en/modules/bluetape4k-cache-hazelcast/jcache-near-cache-serialization.md`, `docs/manual/ko/modules/bluetape4k-cache-hazelcast/jcache-near-cache-serialization.md` | factory/config examples, clear 범위, close ownership |
| release note | `CHANGELOG.md` | behavior migration과 explicit authority 안내 |
| lesson | `docs/lessons/2026-08-17-issue-1368-nearcache-clear-authority.md` | 결과, 검증, 실패/놀람, 후속 경계 기록 |

symbol search에서 추가 public factory 또는 destructive clear 예제가 발견되면 같은 PR에서 EN/KO parity를 맞춘다. 새 module/dependency 또는 tenant authorization protocol이 필요해지면 이 계획의 범위를 중단하고 별도 설계로 분리한다.

## Task 1 — baseline, 영향도와 frozen contract inventory

**Depends on:** 승인된 design spec과 PR1 exact head 확인

**Files:** read-only source, tests, provider factories, docs, live Issue/PR metadata

- [ ] 현재 worktree와 canonical `develop`의 base/head를 확인하고 PR1의 exact head, CI, review blocker, mergeability를 live-read한다.
- [ ] `rg -n "NearJCache\\(|clearAllCache|removeAll\\(|fun clear\\(" cache/cache-core cache/cache-lettuce cache/cache-hazelcast cache/cache-redisson`으로 public constructor/factory와 destructive call site를 inventory한다.
- [ ] `NearJCacheConfig`의 fields, constructors, `copy`/`copy$default`, `readObject`, serialVersionUID를 기록해 authority가 config에 들어가지 않음을 확인한다.
- [ ] `clear()`의 현재 `compoundGate`·listener detach·mutation epoch·back write generation·front/back clear·listener restore 순서를 읽고 테스트의 observable boundary를 고정한다.
- [ ] public `backCache` 직접 호출과 `ResilientNearJCache.kt`/`ResilientSuspendNearJCache.kt`의 `clearAll()`/`ClearBack` 경로를 별도로 inventory한다. 이 PR2의 wrapper-only authority 주장과 후속 issue 후보를 혼동하지 않도록 source ledger에 기록한다.
- [ ] `AbstractNearJCacheTest` setup/teardown과 management tests의 cleanup call은 explicit owner가 필요하다는 migration 목록에 넣는다.
- [ ] `docs/superpowers/specs/2026-08-16-epic-1408-nearjcache-safety-tail-design.md`의 PR2 API, failure mode, non-goal과 차이가 없음을 표로 기록한다.

**Evidence:** inventory 명령 output, exact base/head read-back, design line references, 수정 없는 `git diff --check`.

## Task 2 — RED 테스트: authority guard와 public contract

**Depends on:** Task 1

**Pattern skills:** `$test-driven-development`, `$bluetape-kotlin-patterns`, `bluetape-kotlin-patterns/references/testing.md`

**Create:** `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheClearAuthorityContractTest.kt`

- [ ] 새 테스트에 minimal fake front/back/listener harness를 만들고, private implementation detail에 의존하지 않고 contents, listener registration, epoch/generation test hook 또는 package-visible probe로 pre-mutation 불변성을 관찰한다.
- [ ] 기존 3-argument constructor와 `NearJCache(config, backCache)`가 authority를 지정하지 않으면 `DENY`임을 고정한다.
- [ ] `DENY`에서 `clear()`, `clearAllCache()`, no-arg `removeAll()` 각각이 operation token을 포함한 `SecurityException`을 던지는지 검증한다. message에 cache name, key, value, provider payload가 포함되지 않는지도 검증한다.
- [ ] 거부 전후 front contents, back contents, listener registration, mutation epoch, back write generation이 동일하고 front/back clear/remove 호출이 0회임을 검증한다.
- [ ] `DENY` guard가 compound lock 또는 listener detach보다 먼저 평가되도록 reentrant/blocking fake로 순서를 고정한다.
- [ ] explicit `EXCLUSIVE_BACK_CACHE` constructor에서 `clear()`가 front와 back을 모두 지우고 listener generation barrier를 유지하는지 검증한다.
- [ ] explicit owner에서 `clearAllCache()`와 no-arg `removeAll()`도 각각 front/back namespace를 지우고 동일한 listener/epoch/generation barrier를 사용하는지 검증한다.
- [ ] owner clear의 back failure에서 primary throwable과 listener restore/cleanup failure의 suppressed throwable 순서가 기존 계약과 동일함을 검증한다.
- [ ] owner `clearAllCache()`와 owner no-arg `removeAll()`의 back failure/restore failure도 operation별로 primary/suppressed 계약을 유지하는지 검증한다.
- [ ] owner clear에서 listener re-registration failure가 원래 clear failure를 가리지 않는지 검증한다.
- [ ] `removeAll(keys)`와 single-key `remove`는 `DENY`에서도 기존 범위 안에서 성공하고 다른 key/peer data를 삭제하지 않는지 검증한다.
- [ ] management snapshot/MXBean read-back이 `DENY`와 `EXCLUSIVE_BACK_CACHE`를 각각 stable token으로 반환하도록 RED test를 추가한다.
- [ ] RED 확인 command를 실행한다:

```bash
./gradlew :bluetape4k-cache-core:test \
  --tests "io.bluetape4k.cache.nearcache.jcache.NearJCacheClearAuthorityContractTest" \
  --no-configuration-cache
```

**Expected RED:** enum/constructor/guard/MXBean API가 아직 없으므로 컴파일 실패 또는 assertion failure가 발생한다. 실패 output을 저장하고 production implementation 전까지 보존한다.

### Task 2A — provider factory RED

**Depends on:** Task 2 core RED

**Files:** 기존 provider 계약 테스트에 추가할 default/explicit authority cases

- [ ] Lettuce `LettuceNearJJCacheTest`, Hazelcast `HazelcastNearJCacheTest`, Redisson `RedissonNearJCacheTest`에 기존 factory default `DENY`, explicit owner success, shared peer-data preservation RED cases를 추가한다.
- [ ] 각 provider에서 `clear()`, `clearAllCache()`, no-arg `removeAll()` 세 operation을 default/explicit owner 양쪽에 고정한다. Testcontainers가 필요한 peer-data assertion은 production API가 컴파일된 뒤 실행한다.
- [ ] production provider overload를 아직 작성하지 않은 상태에서 compile RED를 확인한다:

```bash
./gradlew :bluetape4k-cache-lettuce:compileTestKotlin \
  :bluetape4k-cache-hazelcast:compileTestKotlin \
  :bluetape4k-cache-redisson:compileTestKotlin \
  --no-configuration-cache
```

**Expected RED:** `NearJCacheClearAuthority` import와 explicit factory overload가 없어 provider test compile이 실패한다. 이 실패 output을 Task 3/5 production edit 전에 보존한다.

## Task 3 — core runtime authority 구현

**Depends on:** Task 2 and Task 2A RED

**Files:** enum, `NearJCache.kt`, affected core constructors and test fixture call sites

- [ ] `NearJCacheClearAuthority`를 runtime-only enum으로 추가하고, serialized config/property/stream에 들어가지 않는다는 KDoc을 쓴다.
- [ ] `NearJCache` public constructor `(frontCache, backCache, config, clearAuthority)`를 추가하고 기존 3-argument constructor는 `DENY`로 위임한다.
- [ ] companion factory `NearJCache(config, backCache, clearAuthority)`를 추가하고 기존 factory는 `DENY`로 위임한다. 기존 listener registration semantics는 owner/default behavior가 정해진 경로에 맞춰 보존한다.
- [ ] authority를 immutable property와 configuration snapshot 입력으로 보존한다. factory가 만든 back cache의 생성 여부나 provider lifecycle로 authority를 자동 승격하지 않는다.
- [ ] 공통 private/package-visible guard를 구현해 `clear()`, `clearAllCache()`, no-arg `removeAll()`의 첫 줄 경계에서 호출한다. `DENY`는 compoundGate, listener detach, epoch/generation, front/back mutation보다 먼저 실패한다.
- [ ] `EXCLUSIVE_BACK_CACHE`에서는 기존 clear sequence와 listener generation barrier를 재사용한다. 실패 시 primary/suppressed throwable 및 listener restoration 계약을 바꾸지 않는다.
- [ ] `clearAllCache()`와 no-arg `removeAll()`도 동일 guard를 통과한 뒤 기존 back mutation/error path를 실행하도록 하고, 세 operation 각각의 owner success/failure 테스트를 green으로 만든다.
- [ ] key-scoped `removeAll(keys)`와 single-key `remove`에는 authority guard를 추가하지 않는다.
- [ ] fixture setup/teardown과 기존 destructive cleanup tests에는 explicit `EXCLUSIVE_BACK_CACHE`를 주입하되, default-deny/provider/shared-back contract tests는 별도 factory helper로 `DENY`를 생성한다. 하나의 fixture가 owner cleanup으로 default 거부 assertion을 가리지 않도록 test names와 helper ownership을 분리한다.
- [ ] Task 2 RED test를 다시 실행해 green으로 전환한다.

**Implementation checks:** `rg`로 모든 namespace-wide call path가 guard를 거치는지 확인하고, `git diff --check`를 실행한다.

## Task 4 — stable management metadata

**Depends on:** Task 3 green core contract

- [ ] `NearJCacheConfigurationSnapshot`에 `clearAuthority` stable token을 추가하되 owner identity, credential, cache payload를 포함하지 않는다.
- [ ] `NearJCacheConfigurationMXBean`에 `getClearAuthority(): String`를 `-jvm-default=enable` default method로 추가해 기존 외부 implementor의 binary/source compatibility를 보존한다. 기본 method는 `DENY`를 반환하고 NearJCache management implementation/adapter는 snapshot 값을 override해 반환한다.
- [ ] management registration이 실제 `MBeanServer`에서 `DENY`/`EXCLUSIVE_BACK_CACHE`를 read-back하는 테스트를 추가한다.
- [ ] 기존 logical/tier statistics와 management registration lifecycle이 변하지 않는지 기존 tests를 수정 없이 또는 최소 변경으로 유지한다.
- [ ] MXBean method signature와 descriptor를 `javap`/reflection으로 확인하고, 새 method를 구현하지 않는 precompiled-style Java implementor fixture가 current interface에서 로드되는지 검증한다.

**Evidence:** management unit test, real MBeanServer read-back, descriptor output, diff check.

## Task 5 — provider factory overload와 shared-back 계약

**Depends on:** Task 3, Task 4

- [ ] Lettuce `nearJCache` DSL/config factory의 기존 signature를 `DENY`로 유지하고, authority를 필수로 받는 overload를 추가한다. DSL overload는 `(redisClient, clearAuthority, codec, block)`, config overload는 `(redisClient, config, clearAuthority, codec)` 순서로 고정해 기존 `codec` positional call과 충돌하지 않게 한다. KDoc에 factory creation이 ownership proof가 아님을 쓴다.
- [ ] Hazelcast `HazelcastCaches.nearJCache`와 `HazelcastNearJCache` public factory에 기존 listener-free 경로를 유지한 explicit authority overload를 추가한다. DSL은 `(hazelcastInstance, clearAuthority, block)`, config는 `(hazelcastInstance, config, clearAuthority)`, public object factory는 기존 required args 뒤에 필수 authority를 둔다. supplied front close ownership과 back/provider non-ownership KDoc을 유지한다.
- [ ] Redisson direct-back/client factory에도 기존 default와 explicit authority overload를 추가한다. direct-back은 `(backCache, nearJCacheConfig, clearAuthority)`, client factory는 authority를 required enum slot으로 두어 기존 optional `Configuration`/`NearJCacheConfig` positional call을 보존한다.
- [ ] 각 provider contract test에서 default factory의 `clear()`/`clearAllCache()`/no-arg `removeAll()` 거부와 explicit owner clear를 검증한다.
- [ ] shared back fixture에 peer cache data를 채우고 non-owner/default 호출 후 peer data가 보존되는지 검증한다.
- [ ] Testcontainers-backed provider tests는 Lettuce → Hazelcast → Redisson 순서로 하나씩 실행한다. Docker host/Ryuk 설정은 현재 repository/user configuration을 그대로 사용하고 테스트 간 daemon overlap을 만들지 않는다.
- [ ] public JVM signature를 `javap` 또는 Kotlin reflection으로 확인해 기존 overload가 사라지지 않았음을 증명한다.
- [ ] 각 provider의 기존 positional call, default `DENY` call, explicit owner call을 작은 compile-only fixture로 각각 컴파일해 overload ambiguity와 default authority 누락을 조기에 잡는다.

**Provider test commands:**

```bash
./gradlew :bluetape4k-cache-lettuce:test \
  --tests "io.bluetape4k.cache.nearcache.jcache.LettuceNearJJCacheTest" \
  --tests "io.bluetape4k.cache.nearcache.jcache.LettuceNearJCacheWriteThroughReentrancyTest" \
  --no-configuration-cache
./gradlew :bluetape4k-cache-hazelcast:test \
  --tests "io.bluetape4k.cache.nearcache.jcache.HazelcastNearJCacheTest" \
  --no-configuration-cache
./gradlew :bluetape4k-cache-redisson:test \
  --tests "io.bluetape4k.cache.nearcache.jcache.RedissonNearJCacheTest" \
  --no-configuration-cache
```

## Task 6 — 문서, KDoc, migration과 release-facing note

**Depends on:** Task 4 API/token stable, Task 5 factory signatures stable

- [ ] core README 영어/한글에 default `DENY`, explicit exclusive owner example, key-scoped remove와 namespace clear의 차이를 추가한다.
- [ ] Lettuce/Hazelcast/Redisson provider README 영어/한글의 실제 `NearJCache` factory surface를 갱신한다. blocking factory 예제가 없는 locale/provider는 검색 결과와 N/A 사유를 evidence로 남기고, 존재하는 예제에는 default `DENY`, explicit authority overload, `close()` ownership을 반영한다.
- [ ] capability matrix에 destructive operation authority, default, owner path, shared-back restriction, `close()` non-deletion을 기록한다.
- [ ] core manual 영어/한글에 constructor/factory migration, SecurityException, tenant는 namespace/key list로 분리해야 한다는 경계를 기록한다.
- [ ] 문서에는 `nearCache.backCache.clear()` 같은 provider 직접 호출이 이 wrapper guard의 대상이 아니며, `backCache` reference를 권한 없는 코드에 전달하면 안 된다는 caller-owned 경계를 명시한다. `ResilientNearJCache`/`ResilientSuspendNearJCache`의 `ClearBack`는 이번 PR2 보장 범위가 아니라는 문구도 추가한다.
- [ ] Hazelcast serialization manual 영어/한글에서 serialization-safe config와 runtime authority를 분리하고, `clear()`가 front/back를 모두 지우는 사실과 `close()` ownership을 정확히 설명한다.
- [ ] public constructor/factory parameter와 MXBean getter KDoc에 ownership/capability 전달과 default semantics를 명시한다.
- [ ] `CHANGELOG.md`에 behavior migration을 한국어로 기록하되 API identifiers와 exception token은 그대로 보존한다.
- [ ] 변경한 EN/KO 문서 쌍에는 동일한 `<!-- nearjcache-clear-authority-contract -->` / `<!-- /nearjcache-clear-authority-contract -->` marker를 넣고, marker block 안의 heading level 순서, fenced-code block 내용, API identifier·숫자 occurrence map, stale clear 범위 문구 부재를 검증한다. provider별 pair와 Redisson N/A inventory를 결과에 포함한다.

**Documentation checks:** Markdown links, code fences, `git diff --check`, relevant documentation tests, exact `clear()` result examples, EN/KO normalized heading/code-fence/token parity read-back.

```bash
python3 - <<'PY'
from collections import Counter
from pathlib import Path
import re

pairs = [
    ("cache/cache-core/README.md", "cache/cache-core/README.ko.md"),
    ("cache/cache-lettuce/README.md", "cache/cache-lettuce/README.ko.md"),
    ("cache/cache-hazelcast/README.md", "cache/cache-hazelcast/README.ko.md"),
    ("cache/cache-redisson/README.md", "cache/cache-redisson/README.ko.md"),
    ("docs/manual/en/modules/bluetape4k-cache-core/near-cache-semantics.md", "docs/manual/ko/modules/bluetape4k-cache-core/near-cache-semantics.md"),
    ("docs/manual/en/modules/bluetape4k-cache-hazelcast/jcache-near-cache-serialization.md", "docs/manual/ko/modules/bluetape4k-cache-hazelcast/jcache-near-cache-serialization.md"),
]
technical = re.compile(r"`[^`]+`|#(?:1355|1363|1368|1369|1408)|[0-9]+")
start, end = "<!-- nearjcache-clear-authority-contract -->", "<!-- /nearjcache-clear-authority-contract -->"
def contract(text):
    block = text.split(start, 1)[1].split(end, 1)[0]
    levels = re.findall(r"^(#{1,6}) ", block, re.M)
    fences = re.findall(r"```[^\n]*\n(.*?)```", block, re.S)
    return levels, [Counter(technical.findall(fence)) for fence in fences], Counter(technical.findall(block))

for left, right in pairs:
    left_text, right_text = Path(left).read_text(), Path(right).read_text()
    assert start in left_text and end in left_text and start in right_text and end in right_text, (left, right, "marker")
    assert contract(left_text) == contract(right_text), (left, right, "contract-block")
    assert not re.search(r"front[ -]only|프론트만|front-only", left_text + right_text, re.I), (left, right, "stale-clear-scope")
print(f"validated {len(pairs)} EN/KO pairs: heading/fence/token parity")
PY
```

## Task 7 — 순차 검증과 independent review

**Depends on:** Tasks 2–6

- [ ] core targeted authority/management tests를 실행한다.
- [ ] `./gradlew :bluetape4k-cache-core:test --no-configuration-cache`를 실행한다.
- [ ] `./gradlew detekt --no-configuration-cache`를 실행한다.
- [ ] provider tests를 Task 5 순서로 순차 실행하고 Docker/Testcontainers evidence를 기록한다.
- [ ] public API/ABI descriptor와 serialization compatibility를 확인한다.
- [ ] `git diff --check`와 worktree status를 확인한다.
- [ ] six perspective review를 독립 lane으로 요청한다: architecture/security, API/ABI, tests/failure modes, provider integration, documentation/Korean parity, operations/stack boundaries. 각 lane은 read-only이며 현재 plan/code exact head를 읽는다.
- [ ] P0/P1 finding, ABI break, peer-data deletion, listener barrier regression이 있으면 해당 task로 돌아가 수정 후 영향을 받은 review와 검증을 다시 실행한다.

## Task 8 — lesson, stacked-train handoff, stop boundary

**Depends on:** Task 7 clear

- [ ] `docs/lessons/2026-08-17-issue-1368-nearcache-clear-authority.md`에 결정, 검증 명령, 실제 결과, 실패/놀람, 후속 guard를 한국어로 기록한다.
- [ ] implementation branch가 PR1 exact head 위에 있고, PR1 merge 후 base rebase가 필요한 경우 rebase 전후 commit/tree를 기록한다.
- [ ] PR body를 작성할 경우 마지막 section은 정확히 `## DoD Status`로 하고, `Required checks: X/Y; N/A: N; Blocked: N` 형식과 evidence table, final status, unchecked items를 포함한다. PR 생성은 사용자에게 대상 repo/base/head와 검증 evidence를 보고한 뒤 별도 authority로 처리한다.
- [ ] merge는 하지 않는다. merge 전에 exact head, CI, review threads, mergeability, linked issue와 fresh merge approval를 다시 읽는다.
- [ ] canonical `develop`을 변경하지 않았고 isolated worktree가 clean인지 확인한다.

## 실패 모드와 rollback/risk 표

| 위험 | 방지 계약 | 검증 | rollback |
| --- | --- | --- | --- |
| 기존 caller의 destructive clear가 갑자기 실패함 | 기존 overload는 `DENY`, 명시적 owner만 opt-in, migration docs/KDoc | default-deny tests, docs parity, changelog | caller가 명시적 owner overload로 전환; shared caller는 key-list/namespace 전략 사용 |
| `NearJCacheConfig` serialization/ABI가 깨짐 | authority를 config 밖 runtime state로 분리하고 기존 constructor/copy/stream을 유지 | reflection/javap, serialization tests, precompiled consumer | production code를 revert해도 config wire format은 그대로 유지 |
| provider factory가 ownership을 과도하게 부여함 | 모든 기존 factory default `DENY`, explicit overload만 authority 전달 | Lettuce/Hazelcast/Redisson default + explicit tests | factory overload를 제거하지 않고 caller migration을 되돌림 |
| listener barrier 순서가 변함 | guard는 pre-mutation, owner path는 기존 sequence 재사용 | listener registration/epoch/generation and failure tests | authority guard와 overload만 revert하고 기존 clear sequence 복원 |
| fixture cleanup이 전부 거부됨 | cleanup 의도에 explicit `EXCLUSIVE_BACK_CACHE` 주입, shared tests는 deny 유지 | core fixture/full module tests | fixture injection commit만 revert하고 public default는 유지 |
| shared peer data가 삭제됨 | non-owner/default guard와 peer-data preservation test | provider shared-back tests | 해당 branch를 중단하고 offending factory/guard path 수정 |
| public `backCache` 직접 호출이 wrapper guard를 우회함 | caller-owned escape hatch를 문서화하고 untrusted caller에 reference를 노출하지 않음 | source inventory, docs/KDoc negative boundary, no false global-ACL claim | wrapper API surface를 숨기는 별도 ABI/design issue로 분리 |
| Resilient wrapper의 `ClearBack` 경로가 별도 authority 없이 back을 지움 | 이번 PR2의 non-goal과 후속 issue 후보를 lesson에 고정 | Resilient source inventory와 scope review | 별도 authority design 없이는 PR2에 포함하지 않음 |
| management metadata가 unstable payload를 노출함 | 두 stable enum token만 snapshot/MXBean에 공개 | MBeanServer read-back and descriptor test | metadata getter change만 revert; runtime authority contract 유지 |
| #1355 atomicity와 scope가 섞임 | compound atomicity/SuspendNearJCache를 명시적 non-goal로 고정 | plan review and symbol diff | 별도 issue/design으로 분리; PR2에 추가하지 않음 |

## 실행 명령 요약

```bash
git -C .worktrees/fix-1368-nearcache-clear-authority status --short --branch
git -C .worktrees/fix-1368-nearcache-clear-authority diff --check

./gradlew :bluetape4k-cache-core:test \
  --tests "io.bluetape4k.cache.nearcache.jcache.NearJCacheClearAuthorityContractTest" \
  --no-configuration-cache
./gradlew :bluetape4k-cache-core:test --no-configuration-cache
./gradlew detekt --no-configuration-cache
./gradlew :bluetape4k-cache-lettuce:test \
  --tests "io.bluetape4k.cache.nearcache.jcache.LettuceNearJJCacheTest" \
  --tests "io.bluetape4k.cache.nearcache.jcache.LettuceNearJCacheWriteThroughReentrancyTest" \
  --no-configuration-cache
./gradlew :bluetape4k-cache-hazelcast:test \
  --tests "io.bluetape4k.cache.nearcache.jcache.HazelcastNearJCacheTest" \
  --no-configuration-cache
./gradlew :bluetape4k-cache-redisson:test \
  --tests "io.bluetape4k.cache.nearcache.jcache.RedissonNearJCacheTest" \
  --no-configuration-cache
```

각 명령은 실제 exit code와 relevant test summary를 기록한다. Testcontainers 명령은 병렬 실행하지 않는다. 실패하면 실패한 명령, 첫 원인, 재현 조건, 수정 후 재실행 결과를 lesson과 final DoD에 남긴다.

## 요구사항 traceability

| 승인된 설계 요구 | 계획 task | 구현/검증 증거 |
| --- | --- | --- |
| serializable config와 runtime authority 분리 | Task 1, Task 3 | enum 파일, 기존 constructor/stream 불변성, reflection/serialization read-back |
| 세 namespace-wide operation fail-closed | Task 2, Task 3 | `NearJCacheClearAuthorityContractTest`, pre-mutation call-count와 state snapshot |
| exclusive owner의 기존 clear barrier 보존 | Task 2, Task 3 | listener detach/restore, epoch/generation, primary/suppressed failure tests |
| key-scoped remove는 유지 | Task 2, Task 3 | `removeAll(keys)`·single-key `remove` 허용 및 peer-data 보존 tests |
| provider가 ownership을 추론하지 않음 | Task 5 | Lettuce/Hazelcast/Redisson default DENY와 explicit overload tests |
| stable management visibility | Task 4 | snapshot/MXBean token, real `MBeanServer` read-back, descriptor evidence |
| reader migration과 tenant boundary | Task 6 | README/manual/matrix/CHANGELOG EN/KO parity, close ownership 문서 |
| wrapper/provider direct-call 경계와 Resilient non-goal | Task 1, Task 6, Task 8 | source inventory, explicit negative docs, 후속 issue 후보 lesson |
| stacked PR train과 별도 merge gate | Task 1, Task 7, Task 8 | exact base/head, CI/review read-back, PR/merge hold |

## SPW writer gate 기록

- [x] **SPW-01:** 독자는 구현자·리뷰어·운영자이고 목적은 #1368 PR2의 실행 순서와 증거를 고정하는 것이다. 기준 원본은 Issue #1368/#1408, 승인된 safety-tail spec, #1363 lesson, #1355 boundary와 현재 source/factory/test/docs inventory다. 미확정 사항은 implementation/test 결과로 남기며 계획에서 사실로 단정하지 않는다.
- [x] **SPW-02:** goal, architecture, scope/non-goal, exact file map, dependency order, RED/green actions, commands/evidence, rollback/rerun points, review/approval gates와 stop boundary를 포함했다.
- [x] **SPW-03:** 한국어 기술 문체, 고정 용어(`authority`, `namespace`, `front/back`, `listener generation barrier`, `SecurityException`)와 exact identifiers/commands를 보존하고 KO-01..KO-06 자연스러움 점검을 계획 read-back에서 수행했다.
- [x] **SPW-04:** 승인 spec-to-plan traceability 표와 현재 source symbol inventory를 기준으로 API/ABI, failure mode, provider ownership, management token, EN/KO migration을 대조했고, 독립 spec review가 CLEAR를 반환했다.
- [x] **SPW-05:** 최종 Markdown read-back에서 heading/table/list/code fence, 링크와 unchecked item을 확인하고, 독립 review disposition을 반영했다.

## Plan review disposition

- [x] architecture/API/stack review: 이전 public `backCache` escape와 Resilient `ClearBack` 누락을 wrapper-only boundary 및 explicit non-goal로 보완한 뒤 CLEAR.
- [x] quality/TDD review: provider RED, exact Lettuce FQCN, owner/default fixture 분리, 세 operation의 owner failure coverage와 MXBean default bridge를 보완한 뒤 CLEAR 조건 충족.
- [x] writer/Korean review: provider README 6개 surface, marker-scoped exact EN/KO parity, stale clear-scope negative assertion과 검증 순서를 보완한 뒤 CLEAR.

## Plan self-review checklist

- [x] 모든 구현 단계가 정확한 파일과 선행 조건을 가진다.
- [x] RED → production → management → provider → docs → verification 순서가 의존성을 만족한다.
- [x] default deny, pre-mutation guard, owner barrier, peer-data preservation이 독립 테스트로 고정된다.
- [x] old API/serialization shape와 new explicit overload의 경계가 명시되어 있다.
- [x] EN/KO 문서와 KDoc의 migration/close ownership/tenant non-goal이 포함되어 있다.
- [x] provider Testcontainers 순서, Docker/Ryuk 운영 경계, stacked PR base/head가 포함되어 있다.
- [x] 미완료 placeholder나 임시 보류 표식이 없다.
- [x] plan read-back, Korean naturalness, traceability, `git diff --check`가 완료된 뒤 commit한다.
