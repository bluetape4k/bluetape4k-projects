# Issue #1351 NearJCache management 구현 검토

## 범위와 판정

- Base: `513f70e785ea6975fc150844b6b8f23b9238031c`
- 검토 head: `a559a3a23dc619a471d7df6c24716ac3e9ad667d`
- 범위: `NearJCache` configuration/statistics recorder, explicit JMX registrar와 lifecycle,
  public ABI, EN/KO 문서, 운영 template, JMH 증거
- 결과: **INLINE CLEAR**, P0 0건, P1 0건
- Architecture status: **WATCH**

사용자는 단일 개발자 workflow의 인라인 검토를 명시적으로 선택했다. 따라서 이 문서는 독립
`code-reviewer`/`architect` 승인으로 간주하지 않는다. 같은 체크리스트를 여섯 관점으로 직접
검증한 기록이며, 병합은 exact PR head의 CI와 별도 fresh 사용자 승인을 다시 요구한다.

## Spec acceptance mapping

| Spec 계약 | Production symbol | 주요 test | 문서·운영·성능 증거 | 결과 |
|---|---|---|---|---|
| §5 configuration snapshot/type source/flags | `nearJCacheConfigurationSnapshot`, `NearJCacheConfigurationSnapshot` | `NearJCacheConfigurationSnapshotTest`의 actual/supplied/back/unresolved pair와 runtime failure test | README/manual의 immutable configuration 설명 | PASS |
| §6.1 logical/tier/capability | `NearJCacheStatisticsMXBean`, `NearJCacheTierStatisticsMXBean` | `NearJCacheStatisticsMXBeanTest`, `NearJCacheOperationStatisticsTest` | capability matrix의 wrapper scope와 unsupported 표 | PASS |
| §6.2 operation matrix | `NearJCache.get/getAll/put*/remove*/getAnd*` | `NearJCacheOperationStatisticsTest`의 read, bulk, mutation, compound, failure test | README/manual의 caller-visible 통계 경계 | PASS |
| §6.3 generation reset | `ActiveNearJCacheStatisticsRecorder.clear/current` | `operation은 시작 때 얻은 generation에만 기록한다`, `clear와 concurrent update가 서로 다른 generation을 섞지 않는다` | statistics clear와 data clear 분리 예제 | PASS |
| §6.4 disabled 비용 | `NoOpNearJCacheStatisticsRecorder`, disabled `NearJCache.get` | counting fake clock test | baseline/candidate JMH 10개 hard-gate key 전부 PASS | PASS |
| §6.5 unsupported eviction | `isFrontEvictionObservationSupported=false`, eviction getter 0 | statistics capability test | capability matrix | PASS |
| §7.1 explicit API/security | `NearJCache.registerMBeans`, Java `NearJCacheMBeans` facade | ID validation/quoting, exact Java descriptor test | operations guide의 non-secret stable ID 규칙 | PASS |
| §7.2 flag matrix/live state | `NearJCacheMBeanRegistrationState`, `activeObjectNames` | four flag combinations, immutable Java snapshot test | lifecycle classifier/template | PASS |
| §7.3 collision/rollback/close | `DefaultNearJCacheMBeanRegistration`, `NearJCache.close` | collision, rollback recovery, foreign replacement, reentry, concurrent close, retry test | collision/recovery/stale-owner runbook | PASS |
| §8 ABI | 기존 constructor/mutator와 additive interfaces | reflection, Java consumer, legacy/current serialization test | public KDoc와 EN/KO usage | PASS |
| §9 failure modes | registrar/recorder/lifecycle failure branches | targeted negative/concurrency test 전체 | `RECOVERY_REQUIRED` alert와 retry 절차 | PASS |
| §10 complete validation | cache-core 및 benchmark tasks | `653 passing` full module | detekt, Kover XML, benchmark compile, comparator | PASS |
| §11 docs/runbook | README/manual/matrix/operations/template | `NearJCacheDocumentationTest` 4개 계약 | EN/KO token parity와 executable JMX proxy example | PASS |
| §12 issue acceptance | 위 production/test 조합 | 위 acceptance test | 위 문서와 raw evidence | PASS |
| §13 DoD/lesson | 이 review와 lesson | final local proof | `docs/lessons/...` | PASS |
| §14 stacked train/rollout | operations template와 branch metadata | JSON parse/documentation test | base `fix/1348-lettuce-entryprocessor-atomicity`, head `feat/1351-nearcache-management` | PASS |

