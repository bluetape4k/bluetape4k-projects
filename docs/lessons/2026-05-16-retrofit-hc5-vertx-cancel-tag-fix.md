# Retrofit HC5 / Vert.x cancel() 전파와 tag() 저장 수정

**날짜**: 2026-05-16
**이슈**: #484, #489
**PR**: #512

## 근본 원인

### cancel() — 불완전한 전파

`Hc5CallFactory.AsyncClientCall`과 `VertxCallFactory.VertxCall`은 모두 `promise?.cancel(true)`만
호출했다. 이는 `CompletableFuture`만 취소하고 하위 network request는 취소하지 않는다.
HC5 `Future<SimpleHttpResponse>`와 Vert.x `HttpClientRequest`는 계속 실행되어 30초 `callTimeout`이
만료될 때까지 thread를 blocking했다.

`isCanceled()`를 `promise?.isCancelled ?: false`에서 가져온 것도 잘못이었다.
이 값은 `cancel()`이 이미 실행되기 전까지 `false`를 반환하므로 pre-execute read가 항상 false다.

### tag() — 조용한 no-op

두 factory의 네 `tag()` overload 모두 backing store가 없었다. `tag(type, computeIfAbsent)`는
매번 값을 계산한 뒤 버렸고, `tag(type)`은 항상 null을 반환했다.

### #489 — 이른 `@Disabled`

`ApacheHc5HttpbinCoroutineJacksonTest`와 `ApacheHc5HttpbinCoroutineFastjsonTest`에는 모두
`get post's comments()`에 대한 빈 `@Disabled` override가 있었다. 하위 parent test는
`AsyncApacheHttp5Client`에서 올바르게 통과하므로 disable은 필요하지 않았고 coverage를 억제했다.

## 수정

### Hc5CallFactory

- `@Volatile var hc5Future: Future<SimpleHttpResponse>? = null` 추가.
  `asyncClient.execute()` 반환 직후 할당한다.
- `cancel()`이 `promise?.cancel(true)`와 함께 `hc5Future?.cancel(true)`도 호출한다.
- `@Volatile var cancelled = false` 추가. `isCanceled()`는 `cancelled`를 직접 반환한다.
- `executeAsync()`의 post-assignment race guard: `hc5Future`가 할당될 때 이미 `cancelled`가
  true이면 즉시 취소한다.
- Tag no-op을 `type.java`를 key로 쓰는 `ConcurrentHashMap<Class<*>, Any>`로 교체했다.

### VertxCallFactory

- `@Volatile var vertxRequest: HttpClientRequest? = null` 추가.
  `req.send()` 전에 `onSuccess` handler 내부에서 할당한다.
- `cancel()`은 `vertxRequest?.reset()`을 호출한다(Vert.x 5.x `reset()`은 멱등적).
- HC5와 같은 `@Volatile cancelled` pattern과 post-assignment race guard 적용.
- 동일한 `ConcurrentHashMap` tag storage 적용.

### #489

두 HC5 coroutine test class에서 `@Disabled` override와 빈 body를 제거했다.

## 스레드 안전성 분석

JMM에서 `cancelled` + `hc5Future`/`vertxRequest`의 volatile read/write pair로 충분하다:

- Path A: `cancel()`이 먼저 실행 → `cancelled=true` 설정, `hc5Future=null` 읽음(no-op) →
  `executeAsync()`가 `hc5Future`를 할당하고 `cancelled=true`를 확인해 즉시 취소.
- Path B: `executeAsync()`가 먼저 할당 → `cancel()`이 non-null `hc5Future`를 읽고 취소.

두 path 모두 cover되며 추가 lock은 필요 없다.

## 검증

`Hc5HttpClientTest`와 `VertxHttpClientTest`에 regression test를 추가했다:

1. `cancel sets isCanceled to true immediately before execute` — `isCanceled()` authority contract.
2. `cancel during enqueue propagates to underlying request and fires onFailure promptly` —
   `SocketPolicy.NO_RESPONSE` + 5 s `CountDownLatch`; 수정 전에는 30초 hang 후 실패.
3. `tag computeIfAbsent caches and returns same instance on repeated calls` — `shouldBeSameInstanceAs`.
4. `tag read returns null when tag has not been set`.

결과: 24 tests(`Hc5HttpClientTest`), 24 tests(`VertxHttpClientTest`) — failure 0, skipped 0.
Feign: 232 tests — failure 0, skipped 0(기존 disabled test 2개가 이제 통과).

## 향후 가이드

- `CompletableFuture.cancel(true)`는 future state만 전환하며 하위 blocking operation으로 전파되지 않는다.
  실제 async handle을 capture하고 취소해야 한다.
- `isCanceled()`는 future state가 아니라 user intent(`cancelled` flag)를 반영해야 한다.
- `tag()` read와 `tag(type, computeIfAbsent)` overload는 하나의 backing map을 공유해야 한다.
  Type-safe concurrent access에는 `type.java`를 key로 쓰는 `ConcurrentHashMap<Class<*>, Any>`를 사용한다.
- Vert.x `HttpClientRequest.reset()`은 5.x에서 멱등적이다. 이 동작에 의존할 때는 문서화한다.
