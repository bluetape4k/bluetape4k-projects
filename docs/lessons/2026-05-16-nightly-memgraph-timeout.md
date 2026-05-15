# Nightly Memgraph Timeout

## Context

Nightly full run `25942332959` failed only in `Test / Testcontainers (graphdb-memgraph)`.
The Memgraph job reached `:bluetape4k-testcontainers:test` and then hit the
outer GitHub Actions timeout twice without a JUnit failure report.

## Decision or Finding

`MemgraphServer` waited only for a listening port. For Memgraph, this can leave
CI failures opaque because the test process may hang until the workflow timeout.
Use a combined startup wait (`Memgraph` startup log plus listening port) and add
preemptive JUnit timeouts around the integration test class.

## Outcome

`MemgraphServerTest` now starts an isolated container in `BeforeAll`, closes it
in `AfterAll`, caps the full class at 5 minutes, and gives the Bolt query test a
3-minute timeout. This keeps CI failures inside JUnit/Gradle instead of waiting
for the 25-minute Actions wrapper.

## Verification

- IDE diagnostics: `MemgraphServer.kt` and `MemgraphServerTest.kt` reported 0 problems.
- Targeted test: `:bluetape4k-testcontainers:test --tests io.bluetape4k.testcontainers.graphdb.MemgraphServerTest` passed in 11 seconds.

## Future Guidance

For Testcontainers services that have known readiness logs, prefer log plus
port wait strategies. For CI-only hangs, add a JUnit-level timeout close to the
test boundary so retry logs show a real test failure instead of only process
exit code 124.
