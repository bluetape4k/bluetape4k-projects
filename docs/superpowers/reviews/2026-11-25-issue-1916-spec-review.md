# #1916 Redisson 재시도 지연 설계 검토

## 판정

**Inline verdict: PASS; P0=0, P1=0.** 현재 명세 SHA-256 `2950ec6b4d11a9c83b7e52ab65ea30a9cc6867ca631aeb11008a25f8b177c66e`에 대해 주 세션이 수행한 inline fallback review다. 독립 리뷰어의 검토 완료나 독립 attestation으로 간주하지 않는다. 수정된 P2 발견은 모두 명세에 반영했으며, 미해결 P2/P3는 0건이다.

- 대상 명세: `docs/superpowers/specs/2026-11-25-issue-1916-redisson-retry-delay-contract.md`
- 기준 HEAD: `28ed92559d7f42bf27f20902dd545833c4c1d58e`
- 검토 시 브랜치: `feat/issue-1916-catalog-contracts`
- 현재 범위: 설계와 리뷰 문서만 변경했다. 제품 코드 및 테스트는 수정하거나 실행하지 않았다.
- GitHub 확인: 이슈 [#1916](https://github.com/bluetape4k/bluetape4k-projects/issues/1916)은 OPEN, assignee `debop`, milestone `2.2.0`이다. 이슈 범위에는 중앙 Catalog 조사, deprecated API 대체, 7-Tier, KDoc, 테스트가 포함된다.

## 리뷰 출처와 한계

현재 명세 해시에 대한 독립 리뷰 시도는 실행 환경 오류로 usable verdict를 내지 못했다. 0.171.0 host 실행 파일 누락과 CUA host unavailable이 확인됐고, 0.172.0 재시도는 `agent thread limit reached`로 종료됐다. 앞선 해시의 독립 검토도 현재 명세 해시의 판정으로 재사용하지 않는다. workspace의 Independent Code Review Fallback에 따라 주 세션에서 6개 관점과 통합 검토를 다시 수행했다.

요청된 외부 모델/독립 reviewer provenance는 성립하지 않았다. workflow receipt는 미완료 상태이며, 이 기록만으로 실패한 native lane을 성공 처리하지 않는다. merge 전 lane 해소는 workflow helper가 요구하는 clean worktree와 HEAD에 추적된 inline report 조건을 별도로 충족해야 한다.

## 6개 관점 및 통합 검토

| 관점 | 판정 | 근거와 적용 범위 |
|---|---|---|
| Performance | PASS | [명세 62–67, 78행](../specs/2026-11-25-issue-1916-redisson-retry-delay-contract.md)은 제품 경로의 1초 `ConstantDelay`를 유지하고, 새 작업은 설정 파싱 테스트로 제한한다. 요청 경로에 계산이나 할당을 추가하지 않는다. |
| Stability | PASS | [명세 12–24, 62–67, 101–104행](../specs/2026-11-25-issue-1916-redisson-retry-delay-contract.md)은 고동시성 기본 설정의 지연/시도 횟수를 고정하고 파서 실패 시 테스트 실패를 유지한다. 테스트 YAML은 무시된 키가 기본 `EqualJitterDelay`를 남겼던 사실과 새 1.5초 고정 지연을 적용하는 범위를 명시한다. |
| Security | PASS | [명세 73행](../specs/2026-11-25-issue-1916-redisson-retry-delay-contract.md)의 정적 설정 파싱은 입력 경계나 인증 값을 추가하지 않는다. fixture의 현재 password는 `null`이며 새 비밀 값은 계획되지 않았다. |
| Operator/Ops | PASS | [명세 18–20, 74, 79, 83행](../specs/2026-11-25-issue-1916-redisson-retry-delay-contract.md)은 Catalog ref와 날짜를 기록하고, 1초 제품 설정과 test YAML의 1.5초 설정을 구분한다. `EqualJitterDelay`에서 `ConstantDelay`로의 변화는 test YAML에 한정된 효과로 공개한다. |
| Developer/API | PASS | [명세 21, 46, 56, 58, 84, 87, 90행](../specs/2026-11-25-issue-1916-redisson-retry-delay-contract.md)은 `DEFAULT_RETRY_INTERVAL_MILLIS`의 공개 이름/값, `writeRetryInterval`, 함수 시그니처, JVM ABI를 보존한다. deprecated Redisson 설정 이름과 Bluetape 자체 식별자를 구분한다. |
| User/caller | PASS | [명세 26–31, 47–48, 63–67, 85–86행](../specs/2026-11-25-issue-1916-redisson-retry-delay-contract.md)은 YAML의 실제 `singleServerConfig` 중첩, 1500ms 값, 파싱 assertion 및 Redis 비의존 테스트를 명시한다. |
| 통합 | PASS | 이슈 #1916, 고정 Catalog ref, 현재 Kotlin 구현, 테스트 fixture 및 Redisson 4.8.0 공식 소스를 대조했다. 새 dependency, 버전 변경, Catalog 수정, 공개 API 변경은 없다. |

## 발견 사항 및 조치

| 심각도 | 발견 | 증거 | 조치 및 상태 |
|---|---|---|---|
| P2 | 기존 YAML의 `retryInterval: 1500`은 Redisson 4.8.0 YAML 파서가 무시한다. 따라서 기존 test fixture의 유효 전략은 1.5초 고정 지연이 아니라 `BaseConfig` 기본 `EqualJitterDelay`다. 이를 “기존 지연 동작 보존”이라고 서술하면 잘못된 동작 보존 주장이 된다. | 고정 태그 [ConfigSupport.java](https://github.com/redisson/redisson/blob/redisson-4.8.0/redisson/src/main/java/org/redisson/config/ConfigSupport.java)의 `ignoredProperties`에 `retryInterval`이 포함되고, [BaseConfig.java](https://github.com/redisson/redisson/blob/redisson-4.8.0/redisson/src/main/java/org/redisson/config/BaseConfig.java)는 기본 `retryDelay`를 `EqualJitterDelay`로 초기화한다. 로컬 fixture는 `infra/redisson/src/test/resources/redisson.yaml:6–7`에 `retryAttempts: 3`, `retryInterval: 1500`을 둔다. | 명세를 고쳐 test YAML만 기본 jitter에서 `ConstantDelay(1500ms)`로 실제 적용하는 변경이라고 기록했다. 제품 코드의 고동시성 1초 고정 지연은 보존한다. **해결됨** |
| P2 | 검색 수용 기준이 넓으면 공개 상수 `DEFAULT_RETRY_INTERVAL_MILLIS`나 별도 API `writeRetryInterval`도 변경 대상으로 오해될 수 있었다. | 원본 구현 `RedissonConst.kt:64–65`, `RedissonClientSupport.kt:195–196`; 명세 수용 기준과 범위. | 상수 이름/값을 유지하고 두 Bluetape 식별자를 Redisson 설정 키·setter와 구분하도록 명시했다. 검색 기준도 deprecated Redisson API와 YAML 키로 좁혔다. **해결됨** |
| P2 | YAML 예시가 루트 설정처럼 보일 수 있어 실제 `singleServerConfig` 구조와 다르게 복사될 수 있었다. | 로컬 fixture `infra/redisson/src/test/resources/redisson.yaml:1–7`. | 예시를 `singleServerConfig` 아래에 중첩했다. **해결됨** |

이전 해시 리뷰의 추가 발견도 현재 명세에서 확인했다. `retryAttempts == 3`은 YAML 및 고동시성 테스트에 각각 포함하고([명세 64–65, 86행](../specs/2026-11-25-issue-1916-redisson-retry-delay-contract.md)), `calcDelay(0/1)`은 고정 지연 함수의 계산값만 검증하도록 제한했다([명세 22, 64, 86행](../specs/2026-11-25-issue-1916-redisson-retry-delay-contract.md)). 버전 근거에는 관찰 날짜와 `redisson-4.8.0` 불변 태그가 있다([명세 10, 110행](../specs/2026-11-25-issue-1916-redisson-retry-delay-contract.md)).

## 검증 증거와 미검증 항목

- `settings.gradle.kts`가 고정한 Catalog ref `c44ec6c47aa3a8dc3e862dbd7d041eb9d78355f3`의 `redisson`은 `4.8.0`이다. Projects가 쓰는 pinned ref를 근거로 삼았다.
- 2026-11-25에 공식 `/releases/latest`는 `redisson-4.8.0` 태그로 이동했다. [공식 릴리스](https://github.com/redisson/redisson/releases/tag/redisson-4.8.0), [최신 릴리스 경로](https://github.com/redisson/redisson/releases/latest).
- 고정 태그 [BaseConfig.java](https://github.com/redisson/redisson/blob/redisson-4.8.0/redisson/src/main/java/org/redisson/config/BaseConfig.java)는 `setRetryInterval`을 deprecated로 표시하고 `ConstantDelay`로 변환한다. [ConstantDelay.java](https://github.com/redisson/redisson/blob/redisson-4.8.0/redisson/src/main/java/org/redisson/config/ConstantDelay.java)는 모든 attempt에서 같은 delay를 반환한다.
- 로컬 구현 `RedissonClientSupport.kt:188–197`은 재시도 횟수 3과 1초 `ConstantDelay`를 설정한다. `RedissonConst.kt:62–65`는 공개 재시도 상수 이름/값을 정의한다. 이 설계 범위에서는 API/ABI를 변경하지 않는다.
- 현재 issue #1916은 OPEN이며 이슈 본문은 버전 조사, deprecated 대체, 7-Tier, KDoc 및 테스트를 요청한다.
- Gradle 테스트나 Detekt는 실행하지 않았다. 이는 코드 구현 전 설계 검토 단계다. YAML parser 동작을 고정하는 신규 Redis 비의존 테스트는 구현 단계의 필수 검증으로 남는다.
- 현재 spec/review 파일은 feature worktree에서 아직 untracked다. 따라서 helper의 tracked-report/clean-worktree 조건에 따른 formal `lane-resolve`는 수행하지 않았고 workflow completion을 주장하지 않는다.

## 승인 및 다음 게이트

- 승인 완료: Redisson `4.8.0` 유지, jitter로 전환하지 않고 고정 지연 방향을 선택한 설계 방향.
- 최종 명세 승인 대기: 위 해시 `2950ec6b4d11a9c83b7e52ab65ea30a9cc6867ca631aeb11008a25f8b177c66e`. 이 버전은 test YAML의 기존 값이 무시됐다는 사실과 이를 1.5초 고정 지연으로 적용하는 효과를 분명히 적는다.
- Step 2-R 결과: **PASS — inline fallback review**. 독립 리뷰 attestation이 아니며, 해결되지 않은 P0/P1은 없다.
- 전체 workflow 결과: **PENDING**. native reviewer lane 및 main verification receipt가 미완료이며, 정확한 명세 승인과 이후 구현 계획/테스트/코드 검증이 남아 있다.
- 다음 단계: 사용자가 현재 명세를 승인하면 구현 계획 단계로 진행한다. 코드 및 테스트는 그 승인 이후 변경한다.

## 문서 검증

| 항목 | 결과 | 증거 |
|---|---|---|
| 범위·기준 고정 | PASS | issue #1916 live readback, HEAD `28ed92559d7f42bf27f20902dd545833c4c1d58e`, 명세 SHA-256을 기록했다. |
| 6개 관점 및 통합 | PASS | 각 관점의 판정과 명세 근거, P2 findings의 증거와 disposition을 위 표에 기록했다. |
| 기술 사실·불확실성 | PASS | 공식 Redisson 4.8.0 source 및 실제 YAML parser ignored property 동작을 반영했다. |
| 한국어 기술 문체 | PASS | `audit-korean-terms.mjs`: 두 문서, findings=0. |
| Markdown·해시 재검증 | PASS | 두 문서의 행 끝 공백 0건, 코드 펜스 짝수, 상대 링크 정상, 리뷰의 명세 SHA 일치. |
