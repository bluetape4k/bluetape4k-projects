# io/http 테스트 커버리지 향상 구현 플랜

관련 이슈: #178 Spec: docs/superpowers/specs/2026-04-27-http-test-coverage-design.md

## Task 목록

### Group 1 — 순수 단위 테스트 (complexity: low)

**T1** `hc5/http/HttpHostTest.kt` 작성

- complexity: low
- `URI.toHttpHost()`, `httpHostOf()` 검증
- HTTP/HTTPS URL 파싱, 포트 포함 URL 파싱

**T2** `hc5/http/ContentTypesTest.kt` 작성

- complexity: low
- `ContentTypes.TEXT_PLAIN_UTF8` MIME 타입 및 charset 검증

**T3** `hc5/http/BasicRequestBuilderTest.kt` 작성

- complexity: low
- `basicHttpRequest(String)`, `basicHttpRequest(Method)` DSL
- `basicHttpRequestOf()` 오버로드 (host+path, headers 포함)

**T4** `hc5/http/BasicResponseBuilderTest.kt` 작성

- complexity: low
- `basicHttpResponse(Int)`, `basicHttpResponse(HttpResponse)` 상태코드/헤더 검증

**T5** `hc5/http/ClassicRequestBuilderTest.kt` 작성

- complexity: low
- `classicRequest(String)`, `classicRequest(Method)` URI/메서드 검증

**T6** `hc5/http/AuthScopeTest.kt` 작성

- complexity: low
- `authScopeOf(protocol, host, port)`, `authScopeOf(HttpHost)`, `authScopeOf(url)`, `authScopeOf(host, port)` 4가지 오버로드

**T7** `hc5/http/ConnectExceptionSupportTest.kt` 작성

- complexity: low
- `IOException.toConnectTimeoutException()`, `IOException.toHttpHostConnectException()`, `IOException.enhance()` 검증

**T8** `hc5/http/OperationsTest.kt` 작성

- complexity: low
- `Future<*>.toCancellable()` — CompletableFuture 로 검증

**T9** `hc5/http/HttpRequestTest.kt` 작성

- complexity: low
- `HttpRequest.extractPathPrefix()` — classicRequest DSL로 요청 생성 후 prefix 추출 검증

**T10** `hc5/http/ConfigBuilderTest.kt` 작성

- complexity: low
- `ConnectionConfig`, `RequestConfig`, `SocketConfig`, `TlsConfig`, `Http1Config`, `CharCodingConfig` 빌더 DSL 파라미터 검증

**T11** `hc5/http2/H2ConfigTest.kt` 작성

- complexity: low
- `H2Config` DSL 빌더 파라미터 검증

**T12** `hc5/reactor/IOReactorConfigTest.kt` 작성

- complexity: low
- `IOReactorConfig` DSL 빌더 파라미터 검증

**T13** `hc5/entity/EntityBuilderTest.kt` 작성

- complexity: low
- `httpEntity {}`, `httpEntityOf(String)`, `httpEntityOf(ByteArray)` — Content-Type, 인코딩, 내용 검증

**T14** `hc5/entity/HttpEntitySupportTest.kt` 작성

- complexity: low
- `consumeQuietly()`, `consume()`, `toByteArrayOrNull()`, `toStringOrNull()`, `parse()` 검증
- StringEntity / ByteArrayEntity 로 검증

**T15** `hc5/entity/MimeEntityTest.kt` 작성

- complexity: low
- `FormBodyPartBuilder`, `MultipartEntityBuilder`, `MultipartPartBuilder` DSL 빌더 생성 검증

**T16** `hc5/auth/CredentialsProviderBuilderTest.kt` 작성

- complexity: low
- `credentialsProvider {}`, `emptyCredentialsProvider()`, `credentialsProviderOf(AuthScope, Credentials)` 검증
- `UsernamePasswordCredentials` 를 이용하여 자격증명 조회 검증

**T17** `hc5/ssl/SslSupportTest.kt` 작성

- complexity: low
- `SSLContexts.createDefault()`, `SSLContexts.createSystemDefault()`, `HttpsSupport` 기본 TLS 설정 생성 검증

### Group 2 — 통합 테스트 (complexity: medium)

**T18** `hc5/classic/ClassicHttpClientTest.kt` 작성

- complexity: medium
- `AbstractHc5Test` 상속
- `httpClientOf {}`, `minimalHttpClientOf {}`, `virtualThreadHttpClientOf {}` 빌더 검증
- GET 요청 → httpbin `/get` 응답 상태코드 200 검증

**T19** `hc5/async/AsyncHttpClientTest.kt` 작성

- complexity: medium
- `AbstractHc5Test` 상속
- `httpAsyncClientOf {}` 빌더로 클라이언트 생성 검증
- `SimpleHttpRequests.get()` 으로 비동기 GET 요청 → 상태코드 200 검증

**T20** `hc5/async/AsyncHttpClientCoroutinesTest.kt` 작성

- complexity: medium
- `AbstractHc5Test` 상속
- `executeAwait()` suspend 함수 검증 — `/get`, `/ip`, `/headers` 병렬 요청

**T21** `jdk/JdkHttpClientSupportTest.kt` 작성

- complexity: medium
- `jdkHttpClientOf()`, `jdkVirtualThreadHttpClientOf()` 클라이언트 생성 검증
- `AbstractHttpTest` 상속, GET 요청 → 상태코드 200

**T22** `jdk/JdkHttpClientCoroutinesTest.kt` 작성

- complexity: medium
- `AbstractHttpTest` 상속
- `getAwait(uri)`, `getStringAwait(uri)`, `sendAwait()` suspend 함수 검증

### Group 3 — 추가 커버리지 (complexity: low)

**T23** `hc5/cache/CachingHttpClientBuilderTest.kt` 작성

- complexity: low
- `CachingHttpClientBuilder` DSL — 캐시 설정 포함 클라이언트 생성 검증

**T24** `hc5/fluent/FluentRequestTest.kt` 작성

- complexity: low
- `Request.kt` DSL 빌더 기본 생성 검증 (fluent API 체인)

### 최종 검증 (complexity: low)

**T25** 전체 테스트 실행 및 커버리지 확인

- complexity: low
- `./gradlew :bluetape4k-http:test` 전수 통과 확인
- JaCoCo 커버리지 리포트 생성 및 70% 이상 확인
- `docs/testlogs/2026-04.md` testlog 업데이트

**T26** README 업데이트

- complexity: low
- `io/http/README.md` + `io/http/README.ko.md` 커버리지 현황 반영

## 의존성 순서

```
T1-T17 (단위) → 병렬 실행 가능
T18-T22 (통합) → AbstractHc5Test/AbstractHttpTest 의존, 병렬 실행 가능
T23-T24 → T18 완료 후
T25 → T1-T24 전체 완료 후
T26 → T25 완료 후
```

## 모듈 경로

- 소스: `io/http/src/main/kotlin/io/bluetape4k/http/`
- 테스트: `io/http/src/test/kotlin/io/bluetape4k/http/`
- Gradle 모듈명: `:bluetape4k-http`
