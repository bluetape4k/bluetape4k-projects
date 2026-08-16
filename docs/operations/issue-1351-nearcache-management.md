# Issue #1351 NearJCache management 운영 가이드

이 문서는 `NearJCache`의 explicit JMX 등록, canary 판정, migration, rollback,
cleanup 증거를 한 흐름으로 기록한다. Dashboard는 설정 identity와 application
inventory를 함께 조회해야 한다. JMX 부재만으로 `DISABLED`로 분류하지 않는다.

## Lifecycle classifier

Classifier 입력은 configuration의 management/statistics flag, registration handle의
state, handle이 추적하는 `activeObjectNames`, 실제 server에 등록된 exact name이다.

| 상태 | 판정 | 조치 |
| --- | --- | --- |
| `DISABLED` | management와 statistics flag가 모두 false라는 configuration 증거가 있음 | 의도된 비활성인지 rollout record와 비교 |
| `NOT_REGISTERED` | 하나 이상의 flag가 true지만 handle 또는 active name이 없음 | 등록 단계 실패·누락 조사 |
| `REGISTERED` | handle state가 REGISTERED이고 active name이 server에 존재 | 정상 관찰 |
| `RECOVERY_REQUIRED` | rollback/cleanup 뒤 handle이 이름을 계속 추적 | 즉시 alert, recovery handle의 `close()` 재시도 |
| `CLOSING` | cleanup attempt가 진행 중 | configured seconds가 지난 뒤 alert |
| `CLOSED` | handle이 추적하는 이름이 없고 state가 CLOSED | back/provider 비소유권을 별도 확인 |

`DISABLED`는 configuration identity 없이 JMX query 결과만 보고 선택할 수 없다.
`CLOSING` 지연 기준은 양수 초로 application별 사전 설정한다.

## Rollout과 canary

Rollout 전에 base/head/tree SHA, artifact identity, configuration identity, canary
대상, query·window·threshold·result를 JSON 템플릿에 기록한다. 비동기 threshold는
application별 traffic과 remote SLO를 기준으로 rollout 전에 정한다. 공통 default를
사후에 맞추지 않는다.

Logical counter의 `statisticsScope`는 `NEAR_JCACHE_WRAPPER_V1`이다.
`supportedOperations` 밖의 `loadAll`, `invoke`, `invokeAll`, `SuspendNearJCache`를
counter에 포함했다고 해석하지 않는다. `isFrontEvictionObservationSupported`,
`isBulkRemovalCountSupported`, `isBackWriteCompletionIncluded`가 false이면 그 사건이
없다는 뜻이 아니라 관찰 capability가 없다는 뜻이다.

## 비동기 completion inventory

API 성공은 caller-visible acceptance다. Remote write 결과는 각
`BackCacheWriteCompletion`의 `operationId`를 stable correlation key로 사용하고
`operation`을 diagnostic label로 기록한다. 전역 zero-loss drain API는 없으므로
migration 전에 새 admission을 중단하고 outstanding operation inventory가 모두 terminal
상태가 될 때까지 기다린다.

## Migration과 rollback

동기 migration 순서는 다음과 같다.

1. old registration handle을 `close()`한다.
2. old `NearJCache`를 `close()`한다.
3. replacement cache를 생성하고 새 ID로 등록한다.

`statistics.clear()`는 counter generation만 교체하며 data를 지우지 않는다.
`nearCache.clear()`는 해당 wrapper의 front와 back data를 모두 지운다. Rollback은
기록된 `rollbackIdentity`로 artifact/configuration을 되돌리고 같은 classifier를 다시
수행한다.

## Collision, recovery, stale owner

Handle이 열려 있는 동안 exact `ObjectName` namespace를 독점한다. Collision이 발생하면
기존 MBean owner와 rollout identity를 조사하며 임의 unregister를 하지 않는다. Ownership
token은 stale owner replacement를 줄이는 best-effort 검사이고 JMX의 atomic CAS가 아니다.
`RECOVERY_REQUIRED` 예외의 recovery handle은 현재 process에서 즉시 재시도하고, 직렬화된
예외의 immutable remaining name은 진단 자료로만 사용한다.

Registration과 `NearJCache.close()`는 caller-owned `MBeanServer`, back cache, cache
manager, provider를 닫지 않는다. Cleanup 완료 후 active name 부재, back/provider 생존,
inventory terminal 상태를 각각 증거로 남긴다.

## ID와 보안

`managerId`와 `cacheId`는 1..256자이며 blank, 앞뒤 whitespace, ISO control character를
허용하지 않는다. Unicode normalization은 하지 않는다. ID는 `ObjectName`과 recovery
exception에 노출되므로 credential, access token, PII를 넣지 않는다.

## 사용 API

Kotlin은 `registerMBeans`, Java는
`NearJCacheMBeans.registerMBeans(nearCache, server, managerId, cacheId)`를 사용한다.
JMX client는 `NearJCacheConfigurationMXBean`, `NearJCacheTierStatisticsMXBean` proxy를
사용한다. Template owner와 reviewer는 cleanup evidence까지 확인한다.

## 근거

- [`NearJCache.kt`](../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCache.kt)
- [`NearJCacheMBeanRegistration.kt`](../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheMBeanRegistration.kt)
- [`NearJCacheMBeanLifecycleTest.kt`](../../cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheMBeanLifecycleTest.kt)
- [`NearJCacheDocumentationTest.kt`](../../cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheDocumentationTest.kt)
