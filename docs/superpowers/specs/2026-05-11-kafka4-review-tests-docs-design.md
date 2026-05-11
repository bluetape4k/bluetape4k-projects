# infra/kafka4 Review, Tests, and Docs Design

## Scope

- Module: `infra/kafka4`
- Work type: review-driven test and documentation hardening.
- Verification mode: GitHub CI only, with local compile check for edited tests.

## Findings

### P1 - README lacks the public codec matrix

`KafkaCodecs` exposes string, byte array, Jackson 3, JDK, Kryo, Fory, and compressed codec combinations. README and README.ko describe the families but do not list the public constants, so users cannot verify the Kafka 4 public API surface from the module docs.

### P2 - coroutine producer edge behavior is under-tested

`Producer.suspendSend` wraps Kafka callback futures with `suspendCancellableCoroutine`, but existing tests cover successful broker sends only. Missing edge coverage:

- callback exception propagation.
- coroutine cancellation cancelling the Kafka `Future` with interruption.
- repeated parallel suspend sends under `SuspendedJobTester`.

### P2 - codec KDoc example favors Kryo over the current recommended Fory path

`KafkaCodecs` KDoc already names Fory, but the example still shows LZ4 + Kryo. A Fory example better matches current guidance and README security notes.

## Acceptance Criteria

- README and README.ko list every public `KafkaCodecs` constant.
- Public KDoc for `KafkaCodecs` and `Producer.suspendSend` reflects current behavior and examples.
- Edge tests cover `suspendSend` failure, cancellation, and `SuspendedJobTester` stress behavior without adding new runtime dependencies.
- 6-Tier review gate has no remaining P0/P1 findings for this scope.

