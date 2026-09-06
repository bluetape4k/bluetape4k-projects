# tenant별 ConnectionFactory registry 공용 API 설계

## 목표

`bluetape4k-r2dbc`가 tenant와 프레임워크를 알지 못한 채, 임의의 non-null
key를 `ConnectionFactory`로 조회하고 Spring routing map으로 변환할 수 있는
정적 registry 계약을 제공한다. 현재 두 WebFlux 예제가 복사한 lookup 및
lifecycle 규칙을 provider에서 한 번만 검증한다.

## 현재 근거

- provider의 `data/r2dbc`에는 `connectionPoolOf`와 `r2dbcConnectionPool`만
  있고 tenant registry는 없다.
- 두 workshop consumer는 `Map<Tenant, ConnectionPool>`을 직접 보유하고
  `DisposableBean.destroy()`에서 pool을 dispose한다.
- issue #1648은 pool 생성, tenant parsing, 인증/인가, request context,
  transaction/rollback, 동적 onboarding을 caller 책임으로 명시한다.
- `ConnectionPool`은 `ConnectionFactory`, R2DBC `Closeable`, Reactor
  `Disposable`을 함께 구현하므로 core에서 Spring 타입을 참조하지 않고도
  pool lifecycle을 감쌀 수 있다.

## 선택한 설계

### 공개 타입

```kotlin
package io.bluetape4k.r2dbc.pool

enum class ConnectionFactoryOwnership { BORROWED, OWNED }

class R2dbcConnectionFactoryEntry private constructor(
    val connectionFactory: ConnectionFactory,
    val ownership: ConnectionFactoryOwnership,
    private val closeAction: () -> Mono<Void>,
) {
    internal fun closeResource(): Mono<Void>
    companion object {
        fun borrowed(connectionFactory: ConnectionFactory): R2dbcConnectionFactoryEntry
        fun owned(connectionFactory: ConnectionFactory): R2dbcConnectionFactoryEntry
        fun owned(
            connectionFactory: ConnectionFactory,
            closeAction: () -> Mono<Void>,
        ): R2dbcConnectionFactoryEntry
    }
}

class R2dbcConnectionFactoryRegistry<K : Any>(
    entries: Map<K, R2dbcConnectionFactoryEntry>,
) : io.r2dbc.spi.Closeable, reactor.core.Disposable {
    val keys: Set<K>
    operator fun get(key: K): ConnectionFactory
    fun asMap(): Map<K, ConnectionFactory>
    fun <R : Any> routingMap(keyMapper: (K) -> R): Map<R, ConnectionFactory>
    override fun close(): Mono<Void>
    override fun dispose()
    override fun isDisposed(): Boolean

    companion object {
        fun <K : Any> borrowed(entries: Map<K, out ConnectionFactory>): R2dbcConnectionFactoryRegistry<K>
        fun <K : Any> owned(entries: Map<K, out ConnectionFactory>): R2dbcConnectionFactoryRegistry<K>
    }
}
```

`borrowed`는 조회만 하고 종료하지 않는다. `owned`는 registry가 종료를
책임지며, closeable/disposable resource가 아니면 생성 시 fail-fast한다.
사용자 정의 factory는 두 번째 `owned` overload로 명시적인 `closeAction`을
제공할 수 있다. `entries`, `keys`, `asMap`, `routingMap`의 결과는 registry의
생성 당시 snapshot이며 이후 caller가 원본 map을 바꿔도 변경되지 않는다.

### 조회와 routing

- `get(key)`는 등록되지 않은 key에
  `NoSuchElementException("No ConnectionFactory configured for key '$key'")`를
  던진다.
- `keys`는 입력 map의 insertion order를 유지하는 읽기 전용 집합이다.
- `routingMap`은 key mapper가 만든 결과 key가 충돌하면 조용히 덮어쓰지 않고
  `IllegalArgumentException`으로 실패한다. tenant parsing이나 authorization은
  mapper와 caller context에서 수행한다.
- registry에는 `register`, `unregister`, `Mutex`, global singleton을 넣지 않는다.

### lifecycle

- 같은 factory가 여러 key에 연결되어도 identity 기준으로 한 번만 종료한다.
- `close()`는 첫 호출에서 종료 `Mono`를 만들고, 동시/후속 호출은 같은
  cached `Mono`를 반환한다. 모든 owned resource를 순서대로 시도한 뒤 첫
  오류를 주 오류로 유지하고 후속 오류를 `Throwable.addSuppressed`로 붙인다.
