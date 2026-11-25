# #1916 Redisson 재시도 지연 구현 계획

> **구현자 지침:** 계획 승인 후 `$executing-plans`, `$test-driven-development`, `$bluetape-kotlin-patterns`를 적용하고 아래 순서대로 실행한다.

**목표:** 승인된 명세에 따라 Redisson 고동시성 설정의 1초 고정 지연을 보존하고, 테스트 YAML이 1.5초 `ConstantDelay`를 실제로 적용하도록 바꾸며, 공개 API/ABI와 Catalog를 유지한다.

**구성:** 제품 설정 코드는 이미 `retryDelay = ConstantDelay(1초)`를 사용하므로 동작 코드는 수정하지 않는다. Redis 연결 없는 단위 테스트로 YAML 파싱 결과와 고동시성 기본 설정을 검증하고, YAML fixture와 KDoc만 갱신한다.

**기술:** Kotlin/JVM, Redisson 4.8.0, JUnit 5, bluetape4k `shouldBeEqualTo`/`shouldBeInstanceOf`, Gradle Wrapper, `repo-test-summary`.

---

## 승인 기준과 현재 증거

- 저장소: `bluetape4k/bluetape4k-projects`
- 작업 브랜치: `feat/issue-1916-catalog-contracts`
- 기준 브랜치: `develop`
- 모듈 Gradle 경로: `:bluetape4k-infra-redisson`. `settings.gradle.kts`의 `includeModules("infra", withBaseDir = false)`와 루트 `projectName = "bluetape4k"`에서 도출했다.
- 승인된 명세: `docs/superpowers/specs/2026-11-25-issue-1916-redisson-retry-delay-contract.md`
- 승인된 명세 SHA-256: `2950ec6b4d11a9c83b7e52ab65ea30a9cc6867ca631aeb11008a25f8b177c66e`
- 현재 소스 근거: `infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/RedissonClientSupport.kt:188–197`; YAML fixture `infra/redisson/src/test/resources/redisson.yaml:1–7`; 기존 assertion 함수와 테스트 관례 `infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/RedissonClientSupportTest.kt`.
- Redisson YAML tag/`Duration` 근거는 승인된 명세의 공식 Redisson 4.8.0 `ConfigSupport.java`, `BaseConfig.java`, `ConstantDelay.java` 링크를 따른다.

## 변경 파일

| 파일 | 작업 |
|---|---|
| `infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/RedissonRetryDelayConfigTest.kt` | 새 Redis 비의존 테스트. YAML 파싱과 `applyHighConcurrencyDefaults` 재시도 계약을 확인한다. |
| `infra/redisson/src/test/resources/redisson.yaml` | `singleServerConfig.retryInterval: 1500`을 적용되는 1.5초 `retryDelay: ConstantDelay(PT1.5S)`로 바꾼다. 다른 fixture 설정은 보존한다. |
| `infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/RedissonClientSupport.kt` | 고동시성 설정 KDoc의 항목을 `retryInterval`에서 `retryDelay`/고정 지연으로 고친다. 함수와 구현은 그대로 둔다. |
| `infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/RedissonConst.kt` | `DEFAULT_RETRY_INTERVAL_MILLIS` 이름/값을 유지하고 KDoc만 고친다. |
| `docs/superpowers/specs/2026-11-25-issue-1916-redisson-retry-delay-contract.md` | 승인된 사양. 계획 승인 뒤 계획·검토 문서와 선행 커밋한다. |
| `docs/superpowers/reviews/2026-11-25-issue-1916-spec-review.md` | inline fallback 검토 근거를 보존한다. |
| `docs/superpowers/plans/2026-11-25-issue-1916-redisson-retry-delay-plan.md` | 이 실행 계획. |
| `docs/superpowers/reviews/2026-11-25-issue-1916-plan-review.md` | 6개 관점 통합 계획 리뷰. 독립 리뷰 실행이 불가능하면 inline fallback으로 명시한다. |
| `docs/review/2026-11-25-issue-1916-redisson-retry-delay-review.md` | 구현 diff의 7-Tier 및 6개 관점 통합 사전 PR 코드 검토. |
| `docs/lessons/2026-11-25-issue-1916-redisson-retry-delay.md` | 선택 근거, 파서 동작 발견, 검증 결과와 재발 방지 기준을 기록한다. |
| `docs/superpowers/index/2026-11.md` | 이번 명세, 계획, 검토, 교훈 문서를 월별 색인에 연결한다. |
| `docs/testlogs/2026-11.md` | 대상/모듈 테스트와 Detekt의 실제 명령 및 결과를 기록한다. |

