# Protobuf Codec Benchmark

[English](./README.md) | 한국어

이 모듈은 issue #757의 결정론적 JMH allocation 근거를 수집합니다. `ProtobufSerializer`의 기존 `ByteArray`
경로와 caller-owned `ByteBuffer` encode 경로 및 상속된 decode compatibility 경로,
`RedissonProtobufCodec`의 copied/contiguous/composite decode 경로, `LettuceProtobufCodecs`의 copied 경로와
caller-owned `ByteBuf` encode 경로를 같은 fixture에서 비교합니다. throughput은 진단 지표로 보존하지만 claim
판정에는 `gc.alloc.rate.norm` (`B/op`)만 사용합니다.

## 정확한 method matrix

runner와 validator는 아래 17개 method만 허용합니다. 누락, 중복, 추가 method가 있으면 validation이 실패합니다.

| Method | 비교 역할 | Claim 가능 |
|---|---|---|
| `serializerEncodeByteArray` | Serializer encode baseline | 아니요 |
| `serializerEncodeHeapOptimized` | Heap caller-buffer candidate | 예 |
| `serializerEncodeDirectOptimized` | Direct caller-buffer candidate | 예 |
| `serializerDecodeByteArray` | Serializer decode baseline | 아니요 |
| `serializerDecodeHeapOptimized` | Heap source-buffer compatibility 측정 | 아니요 |
| `serializerDecodeDirectOptimized` | Direct source-buffer compatibility 측정 | 아니요 |
| `redissonDecodeCopiedByteArray` | Redisson copied baseline | 아니요 |
| `redissonDecodeContiguousOptimized` | Contiguous `ByteBuf` candidate | 예 |
| `redissonDecodeCompositeCompatibility` | Composite copied compatibility control | 아니요 |
| `lettuceEncodeHeapCopied` | Heap copied baseline | 아니요 |
| `lettuceEncodeHeapOptimized` | Heap caller-owned `ByteBuf` candidate | 예 |
| `lettuceEncodeDirectCopied` | Direct copied baseline | 아니요 |
| `lettuceEncodeDirectOptimized` | Direct caller-owned `ByteBuf` candidate | 예 |
| `trustedFallbackEncodeByteArray` | Trusted fallback encode control | 아니요 |
| `trustedFallbackEncodeBufferCompatibility` | Trusted fallback buffer encode control | 아니요 |
| `trustedFallbackDecodeByteArray` | Trusted fallback decode control | 아니요 |
| `trustedFallbackDecodeBufferCompatibility` | Trusted fallback buffer decode control | 아니요 |

유지된 serializer, Redisson, Lettuce의 `*Optimized` method 5개만 positive allocation claim 대상입니다.
Serializer decode method 2개는 shared direct decode dispatch 롤백 뒤 최종 compatibility 측정을 위해 정확한
matrix에 남겨 두며, baseline, 다른 compatibility control 및 fallback cell과 함께 claim에는 사용할 수 없습니다.
커밋된 report는 Lettuce heap/direct 결과를 accepted로 기록하지만 zero-copy나 일반 throughput 향상을 주장하지 않습니다.

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

Smoke profile은 `-t 1 -f 1 -wi 1 -i 1 -w 1s -r 1s -prof gc -rf json`으로 고정됩니다. 17개 cell이 schema,
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
`validation.json`이 생성됩니다. 최종 evidence는 immutable generation으로 publish하고 hash-bound active pointer를
검증합니다:

```bash
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py publish-generation \
  --state .omx/evidence/issue-757-jmh-state.json \
  --evidence-root docs/benchmarks/raw/issue-757 \
  --control-root .omx/evidence/issue-757-promotion \
  --owner issue-757-lettuce \
  --legacy-manifest docs/benchmarks/raw/issue-757/delivery-manifest.json \
  --report-output docs/benchmarks/2026-07-18-protobuf-buffer-allocation.md
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py verify-active-generation \
  --evidence-root docs/benchmarks/raw/issue-757
```

Publisher는 exclusive lock, monotonic fencing token, platform atomic no-replace directory rename, fsync와 active pointer
compare-and-swap을 사용하고 이전 generation을 모두 보존합니다. Active generation의 검증된 delivery manifest에서
결정적으로 생성한 report hash도 pointer에 결합합니다. Report/evidence 전용 변경을 commit한 뒤
`validate-final-head --manifest <active-generation>/delivery-manifest.json`을 실행합니다. Measurement 이후 production,
build, test, benchmark 또는 KDoc drift가 있으면 fail-closed됩니다.

## 판정 규칙과 한계

각 eligible candidate의 `gc.alloc.rate.norm` delta를 연결된 baseline과 비교해 두 canonical run에서 독립적으로
판정합니다:

- `accepted`: 두 delta가 모두 `-5%` 이하일 때만 좁은 범위의 measured allocation reduction claim을 허용합니다.
- `inconclusive`: 방향이 섞이거나 어느 한 결과라도 `-5%` 초과 `+5%` 미만이면 올바른 코드는 유지하되
  reduction claim은 쓰지 않습니다.
- `regressed`: 두 delta가 모두 `+5%` 이상이면 기록된 rollback 절차를 수행하고 fresh run 2회를 다시 수집합니다.
- `ineligible`: baseline, composite, trusted-fallback control이며 positive claim에 사용하지 않습니다.

Mapped regression이 발생하면 변경 전 clean measurement head에서
`record-rollback --archive-root .omx/evidence/issue-757-rollback`을 실행해 immutable v2 preparation을 만듭니다.
이 durable ignored root는 Gradle clean 대상인 module `build/` 밖에 있습니다. Immutable rollback preparation,
bundle, archive를 `benchmark/protobuf-codec-benchmark/build/` 아래에 저장하지 마세요. Source rollback을 적용하고
commit한 다음 `finalize-rollback --preparation <path>`을 실행합니다. Fresh `resolve-jar --rollback-bundle`에는 finalized v2 bundle만 사용할 수 있으며 v1 bundle과
preparation file은 fail-closed됩니다. `regressed_cells`는 실제 non-empty trigger subset이고 `removed_cells`는
`ineligible`/`removed_after_regression`이 되는 dispatch 전체 mapping입니다. Bound source lineage를 rebase 또는
amend하면 preparation/bundle이 무효가 되므로 exact measurement head부터 절차를 다시 시작합니다.
`lettuce_encode` finalization은 승인된 canonical path/blob contract와 baseline ABI exact-equality verifier를 추가로
요구합니다. Public CLI는 rejected-terminal workflow가 이 verifier를 제공하기 전까지 해당 rollback을 의도적으로
차단합니다.

이 측정은 zero-copy를 증명하지 않습니다. Protobuf, Netty, direct buffer, fallback codec 내부에서 copy나 allocation이
계속 발생할 수 있습니다. 또한 다른 payload, JDK, 장비, concurrency, storage 경계 전반의 throughput 향상이나 보장을
의미하지 않습니다.
