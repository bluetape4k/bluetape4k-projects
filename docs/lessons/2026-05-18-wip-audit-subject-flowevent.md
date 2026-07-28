# 2026-05-18 — Projects WIP audit for Subject and FlowEvent follow-ups

## 배경

GNO 기반 audit가 core/shared repository에 follow-up 2개를 등록했다:
`BehaviorSubject.emitError()` cancellation semantics를 다루는 #543과 Kotlin 2 hot path에서
`FlowEvent` value-class wrapper를 평가하는 #544다.

## 결정

#543은 core coroutine primitive의 terminal notification behavior에 영향을 주므로 다음 projects
correctness item으로 유지한다. #544는 즉시 source-compatible refactor를 수행하는 항목이 아니라
P2 performance/API evaluation으로 둔다.

## 결과

`WIP.md`는 GNO 기반 audit note를 포함하고, open assigned issue 17개를 나열하며, #543을
feature work보다 앞에 배치한다.

## 검증

- `gh issue list --state open --assignee debop`이 open issue 17개를 반환했다.
- `git diff origin/develop -- WIP.md`로 기존 upstream #545 refresh 이후 남은 WIP delta를 확인했다.
- Queue 확정 전에 `gno query ... --no-rerank`를 사용했다.

## 향후 agent 가이드

`BehaviorSubject` terminal event에서는 parent cancellation과 collector-local cancellation을 분리해
test한다. `FlowEvent`에서는 data class를 value class로 교체하기 전에 source/binary compatibility를
증명한다.
