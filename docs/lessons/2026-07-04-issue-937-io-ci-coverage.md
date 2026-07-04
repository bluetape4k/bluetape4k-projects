# Issue 937 IO CI Coverage

## Context

The CI workflow had narrow IO path filters. Recent changes under `io/grpc/**`, `io/http/**`, `io/jackson2/**`, and `io/retrofit2/**` could pass CI while relevant module tests were skipped.

## Decision

Expand the existing targeted CI lanes instead of adding a full repository test fanout:

- `Test / IO`: include Jackson2, gRPC, and Tink paths plus matching test/Kover tasks.
- `Test / IO HTTP`: include HTTP, Retrofit2, and Vert.x paths plus matching test/Kover tasks.
- Leave `io/protobuf/**` for the dedicated open issue #926.

## Outcome

Workflow syntax and Gradle task wiring validated locally. PR CI is expected to provide the live path-filter/job execution proof because `.github/workflows/ci.yml` is part of the shared path filter.

## Future Guidance

- When adding CI path filters, add the matching Gradle test and Kover tasks in the same change.
- Keep separate backlog issues separate unless the PR explicitly closes them; here protobuf remains scoped to #926.
- Always run `actionlint`, escaped workflow quote scan, `git diff --check`, and Gradle dry-run task validation before opening workflow PRs.
