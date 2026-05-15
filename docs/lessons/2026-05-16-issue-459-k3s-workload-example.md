# Issue 459 K3s workload example

## Context

Issue #459 asked for a practical `K3sServer` integration-test example that goes
beyond node listing and demonstrates Kubernetes workload resources against a
real K3s API server.

## Decision

Add a dedicated `K3sWorkloadExampleTest` under `testing/testcontainers` instead
of expanding `K3sServerTest`. The new file keeps the basic server smoke tests
separate from workload examples and uses the existing `@Tag("k8s")`/`k8sTest`
path for privileged-runner execution.

## Outcome

The example covers ConfigMap CRUD, Deployment-backed Service readiness, and
Secret value decoding. Each test closes its Kubernetes client, performs
idempotent pre-cleanup that waits for stale resources to disappear, and cleans
created resources with `try/finally` so the singleton K3s container can be
reused safely.

## Verification

- `./gradlew :bluetape4k-testcontainers:compileTestKotlin --no-daemon --console=plain`
  passed.
- `./gradlew :bluetape4k-testcontainers:k8sTest --tests 'io.bluetape4k.testcontainers.infra.K3sWorkloadExampleTest' --no-daemon --no-configuration-cache --console=plain`
  passed with 3 tests executed.

## Next Time

When adding K3s examples, keep long-running or privileged Docker scenarios under
`@Tag("k8s")`, run the dedicated `k8sTest` task locally when Docker supports it,
and use both wait-backed pre-cleanup and `finally` cleanup for
singleton-container safety.
