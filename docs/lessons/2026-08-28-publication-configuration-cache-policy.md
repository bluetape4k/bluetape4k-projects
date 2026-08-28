# Publication 검증은 strict POM 검사와 no-cache 실행을 함께 고정한다

## 맥락

Milestone `2.0.0`의 [#1565](https://github.com/bluetape4k/bluetape4k-projects/issues/1565)는
공통 Maven publication 경로에서 서로 다른 두 실패가 함께 드러난 사례다.

- `:bluetape4k-core:checkPomFileForBluetape4kPublication`는 developer의
  `<organization>`과 `<organizationUrl>`이 없어서 실패했다.
- `GenerateMavenPom` task는 configuration cache가 활성화되면 `Configuration`,
  `ConfigurationContainer`, `DependencyHandler`, `Project`를 직렬화하지 못했다.

기존 CI와 배포 workflow는 POM을 생성한 뒤 Maven model validator를 실행했지만,
Kotlin Gradle plugin이 제공하는 strict POM 검사 task는 호출하지 않았다. publication
task에 `notCompatibleWithConfigurationCache`가 선언돼 있었어도 명령에서 configuration
cache를 끄지 않으면 Gradle은 문제를 기록하고 cache entry를 폐기했다.

## 결정

- `applyBluetape4kPomMetadata`를 developer 메타데이터의 단일 설정 지점으로 유지하고
  `organization=Bluetape4k`, `organizationUrl=https://github.com/bluetape4k`를 추가한다.
- CI, RELEASE, SNAPSHOT의 `Validate publication metadata` 단계는 다음 세 task를 같은
  명령에서 실행한다.
  - `generatePomFileForBluetape4kPublication`
  - `checkPomFileForBluetape4kPublication`
  - `generateMetadataFileForBluetape4kPublication`
- publication 검증과 실제 배포 명령은 `--no-configuration-cache`를 명시한다. task의
  compatibility 선언을 CLI 정책의 대체물로 취급하지 않는다.
- `scripts/test_release_workflow_policy.py`가 세 workflow의 검증 task와 no-cache 옵션을
  읽어 정책 누락을 PR 단계에서 차단한다.

## 결과

공통 메타데이터 변경 뒤 core의 POM 생성과 strict 검사가 모두 통과했다. 생성된 POM에서
두 organization 필드도 직접 확인했다. 전체 publication 검증은 POM 76개와 module
metadata 76개를 생성했고, 모든 strict POM 검사 task가 성공했다.

README의 RELEASE와 SNAPSHOT 예시도 동일한 configuration-cache 정책을 사용한다. 로컬
복사 실행과 GitHub Actions 실행 사이에서 cache 사용 여부가 달라지는 경로를 제거했다.

## 검증

- 결함 재현:
  - core strict POM 검사 실패: `<organization>`, `<organizationUrl>` 누락.
  - configuration cache 저장 문제 8건, 고유 유형 4개:
    `Configuration`, `ConfigurationContainer`, `DependencyHandler`, `Project`.
- `python3 -m unittest scripts.test_release_workflow_policy -v`: 36개 테스트 통과.
- `./gradlew -p buildSrc test --no-daemon --no-configuration-cache --no-build-cache`:
  성공.
- core `generatePomFileForBluetape4kPublication`와
  `checkPomFileForBluetape4kPublication`: 성공.
- 전체 publication 생성·검사: `BUILD SUCCESSFUL`, 419개 task 중 227개 실행,
  192개 up-to-date.
- `validate_poms.rb`: POM 76개, dependency 31,598개, 실패 0.
- `validate_module_metadata.rb`: metadata 76개, variant 157개, dependency 1,487개,
  실패 0.
- publication Ruby 단위 테스트: 16개 test, 38개 assertion, 실패·오류·skip 0.
- `actionlint`, `git diff --check`: 통과.

## 놓친 점과 주의사항

- 실패한 판단: GitHub Actions shell을 일부만 해석하는 범용 parser로 publication
  계약을 증명할 수 있다고 판단했다. 독립 리뷰가 필수 task를 `echo`, 다른 인자의
  부분 문자열, `&&`·`;` 뒤 명령, Gradle 옵션 값, 여러 형식의 heredoc 본문으로 옮겨도
  정책 검사가 통과하는 false green을 차례로 재현했고, raw workflow 문자열의 비실행
  YAML scalar에 exact step 전체를 넣는 우회도 확인했다. 이에 따라 shell 문법을 계속
  확장하거나 raw text를 세지 않고, YAML block scalar 경계를 제외한 실제
  `jobs.*.steps[].name/run` 구조만 추출해 지정된 CI·RELEASE·SNAPSHOT job의 허용된
  명령과 정확히 비교하도록 수정했다. exact step을 다른 job으로 옮기거나 quoted key를
  포함한 step-level `if`/`continue-on-error`, release job-level guard로 실행을 무력화하는
  경우도 false green으로 재현해 거부한다. SNAPSHOT publish job은 nightly 검증 결과를
  확인하는 기존 `if` 조건과 정확히 일치할 때만 허용한다. 추가 publication 호출은 실제
  `jobs.*.steps[].run` 블록을 shell token으로 분리해 정확히 한 번만 허용하고, 주석·heredoc
  본문·`echo`의 동일 문자열은 실행 호출로 세지 않는다. 앞으로 workflow 정책은 지정 job의
  실행 guard와 step 구조가 허용 계약에 정확히 일치하는 경우에만 통과시키며 위 우회 형태를
  음성 fixture로 유지한다.
- `generatePomFileForBluetape4kPublication` 성공은 strict POM 계약 통과를 뜻하지 않는다.
  생성과 `checkPomFileForBluetape4kPublication`을 함께 실행해야 한다.
- `notCompatibleWithConfigurationCache`는 configuration cache를 자동으로 끄는 옵션이
  아니다. publication 명령에서 `--no-configuration-cache`를 생략하지 않는다.
- #1562의 TenantContext artifact 세 개는 아직 `develop`에 없으므로 이 변경의 독립
  worktree에서는 검증하지 않았다. #1562가 공통 변경을 통합한 뒤 세 artifact의
  generate/check task를 별도로 실행해야 한다.

## 향후 지침

- 새 publication workflow나 문서 명령을 추가할 때 generation, strict POM 검사,
  module metadata 생성, `--no-configuration-cache`를 하나의 계약으로 검토한다.
- developer 메타데이터를 module별 build script에 복제하지 않는다.
  `applyBluetape4kPomMetadata`에서 공통 적용한다.
- configuration-cache 호환성을 다시 시도하려면 `Project`와 Gradle model 객체가
  `GenerateMavenPom.mavenPomSpec`에 캡처되는 경계를 먼저 제거하고, 문제 수 0과 cache
  재사용을 별도 이슈에서 증명한다.

## 문서 SPW 감사

- SPW-01: PASS — 대상 독자는 Gradle publication과 release workflow 유지보수자다.
  근거는 #1565, 재현 로그, `PublishingSigningSupport.kt`, 세 workflow, 정책 테스트와
  생성된 publication 결과다.
- SPW-02: PASS — 맥락, 결정, 결과, 검증, 놓친 점, 향후 재발 방지 규칙을 포함한다.
- SPW-03: PASS — 한국어 기술 문체를 사용하고 task, option, Gradle type, URL과 수치를
  원문 그대로 보존했다.
- SPW-04: PASS — 실패 유형, task 수, POM·metadata 수와 정책 파일을 현재 실행 결과에
  대조했다. TenantContext 검증 공백을 명시했다.
- SPW-05: PASS — Markdown 구조, 명령, 수치, 링크와 재실행 조건을 다시 읽어 확인했다.

## 한국어 자연스러움 감사

- KO-01: PASS — 식별자, 명령, URL, 수치와 미검증 범위를 보존했다.
- KO-02: PASS — 일반적인 중요성 표현 대신 실패 유형과 검증 결과를 적었다.
- KO-03: PASS — 번역투와 불필요한 명사화를 줄이고 원인과 결정을 직접 서술했다.
- KO-04: PASS — `publication`, `strict POM 검사`, `configuration cache` 용어를
  일관되게 사용했다.
- KO-05: PASS — 장식적 비유와 홍보 표현을 사용하지 않았다.
- KO-06: PASS — 제목, 본문, 목록, 링크와 code token을 확인했다. 단일 한국어 lesson이라
  대응 locale은 해당하지 않는다.
- KO-07: PASS — `audit-korean-terms.mjs`로 README, plan, lesson과 review를 포함한
  한국어 변경 파일 5개를 검사했고 용어 충돌이 없었다.
