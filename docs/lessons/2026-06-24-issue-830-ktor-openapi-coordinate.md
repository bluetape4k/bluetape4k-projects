# 이슈 #830 Ktor OpenAPI Maven coordinate 교훈 (2026-06-24)

관련 이슈: #830
module: `:bluetape4k-ktor-openapi`

## L1: README dependency snippet은 `projectGroup`을 따라야 한다

### 문제

repository가 `projectGroup=io.github.bluetape4k`로 publish하는데도 `ktor/openapi` README
snippet은 `io.bluetape4k`를 사용했다.

snippet을 복사한 사용자는 published Maven group과 맞지 않는 artifact coordinate를 요청하게
된다.

### 교훈

module dependency snippet을 바꿀 때는 오래된 README example을 복사하지 말고
`gradle.properties`에 있는 group id를 검증한다. localized README set은 `README.md`와
`README.ko.md`를 함께 업데이트하고, issue를 닫기 전에 stale module coordinate를 grep한다.

## 증거

- source of truth: `gradle.properties`에는 `projectGroup=io.github.bluetape4k`가 있다.
- fixed snippet: `io.github.bluetape4k:bluetape4k-ktor-openapi:$bluetape4kVersion`.
- validation: `rg "io\\.bluetape4k:bluetape4k-ktor-openapi" ktor/openapi/README.md ktor/openapi/README.ko.md`
  는 변경 후 match를 반환하지 않았다.
