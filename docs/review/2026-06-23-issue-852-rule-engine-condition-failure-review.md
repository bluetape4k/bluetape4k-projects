# Issue #852 Rule Engine Condition Failure Review

## Scope

- Branch: `fix/rule-engine-condition-failure-852`
- Module: `:bluetape4k-rule-engine`
- Issue: `#852`
- Files: synchronous rule engine implementation and regression tests.

## RED Evidence

Against `origin/develop`, applying the sync regression tests without production code changes failed as expected:

```text
./gradlew :bluetape4k-rule-engine:test \
  --tests "io.bluetape4k.rule.core.DefaultRuleEngineTest.평가 실패는 skipOnFirstFailedRule 에 따라 다음 Rule 을 중단한다" \
  --tests "io.bluetape4k.rule.core.DefaultRuleEngineTest.평가 실패는 기본 설정에서 다음 Rule 실행을 막지 않는다" \
  --tests "io.bluetape4k.rule.core.DefaultRuleEngineTest.평가 실패도 listener lifecycle 을 완료한다" \
  --tests "io.bluetape4k.rule.core.DefaultRuleEngineTest.check 중 평가 실패는 false 로 기록한다"
```

Result: FAILED, 0 passing and 4 failing.

- `fire()` condition failure escaped from `DefaultRuleEngine.doFire` before `skipOnFirstFailedRule` handling.
- Default `skipOnFirstFailedRule=false` could not isolate a failed condition and continue to the next rule.
- Rule listener lifecycle did not complete because the condition exception escaped before `afterEvaluate(false)` and rule-set `afterExecute`.
- `check()` condition failure escaped from `DefaultRuleEngine.doCheck` instead of recording `false`.

During review, the interim broad-catch implementation also failed the sync cancellation parity tests:

```text
./gradlew :bluetape4k-rule-engine:test \
  --tests "io.bluetape4k.rule.core.DefaultRuleEngineTest.실행 중 CancellationException 은 삼키지 않고 전파한다" \
  --tests "io.bluetape4k.rule.core.DefaultRuleEngineTest.평가 중 CancellationException 은 삼키지 않고 전파한다" \
  --tests "io.bluetape4k.rule.core.DefaultRuleEngineTest.check 중 CancellationException 은 삼키지 않고 전파한다"
```

Result: FAILED, 0 passing and 3 failing. The broad `Exception` catches swallowed `CancellationException` as ordinary rule failure.

## Findings

| Severity | Count | Notes |
|---|---:|---|
| P0 | 0 | No data-loss, API-breaking, or CI-blocking issue found in the repaired diff. |
| P1 | 0 | Sync evaluation failures now follow the rule failure policy while `CancellationException` still propagates. |
| P2 | 0 | Review evidence and coverage gaps were corrected before PR. |

## Evidence

| Check | Result | Evidence |
|---|---|---|
| Issue RED | PASS | The four issue regression tests failed against `origin/develop` with test-only changes. |
| Cancellation RED/GREEN | PASS | The three sync cancellation parity tests failed on the interim broad-catch code and pass after explicit cancellation rethrow. |
| Targeted regression | PASS | Seven sync regression tests completed with 7 passing. |
| Class parity | PASS | `DefaultRuleEngineTest` XML: 17 tests, 0 failures, 0 errors. `DefaultSuspendRuleEngineTest` XML: 11 tests, 0 failures, 0 errors. |
| Module build | PASS | `./gradlew :bluetape4k-rule-engine:build --rerun-tasks --no-parallel --max-workers=1 --no-configuration-cache` completed with 352 passing and 5 pending. |
| Diff hygiene | PASS | `git diff --check` produced no output. |
| Concurrency gate | PASS | Not concurrency-related; no thread/coroutine stress helper applies. |

## Verdict

Gate passes with P0=0 and P1=0.
