# Spring Kafka 4 Dependabot Rollback

## 배경

Dependabot PR #524는 `gradle/libs.versions.toml`에서 `spring-kafka4` version-catalog alias를
`4.0.5`에서 `3.3.15`로 변경했다.

## 결정

`spring-kafka4`를 `4.0.5`로 복원하고, Dependabot에서 `org.springframework.kafka:*`와
`spring-kafka*` version-alias update를 ignore한다.

## 결과

Spring Kafka 3과 Spring Kafka 4 compatibility line은 수동으로 관리된다. Dependabot은 두 alias가
같은 Maven coordinate를 쓰지만 서로 다른 compatibility baseline을 가진다는 점을 추론할 수 없다.

## 검증

- `bluetape4k-projects`의 `origin/develop` 확인.
- Spring Boot, Jackson, Kafka, Spring Kafka line 전반의 compatibility alias drift를 모든
  non-archived `bluetape4k` GitHub repository에서 비교.

## 향후 가드

Version catalog가 하나의 Maven coordinate에 여러 alias를 유지한다면, compatibility line이 collapse되지
않도록 alias를 split/group할 수 있기 전에는 Dependabot이 그 coordinate를 업데이트하게 두지 않는다.
