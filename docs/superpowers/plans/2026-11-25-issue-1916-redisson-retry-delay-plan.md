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
- 모듈 Gradle 경로: `:bluetape4k-redisson`. 실행 중 `./gradlew projects`의 실제 출력에서 확인했다.
- 승인된 명세: `docs/superpowers/specs/2026-11-25-issue-1916-redisson-retry-delay-contract.md`
- 구현 전 사용자 승인 명세 SHA-256: `2950ec6b4d11a9c83b7e52ab65ea30a9cc6867ca631aeb11008a25f8b177c66e`
- 구현 중 사실 보정 및 검증 결과가 반영된 명세 SHA-256: `a5207398347059409a57e6203d192f736ebcf551938e37b76380db62c25d3f71`. `Config.fromYAML`의 실제 오류, 미지원 fixture 속성과 최종 검증 결과를 반영했다. 고동시성 1초 및 test YAML 1.5초 목표는 유지한다.
- 구현 전 승인 계획 SHA-256: `275839b139f07ed489e6e958cf599327d294f2e6e0062213132973d6c1aeabd2`. 실행 검증에서 드러난 Gradle 경로, Java getter 접근, YAML parser 동작을 아래 계획에 보정했다. 고동시성 1초 및 YAML 1.5초 계약은 유지한다.
- Redisson 4.8.0 `Config.fromYAML`은 기존 fixture의 deprecated/미지원 속성에서 null property 예외를 낸다. `redisson.yaml`의 미지원 `pingTimeout`, `reconnectionTimeout`, `failedAttempts`, `dnsMonitoring`, `useLinuxNativeEpoll`을 제거하고 `retryInterval`은 지원되는 `retryDelay`로 교체한다. 실제 parser를 통과하는 fixture에서 고정 지연 계약을 검증한다.
- `Config.singleServerConfig`의 Java getter는 protected다. 테스트에서는 현재 설정을 돌려주는 public `config.useSingleServer()`를 사용한다.
- 현재 소스 근거: `infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/RedissonClientSupport.kt:188–197`; YAML fixture `infra/redisson/src/test/resources/redisson.yaml:1–7`; 기존 assertion 함수와 테스트 관례 `infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/RedissonClientSupportTest.kt`.
- Redisson YAML tag/`Duration` 근거는 승인된 명세의 공식 Redisson 4.8.0 `ConfigSupport.java`, `BaseConfig.java`, `ConstantDelay.java` 링크를 따른다.
- 이번 이어서 작업 전 GNO `bluetape4k-github`, `bluetape4k-docs`, `bluetape4k-wiki`를 넓은 키워드와 좁힌 Redisson/issue 키워드로 각각 검색했으나 결과가 없었다. context-mode 대체 검색도 task-specific CI/PR 근거를 찾지 못했다. issue #1916의 최신 내용은 live GitHub에서 확인하며 GNO 결과를 권위 있는 상태로 간주하지 않는다.

## 변경 파일

| 파일 | 작업 |
|---|---|
| `infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/RedissonRetryDelayConfigTest.kt` | 새 Redis 비의존 테스트. YAML 파싱과 `applyHighConcurrencyDefaults` 재시도 계약을 확인한다. |
| `infra/redisson/src/test/resources/redisson.yaml` | deprecated `retryInterval`을 적용되는 1.5초 `retryDelay: ConstantDelay(PT1.5S)`로 바꾸고, Redisson 4.8.0에 없는 다섯 오래된 fixture 속성을 제거한다. 나머지 유효한 설정은 보존한다. |
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
| 기존 fixture가 Redisson 4.8.0에서 파싱되지 않는 점과 새 YAML 1.5초 고정 지연 적용 | 작업 0b/1/2/6 | 이슈·교훈 문서 기록, YAML 테스트 1500ms assertion PASS |
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

- [x] plan과 통합 계획 리뷰 승인 및 `SPW-01`–`SPW-05` PASS를 확인했다. 사용자가 구현 계획을 승인했고, 승인 문서는 아래 커밋으로 코드 변경 전에 저장했다.
- [x] 명시된 문서만 스테이징하고 Lore 형식으로 커밋했다. 구현 전 문서 커밋은 `9379b746d17f2c6ffed1a827dec296f12815033c`다.

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
  -m "Directive: YAML 파서가 처리하는 retryInterval 대신 retryDelay를 명시한다." \
  -m "Tested: 승인 spec/plan review, 6관점 통합 검토, SPW gate, 한국어 용어 감사." \
  -m "Not-tested: 구현 전이므로 Kotlin 테스트와 Detekt는 실행하지 않았다."