## 여섯 관점 검토

| 관점 | 판정 | 근거 | 잔여 위험 |
|---|---|---|---|
| Performance | CLEAR | disabled path는 clock/counter update를 하지 않고, 최종 JMH에서 single 5개와 concurrency 5개가 모두 95% throughput 및 allocation uncertainty budget을 통과했다. 최저 ratio는 4-thread 96.23%다. | active statistics의 약 10k ops/ms 고병렬 처리량은 관찰값이며 production sizing 근거가 아니다. |
| Stability | CLEAR | generation capture, partial rollback recovery, pending registration drain, concurrent close 결과 공유, resource별 retry가 결정적 test로 고정됐다. clean full module 653 test가 통과했다. | caller-owned `MBeanServer`가 무기한 block하면 production close도 block할 수 있다. library가 임의 timeout을 강제하지 않는 설계 선택이다. |
| Security/privacy | WATCH | ID는 길이/blank/control/whitespace를 검증하고 `ObjectName.quote`를 사용한다. collision/foreign replacement를 삭제하지 않고 credential·PII 사용 금지를 KDoc/runbook에 명시한다. | JMX에는 descriptor token 비교와 unregister의 원자 CAS가 없다. handle lifetime 동안 exact ObjectName exclusive namespace를 caller가 보장해야 한다. |
| Operator/Ops | CLEAR | `REGISTERED`, `RECOVERY_REQUIRED`, `CLOSING`, `CLOSED`와 configured/live 분류, alert, retry, cleanup inventory가 guide/template에 있다. `library_pr_only`는 canary/deploy/rollback을 N/A로 명시한다. | 실제 downstream rollout은 template의 양수 threshold와 query/window/result를 채우기 전 완료로 처리하면 안 된다. |
| Developer/API | CLEAR | 기존 constructor와 `add*` ABI를 보존하고 Java static facade는 exact descriptor 하나만 제공한다. public collection/array는 defensive copy다. | runtime statistics/management toggle은 지원하지 않으며 새 wrapper를 생성해야 한다. |
| Caller/user | CLEAR | logical/tier count, unsupported metric, cache/statistics clear, async caller success와 remote completion을 문서와 executable example에서 분리했다. | JMX-only inventory는 `DISABLED`와 `NOT_REGISTERED`를 구분하지 못하므로 application health state가 필요하다. |

## 검증 증거

- `repo-test-summary -- ./gradlew :bluetape4k-cache-core:cleanTest :bluetape4k-cache-core:test --no-build-cache`: `653 passing`, `BUILD SUCCESSFUL`
- compile, test compile, detekt, Kover XML, Lettuce benchmark compile: `BUILD SUCCESSFUL`
- Kover XML의 management package match: 52
- benchmark source SHA-256: `910dd0f209af0f34fa2479f60a4ab15f89bb5bb6c349d2de265de438cd4c9fb3`
- baseline JAR SHA-256: `c2949db3a166ce008f4bce64fe6fdb68cf4fb4ee5e6c9eab722172c34adcf5d8`
- candidate JAR SHA-256: `7c94c3d226fc855fbfb8bbceb888a3266b321a98865bf69edd72a96202211118`
- single-thread 및 disabled concurrency comparator: PASS
- operations template JSON parse와 `git diff --check`: PASS

## Disposition

- P0/P1: 없음
- accepted P2: JMX token-check/unregister 비원자성. exclusive namespace 계약과 WATCH를 유지한다.
- 독립 리뷰: 사용자 선택에 따라 미실행. 이 inline review를 독립 승인으로 표시하지 않는다.
- merge: fresh exact-head 승인 전 금지

## Writer DoD

- SPW-01 PASS: 독자는 NearJCache maintainer와 operator이고, 목적은 acceptance/review/lesson 증거 보존이다.
- SPW-02 PASS: review는 scope, mapping, severity, 관점별 판정, 검증, gap을 포함하고 lesson은 실제 구현 중 확인한 교훈만 기록한다.
- SPW-03 PASS: 한국어 기술 문체를 문장 단위로 다시 읽었고 code token, 상태명, SHA-256, benchmark 수치를 바꾸지 않았다.
- SPW-04 PASS: spec §5–§14, production symbol, test 이름, raw JMH와 문서의 수치·한계를 대조했다.
- SPW-05 PASS: heading/table/list 렌더링, `git diff --check`, JSON parse 후 최종 문서를 다시 읽었다.
