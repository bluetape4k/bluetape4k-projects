# Issue #975 Kafka CI Coverage

## Context

Push CI did not run `:bluetape4k-kafka:test` for changes under `infra/kafka/**`.
The existing `infra` lane was Redis/cache-specific, while `telemetry-infra`
covered `infra/opentelemetry/**` and `infra/kafka-logback/**`.

## Decision

Add a dedicated `kafka-infra` path-filter output and `Test / Kafka Infra` job
instead of mixing Kafka into the Redis-focused infra lane.

## Rationale

- `infra/kafka` tests use embedded Kafka/Testcontainers-style infrastructure and should run serially with `--max-workers=1`.
- A dedicated job keeps Kafka failures distinct from Redis/cache and telemetry/logback failures.
- CI aggregation must list the new job in both `coverage-report.needs` and `ci-status.needs` so skipped/failed Kafka coverage is visible.

## Verification

- `actionlint .github/workflows/ci.yml`
- Backslash-single-quote guard for GitHub Actions expressions
- `git diff --check`
- `./gradlew :bluetape4k-kafka:cleanTest :bluetape4k-kafka:test --max-workers=1 --no-build-cache --no-configuration-cache`
- `./gradlew :bluetape4k-kafka:koverXmlReport --max-workers=1 --no-configuration-cache`

## Future Guard

When adding CI coverage for a module family, update all four surfaces together:
path-filter output, test job, `coverage-report.needs`, and `ci-status.needs`.
