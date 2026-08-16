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

Issue #1369 bulk policy canary는 다음 evidence가 모두 준비된 뒤 시작한다.

- configuration의 `managementEnabled`와 `statisticsEnabled`가 모두 true다.
- `registerMBeans`를 호출했고 configuration/statistics `ObjectName`을 기록했다.
- `canaryTarget`, `query`, 동일 traffic `window`, 사전 `threshold`, observed `result`,
  `rollbackIdentity`를 rollout 전에 채웠다.
- Configuration MXBean의 `bulkFrontPopulationPolicy`가 `BYPASS_FRONT` 또는
  `POPULATE_IF_AT_MOST`이고 `bulkFrontPopulationMaximumEntryCount`가 의도한 값이다.

하나라도 누락되면 canary를 진행하지 않는다. 배포 전후 같은 traffic window에서
Statistics MXBean의 `FrontHits`, `FrontMisses`, `BackHits`, `BackMisses`,
`AverageGetTime`과
외부 back read load를 비교한다. Bypass 원인만 분리하는 전용 counter는 없으므로 이
관측만으로 back load 변화의 원인을 단정하지 않는다.

각 query는 rollout 전에 비교 연산자, 허용 방향, `threshold`를 고정한다. 판정식은
`observed result`가 모든 사전 threshold를 만족하는지 확인하는 AND 조건이다. 하나라도
초과하거나 evidence가 누락되면 rollout을 중단하고 아래 정상 rollback을 실행한다.
배포 결과를 본 뒤 threshold를 바꾸거나 실패 항목을 평균으로 상쇄하지 않는다.

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

정상 replacement와 rollback은 다음 순서로 수행한다.

1. 현재 wrapper로 들어오는 새 admission을 중단한다.
2. 비동기 write가 있으면 outstanding operation inventory가 모두 terminal 상태가 될 때까지 drain한다. 비동기 write가 없으면 빈 inventory를 증거로 남긴다.
3. 더 작은 `PopulateIfAtMost(n)` 또는 `BypassFront`로 replacement wrapper를 만들고 새 ID로 MBean을 등록한다.
4. Replacement Configuration MXBean의 정책·상한과 사전 query를 확인한다.
5. Traffic을 replacement wrapper로 전환한다.
6. old registration handle과 old `NearJCache`를 차례로 `close()`한다. 공유 back cache와 provider는 닫지 않는다.
7. 사후 classifier, 동일 window metric, active name 부재, back/provider 생존을 기록한다.

`statistics.clear()`는 counter generation만 교체하며 data를 지우지 않는다.
`nearCache.clear()`는 해당 wrapper의 front와 back data를 모두 지운다. Rollback은
같은 config 객체를 hot-reload하지 않는다. 새 wrapper identity를 `rollbackIdentity`에
기록하며, 위 handover sequence가 끝나기 전에는 rollback 성공으로 판정하지 않는다.

Issue #1369 이전 artifact는 무제한 batch population을 복원하므로 일반 rollback
대상에서 제외한다. 해당 artifact는 break-glass 절차로만 사용할 수 있다. 이때 사용
시간, front heap cap, traffic 제한, 책임자와 종료 시각을 먼저 기록하고 즉시
forward-fix를 시작한다. 제한이나 종료 시각이 없으면 이전 artifact를 배포하지 않는다.

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
