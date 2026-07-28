# 이슈 #711 Central snapshot retry

## 배경
GitHub runner가 Central Portal snapshot metadata에서 일시적인 HTTP 403 response를 받으면
downstream CI와 Nightly run이 실패할 수 있다.

## 결정
Gradle command semantic을 바꾸지 않고 top-level Gradle build와 Nightly detekt gate를
bounded three-attempt retry loop로 감싼다.

## 검증
- `git diff --check`
- `actionlint .github/workflows/ci.yml .github/workflows/nightly-tests.yml`

## 다음 번
bluetape4k SNAPSHOT dependency가 Central metadata 403으로 실패하면 먼저 upstream publish
status를 확인한 뒤, dependency 또는 catalog churn보다 bounded workflow retry를 우선한다.
