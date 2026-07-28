# Kafka ByteBuffer Codec Allocation Benchmark - 2026-07-19

## 범위

Issue [#758](https://github.com/bluetape4k/bluetape4k-projects/issues/758)는 같은 Kryo-backed
`BinaryKafkaCodec` payload에 대해 standard Kafka `ByteArray` codec call과 opt-in
caller-owned `ByteBuffer` call을 비교한다. Benchmark는 codec serialization과
deserialization을 분리한다. Kafka broker, network transport, batching, compression,
header creation은 제외한다.

## 명령

Benchmark jar를 build하기 전에 생성된 benchmark task를 다시 확인했다.

```bash
./gradlew :serializer-benchmark:tasks --all --no-configuration-cache | \
  rg 'benchmarkBenchmark(Compile|Jar)|compileBenchmarkKotlin'

./gradlew :serializer-benchmark:clean :serializer-benchmark:benchmarkBenchmarkJar \
  --no-configuration-cache --no-build-cache

exact_jar='benchmark/serializer-benchmark/build/benchmarks/benchmark/jars/serializer-benchmark-benchmark-jmh-1.12.0-JMH.jar'

java -jar "$exact_jar" '.*KafkaCodecAllocationBenchmark.*' \
  -t 1 -f 1 -wi 1 -i 1 -w 1s -r 1s -prof gc
```

첫 번째 evidence run은 다음을 사용했다.

```bash
evidence_root='docs/benchmarks/raw/issue-758'
first_run_id="run-$(date -u +%Y%m%dT%H%M%SZ)"
first_run_dir="$evidence_root/$first_run_id"
mkdir -p "$first_run_dir"
{
  git rev-parse HEAD
  java -version 2>&1
  sw_vers
  sysctl -n machdep.cpu.brand_string
  sysctl -n hw.memsize
} > "$first_run_dir/environment.txt"
java -jar "$exact_jar" '.*KafkaCodecAllocationBenchmark.*' \
  -t 1 -f 2 -wi 3 -i 5 -w 1s -r 1s -prof gc -rf json \
  -rff "$first_run_dir/jmh.json"
python3 benchmark/serializer-benchmark/scripts/summarize-jmh.py run \
  --input "$first_run_dir/jmh.json" --output "$first_run_dir/summary.csv"
```

두 번째 evidence run은 첫 번째 run과 summary가 완료된 뒤에만 시작했다.

```bash
second_run_id="run-$(date -u +%Y%m%dT%H%M%SZ)"
second_run_dir="$evidence_root/$second_run_id"
mkdir -p "$second_run_dir"
{
  git rev-parse HEAD
  java -version 2>&1
  sw_vers
  sysctl -n machdep.cpu.brand_string
  sysctl -n hw.memsize
} > "$second_run_dir/environment.txt"
java -jar "$exact_jar" '.*KafkaCodecAllocationBenchmark.*' \
  -t 1 -f 2 -wi 3 -i 5 -w 1s -r 1s -prof gc -rf json \
  -rff "$second_run_dir/jmh.json"
python3 benchmark/serializer-benchmark/scripts/summarize-jmh.py run \
  --input "$second_run_dir/jmh.json" --output "$second_run_dir/summary.csv"

python3 benchmark/serializer-benchmark/scripts/summarize-jmh.py compare \
  --run "$first_run_dir/summary.csv" \
  --run "$second_run_dir/summary.csv" \
  --output 'docs/benchmarks/raw/issue-758/comparison.csv'
```

## 실행 조건

| 항목 | 값 |
|---|---|
| Evidence commit | `60fdb9b90ba129f6bd1de4747a3f6d9e960fbdf9` |
| Run IDs | `run-20260718T204256Z`, `run-20260718T204443Z` |
| JDK | Oracle GraalVM 21.0.11+9.1, Java 21.0.11 LTS, JVMCI 23.1-b92 |
| OS | macOS 26.5.2, build 25F84 |
| CPU | Apple M4 Pro |
| Memory | 51539607552 bytes |
| JMH | 1.37; one thread; two forks; three warmup iterations; five measurement iterations; 1-second warmup and measurement windows; GC profiler |
| Primary decision metric | `gc.alloc.rate.norm` in B/op |

두 evidence run은 같은 measured environment에서 순차 실행했다. Direction은 optimized
`ByteBuffer` cell이 두 run 모두에서 `ByteArray` baseline보다 allocation을 최소 5% 적게
할당할 때만 accepted가 된다.

## Raw Artifact

- Run 1: [environment](raw/issue-758/run-20260718T204256Z/environment.txt), [JMH JSON](raw/issue-758/run-20260718T204256Z/jmh.json), [summary CSV](raw/issue-758/run-20260718T204256Z/summary.csv)
- Run 2: [environment](raw/issue-758/run-20260718T204443Z/environment.txt), [JMH JSON](raw/issue-758/run-20260718T204443Z/jmh.json), [summary CSV](raw/issue-758/run-20260718T204443Z/summary.csv)
- [Two-run comparison CSV](raw/issue-758/comparison.csv)
- Chart: 생성하지 않았다. Table과 raw JMH JSON이 authoritative source다.

## Allocation 결과

아래 값은 `comparison.csv`를 정확히 재현한다. Negative delta는 optimized `ByteBuffer` cell이
`ByteArray` baseline보다 더 적은 B/op를 할당했음을 뜻한다.

| Direction | Run | ByteArray baseline B/op | Optimized ByteBuffer B/op | Delta | Comparison verdict |
|---|---|---:|---:|---:|---|
| Deserialize | `run-20260718T204256Z` | 3316.0038829966643 | 3168.004115046093 | -4.463196% | inconclusive |
| Deserialize | `run-20260718T204443Z` | 3328.0038476713153 | 3168.004103248346 | -4.807679% | inconclusive |
| Serialize | `run-20260718T204256Z` | 1288.0021052118198 | 116.00226561432541 | -90.993628% | accepted |
| Serialize | `run-20260718T204443Z` | 1288.0021419231969 | 104.00230389806492 | -91.9253% | accepted |

## Diagnostic Throughput

Throughput score와 error는 raw JMH JSON과 각 `summary.csv`에서 확인할 수 있다.
Throughput은 diagnostic only이며 allocation decision을 확립하지 않는다.

## 해석

- Serialize는 accepted다. 두 fresh run 모두 optimized `ByteBuffer` path의
  `gc.alloc.rate.norm`이 `ByteArray` baseline보다 최소 5% 낮았다.
- Deserialize는 inconclusive다. Optimized `ByteBuffer` path가 두 run 모두에서 낮았지만, 어느
  exact delta도 요구된 5% reduction threshold에 도달하지 못했다.

이 결과는 measured codec condition에서 serialization에 대해서만 allocation-reduction claim을
뒷받침한다. Zero-copy Kafka behavior나 broker throughput을 입증하지 않는다.

## 한계

이 evidence는 committed payload, 현재 Kryo configuration, caller-owned heap output, bounded
direct input, codec-only loop, measured environment에 적용된다. Broker behavior, network
transport, batching, compression, header creation, 다른 payload 또는 codec configuration은
제외한다. Zero-copy Kafka 또는 broker-throughput claim은 하지 않는다.
