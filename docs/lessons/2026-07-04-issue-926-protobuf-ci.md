# Issue #926 Protobuf CI Coverage

## Context

Push CI skipped `Test / IO` for changes under `io/protobuf/**`, even though protobuf is an IO serialization module and recent protobuf codec changes included module tests.

## Decision

Add `io/protobuf/**` to the existing `io` path-filter output and include `:bluetape4k-protobuf` in the existing `Test / IO` test and Kover task lists.

## Rationale

- Protobuf belongs with adjacent IO serialization modules such as JSON, Jackson, gRPC, and Tink.
- A separate protobuf job would add workflow fanout without improving failure isolation enough to justify it.
- Reusing `test-io` preserves existing `coverage-report` and `ci-status` wiring.

## Verification

- `actionlint .github/workflows/ci.yml`
- Backslash-single-quote guard for GitHub Actions expressions
- `git diff --check`
- Gradle project/task wiring dry-run
- `:bluetape4k-protobuf:test`
- `:bluetape4k-protobuf:koverXmlReport`

## Future Guard

When adding or moving IO modules, keep the path-filter patterns, Gradle test task list, and Kover task list synchronized in the same PR.
