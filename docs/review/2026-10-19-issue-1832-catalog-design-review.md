# Issue #1832 표준 카탈로그 설계 리뷰

## 판정

**구현 시작 가능 — 문서 설계 리뷰에서 P0=0, P1=0, 미해결 P2=0, 미해결 P3=0.** 아래에 기록한 세 건의 초기 P1은 계획을 고쳐 닫았다. 이 판정은 설계와 실행 계획에만 적용하며 generator 구현, 실제 Gradle 해석, 공개 산출물, hosted Dependabot 성공을 증명하지 않는다.

## 검토 대상과 출처

| 자료 | 검토 기준 SHA-256 | 범위 |
|---|---|---|
| `docs/superpowers/specs/2026-10-19-issue-1832-dependabot-standard-catalog-design.md` | `cf3dc67fb2089b86b4d9f96e6fc3a57d36a870dff05054a6d115c7cb03d979fd` | 요구사항, 좌표 매핑, 경로·해시·승격 계약, 수용 기준 |
| `docs/superpowers/plans/2026-10-19-issue-1832-dependabot-standard-catalog-plan.md` | `63e336eb71b79bebb50c6e027f2ff01e471f944a35f2fc57a9671e848708c22b` | 두 저장소 작업, 테스트, 증거 수집, 승인 경계 |

