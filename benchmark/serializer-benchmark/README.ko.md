# Serializer 할당 벤치마크

이 비배포 모듈은 기존 serializer의 `ByteArray`, 호환 `ByteBuffer`, 최적화 `ByteBuffer` 경로에서 할당 동작을 검증합니다. production dispatch, wire format, 소유권, 보안 설정은 변경하지 않습니다.

## 명령

```bash
./gradlew :serializer-benchmark:benchmarkBenchmarkCompile --no-configuration-cache
./gradlew :serializer-benchmark:benchmarkBenchmarkJar --no-configuration-cache
java -jar build/benchmarks/benchmark/jars/*-JMH.jar -l
java -jar build/benchmarks/benchmark/jars/*-JMH.jar '.*SerializerAllocationBenchmark.*' \
  -t 1 -f 2 -wi 3 -i 5 -w 1s -r 1s -prof gc -rf json -rff jmh.json
```

주 지표는 `gc.alloc.rate.norm`(B/op)이고 throughput은 진단용입니다. 두 번의 새 run이 모두 같은 방향으로 5% 이상 개선될 때만 긍정적인 할당 근거로 인정합니다.

## 매트릭스

40개 셀은 JDK(6), Kryo(6), Fory(4), Jackson 2(6), Jackson 3(6), Fastjson2(6), Avro reflect(6)를 포함하며 직렬화와 역직렬화를 분리합니다. 호환 및 fallback 셀은 사용 편의성 비교 전용입니다.

| Backend | 출력 | 입력 |
|---|---|---|
| JDK, Kryo | concrete 최적화 경로 | concrete 최적화 경로 |
| Fory | fallback | concrete 최적화 경로 |
| Jackson 2/3 | concrete 최적화 경로 | concrete 최적화 경로 |
| Fastjson2 | fallback | array-backed 최적화, direct/read-only fallback |
| Avro reflect | concrete 최적화 경로 | concrete 최적화 경로 |

## Buffer 계약

호출자는 남은 용량이 충분한 writable target을 소유하고 재사용해야 합니다. 출력 성공 시 기록한 크기만큼 `position`이 이동하고 `limit`은 넓어지지 않습니다. overflow/read-only 실패 시 호출자 상태를 rollback합니다. 입력은 duplicate view로 읽어 source의 `position`과 `limit`을 보존합니다.

Kotlin과 Java는 같은 public `serializeTo`/`deserializeFrom` 메서드를 사용합니다. 벤치마크 fixture는 Kotlin 호출을, 각 public 모듈 README는 두 언어 사용법을 제공합니다.

[2026-07-18 보고서](../../docs/benchmarks/2026-07-18-bytebuffer-serializer-allocation.md)와 커밋된 raw 근거를 참고하십시오. #755, #756, #757, #758은 명시적으로 범위 밖입니다.