중앙 Catalog, Gradle 설정, 공개 선언/시그니처, README, `RedissonCacheConfig.writeRetryInterval`은 수정하지 않는다. 모듈 추가나 의존성 변경이 없어 자동 등록·Catalog 생성 규칙 검사는 적용되지 않는다.

## 사양 수용 기준 추적

| 승인 명세 기준 | 실행 작업 | 성공 증거 |
|---|---|---|
| Redisson 4.8.0 최신 릴리스 관찰일과 Catalog ref를 기록 | 작업 0b/4/6 | 이슈 본문과 교훈 문서에 근거 기록, Catalog diff 없음 |
| deprecated Redisson KDoc/YAML 설정 제거, Bluetape 식별자 보존 | 작업 3/4/5 | KDoc·YAML 갱신, 검색 결과 및 상수/API diff 확인 |
| 고동시성 1초 고정 지연 유지 | 작업 1/4 | `retryAttempts`, `ConstantDelay`, 1000ms assertion PASS |
| 무시된 fixture 키가 기본 `EqualJitterDelay`를 남겼다는 점과 새 YAML 1.5초 고정 지연 적용 | 작업 0b/1/2/6 | 이슈·교훈 문서 기록, YAML 테스트 1500ms assertion PASS |
| `calcDelay(0/1)`의 1500ms와 YAML `retryAttempts == 3` 확인 | 작업 1/2 | YAML 테스트 assertion PASS |
| YAML tag 또는 `Duration` 파싱 실패 시 실패를 보존하고 수정 | 작업 2/4 | 실패 로그를 근거로 표현 수정, 같은 지연 assertion PASS |
| deprecated Redisson 설정 잔존 검색과 Bluetape 자체 이름 제외 | 작업 4 | 제한된 `rg` 검색 결과 없음; `DEFAULT_RETRY_INTERVAL_MILLIS`, `writeRetryInterval` 보존 |
| Redis 연결 없는 계약 테스트, 전체 모듈 테스트, Detekt, diff check | 작업 1/2/4/6 | 대상 및 모듈 테스트·정적 검사 결과를 testlog에 기록 |
| Kotlin 7-Tier 및 6관점 검토에서 P0/P1 0건 | 작업 5/7 | 추적 가능한 검토 문서와 모든 발견 사항 처리 기록 |
| 공개 API/ABI, Catalog, dependency 무변경 | 작업 4/5 | stage된 diff와 공개 선언 확인 |
| 이슈 #1916의 선택 근거와 구현 상태를 구체화 | 작업 0b/7 | 기존 본문 보존, 추가 절과 GitHub 이슈 메타데이터 재조회 |
| 한국어 KDoc, PR 템플릿/이슈 연결/메타데이터, 테스트 기록과 교훈·색인 | 작업 3/6/7 | 관련 산출물과 GitHub PR 메타데이터·본문 재조회 |

## 실행 순서

### 작업 0: 승인된 사양과 계획을 구현 전에 커밋

**파일:** 승인 spec, spec review, 이 plan, plan review.

- [ ] plan과 통합 계획 리뷰 승인 및 `SPW-01`–`SPW-05` PASS를 확인한다.
- [ ] 명시된 문서만 스테이징하고 Lore 형식으로 커밋한다.

