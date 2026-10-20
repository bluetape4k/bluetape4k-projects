# Issue #1832 Projects 구현 7-Tier 리뷰

## 판정

현재 로컬 변경에 대한 inline 구현 리뷰를 통과했다. 변경 자체에서 P0=0, 미해결 P1=0, 미해결 P2=0이다. 이 판정은 merge 준비나 보안 alert 해소를 뜻하지 않는다. GitHub 이슈 #1832는 OPEN이고 milestone 2.1.0이다. live alert #235, GHSA-27j2-h3m2-8237은 CRITICAL이며 아직 OPEN이다.

## 검토 범위와 기준

- 저장소: bluetape4k/bluetape4k-projects, 브랜치 fix/issue-1832-dependabot-standard-catalog.
- 기준: origin/develop 공통 조상 34c84a6115c80e3b3dfcde84be54106084259cfa와 구현 작업 commit 69b19b6865fd5ae8853fba583896e409f6d1cf0d 이후의 미커밋 diff.
- 검토 파일: build.gradle.kts, .github/workflows/ci.yml, .github/workflows/dependency-submission.yml, AGENTS.md, CLAUDE.md, 프로젝트 원본·생성 catalog와 allowlist, workflow 검증기 및 테스트.
- 방식: 사용자가 승인한 inline review. 독립 reviewer나 subagent attestation은 포함하지 않는다. 요청 모델은 gpt-6-luna max이며 실제 runtime provenance는 확인하지 않았다.
- 외부 근거: [GHSA-27j2-h3m2-8237](https://github.com/advisories/GHSA-27j2-h3m2-8237)는 FreeMarker 2.3.34 이하를 영향 범위, 2.3.35를 수정 버전으로 기록한다. [Apache FreeMarker 2.3.35 릴리스 노트](https://freemarker.apache.org/docs/versions_2_3_35.html)도 template loader의 상위 경로 이동 방어 변경을 설명한다.

## 7-Tier 결과

| Tier | 결과 | 판정 |
|---|---|---|
| 보안 | 프로젝트 표준 catalog에 매핑된 FreeMarker 최소 버전은 2.3.35이고, root build는 중앙 버전과 같게 Dokka 구성의 FreeMarker를 강제한다. dotted-release 비교는 예상 형식이 아닌 버전을 거부한다. 현재 #235 alert는 develop에서 계속 OPEN이며 hosted Dependabot 결과는 아직 없다. | 변경 finding P0/P1=0; 실제 alert 해소 PENDING |
| 정확성·수명주기 | 기존 303개 원본 alias를 versionless source catalog에 보존하고 10개 대상만 생성 catalog에서 version.ref로 읽힌다. allowlist·receipt·lock과 deterministic output 검사로 원본 변경 및 stale output을 잡는다. | P0/P1=0 |
| 구조·재사용 | 원본 catalog는 libs.project.versions.toml, Dependabot 표준 경로는 생성한 libs.versions.toml이다. 중앙 mapping과 generator가 authority를 유지하며 Projects workflow가 중앙 source checkout을 SHA로 확인한다. | P0/P1=0 |
| Kotlin API·ABI·공개 계약 | Kotlin/Java production source 변경은 없다. 새 accessor 다섯 개는 build-time catalog accessor이며 기존 alias를 보존한다. 13개 게시 모듈의 POM 및 module.json 26개가 baseline과 byte-identical했다. 별도 ABI dump/consumer fixture는 N/A이며 수행하지 않았다. | P0/P1=0; ABI 도구 N/A |
| 테스트·호출자 | workflow contract 8개, Projects verifier, actionlint, Ruff, Python compile, diff check가 통과했다. 실제 Dokka 내부 runtime resolver 98개에서 `dependencyInsight` 196건이 중앙 jsoup `1.23.2`/FreeMarker `2.3.35`를 선택했고, 중앙 catalog guard의 오래된·dynamic·range·rich-range·qualifier·malformed·empty 입력 9개가 모두 거부됐다. 영향 모듈 classes와 두 mock server Jib 빌드도 앞선 검증에서 통과했다. | 로컬 검사 PASS |
| 성능 | root 설정 시 dotted version을 두 번 비교하고 각 Dokka configuration에서 두 좌표를 검사한다. 설정 단계의 작은 선형 작업이며 런타임 Kotlin 호출 경로를 추가하지 않는다. 벤치마크는 하지 않았다. | 성능 P2 없음 |
| Ops·CI·통합 | Dependency Submission은 github.sha를 checkout하고 HEAD를 비교하며 SHA별 concurrency를 사용하고 취소하지 않는다. Central PR exact-head CI run 39295843422는 9/9 job 성공, merge SHA `dcc7fa7557d3c0548b9e3fc28c48877827ea2f74`의 post-merge CI run 39359607639는 8 success/0 failure이고 push 조건부 publication contract만 skip됐다. 해당 contract는 PR exact-head에서 통과했다. Projects는 현재 기존 Central SHA `e9946b18bdb0022f52cd5dc8994d859d470e7bab`을 고정 중이므로 서명 tag와 새 pin 전에는 Projects exact-head CI를 실행할 수 없다. | Central CI PASS; Projects exact-head CI PENDING |

## 수정한 리뷰 발견

| 심각도 | 발견 | 수정 및 회귀 근거 |
|---|---|---|
| P1 | Projects runner는 별도 job의 parser receipt를 공유받지 못하지만 generator --check는 parser receipt를 요구했다. | workflow contract 테스트에서 RED를 확인한 뒤 같은 catalog-governance job 안에서 pinned Dependabot Core parser를 먼저 실행하도록 고쳤다. workflow contract 8개와 actionlint 통과. |
| P2 | 중앙 parser job의 receipt는 Projects runner와 공유되지 않아 그 receipt를 대체 입력으로 쓰면 검증 경계가 잘못 결속된다. | Projects CI가 mapping의 full commit을 checkout하고 자체 parser receipt를 발행한다. verifier가 parser 실행 순서를 검사한다. |

현재 남은 코드 P0/P1은 없다. 위 P1/P2는 수정된 선행 발견이며 미해결 finding이 아니다.

## 검증 결과와 제한

- Projects workflow contract: 8 tests PASS.
- catalog workflow verifier: PASS.
- actionlint: 두 변경 workflow PASS.
- Ruff: 새 verifier와 테스트 파일 0 finding; 기존 수정 파일에서 새 finding 0.
- Python compile 및 git diff --check: PASS.
- Gradle configuration-cache help, 98개 Dokka configuration/196개 `dependencyInsight` 보고서의 선택 버전, 잘못된 catalog 입력 9개 거부, 영향 모듈 classes, 두 mock server Jib 빌드, 13개 모듈 POM/GMM 비교: 로컬 검증에서 PASS.
- Ruby 2.6.10/Bundler 1.17.2 환경에서는 pinned Dependabot parser를 직접 실행할 수 없었다. PR CI가 pinned upstream commit에서 실행해야 한다.
- Central exact-head 및 post-merge CI, merge-SHA Dependency Submission은 성공했다. Projects exact-head CI, signed central tag, Projects merge, hosted Dependabot Try again은 아직 실행되지 않았다. Issue #1832 해결 여부를 주장하지 않는다.

## Writer DoD

| 검사 | 결과 |
|---|---|
| SPW-01 목적·독자·근거·미확인 사항 | PASS |
| SPW-02 리뷰 구조·심각도·수정·남은 검증 | PASS |
| SPW-03 한국어 기술 문체 및 용어 일관성 | PASS |
| SPW-04 파일·테스트·이슈·advisory 근거 대조 | PASS |
| SPW-05 최종 read-back 및 workflow DoD 기록 | PASS |
| KO-01..KO-06 사실·문체·용어·표·링크 점검 | PASS |
| KO-07 contextual terminology audit | PASS, 최종 audit 결과를 계획에 기록 |
