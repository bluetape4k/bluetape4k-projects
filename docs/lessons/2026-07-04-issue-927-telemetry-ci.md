# Issue #927 Telemetry CI Coverage

## Context

Push CI skipped module tests for changes under `infra/opentelemetry/**` and `infra/kafka-logback/**`. Nightly already covered OpenTelemetry, but push CI still allowed telemetry and logging changes to pass without targeted module tests.

## Decision

Add a dedicated `telemetry-infra` path-filter output and `Test / Telemetry Infra` job instead of adding these modules to the Redis-focused infra lane.

## Rationale

- `infra/kafka-logback` uses Kafka Testcontainers and should run serially with `--max-workers=1`.
- `infra/opentelemetry` is observability-focused and already has a Nightly shape, but needs push CI coverage when its own files change.
- A separate job keeps Redis/cache infra results distinct from telemetry/logback failures.

## Verification

- `actionlint .github/workflows/ci.yml`
- Backslash-single-quote guard for GitHub Actions expressions
- `git diff --check`
- Gradle project/task wiring dry-run
- `:bluetape4k-opentelemetry:test`
- `:bluetape4k-kafka-logback:test`
- Matching Kover XML tasks

## Future Guard

When adding CI path filters for a module family, update all four surfaces together: path-filter output, test job, `coverage-report.needs`, and `ci-status.needs`.

