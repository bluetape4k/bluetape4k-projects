# Issue #1637 Jackson 입력 제한 수정·검증 기록

## 목표와 승인 범위

Jackson2 실제 의존성을 catalog의 `2.22.2`에 맞추고 Jackson3 `3.2.2`와 함께 입력 제한을 검증한다. 독자는 유지보수자이며, 대화에서 승인된 실행 계획과 현재 검증 근거를 연결하는 문서다. 상세 체크리스트가 파일로 남지 않았던 부분을 사후 보완한다. 이전 실행 순서를 새로 수행한 것처럼 표시하지 않는다.

작업 경로는 `fix/issue-1637-jackson-input-constraints` 격리 worktree다. 변경은 두 Gradle 파일, 두 모듈의 `StreamReadConstraintsTest.kt`·`DatabindInputConstraintsTest.kt`, 이 문서와 lesson이다. 실패 시 두 selector 수정만 되돌려 RED를 재현한 뒤 원인을 수정하고 관련 테스트를 다시 실행한다. 기본 mapper 설정·새 API·새 의존성·배포·merge는 범위 밖이다.

## 실행 체크리스트

- [x] **WF-00 — 기준 정보 확인**
  - **Action:** 사용자·workspace·worktree AGENTS 경로와 적용 범위를 확인한다.
  - **Evidence:** 각 계층을 읽었으며 repo overlay와 한국어 문서 정책을 적용했다.
  - **Failure:** 해당 단계와 종속 검증을 다시 확인한다.
- [x] **WF-01 — Type C 분류**
  - **Action:** 실제 버전과 실패 경로로 결함을 분류한다.
  - **Evidence:** jackson 키가2.22.1을 선택하는 결함과 #1637 범위를 확인했다.
  - **Failure:** 해당 단계와 종속 검증을 다시 확인한다.
- [x] **WF-02 — 승인 계획 유지**
  - **Action:** 격리·RED·수정·검증·검토 순서를 유지한다.
  - **Evidence:** 대화에서 승인된 계획을 아래 실행·검증 기록으로 보완한다.
  - **Failure:** 해당 단계와 종속 검증을 다시 확인한다.
- [x] **WF-03 — 계획 승인 확인**
  - **Action:** 사용자의 승인과 계속 요청을 확인한다.
  - **Evidence:** 현재 대화의 승인 및 계속해. PR 생성·merge는 별도 경계다.
  - **Failure:** 해당 단계와 종속 검증을 다시 확인한다.
- [x] **WF-04 — 실행 계약 적용**
  - **Action:** bugfix·Kotlin·TDD·writer 및 공통 계약을 적용한다.
  - **Evidence:** 해당 SKILL.md와 필요한 참조를 읽고 기존 테스트 패턴을 사용했다.
  - **Failure:** 해당 단계와 종속 검증을 다시 확인한다.
- [x] **WF-04A — 실행 기록**
  - **Action:** 지원 helper로 격리 실행을 관리한다.
  - **Evidence:** run 20260905T145235Z-bdda3aa2, component jackson-constraints, mutation-check PASS.
  - **Failure:** 해당 단계와 종속 검증을 다시 확인한다.
- [x] **WF-05 — 검증 순서**
  - **Action:** RED 뒤 수정하고 영향 범위를 검사한다.
  - **Evidence:** 아래 로그와 최종 재검증 순서를 확인했다.
  - **Failure:** 해당 단계와 종속 검증을 다시 확인한다.
- [x] **WF-06 — 기록 보완**
  - **Action:** 상세 체크리스트 누락을 보완하고 영향 검증을 갱신한다.
  - **Evidence:** 이 문서는 사후 보완 기록이다. 최종 검증·검토·diff 확인을 다시 수행한다.
  - **Failure:** 해당 단계와 종속 검증을 다시 확인한다.
- [x] **CG-01 — 범위 재확인**
  - **Action:** 기준 정보·승인·diff를 확인한다.
  - **Evidence:** #1637 이외 기능·정책은 변경하지 않는다.
  - **Failure:** 해당 단계와 종속 검증을 다시 확인한다.