```bash
git add \
  docs/superpowers/specs/2026-11-25-issue-1916-redisson-retry-delay-contract.md \
  docs/superpowers/reviews/2026-11-25-issue-1916-spec-review.md \
  docs/superpowers/plans/2026-11-25-issue-1916-redisson-retry-delay-plan.md \
  docs/superpowers/reviews/2026-11-25-issue-1916-plan-review.md
git commit \
  -m "승인된 재시도 정책과 검증 범위를 구현 전에 고정한다" \
  -m "Constraint: Redisson Catalog 4.8.0과 제품 코드의 1초 고정 지연을 유지한다." \
  -m "Rejected: jitter 전략 전환 | 승인된 고정 지연 방향과 fixture 수용 기준에 맞지 않는다." \
  -m "Confidence: high" \
  -m "Scope-risk: narrow" \
  -m "Directive: YAML에서 무시되는 retryInterval 대신 retryDelay를 명시한다." \
  -m "Tested: 승인 spec/plan review, 6관점 통합 검토, SPW gate, 한국어 용어 감사." \
  -m "Not-tested: 구현 전이므로 Kotlin 테스트와 Detekt는 실행하지 않았다."
```

**성공 증거:** 승인 spec과 plan/review가 커밋에 포함되고 작업 트리가 clean이다. 코드 작업은 이 커밋 이후 시작한다.

### 작업 0b: 이슈 #1916에 승인된 조사 근거를 추가

**외부 대상:** `bluetape4k/bluetape4k-projects` issue `#1916`.

- [ ] 변경 직전에 issue 상태, 제목, assignee, milestone, labels, 본문을 다시 읽는다. 현재 본문은 일반 작업 범위만 설명하며 Redisson 4.8.0 판단을 담지 않는다.
- [ ] 기존 본문을 보존하고 `## Redisson 4.8.0 계약 조사` 절을 추가한다. 승인 명세에서 Catalog ref와 2026-11-25 릴리스 확인, deprecated setter와 YAML 파서 차이, fixture의 기존 유효 기본값, 제품 1초/fixture 1.5초 범위를 요약한다. 구현 후 7-Tier 결과는 검증 근거와 함께 갱신한다. 재조회 시 같은 절이 이미 생겼으면 중복 추가하지 말고 해당 절만 현재 근거에 맞춰 갱신한다.
- [ ] `gh issue edit 1916 --repo bluetape4k/bluetape4k-projects --body-file "$ISSUE_BODY_FILE"`로 갱신하고 `gh issue view 1916 --repo bluetape4k/bluetape4k-projects --json number,title,state,assignees,milestone,labels,body,url`로 본문과 메타데이터를 다시 읽는다.

```bash
ISSUE_BODY_FILE=$(mktemp)
gh issue view 1916 --repo bluetape4k/bluetape4k-projects --json body --jq .body > "$ISSUE_BODY_FILE"
cat >> "$ISSUE_BODY_FILE" <<'EOF'

## Redisson 4.8.0 계약 조사

- 2026-11-25 확인한 Projects Catalog ref `c44ec6c47aa3a8dc3e862dbd7d041eb9d78355f3`는 Redisson `4.8.0`을 고정하며, 같은 날짜에 공식 `/releases/latest`는 `redisson-4.8.0` 태그로 이동했다.
- Redisson 4.8.0의 `BaseConfig.setRetryInterval(int)`는 deprecated다. YAML 파서는 `retryInterval`을 무시하므로 현재 테스트 fixture의 유효 전략은 `ConstantDelay(1500ms)`가 아니라 기본 `EqualJitterDelay`다.
- `applyHighConcurrencyDefaults`의 1초 `ConstantDelay`는 유지한다. 테스트 fixture에는 `retryDelay: ConstantDelay(PT1.5S)`를 적용하고 Redis 연결 없이 파싱 결과와 `calcDelay(0/1)`을 검증한다.
- `DEFAULT_RETRY_INTERVAL_MILLIS`, `writeRetryInterval`, 공개 API/ABI, Catalog ref와 의존성 버전은 변경하지 않는다.

구현 후 테스트와 7-Tier 검토 결과는 검증한 명령과 발견 사항 처리 근거를 바탕으로 추가한다.
EOF
gh issue edit 1916 --repo bluetape4k/bluetape4k-projects --body-file "$ISSUE_BODY_FILE"
gh issue view 1916 --repo bluetape4k/bluetape4k-projects --json number,title,state,assignees,milestone,labels,body,url
rm "$ISSUE_BODY_FILE"
```

