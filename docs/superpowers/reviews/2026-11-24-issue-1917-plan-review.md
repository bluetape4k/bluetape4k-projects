# 이슈 #1917 Digesters 구현 계획 통합 리뷰

## 검토 범위와 기준

- 대상 저장소: `bluetape4k-projects`, branch `feat/digest-api`, 계획 검토 시 HEAD `06634e9ebf985258f8f1c48aac3b6c6ff1853d7a`
- 이슈: [#1917 — io 공통 SHA-256 API와 Workshop 소비자 이전](https://github.com/bluetape4k/bluetape4k-projects/issues/1917), milestone `2.2.0`, assignee `debop`, 검토 시 OPEN
- 계획: `docs/superpowers/plans/2026-11-24-issue-1917-shared-digester-plan.md`, SHA-256 `fc8af928fe2f501b6c1e4c8345831073724cedbd3995f9e703c8e59125568931`
- 설계 명세: `docs/superpowers/specs/2026-11-23-shared-digester-design.md`, SHA-256 `a8926875835e2b3744c721936e0fcebf83961013f10774d4c5dbe6b1b3ebbd39`
- 명세 리뷰: `docs/superpowers/reviews/2026-11-23-shared-digester-spec-review.md`, SHA-256 `11853ab203a5ab8ae4a9887dd7d32d9838c695935cb79b4a326cc27ae0ef6ad8`
- 검토 목적: 승인된 `Digesters.SHA256` API와 기존 `TinkDigester` 호환 어댑터, Workshop 이전의 구현 순서·검증·롤백이 명세를 빠짐없이 실행하는지 확인한다.
- 경계: 이 리뷰 단계에서는 소스, 테스트, benchmark를 실행하거나 변경하지 않았다. 원격 push, PR, merge, release, issue close는 범위에 없다.

## 관점별 검토 결과

| 관점 | 검토 방식과 기준 해시 | 결과 | 최종 근거 |
|---|---|---|---|
| 성능 | 독립 `code-reviewer`, `df0ff9bb706051d263c492c1f2d23f45280862c3056e7a6ce316a2b0e12b2c0d`; 최종 변경과 무관한 benchmark 구간을 현 해시에서도 다시 확인 | 통과 | timed region, 세션 생성 비용, 결정적 입력, 환경·원시 JMH 출력 기록이 Task 5에 있다. 현 계획에서 달라진 부분은 Task 6의 호출 개수 검사뿐이다. |
| 안정성 | 초기 독립 요청 사항 반영 후 `plan-stability-r5` 검토 lane 중단; 현 계획 해시에서 인라인 fallback 재검토 | 통과 | Task 2–3은 세션 성공·재사용·범위 실패·provider 실패·동시 격리 계약을 정하고, Task 6–7은 취소 전파와 순차 Testcontainers 실행·경로 단위 rollback을 다룬다. |
| 보안 | 초기 독립 요청 사항 반영 후 `plan-security-r5` 검토 lane 중단; 현 계획 해시에서 인라인 fallback 재검토 | 통과 | Task 1–4와 6에서 provider/algorithm parity, strict Base64 예외 경계, MD5/SHA-1 제한, MAC 분리, caller-owned 배열, zeroization 보존을 확인한다. |
| 운영 | `plan-ops-r6` 검토 lane 중단; 현 계획 해시에서 인라인 fallback 재검토 | 통과 | Task 1과 6에 clean baseline, write-ahead 경로 목록, 모듈별 rollback 및 sequential tests가 있다. PR·게시·배포 경계도 명시한다. |
| 개발자/API | `plan-api-r6` 검토 lane 중단; 현 계획 해시에서 인라인 fallback 재검토 | 통과 | Task 2–5가 Kotlin/Java 계약, 기존 ABI, 의존 방향, 양 locale README와 실제 descriptor 확인을 순서대로 연결한다. |
| 호출자 | 독립 `writer`, 계획 SHA-256 현 해시와 검토 전후 동일 | 통과 | `Digesters` 이름, Kotlin/Java 예제, framing/output 보존, 9개 모듈·29개 파일 범위를 확인했다. |
| 통합 | 주 세션에서 설계 명세, 구현 계획, repo/workspace 규칙을 대조 | 통과 | 명세 acceptance 14개 항목이 Traceability 표 및 Task 1–8에 연결되고 선행 산출물 의존이 뒤집히지 않는다. |

중단된 네 review lane은 독립 통과로 기록하지 않는다. 보안, 안정성, 운영, API 관점의 인라인 검토는 workflow의 bounded fallback이며 독립 리뷰 증거와 구분한다. 성능 독립 리뷰는 최종 계획 해시 직전 버전에서 수행됐다. 현재 해시에서 바뀐 유일한 계획 내용은 Task 6의 regex call count 제거이며 성능 구간은 바뀌지 않았다.

## 발견 사항과 조치

| 우선순위 | 관점 | 발견 및 근거 | 조치와 재검토 |
|---|---|---|---|
| P1 | 호출자 | 이전 계획은 `Digesters.SHA256.newSession()`과 뒤의 `DigestSession.digest*()`를 각각 세면서 정규식 합계가 35여야 통과했다. 한 논리 호출이 두 번 집계될 수 있었다. | Task 7 검증 스크립트에서 총 호출 수 조건을 제거하고 파일별 API 사용 여부만 검사한다. 35개 논리 호출은 Task 6의 35-row parity 표로만 증명한다. 독립 호출자 재검토는 현 계획 해시에서 이 수정을 확인하고 추가 P0/P1 없이 통과했다. |
| 보안·안정성 | 보안·안정성 | 초기 계획은 provider/예외 경계, 세션 상태 전이, 동시 격리, 취소 전파, rollback 및 benchmark 증거를 충분히 고정하지 않았다. | Task 1–7에 요청 사항을 반영했다. 인라인 재검토에서 구현 전에 각 계약을 증명할 위치, 테스트, 출력 보존 또는 rollback 경로를 확인했다. |
| 성능 | 성능 | 초기 benchmark task는 측정 구간과 reproducibility/raw output 보존을 충분히 지정하지 않았다. | Task 5에 네 측정 경로, timed region, 세션 초기화 비용 귀속, 결정적 payload, 전체 실행 환경, fork별 오차와 원시 JMH 출력을 지정했다. 독립 performance reviewer가 수정 계획을 승인했다. |

## 구현 전 확인할 parity 기준

명세는 Workshop 범위를 9개 모듈, 29개 production 파일, 35개 호출 표현으로 고정한다. 계획은 source 변경 전에 `docs/review/2026-11-24-issue-1917-digest-migration-review.md`에 한 행당 한 논리 호출인 parity 표를 작성하도록 요구한다. 각 행에는 파일/함수 locator, 기존 호출, 순서가 있는 입력/framing, 출력 표현, 결정적 테스트, 이전·이후 값을 기록한다. 독립 호출자 리뷰가 지적한 대로 이 표는 현재 계획 산출물이 아니라 구현 전 선행 산출물이다. 35개의 고유 locator와 바이트/문자열/wire 결과를 대조하기 전에는 source 수정을 시작하지 않는다.

완결성 스크립트는 appendix의 정확한 29개 경로, 각 파일의 `Digesters.SHA256` 사용, 잔존 `DigestUtils`/`MessageDigest`/Tink digest 구현 부재만 검사한다. 해당 검사는 35개 논리 호출 수를 세지 않는다. 실행한 문서 정적 검사에서는 21개 shell block과 5개 Python block의 구문, 29개 고유 경로 및 이 완결성 기준을 확인했다. 문서 변경의 `git diff --check`와 한국어 용어 검사는 통과했다.

## 통합 판단

설계 명세와 계획은 같은 `Digesters` 이름과 `io.bluetape4k.io.digest` 계약을 사용한다. 계획은 public API와 기존 Tink ABI, `io/tink -> io/io` 의존 방향, strict Base64와 legacy matcher 차이, 소비자 framing/output, 키와 MAC 경계, 세션 수명, 동시성·취소, benchmark, 모듈 검증 및 rollback을 작업과 증거에 연결한다. Workshop의 기존 catalog/settings 변경을 제외하고 외부 게시·PR·merge는 실행하지 않는다.

35행 parity 표는 아직 작성되지 않았으므로 구현의 선행조건으로 남는다. 이 조건은 계획에 명시되어 있어 계획 승인 자체를 막지 않는다. 그 밖에 해결되지 않은 P0/P1 또는 계획 단계 P2/P3은 없다.

**Inline verdict: PASS; P0=0, P1=0.**

## Writer DoD

- **SPW-01 — 근거 고정:** 리뷰 대상, 독자, 목적, issue 상태, branch/HEAD, spec·plan·기존 review의 정확한 hash와 외부 side effect 경계를 위에 기록했다.
- **SPW-02 — review 계약:** 관점별 결과, 우선순위별 발견·조치, 계획 traceability, 남은 구현 전 parity 조건과 명시 verdict를 포함했다.
- **SPW-03 — 한국어 기술 문체:** 설명은 한국어로 쓰고 API 이름, 경로, 명령, hash, 우선순위와 GitHub 식별자는 그대로 보존했다. 일반 용어 감사는 0건이다.
- **SPW-04 — 기술 의미와 추적성:** 최종 spec acceptance와 계획 Task 1–8 및 검증 명령을 대조했다. 미래에 작성할 parity 표를 현재 증거처럼 주장하지 않았다.
- **SPW-05 — 최종 읽기:** Markdown 표·목록·경로·코드 토큰을 다시 읽고 빈 placeholder와 `git diff --check` 오류가 없는지 확인했다.
