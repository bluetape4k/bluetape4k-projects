# Lessons - Issue #852 Rule Engine Condition Failure

## Context

Issue #852 aligned synchronous `DefaultRuleEngine` with the suspend engine when a rule condition throws.

## Lessons

- Rule failure policy must cover condition evaluation and action execution. Catching only action exceptions leaves `skipOnFirstFailedRule` inconsistent.
- `check()` should produce per-rule evaluation results. One condition exception should record `false` for that rule instead of aborting the whole result map.
- Listener ordering matters when converting exceptions into policy outcomes. A condition exception should still complete the rule-set lifecycle and report evaluation as `false`.
- `CancellationException` is not an ordinary rule failure. Sync and suspend engines must rethrow it before broad `Exception` handling.
- Keep sync and suspend engines behaviorally paired when they expose the same configuration semantics.

## Guard

When modifying rule engine failure policy, add sync and suspend parity tests for both `fire()` and `check()` before changing implementation.
