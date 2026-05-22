# Lessons Learned - Issue 602 Memgraph Driver Close Timeout (2026-05-22)

**Related issue**: #602
**Affected module**: `:bluetape4k-testcontainers`

## Context

Full Nightly for the 1.9.0 release candidate repeatedly timed out in
`MemgraphServerTest` after a successful Bolt query path. The failing stack was
inside Neo4j Java Driver `Driver.close()`, not container startup.

## Decision

Keep the Memgraph image and Neo4j driver versions pinned because the repository
already uses the current release line at the time of investigation:
`memgraph/memgraph:3.9.0`, `neo4j-java-driver:6.1.0`, and
`neo4j-bolt-connection-netty:11.0.2`.

For Memgraph compatibility tests, constrain the driver to a single connection
pool/event-loop, disable telemetry and auto-commit retries, and close the driver
through `closeAsync()` with a bounded timeout. The test's release-gate assertion
remains the successful Bolt query; cleanup must not hang the nightly runner
indefinitely.

## Verification

- `./gradlew :bluetape4k-testcontainers:test --tests io.bluetape4k.testcontainers.graphdb.MemgraphServerTest --no-configuration-cache --no-build-cache --rerun-tasks --max-workers=1`
  - Result: `BUILD SUCCESSFUL`, 6 passing.
- `./gradlew :bluetape4k-testcontainers:test --tests 'io.bluetape4k.testcontainers.graphdb.*' --no-configuration-cache --no-build-cache --rerun-tasks --max-workers=1`
  - Result: `BUILD SUCCESSFUL`, 27 passing.

## Future Guidance

When a Testcontainers-backed driver test proves connectivity but hangs during
client cleanup, inspect the close stack before changing container readiness.
Prefer bounded cleanup around the affected client in tests over widening global
timeouts.
