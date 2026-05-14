# Issue 450 Jackson3 Nightly Regressions

## Context

Nightly after the Jackson3 consumer migration failed in `bluetape4k-projects`.
The failing jobs were `Test / IO HTTP` and `Test / Data (nosql)`.

## Decision

Use a Jackson3 object reader without `FAIL_ON_TRAILING_TOKENS` for iterator
stream decoding, because each iterator element is read from a parser that still
has later array elements available. For Cassandra JSON function examples, pass
Jackson3-generated JSON text to `fromJson` instead of routing Jackson3 nodes or
objects through DataStax JSON codecs.

## Outcome

The failing Feign iterator test and Cassandra JSON function test now pass
locally with the Jackson3 runtime.

## Verification

- `./gradlew :bluetape4k-feign:test --tests io.bluetape4k.feign.codec.JacksonIteratorDecoder2Test`
- `./gradlew :bluetape4k-cassandra:test --tests io.bluetape4k.cassandra.examples.json.JacksonJsonFunctionExamples`
- `./gradlew :bluetape4k-cassandra:test --tests io.bluetape4k.cassandra.examples.json.JacksonJsonFunctionExamples --rerun-tasks`

## Future Notes

For Jackson3 streaming readers, check strict trailing-token behavior when
reading multiple values from one parser. For Cassandra `fromJson`, prefer
explicit JSON text over driver JSON codecs unless the codec is known to support
the exact Jackson major version in use.