- [x] **CG-02 — 이력·현재 상태**
  - **Action:** GNO와 live issue를 조회한다.
  - **Evidence:** bluetape4k-github의 #1637 및 bluetape4k-docs Jackson 검색, gh issue view 확인.
  - **Failure:** 해당 단계와 종속 검증을 다시 확인한다.
- [x] **CG-03 — 격리**
  - **Action:** develop과 사용자 변경을 보존한다.
  - **Evidence:** fix/issue-1637-jackson-input-constraints, base 17bad37bc817df585733ec01b65003defd5df3c3.
  - **Failure:** 해당 단계와 종속 검증을 다시 확인한다.
- [x] **CG-04 — 정책·언어**
  - **Action:** 기술 식별자를 유지하고 문서는 한국어로 작성한다.
  - **Evidence:** 공개 API·권한·배포 설정 변경 없음.
  - **Failure:** 해당 단계와 종속 검증을 다시 확인한다.
- [x] **CG-05 — 기존 패턴**
  - **Action:** 새 의존성 없이 기존 mapper와 검증 helper를 사용한다.
  - **Evidence:** assertFailsWith, 기존 async API, catalog jackson2 키 재사용.
  - **Failure:** 해당 단계와 종속 검증을 다시 확인한다.
- [x] **CG-06 — 문서 계약**
  - **Action:** 변경 범위와 미검증 범위를 기록한다.
  - **Evidence:** 이 계획과 lesson. 공개 API·README·모듈 등록 변경 없음.
  - **Failure:** 해당 단계와 종속 검증을 다시 확인한다.
- [x] **CG-07 — 동작 검증**
  - **Action:** RED/GREEN 및 관련 모듈 검증을 실행한다.
  - **Evidence:** 최종 RED11실패; 새46개와 전체992개 GREEN.
  - **Failure:** 해당 단계와 종속 검증을 다시 확인한다.
- [x] **CG-08 — 고비용 검사 직렬화**
  - **Action:** 모듈 테스트를 순차 실행한다.
  - **Evidence:** Jackson2 이후 Jackson3 실행. 전역 build의 별도 Test 발견 후 중단하고 컴파일만 재실행.
  - **Failure:** 해당 단계와 종속 검증을 다시 확인한다.
- [x] **C-01 — 원인 입증**
  - **Action:** 실제 runtime 버전을 조회한다.
  - **Evidence:** dependency management 및 Jackson2 BOM이 이전 jackson 키를 사용했다.
  - **Failure:** 해당 단계와 종속 검증을 다시 확인한다.
- [x] **C-02 — 이슈 범위**
  - **Action:** 기존 이슈 metadata를 확인한다.
  - **Evidence:** #1637, debop, milestone2.1.0, test/security/dependencies.
  - **Failure:** 해당 단계와 종속 검증을 다시 확인한다.
- [x] **C-03 — 회귀 RED**
  - **Action:** 기존 버전 선택으로 최종 회귀 테스트를 실행한다.
  - **Evidence:** /tmp/issue-1637-final-red.log:23개 중11개 의도한 동작 실패.
  - **Failure:** 해당 단계와 종속 검증을 다시 확인한다.
- [x] **C-04 — 최소 수정**
  - **Action:** 두 build 파일의 Jackson2 버전 키를 정렬한다.
  - **Evidence:** 전역43개 selector와 모듈 BOM1개. production mapper 변경 없음.
  - **Failure:** 해당 단계와 종속 검증을 다시 확인한다.
- [x] **C-05 — GREEN·영향**
  - **Action:** 전체 모듈 테스트와 전역 컴파일을 확인한다.
  - **Evidence:** Jackson2 499, Jackson3 493, 실패·오류·skip0; 전역356tasks 성공.
  - **Failure:** 해당 단계와 종속 검증을 다시 확인한다.
- [x] **C-06 — 교훈 색인**
  - **Action:** lesson을 읽고 GNO 검색을 검증한다.
  - **Evidence:** docs/lessons/2026-09-06-issue-1637-jackson-input-constraints.md; gno update·embed 성공, issue1637 검색1건 확인.
  - **Failure:** PENDING으로 남기고 종속 배포를 시작하지 않는다.
