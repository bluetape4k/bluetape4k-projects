# infra/kafka Review, Tests, and Docs Design

## Scope

- Module: `infra/kafka`
- Work type: review-driven test and documentation hardening.
- Verification mode: GitHub CI only, per latest user direction.

## Findings

### P1 - codec documentation still references FST

`KafkaCodecs` exposes JDK, Kryo, and Fory binary codecs. README and public KDoc still describe the binary family as `FST` in several places, and the English table describes `KafkaCodecs.Fory` as FST serialization.

### P1 - README codec matrix omits public codec constants

The public API includes Fory-backed compressed codecs and additional Snappy/Zstd combinations:

- `KafkaCodecs.Lz4Fory`
- `KafkaCodecs.SnappyKryo`
- `KafkaCodecs.SnappyFory`
- `KafkaCodecs.ZstdJdk`
- `KafkaCodecs.ZstdFory`

The README table and diagram should list the same public surface.

### P2 - coroutine producer edge behavior is under-tested

`Producer.suspendSend` wraps Kafka callback futures with `suspendCancellableCoroutine`, but existing tests mostly cover successful sends through a real Kafka broker. Missing edge coverage:

- callback exception propagates to the suspended caller.
- coroutine cancellation cancels the Kafka `Future` with interruption.
- repeated parallel suspend sends remain stable under the bluetape4k coroutine stress tester.

### P2 - topic partition KDoc names a stale validation helper

`topicPartitionOf` uses `requireNotBlank("tp")`; KDoc still mentions `assertNotBlank("tp")`.

## Acceptance Criteria

- README and README.ko no longer reference FST for Kafka binary codecs.
- README codec table and diagram include all public `KafkaCodecs` constants.
- Public KDoc for `KafkaCodecs`, `Producer.suspendSend`, and `topicPartitionOf` reflects current behavior.
- Edge tests cover `suspendSend` failure, cancellation, and `SuspendedJobTester` stress behavior without adding new runtime dependencies.
- 6-Tier review gate has no remaining P0/P1 findings for this scope.

