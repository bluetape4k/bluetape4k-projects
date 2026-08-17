# Issue #1369 NearJCache bounded bulk front population 교훈

## 맥락

기존 blocking `NearJCache.getAll()`은 back cache에서 읽은 항목을 front cache에 제한 없이 저장했다.
호출자가 큰 key 집합을 반복해서 조회하면 반환 결과는 정확하더라도 front residency가 빠르게 증가할 수
있었다. 이 변경의 보안 경계는 요청 key 수나 value byte 크기가 아니라, 실제 back cache hit 결과인
`backValues.size`다.

Issue #1369는 Epic #1408 stacked PR train의 첫 변경이다. 이 PR은 bulk front population 정책만 소유하며
`clear`, provider lifecycle, `SuspendNearJCache` 동작은 바꾸지 않는다. 후속 Issue #1368은 이 PR의 exact
head와 CI·review가 수렴하기 전에는 시작하지 않는다.

## 결정

기본 정책은 `BulkFrontPopulationPolicy.BypassFront`로 정했다. `getAll()`은 front hit와 back hit를 합친
정확한 결과를 그대로 반환하지만, back hit를 front에 추가 저장하지 않는다. bounded opt-in인
`BulkFrontPopulationPolicy.PopulateIfAtMost(maximumEntryCount)`는 `backValues.size`가 상한 이하일 때만
한 번의 `putAll`을 실행한다. 상한을 넘으면 일부 항목만 저장하지 않고 전체 population을 건너뛴다.

single-key `get()`은 이 정책의 적용 대상이 아니다. 따라서 기본 `BypassFront`에서는 같은 bulk read를
반복할 때 back read load가 증가할 수 있다. 기존 직렬화 stream에서 정책이 없거나 `null`인 경우도
무제한 동작을 복원하지 않고 `BypassFront`로 복구한다.

호환성은 기존 constructor와 Kotlin synthetic ABI를 명시적으로 보존하는 방식으로 고정했다. public
no-arg 및 5/6/7-인자 constructor, 이전·현재 synthetic constructor, `copy`와 `copy$default` descriptor를
pre-#1369 compiled consumer와 reflection test로 검증했다. 이전 5/6-field copy 경로는 새 정책을
`BypassFront`로 초기화한다.

운영 metadata는 subtype 이름에 의존하지 않는 `BYPASS_FRONT`와 `POPULATE_IF_AT_MOST` stable token,
그리고 `bulkFrontPopulationMaximumEntryCount`를 read-only MXBean attribute로 노출한다. 상한 `0`은
bounded zero가 아니라 `BypassFront`임을 뜻한다.

## 결과

runtime은 반환 map과 logical/tier statistics 계산 위치를 유지하고, back hit를 얻은 뒤 policy와 mutation
epoch를 같은 guard 안에서 판정한다. 기본 정책, 상한과 같은 크기, 상한 초과, 일부 hit/miss, 1 MB value,
front/back failure, cancellation, concurrent mutation race를 회귀 test로 고정했다.

README와 manual의 영어·한국어 쌍, Lettuce/Hazelcast 실제 DSL 예제, capability matrix, 운영 runbook과 JSON
template을 함께 갱신했다. 카나리는 사전에 정한 threshold를 AND로 판정하며 evidence 누락이나 단일 실패도
중단과 정상 rollback을 요구한다. 정상 rollback은 새 bounded 또는 `BypassFront` wrapper로 handover하고,
pre-#1369 artifact 복원은 time-bound break-glass로만 허용한다.

동일한 committed JMH harness와 환경에서 baseline/candidate 6개 profile, 31개 comparison row가 모두
통과했다. throughput ratio의 전체 범위는 `0.9889508759282967`부터 `1.774692209490313`까지였고,
allocation 허용 상한 대비 최소 여유는 `0.03450067306687288 B/op`였다. 이 결과는 95% throughput gate와
`baseline allocation + max(0.001, baseline scoreError + candidate scoreError)` allocation gate를 통과했다는
뜻이며, 일반적인 성능 향상을 주장하는 자료는 아니다.

## 검증 증거

- measurement commit/tree: `027ee9f675015bdd6d6e7be1490ef96c551e0a85` /
  `4bde98f4e408d456d6159a40723837893045f677`
