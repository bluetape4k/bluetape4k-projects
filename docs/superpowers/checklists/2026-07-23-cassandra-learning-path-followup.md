# Cassandra Learning Path 후속 완료 체크리스트

## 범위

- 유형: Type E — 문서 유지보수
- Projects 범위: Cassandra 한글/영문 landing, 학습 경로 계획·설계 기록
- 후속 Site 범위: Projects 병합 후 `bluetape4k.github.io`의 1.11 snapshot 갱신
- 제외: 라이브러리 API·동작·의존성 변경, 이미 완료된 Site 작업의 무근거 재작성
- PR: Projects와 Site를 각각 별도 PR로 전달하며, 각 병합은 새 승인 후에만 수행한다.

## Projects preflight

- [x] **CG-01 — 권한과 워크플로를 재확인한다**
    - **Action:** 승인된 Type E 계획, `AGENTS.md`, `bluetape-workflow`, `bluetape-maintenance`, `bluetape-writer`를 읽는다.
    - **Evidence:** 2026-07-23 승인과 현재 worktree의 clean 상태.
    - **Failure:** 수정 전에 중단한다.
- [x] **CG-02 — 기존 이력과 현재 증거를 조회한다**
    - **Action:** GNO docs/GitHub과 GitHub PR·issue 검색을 수행한다.
    - **Evidence:** 기존 Cassandra 설계·체크리스트는 존재하고, 이 follow-up에는 연결 PR/issue가 없다.
    - **Failure:** 이력 의존 결정을 중단한다.
- [x] **CG-03 — 사용자 작업과 경계를 보호한다**
    - **Action:** 현재 branch·upstream·diff·worktree를 확인한다.
    - **Evidence:** `docs/cassandra-learning-path-followup`은 clean, `origin/develop` 대비 ahead 4/behind 61이다.
    - **Failure:** 관련 없는 변경을 보존하고 중단한다.
- [x] **CG-04 — 문서·언어 경계를 적용한다**
    - **Action:** EN/KO landing parity와 공개 PR 영어 정책을 적용한다.
    - **Evidence:** 변경 대상은 한영 manual과 사용자용 한국어 계획/체크리스트다.
    - **Failure:** locale drift를 수정한다.
- [x] **CG-05 — 기존 패턴을 재사용한다**
    - **Action:** 기존 Cassandra 상세 매뉴얼 spec·plan·checklist를 기준으로 한다.
    - **Evidence:** `2026-07-13-cassandra-detailed-manual-design.md`, 기존 manual checklist.
    - **Failure:** 새 문서 구조를 임의로 추가하지 않는다.
- [x] **CG-06 — 공개 문서 계약을 증명한다**
    - **Action:** 한영 landing과 대표 chapter link·legacy anchor를 함께 검토한다.
    - **Evidence:** 두 locale에 같은 다섯 chapter와 이전 heading anchor가 있으며, public API·README 등록 변경은 없다.
    - **Failure:** parity 또는 링크 drift를 수정한다.
- [x] **CG-07 — targeted 문서 proof를 실행한다**
    - **Action:** manual validator, generator test, manifest check, diff check를 실행한다.
    - **Evidence:** validator 18 runs/56 assertions, generator 1 run/39 assertions, alignment·manifest·diff check 성공.
    - **Failure:** 수정 후 모든 문서 proof를 다시 실행한다.
- [x] **CG-08 — heavyweight 검증을 제외한다**
    - **Action:** Testcontainers·real DB·native·emulator·benchmark 검증 필요성을 분류한다.
    - **Evidence:** Markdown·계획·lesson만 변경하며 라이브러리 동작과 실행 환경은 변경하지 않아 N/A다.
    - **Failure:** 실행 동작 변경이 발견되면 순차 heavyweight 검증으로 재분류한다.

## Projects delivery

- [x] **E-01 — 최신 develop 위로 복구 branch를 재정렬한다**
    - **Action:** 현재 `origin/develop`에 rebase하고 충돌을 문서 scope 안에서 해결한다.
    - **Evidence:** `git rebase origin/develop`가 다섯 커밋을 충돌 없이 적용했고, rebase 후 worktree는 clean이다.
    - **Failure:** 충돌 내용을 보존하고 중단한다.
- [x] **E-02 — 한영 manual 계약을 검증한다**
    - **Action:** Cassandra landing의 기능 소개·학습 경로·호환 anchor를 대조한다.
    -
  **Evidence:** 두 landing 모두 legacy anchor, 새 heading, 다섯 chapter 설명을 포함한다. `CqlSessionProvider`와 `executeSuspending`은 현재 Cassandra 소스 graph에서도 확인했다.
    - **Failure:** 문구 또는 anchor drift를 수정한다.
- [x] **E-03 — 문서 검증을 실행한다**
    - **Action:** manual validator, manifest check, `git diff --check`를 실행한다.
    -
  **Evidence:** `validate_manuals_test.rb` 18 runs/56 assertions, `generate_manuals_test.rb` 1 run/39 assertions, validator와 manifest check가 성공했다.
    - **Failure:** PR 생성을 중단하고 수정한다.
- [x] **CG-09 — lesson gate를 판단한다**
    - **Action:** 복구·재정렬에서 재사용 가능한 교훈이 생겼는지 diff와 이력을 검토한다.
    -
  **Evidence:** `docs/lessons/2026-07-23-cassandra-learning-path-recovery.md`에 stale canonical branch와 versioned Site snapshot의 순서 의존성을 기록했다.
    - **Failure:** pre-PR proof를 중단한다.
- [x] **CG-10 — 최종 pre-PR proof를 수렴한다**
    - **Action:** 최종 diff·검증·문서 review를 완료하고 Lore 형식으로 커밋한다.
    -
  **Evidence:** manual validator·manifest check·`git diff --check`를 rebase 후 다시 통과했고, scoped diff review에서 P0=0/P1=0이다. exact local SHA는 PR publication 직전에 재확인한다.
    - **Failure:** PR 생성을 중단한다.
- [ ] **CG-11..15 — Projects PR을 생성하고 merge-ready를 보고한다**
    - **Action:** exact head push, PR metadata/CI/review를 완료한다.
    - **Evidence:** PR URL, exact head, CI·review 결과, DoD status.
    - **Failure:** CG-16 이전에 병합하지 않는다.
- [ ] **CG-16..18 — 새 병합 승인 후 Projects를 병합·동기화한다**
    - **Action:** 사용자 승인 후에만 merge하고 local sync/cleanup을 수행한다.
    - **Evidence:** merge SHA와 local/upstream 일치.
    - **Failure:** 승인 대기 상태를 유지한다.

## Site follow-up

- [ ] **S-01 — Projects 병합 commit을 source로 고정한다**
    - **Action:** Projects merge SHA를 확인한 뒤 Site 1.11 manual snapshot을 refresh한다.
    - **Evidence:** source/release provenance와 한영 generated landing.
    - **Failure:** Projects 병합 전 Site 갱신을 시작하지 않는다.
- [ ] **S-02 — Site 검증과 별도 PR을 완료한다**
    - **Action:** manual sync check·site build·한영 route 확인 후 Site PR을 만든다.
    - **Evidence:** exact Site PR head와 CI/review.
    - **Failure:** 별도 merge 승인 전 병합하지 않는다.
