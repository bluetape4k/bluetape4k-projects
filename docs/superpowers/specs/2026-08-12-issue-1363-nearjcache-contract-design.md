# NearJCache 표준 read/clear 계약 정합성 설계

- 이슈: [#1363](https://github.com/bluetape4k/bluetape4k-projects/issues/1363)
- 날짜: 2026-08-12
- 상태: 독립 검토 P1 보강안 승인됨
- 대상: `cache/cache-core`
- 분류: Type-A Full Feature (공개 JCache API·2-tier 의미론 변경)

## 문제

`NearJCache`는 `javax.cache.Cache`를 구현하지만 표준 메서드의 대상이
front cache로만 제한되어 있다. back cache에만 존재하는 값을 표준 `get`,
`containsKey`, `getAll`로 관찰할 수 없고, `clear()`도 front만 지운다.
back까지 조회하는 동작은 비표준 확장 메서드인 `getDeeply()`에만 존재한다.

이 상태에서 호출자가 `NearJCache`를 `Cache<K, V>`로 받으면 동일한 논리적
캐시에서 값이 누락되거나 `clear()` 이후 back 값이 다시 나타나는 예측 불가능한
결과를 얻는다. README는 현재 구현을 JCache 호환 2-tier 캐시로 설명하므로
문서·타입·실행 의미가 서로 어긋난다.

## 현재 근거

| 영역 | 근거 | 관찰 |
| --- | --- | --- |
| 공개 타입 | `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/jcache/JCacheType.kt:6` | `JCache`는 `javax.cache.Cache` typealias다. |
| clear | `NearJCache.kt:173-176` | `clear()`가 `frontCache.clear()`만 호출한다. |
| contains/get | `NearJCache.kt:214-219` | `containsKey`와 `get`이 front만 조회한다. |
| getAll | `NearJCache.kt:243-247` | `getAll`이 front 결과만 반환한다. |
| 비표준 fallback | `NearJCache.kt:221-241` | `getDeeply`만 back 조회 후 front를 채운다. |
| 기존 테스트 | `AbstractNearJCacheTest.kt:81-99, 531-553` | front-only 동작과 별도 `clearAllCache`가 고정되어 있다. |
| 사용자 문서 | `cache/cache-core/README.ko.md:101-104, 151-156` | JCache 호환 2-tier 구현으로 설명한다. |
| capability 문서 | `docs/cache/near-cache-capability-matrix.md:30-42` | backend별 listener/degraded 경계를 정의하지만 표준 read 계약은 고정하지 않는다. |

JCache 1.1 API는 `get`, `containsKey`, `getAll`, `clear`를 캐시 인스턴스의
표준 메서드로 정의하고, `getAndRemove`·`getAndReplace` 등 compound 메서드는
원자성을 별도로 요구한다. 본 설계는 [공식 Cache API 문서](https://www.javadoc.io/static/javax.cache/cache-api/1.1.0/javax/cache/Cache.html)의
read/clear 항목만 직접 다루며 compound 원자성은 #1355의 범위로 남긴다.

## 목표

1. `NearJCache`를 `Cache<K, V>`로 참조하는 호출자가 논리적 2-tier 캐시를 일관되게 관찰하게 한다.
2. 표준 `get`, `containsKey`, `getAll`, `clear` 의미를 구현·테스트·문서에 동일하게 고정한다.
3. 기존 `getDeeply`와 `clearAllCache` 사용자의 소스 호환성을 유지한다.
4. back cache 공유 인스턴스에 대한 listener 전파 여부를 `clear` 계약과 분리해 명시한다.
5. 비동기 write와 동시 read-through가 `clear` 이후 stale 값을 재삽입하지 않도록 한다.
6. 기본 front cache의 자동 populate가 필터 없는 Java serialization 경계를 만들지 않도록 한다.

## 비목표 및 경계

- `getAndPut`, `getAndRemove`, `getAndReplace`, `putIfAbsent`, `replace`의
  cross-tier 원자성은 구현하지 않는다. 해당 범위는 [#1355](https://github.com/bluetape4k/bluetape4k-projects/issues/1355)에서 다룬다.
- `iterator`, `invoke`, `loadAll` 등 아직 front/back 경계가 별도로 정의되지 않은
  메서드의 확장은 본 작업에서 하지 않는다. 표준 read/clear 범위를 문서에서
  명시하고 후속 이슈 후보로 남긴다.
- 새로운 의존성, 모듈, catalog 버전, backend provider를 추가하지 않는다.
- `SuspendNearJCache`의 동작을 동기 API 변경에 맞춰 임의로 변경하지 않는다.
  다만 문서에서 동기·suspend 계약의 차이가 오해를 만들지 않는지 확인한다.
- 이번 이슈에서는 front cache 용량 상한과 공유 back cache의 tenant/owner 권한 모델을
  새 공개 설정으로 추가하지 않는다. 현재는 명시적인 문서 계약과 provider별 기존
  capacity 설정을 사용하며, 무제한 bulk residency와 destructive shared clear는
  후속 이슈 후보로 기록한다.

## 선택한 설계

### 논리적 2-tier 표준 Cache 유지

`NearJCache`가 계속 `JCache<K, V>`를 구현하도록 유지하고, 표준 메서드의
조회 대상은 front와 back을 합친 논리적 캐시로 정의한다.

| 메서드 | 계약 |
| --- | --- |
| `get(key)` | front hit를 먼저 반환한다. front miss면 back을 조회하고, 값을 찾으면 front에 populate한 뒤 반환한다. |
| `containsKey(key)` | front hit 또는 back hit이면 `true`다. back 조회를 위해 값을 읽지 않는다. |
| `getAll(keys)` | front 결과를 먼저 취하고, 누락 키만 back에서 조회한다. back 결과는 front에 populate하고 두 결과를 병합한다. |
| `clear()` | 해당 `NearJCache`가 소유한 front와 back의 모든 매핑을 삭제한다. JCache `clear`처럼 listener를 통한 peer 전파는 보장하지 않는다. |
| `getDeeply(key)` | 기존 소스 호환성을 위해 유지하며 표준 `get(key)`와 동일한 동작을 사용한다. |
| `clearAllCache()` | 기존 소스 호환성을 위해 유지하며 표준 `clear()`와 동일한 front/back 삭제 계약을 사용한다. |

`clear()`가 공유 back cache를 지워도 다른 NearJCache 인스턴스의 front가
listener 없는 factory/degraded 조합에서 이미 보유한 값을 자동으로 지운다는
뜻은 아니다. capability matrix와 README에 이 경계를 명시하고, peer 전파가
필요한 경우 기존 `removeAll()` 경로를 사용하도록 안내한다.

back read 실패는 현재 JCache provider가 노출하는 예외를 호출자에게 전달한다.
back에서 읽은 값을 front에 populate하지 못하더라도 이미 확보한 back 값은
반환하되, 기존 로깅 규칙을 사용해 populate 실패를 관찰 가능하게 한다. 이
정책은 read availability와 local warm-up 실패를 분리한다.

### 동시성·비동기 write barrier

read-through는 `mutationEpoch`를 관찰한다. `get`과 `getAll`은 front 조회 후
epoch를 캡처하고, back 조회가 끝난 뒤 동일 epoch일 때만 front populate를
수행한다. epoch가 바뀌었으면 back에서 읽은 값은 호출 결과로 반환할 수 있지만
front에는 저장하지 않는다. 모든 front mutation과 `clear`는 epoch를 증가시킨다.

back mutation은 `backWriteLock` 아래에서 실행한다. 비동기 write는 예약 시
epoch를 캡처하고 lock을 얻은 뒤 현재 epoch가 다르면 backend 작업을 건너뛴다.
`clear`는 mutation gate를 잡고 새 epoch를 발행한 다음 front를 지우고, 기존
write가 실제 backend 호출을 끝내 lock을 놓을 때까지 기다린 뒤 back을 지운다.
따라서 timeout으로 completion이 먼저 실패해도 late backend write가
`clear()` 반환 뒤 back 값을 되살릴 수 없다. clear 중 새 mutation은 gate가
풀린 뒤 새 epoch로 시작한다.

### compound operation 경계

`getAndPut`, `getAndRemove`, `getAndReplace`, `putIfAbsent`, `replace`의
cross-tier 원자성은 #1355의 범위다. 이번 변경으로 표준 `containsKey`/`get`이
back을 조회하게 되더라도 compound 메서드는 내부 `frontContainsKey`/
`frontGet` helper를 사용해 기존 front-only 왕복 수와 동작을 유지한다. 이
경계를 문서와 호출 수 회귀 테스트에 고정하고, 원자성 개선을 이번 PR에서
주장하지 않는다.

### front cache serialization 신뢰 경계

`NearJCacheConfig.getDefaultFrontCacheConfiguration()`은
`setStoreByValue(false)`를 사용해 기본 Caffeine front cache를
store-by-reference로 만든다. `NearJCache`는 `frontCacheConfiguration`이
store-by-value로 설정된 custom config를 fail-closed로 거부한다. 따라서
back 값을 자동 populate할 때 필터 없는 Java serialization copier가 기본
경로에 들어오지 않는다. 운영자가 별도 provider/copy 정책을 선택해야 하는
경우에는 해당 provider의 안전한 serialization/filter 정책을 별도 설계해야
하며, 이 이슈에서는 우회 플래그를 추가하지 않는다.

populate 실패 로그는 operation/provider/cache lifecycle 메타데이터만 기록하고
key, value, raw payload, credential 또는 provider payload를 기록하지 않는다.

### 고려했지만 채택하지 않은 대안

1. **front-only 타입으로 분리**: `javax.cache.Cache` 구현을 제거하고 별도
   `FrontOnlyNearCache` 타입을 노출한다. 의미는 가장 명확하지만 기존 반환
   타입·사용자 캐스팅·README 예제를 깨뜨리는 공개 API 변경이므로 거부한다.
2. **표준 타입은 유지하되 현재 front-only 의미를 문서화**: 구현 변경은 작지만
   JCache 호출자에게 표준 계약 위반을 계속 노출하고 #1363의 결함을 해결하지
   못하므로 거부한다.
3. **표준 메서드와 front-only 메서드를 모두 별도 이름으로 유지**: `get`과
   `getDeeply`를 병행하면 호출자가 어떤 메서드를 써야 하는지 다시 판단해야
   하며 표준 `Cache` 참조에서는 문제를 해결하지 못하므로 거부한다.

## 구현 경계

예상 변경 파일은 다음과 같다.

- `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCache.kt`
- `cache/cache-core/src/testFixtures/kotlin/io/bluetape4k/cache/nearcache/jcache/AbstractNearJCacheTest.kt`
- `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/*` 중 계약 회귀가 필요한 테스트
- `cache/cache-core/README.md`
- `cache/cache-core/README.ko.md`
- `docs/cache/near-cache-capability-matrix.md`
- Hazelcast factory 테스트 등 `clear()` 후 `getDeeply()`를 직접 검증하는 영향받은 테스트

모듈 등록, dependency catalog, Gradle workflow, API artifact 이름은 변경하지
않는다. 실제 symbol search에서 추가 caller가 발견되면 계획 문서에 영향 범위를
갱신하고, 승인된 범위를 넘는 공개 API 변경은 중단한다.

## 테스트 설계

테스트는 현재 공용 `AbstractNearJCacheTest` fixture를 재사용하고, 표준 타입
참조를 통해 동작을 검증한다.

1. `val cache: Cache<String, Any> = nearJCache` 컴파일/실행 경계에서 back-only
   key에 `get`이 값을 반환하고 front를 populate하는지 검증한다.
2. back-only key에 `containsKey`가 `true`인지, 없는 key가 `false`인지 검증한다.
3. front hit와 back-only key가 섞인 `getAll`이 병합 결과를 반환하고 back 값을
   front에 populate하는지 검증한다.
4. `clear()`가 해당 front와 back 모두를 비우는지 검증하고, listener 없는
   peer front에 대한 전파를 계약에서 보장하지 않는지 확인한다.
5. `getDeeply`와 `clearAllCache`가 표준 메서드와 의미가 갈라지지 않는지 검증한다.
6. `getAll`이 `backCache.getAll(missedKeys)`와 `frontCache.putAll(backValues)`를
   각각 최대 한 번 사용하고, front hit만 있으면 back을 호출하지 않는지 검증한다.
7. latch 기반으로 `get`/`getAll`과 `put`/`replace`/`remove`/`clear`가 겹칠 때
   stale front populate가 발생하지 않는지 검증한다.
8. 비동기 write의 timeout-late completion이 `clear()` 후 back을 재삽입하지
   않는지 검증한다.
9. 기본 config가 store-by-reference이고 store-by-value custom config가
   생성 단계에서 거부되는지 검증한다.
10. compound 메서드가 front-only helper를 사용해 불필요한 back round-trip을
   추가하지 않는지 호출 수로 검증한다.
11. 기존 backend fixture(Lettuce, Redisson, Hazelcast, Cache2k, Ehcache)의
   지원/degraded/unsupported 경계를 깨지 않는지 순차 검증한다.

compound operation 테스트는 #1355에 중복 등록하지 않으며, 이번 변경으로
새롭게 노출되는 경계가 있으면 해당 이슈에 연결만 남긴다.

## 실패 모드와 대응

| 실패 모드 | 대응 |
| --- | --- |
| back read가 실패함 | provider 예외를 숨기지 않고 호출자에게 전달; 대상 테스트에서 예외 계약 확인 |
| front populate가 실패함 | back에서 확보한 값은 반환하고 경고 로그 기록; 다음 read가 재시도 가능해야 함 |
| read-through와 mutation이 경합함 | epoch가 바뀌면 populate를 건너뛰고, stale 값을 front에 저장하지 않음 |
| timeout-late write가 clear와 경합함 | backWriteLock과 epoch barrier로 실제 backend 호출 종료 후 clear를 수행 |
| 공유 back `clear` 후 peer front가 stale함 | `clear`는 peer listener 전파를 보장하지 않는다고 문서·matrix에 명시; peer 전파는 `removeAll` 사용 |
| 기존 테스트가 front-only 가정을 고정함 | 계약 테스트를 표준 `Cache` 기준으로 전환하고, peer/degraded 테스트는 의도한 경계를 분리 |
| compound 메서드가 새 read 계약과 충돌함 | 이번 작업에서 원자성 개선을 주장하지 않고 #1355 후속으로 추적; P0/P1 regression이면 구현 범위를 재승인 |
| store-by-value custom config가 전달됨 | 생성 단계에서 fail-closed하고 안전한 copier/filter 정책을 별도 설계하도록 안내 |
| clear의 front 또는 back 삭제가 실패함 | 수행 순서와 부분 상태를 로그·예외로 노출하고, 호출자가 같은 clear를 재시도할 수 있도록 유지 |

## 수용 기준과 DoD

- [ ] 표준 `Cache`의 `get`, `containsKey`, `getAll`, `clear` 의미가 위 표와 구현에 일치한다.
- [ ] back fallback과 front populate 회귀 테스트가 `Cache<K,V>` 참조를 통해 통과한다.
- [ ] `clear`가 해당 front/back를 지우고 peer 전파 한계를 문서화한다.
- [ ] `getDeeply`·`clearAllCache` 호환 동작이 고정된다.
- [ ] mutation epoch와 back-write barrier가 동시 read/clear 및 timeout-late write의 stale 재삽입을 차단한다.
- [ ] `getAll` bulk 호출 수와 compound front-only 경계가 회귀 테스트로 고정된다.
- [ ] 기본 front config가 store-by-reference이며 store-by-value custom config를 fail-closed한다.
- [ ] `README.md`, `README.ko.md`, capability matrix가 동일한 계약을 설명한다.
- [ ] `:bluetape4k-cache-core:test`와 영향 backend 검증이 통과한다.
- [ ] `git diff --check`, Kotlin checklist, Type-A pre-PR review에서 P0/P1이 0건이다.

## 롤백 및 중단 조건

구현 중 public API 삭제, 새로운 backend 의존성, compound atomicity 보장이
필요해지면 이 설계를 중단하고 별도 승인/이슈로 분리한다. 표준 `Cache` 타입
참조에서 기존 동작과 호환되지 않는 P0/P1 회귀가 발견되면 feature branch에서
수정하고, 설계 범위를 바꾸기 전 사용자 재승인을 받는다.
