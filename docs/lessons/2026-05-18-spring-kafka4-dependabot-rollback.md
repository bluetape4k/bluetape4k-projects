# Spring Kafka 4 Dependabot Rollback

## Context

Dependabot PR #524 changed the `spring-kafka4` version-catalog alias from
`4.0.5` to `3.3.15` in `gradle/libs.versions.toml`.

## Decision

Restore `spring-kafka4` to `4.0.5` and ignore `org.springframework.kafka:*`
and `spring-kafka*` version-alias updates in Dependabot.

## Outcome

Spring Kafka 3 and Spring Kafka 4 compatibility lines stay manually managed.
Dependabot cannot infer that both aliases use the same Maven coordinate with
different compatibility baselines.

## Verification

- Checked `origin/develop` for `bluetape4k-projects`.
- Compared all non-archived `bluetape4k` GitHub repositories for compatibility
  alias drift across Spring Boot, Jackson, Kafka, and Spring Kafka lines.

## Future Guard

When a version catalog keeps multiple aliases for one Maven coordinate, do not
let Dependabot update that coordinate unless the aliases can be split or grouped
without collapsing compatibility lines.