**성공 증거:** 기존 이슈 범위와 메타데이터가 보존되고 추가 절이 명세와 일치한다. 이슈가 닫혔거나 본문이 새로 변경되어 안전하게 반영할 수 없으면 수정하지 않고 상태를 재평가한다.

### 작업 1: Redis 비의존 회귀 테스트를 먼저 추가하고 RED 확인

**파일:** 새 `infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/RedissonRetryDelayConfigTest.kt`.

- [ ] 아래 테스트 파일을 추가한다. Redisson `Config`만 파싱하며 Redis client를 만들지 않는다.
- [ ] 대상 테스트만 실행한다. YAML 테스트는 기존 `retryInterval` 키가 무시되어 기본 `EqualJitterDelay`가 남으므로 타입 assertion에서 실패해야 한다. 고동시성 기본 설정 테스트는 현재 1초 `ConstantDelay` 계약을 통과해야 한다.

```kotlin
package io.bluetape4k.redis.redisson

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.redisson.config.Config
import org.redisson.config.ConstantDelay
import java.time.Duration

class RedissonRetryDelayConfigTest {

    @Test
    fun `parses fixed retry delay from yaml`() {
        val yaml = requireNotNull(javaClass.getResource("/redisson.yaml"))
        val config = Config.fromYAML(yaml)
        val singleServer = requireNotNull(config.singleServerConfig)

        singleServer.retryAttempts shouldBeEqualTo 3
        val delay = singleServer.retryDelay.shouldBeInstanceOf<ConstantDelay>()
        delay.calcDelay(0) shouldBeEqualTo Duration.ofMillis(1_500)
        delay.calcDelay(1) shouldBeEqualTo Duration.ofMillis(1_500)
    }

    @Test
    fun `keeps high concurrency defaults at one second fixed delay`() {
        val config = Config().applyHighConcurrencyDefaults()
        val singleServer = config.useSingleServer()

        singleServer.retryAttempts shouldBeEqualTo 3
        val delay = singleServer.retryDelay.shouldBeInstanceOf<ConstantDelay>()
        delay.calcDelay(0) shouldBeEqualTo Duration.ofMillis(1_000)
        delay.calcDelay(1) shouldBeEqualTo Duration.ofMillis(1_000)
    }
}
```

```bash
repo-test-summary -- ./gradlew :bluetape4k-infra-redisson:test \
  --tests "io.bluetape4k.redis.redisson.RedissonRetryDelayConfigTest"
```

**예상 증거:** 테스트 클래스 컴파일 성공, YAML 계약 테스트 실패(기대 타입 `ConstantDelay`, 실제 기본 `EqualJitterDelay`), 고동시성 테스트 통과. 타입 assertion 대신 파싱 오류가 나면 로그를 보존하고 원인을 확인한다.

### 작업 2: test YAML의 새 지연 전략 적용 및 GREEN 확인

**파일:** `infra/redisson/src/test/resources/redisson.yaml`.

- [ ] `singleServerConfig` 안의 `retryInterval: 1500`을 아래 설정으로 교체한다. `retryAttempts: 3`과 다른 fixture 항목은 그대로 둔다.

```yaml
singleServerConfig:
    retryAttempts: 3
    retryDelay: !<org.redisson.config.ConstantDelay> { delay: PT1.5S }
```