- [x] **CG-09 — 교훈 완료**
  - **Action:** 재발 방지·writer 증거를 확인하고 commit에 포함한다.
  - **Evidence:** selector·YAML fixture·Path·deprecated API·build 제외 규칙을 기록했다. GNO 검색 확인 및 로컬 구현 commit에 포함했다.
  - **Failure:** PENDING으로 남기고 종속 배포를 시작하지 않는다.
- [x] **CG-10 — 최종 검토·commit**
  - **Action:** 검토 수정 결과와 diff를 재확인하고 로컬 commit한다.
  - **Evidence:** 독립 재검토 APPROVE, P0/P1/P2=0. asString 수정 후493개 테스트 및 전역 컴파일 통과. 로컬 구현 commit 완료, 최종 SHA는 Git 이력과 사용자 보고로 확인한다.
  - **Failure:** PENDING으로 남기고 종속 배포를 시작하지 않는다.
- [ ] **C-08 — 최종 보고**
  - **Action:** 완료 범위와 미완료 항목을 보고한다.
  - **Evidence:** 로컬 검증24/25 완료, 최종 보고가 남았다. 외부 전달11개 단계는 PENDING이다. PR 생성 승인 범위는 별도로 확정한다.
  - **Failure:** PENDING으로 남기고 종속 배포를 시작하지 않는다.

## 검증 근거와 한계

| 검사 | 명령·근거 | 결과 |
| --- | --- | --- |
| 회귀 | `/tmp/issue-1637-final-red.log`, `/tmp/issue-1637-final-targeted.log` | Jackson2 이전 selector에서11실패, 수정 후 양쪽23개씩 통과 |
| 모듈 전체 | `:bluetape4k-jackson2:test`, `:bluetape4k-jackson3:test` | JUnit499/493, 실패0·오류0·skip0 |
| 수정 후 재검증 | `/tmp/issue-1637-review-fix.log` | Jackson3 cleanTest/test/detekt exit0,493개 통과 |
| 정적 분석 | 각 모듈 `detekt` XML | 새 테스트 파일 지적0. 기존 Jackson2 52개·Jackson3 46개 지적; ignoreFailures 설정이므로 exit0을 전체 clean으로 해석하지 않음 |
| 실제 의존성 | 양쪽 testRuntimeClasspath의 `dependencyInsight --dependency jackson` | Jackson2 BOM/core/databind/dataformat2.22.2, Jackson3 3.2.2. annotations2.22 유지 |
| 전역 컴파일 | `./gradlew compileKotlin compileTestKotlin -PexcludeBenchmarks=true --parallel --no-configuration-cache --console=plain` | `/tmp/issue-1637-broad-compile.log`, 성공356tasks |
| 전역 runtime | 중단한 `/tmp/issue-1637-broad-build.log` | exit143, 성공 증거 아님. Full Nightly·전체 모듈 runtime 테스트 미수행 |

XML dataformat은 두 모듈에 노출되지 않으므로 N/A이며, JSON에서 XML datatype으로 역직렬화하는 숫자 제한은 포함한다. 경계값·초과값, async chunk1/7/1024, Reader 조기 거부, CBOR 선언 길이, Smile, YAML merge, Path scheme을 검사한다.

## 문서 검증

계획과 lesson 각각 SPW-01부터 SPW-05 및 KO-01부터 KO-07을 확인했다. 근거는 위 로그, 실제 diff, 연결된 공식 issue/PR이다. 식별자·숫자·실패 범위를 유지하고 기술 의미·한국어 문체·링크·최종 파일을 확인했다. audit-korean-terms 결과 두 파일 findings=0이다. 새 코드 검토는 독립 reviewer의 APPROVE이며, 이 문서는 작성자가 직접 검증했다.

## 외부 작업 경계

현재 로컬 구현 완료 단계에서 PR 생성·push·CI·merge·정리는 실행하지 않는다. CG-11~CG-18(중간 CG-12A 포함), C-07, C-09는 후속 PR 전달 단계이며 PENDING이다. CG-X01은 tag·publish·dispatch·삭제 요청이 없어 N/A다. Full Nightly는 아직 증거가 없으며 전역 컴파일로 대체 입증하지 않는다.
