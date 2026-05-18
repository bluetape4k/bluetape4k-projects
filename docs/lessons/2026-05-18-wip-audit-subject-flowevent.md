# 2026-05-18 — Projects WIP audit for Subject and FlowEvent follow-ups

## Context

The qmd-backed audit registered two follow-ups in the core/shared repository:
#543 for `BehaviorSubject.emitError()` cancellation semantics and #544 for
evaluating `FlowEvent` value-class wrappers on Kotlin 2 hot paths.

## Decision

Keep #543 as the next projects correctness item because it affects terminal
notification behavior in a core coroutine primitive. Keep #544 as a P2
performance/API evaluation, not an immediate source-compatible refactor.

## Outcome

`WIP.md` now includes the qmd-backed audit notes, lists 17 open assigned issues,
and moves #543 ahead of feature work.

## Verification

- `gh issue list --state open --assignee debop` returned 17 open issues.
- `git diff origin/develop -- WIP.md` showed the remaining WIP delta after the
  existing upstream #545 refresh.
- `qmd query ... --no-rerank` was used before finalizing the queue.

## Future Agents

For `BehaviorSubject` terminal events, test parent cancellation separately from
collector-local cancellation. For `FlowEvent`, prove source/binary compatibility
before replacing data classes with value classes.
