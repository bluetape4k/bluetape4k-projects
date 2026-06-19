# Protobuf Codec Benchmark

[English](./README.md) | 한국어

이 모듈은 bluetape4k 직렬화와 Redisson 연동 코드에서 사용하는 protobuf
codec 경로를 측정합니다. 단일 protobuf payload를 메모리에 두고 encode/decode
처리량을 비교하므로, codec 구현 간 상대적인 차이를 보기 위한 좁은 범위의
benchmark입니다.

![Protobuf codec throughput](../../docs/images/readme-charts/benchmark-protobuf-codec-throughput-chart-01.png)

## 무엇을 측정하나

Benchmark 클래스는
`io.bluetape4k.protobuf.benchmark.ProtobufCodecBenchmark`입니다.

JMH throughput 모드로 실행하며, warmup 2회와 measurement 3회를 각각 1초씩
수행합니다. 측정 payload는 안정적인 `id`, `name`, 반복 문자열 payload를 가진
`BenchmarkMessage`입니다. fallback benchmark는 protobuf가 아닌
`Serializable` Kotlin data object를 사용해 fallback 직렬화 경로를 측정합니다.

| Benchmark | 목적 |
|---|---|
| `redissonProtobufEncode` | `RedissonProtobufCodec`으로 protobuf message를 encode합니다. |
| `redissonProtobufEncodeByteArrayWrappedBaseline` | 이미 감싼 protobuf byte array를 Redisson codec 경로로 encode합니다. |
| `redissonProtobufDecode` | Redisson protobuf payload를 protobuf message 타입으로 decode합니다. |
| `protobufSerializerEncode` | `ProtobufSerializer`로 protobuf message를 encode합니다. |
| `protobufSerializerFallbackEncode` | protobuf가 아닌 직렬화 가능 객체를 fallback 경로로 encode합니다. |

## 최근 로컬 결과

실행일: 2026-06-19. Runtime: GraalVM JDK 21.0.11. Mode: JMH throughput,
single thread, one fork, `ops/s`.

| Benchmark | Score | Error |
|---|---:|---:|
| `redissonProtobufEncodeByteArrayWrappedBaseline` | 4,029,806 ops/s | ±137,575 |
| `redissonProtobufEncode` | 3,889,386 ops/s | ±624,283 |
| `protobufSerializerEncode` | 3,714,491 ops/s | ±6,802,622 |
| `redissonProtobufDecode` | 3,387,060 ops/s | ±479,845 |
| `protobufSerializerFallbackEncode` | 1,440,791 ops/s | ±370,311 |

이 결과는 codec 경로의 상대적인 양상을 보기 위한 로컬 snapshot입니다. 절대
성능 보증으로 사용하기에는 측정 시간이 짧습니다. 특히
`protobufSerializerEncode`의 error 범위가 크므로, release 근거로 쓰려면 더 긴
measurement window로 다시 측정해야 합니다.

## 실행

```bash
./gradlew :protobuf-codec-benchmark:benchmarkBenchmark
```

원본 JMH JSON report는
`benchmark/protobuf-codec-benchmark/build/reports/benchmarks/main/` 아래에
생성됩니다.
