# 이슈 #847 JwtReader remaining TTL

issue #847은 `JwtReader.expiredTtl`이 TTL로 문서화되고 사용됐지만 실제로는 JWT `exp`의
absolute epoch milliseconds timestamp를 반환한다는 점을 찾았다.

## 결정

두 개념을 명시적으로 분리한다. `expiresAtMillis`는 absolute expiration timestamp를
노출하고, `remainingTtlMillis`는 남은 TTL milliseconds를 노출한다. 기존 `expiredTtl`
property는 문서화된 이름과의 호환성을 위해 remaining TTL contract를 따른다. `isExpired`는
TTL value를 재사용하지 않고 absolute expiration timestamp를 직접 비교한다.

## 교훈

- public auth API는 하나의 값을 TTL과 absolute timestamp 양쪽으로 overload하면 안 된다.
  단위가 섞이면 cache/session caller가 authorization state를 지나치게 오래 유지할 수 있다.
- one-hour token은 효과적인 regression fixture다. remaining TTL은 약 3,600,000
  milliseconds여야 하고 epoch timestamp는 그보다 훨씬 크다.
- README example은 remaining TTL과 absolute expiration accessor를 모두 보여줘 consumer가
  cache TTL과 audit log에 맞는 값을 선택하게 해야 한다.

## 검증

- RED: `./gradlew :bluetape4k-jwt:test --tests "io.bluetape4k.jwt.reader.JwtReaderExpirationTest.expiredTtl - exp 클레임이 있으면 남은 TTL 밀리초를 반환한다" --no-build-cache`가 `Expected <1782192683000> to be less than <3600001>`로 실패했다.
- GREEN targeted: `./gradlew :bluetape4k-jwt:test --tests "io.bluetape4k.jwt.reader.JwtReaderExpirationTest" --no-build-cache`가 통과했다.
- module: `./gradlew :bluetape4k-jwt:test --no-build-cache`가 150 tests, 10 pending으로 통과했다.
- build: `./gradlew :bluetape4k-jwt:build --no-build-cache`가 통과했다.
- hygiene: `git diff --check`가 통과했다.
- static analysis: `./gradlew detekt`가 `:detekt NO-SOURCE`와 함께 통과했다.
