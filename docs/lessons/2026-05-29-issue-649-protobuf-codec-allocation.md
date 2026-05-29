# Issue 649 Protobuf Codec Allocation

## Context

The Redisson protobuf encoder had a removable allocation: protobuf `Any` was
serialized to `ByteArray` and then wrapped as a Netty buffer.

## Decision

Use `Any.serializedSize` plus `CodedOutputStream` over a Netty buffer's
`ByteBuffer` view for the Redisson fast path.
Keep `ProtobufSerializer` unchanged because its public contract returns
`ByteArray`.

## Verification

Targeted protobuf codec tests passed. A short local benchmark sample from
`:protobuf-codec-benchmark:benchmarkBenchmark` showed `redissonProtobufEncode`
at 3,936,257 ops/s versus the old `toByteArray()`/`wrappedBuffer()` baseline at
4,056,681 ops/s, within the short-run error interval. Decode and fallback
benchmarks ran in the same module without weakening the protobuf allowlist.

## Future Guidance

Only optimize encode paths where the owning API returns a buffer or stream. Put
benchmark harnesses under `benchmark/*-benchmark`, not production modules. Do
not weaken protobuf decode allowlists for benchmark wins. Keep the old encode
baseline in benchmark code so future allocation profilers can compare both
paths directly.
