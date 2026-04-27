# io/http 테스트 커버리지 향상 설계 (32.40% → 70%)

## 목표

`io/http` 모듈의 테스트 커버리지를 32.40%에서 70% 이상으로 향상시킵니다.

관련 이슈: #178

## 현황 분석

### 기존 테스트 (커버된 영역)

| 패키지 | 커버된 파일 |
|--------|------------|
| `hc5/cache` | `InMemoryHttpCacheStorage`, `JavaCacheHttpCacheStorage` |
| `okhttp3` | `OkHttp3Support`, `LoggingInterceptor`, `CachingInterceptor`, `OkHttpClientExtensionsCoroutines`, `OkHttpResponseExtensions` |
| `okhttp3/mock` | `MockWebServerExtensions` |

### 테스트 공백 (미커버 영역)

**Group 1 — 순수 단위 테스트 대상 (서버 불필요, DSL/빌더 함수)**

| 패키지 | 파일 수 | 주요 파일 |
|--------|---------|----------|
| `hc5/http` | 16 | `HttpHost`, `ContentTypes`, `BasicRequestBuilder`, `BasicResponseBuilder`, `ClassicRequestBuilder`, `AuthScope`, `ConnectExceptionSupport`, `Operations`, `HttpRequest`, `ConnectionConfig`, `RequestConfig`, `SocketConfig`, `TlsConfig`, `Http1Config`, `ContextBuilder`, `CharCodingConfig` |
| `hc5/entity` | 5 | `EntityBuilder`, `HttpEntitySupport`, `FormBodyPartBuilder`, `MultipartEntityBuilder`, `MultipartPartBuilder` |
| `hc5/auth` | 1 | `CredentialsProviderBuilder` |
| `hc5/ssl` | 2 | `HttpsSupport`, `SSLContexts` |
| `hc5/http2` | 1 | `H2Config` |
| `hc5/reactor` | 1 | `IOReactorConfig` |

**Group 2 — 통합 테스트 대상 (BluetapeHttpServer 로컬 httpbin 사용)**

| 패키지 | 파일 수 | 주요 파일 |
|--------|---------|----------|
| `hc5/async` | 5+5 | `HttpAsyncClient`, `CloseableHttpAsyncClientCoroutines`, `AsyncClientConnectionManager`, `MinimalHttpAsyncClient`, `async/methods/*` |
| `hc5/classic` | 4 | `HttpClient`, `HttpClientConnectionManager`, `MinimalHttpClient`, `VirtualThreadHttpClient` |
| `jdk` | 2 | `JdkHttpClientSupport`, `JdkHttpClientCoroutines` |
| `hc5/routing` | 1 | `RoutingSupport` |

**Group 3 — 기타**

| 패키지 | 파일 수 | 비고 |
|--------|---------|------|
| `vertx` | 1 | `VertxHttpClientSupport` — Vert.x 의존성 복잡, 우선순위 낮음 |
| `hc5/cache` builders | 2 | `CachingHttpClientBuilder`, `CachingHttpAsyncClientBuilder` |
| `hc5/fluent` | 1 | `Request.kt` |
| `hc5/protocol` | 1 | `HttpClientContext` |

## 설계 방향

### 우선순위 전략

커버리지 향상 효율 = (커버 라인 수) / (구현 난이도)

1. **Group 1 우선**: 순수 단위 테스트. 외부 의존성 없음, 높은 ROI
2. **Group 2 중간**: BluetapeHttpServer (Testcontainers)를 이미 AbstractHc5Test에서 사용하므로 재사용 가능
3. **Group 3 선택적**: Vert.x는 제외, cache builders와 fluent는 포함

### 테스트 구조

#### Group 1 — 단위 테스트 신규 파일

```
src/test/kotlin/io/bluetape4k/http/hc5/http/
  ├── HttpHostTest.kt              # URI.toHttpHost(), httpHostOf()
  ├── ContentTypesTest.kt          # ContentTypes.TEXT_PLAIN_UTF8
  ├── BasicRequestBuilderTest.kt   # basicHttpRequest(), basicHttpRequestOf()
  ├── BasicResponseBuilderTest.kt  # basicHttpResponse()
  ├── ClassicRequestBuilderTest.kt # classicRequest()
  ├── AuthScopeTest.kt             # authScopeOf() 오버로드
  ├── ConnectExceptionSupportTest.kt # IOException 확장
  ├── OperationsTest.kt            # Future.toCancellable()
  ├── HttpRequestTest.kt           # extractPathPrefix()
  └── ConfigBuilderTest.kt         # ConnectionConfig, RequestConfig, SocketConfig, TlsConfig, Http1Config, CharCodingConfig

src/test/kotlin/io/bluetape4k/http/hc5/entity/
  ├── EntityBuilderTest.kt         # httpEntity(), httpEntityOf()
  ├── HttpEntitySupportTest.kt     # consumeQuietly(), consume(), toByteArrayOrNull(), toStringOrNull()
  └── MimeEntityTest.kt            # FormBodyPartBuilder, MultipartEntityBuilder, MultipartPartBuilder

src/test/kotlin/io/bluetape4k/http/hc5/auth/
  └── CredentialsProviderBuilderTest.kt # credentialsProvider(), emptyCredentialsProvider(), credentialsProviderOf()

src/test/kotlin/io/bluetape4k/http/hc5/ssl/
  └── SslSupportTest.kt            # HttpsSupport, SSLContexts (기본 SSL 컨텍스트 생성)

src/test/kotlin/io/bluetape4k/http/hc5/http2/
  └── H2ConfigTest.kt              # H2Config DSL

src/test/kotlin/io/bluetape4k/http/hc5/reactor/
  └── IOReactorConfigTest.kt       # IOReactorConfig DSL
```

#### Group 2 — 통합 테스트 신규 파일

```
src/test/kotlin/io/bluetape4k/http/hc5/classic/
  └── ClassicHttpClientTest.kt     # httpClient(), minimalHttpClient(), virtualThreadHttpClient()

src/test/kotlin/io/bluetape4k/http/hc5/async/
  ├── AsyncHttpClientTest.kt       # httpAsyncClient() GET/POST
  └── AsyncHttpClientCoroutinesTest.kt # suspend executeAwait()

src/test/kotlin/io/bluetape4k/http/jdk/
  ├── JdkHttpClientSupportTest.kt  # jdkHttpClientOf(), jdkVirtualThreadHttpClientOf()
  └── JdkHttpClientCoroutinesTest.kt # getAwait(), getStringAwait(), sendAwait()
```

## DoD

- [ ] `io/http` 모듈 전체 테스트 통과
- [ ] 커버리지 70% 이상 달성 (JaCoCo 기준)
- [ ] 모든 신규 테스트 클래스에 `companion object : KLogging()` 포함
- [ ] 기존 테스트와 충돌 없음
- [ ] `README.md` + `README.ko.md` 업데이트 (테스트 커버리지 현황 반영)
