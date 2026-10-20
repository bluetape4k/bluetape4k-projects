# Issue #1832 Projects 구현 7-Tier 리뷰

## 초기 inline 판정 (이전 검토 기준 코드 상태)

이 섹션은 앞선 구현 상태를 대상으로 한 inline 리뷰 기록이다. 아래의 exact-head 독립 리뷰가 후속 코드 상태를 판단하며, 이 초기 판정을 현재 후보의 결론으로 사용하지 않는다. 어느 판정도 merge 준비나 보안 alert 해소를 뜻하지 않는다.

## 후속 독립 리뷰 (2026-10-20, PR HEAD `a702d7f1b2573a8d03ca7e7382d780a99d28528d`)

### 판정

독립 `code-reviewer`는 `REQUEST CHANGES`를 반환했다. P0=0, P1=0, P2=2, P3=0이다. 두 P2는 검증기가 YAML 설정의 실제 적용 스코프와 실행 step의 셸 데이터 흐름을 보존하는지 확인하지 않는 문제다.

| Tier | 판정 | 근거 |
|---|---|---|
| 보안·요구사항 | FAIL | `PROJECTS_EXPECTED_SHA`의 job/step env override와 중복 key가 검증을 우회할 수 있다. |
| 정확성 | FAIL | ancestry 명령 문자열이 merge job 어딘가에 있는지만 보고, 지정된 실행 step과 PR SHA 값의 실제 사용을 확인하지 않는다. |
| API·ABI | PASS / N/A | 변경 범위는 Python workflow verifier와 YAML/테스트이며 Kotlin/Java public API·ABI 변경은 없다. |
| 테스트 | PARTIAL | 기존 negative tests는 top-level env 문자열과 step env 값은 확인하지만 스코프 override와 셸 data flow는 잠그지 않았다. |
| CI·도구체인 | PARTIAL | `actionlint`는 로컬에서 통과하지만 Projects CI workflow에는 actionlint 검사가 없다. 따라서 중복 YAML key는 verifier 자체에서 거부해야 한다. |
| 성능 | PASS | workflow 검증만 바꾸며 library runtime 경로는 추가하지 않는다. |
| 문서·유지보수성 | PARTIAL | regex 기반 검증기가 실행 의미를 충분히 구분하지 못해 회귀 보호가 부족하다. |

### 발견과 처분

| 심각도 | 위치 | 재현된 문제 | 현재 작업 트리의 수정 |
|---|---|---|---|
| P2 | `scripts/verify-catalog-workflows.py:73-80, 161-164` | top-level expected SHA만 확인하고 job/step env override는 거부하지 않는다. top-level duplicate key도 verifier가 통과한다. | 유일한 top-level 값을 요구하고 모든 job/step env scope의 같은 key를 거부한다. 중복 정의·job override·checkout step override 테스트를 추가했다. |
| P2 | `scripts/verify-catalog-workflows.py:332-357`, `.github/workflows/ci.yml:463-475` | merge-base 명령이 disabled step으로 이동해도 job 전체의 문자열 검사에 걸리지 않는다. `PR_HEAD_SHA`/`PR_BASE_SHA` 셸 재할당이나 `GITHUB_ENV` 전달도 차단되지 않는다. | 이름이 고정된 literal `run: |` step에서 순서대로 실행되는 명령을 요구하고 step/job 조건·`continue-on-error`, 변수 재할당, `GITHUB_ENV`를 거부한다. |

리뷰어가 `PROJECTS_EXPECTED_SHA`의 실제 값을 `${{ github.sha }}`로 바꾸고 올바른 값을 comment에 남긴 경우, 그리고 PR head/base 값을 merge SHA로 다시 할당한 경우를 확인했다. 각 변형에서 기존 verifier는 빈 오류 목록을 반환했고 `actionlint`도 중복 env key 외의 의미 우회를 잡지 못했다. 중복 key는 로컬 `actionlint`가 거부했지만 이 검사는 현재 CI에서 실행되지 않는다.

### 수정 및 검증 상태

현재 작업 트리에서 먼저 8개 신규 테스트에 걸친 13개 변형을 실행했다. 기존 코드에서는 각 변형이 `validate_workflows()`의 빈 오류 목록으로 실패했다. 수정 뒤 전체 workflow contract 41/41, verifier, Ruff lint, Python compile, `git diff --check`가 통과했다. `actionlint`는 변경 없는 workflow 파일을 검사하는 도구이므로 새 후보에서도 최종 검증 시 다시 실행한다. 두 Python 파일의 `ruff format --check`는 PR HEAD `a702d7f1` 원본과 현재 파일 모두 실패하며, 현재 `ruff format --diff`에는 신규 코드 줄이 남지 않았다.

`a702d7f1`의 exact-head CI run `39420175917`은 28/28 job 성공으로 끝났지만, 이 run은 현재 수정 전 코드만 검사했다. 현재 수정은 아직 commit/push되지 않았으므로 새 exact-head CI 및 수정본 독립 재리뷰는 PENDING이다. Issue #1832와 CRITICAL alert #235도 계속 OPEN이다.

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

앞선 inline 검토 범위에서는 보고된 P1/P2를 수정한 뒤 남은 코드 P0/P1이 없다고 판정했다. 이 결론은 후속 `a702d7f1` 독립 리뷰 finding에 적용되지 않는다.

## 초기 inline 검증 결과와 제한

- Projects workflow contract: 8 tests PASS.
- catalog workflow verifier: PASS.
- actionlint: 두 변경 workflow PASS.
- Ruff: 새 verifier와 테스트 파일 0 finding; 기존 수정 파일에서 새 finding 0.
- Python compile 및 git diff --check: PASS.
- Gradle configuration-cache help, 98개 Dokka configuration/196개 `dependencyInsight` 보고서의 선택 버전, 잘못된 catalog 입력 9개 거부, 영향 모듈 classes, 두 mock server Jib 빌드, 13개 모듈 POM/GMM 비교: 로컬 검증에서 PASS.
- Ruby 2.6.10/Bundler 1.17.2 환경에서는 pinned Dependabot parser를 직접 실행할 수 없었다. PR CI가 pinned upstream commit에서 실행해야 한다.
- Central exact-head 및 post-merge CI, merge-SHA Dependency Submission은 성공했다. 이 inline 기록을 작성할 당시 Projects exact-head CI, signed central tag, Projects merge, hosted Dependabot Try again은 아직 실행되지 않았다. 이후 `39420175917`이 `a702d7f1`에서 성공했지만 그 뒤 발견한 verifier P2는 해결하지 못한 코드 상태였다. Issue #1832 해결 여부를 주장하지 않는다.

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

**후속 독립 리뷰 기록 writer DoD (2026-10-20):** SPW-01–05와 KO-01–07을 이 문서의 후속 독립 review 섹션에 별도로 적용했다. 독립 reviewer의 exact head, `REQUEST CHANGES` 판정, P2 두 건과 세부 위치를 review 원문 및 현재 verifier/tests와 대조했다. 로컬 41/41 결과와 old-head CI 성공을 구분했고, 수정본 exact-head CI 및 독립 재리뷰는 PENDING으로 유지했다. 세 문서 read-back과 term audit `findings=[]`를 확인했다.