```

**성공 증거:** 승인 spec과 plan/review가 커밋에 포함되고 작업 트리가 clean이다. 코드 작업은 이 커밋 이후 시작한다.

### 작업 0b: 이슈 #1916에 승인된 조사 근거를 추가

**외부 대상:** `bluetape4k/bluetape4k-projects` issue `#1916`.

- [x] 변경 직전에 issue 상태, 제목, assignee, milestone, labels, 본문을 다시 읽는다. 현재 본문은 일반 작업 범위만 설명하며 Redisson 4.8.0 판단을 담지 않는다.
- [x] 기존 본문을 보존하고 `## Redisson 4.8.0 계약 조사` 절을 추가한다. 구현 근거는 Redisson 4.8.0 parser 오류와 미지원 fixture 키, 제품 1초/fixture 1.5초 범위를 포함한다. 구현 후 7-Tier 결과는 검증 근거와 함께 갱신한다.
- [x] issue 본문 수정 후 `gh issue view`로 원문 보존 여부와 본문·assignee·milestone·labels를 재조회했다.

```bash
ISSUE_BODY_FILE=$(mktemp)
gh issue view 1916 --repo bluetape4k/bluetape4k-projects --json body --jq .body > "$ISSUE_BODY_FILE"
cat >> "$ISSUE_BODY_FILE" <<'EOF'

## Redisson 4.8.0 계약 조사

- 2026-11-25 확인한 Projects Catalog ref `c44ec6c47aa3a8dc3e862dbd7d041eb9d78355f3`는 Redisson `4.8.0`을 고정하며, 같은 날짜에 공식 `/releases/latest`는 `redisson-4.8.0` 태그로 이동했다.
- Redisson 4.8.0의 `BaseConfig.setRetryInterval(int)`는 deprecated다. `ConfigSupport`는 `retryInterval`을 무시 목록에 두지만, 실행한 `Config.fromYAML`은 null property 예외로 중단된다. 추가로 `pingTimeout`, `reconnectionTimeout`, `failedAttempts`, `dnsMonitoring`, `useLinuxNativeEpoll`은 Redisson 4.8.0 JavaBean 속성이 아니다.
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

- [x] 아래 테스트 파일을 추가한다. Redisson `Config`만 파싱하며 Redis client를 만들지 않는다.
- [x] 대상 테스트로 RED를 확인했다. 기존 YAML parser는 `pingTimeout` 및 `retryInterval`의 null property 예외를 냈고, 고동시성 기본 설정 테스트는 통과했다.

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
        val singleServer = config.useSingleServer()

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
repo-test-summary -- ./gradlew :bluetape4k-redisson:test \
  --tests "io.bluetape4k.redis.redisson.RedissonRetryDelayConfigTest"
```

**예상 증거:** 테스트 클래스 컴파일 성공, YAML 계약 테스트 실패(기존 `retryInterval` 처리 중 YAML parser null property 예외), 고동시성 테스트 통과. parser가 멈춘 정확한 키와 Redisson 4.8.0 JavaBean 속성을 대조하고 미지원 fixture 키만 정리한다.

### 작업 2: test YAML의 새 지연 전략 적용 및 GREEN 확인

**파일:** `infra/redisson/src/test/resources/redisson.yaml`.

- [x] `singleServerConfig` 안의 `retryInterval: 1500`을 아래 설정으로 교체한다. `retryAttempts: 3`과 나머지 Redisson 4.8.0 유효 fixture 설정은 그대로 두고, 지원하지 않는 fixture 키는 제거했다.

```yaml
singleServerConfig:
    retryAttempts: 3
    retryDelay: !<org.redisson.config.ConstantDelay> { delay: PT1.5S }
```

- [x] 작업 1의 정확한 Gradle 명령을 다시 실행해 두 테스트 GREEN을 확인했다.

