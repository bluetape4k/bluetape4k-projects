# HC5 캐시 DSL 및 OkHttp3 캐시 지원 추가

**날짜**: 2026-05-24
**이슈**: #583
**브랜치**: feat/http-cache-dsl-20260524

---

## 배경

Apache HttpComponents 5 캐싱 클라이언트와 OkHttp3 디스크 캐시의 설정 코드가 반복적이고 장황했다. 캐시 설정 DSL, 컨텍스트 확장 함수, 메트릭 헬퍼를 추가하여 사용 편의성을 개선했다.

---

## 구현 결정

### 1. `memoryCachingHttpAsyncClientOf` auto-start 제거

**문제**: 파라미터 있는 오버로드가 `.also { it.start() }`를 호출하여 이미 시작된 클라이언트를 반환했다. 단일-인자 오버로드는 미시작 상태로 반환하여 API 동작이 불일치했다.

**결정**: 파라미터 있는 오버로드에서 `.also { it.start() }` 제거. 모든 오버로드가 미시작 클라이언트를 반환하도록 통일. 호출자가 `start()`를 직접 호출하거나 `use {}` 블록 내에서 `client.start()`를 호출한다.

**이유**: 동일한 함수명의 오버로드 간에 라이프사이클 계약이 달라지면 double-start 위험이 생기고 호출자 혼란을 야기한다.

### 2. `CacheConfig.kt`에서 검증을 집중

`memoryCacheConfigOf`/`fileCacheConfigOf`에서 `requirePositiveNumber` 검증을 수행하도록 했다. `fileCachingHttpClientOf`/`fileCachingHttpAsyncClientOf`의 `maxCacheMb`는 `fileCacheConfigOf`로 전달되지 않고 직접 나눗셈에 사용되므로 호출부에서도 별도로 검증한다.

**이유**: `maxObjectSizeBytes = 0`이면 나눗셈 시 `ArithmeticException` 발생. 명시적 검증으로 빠른 실패와 명확한 오류 메시지 제공.

### 3. `OkHttp3CacheMetrics`는 `Serializable` 구현

**이유**: bluetape4k 프로젝트 규칙 — 모든 `data class`는 `Serializable`을 구현하고 `serialVersionUID = 1L`을 선언해야 한다.

### 4. `logCacheStatus` / `logMetrics` 파라미터 타입은 `org.slf4j.Logger`

bluetape4k 로깅은 `org.slf4j.Logger`에 `io.bluetape4k.logging.debug` 람다 확장 함수를 추가하는 방식이다. KDoc에서 `[KLogger]`로 잘못 표기되면 링크가 깨지므로 `SLF4J logger` 또는 `[org.slf4j.Logger]`로 표기해야 한다.

---

## HC5 API 확인 사항

- `HttpCacheContext.setCacheResponseStatus(CacheResponseStatus)` — public으로 테스트에서 직접 설정 가능. 단위 테스트에 활용.
- `CachingHttpAsyncClientBuilder.setHttpCacheStorage(HttpCacheStorage)` — sync `HttpCacheStorage`를 받는 오버로드가 존재하여 `InMemoryHttpCacheStorage`를 직접 사용 가능.
- `CachingHttpClientBuilder.setCacheDir(File)` — `Path`가 아닌 `File`을 받음.

---

## 검증

```
:bluetape4k-http:test
51 passing (5.2s) — BUILD SUCCESSFUL
```

커버리지: `CacheConfigTest` (5), `HttpCacheContextExtensionsTest` (10), `OkHttp3CacheSupportTest` (7), `CachingHttpClientBuilderTest` (+2), `CachingHttpAsyncClientBuilderTest` (+2)

---

## 향후 주의사항

- HC5 async 클라이언트 오버로드 추가 시: 라이프사이클 계약 (시작/미시작) 통일 필수
- `fileCachingHttp*Of` 계열에 `maxCacheMb` 파라미터 추가 시: `requirePositiveNumber` 누락 주의 (division-by-zero 위험)
- 로깅 파라미터 KDoc: `[KLogger]` 사용 금지, `org.slf4j.Logger` 사용
