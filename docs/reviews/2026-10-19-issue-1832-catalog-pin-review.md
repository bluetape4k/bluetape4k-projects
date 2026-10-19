# Issue #1832 Gradle 카탈로그 고정 변경 리뷰

## 판정 요약

검토한 변경은 `settings.gradle.kts`와 `.github/workflows/ci.yml`에서 Projects가 사용하는 Dependencies 카탈로그 참조를 동일한 불변 commit으로 갱신한다. Kotlin 제품 코드, 공개 API, ABI, 발행 모듈 구성은 바뀌지 않았다.

- 소스 변경의 P0/P1/P2/P3 발견: 0건
- 독립 아키텍처 리뷰: `CLEAR`, P0/P1 0건
- 독립 7-Tier 코드 리뷰: 코드 결함 0건, P0/P1 0건. 형식상 `COMMENT`는 LSP 진단 도구 부재와 미실행 원격 검증을 가리킨다.
- 머지 전 남은 검증: 새 PR HEAD의 GitHub CI
- 머지 뒤 남은 검증: default-branch Dependency Submission과 실제 Dependabot `Try again`

Inline verdict: PASS; P0=0, P1=0.

이 inline verdict는 소스 diff에 대한 주 세션의 판정이다. 먼저 만들어진 `code-review`와 `architecture-review` 예약은 dispatch 전에 만료되어 취소됐다. 이 문서로 해당 예약의 terminal 상태를 inline fallback으로 해소하며, 두 예약을 완료된 독립 리뷰로 표시하지 않는다. 별도로 만든 `code-review-r2`와 `architecture-review-r2`는 독립 리뷰를 마쳤다.

## 검토 범위와 기준

| 항목 | 값 |
|---|---|
| 저장소 | `bluetape4k/bluetape4k-projects` |
| 작업 | Issue #1832, Gradle Dependabot 의존성 목록 조회 실패 |
| 기준 HEAD | `d78baa47c6b60bd8e931d61539c2fe939f8cc3f5` |
| 검토한 patch SHA-256 | `a637548e64220330b1b241113d5605c9dc826d39ac217852c5a5efd7aab23f7d` |
| 범위 | `settings.gradle.kts`, `.github/workflows/ci.yml` |

현재 변경은 두 파일의 각 한 줄을 이전 Dependencies SHA `198147fd6ca2992fae32e175bd5f3d396c497096`에서 `e9946b18bdb0022f52cd5dc8994d859d470e7bab`으로 바꾼다. 새 SHA는 signed tag `catalog/2026-10-18-01`의 peeled commit이다. GitHub API는 tag object `3ff61d41521657b97be1d6ecaf7004fa61710634`의 서명을 `verified=true`, `reason=valid`로 확인했다. 해당 카탈로그 변경은 `com.rabbitmq:amqp-client`를 `5.35.0`에서 `5.37.0`으로 올린다.

### 원본과 주장 연결

