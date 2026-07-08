# Issue #927 Telemetry CI Coverage Review

## Scope

- Issue: #927 `ci: Run telemetry infra tests for kafka-logback and opentelemetry changes`
- Changed file: `.github/workflows/ci.yml`
- Modules covered by the new CI lane:
  - `:bluetape4k-opentelemetry`
  - `:bluetape4k-kafka-logback`

## 7-Tier Review

| Tier | Verdict | Evidence |
|------|---------|----------|
| 1. Security | PASS | The change adds CI execution only. No secrets, permissions, credentials, or production runtime paths changed. |
| 2. Architecture | PASS | Telemetry infra gets a dedicated `telemetry-infra` filter/job instead of being mixed into the Redis-specific infra lane. |
| 3. Reliability | PASS | `kafka-logback` uses Testcontainers, so the job runs with `--max-workers=1` and `--no-configuration-cache`. |
| 4. Code quality | PASS | Workflow wiring is narrow: filter output, job, coverage needs, and status needs are synchronized. |
| 5. Testing | PASS | Actual telemetry test command passed locally: OpenTelemetry 78 tests and Kafka Logback 22 tests. |
| 6. Performance | PASS | The lane is path-filtered and does not expand unrelated CI jobs for non-telemetry changes. |
| 7. Documentation / Evidence | PASS | This review plus the lesson document record the issue boundary, commands, and future guard. |

## Validation

- `actionlint .github/workflows/ci.yml`: PASS.
- Backslash-single-quote guard for `.github/workflows/ci.yml`: PASS, no escaped expression quotes.
- `git diff --check`: PASS.
- Gradle project/task wiring dry-run for `:bluetape4k-opentelemetry` and `:bluetape4k-kafka-logback`: PASS.
- `./gradlew :bluetape4k-opentelemetry:test :bluetape4k-kafka-logback:test --max-workers=1 --no-configuration-cache`: PASS.
- `./gradlew :bluetape4k-opentelemetry:koverXmlReport :bluetape4k-kafka-logback:koverXmlReport --max-workers=1 --no-configuration-cache`: PASS.

## Findings

- P0: 0
- P1: 0
- P2/P3: 0 open