- [ ] 작업 1의 정확한 Gradle 명령을 다시 실행한다.

**성공 증거:** 대상 테스트 2건 PASS. YAML 테스트는 `retryAttempts == 3`, `ConstantDelay`, 인덱스 0/1에서 1500ms를 확인한다. 값을 낮추거나 테스트를 skip하지 않는다.

### 작업 3: 두 KDoc만 갱신

**파일:** `infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/RedissonClientSupport.kt`, `infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/RedissonConst.kt`.

- [ ] `applyHighConcurrencyDefaults` KDoc의 `retryInterval: 1초`를 `retryDelay: 1초 고정 지연 (ConstantDelay)`로 바꾼다.
- [ ] 상수 KDoc을 `/** Redisson 기본 재시도 고정 지연 (ms) */`로 바꾼다. `DEFAULT_RETRY_INTERVAL_MILLIS` 이름과 `1_000` 값은 보존한다.
- [ ] 작업 1의 대상 Gradle 테스트를 다시 실행한다.

**성공 증거:** 두 대상 테스트 PASS, 함수/상수 선언과 실행 코드는 그대로이고 KDoc만 바뀐다.

### 작업 4: 모듈 검증과 범위/API 점검

**파일:** 작업 1–3의 네 구현 파일.

- [ ] `repo-test-summary -- ./gradlew :bluetape4k-infra-redisson:test`로 모듈 테스트를 실행한다. 다른 모듈/워크트리의 Testcontainers 테스트와 병렬 실행하지 않는다.
- [ ] `./gradlew :bluetape4k-infra-redisson:detekt`를 실행한다.
- [ ] `rg -n 'setRetryInterval\(|\.retryInterval[[:space:]]*=|retryInterval' infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson infra/redisson/src/test/kotlin` 및 `rg -n '^[[:space:]]*retryInterval[[:space:]]*:' infra/redisson/src/test/resources/redisson.yaml` 검색 결과가 비어 있는지 확인한다. `rg`의 종료 코드 1은 정상이다. 대소문자 구분 검색이므로 Bluetape 식별자 `DEFAULT_RETRY_INTERVAL_MILLIS`와 `writeRetryInterval`은 대상이 아니다.
- [ ] `git add -- infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/RedissonClientSupport.kt infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/RedissonConst.kt infra/redisson/src/test/resources/redisson.yaml infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/RedissonRetryDelayConfigTest.kt`로 네 구현 파일만 stage한다.
- [ ] `git diff --cached --check`, `git diff --cached --stat`, `git diff --cached -- infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/RedissonClientSupport.kt infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/RedissonConst.kt infra/redisson/src/test/resources/redisson.yaml infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/RedissonRetryDelayConfigTest.kt`를 실행해 새 테스트 파일을 포함한 diff를 읽는다.

**성공 증거:** 모듈 테스트와 Detekt PASS, deprecated 설정 검색 결과 없음, staged diff check PASS, 변경 범위가 네 구현 파일에 한정되고 실행 코드/API/ABI/Catalog diff가 없다.

### 작업 5: 구현 diff의 7-Tier 및 사전 PR 코드 리뷰

**파일:** 구현 diff와 새 `docs/review/2026-11-25-issue-1916-redisson-retry-delay-review.md`.

- [ ] `$bluetape-kotlin-patterns`의 7-Tier 기준과 workflow Step 6-R의 Performance, Stability, Security, Operator/Ops, Developer/API, User/caller 6개 관점 및 통합 검토를 적용한다.
- [ ] 독립 코드 검토를 수행하고 정확한 diff/HEAD에 연결된 결과를 보존한다. 단독 개발자 lane의 인간 리뷰는 N/A로 처리하되 독립 리뷰를 대체하지 않는다. 독립 리뷰가 실행 불가능하면 workflow가 허용하는 fallback과 provenance를 기록하고 독립 검토 미완료 상태를 숨기지 않는다.
- [ ] P0/P1을 모두 수정하고 관련 테스트를 다시 실행한다. P2/P3는 수정하거나 근거를 들어 보류한다. P0/P1 미해결 또는 독립 리뷰 미완료이면 PR 생성 단계로 진행하지 않는다.
- [ ] 코드 리뷰 artifact에 범위/기준 SHA, 각 관점 판단, finding 근거·조치, 미검증 항목을 기록하고 SPW-01–SPW-05 writer gate를 통과시킨다.

