# 이슈 #975 Kafka CI coverage

## 배경

Push CI는 `infra/kafka/**` 변경에서 `:bluetape4k-kafka:test`를 실행하지 않았다.
기존 `infra` lane은 Redis/cache 중심이었고, `telemetry-infra`는
`infra/opentelemetry/**`와 `infra/kafka-logback/**`를 다뤘다.

## 결정

Kafka를 Redis 중심 infra lane에 섞지 않고, 전용 `kafka-infra` path-filter output과
`Test / Kafka Infra` job을 추가한다.

## 근거

- `infra/kafka` test는 embedded Kafka/Testcontainers-style infrastructure를 사용하므로 `--max-workers=1`로 순차 실행해야 한다.
- 전용 job은 Kafka failure를 Redis/cache와 telemetry/logback failure에서 분리한다.
- Skipped/failed Kafka coverage가 보이도록 CI aggregation은 새 job을 `coverage-report.needs`와 `ci-status.needs` 양쪽에 나열해야 한다.

## 검증

- `actionlint .github/workflows/ci.yml`
- Backslash-single-quote guard for GitHub Actions expressions
- `git diff --check`
- `./gradlew :bluetape4k-kafka:cleanTest :bluetape4k-kafka:test --max-workers=1 --no-build-cache --no-configuration-cache`
- `./gradlew :bluetape4k-kafka:koverXmlReport --max-workers=1 --no-configuration-cache`

## 향후 방지책

Module family에 CI coverage를 추가할 때는 네 surface를 함께 갱신한다.
Path-filter output, test job, `coverage-report.needs`, `ci-status.needs`가 모두
포함되어야 한다.
