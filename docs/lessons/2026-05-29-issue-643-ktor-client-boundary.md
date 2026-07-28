# 이슈 643 Ktor client boundary

## 배경

issue #643은 `bluetape4k-projects`가 전용 `bluetape4k-ktor-client` module을 도입할지,
아니면 기존 module 안에 Ktor client support를 유지할지 결정해야 했다.

## 결정

Ktor client ownership은 `bluetape4k-http`에 유지한다. explicit-engine client creation,
Kotlinx JSON content negotiation, timeout default를 위한 얇은 helper만 추가한다.

dependency나 ownership 혼동 없이 HTTP module에 둘 수 없는 더 넓은 API surface가 생길
때까지 별도 Ktor client module은 피한다.

## 결과

`KtorHttpClientSupport`는 이제 다음을 노출한다.

- `defaultKtorClientJson`
- `KtorClientTimeouts`
- `ktorJsonHttpClientOf`
- `ktorCioJsonHttpClientOf`

README locale set은 retry, resilience, authentication, logging, service-specific
plugin이 application-level concern으로 남거나 기존 dedicated module에 속한다고
문서화한다.

## 검증

- `./gradlew :bluetape4k-http:test --tests 'io.bluetape4k.http.ktor.KtorHttpClientSupportTest' --no-configuration-cache`

## 향후 가드

Ktor client helper를 추가할 때는 명확한 cross-application pattern을 먼저 요구한다. 넓은
facade보다 explicit engine selection과 좁은 plugin installation을 우선한다.
