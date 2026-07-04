# Issue 937 IO CI Coverage Review

## Scope

- Issue: #937 `ci: Run gRPC, HTTP, Jackson2, and Retrofit2 tests for IO changes`
- Milestone: 1.11.1
- Branch: `fix/issue-937-io-ci-coverage`
- Target: `.github/workflows/ci.yml`

## 7-Tier Review

| Tier | Result | Evidence |
|---|---|---|
| P0 Correctness | PASS | CI path filters now map `io/jackson2/**`, `io/grpc/**`, and `io/tink/**` to `Test / IO`, and `io/http/**`, `io/retrofit2/**`, and `io/vertx/**` to `Test / IO HTTP`. |
| P1 Runtime Safety | PASS | Runtime/library code is unchanged. The change only expands PR/push CI coverage for existing modules. |
| P2 Workflow Reliability | PASS | Added matching Gradle `test` and `koverXmlReport` tasks for every newly covered path-filter module. |
| P3 Test Quality | PASS | The workflow avoids a full repository test fanout and keeps IO and IO HTTP lanes targeted. Protobuf remains out of scope for separate issue #926. |
| P4 Scope Control | PASS | Only `.github/workflows/ci.yml` was changed. No build scripts, source, generated files, or module registrations changed. |
| P5 Build Hygiene | PASS | `actionlint`, escaped-quote scan, `git diff --check`, Gradle project lookup, and dry-run task validation passed. |
| P6 Process | PASS | PR metadata must mirror issue #937: milestone `1.11.1`, assignee `debop`, and labels `bug`, `test`, `infra/io`, `ci`, `codex`, `codex-automation`. |

## Verification

- `actionlint .github/workflows/ci.yml`
  - PASS.
- `rg -n "\\\\'" .github/workflows/ci.yml`
  - PASS: no escaped quotes.
- `git diff --check`
  - PASS.
- `./gradlew projects --no-configuration-cache | rg 'Project .:(bluetape4k-(jackson2|grpc|tink|http|retrofit2|vertx))'`
  - PASS: all six projects are registered.
- `./gradlew :bluetape4k-io:test :bluetape4k-okio:test :bluetape4k-json:test :bluetape4k-jackson2:test :bluetape4k-jackson3:test :bluetape4k-grpc:test :bluetape4k-tink:test --parallel --dry-run --no-configuration-cache`
  - PASS: `BUILD SUCCESSFUL`.
- `./gradlew :bluetape4k-feign:test :bluetape4k-http:test :bluetape4k-retrofit2:test :bluetape4k-vertx:test --dry-run --no-configuration-cache`
  - PASS: `BUILD SUCCESSFUL`.
- `./gradlew :bluetape4k-jackson2:koverXmlReport :bluetape4k-grpc:koverXmlReport :bluetape4k-tink:koverXmlReport :bluetape4k-http:koverXmlReport :bluetape4k-retrofit2:koverXmlReport :bluetape4k-vertx:koverXmlReport --dry-run --no-configuration-cache`
  - PASS: `BUILD SUCCESSFUL`.

## Residual Risk

- Local validation proves workflow syntax and task wiring. The actual path-filter/job execution proof must come from GitHub PR CI because the changed file itself triggers the shared CI path.
