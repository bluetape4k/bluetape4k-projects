# 이슈 649 Protobuf codec allocation 감소

## 배경

Redisson protobuf encoder에는 제거 가능한 allocation이 있었다. protobuf `Any`를
`ByteArray`로 serialize한 뒤 Netty buffer로 감쌌다.

## 결정

Redisson fast path에는 Netty buffer의 `ByteBuffer` view 위에서 `Any.serializedSize`와
`CodedOutputStream`을 사용한다. public contract가 `ByteArray`를 반환하므로
`ProtobufSerializer`는 변경하지 않는다.

## 검증

targeted protobuf codec test가 통과했다. `:protobuf-codec-benchmark:benchmarkBenchmark`의
짧은 local benchmark sample은 `redissonProtobufEncode`가 3,936,257 ops/s이고 기존
`toByteArray()`/`wrappedBuffer()` baseline이 4,056,681 ops/s로 short-run error interval
안에 있음을 보였다. decode와 fallback benchmark도 protobuf allowlist를 약화하지 않은
상태로 같은 module에서 실행됐다.

## 향후 지침

owning API가 buffer나 stream을 반환하는 encode path만 최적화한다. benchmark harness는
production module이 아니라 `benchmark/*-benchmark` 아래에 둔다. benchmark 성과를 위해
protobuf decode allowlist를 약화하지 않는다. future allocation profiler가 두 path를
직접 비교할 수 있도록 기존 encode baseline은 benchmark code에 유지한다.
