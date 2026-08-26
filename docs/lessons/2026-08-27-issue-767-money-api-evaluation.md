# Money API 평가 PR은 issue와 PR metadata를 함께 닫아야 한다

## 배경

Epic #1423의 child #767은 owned Money API를 바로 구현하지 않고, 현재
JSR-354/Moneta public API를 유지할 근거와 재개 조건을 기록하는 Type A 평가
작업이다. 설계·평가·실행 계획 문서는 production source를 변경하지 않는다는
결정과 `G1-G5` 재평가 조건을 고정했다.

PR #1532를 만든 뒤 live read-back을 다시 수행하면서, 연결된 Issue #767의
milestone·assignee·labels는 정상인데 PR 자체의 assignee, labels, milestone이
비어 있는 것을 확인했다. 또한 Type A의 A-09 lesson commit이 PR 생성 전에
완료되지 않았다.

## 결정

- PR metadata는 issue metadata를 읽었다는 기록만으로 충족하지 않는다. PR의
  `assignee`, `labels`, `milestone`, base/head, 연결 방식과 본문 `## Metadata`
  를 모두 live read-back한다.
- #767은 이번 평가로 해결되는 issue가 아니므로 `Refs #767`만 사용하고,
  `closingIssuesReferences`가 비어 있는지 확인한다.
- Type A는 문서 전용 변경이어도 A-09 lesson을 commit한 뒤에만 PR delivery로
  진행한다.
- `docs/superpowers/index/`는 현재 `origin/develop`에 없고
  `docs/superpowers/README.md`가 archive 구조를 설명하므로, 해당 legacy
  index 갱신은 concrete N/A로 기록한다.

## 결과

owned API, dependency, deprecation, downstream migration, provider network,
benchmark는 변경하지 않는다. PR에는 다음 metadata를 반영한다.

- assignee: `debop`
- milestone: `2.0.0`
- issue의 relevant labels: `enhancement`, `refactor`, `tech-debt`
- 문서 변경을 나타내는 PR label: `documentation`
- base/head: `develop` / `docs/767-money-api-evaluation`
- linkage: `Refs #767`, 자동 종료 없음

Lesson commit 뒤에는 새 commit SHA를 exact head로 다시 publish하고, 이전 SHA의
CI 결과를 재사용하지 않는다. 새 head에서 workflow를 수동 실행하고 terminal
결론을 읽은 뒤에만 `CG-14`를 판정한다.

## 검증

- `gh pr view 1532 --json assignees,labels,milestone,baseRefName,headRefName,headRefOid,closingIssuesReferences`: 초기 read-back에서 PR metadata 누락을 재현했다.
- `gh issue view 767 --json assignees,labels,milestone,state`: Issue #767의 기대 metadata를 확인했다.
- `git diff --name-status origin/develop...HEAD`: 초기 head의 변경은 승인된
  spec/plan/research 세 문서뿐이었고 lesson은 없었다.
- 설계·계획·평가 기록의 Korean terminology audit와 `git diff --check`는
  기존 head에서 통과했다. lesson 추가 뒤 네 문서에 audit와 diff check를 다시
  실행한다.
- exact-head hosted CI와 fresh merge approval은 lesson/metadata 보정 뒤의 새
  head에서 다시 검증할 항목이며, 이 lesson은 그 결과를 선행 주장하지 않는다.

## 놓친 점과 복구

초기 PR body에는 issue metadata를 확인했다는 문장이 있었지만 PR live fields를
실제로 채우지 않아 문장과 외부 상태가 불일치했다. 또 `## DoD Status`는 있었지만
중앙 PR template의 `## Metadata`와 정확한 check count가 없었다. 이 복구에서는
lesson을 먼저 commit하고, standard PR sections와 metadata/read-back 증거를
갱신한 뒤 exact-head CI를 새로 실행한다.

## 향후 지침

- `CG-12A` 직후 `gh pr view`로 PR field와 linked issue field를 같은 표로
  비교한다. 하나라도 비어 있거나 다르면 CI/리뷰 단계로 진행하지 않는다.
- PR body에는 `Issue`, `PR`, `Base`, `Head`, `Labels`, `milestone`, `assignee`,
  `closingIssuesReferences`, `CI`를 `## Metadata`에 명시하고 `## DoD Status`를
  마지막 H2로 둔다.
- Type A 문서 전용 작업도 A-09 lesson을 commit하고, legacy index처럼 실제
  source surface가 없는 항목은 경로와 저장소 상태를 포함한 N/A로 남긴다.
- head가 바뀌면 이전 CI와 review evidence를 폐기하고 새 SHA에 대해 다시
  `CG-12`부터 `CG-14`를 검증한다.

## 문서 SPW 감사

- SPW-01: PASS — 독자는 PR을 검토하는 유지보수자이며, Issue #767, PR #1532,
  Type A A-09, `bluetape-workflow` common gates와 live GitHub read-back을
  근거로 삼았다. hosted CI와 merge approval은 미확정으로 남겼다.
- SPW-02: PASS — 배경, 결정, 결과, 검증, 놓친 점, 복구, 향후 guard를 lesson
  계약에 맞게 기록했다.
- SPW-03: PASS — 한국어 기술 문체를 사용하고 API 이름, 명령, SHA, URL, issue와
  gate 식별자를 그대로 보존했다.
- SPW-04: PASS — PR/issue live field, 현재 branch diff, workflow gate와 문서
  template을 대조했으며 초기 누락과 새 head 재검증 경계를 분리했다.
- SPW-05: PASS — 최종 Markdown을 다시 읽어 heading, 목록, code span, 긴 명령
  줄과 불확실성 표기를 확인했다.
