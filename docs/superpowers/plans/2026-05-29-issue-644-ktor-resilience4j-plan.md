# Issue 644 Ktor Resilience4j Plan

## Scope

Implement a narrow `bluetape4k-ktor-resilience4j` module backed by the current Ktor foundation and existing coroutine Resilience4j facade.

## Tasks

1. Add `ktor/resilience4j` module with Gradle dependencies and test resources.
2. Implement block and route helpers with cancellation-safe circuit breaker handling.
3. Add StatusPages mappings for open circuit, rate limit, and timeout failures.
4. Add focused Ktor tests for success, retry, open circuit, rate limit, and cancellation.
5. Update module README locale set and root README.
6. Wire CI and nightly Ktor module task lists.
7. Add lesson and verify registration, tests, coverage, diff hygiene, and workflow YAML.

## Validation

- `./gradlew -q projects | rg "bluetape4k-ktor-resilience4j"`
- `./gradlew :bluetape4k-ktor-resilience4j:compileKotlin :bluetape4k-ktor-resilience4j:compileTestKotlin --no-configuration-cache`
- `./gradlew :bluetape4k-ktor-resilience4j:test :bluetape4k-ktor-resilience4j:koverXmlReport --no-configuration-cache`
- `actionlint .github/workflows/ci.yml .github/workflows/nightly-tests.yml`
- `git diff --check`

## Stop Condition

Stop when the PR is merged into `develop`, CI is green, and the feature worktree is removed.
