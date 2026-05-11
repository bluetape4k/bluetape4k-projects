# Module Review, Tests, and Docs Workflow Lessons

## Context

`bluetape4k` modules were reviewed in repeated cycles: code review, missing and
edge tests, public KDoc examples, README updates, commits, and draft PRs. The
work included IO/security serialization modules such as Tink, Jackson3, and
Jackson2, and reused the same 6-Tier P0/P1 gate discipline.

## Decision or Finding

P0/P1 convergence must be treated as an explicit artifact, not a private review
state. Each PR should show the baseline P0/P1 list, advisor iteration results,
and the final `P0=0`, `P1=0` gate result.

The most repeatable review misses were not syntax issues. They were lifecycle
contracts, stale documentation, and missing edge tests:

- Worktree setup must happen before spec/plan/docs edits. Untracked planning
  files on `develop` do not follow a later-created worktree.
- Public API changes need tests, Korean KDoc, README.md, and README.ko.md in the
  same pass. Updating only code leaves the next reviewer to rediscover the
  contract.
- Documentation must be checked against actual source files. Jackson2 README
  still documented stale `JsonEncrypt`/Jasypt classes that were no longer in the
  module.
- Streaming APIs need explicit lifecycle tests. Jackson non-blocking parsers do
  not know that a finite stream ended unless `endOfInput()` is called.
- Coroutine APIs need cancellation evidence. A checkpoint is incomplete without
  a test proving `CancellationException` is not wrapped or delayed.
- Advisor gates can be slow or produce empty output. Save artifacts and rerun
  with a shorter prompt when the file is empty, then record the final P0/P1
  table.
- GitHub Actions runner allocation can be the bottleneck on free accounts. Do
  not wait on queued CI when local module validation already proves the PR
  scope; record the local evidence clearly in the PR.

## Outcome

The workflow became more predictable:

- Draft PRs now include P0/P1 tables instead of only a prose summary.
- README drift is caught alongside API review, not after code is already merged.
- Edge tests now cover final-stream truncation, post-terminal parser reuse,
  double terminal calls, empty input, partial byte-array length, and coroutine
  cancellation where relevant.
- Local verification can be used as the completion signal when Actions are
  waiting for runner capacity.

## Verification

Evidence from the Jackson2 iteration:

- `./gradlew :bluetape4k-jackson2:compileTestKotlin --no-build-cache --no-daemon`
  passed.
- Targeted async parser tests passed after adding EOF, post-EOF, empty input,
  double EOF, partial length, and cancellation cases.
- Full module test passed locally with `428 passing, 1 pending`.
- `git diff --check` passed.
- Claude advisor review converged from `P0=0, P1=8` to `P0=0, P1=0`.
- Draft PR #396 documented the final gate and local validation because Actions
  runner allocation was delayed.

## Future Guidance

- Start every module-scale review in a dedicated worktree and write spec/plan
  inside that worktree.
- Keep a P0/P1 table in the spec, plan, PR body, and final report.
- Pair every public API addition with Korean KDoc, README.md, README.ko.md, and
  direct edge tests before considering the code done.
- For streaming parsers, always test logical EOF, truncated final input,
  terminal method idempotency, and post-terminal reuse.
- For coroutine paths, test cancellation propagation explicitly. Do not rely on
  the presence of `ensureActive()` alone.
- When an external advisor returns no useful artifact, rerun with a narrower
  prompt before closing the gate.
- Prefer targeted and full module tests over waiting for queued remote CI when
  the change is module-scoped and local validation already covers the behavior.