- raw evidence commit: `9b1737f73a562e1e0b0feec3775cce79ec8a309f`
- benchmark source SHA-256: `42efaf18c3ab37dec9861ecf032d1f91c10741c22fce3662d550e61260c397ab`
- candidate JMH JAR SHA-256: `de0f58a05953fa62ea83db817e80a59667152cf21290702fe95aaf8b20426900`
- runtime RED: default/bounded 계약을 추가한 뒤 예상된 4개 failure를 확인했다.
- 단계별 GREEN: policy/ABI 18개, runtime 65개, management metadata 40개, documentation 5개 test가 통과했다.
- 최종 `:bluetape4k-cache-core:test`: 59 suites, 680 tests, failure/error/skipped 0.
- 최종 `:bluetape4k-cache-core:detekt`: `BUILD SUCCESSFUL`.
- JMH: path 21 rows와 contention 1/2/4/8/16-thread 각 2 rows, 총 31 rows가 모두 threshold를 통과했다.
- provider Testcontainers는 factory signature와 provider 동작을 바꾸지 않아 실행 범위에서 제외했다.

## 실패 또는 예상과 달랐던 점

문서 첫 검토에서는 한국어 번역투와 영문 용어 혼용, 카나리 판정식 누락, rollback handover 순서 누락,
locale parity test의 약한 heading 검증, JUnit assertion 혼용, 일부 문서에 한정된 stale 문구 검사가
발견됐다. 한국어 제목과 용어를 다듬고, code·숫자·heading level/order·stable token occurrence parity를
실행 test로 고정했다. 카나리 AND 판정식과 admission stop부터 old wrapper close까지의 순서도 runbook과
JSON template에 함께 반영한 뒤 writer/spec 재검토가 `CLEAR`로 수렴했다.

최종 운영 검토에서는 replacement가 old wrapper와 front cache를 공유하면 old `close()`가 replacement
front까지 닫을 수 있다는 P1이 발견됐다. Replacement의 별도 front identity와 비공유 증거를 traffic
전환 전 필수 조건으로 만들고, old front close와 replacement front open을 사후 증거로 추가했다. 같은
검토의 P2에 따라 canary template을 query별 comparator·direction·threshold·result 구조로 바꾸고,
break-glass에 시작 시각·최대 사용 시간·승인자·승인 시각을 추가했다.
재검토에서는 첫 query만 확인하던 regex와 사전 `observedResult` 기록 문구를 다시 수정했다. 실행 test는
6개 ID, query별 필수 필드, 동일 window, 중복 부재와 결손·중복 negative fixture를 검사한다.
`observedResult`와 `passed`는 traffic window가 끝난 뒤 최종 AND 판정 전에 기록한다.

JMH가 생성한 JSON은 EOF에 불필요한 빈 줄 두 개를 남겨 `git diff --check`가 실패했다. JSON 의미를
바꾸지 않고 마지막 newline 하나로 정규화한 뒤 raw evidence commit을 다시 고정했다. 비교 실행의 임시
파일 삭제 명령은 안전 guard에 거부됐으므로, 새 `mktemp -d` 내부에만 임시 결과를 쓰고 성공한 artifact만
`mv`하는 방식으로 바꿨다. benchmark는 다시 실행하지 않았다.

## 다음 변경을 위한 guard

- bulk 결과 정확성과 front residency를 별도 계약과 metric으로 검증한다.
- bounded 판정에는 `keys.size`가 아니라 실제 `backValues.size`를 사용한다.
- oversized 결과는 일부 population 없이 all-or-nothing으로 건너뛴다.
- benchmark source SHA, JMH JAR SHA, measurement commit/tree, raw evidence commit을 분리해 보존한다.
- baseline/candidate는 environment, toolchain, profile, fixture invariant가 완전히 같을 때만 비교한다.
- 한 comparison row라도 실패하면 candidate만 다시 측정하지 않는다. 환경 drift나 flake가 의심되면
  plan을 다시 승인받고 baseline/candidate 전체 pair를 새 profile로 수집한다.
- 운영 rollback은 admission 중단, outstanding drain, replacement 등록과 preflight, traffic 전환, old
  wrapper close, 사후 evidence 기록 순서를 지킨다. Replacement는 old wrapper와 별도 front를 소유하며,
  back cache와 provider ownership은 이전하지 않는다.
- #1368은 #1369 PR의 exact head CI와 review blocker가 모두 해소된 뒤 이 branch 위에 쌓는다.
