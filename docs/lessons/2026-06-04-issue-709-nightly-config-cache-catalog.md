# 2026-06-04 이슈 709 Nightly config cache와 catalog

## 배경

Nightly workflow는 snapshot과 BOM-managed dependency를 사용하므로, stale
Gradle/configuration state가 versionless dependency coordinate를 드러낼 수 있다.

## 결정

Nightly Gradle command에는 `--no-configuration-cache`를 유지하고, local bluetape4k
alias는 BOM ref를 통해 versioned 상태로 유지한다.

## 결과

Nightly command는 dependency refresh 중 더 이상 configuration cache에 의존하지 않으며,
repo-local catalog alias는 `group:artifact:.` coordinate를 피한다.

## 검증

- planned: `actionlint`, `git diff --check`, command audit, catalog alias audit.

## 향후 규칙

snapshot을 refresh하는 Nightly job에서는 repo-specific proof가 달리 말하지 않는 한 Gradle
action cache와 configuration cache를 모두 비활성화한다.
