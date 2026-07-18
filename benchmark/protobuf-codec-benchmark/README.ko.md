# Protobuf Codec Benchmark

[English](./README.md) | 한국어

이 모듈은 issue #757의 결정론적 JMH allocation 근거를 수집합니다. `ProtobufSerializer`의 기존 `ByteArray`
경로와 caller-owned `ByteBuffer` 경로, `RedissonProtobufCodec`의 copied/contiguous/composite decode 경로를 같은
fixture에서 비교합니다. throughput은 진단 지표로 보존하지만 claim 판정에는 `gc.alloc.rate.norm` (`B/op`)만
사용합니다.

## 정확한 method matrix

runner와 validator는 아래 13개 method만 허용합니다. 누락, 중복, 추가 method가 있으면 validation이 실패합니다.

| Method | 비교 역할 | Claim 가능 |
|---|---|---|
| `serializerEncodeByteArray` | Serializer encode baseline | 아니요 |
| `serializerEncodeHeapOptimized` | Heap caller-buffer candidate | 예 |
| `serializerEncodeDirectOptimized` | Direct caller-buffer candidate | 예 |
| `serializerDecodeByteArray` | Serializer decode baseline | 아니요 |
| `serializerDecodeHeapOptimized` | Heap source-buffer candidate | 예 |
| `serializerDecodeDirectOptimized` | Direct source-buffer candidate | 예 |
| `redissonDecodeCopiedByteArray` | Redisson copied baseline | 아니요 |
| `redissonDecodeContiguousOptimized` | Contiguous `ByteBuf` candidate | 예 |
| `redissonDecodeCompositeCompatibility` | Composite copied compatibility control | 아니요 |
| `trustedFallbackEncodeByteArray` | Trusted fallback encode control | 아니요 |
| `trustedFallbackEncodeBufferCompatibility` | Trusted fallback buffer encode control | 아니요 |
| `trustedFallbackDecodeByteArray` | Trusted fallback decode control | 아니요 |
| `trustedFallbackDecodeBufferCompatibility` | Trusted fallback buffer decode control | 아니요 |

`*Optimized` 5개 method만 positive allocation claim 대상입니다. compatibility와 fallback cell도 결과에는 남기지만
claim에는 사용할 수 없습니다.

## Build와 smoke validation

실행 가능한 benchmark JAR 하나를 만들고 path, hash, file identity를 고정한 다음 clean committed tree에서 짧은
smoke profile을 실행합니다:

```bash
./gradlew :protobuf-codec-benchmark:benchmarkBenchmarkJar --no-configuration-cache
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py resolve-jar \
  --jar-dir benchmark/protobuf-codec-benchmark/build/benchmarks/benchmark/jars \
  --state benchmark/protobuf-codec-benchmark/build/issue-757-smoke/jar.json
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py run \
  --state benchmark/protobuf-codec-benchmark/build/issue-757-smoke/jar.json \
  --profile smoke --concurrent-heavy-work absent \
  --output-root benchmark/protobuf-codec-benchmark/build/issue-757-smoke
```

Smoke profile은 `-t 1 -f 1 -wi 1 -i 1 -w 1s -r 1s -prof gc -rf json`으로 고정됩니다. 13개 cell이 schema,
provenance, throughput, allocation metric을 모두 생성하는지만 확인하며 공개 성능 근거로는 사용할 수 없습니다.
Canonical 측정은 새로운 state와 output root에서 시작합니다.

## Canonical 2회 측정 protocol

fresh clean/JAR build 뒤 `build/issue-757-evidence/`에 새 state를 만들고, 고정한 JAR을 다시 build하거나 교체하지
않은 채 canonical profile을 두 번 실행합니다:

```bash
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py resolve-jar \
  --jar-dir benchmark/protobuf-codec-benchmark/build/benchmarks/benchmark/jars \
  --state benchmark/protobuf-codec-benchmark/build/issue-757-evidence/jar.json
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py run \
  --state benchmark/protobuf-codec-benchmark/build/issue-757-evidence/jar.json \
  --profile canonical --concurrent-heavy-work absent \
  --output-root benchmark/protobuf-codec-benchmark/build/issue-757-evidence
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py run \
  --state benchmark/protobuf-codec-benchmark/build/issue-757-evidence/jar.json \
  --profile canonical --concurrent-heavy-work absent \
  --output-root benchmark/protobuf-codec-benchmark/build/issue-757-evidence
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py compare \
  --state benchmark/protobuf-codec-benchmark/build/issue-757-evidence/jar.json \
  --output benchmark/protobuf-codec-benchmark/build/issue-757-evidence/comparison.csv \
  --validation benchmark/protobuf-codec-benchmark/build/issue-757-evidence/validation.json
```

Canonical JMH 인자는 다음 값으로 고정됩니다:

```text
-t 1 -f 2 -wi 3 -i 5 -w 1s -r 1s -prof gc -rf json
-jvmArgsAppend "-Xms1g -Xmx1g -XX:+UseG1GC"
```

`run-evidence.py run`은 각 run마다 `validate-jmh.py run`을 호출합니다. 이어서 `run-evidence.py compare`가 동등한
2-run comparison validation을 실행하고 결과를 state에 결합합니다. 수동 진단 시에는 정확한 validator CLI
contract를 다음 명령으로 확인할 수 있습니다:

```bash
python3 benchmark/protobuf-codec-benchmark/scripts/validate-jmh.py run --help
python3 benchmark/protobuf-codec-benchmark/scripts/validate-jmh.py compare --help
```

각 run directory에는 `environment.json`, `metadata.json`, `argv.json`, `run.log`, `jmh.json`, `summary.csv`,
`validation.json`이 생성됩니다. 최종 promoted evidence는 `docs/benchmarks/raw/issue-757/`에 두며, 최종 report는
검증된 delivery manifest에서만 생성합니다.

## 판정 규칙과 한계

각 eligible candidate의 `gc.alloc.rate.norm` delta를 연결된 baseline과 비교해 두 canonical run에서 독립적으로
판정합니다:

- `accepted`: 두 delta가 모두 `-5%` 이하일 때만 좁은 범위의 measured allocation reduction claim을 허용합니다.
- `inconclusive`: 방향이 섞이거나 어느 한 결과라도 `-5%` 초과 `+5%` 미만이면 올바른 코드는 유지하되
  reduction claim은 쓰지 않습니다.
- `regressed`: 두 delta가 모두 `+5%` 이상이면 기록된 rollback 절차를 수행하고 fresh run 2회를 다시 수집합니다.
- `ineligible`: baseline, composite, trusted-fallback control이며 positive claim에 사용하지 않습니다.

이 측정은 zero-copy를 증명하지 않습니다. Protobuf, Netty, direct buffer, fallback codec 내부에서 copy나 allocation이
계속 발생할 수 있습니다. 또한 다른 payload, JDK, 장비, concurrency, storage 경계 전반의 throughput 향상이나 보장을
의미하지 않습니다.
