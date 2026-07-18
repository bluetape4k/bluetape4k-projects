# Protobuf Codec Benchmark

English | [한국어](./README.ko.md)

This module collects deterministic JMH allocation evidence for issue #757. It compares the existing `ByteArray`
paths with caller-owned `ByteBuffer` encode paths and inherited decode compatibility paths in `ProtobufSerializer`,
plus copied, contiguous, and composite decode paths in `RedissonProtobufCodec`. Throughput is retained as a diagnostic
metric; `gc.alloc.rate.norm` (`B/op`) is the claim gate.

## Exact Method Matrix

The runner and validator require exactly these 13 methods. Missing, duplicated, or additional methods fail validation.

| Method | Comparison role | Claim eligible |
|---|---|---|
| `serializerEncodeByteArray` | Serializer encode baseline | No |
| `serializerEncodeHeapOptimized` | Heap caller-buffer candidate | Yes |
| `serializerEncodeDirectOptimized` | Direct caller-buffer candidate | Yes |
| `serializerDecodeByteArray` | Serializer decode baseline | No |
| `serializerDecodeHeapOptimized` | Heap source-buffer compatibility measurement | No |
| `serializerDecodeDirectOptimized` | Direct source-buffer compatibility measurement | No |
| `redissonDecodeCopiedByteArray` | Redisson copied baseline | No |
| `redissonDecodeContiguousOptimized` | Contiguous `ByteBuf` candidate | Yes |
| `redissonDecodeCompositeCompatibility` | Composite copied compatibility control | No |
| `trustedFallbackEncodeByteArray` | Trusted fallback encode control | No |
| `trustedFallbackEncodeBufferCompatibility` | Trusted fallback buffer encode control | No |
| `trustedFallbackDecodeByteArray` | Trusted fallback decode control | No |
| `trustedFallbackDecodeBufferCompatibility` | Trusted fallback buffer decode control | No |

Only the three retained encode and Redisson `*Optimized` methods are eligible for a positive allocation claim. The two
serializer decode methods remain in the exact matrix for final compatibility measurement after their shared direct
decode dispatch was rolled back; they, the other compatibility controls, and fallback cells remain claim-ineligible.

## Build and Smoke Validation

Build one runnable benchmark JAR, pin its path, hash, and file identity, then run the short smoke profile from a clean
committed tree:

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

Smoke uses `-t 1 -f 1 -wi 1 -i 1 -w 1s -r 1s -prof gc -rf json`. It proves that all 13 cells emit the required
schema, provenance, throughput, and allocation metrics; it is not publishable performance evidence. Use a fresh state
and output root for canonical evidence.

## Canonical Two-Run Protocol

After a fresh clean/JAR build, resolve a new state under `build/issue-757-evidence/` and run the canonical profile twice
without rebuilding or replacing the pinned JAR:

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

The canonical JMH arguments are fixed:

```text
-t 1 -f 2 -wi 3 -i 5 -w 1s -r 1s -prof gc -rf json
-jvmArgsAppend "-Xms1g -Xmx1g -XX:+UseG1GC"
```

`run-evidence.py run` invokes `validate-jmh.py run` for each run. `run-evidence.py compare` then invokes the equivalent
two-run comparison validation and binds its outputs to state. For manual diagnosis, inspect the exact validator CLI
contracts with:

```bash
python3 benchmark/protobuf-codec-benchmark/scripts/validate-jmh.py run --help
python3 benchmark/protobuf-codec-benchmark/scripts/validate-jmh.py compare --help
```

Each run directory contains `environment.json`, `metadata.json`, `argv.json`, `run.log`, `jmh.json`, `summary.csv`, and
`validation.json`. Final promoted evidence belongs under `docs/benchmarks/raw/issue-757/`; the final report is generated
only from a verified delivery manifest.

## Decision Rule and Limits

For each eligible candidate, calculate its `gc.alloc.rate.norm` delta against the mapped baseline independently in both
canonical runs:

- `accepted`: both deltas are at most `-5%`; a narrowly scoped measured allocation-reduction claim is allowed.
- `inconclusive`: the direction is mixed or either result is strictly between `-5%` and `+5%`; retain correct code
  without a reduction claim.
- `regressed`: both deltas are at least `+5%`; follow the recorded rollback workflow and collect two fresh runs.
- `ineligible`: baseline, composite, or trusted-fallback control; never use it for a positive claim.

For a mapped regression, run `record-rollback` on the unchanged clean measurement head to create an immutable v2
preparation. Apply and commit the source rollback, then run `finalize-rollback --preparation <path>`. Only the finalized
v2 bundle may be passed to fresh `resolve-jar --rollback-bundle`; v1 bundles and preparation files fail closed. A
decision's `regressed_cells` is the actual non-empty trigger subset, while `removed_cells` is the full dispatch mapping
that becomes `ineligible` with reason `removed_after_regression`. Rebasing or amending the bound source lineage
invalidates the preparation/bundle and requires the workflow to restart from the exact measurement head.

These measurements do not prove zero-copy behavior. Protobuf, Netty, direct buffers, or fallback codecs may still copy
or allocate internally. They also do not establish a general throughput improvement or guarantee for other payloads,
JDKs, machines, concurrency levels, or storage boundaries.
