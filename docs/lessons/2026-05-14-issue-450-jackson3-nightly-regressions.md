# Issue 450 Jackson3 Nightly Regression

## 배경

Jackson3 consumer migration 이후 `bluetape4k-projects` Nightly가 실패했다. 실패한
job은 `Test / IO HTTP`와 `Test / Data (nosql)`였다.

## 결정

Iterator stream decoding에는 `FAIL_ON_TRAILING_TOKENS`가 없는 Jackson3 object reader를
사용한다. 각 iterator element는 이후 array element가 아직 남아 있는 parser에서 읽히기
때문이다. Cassandra JSON function example에서는 Jackson3 node나 object를 DataStax JSON
codec을 통해 routing하지 않고, Jackson3가 만든 JSON text를 `fromJson`에 전달한다.

## 결과

실패하던 Feign iterator test와 Cassandra JSON function test는 이제 Jackson3 runtime에서
로컬로 통과한다.

## 검증

- `./gradlew :bluetape4k-feign:test --tests io.bluetape4k.feign.codec.JacksonIteratorDecoder2Test`
- `./gradlew :bluetape4k-cassandra:test --tests io.bluetape4k.cassandra.examples.json.JacksonJsonFunctionExamples`
- `./gradlew :bluetape4k-cassandra:test --tests io.bluetape4k.cassandra.examples.json.JacksonJsonFunctionExamples --rerun-tasks`

## 향후 메모

Jackson3 streaming reader에서는 하나의 parser에서 여러 value를 읽을 때 strict
trailing-token behavior를 확인한다. Cassandra `fromJson`에서는 codec이 사용 중인 정확한
Jackson major version을 지원한다고 알려진 경우가 아니라면 driver JSON codec보다
명시적인 JSON text를 우선한다.