| 출처 | 확인한 내용 |
|---|---|
| [Dependabot Gradle file fetcher](https://github.com/dependabot/dependabot-core/blob/main/gradle/lib/dependabot/gradle/file_fetcher.rb#L969-L973) | 현재 `main`의 지원 카탈로그 경로가 `gradle/libs.versions.toml`임을 확인했다. 구현 단계에서는 이 이동 가능한 참조를 exact `dependabot-core` commit으로 고정해야 한다. |
| [Dependabot Gradle parser](https://github.com/dependabot/dependabot-core/blob/main/gradle/lib/dependabot/gradle/file_parser.rb#L2321-L2342) | catalog 선언을 순회하고 지원 가능한 버전 또는 `version.ref`가 있는 항목을 dependency로 만든다. manifest parser 인식과 Gradle resolved graph를 구분했다. |
| 중앙 기준 `scripts/catalog_candidate.py:711,845` | 현재 기준의 repository 관측 및 v1 map loader 경계를 확인했다. 계획은 v1 의미를 유지하며 정규화 API와 v2 경로를 추가한다. |
| 중앙 기준 `scripts/run-catalog-validation.py:648-705` | manifest map을 downstream consumer에 전달하는 실제 호출 경로를 확인했다. 계획 Task 3에 해당 파일과 테스트가 포함돼 있다. |
| [Python `tomllib` 문서](https://docs.python.org/3/library/tomllib.html) | 표준 라이브러리 모듈이 Python 3.11에 추가됐음을 확인했다. 문서는 TOML 쓰기 기능을 제공하지 않는다고도 명시하므로, 구현의 서식 보존 쓰기는 별도 검증 대상이다. |

## 리뷰 방식과 증거 경계

사용자가 승인한 inline 경로로 설계와 계획을 재검토했다. 독립 reviewer attestation이나 별도 subagent 결과로 표현하지 않는다. 요청 모델은 `gpt-6-luna max`이며 실제 세션의 모델 provenance는 별도 증거로 검증하지 않았다.

검토는 저장소 기준 HEAD `34c84a6115c80e3b3dfcde84be54106084259cfa`의 Projects worktree와 중앙 기준 HEAD `e9946b18bdb0022f52cd5dc8994d859d470e7bab`의 source map 경계를 참조했다. 위의 Dependabot source URL은 오늘 읽은 `main` 코드이므로, exact commit parser fixture가 통과하기 전까지는 구현 근거의 최종 pin으로 간주하지 않는다.

## 7-Tier 결과

| Tier | 검토 결과 | 상태 |
|---|---|---|
| 보안 | canonical non-symlink 입력, 정규 저장소 root, 고정 GAV 매핑, 입력 재해시, exact central SHA, signed immutable tag, hosted retry 및 merge 승인 경계가 명시돼 있다. | P0=0, P1=0 |
| 정확성·수명주기 | 원본 alias/allowlist 검증, version 참조 해석, 결정적 출력, atomic replace/read-back 및 기존 파일 복구를 정의한다. parser가 catalog declaration을 읽는 근거와 실제 dependency graph 근거를 분리한다. | P0=0, P1=0 |
| 구조·재사용 | v1 map 기본 의미를 유지하고 공통 normalized map API에 `catalog`와 `authority_catalog`을 둔다. 직접·간접 consumer 전체와 `run-catalog-validation.py` 전달 경로를 Task 3에 포함했다. | P0=0, P1=0 |
| Kotlin API·ABI·공개 계약 | Kotlin production source를 추가하지 않으며 기존 `libs.*` accessor를 보존하고 예상 다섯 accessor만 허용한다. Gradle resolution, source/API/ABI, POM, Gradle Module Metadata 차이는 자동 승인하지 않도록 계획했다. Dokka jsoup 변경은 실제 resolution 증명 대상이다. | P0=0, P1=0 |
| 테스트·호출자 | 생성기 음성·결정성·복구 테스트, map v1/v2 consumer 테스트, parser fixture, workflow 구조 검사, exact-SHA dependency submission, 영향 모듈 비교를 acceptance criteria에 연결했다. | P0=0, P1=0 |
| 성능 | 303개 원본 alias의 선형 검증이며 런타임 Kotlin 경로를 추가하지 않는다. 작은 설정 파일의 생성기에 별도 벤치마크 위험을 발견하지 못했다. | P0=0, P1=0 |
| 문서·CI·승격 | Python 3.11 하한과 Python 3.13 CI, 중앙 tag와 두 PR의 별도 승인, exact-head CI, alert 재조회·결과 분류와 hold/retry 경계를 적었다. | P0=0, P1=0 |

통합 관점에서도 v1 호환부터 v2 consumer 전환, 두 저장소의 clean committed HEAD map과 receipt, 중앙 tag, Projects 검증, 실제 alert 결과의 순서가 계획과 일치한다. tag 생성/push, 어느 PR의 merge, hosted `Try again`, cleanup은 이 문서 리뷰로 승인되지 않는다.

## 초기 P1과 수정 확인

| 초기 발견 | 계획 수정 | 현재 확인 |
|---|---|---|
| Task 3 파일 목록에서 `run-catalog-validation.py`와 테스트가 빠져 있었다. | Task 3에 두 파일과 명시적 테스트 실행을 추가하고, manifest map을 downstream consumer에 넘기는 호출 경로를 설명했다. | 수정 확인; 미해결 P1 아님 |
| Task 7에서 receipt와 lock을 검증하기 전에 map을 제거할 수 있었다. receipt가 map 원시 바이트 hash를 보유한다. | `$EVIDENCE_DIR`의 map, generator receipt, candidate lock, parser receipt를 승인된 closeout까지 보존하도록 수정했다. | 수정 확인; 미해결 P1 아님 |
| 계획의 독립 reviewer 요구가 사용자가 승인한 inline 리뷰와 맞지 않았다. | 구현과 PR 리뷰를 승인된 inline 7-Tier 경로로 정정하고, 독립 attestation 주장을 제거했다. 단일 개발자 human-reviewer subgate만 근거와 함께 N/A로 둔다. | 수정 확인; CI, review/thread read-back, fresh merge approval은 유지 |

## 남은 검증

- 중앙 생성기, 모든 map consumer와 CI가 아직 구현·검증되지 않았다.
- Projects의 표준 카탈로그 투영, alias accessor 비교, Dokka jsoup 선택, 실제 dependency graph 및 공개 산출물 비교가 아직 실행되지 않았다.
- exact `dependabot-core` commit parser fixture, exact-head CI, Projects merge SHA의 dependency-submission 결과 및 hosted alert 재실행은 미완료다.
- 현재 alert 수와 GAV 집합은 변할 수 있으므로 구현 시작 전과 retry 직전에 live GitHub에서 다시 조회해야 한다.
- 요청 모델 `gpt-6-luna max`의 runtime provenance는 검증하지 않았다. inline 검토는 독립 증명을 대신하지 않는다.

따라서 본 리뷰는 **설계 문서 게이트를 통과시켜 Task 1을 시작할 수 있음**을 뜻한다. Issue #1832의 보안 오류가 실제로 해소됐다는 판정은 아니다.

## 문서 게이트

| SPW | 확인 항목 | 결과 |
|---|---|---|
| SPW-01 | 독자·목적, source ledger, 미확인 사항을 기록했다. | PASS |
| SPW-02 | 범위, 7-Tier 결과, 수정 이력, 미완료 증거를 찾을 수 있는 구조로 나눴다. | PASS |
| SPW-03 | 한국어 자연스러움 및 문맥 용어 감사를 실행했다. | PASS |
| SPW-04 | 판단을 spec/plan hash, upstream source, 로컬 호출 경로에 연결했다. | PASS |
| SPW-05 | 저장 후 전체 문서를 다시 읽고 최종 SHA-256을 기록했다. | PASS |
