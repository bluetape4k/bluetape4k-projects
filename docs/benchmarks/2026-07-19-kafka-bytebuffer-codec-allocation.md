# Kafka ByteBuffer Codec Allocation Benchmark - 2026-07-19

## Scope

Issue [#758](https://github.com/bluetape4k/bluetape4k-projects/issues/758) compares standard Kafka `ByteArray` codec calls with opt-in, caller-owned `ByteBuffer` calls for the same Kryo-backed `BinaryKafkaCodec` payload. The benchmark isolates codec serialization and deserialization. It excludes the Kafka broker, network transport, batching, compression, and header creation.

## Commands

The generated benchmark tasks were reconfirmed before building the benchmark jar:

```bash
./gradlew :serializer-benchmark:tasks --all --no-configuration-cache | \
  rg 'benchmarkBenchmark(Compile|Jar)|compileBenchmarkKotlin'

./gradlew :serializer-benchmark:clean :serializer-benchmark:benchmarkBenchmarkJar \
  --no-configuration-cache --no-build-cache

exact_jar='benchmark/serializer-benchmark/build/benchmarks/benchmark/jars/serializer-benchmark-benchmark-jmh-1.12.0-JMH.jar'

java -jar "$exact_jar" '.*KafkaCodecAllocationBenchmark.*' \
  -t 1 -f 1 -wi 1 -i 1 -w 1s -r 1s -prof gc
```

The first evidence run used:

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

The second evidence run started only after the first run and its summary completed:

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

## Run Conditions

| Field | Value |
|---|---|
| Evidence commit | `60fdb9b90ba129f6bd1de4747a3f6d9e960fbdf9` |
| Run IDs | `run-20260718T204256Z`, `run-20260718T204443Z` |
| JDK | Oracle GraalVM 21.0.11+9.1, Java 21.0.11 LTS, JVMCI 23.1-b92 |
| OS | macOS 26.5.2, build 25F84 |
| CPU | Apple M4 Pro |
| Memory | 51539607552 bytes |
| JMH | 1.37; one thread; two forks; three warmup iterations; five measurement iterations; 1-second warmup and measurement windows; GC profiler |
| Primary decision metric | `gc.alloc.rate.norm` in B/op |

The two evidence runs executed sequentially on the same measured environment. A direction is accepted only when the optimized `ByteBuffer` cell allocates at least 5% less than its `ByteArray` baseline in both runs.

## Raw Artifacts

- Run 1: [environment](raw/issue-758/run-20260718T204256Z/environment.txt), [JMH JSON](raw/issue-758/run-20260718T204256Z/jmh.json), [summary CSV](raw/issue-758/run-20260718T204256Z/summary.csv)
- Run 2: [environment](raw/issue-758/run-20260718T204443Z/environment.txt), [JMH JSON](raw/issue-758/run-20260718T204443Z/jmh.json), [summary CSV](raw/issue-758/run-20260718T204443Z/summary.csv)
- [Two-run comparison CSV](raw/issue-758/comparison.csv)
- Charts: Not produced. The tables and raw JMH JSON are authoritative.

## Allocation Results

The values below reproduce `comparison.csv` exactly. Negative deltas mean the optimized `ByteBuffer` cell allocated fewer B/op than the `ByteArray` baseline.

| Direction | Run | ByteArray baseline B/op | Optimized ByteBuffer B/op | Delta | Comparison verdict |
|---|---|---:|---:|---:|---|
| Deserialize | `run-20260718T204256Z` | 3316.0038829966643 | 3168.004115046093 | -4.463196% | inconclusive |
| Deserialize | `run-20260718T204443Z` | 3328.0038476713153 | 3168.004103248346 | -4.807679% | inconclusive |
| Serialize | `run-20260718T204256Z` | 1288.0021052118198 | 116.00226561432541 | -90.993628% | accepted |
| Serialize | `run-20260718T204443Z` | 1288.0021419231969 | 104.00230389806492 | -91.9253% | accepted |

## Diagnostic Throughput

Throughput scores and errors remain available in the raw JMH JSON and each `summary.csv`. Throughput is diagnostic only and does not establish the allocation decision.

## Interpretation

- Serialize is accepted: both fresh runs show at least 5% lower `gc.alloc.rate.norm` for the optimized `ByteBuffer` path than for the `ByteArray` baseline.
- Deserialize is inconclusive: the optimized `ByteBuffer` path was lower in both runs, but neither exact delta reached the required 5% reduction threshold.

These results support an allocation-reduction claim only for serialization under the measured codec conditions. They do not establish zero-copy Kafka behavior or broker throughput.

## Limitations

The evidence applies to the committed payload, the current Kryo configuration, caller-owned heap output, bounded direct input, the codec-only loop, and the measured environment. It excludes broker behavior, network transport, batching, compression, header creation, and other payloads or codec configurations. No zero-copy Kafka or broker-throughput claim is made.
