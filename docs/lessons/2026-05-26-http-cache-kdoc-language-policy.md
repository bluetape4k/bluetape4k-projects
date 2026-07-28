# HTTP Cache KDoc Language Policy Cleanup

**날짜**: 2026-05-26
**이슈**: #633
**브랜치**: docs/issue-633-http-cache-kdoc-english

## 문제

`CachingHttpClientBuilder.kt`와 `CachingHttpAsyncClientBuilder.kt`에는 새로 추가된
영어 KDoc overload 옆에 기존 no-arg cache builder function의 오래된 한국어 KDoc이
남아 있었다. 이는 의미 있게 수정한 KDoc은 영어로 유지한다는 당시 contributor
documentation policy와 충돌했다.

## 해결

original classic/async HTTP cache builder에 남아 있던 한국어 KDoc을 새 parameterized
overload 스타일에 맞춰 영어로 변환했다.

## 검증

- 영향을 받은 두 builder file에 한국어 문장이 남지 않았음을 확인했다.
- `./gradlew :bluetape4k-http:compileKotlin`을 실행했다.

## 교훈

1. 영어 KDoc이 있는 overload를 추가할 때는 PR을 열기 전에 같은 file의 인접
   overload에 오래된 localized KDoc이 남아 있는지 확인한다.
2. 실제 Gradle module path는 `./gradlew projects`에서 확인한다. source path
   `io/http`는 `:io:http`가 아니라 `:bluetape4k-http`에 매핑된다.