**성공 증거:** 사전 PR 검토는 정확한 구현 diff에 연결되고 P0=0/P1=0이며 남은 발견 사항의 처리를 기록한다. README/CHANGELOG는 사용자 동작이나 공개 API가 바뀌지 않는다는 diff 근거로 N/A 처리한다.

### 작업 6: 테스트 기록, 교훈, Superpowers 색인 갱신

**파일:** `docs/testlogs/2026-11.md`, `docs/lessons/2026-11-25-issue-1916-redisson-retry-delay.md`, `docs/superpowers/index/2026-11.md`.

- [ ] 대상 테스트, 모듈 테스트, Detekt의 실제 명령/결과와 실행 날짜를 testlog에 추가한다. 실패/재시도/skip이 있으면 각각 구분하고 최종 성공 기록만 남겨 이전 실패를 지우지 않는다.
- [ ] 교훈 문서에 Redisson YAML 파서가 deprecated `retryInterval`을 무시한 사실, 테스트 fixture의 유효 전략 변화, 제품 1초 계약 보존, 검증 결과와 향후 `retryDelay` 사용 지침을 기록한다.
- [ ] 월별 Superpowers 색인에 명세, 명세 검토, 계획, 계획 검토, 구현 검토와 교훈 문서 링크를 기록한다.
- [ ] 변경된 한국어 문서를 다시 읽고 `node /Users/debop/.codex/skills/bluetape-writer/scripts/audit-korean-terms.mjs docs/superpowers/specs/2026-11-25-issue-1916-redisson-retry-delay-contract.md docs/superpowers/reviews/2026-11-25-issue-1916-spec-review.md docs/superpowers/plans/2026-11-25-issue-1916-redisson-retry-delay-plan.md docs/superpowers/reviews/2026-11-25-issue-1916-plan-review.md docs/review/2026-11-25-issue-1916-redisson-retry-delay-review.md docs/lessons/2026-11-25-issue-1916-redisson-retry-delay.md docs/superpowers/index/2026-11.md docs/testlogs/2026-11.md` 및 `git diff --check`를 실행한다. 결과를 각 문서/워크플로 evidence에 남긴다.
- [ ] 문서 산출물은 구현 커밋과 분리해 Lore 형식으로 커밋한다.

**성공 증거:** testlog에는 실행 결과만, 교훈 문서에는 재사용할 교훈만, 색인에는 실제 존재하는 문서 경로만 포함하고 관련 문서를 커밋한다.

### 작업 7: 지정 저장소/브랜치의 PR 생성 및 메타데이터 재조회

- [ ] PR 직전에 이슈 #1916의 상태·본문·assignee·milestone·labels를 다시 읽고 구현 완료 근거를 보완한다. 변경된 메타데이터는 현재값을 기준으로 삼는다.
- [ ] `git fetch origin develop` 후 `git merge-base --is-ancestor origin/develop HEAD`로 현재 base가 feature head의 조상인지 확인한다. 아니라면 최신 `origin/develop`을 반영하고 영향받은 테스트·리뷰를 다시 실행한다.
- [ ] `git push --set-upstream origin feat/issue-1916-catalog-contracts`로 검증된 최종 head를 게시한다.
- [ ] `.github/pull_request_template.md`의 모든 섹션을 유지하는 한국어 `PR_BODY_FILE`을 작성한다. 변경 이유/요약을 테스트 결과보다 먼저 쓰고, 적용/비적용 checkbox를 증거와 맞추며 끝에 `## DoD Status`를 둔다. `Closes #1916`을 포함한다.
- [ ] `PR_BODY_FILE=$(mktemp)`를 만들고 `.github/pull_request_template.md`를 복사한 뒤, 현재 테스트/리뷰/DoD 증거에 맞게 섹션과 checkbox를 채운다. 템플릿 밖의 추가 절은 마지막 `## DoD Status` 앞에 둔다.
- [ ] 승인된 계획 범위로 `bluetape4k/bluetape4k-projects` base `develop`, head `feat/issue-1916-catalog-contracts` PR을 만든다. 현재 issue assignee `debop`, milestone `2.2.0`, 관련 labels `documentation`, `management`, `test`, `dependencies`, `tech-debt`를 반영한다.