**성공 증거:** 대상 테스트 2건 PASS. YAML 테스트는 `retryAttempts == 3`, `ConstantDelay`, 인덱스 0/1에서 1500ms를 확인한다. 값을 낮추거나 테스트를 skip하지 않는다.

### 작업 3: 두 KDoc만 갱신

**파일:** `infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/RedissonClientSupport.kt`, `infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/RedissonConst.kt`.

- [x] `applyHighConcurrencyDefaults` KDoc의 `retryInterval: 1초`를 `retryDelay: 1초 고정 지연 (ConstantDelay)`로 바꾼다.
- [x] 상수 KDoc을 `/** Redisson 기본 재시도 고정 지연 (ms) */`로 바꾸고 `DEFAULT_RETRY_INTERVAL_MILLIS` 이름과 `1_000` 값은 보존한다.
- [x] 작업 1의 대상 테스트를 재확인했다. 이후 전체 모듈 테스트에서도 두 테스트를 포함해 PASS했다.

**성공 증거:** 두 대상 테스트 PASS, 함수/상수 선언과 실행 코드는 그대로이고 KDoc만 바뀐다.

### 작업 4: 모듈 검증과 범위/API 점검

**파일:** 작업 1–3의 네 구현 파일.

- [x] `repo-test-summary -- ./gradlew :bluetape4k-redisson:test`로 모듈 테스트를 실행했다. 다른 모듈/워크트리의 Testcontainers 테스트와 병렬 실행하지 않았다.
- [x] `./gradlew :bluetape4k-redisson:detekt --console=plain`을 실행했다. BUILD SUCCESSFUL; 기존 진단은 변경하지 않은 파일에 한정됐다.
- [x] deprecated 호출 및 fixture 키 검색 결과가 비어 있고 Bluetape 식별자를 보존했다.
- [x] 정확한 네 구현 파일만 stage하고 `git diff --cached --check`와 전체 staged diff를 확인했다.

**성공 증거:** 모듈 테스트와 Detekt PASS, deprecated 설정 검색 결과 없음, diff check PASS, 변경 범위는 코드 커밋 `66684dce98cd6d074c9b0cb6814a78fe11e2089d`의 네 구현 파일뿐이며 실행 코드/API/ABI/Catalog diff가 없다.

### 작업 5: 구현 diff의 7-Tier 및 사전 PR 코드 리뷰

**파일:** 구현 diff와 새 `docs/review/2026-11-25-issue-1916-redisson-retry-delay-review.md`.

- [x] `$bluetape-kotlin-patterns`의 7-Tier 기준과 workflow Step 6-R의 Performance, Stability, Security, Operator/Ops, Developer/API, User/caller 6개 관점 및 통합 검토를 적용했다.
- [x] 독립 코드 검토를 시도했다. `code-reviewer` agent는 로컬 코드 호스트 실행 파일 누락으로 exact diff를 검토하지 못해 `UNASSESSABLE`을 반환했다. workspace router의 Independent Code Review Fallback에 따라 주 세션에서 인라인 검토를 수행했고, 독립 attestation은 없다고 기록했다.
- [x] P2 테스트 관용구 한 건을 `shouldNotBeNull()`로 수정한 뒤 targeted 테스트 2건과 모듈 테스트 456건을 다시 통과시켰다. 계획의 독립 reviewer 실패 처리도 workspace router에 맞게 고쳤다. 최종 인라인 검토 결과 P0=0/P1=0, 미해결 P2/P3=0이다. 독립 verdict 부재만으로 PR을 막지는 않으며, merge 전 필요한 exact-head review/CI와 reviewer lane 상태는 별도 게이트로 유지한다.
- [x] 코드 리뷰 artifact `docs/review/2026-11-25-issue-1916-redisson-retry-delay-review.md`에 범위/diff SHA, 일곱 Tier, 여섯 관점, finding 조치, 검증과 미검증 항목을 기록했다. SPW-01–SPW-05 검증은 작업 6 문서 감사에서 함께 기록한다.

**성공 증거:** 사전 PR 검토는 정확한 구현 diff에 연결되고 P0=0/P1=0이며 남은 발견 사항의 처리를 기록한다. README/CHANGELOG는 사용자 동작이나 공개 API가 바뀌지 않는다는 diff 근거로 N/A 처리한다.

### 작업 6: 테스트 기록, 교훈, Superpowers 색인 갱신

