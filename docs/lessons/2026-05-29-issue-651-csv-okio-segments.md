# 이슈 651 CSV Okio segment 활용

## 배경

issue #651은 large CSV parsing의 allocation을 줄이고 throughput을 높이는 것을 목표로
했다. 기존 `CsvLexer`는 `Reader`를 통해 decode하고 field마다 `StringBuilder`에 문자를
append했다.

## 결정

지원되는 CSV setting에는 Okio 기반 internal UTF-8 fast path를 사용한다. 조건은
single-byte ASCII delimiter와 quote character, doubled-quote escaping이다. public
`CsvRecordReader` API는 변경하지 않고, 지원하지 않는 charset이나 setting에서는 기존
reader-based lexer로 fallback한다.

Okio `Buffer.UnsafeCursor`는 buffered source 안에서 read-only structural-byte scan에만
사용한다. field byte는 Okio buffer 사이에서 이동하고 field completion 시 한 번만
decode한다.

## 결과

cursor-backed implementation은 기존 CSV behavior를 보존하면서 benchmark 대상 public
reader path의 throughput을 개선했다.

- small: 20,173.731 -> 45,417.110 ops/s
- medium: 296.944 -> 683.434 ops/s
- large: 17.312 -> 40.115 ops/s

## 검증

- `./gradlew :bluetape4k-csv:test`
- `./gradlew :bluetape4k-csv:testBenchmark`

## 향후 지침

Okio `UnsafeCursor`를 사용할 때는 최적화 전에 fixture equivalence test로 behavior를
고정한다. naive byte loop는 정확했지만 성능 향상이 작았다. read-only cursor scan은
scanned buffer segment를 drain해 memory를 제한하면서 유의미한 성능 향상을 제공했다.
