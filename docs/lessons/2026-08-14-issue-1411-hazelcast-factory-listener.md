# HazelcastNearJCache 공개 factory의 listener 경계 (#1411)

## 배경

Hazelcast JCache listener 설정은 cluster 배포를 위해 직렬화되어야 한다. 기존 공개 `HazelcastNearJCache(...)` factory는 `NearJCache`를 만든 뒤 back-cache listener를 등록했으므로, listener factory가 Caffeine front cache를 캡처하는 순간 `MutableCacheEntryListenerConfiguration` 직렬화가 시작됐다. Hazelcast client JCache 경로에서는 `HazelcastSerializationException`과 내부 `NotSerializableException`이 발생했고, stack trace에는 `JCacheEntryEventListener`가 포함됐다.

## 결정

공개 `HazelcastNearJCache(...)` factory는 `NearJCache(frontCache, backCache, nearCacheCfg)` listener-free 생성 경로를 사용한다. `HazelcastCaches.nearJCache(...)`와 동일하게 read-through와 write-through는 제공하지만 peer front-cache propagation은 보장하지 않는다. listener를 직접 등록하는 `NearJCache(config, backCache)` 생성 경로는 Hazelcast JCache에서 계속 unsupported로 둔다.

## 결과

공개 factory가 Hazelcast JCache back cache를 사용해 정상적으로 생성되고 put/get을 수행한다. API signature는 바꾸지 않았으며, Kotlin KDoc, README, EN/KO manual, capability matrix, CHANGELOG가 factory의 degraded capability와 direct listener 경계를 같은 의미로 설명한다.

## 검증

- RED: `JAVA_HOME=/Library/Java/JavaVirtualMachines/graalvm-jdk-25/Contents/Home ./gradlew :bluetape4k-cache-hazelcast:test --tests 'io.bluetape4k.cache.nearcache.jcache.HazelcastNearJCacheTest.public factory creates listener-free NearJCache on Hazelcast JCache' --no-configuration-cache --no-build-cache --rerun-tasks --max-workers=1` — 기존 구현은 listener 직렬화 때문에 `HazelcastSerializationException`으로 실패했다.
- GREEN: 같은 targeted test가 `BUILD SUCCESSFUL in 23s`로 통과했다.
- 회귀: `:bluetape4k-cache-hazelcast:test` 전체가 `BUILD SUCCESSFUL in 29s`로 통과했고, direct listener unsupported test와 두 listener-free factory test를 함께 실행했다.
- 리뷰 보완 후 `HazelcastNearJCacheTest` 전체가 `BUILD SUCCESSFUL in 38s`로 통과했고, write-through back 저장, back-only read-through, front 소유권 close, back/provider 비소유권, listener-backed cleanup을 추가로 검증했다. `:bluetape4k-cache-hazelcast:test` 전체도 같은 변경 상태에서 순차 재실행했다.

## 놓친 점과 다음 guard

기존 unsupported test는 `NearJCache(config, backCache)` 직접 생성 경로만 고정하고 공개 factory 호출은 검증하지 않았다. 또한 새 integration test는 Caffeine front가 store-by-reference여야 하므로 `NearJCacheConfig.getDefaultFrontCacheConfiguration()`을 사용해야 했다. public factory 문서에서는 supplied front의 ownership과 `clear()`의 front/back 범위를 명시해야 한다. 앞으로 Hazelcast JCache factory를 추가하거나 변경할 때는 listener capability를 KDoc와 capability matrix에 명시하고, 공개 factory integration test와 direct listener unsupported test를 모두 유지한다. Testcontainers 기반 검증은 모듈 간 동시 실행 없이 순차적으로 수행한다.
