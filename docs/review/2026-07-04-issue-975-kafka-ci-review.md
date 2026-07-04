# Issue #975 Kafka CI Coverage Review

## Scope

- Issue: #975 `ci: Run Kafka infra tests for infra/kafka changes`
- Changed file: `.github/workflows/ci.yml`
- Module covered by the new CI lane:
  - `:bluetape4k-kafka`

## 7-Tier Review

| Tier | Verdict | Evidence |
|------|---------|----------|
| 1. Security | PASS | The change adds CI execution only. No secrets, permissions, credentials, or production runtime paths changed. |
| 2. Architecture | PASS | Kafka infra gets a dedicated `kafka-infra` filter/job instead of being mixed into the Redis/cache infra lane. |
| 3. Reliability | PASS | The Kafka job runs serially with `--max-workers=1` and disables the configuration cache, matching the module's infrastructure-test profile. |
| 4. Code quality | PASS | Workflow wiring is narrow: filter output, job, coverage needs, and status needs are synchronized. |
| 5. Testing | PASS | `:bluetape4k-kafka:cleanTest :bluetape4k-kafka:test` passed with 265 tests, and Kover XML generation passed. |
| 6. Performance | PASS | The lane is path-filtered to `infra/kafka/**` and does not expand unrelated Redis/cache or telemetry jobs for Kafka-only changes. |
| 7. Documentation / Evidence | PASS | This review plus the lesson document record the issue boundary, commands, and future guard. |

## Validation

- `actionlint .github/workflows/ci.yml`: PASS.
- Backslash-single-quote guard for `.github/workflows`: PASS.
- `git diff --check`: PASS.
- `./gradlew :bluetape4k-kafka:cleanTest :bluetape4k-kafka:test --max-workers=1 --no-build-cache --no-configuration-cache`: PASS, 265 tests.
- `./gradlew :bluetape4k-kafka:koverXmlReport --max-workers=1 --no-configuration-cache`: PASS.

## Findings

- P0: 0
- P1: 0
- P2/P3: 0 open