```bash
PR_URL=$(gh pr create \
  --repo bluetape4k/bluetape4k-projects \
  --base develop \
  --head feat/issue-1916-catalog-contracts \
  --title "Redisson 재시도 지연 설정 계약을 명확히 한다" \
  --assignee debop \
  --milestone "2.2.0" \
  --label documentation \
  --label management \
  --label test \
  --label dependencies \
  --label tech-debt \
  --body-file "$PR_BODY_FILE")
gh pr view "$PR_URL" --repo bluetape4k/bluetape4k-projects \
  --json number,title,body,assignees,labels,milestone,headRefOid,headRefName,baseRefName,closingIssuesReferences,url
```

- [ ] GitHub에서 PR을 다시 조회해 repository, base/head, head SHA, assignee, milestone, labels, `Closes #1916`, closing issue reference, 본문의 마지막 `## DoD Status`를 확인한다.

**성공 증거:** 생성된 PR URL과 현재 head의 메타데이터·본문 조회 결과가 일치한다. 이 단계의 사용자 승인은 PR 생성까지만 포함하며 merge는 포함하지 않는다.

### 작업 8: PR 후 리뷰/CI 수렴 및 merge-ready 보고

- [ ] 실제 PR diff와 정확한 head에 대해 6개 관점+통합 post-PR review를 실행하고, `REQUEST_CHANGES` 또는 P0/P1이면 구현·검증·리뷰 단계로 돌아간다.
- [ ] exact head를 기록한 뒤 `gh pr checks "$PR_URL"`을 실행하고, 다시 읽은 `headRefOid`가 동일한지 비교한다. CI 대기 중이면 PENDING 상태로 멈추고, skip/실패를 성공으로 기록하지 않는다.
- [ ] PR 상태·review decision·mergeability는 `gh pr view "$PR_URL" --json number,state,headRefOid,baseRefName,headRefName,reviews,reviewDecision,mergeable,mergeStateStatus,closingIssuesReferences`로 확인한다. review thread는 아래 GraphQL 조회에서 `isResolved`, 경로, 줄, 댓글을 확인한다. `pageInfo.hasNextPage`가 true이면 다음 page도 읽는다.

```bash
PR_NUMBER=$(gh pr view "$PR_URL" --json number --jq .number)
gh api graphql \
  -f owner=bluetape4k \
  -f name=bluetape4k-projects \
  -F number="$PR_NUMBER" \
  -f query='query($owner: String!, $name: String!, $number: Int!) { repository(owner: $owner, name: $name) { pullRequest(number: $number) { headRefOid reviewDecision mergeStateStatus reviewThreads(first: 100) { pageInfo { hasNextPage endCursor } nodes { isResolved comments(first: 100) { pageInfo { hasNextPage endCursor } nodes { author { login } body path line } } } } } } }' \
  --jq '.data.repository.pullRequest'
```
- [ ] PR body의 `## DoD Status`를 최신 결과에 맞춰 갱신하고 `gh pr view`로 다시 확인한다.
- [ ] `gno update`, `gno embed --collection bluetape4k-github`, `gno embed --collection bluetape4k-docs`, `gno search "bluetape4k-projects Redisson retryDelay issue 1916" -c bluetape4k-github`로 issue/PR과 명세·계획·교훈 색인을 갱신하고 대표 결과를 확인한다.
- [ ] merge-ready DoD를 작성한다. 정확한 head, 테스트, 검토, PR 메타데이터, GNO 결과를 포함하고 fresh merge approval 전에는 `PENDING`으로 보고한다.