**파일:** `docs/testlogs/2026-11.md`, `docs/lessons/2026-11-25-issue-1916-redisson-retry-delay.md`, `docs/superpowers/index/2026-11.md`.

- [x] 대상 테스트, 모듈 테스트, Detekt의 실제 명령/결과와 실행 날짜를 testlog에 기록했다. 초기 실패, parser 오류와 복구를 보존했고, 최종 assertion 수정 이후 targeted 2건과 모듈 456건을 다시 통과시켰다.
- [x] 교훈 문서에 Redisson 4.8.0의 실제 `Config.fromYAML` 실패, 지원 속성으로 fixture를 정리한 이유, 제품 1초 계약, 공개 accessor 및 정확한 Gradle task 경로를 기록했다.
- [x] 월별 Superpowers 색인에 명세, 명세 검토, 계획, 계획 검토, 구현 검토와 교훈 문서 링크를 추가했다.
- [x] 변경된 한국어 문서를 다시 읽고 `node /Users/debop/.codex/skills/bluetape-writer/scripts/audit-korean-terms.mjs docs/superpowers/specs/2026-11-25-issue-1916-redisson-retry-delay-contract.md docs/superpowers/reviews/2026-11-25-issue-1916-spec-review.md docs/superpowers/plans/2026-11-25-issue-1916-redisson-retry-delay-plan.md docs/superpowers/reviews/2026-11-25-issue-1916-plan-review.md docs/review/2026-11-25-issue-1916-redisson-retry-delay-review.md docs/lessons/2026-11-25-issue-1916-redisson-retry-delay.md docs/superpowers/index/2026-11.md docs/testlogs/2026-11.md` 및 `git diff --check`를 실행했다. 감사가 testlog의 과거 `catalog snapshot train` 표기를 지적해 실제 범위를 보존한 `중앙 Catalog ref 갱신`으로 고쳤다. 재실행은 8개 파일, findings=0으로 통과했다.
- [x] 문서 산출물은 구현 커밋과 분리해 Lore 형식으로 커밋했다.

**성공 증거:** testlog에는 실행 결과만, 교훈 문서에는 재사용할 교훈만, 색인에는 실제 존재하는 문서 경로만 포함하고 관련 문서를 커밋한다.

### 작업 7: 지정 저장소/브랜치의 PR 생성 및 메타데이터 재조회

- [x] PR 직전에 이슈 #1916의 상태·본문·assignee·milestone·labels를 다시 읽고 구현 완료 근거를 보완한다. 현재 live read-back은 OPEN, assignee `debop`, milestone `2.2.0`, 계획한 labels다.
- [x] `git fetch origin develop` 후 `git merge-base --is-ancestor origin/develop HEAD`로 현재 base가 feature head의 조상인지 확인했다. origin base와 feature head가 이 PR에 지정한 범위와 일치한다.
- [x] `git push --set-upstream origin feat/issue-1916-catalog-contracts`로 PR 생성 당시 검증한 head `fd596550c3162288ced9639802c6ef72187a4843`를 게시했다. post-review 문서 커밋 `864050e2…` 직후 branch API와 PR ref가 잠시 어긋났지만, 보정 commit `7a425e1db074f642f8071bf1c138c16b7dbf4d66` 후 GitHub PR head와 `refs/pull/1920/head`가 일치함을 확인했다. 새 PR CI run `45652510093`은 대기 중이고 Security History run `45652510317`은 진행 중이다.
- [x] `.github/pull_request_template.md`의 모든 섹션을 유지하는 한국어 PR 본문을 작성했다. 변경 이유, 적용 범위, 검증과 `Closes #1916` 및 마지막 `## DoD Status`를 포함한다.
- [x] PR 본문은 승인된 변경과 당시 검증 증거에 맞춰 채웠다. exact-head CI와 review 증거를 확인한 뒤 갱신한다.
- [x] 승인된 계획 범위로 `bluetape4k/bluetape4k-projects` base `develop`, head `feat/issue-1916-catalog-contracts` PR을 만들었다. issue의 assignee `debop`, milestone `2.2.0`, 관련 labels를 반영했다.

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

- [x] GitHub에서 PR을 다시 조회해 repository, base/head, head SHA, assignee, milestone, labels, `Closes #1916`, closing issue reference, 본문의 마지막 `## DoD Status`를 확인했다. PR #1920, head `fd596550…`이다.

