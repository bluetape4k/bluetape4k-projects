# Snapshot cache action

## 배경

repository가 Central snapshot의 mutable bluetape4k SNAPSHOT artifact에 의존하는 동안,
Nightly는 Gradle dependency caching을 비활성화하지 않아야 한다.

## 결정

scheduled job이 Gradle changing-module cache policy에 따라 dependency metadata를
재사용할 수 있도록 Nightly Gradle setup step에서 `cache-disabled: true`를 제거한다.

## 결과

Nightly는 기존 task structure를 유지하지만, workflow에서 Gradle cache write/read
behavior를 더 이상 명시적으로 비활성화하지 않는다.

## 검증

- `actionlint .github/workflows/*.yml`
- `rg -n -- '--refresh-dependencies|cache-disabled: true' .github/workflows` -> no matches
- `./gradlew help --no-daemon`
- `git diff --check`

## 향후 지침

explicit dependency refresh는 전용 post-publish freshness check에서만 사용한다. 일반
CI, Nightly, Examples workflow는 cached changing-module metadata에 의존하고, test-only
SNAPSHOT dependency가 필요할 때만 targeted warm-up을 추가한다.