**성공 증거:** PR이 최신 exact head에서 CI와 review gate를 통과했거나 외부 결과 대기로 명시적인 PENDING 상태다. merge는 별도 fresh approval 뒤에만 수행한다.

## 위험 예측과 대응

| 위험 | 신호 | 대응 | 되돌림/재실행 |
|---|---|---|---|
| YAML tag 또는 ISO `Duration` 표기 파싱 실패 | 테스트에서 파싱 예외, 타입/지연 불일치 | Redisson 4.8.0 고정 소스와 `ConfigSupport` 계약에 맞춰 표기를 수정하고 1500ms assertion은 유지한다. 테스트를 skip하지 않는다. | YAML만 수정해 대상 테스트를 다시 실행한다. API나 dependency 변경이 필요하면 계획으로 돌아간다. |
| test YAML 유효 전략 변경 | 기존에 무시되던 키 대신 1.5초 고정 지연이 적용되어 fixture 소비 테스트가 달라진다. | 이 효과는 승인 명세에 기록됐다. 사용처와 실패를 확인하되 제품 설정으로 범위를 넓히지 않는다. | 승인 설계와 맞지 않으면 fixture를 원복하고 명세를 수정해 재승인한다. |
| 모듈 전체 테스트의 Docker/Testcontainers 실패 | 컨테이너 시작 오류, 실패 또는 조건부 skip | 대상 Redis 비의존 테스트와 전체 테스트를 구분한다. macOS에서는 `colima status`, `docker context show`, `docker info`를 확인하고 건강한 Colima를 재시작하지 않는다. | 환경 원인을 진단한 뒤 실패한 모듈 태스크를 순차 재실행한다. 실패/skip을 통과로 보고하지 않는다. |
| Catalog나 제품 코드로 범위 확장 | staged diff에 Catalog, 공개 선언 또는 제품 지연 코드가 나타난다. | 작업을 멈추고 원인을 분류한다. 승인 없이 Catalog/version/API 범위를 넓히지 않는다. | 구현 diff를 승인된 네 경로로 축소하거나 설계 재승인을 받는다. |

## 적용하지 않는 검증과 이유

- README/CHANGELOG/다른 locale: 새 API나 제품 동작이 없고 KDoc과 test fixture만 바뀐다.
- coroutine, cancellation, lifecycle, race 테스트: 새 client나 resource를 만들지 않는 설정 파싱 테스트다.
- 성능 benchmark와 API/ABI dump: hot path/선언이 바뀌지 않는다. staged diff에서 공개 선언과 상수 값을 직접 확인한다.
- module registration과 Catalog 생성 검증: 기존 module이며 Catalog 버전을 바꾸지 않는다.
- 새 Redis/Testcontainers 테스트: 계약은 Config parsing만으로 검증한다. 기존 전체 모듈 테스트의 container coverage는 보존한다.

## 승인 전 자체 확인

- 이 계획 승인 전에는 코드, YAML, KDoc 또는 test source를 수정하지 않는다.
- 승인 명세 SHA-256은 `2950ec6b4d11a9c83b7e52ab65ea30a9cc6867ca631aeb11008a25f8b177c66e`다.
- 기존 테스트 YAML의 `retryInterval`은 무시되므로 새 `retryDelay`의 1.5초는 test fixture에 실제 적용된다. 제품 고동시성 설정은 1초 고정 지연을 유지한다.
- Step 2-R는 P0=0/P1=0 inline fallback verdict이며 독립 attestation이 아니다. 계획 리뷰에도 동일한 경계를 적용한다.