**성공 증거:** 생성된 PR URL과 현재 head의 메타데이터·본문 조회 결과가 일치한다. 이 단계의 사용자 승인은 PR 생성까지만 포함하며 merge는 포함하지 않는다.

### 작업 8: PR 후 리뷰/CI 수렴 및 merge-ready 보고

- [x] 실제 PR diff와 정확한 head에 대해 7-Tier, 6개 관점+통합 post-PR inline review를 실행했다. `docs/review/2026-11-25-issue-1916-redisson-retry-delay-review.md`에 exact head와 diff SHA를 기록했다. P0/P1 및 미해결 P2/P3는 0건이며 독립 reviewer attestation은 없다.
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
- [ ] merge-ready DoD를 작성한다. 정확한 head, 테스트, 검토, PR 메타데이터를 포함하고 fresh merge approval 전에는 `PENDING`으로 보고한다. GNO 색인은 merge 후 canonical checkout 동기화와 cleanup이 끝난 CG-18에서 수행한다.

**성공 증거:** PR이 최신 exact head에서 CI와 review gate를 통과했거나 외부 결과 대기로 명시적인 PENDING 상태다. merge는 별도 fresh approval 뒤에만 수행한다.

### 작업 9: fresh merge approval 이후 통합 및 GNO 색인

- [ ] CG-16: merge-ready 보고 후 현재 PR head SHA에 대한 fresh explicit merge approval을 받는다. 계획 승인이나 PR 생성 승인을 재사용하지 않는다.
- [ ] CG-17: 승인한 merge 전략으로만 PR을 병합하고 GitHub의 merged 상태와 merge SHA를 확인한다.
- [ ] CG-17A: merge 확인 뒤 canonical `develop` checkout을 동기화하고 로컬 변경을 보존한다.
- [ ] CG-17B: 동기화 이후 병합 ancestry와 사용자 변경을 확인하고, 권한이 있는 task-owned worktree/branch만 정리한다. dirty·모호한 상태는 보존한다.
- [ ] CG-18: CG-17A와 CG-17B가 PASS 또는 근거 있는 N/A일 때만 `gno update`, `gno embed --collection bluetape4k-github`, `gno embed --collection bluetape4k-docs`, `gno search "bluetape4k-projects Redisson retryDelay issue 1916" -c bluetape4k-github`를 실행해 canonical collection에서 issue/PR과 명세·계획·교훈을 확인한다.

**성공 증거:** 현재 head에 대한 fresh merge approval, merged SHA, 동기화된 canonical checkout, 보수적 task cleanup, canonical GNO 갱신/대표 검색 결과가 각각 기록된다. 사용자가 merge를 승인하기 전까지 이 작업은 PENDING으로 둔다.

## 위험 예측과 대응

| 위험 | 신호 | 대응 | 되돌림/재실행 |
|---|---|---|---|
| YAML tag 또는 ISO `Duration` 표기 파싱 실패 | 테스트에서 파싱 예외, 타입/지연 불일치 | Redisson 4.8.0 고정 소스와 `ConfigSupport` 계약에 맞춰 표기를 수정하고 1500ms assertion은 유지한다. 테스트를 skip하지 않는다. | YAML만 수정해 대상 테스트를 다시 실행한다. API나 dependency 변경이 필요하면 계획으로 돌아간다. |
| test YAML의 기존 파싱 실패 | deprecated/미지원 fixture 키가 Redisson 4.8.0 parser 예외를 일으킨다. | 테스트 로그와 `javap` getter 목록을 대조해 미지원 키를 제거하고 유효한 설정은 보존한다. 제품 API·지연 동작으로 범위를 넓히지 않는다. | 같은 parser assertion을 재실행하고 원인을 lesson과 testlog에 기록한다. |
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
- 기존 테스트 YAML은 Redisson 4.8.0에서 deprecated/미지원 키로 파싱되지 않았다. 지원되는 `retryDelay`의 1.5초와 제품 고동시성 설정의 1초 고정 지연을 각각 검증한다.
- Step 2-R는 P0=0/P1=0 inline fallback verdict이며 독립 attestation이 아니다. 계획 리뷰에도 동일한 경계를 적용한다.
