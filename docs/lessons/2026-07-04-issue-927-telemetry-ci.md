# 이슈 #927 Telemetry CI coverage

## 배경

Push CI는 `infra/opentelemetry/**`와 `infra/kafka-logback/**` 변경에서 module test를
건너뛰었다. Nightly가 이미 OpenTelemetry를 다루고 있었지만, push CI는 telemetry와
logging 변경이 targeted module test 없이 통과하는 것을 허용했다.

## 결정

이 module들을 Redis 중심 infra lane에 추가하지 않고, 전용 `telemetry-infra`
path-filter output과 `Test / Telemetry Infra` job을 추가한다.

## 근거

- `infra/kafka-logback`은 Kafka Testcontainers를 사용하므로 `--max-workers=1`로 순차 실행해야 한다.
- `infra/opentelemetry`는 observability 중심이고 이미 Nightly 형태가 있지만, 자체 파일이 바뀔 때 push CI coverage가 필요하다.
- 별도 job은 Redis/cache infra 결과와 telemetry/logback failure를 분리한다.

## 검증

- `actionlint .github/workflows/ci.yml`
- Backslash-single-quote guard for GitHub Actions expressions
- `git diff --check`
- Gradle project/task wiring dry-run
- `:bluetape4k-opentelemetry:test`
- `:bluetape4k-kafka-logback:test`
- Matching Kover XML tasks

## 향후 방지책

Module family에 CI path filter를 추가할 때는 네 surface를 함께 갱신한다.
Path-filter output, test job, `coverage-report.needs`, `ci-status.needs`가 모두
포함된다.
