# 2026-05-29 - Issue 674 CSV Okio writer fast path

## 배경

issue #674는 CSV reader Okio 작업의 후속으로 large CSV export pipeline을 대상으로
했다. `FlowCsvWriter.writeFile`은 이미 `Flow<Iterable<*>>`를 받았지만, UTF-8 file
output은 여전히 `OutputStreamWriter`와 많은 작은 `Writer.write(...)` call을 거쳤다.

## 결정

public `FlowCsvWriter` contract는 변경하지 않고 UTF-8 file output을 internal Okio
`BufferedSink` writer로 보낸다. non-UTF-8 encoding은 기존 writer fallback path에
남긴다.

fast path는 기존 writer semantic을 보존한다.

- `null`은 unquoted empty field.
- empty string은 quoted `""`.
- doubled-quote escaping.
- `quoteAll`.
- CSV/TSV delimiter와 line separator.

## 결과

writer benchmark는 legacy Writer baseline과 public Okio-backed `writeFile` path를
비교한다.

| Workload | Writer baseline | Okio writer | Speedup |
|---|---:|---:|---:|
| small | 11,741.418 ops/s | 16,355.656 ops/s | 1.39x |
| medium | 676.110 ops/s | 2,068.696 ops/s | 3.06x |
| large | 83.802 ops/s | 272.157 ops/s | 3.25x |

## 검증

- `./gradlew :bluetape4k-csv:test --tests 'io.bluetape4k.csv.v2.FlowCsvWriterTest'` - 21 passing.
- `./gradlew :bluetape4k-csv:test` - 271 passing.
- `./gradlew :bluetape4k-csv:testBenchmark` - 위 writer benchmark.

## 향후 가드

CSV writer performance work에서는 public `writeFile` path를 benchmark하고, future
agent가 production fallback flag를 다시 도입하지 않고도 이전 `Writer` behavior와
비교할 수 있도록 benchmark에 baseline implementation을 유지한다.