- `dispose()`는 Reactor의 fire-and-forget adapter다. 동일한 atomic close state를
  사용하므로 double-close가 없으며, 관찰 가능한 오류가 필요한 caller는
  `close()`를 subscribe한다. `close()`의 `Mono` 생성과 atomic state 설치는
  하나의 `AtomicReference` compare-and-set으로 묶고, `dispose()`도 같은 cached
  signal을 subscribe한다. `dispose()`의 오류는 adapter의 error consumer에서
  소비하며, 원인 보존이 필요하면 `close()` 결과를 사용한다.
- borrowed entry는 `close()`와 `dispose()` 어느 쪽에서도 종료하지 않는다.
- close state가 설치된 뒤에는 `get`, `asMap`, `routingMap`이
  `IllegalStateException("ConnectionFactory registry is closed")`로 실패한다.
  `keys`는 생성 당시 snapshot이므로 계속 읽을 수 있다.
- 같은 resource identity가 alias된 entry는 ownership가 모두 같아야 한다.
  borrowed/owned 혼합 또는 서로 다른 custom close action은 생성 시
  `IllegalArgumentException`으로 거부한다. 기본 `owned(factory)`의 action은
  factory identity를 action identity로 사용한다.
- 입력 map은 생성 시 iteration 순서대로 별도 `LinkedHashMap`에 복사하고
  unmodifiable wrapper를 사용한다. API는 입력이 `HashMap`일 때의 임의 순서를
  insertion order라고 재해석하지 않고, 관찰된 snapshot 순서만 보장한다.
- lifecycle API는 R2DBC/Project Reactor만 사용하며 Spring `DisposableBean`은
  consumer adapter에 남긴다.

## 대안과 기각

1. **Spring `DisposableBean`을 provider registry에 직접 구현**
   - 기각: core artifact의 framework coupling이 생기고 Ktor caller가 Spring
     타입을 끌어오게 된다.
2. **`Map<K, ConnectionFactory>`만 받고 모든 resource를 무조건 종료**
   - 기각: caller-owned factory를 닫는 파괴적 기본값이며 issue의 ownership
     경계를 위반한다.
3. **mutable registry와 동적 register/unregister 제공**
   - 기각: 이번 issue는 정적 snapshot이며 동시성/인증 경계를 불필요하게
     확장한다. 동적 onboarding은 별도 issue로 남긴다.

## 실패 모드와 대응

1. 알 수 없는 tenant key가 들어오면 즉시 `NoSuchElementException`을 던져
   다른 tenant pool로 fallback하지 않는다.
2. mapper가 두 key를 같은 routing key로 만들면 생성 결과를 덮어쓰지 않고
   `IllegalArgumentException`을 던진다.
3. 동일 pool alias가 여러 key에 등록되어도 identity deduplication으로 한 번만
   종료한다.
4. 복수 pool close가 실패해도 나머지 close를 시도하고 첫 오류와 suppressed
   chain을 보존한다.
5. concurrent lookup은 immutable snapshot만 읽고 close state를 먼저 확인하여
   종료 시작 이후 새 lookup을 거부한다. 종료 시작 전의 lookup은 A/B factory를
   교차시키지 않는다.

## 호환성과 migration

- 새 API는 `bluetape4k-r2dbc`의 additive public API이며 기존 pool builder와
  ABI를 바꾸지 않는다.
- workshop consumer는 `R2dbcConnectionFactoryRegistry.owned(pools)`를 만들고
  `registry[tenant]`, `registry.routingMap(Tenant::id)`, `registry.keys`를
  사용한다. Spring destroy adapter는 `registry.dispose()`만 호출한다.
- provider artifact가 publication/catalog에 반영되기 전에는 consumer migration
  PR을 만들지 않는다. 중앙 catalog의 기존 alias와 BOM version만 사용한다.

## 수용 기준

- [ ] key lookup, keys, snapshot map, routing map, unknown-key 메시지가 public
      KDoc와 테스트로 고정된다.
- [ ] borrowed/owned 및 custom close action의 lifecycle 차이가 테스트된다.
- [ ] close/dispose idempotency, identity deduplication, concurrent close,
      suppressed failure가 테스트된다.
- [ ] tenant A/B concurrent lookup가 서로의 factory를 반환하지 않는다.
- [ ] provider JAR/POM/metadata에 Spring/Ktor 의존성이 유입되지 않는다.
- [ ] README 영문/국문에 동일한 API 계약과 예제가 추가된다.

## DoD

provider 코드·단위 테스트·문서·ABI/POM 검증이 exact head에서 통과하고,
PR은 CI와 독립 리뷰가 수렴한 뒤에만 merge-ready로 보고한다. publication,
catalog 갱신, downstream consumer merge는 이 PR의 구현 증거와 분리된 후속
gate다.
