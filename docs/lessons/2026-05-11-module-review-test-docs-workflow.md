# Module Review, Tests, and Docs Workflow Lessons

## 배경

`bluetape4k` module은 code review, 누락된 test와 edge test, public KDoc 예제,
README 갱신, commit, draft PR을 반복하는 방식으로 검토됐다. 작업에는 Tink,
Jackson3, Jackson2 같은 IO/security serialization module이 포함됐고, 같은
6-Tier P0/P1 gate discipline을 재사용했다.

## 결정 또는 발견

P0/P1 convergence는 private review state가 아니라 명시적인 artifact로 다뤄야 한다.
각 PR은 baseline P0/P1 list, advisor iteration result, 최종 `P0=0`, `P1=0`
gate result를 보여줘야 한다.

가장 반복적으로 놓친 지점은 syntax 문제가 아니었다. lifecycle contract, 오래된 문서,
누락된 edge test였다.

- Worktree setup은 spec/plan/docs edit보다 먼저 끝나야 한다. `develop`에 남은
  untracked planning file은 나중에 만든 worktree로 자동 이동하지 않는다.
- Public API 변경은 test, Korean KDoc, `README.md`, `README.ko.md`를 같은 pass에서
  다뤄야 한다. code만 바꾸면 다음 reviewer가 contract를 다시 찾아야 한다.
- 문서는 실제 source file과 대조해야 한다. Jackson2 README는 module에 더 이상 없는
  오래된 `JsonEncrypt`/Jasypt class를 계속 설명하고 있었다.
- Streaming API에는 명시적인 lifecycle test가 필요하다. Jackson non-blocking parser는
  `endOfInput()`이 호출되기 전까지 finite stream 종료를 알지 못한다.
- Coroutine API에는 cancellation evidence가 필요하다. `CancellationException`이
  wrap되거나 지연되지 않는다는 test가 없으면 checkpoint는 불완전하다.
- Advisor gate는 느리거나 빈 output을 낼 수 있다. artifact를 저장하고 파일이 비어 있으면
  더 짧은 prompt로 다시 실행한 뒤 최종 P0/P1 table을 기록한다.
- Free account에서는 GitHub Actions runner allocation이 병목이 될 수 있다. local module
  validation이 PR scope를 이미 증명했다면 queued CI를 무작정 기다리지 말고 PR에 local
  evidence를 명확히 기록한다.

## 결과

Workflow는 더 예측 가능해졌다.

- Draft PR은 prose summary만 두지 않고 P0/P1 table을 포함한다.
- README drift는 code가 merge된 뒤가 아니라 API review와 함께 잡힌다.
- Edge test는 관련 범위에서 final-stream truncation, post-terminal parser reuse,
  double terminal call, empty input, partial byte-array length, coroutine cancellation을 다룬다.
- Actions가 runner capacity를 기다릴 때는 local verification을 completion signal로 사용할 수 있다.

## 검증

Jackson2 iteration의 evidence:

- `./gradlew :bluetape4k-jackson2:compileTestKotlin --no-build-cache --no-daemon` 통과.
- EOF, post-EOF, empty input, double EOF, partial length, cancellation case를 추가한 뒤
  targeted async parser test 통과.
- Full module test는 local에서 `428 passing, 1 pending`.
- `git diff --check` 통과.
- Claude advisor review는 `P0=0, P1=8`에서 `P0=0, P1=0`으로 수렴.
- Actions runner allocation이 지연되어 draft PR #396은 final gate와 local validation을 문서화했다.

## 향후 지침

- Module-scale review는 항상 dedicated worktree에서 시작하고 spec/plan은 그 worktree 안에 작성한다.
- Spec, plan, PR body, final report에 P0/P1 table을 유지한다.
- Public API 추가는 완료로 보기 전에 Korean KDoc, `README.md`, `README.ko.md`, direct edge test와 함께 묶는다.
- Streaming parser는 logical EOF, truncated final input, terminal method idempotency,
  post-terminal reuse를 항상 test한다.
- Coroutine path는 cancellation propagation을 명시적으로 test한다. `ensureActive()`가 있다는 사실만으로 충분하다고 보지 않는다.
- External advisor가 쓸 만한 artifact를 반환하지 않으면 gate를 닫기 전에 더 좁은 prompt로 다시 실행한다.
- 변경이 module-scoped이고 local validation이 behavior를 이미 다루면 queued remote CI만 기다리기보다
  targeted/full module test evidence를 우선한다.
