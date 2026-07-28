# 2026-06-04 이슈 707 Nightly Gradle cache

## 배경

bluetape4k repository 전반의 Nightly build가 GitHub runner에서 managed dependency를
간헐적으로 `group:artifact:.` 형태로 resolve했다.

## 결정

scheduled run이 stale dependency-management state를 재사용하지 않도록 Nightly job에서
`gradle/actions/setup-gradle` cache restore/write를 비활성화한다.

## 결과

모든 Nightly `setup-gradle` block은 explicit Gradle dependency refresh를 유지하면서
`cache-disabled: true`를 설정한다.

## 검증

- `.github/workflows/nightly-tests.yml`을 감사해 setup-gradle block이 cache-disabled block과
  일치함을 확인했다.
- planned validation: `actionlint`, `git diff --check`.

## 향후 규칙

Nightly workflow가 snapshot 또는 BOM-managed bluetape4k dependency를 사용할 때는, cache
restore가 stale metadata를 재생하지 않는다는 fresh CI proof가 없는 한 Gradle action
cache를 비활성화한다.
