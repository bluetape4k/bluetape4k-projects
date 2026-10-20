# Issue #1832: catalog 선언과 실행 경계별 증거를 함께 검증하기

## 맥락

Projects의 중앙 버전 소유권 catalog는 Gradle 내부 alias의 버전 정보를 중앙 관리하지만 GitHub Dependabot이 읽는 표준 경로와 같지 않았다. Issue #1832는 Gradle 보안 업데이트가 제출된 dependency graph에서 catalog 관리 좌표를 찾지 못해 종료되는 현상을 다룬다. 현재 GitHub issue는 OPEN이다. 2026-10-20 조회에서 FreeMarker alert #235도 CRITICAL 상태로 OPEN이었다.

FreeMarker 보안 공지는 2.3.34 이하를 영향 범위, 2.3.35를 수정 버전으로 지정한다. Projects build 설정은 Dokka 구성에서 중앙 catalog 버전을 선택하고, 최소 2.3.35 아래로 내려가면 빌드를 실패시킨다. [GitHub advisory](https://github.com/advisories/GHSA-27j2-h3m2-8237)와 [Apache 2.3.35 릴리스 노트](https://freemarker.apache.org/docs/versions_2_3_35.html)를 참조한다.

## 결정

Projects의 기존 catalog를 versionless 원본으로 분리하고, 중앙 mapping에서 고른 열 개 GAV만 표준 gradle/libs.versions.toml에 version.ref로 투영한다. generator는 원본 alias와 순서를 유지하고 allowlist, repository map, parser receipt, candidate lock을 서로 해시로 결속한다. CI는 Dependabot Gradle parser가 실제 표준 catalog에서 열 개 좌표를 읽는지 고정 upstream commit으로 확인한다.

각 workflow job은 자체 파일시스템 경계를 가진다. 다른 job의 parser receipt를 사용할 수 없으므로 Projects catalog-governance도 parser fixture를 실행하고 자기 receipt를 만든다. 같은 이유로 Central의 push-only source audit는 Projects 원본 catalog를 직접 읽어야 한다.

## 결과와 검증

- commit `85d0312cf9b7e9c97dc592645582a341a4164ab3` 기준 Projects local workflow contract 테스트 11개와 actionlint, verifier, Python compile, diff check가 통과했다.
- Central 전체 Python suite 514개 통과, 2개 skip. 두 skip은 관리 sibling 저장소가 격리 worktree에서 보이지 않아 real-workspace inventory를 실행하지 않은 결과다.
- 외부 1.9.2-SNAPSHOT artifact 검사 한 건은 HTTP 403으로 건너뛰었다. 제품 테스트 실패로 계산하지 않는다.
- 98개 Dokka configuration에서 FreeMarker 2.3.35와 jsoup 1.23.2를 확인했다. 13개 게시 모듈의 POM과 Gradle Module Metadata는 26개 모두 baseline과 byte-identical했다.
- 대상 모듈 classes와 mock-web-server/mock-webflux-server Jib 빌드가 앞선 검증에서 통과했다.
- 이 결과는 parser PR CI나 hosted Dependabot 성공을 뜻하지 않는다. 현재 FreeMarker alert #235는 계속 OPEN이며 Issue #1832도 해결 처리하지 않는다.

## 놓친 점과 재발 방지

첫 구현에서는 Projects CI가 다른 workflow의 parser receipt를 사용할 수 있다고 가정했다. 새 runner의 파일시스템에는 그 receipt가 없어서 generator check가 실패할 수 있었다. workflow 계약 테스트를 먼저 실패시키고, parser contract를 같은 job 안에서 실행해 receipt를 생성하도록 수정했다.

두 번째 구현 공백은 Projects catalog를 나눈 뒤 Central push workflow가 여전히 기본 generated catalog를 authority로 읽는 경로였다. Projects 원본 파일이 존재할 때 source catalog를 지정하도록 수정하고, 이전 구조의 저장소를 위한 legacy fallback을 보존했다.

새 검증 자료를 추가하면 실행 경계마다 파일이 공유되는지 확인한다. catalog authority를 나누면 정규 map loader뿐 아니라 모든 CI audit와 간접 consumer가 source/generated 역할을 구분하는지 확인한다. 증거 생성 전후로 exact HEAD와 파일 해시를 재검증하며 map, receipt, lock을 closeout까지 보존한다.

## PR exact-head CI에서 찾은 실행 경계 결함

PR #1838의 첫 exact-head run `39370896190`은 Projects HEAD `ac3d40b70367eec66091da02a35d078bdc6bf9b1`에서 실패했다. `JVM Release Contract`는 설정 단계에서 signed tag `catalog/2026-10-20-01`을 commit SHA가 아니라고 거부했다. `Central Catalog Governance`는 Dependabot Core용 `Gemfile` 밖에서 parser fixture를 일반 `ruby`로 실행해 `toml-rb`를 불러오지 못했다. governance 실패가 downstream gate를 닫아 15개 동적 build/test job은 모두 skip되었다. 제품 테스트가 실패한 것은 아니며 해당 run은 제품 테스트 증거도 제공하지 않는다.

수정 commit `9dc756c7be448fbb62b7d2f4cde10c4904b8ac67`은 설정에서 허용된 catalog tag와 peeled exact SHA를 함께 요구하고, 실제 파일 다운로드와 cache key는 SHA로 고정한다. parser fixture는 pinned Dependabot Core `Gemfile`을 `BUNDLE_GEMFILE`로 지정하고 `bundle exec ruby`로 실행한다. 두 실행 경계 회귀 테스트는 수정 전 RED, 수정 후 GREEN을 확인했다. 이후 Projects contract 테스트는 11개로 늘어 모두 통과했다. Dependabot Core bundle의 실제 hosted 실행은 수정된 PR exact-head CI에서 다시 확인해야 한다.

수정 HEAD `9dc756c7be448fbb62b7d2f4cde10c4904b8ac67`의 retry `39371790611`은 parser와 JVM settings 단계를 통과했지만, Central generator가 `BLUETAPE4K_DEPENDENCIES_CATALOG_REF`의 tag 문자열을 central commit SHA와 직접 비교해 실패했다. 앞선 단계에서 signed tag의 peeled SHA를 확인했으므로 generator 호출 범위에서만 `REF`에 그 SHA를 전달해 호환 검사를 만족시킨다. 같은 run의 gitleaks는 cache SHA 변수 이름 `CacheKey`와 이를 고정하는 workflow test 문자열에서 generic API key 패턴 두 건을 보고했다. 실제 값은 인증정보가 아니라 commit SHA 식별자이므로 변수명을 `CacheSha`로 바꿨다. clean source archive에서 재실행한 gitleaks는 누출을 찾지 못했고 Central pin guard, 11개 workflow test, verifier, actionlint, Python compile, Gradle help가 통과했다. 이 수정은 commit `85d0312cf9b7e9c97dc592645582a341a4164ab3`에 들어갔지만 아직 PR 원격에 push하지 않았으며 hosted exact-head 검증이 남아 있다.

PR #1838의 다음 exact-head run `39373076530`은 `Central Catalog Governance`의 adoption 검사에서 실패했다. Projects workflow에는 tag 문자열을 `BLUETAPE4K_DEPENDENCIES_CATALOG_REF`로 기록했지만 Central `sync-shared-versions.py`는 이 값을 정규식으로 full commit SHA만 읽는다. 따라서 catalog `settings.gradle.kts`의 SHA pin은 `local=dcc7fa7557d3c0548b9e3fc28c48877827ea2f74`, CI workflow 값은 `central=<missing>`으로 판정됐다. 이 검사는 실패했으며 Build와 14개 Test matrix job, 총 15개 제품 job이 skip됐다. 제품 테스트 실패로 분류하지 않는다.

회귀 검사 두 개는 수정 전에 기대한 이유로 실패했다. 첫 검사는 workflow의 `REF`가 tag라 SHA 계약을 만족하지 않음을 잡았고, 둘째 검사는 verifier가 tag를 canonical `REF`로 받아들이는 것을 잡았다. `REF`는 full SHA로, immutable tag 이름은 별도 `TAG`로 두고 workflow는 `TAG`를 체크아웃과 peeled-commit 확인에 쓰도록 수정했다. `REF`와 `SHA`는 같은 commit으로 유지한다. 수정 후보는 로컬 workflow contract 12개, verifier, actionlint, Python compile, diff check, Central adoption 비교(`[]`), Gradle help를 통과했다. 원격에는 아직 반영되지 않았으므로 새 exact-head CI가 최종 증거다.

## PR 검사에서 merge SHA와 PR head SHA를 구분하기

PR #1838의 run `39408775102`는 run metadata상 head `3ae9be4d36bdb6eb9d61289cd62d5ffac9d7461c`에 연결됐지만, `pull_request` event의 기본 checkout은 합성 merge commit `41a685c7…`를 사용했다. `Central Catalog Governance`는 checkout HEAD와 `$GITHUB_SHA`를 Projects generator map 및 receipt에 결속했기 때문에, green job만으로 PR head 자체를 검증했다고 볼 수 없었다. [GitHub Actions 공식 문서](https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows)는 `pull_request`의 기본 `GITHUB_SHA`가 merge commit이며 head commit만 검사하려면 `github.event.pull_request.head.sha`를 checkout하라고 설명한다.

P1 회귀를 막기 위해 workflow가 PR에서는 `github.event.pull_request.head.sha`, 다른 event에서는 `github.sha`를 `PROJECTS_EXPECTED_SHA`로 선택하고, catalog-governance checkout 및 generator branch를 이 값에 결속하도록 수정했다. checkout 직후 `git rev-parse HEAD`도 다시 비교한다. verifier 테스트는 checkout을 `github.sha`로 바꾸거나 expected SHA 식을 merge SHA로 바꾸면 실패해야 한다. 해당 부정 테스트는 수정 전 RED였고, 수정 후 workflow contract 14개가 통과했다. `verify-catalog-workflows.py`, `actionlint`, Python compile, `git diff --check`도 통과했다.

run `39408775102`의 나머지 product jobs는 합성 merge tree를 대상으로 했다. 그 결과와 branch-head provenance는 서로 대체하지 않는다.

### Projects의 모든 first-party checkout을 같은 SHA로 고정하기

이후 PR run `39412396158`의 metadata상 head는 `47cd8034d20366f7e5e1cff9a708f6c6f153483b`였다. `catalog-governance`는 예상 PR head를 checkout했지만, 나머지 CI job의 기본 checkout은 계속 `pull_request` 합성 merge SHA를 사용했다. 따라서 governance 외 job이 PR head 코드를 읽는다는 보장이 없었다. 이는 모든 required job에 PR head exact-checkout을 적용한다는 설계 기준을 충족하지 못하는 P1이다.

회귀를 막기 위해 `.github/workflows/ci.yml`의 Projects 저장소 checkout 24곳 모두에 `ref: ${{ env.PROJECTS_EXPECTED_SHA }}`를 지정했다. GitHub Pages, dependencies 저장소, Dependabot Core를 받는 다른 저장소 checkout은 자체 repository/ref를 유지한다. `verify-catalog-workflows.py`는 Projects 저장소 checkout 중 하나라도 이 ref를 빠뜨리거나 다른 값으로 바꾸면 실패한다. 테스트는 product job의 정확한 ref와 merge SHA를 넣은 음성 경로를 확인한다. 두 회귀 검사는 수정 전에 기대한 이유로 RED였고, 수정본에서 catalog workflow contract 20개, verifier, `actionlint`, Python compile, `git diff --check`가 통과했다.

이 변경을 포함하지 않은 run `39412396158`은 head `47cd8034d20366f7e5e1cff9a708f6c6f153483b`의 선행 CI다. run이 완료되더라도 위 checkout 수정의 hosted 증거가 되지 않으므로, push된 새 SHA의 CI에서 각 job checkout과 테스트 결과를 다시 확인해야 한다.

## MongoDB Testcontainers와 Actions runner 커널 호환성

run `39408775102`의 `Test / Spring Boot (batch-nosql)`은 세 번 재시도한 뒤 `ReactiveMongoOperationsCoroutinesTest` 실패로 종료됐다. 테스트 결과 artifact에는 66개 테스트 중 1개 실패가 기록됐고, 원인은 `mongo:8.0.28`이 runner kernel에서 시작을 거부한 것이다. 컨테이너 로그는 Linux kernel `6.19` 이상에 알려진 비호환이 있다고 명시한다. 이는 catalog parser/generator 실패가 아니며, 실행된 MongoDB integration test의 환경 차단으로 분류한다.

[MongoDB 8.0 공식 릴리스 노트](https://www.mongodb.com/docs/manual/release-notes/8.0/)는 kernel `6.19`부터 `7.0.13`까지를 비호환 범위로 안내하고 `7.0.14` 이상을 해결 범위로 든다. 당시 GitHub `ubuntu-latest` job은 Mongo가 거부한 kernel에서 실행됐다. 통합 테스트는 현재 kernel `6.8.0-1070-azure`인 [Ubuntu 22.04 runner 이미지](https://github.com/actions/runner-images/releases/tag/ubuntu22/20261018.327)에서 실행하도록 좁게 고정했다.

Ubuntu 22.04 runner 계열은 2026-09-17부터 deprecation 단계에 들어갔고 2027-04-17 지원 종료가 예정되어 있다. 따라서 이번 고정은 임시 대응이다. 후속 추적 Issue [#1839](https://github.com/bluetape4k/bluetape4k-projects/issues/1839)는 지원되는 runner/kernel 조합으로 이전하고 pin을 제거하도록 2.1.0 milestone에 등록했다. 새 exact-head CI가 Test/Data 및 Spring Boot batch-nosql 결과를 확인하기 전에는 이 수정의 hosted 검증을 완료로 보지 않는다.

## 전달 경계

마지막으로 확인한 Projects 원격 PR head는 `18b67202fdc870ab5c2baab40fca756f115f6052`다. 이 문서와 workflow 보완은 해당 commit 위의 로컬 미커밋 변경이므로 이전 CI나 review가 새 코드를 증명하지 않는다. 변경을 push한 뒤 새 exact-head CI와 독립 리뷰를 확인한다. CI의 제품 job은 PR head를 테스트하고, 별도 merge-result job은 GitHub의 synthetic merge commit에서 workflow contract만 실행한다. 이 두 결과는 서로 다른 범위다. GitHub는 `pull_request` 이벤트의 `github.sha`를 merge branch의 merge commit으로 정의한다([공식 문서](https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows)). merge 후 exact-SHA Dependency Submission과 실제 hosted Dependabot 재실행으로 alert #235를 확인하기 전까지 Issue #1832를 열어 두고 보안 문제가 해결됐다고 보고하지 않는다.

## Workflow validator가 놓친 checkout 경계

원격 HEAD `18b67202fdc870ab5c2baab40fca756f115f6052`를 본 독립 리뷰는 verifier가 `repository:`가 있는 모든 checkout을 건너뛰어 명시적인 Projects 저장소 checkout이 `PROJECTS_EXPECTED_SHA` 검사를 우회할 수 있다고 지적했다(P2). architecture review는 checkout 탐지가 `actions/checkout@v7`만 인식해 다른 action major의 누락을 놓칠 수 있고, 허용된 외부 repository/ref 조합을 완전히 고정하지 않은 점도 지적했다. 같은 review에서 PR synthetic merge result 검증 job과 `dorny/paths-filter@v4`에 필요한 `pull-requests: read` 권한이 빠진 점, Test/Data runner 규칙에 독립 negative test가 없는 점도 확인했다.

보완 후보는 모든 `actions/checkout@...`을 찾고, repository가 생략됐거나 `bluetape4k/bluetape4k-projects` 또는 `${{ github.repository }}`인 경우 first-party checkout으로 분류해 `PROJECTS_EXPECTED_SHA`를 요구한다. 나머지 external checkout은 job, repository, ref, path의 allowlist와 전체 inventory에 맞아야 한다. 이 계약을 지키지 않는 first-party/ref/외부 checkout 테스트를 추가했다. `changes` job에는 `contents: read`와 `pull-requests: read`를 주고, PR event에서 `github.sha`를 checkout해 head와 base가 포함됐는지 확인한 뒤 workflow contract만 실행하는 `test-merge-result` job을 추가했다. 이 권한은 [dorny/paths-filter 공식 README](https://github.com/dorny/paths-filter)가 PR 파일 조회에 요구한다. Test/Data의 `ubuntu-latest` 금지 규칙도 별도 negative test로 고정했다.

구현 전 RED 결과는 28개 테스트 중 7개 실패였다. 구현 후 로컬 contract suite는 30/30 PASS이며 verifier와 `actionlint`, `ruff check`, Python compile, `git diff --check`가 통과했다. `ruff format --check`는 현재 HEAD에서도 두 파일이 실패해 이번 변경으로 생긴 포맷 회귀는 아니다. 새 변경은 아직 push되지 않아 hosted exact-head CI와 새 SHA의 독립 재리뷰는 미확인이다. 작은 merge-result contract job은 product test suite를 synthetic merge commit에서 실행하지 않으므로, 제품 job의 exact-head 결과를 대체하지 않는다.

## SHA 환경값 substring 검증 우회

새 exact-head architecture review는 `d80db8dbda0b311a2e119a290c3a9f9911264534`에서 verifier가 `PROJECTS_EXPECTED_SHA`의 올바른 문자열이 주석에 남아 있기만 해도 통과하고, merge-result 검사에 쓰는 `PR_HEAD_SHA`와 `PR_BASE_SHA`가 `${{ github.sha }}`로 바뀌어 ancestry 검사가 자기 자신을 확인하게 되어도 통과하는 P2를 재현했다. workflow의 현재 값은 올바랐지만, 기존 substring 검사는 보호하려는 계약을 값과 위치에 결속하지 못했다.

각 우회 negative test를 먼저 추가해 기존 verifier에서 세 개 모두 `validate_workflows()`의 빈 오류 목록으로 실패하는 RED를 확인했다. 수정본은 top-level `env.PROJECTS_EXPECTED_SHA`를 실제 key/value로 읽고, 이름이 고정된 merge verification step 안에서 `PR_HEAD_SHA`와 `PR_BASE_SHA`의 단일 env 값을 검사한다. 주석의 복사 문자열은 계약으로 인정하지 않는다. 세 회귀 테스트는 수정 후 통과했다.

현재 로컬 후보는 전체 workflow contract 33/33, `verify-catalog-workflows.py`, `actionlint .github/workflows/ci.yml`, `ruff check`, Python compile, `git diff --check`를 통과했다. 2026-10-20 21:02 KST 당시 후보는 commit/push 전이었으므로 이 결과는 hosted CI 증거가 아니다. d80 code-review lane은 제한 시간 내 결과를 내지 못해 probe grace 만료 후 workflow receipt에 중단을 기록했고 native interrupt를 실행했다. 새 후보 SHA의 독립 code review와 exact-head CI가 남아 있다. merge 후 Dependency Submission 및 실제 hosted Dependabot이 alert #235의 해결을 입증하기 전까지 Issue #1832와 alert #235를 계속 열어 둔다.

2026-10-20 21:07 KST live read-back에서 PR #1838 head는 `54c0513622774eaa1c57bf93a0e43b175425490d`였고 exact-head CI run `39419805598`은 진행 중이었다. 앞선 d80 CI run `39417533841`은 성공했지만 이 수정의 CI 증거는 아니다. 새 SHA code review는 아직 시작하지 않았다.

## YAML 스코프와 실제 셸 실행을 검증할 것

PR #1838의 exact head `a702d7f1b2573a8d03ca7e7382d780a99d28528d`를 본 독립 `code-reviewer`는 P2 두 건을 보고했다(P0=0, P1=0). 첫째, verifier가 top-level `PROJECTS_EXPECTED_SHA`의 문자열만 검사해 job 또는 checkout step의 같은 이름 env 덮어쓰기와 중복 top-level key를 거부하지 않았다. 둘째, ancestry 명령의 텍스트가 `test-merge-result` job 어딘가에 있는지만 검사해 실제 named step에서 제거하거나 비활성 step으로 옮겨도 통과했다. 같은 실행 step 안의 `PR_HEAD_SHA`/`PR_BASE_SHA` 재할당 및 `$GITHUB_ENV` 전달도 검사하지 않았다. `actionlint`는 중복 YAML key를 거부했지만 Projects CI에 actionlint 실행이 없어 verifier의 자체 계약이 필요했다.

근본 원인은 workflow를 단순 문자열로 검사하면서 YAML 적용 스코프와 실제 step의 실행·실패 전파·셸 변수 흐름을 구분하지 않은 점이다. 현재 수정은 `PROJECTS_EXPECTED_SHA`를 단일 top-level 값으로 요구하고 job/step env override를 거부한다. merge-result job의 pull_request 조건을 하나로 고정하고, 이름이 지정된 unconditional `run: |` step에서 순서가 맞는 checkout SHA 및 두 ancestry 명령이 실행되어야 한다. `continue-on-error`, PR SHA 셸 재할당, `GITHUB_ENV` 전달도 거부한다.

먼저 회귀 테스트 8개와 13개 변형을 추가했다. 기존 `a702d7f1` verifier는 각 사례에서 `validate_workflows()`의 빈 오류 목록을 반환했다(RED). 수정 뒤 전체 workflow contract는 41/41 PASS이며 verifier, Ruff lint, Python compile, `git diff --check`도 통과했다. 이전 exact-head CI `39420175917`은 `a702d7f1`의 28개 job 모두 성공했지만 수정 코드는 검사하지 않았다. 최신 변경은 아직 push되지 않았으므로 exact-head CI와 독립 재리뷰는 PENDING이다. 두 Python 파일의 `ruff format --check`는 기존 원본 HEAD에서도 실패했고, 신규 코드 부분에는 format diff가 없다.

다음 workflow verifier를 검토할 때는 key가 정확히 하나 있는지뿐 아니라 허용된 YAML 스코프에 있는지 확인한다. 안전성 명령은 정확히 이름 붙은, 무조건 실행되고 실패가 job에 전파되는 step의 실제 `run` 본문과 인자 출처까지 확인한다. CI에서 별도 YAML linter가 돌지 않으면 duplicate key와 지원하지 않는 우회 문법을 verifier 테스트로 직접 차단한다. workflow 검증 결과는 실행된 product test 범위와 분리해 보고한다.

**후속 lesson writer DoD (2026-10-20):** 독자는 Projects workflow verifier 유지보수자다. exact review head, P2 근거, old-head CI, RED/GREEN test 결과를 검증기와 테스트 소스에 대조했다. `a702d7f1` PR CI PASS와 현재 수정 후보의 PENDING 상태를 분리했다. 한국어 기술 문장을 자연성 기준에 맞춰 검토했고, 문서 read-back 및 contextual term audit 결과는 아래에 기록한다.

## 인용된 YAML key와 중복 checkout key도 검사하기

2026-10-21 inline 검토에서 정규식 기반 workflow verifier가 YAML key/value 표기의 일부만 인식하는 추가 P2를 재현했다. uses: "actions/checkout@v7"는 actions/checkout@... inventory에서 빠졌다. "repository" key는 external checkout을 first-party checkout으로 오분류할 수 있었다. "env", "shell", "defaults" key는 merge-result 검증의 실행 환경 검사를 우회했다. 중복 uses 또는 checkout metadata key는 실제 값과 scanner가 읽은 값이 달라질 수 있었다. "\u0075ses" 표기도 유효했고 로컬 actionlint가 승인했지만 verifier는 추가 checkout을 세지 못했다.

근본 원인은 텍스트 검증기가 YAML의 의미상 같은 인용 key/value를 동일한 계약 입력으로 정규화하지 않고, 한 key를 여러 번 선언한 경우도 거부하지 않은 점이다. CI에는 actionlint step이 없으므로 로컬 linter만으로는 우회 회귀를 막지 못한다.

수정 전 negative test 6개에서 총 9개 YAML 변형을 실행했으며, 모두 기대한 검증 오류 없이 통과해 RED였다. 수정은 공통 matcher로 plain/single-quoted/double-quoted simple key를 다루고, uses, repository, ref, path 중복을 거부하며, escaped mapping key와 YAML anchor/alias/merge key를 fail closed로 차단한다. 수정 뒤 contract suite 53/53, verifier, Ruff lint, Python compile, actionlint, git diff --check가 통과했다. 임시 workflow 네 개에서 quoted checkout/env/shell key와 quoted checkout action 값은 actionlint가 유효하다고 판정했다. escaped uses key 변형도 actionlint exit 0이었으며, 수정된 verifier는 이를 거부한다.

다음 workflow verifier 변경에서는 plain key만 바꾼 mutation 외에도 quoted scalar/key, escaped key, duplicate mapping key, alias/merge key 변형을 추가한다. linter 통과는 구조 계약 검증과 같지 않다. 보안상 중요한 checkout/runner/environment inventory는 CI에서 실행되는 자체 negative tests로 fail closed하게 유지한다.

이 검토 후에도 PR #1838은 새 candidate를 push하지 않은 상태였으므로 a8a118f...의 green CI는 현재 수정의 증거가 아니다. 최신 inline verdict는 P0=0/P1=0, 잔여 P2=0이며 독립 reviewer attestation이 아니다. Issue #1832와 CRITICAL alert #235는 2026-10-21 live read-back에서 계속 OPEN이고 fixed_at은 null이다. 승인된 merge, merge SHA Dependency Submission, hosted Dependabot 결과가 나올 때까지 둘 다 열어 둔다.

**후속 lesson writer DoD (2026-10-21):** SPW-01 PASS—대상 PR, 검토 코드와 live issue/alert 상태를 고정했다. SPW-02 PASS—원인·결정·결과·검증·miss·재발 방지 규칙을 포함했다. SPW-03 PASS—한국어 기술 표현과 key/value 코드 표기를 확인했다. SPW-04 PASS—6개 회귀 테스트, 9개 변형, actionlint 결과, verifier 구현을 대조했다. SPW-05 PASS—최종 read-back을 완료했고 exact-head CI를 미확인으로 남겼다. KO-01–KO-06 PASS; KO-07 기본 `clinic-appointment` term audit는 3개 문서에서 `findings=[]`였다.

## Writer DoD

| 검사 | 결과 |
|---|---|
| SPW-01 독자·목적·근거·미확인 사항 | PASS |
| SPW-02 lesson의 맥락·결정·결과·검증·miss·future guard | PASS |
| SPW-03 한국어 기술 문체 및 용어 일관성 | PASS |
| SPW-04 이슈·workflow·CI run·회귀 테스트·Gradle·advisory 근거 대조 | PASS |
| SPW-05 최종 read-back 및 workflow DoD 기록 | PASS |
| KO-01..KO-06 사실·문체·용어·표·링크 점검 | PASS |
| KO-07 contextual terminology audit | PASS, 수정 lesson findings `0`; 계획·testlog는 앞선 검토에서 findings `0` |

**후속 갱신 writer DoD (2026-10-20 20:42 KST):** 한국어 구현 계획과 lesson에 최신 local review finding, 30/30 contract 결과, 아직 push 전인 상태, exact-head CI·fresh review 대기를 추가했다. SPW-01–05와 KO-01–07을 각 문서에 독립 적용했다. 기존 이슈·commit·run 식별자를 보존하고 새 verifier 계약과 CI 증거를 코드·테스트와 대조했다. 두 문서를 마지막으로 읽고 audit 결과 `findings=[]`를 확인했다. `ruff format --check`가 현재 HEAD에서도 실패하는 것은 코드 포맷 검사 범위이며 lesson에 명시했다. 새 원격 CI와 재리뷰는 미확인 상태로 남겼다.

**후속 lesson writer DoD (2026-10-20):** SPW-01 PASS—독자와 source ledger를 고정했다. SPW-02 PASS—context, root cause, decision, outcome, verification, miss, future guard를 연결했다. SPW-03 PASS—Korean naturalness 기준을 적용했다. SPW-04 PASS—독립 review finding, verifier diff, 13개 RED/GREEN 변형, 41/41 결과, old-head CI의 제한을 대조했다. SPW-05 PASS—최종 read-back을 마쳤다. KO-01–KO-06 PASS; KO-07 PASS, 세 변경 문서 term audit `findings=[]`.