| 주장 | 원본 |
|---|---|
| Gradle의 기본 SHA와 immutable SHA 형식 검증 | [`settings.gradle.kts`](../../settings.gradle.kts#L22-L30) |
| CI 환경 SHA, checkout 및 exact SHA assertion | [`.github/workflows/ci.yml`](../../.github/workflows/ci.yml#L39-L46), [checkout 단계](../../.github/workflows/ci.yml#L283-L300) |
| `amqp-client`의 테스트 전용 의존성 | [`testing/testcontainers/build.gradle.kts`](../../testing/testcontainers/build.gradle.kts#L195-L198) |
| RabbitMQ 기본 포트, 연결 및 빈 이미지 태그 테스트 | [`RabbitMQServerTest.kt`](../../testing/testcontainers/src/test/kotlin/io/bluetape4k/testcontainers/mq/RabbitMQServerTest.kt#L23-L102) |
| signed tag와 대상 catalog commit | [`tag object 3ff61d41`](https://api.github.com/repos/bluetape4k/bluetape4k-dependencies/git/tags/3ff61d41521657b97be1d6ecaf7004fa61710634)의 서명 검증은 `valid`; signed tag `catalog/2026-10-18-01`은 [`commit e9946b18`](https://github.com/bluetape4k/bluetape4k-dependencies/commit/e9946b18bdb0022f52cd5dc8994d859d470e7bab)을 가리킨다. |
| 기존 consumer/CI pin 및 실제 retry 교훈 | [`2026-10-15` lesson](../lessons/2026-10-15-gradle-dependabot-external-catalog.md) |

## 7-Tier 검토

| Tier | 검토 | 결과 |
|---|---|---|
| 보안·공급망 provenance | 브랜치나 가변 태그 대신 40자리 commit SHA를 고정하고, signed tag가 가리키는 commit과 대조했다. | PASS — 두 설정이 같은 commit을 가리킨다. |
| 설정 정확성 | Gradle 속성 → 환경 변수 → 기본값 우선순위와 CI checkout의 ref 및 exact SHA assertion을 확인했다. | PASS — 기존 우선순위와 검증이 유지되고 기본값/CI 값이 일치한다. |
| 구조·유지보수성 | 활성 설정에서 이전 SHA 참조를 검색하고 변경 크기를 확인했다. | PASS — 활성 설정의 stale SHA 없음, 두 줄만 변경. |
| Kotlin API·ABI | Kotlin/JVM 선언, 시그니처, 모듈 공개면 변경 여부를 확인했다. | N/A — Kotlin 소스와 공개 선언이 바뀌지 않는다. |
| 테스트·호출자 | 기본 설정 및 CI 환경에서 의존성 선택과 testcontainers 테스트 컴파일·RabbitMQ 테스트를 확인했다. | PASS — 두 경로 모두 `amqp-client:5.37.0`; 컴파일 성공; RabbitMQ 테스트 3건 성공. |
| 성능·운영 | 새 카탈로그 조회와 실행 경로에 런타임 코드 변경이 있는지 검토했다. | PASS — 제품 런타임 변경 없음. 카탈로그 캐시가 비어 있을 때 새 commit을 한 번 내려받는 동작만 추가된다. |
| CI·toolchain·release | workflow 구문, CodeQL 및 release 입력의 SHA 처리와 미실행 범위를 검토했다. | PASS — `actionlint` 성공. exact-head GitHub CI는 아직 미실행. |

### 독립 리뷰

- **아키텍처 리뷰:** `CLEAR`. 두 설정의 SHA 정렬, Gradle override 순서, CI checkout 검증을 확인했다. 별도 P3 운영 메모로 signed tag와 commit 매핑, 두 pin을 함께 되돌리는 절차를 PR 본문에 쓰도록 권했다.
- **코드 리뷰:** 변경 결함 없음, P0/P1 0건. 공개 API/ABI 변경 없음. 주 세션이 수집한 테스트와 CI 정적 검사 결과를 검토했으며 모순되는 소견은 없었다. Kotlin language server는 Java 25 버전 문자열을 읽지 못했고 Java 21 재시도는 응답 없이 시간 초과되어 `lsp_diagnostics`는 검증 불가로 남았다.

## 로컬 검증 증거

컴파일과 RabbitMQ 테스트는 `BLUETAPE4K_DEPENDENCIES_CATALOG_REF=e9946b18bdb0022f52cd5dc8994d859d470e7bab` 환경 변수로 실행했다.

| 검증 | 결과 |
|---|---|
| `:bluetape4k-testcontainers:dependencyInsight --dependency com.rabbitmq:amqp-client --configuration testRuntimeClasspath` | 기본 설정에서 `5.37.0` 선택 (`5.30.0 → 5.37.0`) |
| 위 명령에 `BLUETAPE4K_DEPENDENCIES_CATALOG_REF=e9946b18bdb0022f52cd5dc8994d859d470e7bab` 지정 | CI 환경에서도 `5.37.0` 선택 |
| `:bluetape4k-testcontainers:compileTestKotlin --rerun-tasks --no-configuration-cache` | 성공 |
| `:bluetape4k-testcontainers:cleanTest :bluetape4k-testcontainers:test --tests io.bluetape4k.testcontainers.mq.RabbitMQServerTest --no-build-cache --rerun-tasks --no-configuration-cache` | 성공; 세 JUnit XML 합계 3 tests, 0 failures, 0 errors, 0 skipped |
| `actionlint .github/workflows/ci.yml` | 성공 |
| `git diff --check` | 성공 |
| signed tag `catalog/2026-10-18-01`와 peeled commit `e9946b18bdb0022f52cd5dc8994d859d470e7bab` | 일치 확인 |

처음 실행한 `dependencyInsight`는 configuration을 생략해 기본 `compileClasspath`를 조회했다. `amqp-client`는 `testImplementation`이므로 결과에 없었고, `testRuntimeClasspath`를 지정한 재실행에서 두 설정 모두 `5.37.0` 선택을 확인했다. 첫 compile 재실행 당시 Gradle daemon 로그에는 `terminated in response to a user interrupt`가 남았고 OOM 문구는 없었다. 진행 출력을 유지한 단독 재실행은 32초 만에 성공했다. 처음 interrupt가 발생한 원인은 확정하지 못했고, 소스 컴파일 오류는 재현되지 않았다. 테스트 자체에서 실패·오류·skip은 없었다.

## 남은 게이트와 대응

1. PR을 만든 뒤 exact HEAD에서 GitHub CI를 확인한다. aggregate CI가 초록이어도 생략된 job을 별도로 확인한다.
2. PR 본문에 signed tag와 peeled commit의 매핑을 적고, rollback 시 `settings.gradle.kts`와 `.github/workflows/ci.yml`의 pin을 함께 되돌린다고 적는다.
3. 머지는 별도의 최신 승인을 받은 뒤 진행한다.
4. 머지 후 default-branch Dependency Submission을 확인하고, Issue #1832에 연결된 alert 화면에서 실제 Dependabot `Try again`을 실행한다. 성공 전까지 Issue #1832를 닫지 않는다.
5. 기존 `docs/lessons/2026-10-15-gradle-dependabot-external-catalog.md`가 consumer/CI pin 및 실제 retry gate를 다루므로 새 lesson은 추가하지 않는다.

## 작성 검증

| Gate | 상태 | 증거 |
|---|---|---|
| SPW-01 | PASS | 리뷰 독자·목적·언어·원본·미해결 검증을 이 문서에 기록했다. |
| SPW-02 | PASS | 범위, 판정, 심각도, 7-Tier 검토, 근거, 남은 게이트를 포함했다. |
| SPW-03 | PASS | 한국어 기술 문체와 식별자 보존을 확인했고 KO-01~KO-07을 완료했다. |
| SPW-04 | PASS | 표의 주장을 연결한 원본과 로컬 검증 결과를 대조했다. |
| SPW-05 | PASS | 최종 read-back 완료; Writer DoD와 문서 digest를 독립 리뷰 gate의 영수증 증거에 기록한다. |
| KO-01~KO-06 | PASS | 사실·불확실성, 기술 문체, 용어, 표, 링크, 코드 토큰을 확인했다. |
| KO-07 | PASS | `audit-korean-terms.mjs` 결과 0 findings. |
