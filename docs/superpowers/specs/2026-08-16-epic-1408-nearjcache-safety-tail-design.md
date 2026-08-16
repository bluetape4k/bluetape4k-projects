# Epic #1408 NearJCache security tail 설계

- Epic: [#1408](https://github.com/bluetape4k/bluetape4k-projects/issues/1408)
- 선행 이슈: [#1363](https://github.com/bluetape4k/bluetape4k-projects/issues/1363)
- 대상 이슈: [#1369](https://github.com/bluetape4k/bluetape4k-projects/issues/1369), [#1368](https://github.com/bluetape4k/bluetape4k-projects/issues/1368)
- 날짜: 2026-08-16
- 상태: written-spec 검토 요청
- 분류: Type-A Full Feature
- 대상 모듈: `cache/cache-core`, `cache/cache-lettuce`, `cache/cache-hazelcast`, `cache/cache-redisson`

## 문제와 현재 기준선

#1363은 `NearJCache`의 표준 read-through와 front/back 전체 `clear()` 계약을
정립했다. 이후 train은 compound operation, listener 재진입, write-through 관찰,
Lettuce 원자성, JMX 운영 지표까지 보강했다. 현재 남은 위험은 다음 두 가지다.

1. `getAll()`이 back에서 찾은 모든 값을 front에 한 번에 저장한다. 요청자가 큰
   key 집합을 전달하면 반환 결과의 정확성과 별개로 local heap residency를
   제한 없이 늘릴 수 있다.
2. `clear()`와 호환 alias `clearAllCache()`가 임의로 전달된 shared back cache의
   전체 namespace를 삭제한다. 같은 효과를 내는 무인자 `removeAll()`도 있으므로
   `clear()`만 제한하면 권한 검사를 우회할 수 있다.

현재 구현과 검증 기준선은 다음과 같다.

| 영역 | 현재 근거 | 설계에 미치는 영향 |
| --- | --- | --- |
| bulk read | `NearJCache.kt:677-729` | 결과는 front/back을 병합하고, back 결과 전체를 `frontCache.putAll`로 저장한다. |
| destructive clear | `NearJCache.kt:344-375` | listener detach와 front 삭제가 back clear 전에 실행된다. 권한 검사는 이보다 앞서야 한다. |
| destructive remove | `NearJCache.kt:917-929` | 무인자 `removeAll()`도 back namespace 전체를 순회 삭제한다. |
| compatibility alias | `NearJCache.kt:424-444` | `clearAllCache()`는 `clear()`에 위임하므로 같은 권한 계약을 적용해야 한다. |
| 직렬화 설정 | `NearJCacheConfig.kt:42-116` | 공개 `Serializable` data class이며 prior 5-인자 ABI와 누락 필드 복원을 유지한다. |
| management | `management/NearJCacheConfigurationSnapshot.kt`, `NearJCacheConfigurationMXBean.kt` | 생성 시점의 immutable, low-cardinality 설정을 JMX로 노출할 수 있다. |
| provider factory | `LettuceCaches.kt`, `HazelcastCaches.kt`, `RedissonCaches.kt`, `HazelcastNearJCache.kt` | factory가 back cache의 독점 소유권을 추론해서는 안 된다. |

## 목표

1. `getAll()` 반환값과 cache hit/miss 통계는 유지하면서, 기본 front bulk populate를
   fail-safe하게 우회한다.
2. 운영자가 명시한 entry-count 상한 안에서만 bulk 전체를 front에 저장하도록 한다.
3. bulk 정책을 직렬화 가능한 설정으로 제공하고, prior 5-인자 API와 legacy stream을
   보존한다.
4. shared back namespace 전체 삭제는 runtime authority가 있을 때만 허용한다.
5. 거부된 destructive operation이 listener, front, epoch, generation, back 중 어느
   상태도 변경하지 않음을 테스트로 증명한다.
6. 두 변경을 독립적으로 검토·merge할 수 있는 stacked PR 두 개로 전달한다.

## 비목표와 경계

- generic `Cache<K, V>` 위에 tenant별 key ownership이나 부분 삭제 protocol을
  추정하지 않는다. tenant 격리가 필요하면 tenant별 cache name/namespace 또는
  provider가 보장하는 별도 authorization layer를 사용한다.
- 값의 byte size를 측정하기 위해 임의 serializer를 호출하지 않는다. generic `V`의
  정확한 resident size는 portable하게 계산할 수 없고, 측정 자체가 serialization
  신뢰 경계와 추가 allocation을 만든다.
- 요청 key 집합 자체나 back provider의 response size를 제한하지 않는다. #1369의
  범위는 back 결과를 local front에 재저장하는 residency amplification이다.
- single-key `get()`의 read-through populate 계약은 변경하지 않는다.
- `SuspendNearJCache`, compound operation 원자성, 새로운 provider, dependency,
  module registration은 변경하지 않는다.
- #1368에서 `close()`를 data deletion operation으로 바꾸거나 back/provider
  lifecycle ownership을 이전하지 않는다.

## stacked PR train

```text
develop
  └─ fix/1369-nearcache-bounded-bulk
       └─ fix/1368-nearcache-clear-authority
```

| 순서 | 이슈 | base | head | 독립 결과 |
| --- | --- | --- | --- | --- |
| PR 1 | #1369 | `develop` | `fix/1369-nearcache-bounded-bulk` | bounded bulk front population과 stable metadata |
| PR 2 | #1368 | `fix/1369-nearcache-bounded-bulk` | `fix/1368-nearcache-clear-authority` | fail-closed namespace clear authority |

PR 1의 exact head CI와 review blocker가 모두 정리된 뒤 PR 2 branch를 만든다.
PR 1이 merge되면 PR 2 base를 `develop`으로 바꾸고 exact head CI와 review를 다시
검증한다. 두 PR의 merge 승인은 각각 별도로 받으며 auto-merge는 사용하지 않는다.

## PR 1 — bounded bulk front population

### 공개 정책 타입

`cache-core`에 다음 의미의 공개 sealed policy를 추가한다. 실제 명칭은 아래 계약을
그대로 사용한다.

```kotlin
sealed interface BulkFrontPopulationPolicy : Serializable {
    data object BypassFront : BulkFrontPopulationPolicy

    data class PopulateIfAtMost(
        val maximumEntryCount: Int,
    ) : BulkFrontPopulationPolicy
}
```

`PopulateIfAtMost.maximumEntryCount`는 1 이상이어야 하며 잘못된 값은 생성 시
`IllegalArgumentException`으로 거부한다. 각 구현 타입은 `serialVersionUID = 1L`을
유지한다. 정책 인스턴스는 identity, key, value, provider payload를 포함하지 않는다.

`NearJCacheConfig`에는 다음 property를 추가한다.

```kotlin
val bulkFrontPopulationPolicy: BulkFrontPopulationPolicy =
    BulkFrontPopulationPolicy.BypassFront
```

기본값 `BypassFront`는 기존의 무제한 local residency를 중단한다. 기존 front hit는
그대로 반환하며 제거하지 않고, back에서 찾은 값도 호출 결과에는 모두 포함한다.

### `getAll()` 실행 계약

`getAll(keys)`는 현재와 같이 front hit를 먼저 읽고, 누락 key만 back에 요청하고,
front와 back 결과를 모두 병합해 반환한다. front populate 여부만 다음처럼 분리한다.

| 정책 | `backValues.size` | front 처리 | 반환 결과 |
| --- | ---: | --- | --- |
| `BypassFront` | 모든 값 | back 결과를 front에 저장하지 않음 | front/back 전체 병합 |
| `PopulateIfAtMost(n)` | `0..n` | back 결과 전체를 한 번의 `putAll`로 저장 | front/back 전체 병합 |
| `PopulateIfAtMost(n)` | `n + 1` 이상 | back 결과 전체의 front populate를 우회 | front/back 전체 병합 |

상한은 요청 key 수나 `missingKeys.size`가 아니라 실제로 새 residency 후보가 되는
`backValues.size`에 적용한다. 상한을 넘으면 임의의 first-N을 선택하지 않고 batch
전체를 우회한다. `Set`/provider iteration order에 따라 front 내용이 달라지는 것을
막기 위한 all-or-nothing 결정이다.

epoch가 back 조회 중 바뀌면 기존 계약대로 front populate를 건너뛴다. 정책 판정은
back 조회가 성공한 뒤, `mutationGate` 안에서 epoch 일치 여부와 함께 확인한다.
front `putAll` 실패 시 back 결과는 반환하고 sanitized warning을 남기며,
`CancellationException`은 기존 계약대로 다시 던진다. back `getAll` 예외도 provider
예외를 숨기지 않는다.

### ABI와 serialization 호환성

- published prior-release 5-인자 constructor와 `copy` descriptor를 유지한다.
- train 기준의 6-인자 constructor와 `copy`도 명시적 overload로 유지한다.
- `serialVersionUID`는 `1L`을 유지한다.
- legacy stream에 `bulkFrontPopulationPolicy`가 없으면 `readObject`가
  `BypassFront`를 복원한다.
- 명시한 `PopulateIfAtMost` 값은 현재 stream round-trip 뒤 동일하게 복원한다.
- 새 primary data-class shape의 `componentN`, `copy`, `copy$default`와 보존 대상
  overload를 reflection test로 고정한다.
- builder는 동일한 safe default를 제공하며 명시한 policy를 config에 전달한다.

과거 stream의 기존 무제한 populate를 재현하지 않고 새 safe default를 적용하는 것은
의도한 보안 migration이다. 반환 결과 호환성은 유지된다.

### stable management metadata

`NearJCacheConfigurationSnapshot`과 `NearJCacheConfigurationMXBean`에 다음
low-cardinality 값을 추가한다.

| 속성 | 값 |
| --- | --- |
| `BulkFrontPopulationPolicy` | `BYPASS_FRONT` 또는 `POPULATE_IF_AT_MOST` |
| `BulkFrontPopulationMaximumEntryCount` | `BypassFront`이면 `0`, bounded 정책이면 양의 상한 |

문자열 값은 Kotlin subtype 이름에 의존하지 않는 stable token으로 고정한다. 숫자
`0`은 “적용 가능한 상한 없음”을 뜻하며 `PopulateIfAtMost(0)`은 허용하지 않는다.
MXBean method는 `getBulkFrontPopulationPolicy(): String`과
`getBulkFrontPopulationMaximumEntryCount(): Int`로 고정한다.
기존 JCache logical/tier 통계 수치는 front populate 정책과 분리한다. 이번 PR에서는
caller-controlled label이나 key/value가 포함된 metric을 추가하지 않는다.

## PR 2 — destructive clear runtime authority

### 권한 모델

serializable `NearJCacheConfig`와 분리된 runtime-only enum을 추가한다.

```kotlin
enum class NearJCacheClearAuthority {
    DENY,
    EXCLUSIVE_BACK_CACHE,
}
```

이 값은 `NearJCache` 인스턴스 생성 시 immutable하게 고정되지만
`NearJCacheConfig` property나 serialized stream에는 들어가지 않는다.
`EXCLUSIVE_BACK_CACHE`는 library가 검증한 소유권 증명이 아니라 caller가 해당 back
namespace의 유일한 destructive owner임을 명시적으로 주장하는 capability다.

기존 public 3-인자 constructor는 그대로 유지하며 `DENY`로 위임한다. authority를
받는 새 4-인자 constructor/overload를 추가한다. `NearJCache(config, backCache)`와
Lettuce/Hazelcast/Redisson factory의 기존 signature도 `DENY`로 유지하고, 명시적
authority를 받는 overload만 추가한다. factory는 cache를 생성하거나 `getOrCreate`
했다는 이유로 `EXCLUSIVE_BACK_CACHE`를 자동 부여하지 않는다.

### 보호할 operation

다음 namespace-wide operation은 동일한 authority guard를 사용한다.

- `clear()`
- `clearAllCache()`
- 인자 없는 `removeAll()`

`DENY` 상태에서는 `SecurityException`을 던진다. 오류 메시지는 operation과 필요한
authority token만 포함하고 cache name, key, value, provider payload를 포함하지 않는다.
검사는 `compoundGate` 획득, listener detach, `mutationEpoch` 증가, back write generation
증가, front 삭제, back 호출보다 먼저 실행한다. 따라서 거부는 실행 상태를 전혀
바꾸지 않는다.

`EXCLUSIVE_BACK_CACHE`에서는 기존 listener generation barrier와 front/back 삭제
순서를 그대로 사용한다. 무인자 `removeAll()`도 동일 authority를 요구하지만,
구체적인 key 집합을 받는 `removeAll(keys)`와 single-key remove는 이번 권한의
대상이 아니다. 이 API는 tenant authorization을 제공하지 않으므로, shared namespace의
호출자는 자신의 key만 전달할 책임이 있으며 강한 tenant 격리는 namespace 분리로
구현해야 한다.

### management와 migration

configuration snapshot에는 `ClearAuthority` stable token을 추가해 `DENY` 또는
`EXCLUSIVE_BACK_CACHE`를 노출한다. 이는 생성 시점의 low-cardinality 운영 정보이며
소유자 identity나 credential이 아니다. MXBean method는
`getClearAuthority(): String`으로 고정한다.

이 변경은 기존 호출자의 `clear()`, `clearAllCache()`, 무인자 `removeAll()` 기본
동작을 의도적으로 fail-closed로 바꾸는 behavior migration이다. README 영문/한글,
capability matrix, 영문/한글 manual, release-facing migration note에 다음을 명시한다.

1. shared back 사용자는 기본 `DENY`를 유지한다.
2. namespace 전체 삭제가 필요한 독점 owner만 runtime authority를 명시한다.
3. tenant별 삭제는 generic `clear()`로 표현하지 말고 별도 namespace 또는 key 목록을
   사용한다.
4. `close()`는 계속 front와 wrapper resource만 정리하며 back data/provider를 닫거나
   삭제하지 않는다.

## 실패 모드와 안전 계약

| 실패 모드 | 계약 |
| --- | --- |
| policy 상한이 0 이하임 | config 사용 전 `IllegalArgumentException`; cache 상태 변화 없음 |
| bulk 결과가 상한을 초과함 | 결과는 모두 반환하고 front populate만 batch 전체 우회 |
| back `getAll` 실패 | provider 예외 전파; 해당 back 결과의 front populate 없음 |
| front `putAll` 실패 | `CancellationException` 재전파, 그 외 runtime failure는 sanitized warning 후 back 결과 반환 |
| bulk read 중 epoch 변경 | 결과는 반환하되 front populate 없음 |
| destructive operation 권한 없음 | 첫 mutation 전에 `SecurityException`; front/back/listener/epoch/generation 불변 |
| exclusive clear 중 back 실패 | 기존 primary/suppressed failure와 listener 복구 계약 유지 |
| provider factory가 back cache를 생성함 | 독점 소유권으로 추론하지 않고 기본 `DENY` 유지 |
| tenant별 부분 clear 요청 | generic JCache로 지원하지 않음; namespace/key-list 전략으로 이동 |

## 구현 경계

### PR 1 예상 파일

- `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/BulkFrontPopulationPolicy.kt`
- `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheConfig.kt`
- `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheConfigBuilder.kt`
- `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCache.kt`
- `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/management/*`
- `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/*`
- `cache/cache-core/README.md`, `cache/cache-core/README.ko.md`
- `docs/cache/near-cache-capability-matrix.md`

### PR 2 예상 파일

- `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheClearAuthority.kt`
- `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCache.kt`
- `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/management/*`
- `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/*`
- `cache/cache-lettuce`, `cache/cache-hazelcast`, `cache/cache-redisson`의 factory와 계약 테스트
- `cache/cache-core/README.md`, `cache/cache-core/README.ko.md`
- `docs/cache/near-cache-capability-matrix.md`
- `docs/manual/en/modules/bluetape4k-cache-core/near-cache-semantics.md`
- `docs/manual/ko/modules/bluetape4k-cache-core/near-cache-semantics.md`
- `docs/manual/en/modules/bluetape4k-cache-hazelcast/jcache-near-cache-serialization.md`
- `docs/manual/ko/modules/bluetape4k-cache-hazelcast/jcache-near-cache-serialization.md`
- `CHANGELOG.md`

symbol search에서 추가 public factory나 manual 예제가 발견되면 같은 PR 안에서
영문/한글 원문 동등성을 맞춘다. 새로운 module/dependency 또는 tenant protocol이 필요해지면 이
설계 범위를 넘으므로 구현을 중단하고 별도 설계로 분리한다.

## 테스트 설계

### PR 1 RED/회귀 테스트

1. 기본 `BypassFront`에서 front miss/back hit가 결과에 포함되지만 front에는 저장되지 않는다.
2. 기존 front hit는 기본 정책에서도 그대로 반환되고 유지된다.
3. `PopulateIfAtMost(n)`에서 back hit 수가 `n` 이하이면 batch 전체가 front에 저장된다.
4. back hit 수가 `n + 1`이면 어느 entry도 front에 저장되지 않는다.
5. 요청 key는 많지만 실제 back hit가 상한 이하인 경우 실제 hit 수를 기준으로 populate한다.
6. 큰 value도 반환 정확성을 유지하며, byte size가 아니라 entry count를 적용함을 고정한다.
7. front/back 부분 hit와 missing key가 섞여도 반환 map과 logical/tier 통계가 정확하다.
8. epoch 경합, back 예외, front populate 예외, cancellation 계약을 유지한다.
9. 5/6-인자 constructor·copy, 새 data-class shape, legacy/current serialization을 검증한다.
10. builder와 configuration MXBean이 실제 적용 policy/token/limit을 노출한다.

### PR 2 RED/회귀 테스트

1. 기존 constructor/factory 기본값에서 `clear()`가 `SecurityException`을 던진다.
2. 거부 전후 front/back contents, listener registration, mutation epoch, generation이 같다.
3. `clearAllCache()`와 무인자 `removeAll()`로 authority를 우회할 수 없다.
4. `EXCLUSIVE_BACK_CACHE`에서 front/back 전체 삭제와 listener generation barrier가 유지된다.
5. back clear 실패와 listener 재등록 실패의 primary/suppressed 예외 계약을 유지한다.
6. `removeAll(keys)`와 single-key remove는 승인된 범위에서 기존 계약을 유지한다.
7. direct constructor와 각 provider factory의 기존 JVM signature가 남아 있고 기본은 `DENY`다.
8. 명시적 factory overload가 authority를 전달하고 management token에 반영한다.
9. Lettuce, Hazelcast, Redisson의 shared back fixture에서 거부된 호출이 peer data를 삭제하지 않는다.

`cache-core` targeted test와 detekt를 먼저 실행한다. provider Testcontainers 검증은
Lettuce → Hazelcast → Redisson 순으로 직렬 실행하며, 활성 Docker context와 managed
Testcontainers 환경을 확인한다. 병렬 worktree에서 Docker-backed test를 겹쳐 실행하지 않는다.

## acceptance criteria 매핑

| 요구 | 증거 |
| --- | --- |
| #1369 safe default | 기본 `BypassFront` RED/회귀 테스트와 README 계약 |
| #1369 bounded opt-in | `PopulateIfAtMost(n)` 경계값 및 all-or-nothing 테스트 |
| #1369 결과/API 호환성 | 병합 결과·통계 테스트, 5/6-인자 ABI와 serialization test |
| #1369 관측 가능 설정 | configuration MXBean stable token/limit test |
| #1368 fail-closed | 세 namespace-wide API의 pre-mutation `SecurityException` test |
| #1368 owner 경로 | explicit authority의 front/back clear와 listener barrier test |
| #1368 shared back 보호 | provider peer-data 보존 test와 namespace guidance |
| #1368 migration | README 양언어, manual 양언어, capability matrix, release-facing note |
| stacked train | live PR base/head read-back, exact head CI, 최신 review blocker 0 |

## 고려했지만 채택하지 않은 대안

1. **기존 무제한 populate 유지**: 반환 결과와 성능은 유지되지만 attacker-controlled
   local heap amplification을 막지 못해 거부한다.
2. **임의 first-N populate**: iteration order가 provider마다 달라 front residency가
   비결정적이므로 거부한다.
3. **value byte-size 측정**: generic value의 portable한 크기 계약이 없고 serializer
   호출이 새 보안·allocation 경계를 만들기 때문에 거부한다.
4. **serializable config의 boolean owner flag**: 복사·직렬화된 설정이 실제 runtime
   소유권처럼 재사용될 수 있어 거부한다.
5. **factory 생성 여부로 독점 소유권 추론**: `getOrCreate` namespace와 client lifecycle은
   공유될 수 있으므로 거부한다.
6. **`clear()`만 보호**: 무인자 `removeAll()` 우회가 남으므로 거부한다.
7. **generic cache에서 tenant별 부분 clear 구현**: key ownership 정보가 없어 안전하게
   판정할 수 없으므로 거부한다.

## 전달과 중단 조건

1. 이 written spec이 승인되기 전에는 Kotlin 구현과 RED 테스트를 시작하지 않는다.
2. 승인 후 별도 implementation plan에서 exact file/action/test 순서를 고정한다.
3. PR 1은 bounded policy, compatibility, metadata만 포함하고 clear authority를 섞지 않는다.
4. PR 2는 PR 1 head 위에만 쌓고 authority와 migration을 포함한다.
5. P0/P1 review finding, ABI 복원 불가, generic API로 tenant ownership을 추론해야 하는
   상황이 발견되면 train을 멈추고 설계를 다시 승인받는다.
6. 각 PR은 targeted test, module test, detekt, `git diff --check`, exact head CI,
   독립 review를 통과해야 다음 gate로 진행한다.

## 설계 DoD

- [x] 문제, 범위, 비범위, 공개 계약, failure mode를 정의했다.
- [x] #1369 반환 정확성과 front residency 결정을 분리했다.
- [x] safe default와 bounded opt-in의 경계값을 정의했다.
- [x] #1368 authority를 serializable config와 분리했다.
- [x] `removeAll()` 우회를 포함한 namespace-wide operation을 식별했다.
- [x] prior API/serialization, provider factory, JMX, 문서 migration을 포함했다.
- [x] 두 PR의 base/head와 merge 후 rebase 검증 순서를 고정했다.
- [ ] 사용자 written-spec 승인
- [ ] 승인된 spec 기반 implementation plan
