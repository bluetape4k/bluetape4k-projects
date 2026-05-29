# Issue 649 Protobuf Codec Allocation Design

## Context

`RedissonProtobufCodec` encoded protobuf messages with
`Any.pack(message).toByteArray()` and then wrapped the byte array into a Netty
`ByteBuf`. That allocates an intermediate array even though Redisson's encoder
contract can return a `ByteBuf` directly.

`ProtobufSerializer` still returns `ByteArray` by `BinarySerializer` contract,
so this change keeps that path unchanged and benchmarked as a comparison point.

## Decision

Encode protobuf messages in `RedissonProtobufCodec` by pre-sizing a Netty buffer
from `Any.serializedSize` and writing the packed message through
`CodedOutputStream` over the buffer's `ByteBuffer` view.

## Security

The decode allowlist and class loading behavior are unchanged. This change only
updates the protobuf encode path and keeps fallback codec delegation intact.

## Measurement

Add `ProtobufCodecBenchmark` under `benchmark/protobuf-codec-benchmark` for:

- Redisson protobuf encode.
- Redisson protobuf encode baseline using the old `toByteArray()` plus
  `wrappedBuffer()` path.
- Redisson protobuf decode.
- `ProtobufSerializer` protobuf encode.
- `ProtobufSerializer` fallback encode.

Local sample on 2026-05-29, macOS, Java 21, 2 warmup / 3 measurement
iterations:

| Benchmark | Score |
|---|---:|
| `redissonProtobufEncode` | 3,936,257 ops/s |
| `redissonProtobufEncodeByteArrayWrappedBaseline` | 4,056,681 ops/s |
| `redissonProtobufDecode` | 3,294,397 ops/s |
| `protobufSerializerEncode` | 4,083,259 ops/s |
| `protobufSerializerFallbackEncode` | 1,472,371 ops/s |

The direct buffer path keeps throughput within the short-run error interval of
the baseline while removing the explicit `toByteArray()` encode handoff.
