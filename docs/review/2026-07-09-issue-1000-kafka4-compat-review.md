# Review - Issue 1000 Kafka4 Compatibility

## Scope

- `gradle/libs.versions.toml`

## 발견 사항

- P0: none.
- P1: none.

## 증거

- `gh issue view 1000`: issue is open, assigned to `debop`, milestone `1.12.0`, labels include `bug`, `test`, and `dependencies`.
- `./gradlew :bluetape4k-kafka4:dependencyInsight --configuration testRuntimeClasspath --dependency org.apache.kafka:kafka-clients --no-daemon --no-configuration-cache --no-build-cache`: PASS, selected `org.apache.kafka:kafka-clients:4.2.1`.
- `./gradlew :bluetape4k-kafka:test :bluetape4k-kafka4:test :bluetape4k-resilience4j:test :bluetape4k-bucket4j:test --max-workers=1 --no-daemon --no-configuration-cache --no-build-cache`: PASS, `BUILD SUCCESSFUL in 2m 51s`.

## Verdict

Approved for PR. The previous `KafkaClusterTestKit.clientProperties()` failure path is covered by the passing `:bluetape4k-kafka4:test` run.
